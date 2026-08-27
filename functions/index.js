// ============================================================================
// AI AGENT WARNING: DO NOT MODIFY WITHOUT CROSS-APP SYNC IMPACT ANALYSIS
// This is the server-side Firebase -> Supabase adapter for Fuel Log and PU Pocket.
// Read /ARCHITECTURE.md before changing Firestore paths, mappings, IDs, money,
// dates, deletes, receipt handling, authorization, RPCs, regions, or secrets.
// ============================================================================
'use strict';

const { initializeApp } = require('firebase-admin/app');
const { defineSecret } = require('firebase-functions/params');
const { getFirestore } = require('firebase-admin/firestore');
const { getStorage } = require('firebase-admin/storage');
const { resolvePayment, receiptObjectPath } = require('./fuel-payment');
const { inspectPaymentReceipt } = require('./payment-ocr');
const { HttpsError, onCall } = require('firebase-functions/v2/https');
const { onDocumentWritten } = require('firebase-functions/v2/firestore');
const crypto = require('crypto');

initializeApp();

const anthropicApiKey = defineSecret('ANTHROPIC_API_KEY');
// The service-role key is used only inside Cloud Functions. It must be set with
// `firebase functions:secrets:set SUPABASE_SERVICE_ROLE_KEY`, never in either APK.
const supabaseUrl = defineSecret('SUPABASE_URL');
const supabaseServiceRoleKey = defineSecret('SUPABASE_SERVICE_ROLE_KEY');
const allowedMediaTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);
const requestWindows = new Map();
let lastRateLimitSweep = 0;

function enforceRateLimit(uid) {
  const now = Date.now();
  if (now - lastRateLimitSweep >= 60_000) {
    for (const [key, timestamps] of requestWindows) {
      if (!timestamps.some(time => now - time < 60_000)) requestWindows.delete(key);
    }
    lastRateLimitSweep = now;
  }
  const recent = (requestWindows.get(uid) || []).filter(time => now - time < 60_000);
  if (recent.length >= 10) throw new HttpsError('resource-exhausted', 'ลองใหม่อีกครั้งใน 1 นาที');
  recent.push(now);
  requestWindows.set(uid, recent);
}

// Claude sometimes reasons in prose before emitting the JSON block (e.g. a "sanity check"
// walkthrough of liters × price ≈ total), so the payload isn't always JSON from the first
// character — earlier versions of this stripped only the ```json fences and JSON.parse'd
// whatever prose was left in front, which threw and made a *correctly*-read receipt look
// like a parse failure (falling back to the much cruder on-device OCR guess client-side).
function extractJsonPayload(text) {
  const fenced = String(text).match(/```json\s*([\s\S]*?)```/i) || String(text).match(/```\s*([\s\S]*?)```/);
  if (fenced) return fenced[1].trim();
  const braced = String(text).match(/\{[\s\S]*\}/);
  if (braced) return braced[0];
  return String(text).trim();
}

function safeJson(text) {
  const parsed = JSON.parse(extractJsonPayload(text));
  const allowed = ['date', 'liters', 'pricePerLiter', 'total', 'station', 'title', 'amount', 'odometer'];
  return Object.fromEntries(allowed.filter(key => parsed[key] !== undefined).map(key => [key, parsed[key]]));
}

exports.scanReceipt = onCall({
  region: 'asia-southeast1',
  secrets: [anthropicApiKey],
  timeoutSeconds: 30,
  memory: '256MiB',
  maxInstances: 5,
  cors: true
}, async request => {
  try {
    return await handleScanReceipt(request);
  } catch (err) {
    // Guarantees a log line for every rejection path (rate limit, bad input, auth), not just
    // network failures — earlier versions of this function threw several of these silently,
    // which made "wrong scan result" reports look like the request never reached the server.
    console.warn('scanReceipt rejected', err?.code || 'unknown', err?.message || String(err));
    throw err;
  }
});

