'use strict';

const { initializeApp } = require('firebase-admin/app');
const { defineSecret } = require('firebase-functions/params');
const { HttpsError, onCall } = require('firebase-functions/v2/https');

initializeApp();

const anthropicApiKey = defineSecret('ANTHROPIC_API_KEY');
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

function safeJson(text) {
  const parsed = JSON.parse(String(text).replace(/```json|```/gi, '').trim());
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
  const response = await fetch('https://api.anthropic.com/v1/messages', {
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
  const result = await response.json();
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
});