// ---------------------------------------------------------------------------
// Fuel Log -> PU Pocket bridge
// ---------------------------------------------------------------------------

const BRIDGE_REGION = 'asia-southeast1';
const IMPORT_SOURCE = 'fuel_log';
const MAX_RECEIPT_BYTES = 10 * 1024 * 1024;

function bridgeHeaders(contentType = 'application/json') {
  return {
    apikey: supabaseServiceRoleKey.value(),
    Authorization: `Bearer ${supabaseServiceRoleKey.value()}`,
    'Content-Type': contentType,
  };
}

async function supabase(path, options = {}) {
  const response = await fetch(`${supabaseUrl.value().replace(/\/$/, '')}${path}`, {
    ...options,
    headers: { ...bridgeHeaders(options.contentType), ...(options.headers || {}) },
  });
  const body = await response.text();
  if (!response.ok) throw new Error(`Supabase ${response.status}: ${body.slice(0, 400)}`);
  return body ? JSON.parse(body) : null;
}

function sha256(value) {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function receiptUrls(entry) {
  // Firestore already contains download URLs after Fuel Log's normal photo
  // upload pass. A local path is deliberately never copied out of the device.
  return String(entry.photoUri || '')
    .split(',')
    .map(value => value.trim())
    .filter(Boolean);
}

function transactionDate(entry) {
  const date = String(entry.date || '');
  const time = /^\d{2}:\d{2}$/.test(String(entry.time || '')) ? entry.time : '00:00';
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error('Fuel entry has no valid date');
  // Fuel Log dates are entered in Thailand, not in UTC.
  return `${date}T${time}:00+07:00`;
}

function entryNote(entry, vehicle) {
  const details = [
    vehicle?.name || vehicle?.registration || 'Fuel Log',
    entry.station,
    Number.isFinite(Number(entry.liters)) ? `${Number(entry.liters)} L` : null,
    Number.isFinite(Number(entry.price)) ? `฿${Number(entry.price)}/L` : null,
  ].filter(Boolean);
  return `เติมน้ำมัน • ${details.join(' • ')}`;
}

async function prepareEntry(vehicleId, entryId, entry, sourceUpdatedAt, ocrBudget = { remaining: 6 }) {
  const bucket = getStorage().bucket();
  const attachments = [];
  for (const url of receiptUrls(entry)) {
    try {
      const path = receiptObjectPath(url, bucket.name, vehicleId, entryId);
      const file = bucket.file(path);
      const [metadata] = await file.getMetadata();
      if (Number(metadata.size) > MAX_RECEIPT_BYTES) throw new Error('Image too large');
      const [bytes] = await file.download({ validation: 'crc32c' });
      if (bytes.length > MAX_RECEIPT_BYTES) throw new Error('Image too large');
      attachments.push({ bytes, contentType: metadata.contentType || 'image/jpeg' });
    } catch {
      // A failed/local/unauthorized attachment is not evidence of cash payment.
      attachments.push(null);
      console.warn('Fuel Log receipt unavailable', { entryId });
    }
  }
  const payment = await resolvePayment(entry, attachments, async attachment => {
    const fingerprint = sha256(`${vehicleId}/${entryId}/v2/${sha256(attachment.bytes)}`);
    const ref = getFirestore().collection('pupu_payment_receipt_cache').doc(fingerprint);
    const cached = await ref.get();
    if (cached.exists) return cached.data().evidence;
    // A link can contain years of history. Bound new paid OCR requests per
    // invocation; exhausted/unclear records remain pending, never guessed cash.
    if (ocrBudget.remaining <= 0) return null;
    ocrBudget.remaining--;
    const evidence = await inspectPaymentReceipt(attachment, anthropicApiKey.value());
    if (evidence) await ref.set({ evidence, createdAt: new Date() });
    return evidence;
  });
  return { attachments, payment, sourceUpdatedAt };
}

async function copyReceiptUrls(link, entryId, attachments) {
  const copied = [];
  for (let index = 0; index < attachments.length; index += 1) {
    const attachment = attachments[index];
    if (!attachment) continue;
    try {
      const { bytes, contentType } = attachment;
      if (!/^(image\/(jpeg|png|webp)|application\/pdf)$/i.test(contentType)) {
        throw new Error(`unsupported content type ${contentType}`);
      }
      const extension = contentType.includes('png') ? 'png' : contentType.includes('webp') ? 'webp' : contentType.includes('pdf') ? 'pdf' : 'jpg';
      const path = `fuel-log/${link.user_id}/${link.vehicle_id}/${entryId}/${index + 1}.${extension}`;
      const upload = await fetch(
        `${supabaseUrl.value().replace(/\/$/, '')}/storage/v1/object/pupu-receipts/${path}`,
        {
          method: 'POST',
          headers: { ...bridgeHeaders(contentType), 'x-upsert': 'true' },
          body: bytes,
        },
      );
      if (!upload.ok) throw new Error(`upload returned ${upload.status}: ${(await upload.text()).slice(0, 200)}`);
      // A signed URL is intentionally not persisted: it expires. Store the
      // private object path in metadata; PU Pocket resolves it when displaying.
      copied.push(path);
    } catch (error) {
      console.warn('Could not copy Fuel Log receipt', { entryId, index, error: error.message });
    }
  }
  return copied;
}

async function upsertEntryForLink(link, vehicle, entryId, entry, prepared) {
  const receiptPaths = await copyReceiptUrls(link, entryId, prepared.attachments);
  const metadata = {
    vehicleId: link.vehicle_id,
    vehicleName: vehicle?.name || null,
    registration: vehicle?.registration || null,
    station: entry.station || null,
    liters: Number(entry.liters || 0),
    pricePerLiter: Number(entry.price || 0),
    odometerKm: Number(entry.odometer || 0),
    fullTank: Boolean(entry.full),
    receiptPaths,
    payment: prepared.payment,
    sourceUpdatedAt: prepared.sourceUpdatedAt,
    importedAt: new Date().toISOString(),
  };
  await supabase('/rest/v1/rpc/upsert_fuel_log_transaction', {
    method: 'POST',
    body: JSON.stringify({
      p_link_id: link.id,
      p_source_id: entryId,
      p_amount_minor: Math.round(Number(entry.total || 0) * 100),
      p_transaction_date: transactionDate(entry),
      p_note: entryNote(entry, vehicle),
      p_receipt_url: receiptPaths[0] || null,
      p_metadata: metadata,
      p_deleted: false,
    }),
  });
}

async function activeLinksForVehicle(vehicleId) {
  return supabase(`/rest/v1/fuel_log_import_links?select=*&vehicle_id=eq.${encodeURIComponent(vehicleId)}&active=eq.true`);
}

async function assertVehicleMember(uid, vehicleId) {
  const vehicle = (await getFirestore().collection('vehicles').doc(vehicleId).get()).data();
  const member = vehicle?.members?.[uid];
  const isListedMember = Array.isArray(vehicle?.memberUids) && vehicle.memberUids.includes(uid);
  if (!vehicle || (vehicle.ownerUid !== uid && !member && !isListedMember)) {
    throw new HttpsError('permission-denied', 'You do not have access to this vehicle');
  }
}

async function syncEntry(vehicleId, entryId, eventTime) {
  const links = await activeLinksForVehicle(vehicleId);
  if (!links.length) return;
  const vehicle = (await getFirestore().collection('vehicles').doc(vehicleId).get()).data() || null;
  // Re-read the authoritative record: late event delivery must not replay an old
  // payment choice over a newer edit. The RPC also compares sourceUpdatedAt.
  const live = await getFirestore().collection('vehicles').doc(vehicleId).collection('entries').doc(entryId).get();
  const after = live.exists ? live.data() : null;
  if (after) {
    const prepared = await prepareEntry(vehicleId, entryId, after, live.updateTime.toDate().toISOString());
    await Promise.all(links.map(link => upsertEntryForLink(link, vehicle, entryId, after, prepared)));
  } else {
    await Promise.all(links.map(link => supabase('/rest/v1/rpc/upsert_fuel_log_transaction', {
      method: 'POST',
      body: JSON.stringify({
        p_link_id: link.id,
        p_source_id: entryId,
        p_amount_minor: 0,
        p_transaction_date: new Date().toISOString(),
        p_note: null,
        p_receipt_url: null,
        p_metadata: { vehicleId, sourceUpdatedAt: eventTime, deletedFromFuelLogAt: new Date().toISOString() },
        p_deleted: true,
      }),
    })));
  }
}

exports.syncFuelEntryToPupu = onDocumentWritten({
  region: BRIDGE_REGION,
  document: 'vehicles/{vehicleId}/entries/{entryId}',
  secrets: [supabaseUrl, supabaseServiceRoleKey, anthropicApiKey],
  timeoutSeconds: 300,
  maxInstances: 5,
}, async event => {
  await syncEntry(event.params.vehicleId, event.params.entryId, event.time);
});

exports.redeemPupuLink = onCall({
  region: BRIDGE_REGION,
  secrets: [supabaseUrl, supabaseServiceRoleKey, anthropicApiKey],
  timeoutSeconds: 540,
}, async request => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign in to Fuel Log first');
  const code = String(request.data?.code || '').trim().toUpperCase();
  const vehicleIds = [...new Set(asArray(request.data?.vehicleIds).filter(value => typeof value === 'string' && value))];
  if (!/^[A-Z2-9]{10}$/.test(code)) throw new HttpsError('invalid-argument', 'Invalid PU Pocket link code');
  if (!vehicleIds.length) throw new HttpsError('invalid-argument', 'Select at least one vehicle');
  await Promise.all(vehicleIds.map(vehicleId => assertVehicleMember(request.auth.uid, vehicleId)));

  const links = await supabase('/rest/v1/rpc/redeem_fuel_log_link', {
    method: 'POST',
    body: JSON.stringify({ p_code_hash: sha256(code), p_firebase_uid: request.auth.uid, p_vehicle_ids: vehicleIds }),
  });
  // Import existing fill-ups immediately. New/updated items continue through
  // the Firestore trigger above.
  const ocrBudget = { remaining: 10 };
  for (const link of links) {
    const vehicle = (await getFirestore().collection('vehicles').doc(link.vehicle_id).get()).data() || null;
    const entries = await getFirestore().collection('vehicles').doc(link.vehicle_id).collection('entries').get();
    for (const entry of entries.docs) {
      const prepared = await prepareEntry(link.vehicle_id, entry.id, entry.data(), entry.updateTime.toDate().toISOString(), ocrBudget);
      await upsertEntryForLink(link, vehicle, entry.id, entry.data(), prepared);
    }
  }
  return { importedVehicles: links.map(link => link.vehicle_id) };
});

async function handleScanReceipt(request) {
  if (!request.auth) throw new HttpsError('unauthenticated', 'กรุณาเข้าสู่ระบบก่อนใช้ OCR');
  enforceRateLimit(request.auth.uid);

  const { imageBase64, mediaType, type } = request.data || {};
  if (!allowedMediaTypes.has(mediaType)) throw new HttpsError('invalid-argument', 'ชนิดรูปไม่รองรับ');
  if (!['fuel', 'expense', 'odometer'].includes(type)) throw new HttpsError('invalid-argument', 'ประเภทเอกสารไม่ถูกต้อง');
  if (typeof imageBase64 !== 'string' || imageBase64.length < 100 || imageBase64.length > 7_000_000) {
    throw new HttpsError('invalid-argument', 'รูปมีขนาดไม่ถูกต้อง');
  }

  const isFuel = type === 'fuel';
  const isOdometer = type === 'odometer';
  const schema = isOdometer
    ? '{"odometer":number|null}'
    : isFuel
      ? '{"date":"YYYY-MM-DD or null","liters":number|null,"pricePerLiter":number|null,"total":number|null,"station":string|null}'
      : '{"date":"YYYY-MM-DD or null","title":string|null,"amount":number|null}';
  const prompt = isOdometer
    ? `Read the odometer (total distance / mileage) display on this car instrument cluster photo. Return JSON only using ${schema}. Ignore speed, RPM, fuel gauge, clock, and warning lights — only the cumulative odometer reading. If there's a smaller trip/tenths digit shown in a different color or box, include it as a decimal. Use null if no odometer digits are legible.`
    : `Extract this ${isFuel ? 'fuel ' : ''}receipt. Return JSON only using ${schema}. Use null when unreadable, convert Buddhist years to Gregorian${isFuel ? ', and normalize fuel volume to liters and unit price to price per liter' : ''}.` +
      (isFuel
        ? ' Thai fuel receipts always use "." as the decimal point, never as a thousands separator — e.g. "36.500L" means 36.5 liters and "36.71" is 36.71 baht per liter, not thirty-six thousand of anything. The product line is often one compact line like "<fuel name> <liters>L,<currency symbol><price per liter>" (e.g. "GASOHOL 95/36.500L,฿36.71"); the grand total is a separate line usually labeled "Total" or "รวม" near the bottom, and is typically in the hundreds to low thousands of baht for a single fill-up — sanity-check that "total" is not accidentally the liters or price-per-liter value with extra zeros appended. Before answering, verify liters × pricePerLiter ≈ total (within normal rounding); if your three readings don\'t roughly agree, re-read the receipt image rather than reporting numbers that don\'t reconcile, and set a field to null instead of guessing if you cannot read it with confidence — a missing value the user fills in by hand is far less harmful than a wrong one.'
        : '');
  let response;
  let rawBody;
  try {
    response = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-api-key': anthropicApiKey.value(),
        'anthropic-version': '2023-06-01'
      },
      body: JSON.stringify({
        model: process.env.CLAUDE_OCR_MODEL || 'claude-sonnet-4-6',
        max_tokens: 600,
        messages: [{
          role: 'user',
          content: [
            { type: 'image', source: { type: 'base64', media_type: mediaType, data: imageBase64 } },
            { type: 'text', text: prompt }
          ]
        }]
      })
    });
    rawBody = await response.text();
  } catch (err) {
    // fetch()/response.text() can throw (network error, hang past the client's own timeout,
    // connection reset) — without this catch, the request dies here with no log line at all,
    // which is what made earlier "wrong scan result" reports look like silent no-ops.
    console.error('Anthropic OCR network error', err?.message || String(err));
    throw new HttpsError('internal', 'บริการ OCR ไม่พร้อมใช้งาน (เครือข่าย)');
  }
  let result;
  try {
    result = JSON.parse(rawBody);
  } catch {
    // A non-JSON body (e.g. an upstream gateway/HTML error page) would otherwise throw here
    // uncaught, again with no log line pointing at the cause.
    console.error('Anthropic OCR non-JSON response', response.status, rawBody?.slice(0, 500));
    throw new HttpsError('internal', 'บริการ OCR ไม่พร้อมใช้งาน (รูปแบบข้อมูล)');
  }
  if (!response.ok) {
    console.error('Anthropic OCR failed', response.status, result?.error?.type);
    throw new HttpsError('internal', 'บริการ OCR ไม่พร้อมใช้งาน');
  }
  const text = (result.content || []).find(block => block.type === 'text')?.text;
  if (!text) throw new HttpsError('data-loss', 'OCR ไม่ส่งข้อมูลกลับมา');
  try {
    return safeJson(text);
  } catch {
    throw new HttpsError('data-loss', 'OCR ส่งรูปแบบข้อมูลไม่ถูกต้อง');
  }
}
