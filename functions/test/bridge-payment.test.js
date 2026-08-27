'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const payment = require('../fuel-payment');

// Execute the actual trigger/callable wiring against in-memory Firebase and
// Supabase doubles. No credentials, network, production writes or paid OCR.
function bridge({ method, deleted = false, historyCount = 1, linkCount = 1, badPhoto = false } = {}) {
  const cache = new Map();
  const writes = [];
  const uploads = [];
  let ocrCalls = 0;
  const links = Array.from({ length: linkCount }, (_, i) => ({ id: `link-${i}`, user_id: `user-${i}`, vehicle_id: 'car' }));
  const entries = Array.from({ length: historyCount }, (_, i) => {
    const id = `entry-${i}`;
    const url = `https://firebasestorage.googleapis.com/v0/b/synthetic-bucket/o/${encodeURIComponent(`vehicles/car/photos/${id}/receipt.jpg`)}`;
    return { id, exists: !deleted, updateTime: { toDate: () => new Date('2026-08-27T11:00:00Z') },
      data: () => ({ id, date: '2026-08-27', time: '20:10', total: 2950, liters: 80, price: 36.875,
        photoUri: badPhoto ? 'https://untrusted.invalid/receipt' : url,
        ...(method === undefined ? {} : { paymentMethod: method }) }) };
  });
  const firestore = { collection(name) {
    if (name === 'pupu_payment_receipt_cache') return { doc: key => ({
      get: async () => ({ exists: cache.has(key), data: () => cache.get(key) }),
      set: async value => { cache.set(key, value); },
    }) };
    assert.equal(name, 'vehicles');
    return { doc: id => {
      assert.equal(id, 'car');
      return { get: async () => ({ data: () => ({ name: 'Synthetic car', ownerUid: 'firebase-user' }) }),
        collection: name => {
          assert.equal(name, 'entries');
          return { get: async () => ({ docs: entries }), doc: id => ({ get: async () => entries.find(entry => entry.id === id) }) };
        } };
    } };
  } };
  const mocks = {
    'firebase-admin/app': { initializeApp() {} },
    'firebase-functions/params': { defineSecret: name => ({ value: () => name === 'SUPABASE_URL' ? 'https://supabase.invalid' : 'synthetic-test-secret' }) },
    'firebase-admin/firestore': { getFirestore: () => firestore },
    'firebase-admin/storage': { getStorage: () => ({ bucket: () => ({ name: 'synthetic-bucket', file: path => ({
      getMetadata: async () => [{ size: '128', contentType: 'image/jpeg' }],
      download: async () => [Buffer.from(path)],
    }) }) }) },
    'firebase-functions/v2/https': { HttpsError: Error, onCall: (_, fn) => fn },
    'firebase-functions/v2/firestore': { onDocumentWritten: (_, fn) => fn },
    './fuel-payment': payment,
    './payment-ocr': { inspectPaymentReceipt: async () => {
      ocrCalls++;
      return { hasReceipt: true, method: 'CREDIT_CARD', provider: 'firstchoice', creditMarker: 'CREDIT', confidence: 'high' };
    } },
    crypto: require('node:crypto'),
  };
  const context = { exports: {}, Buffer, console: { warn() {}, error() {} },
    require: name => { assert.ok(mocks[name], `unexpected dependency ${name}`); return mocks[name]; },
    fetch: async (url, options) => {
      let result = null;
      if (url.includes('/storage/')) uploads.push(url);
      else if (url.includes('/upsert_fuel_log_transaction')) { writes.push(JSON.parse(options.body)); result = 'synthetic-transaction-id'; }
      else if (url.includes('/fuel_log_import_links?') || url.includes('/redeem_fuel_log_link')) result = links;
      else assert.fail(`Unexpected request ${url}`);
      return { ok: true, text: async () => JSON.stringify(result) };
    } };
  vm.runInNewContext(readFileSync(require.resolve('../index.js'), 'utf8'), context);
  return { writes, uploads, cache, ocrCalls: () => ocrCalls,
    sync: () => context.exports.syncFuelEntryToPupu({ params: { vehicleId: 'car', entryId: 'entry-0' }, time: '2026-08-27T10:00:00Z' }),
    redeem: auth => context.exports.redeemPupuLink({ auth, data: { code: 'ABCDEFGHJK', vehicleIds: ['car'] } }) };
}

test('actual trigger shares one receipt decision across links and caches repeated deliveries', async () => {
  const b = bridge({ linkCount: 2 });
  await b.sync(); await b.sync();
  assert.equal(b.ocrCalls(), 1);
  assert.equal(b.writes.length, 4);
  for (const row of b.writes) {
    assert.equal(row.p_source_id, 'entry-0');
    assert.equal(row.p_amount_minor, 295000);
    assert.equal(row.p_transaction_date, '2026-08-27T20:10:00+07:00');
    assert.equal(row.p_metadata.sourceUpdatedAt, '2026-08-27T11:00:00.000Z');
    assert.equal(row.p_metadata.payment.label, 'FirstChoice');
    assert.match(row.p_receipt_url, /^fuel-log\/user-[01]\/car\/entry-0\/1.jpg$/);
  }
  const stored = [...b.cache.values()][0];
  assert.deepEqual(Object.keys(stored).sort(), ['createdAt', 'evidence']);
});

test('explicit cash reaches the RPC without OCR even when its attachment is unavailable', async () => {
  const b = bridge({ method: 'เงินสด', badPhoto: true });
  await b.sync();
  assert.equal(b.ocrCalls(), 0);
  assert.equal(b.writes[0].p_metadata.payment.method, 'CASH');
  assert.equal(b.writes[0].p_metadata.payment.source, 'fuel_log');
});

test('untrusted attachment without a source method is unresolved, not downloaded or cash', async () => {
  const b = bridge({ badPhoto: true });
  await b.sync();
  assert.equal(b.ocrCalls(), 0);
  assert.equal(b.uploads.length, 0);
  assert.equal(b.writes[0].p_metadata.payment.source, 'unresolved');
});

test('authoritative missing document sends only a tombstone with event watermark', async () => {
  const b = bridge({ deleted: true });
  await b.sync();
  assert.equal(b.ocrCalls(), 0);
  assert.equal(b.uploads.length, 0);
  assert.equal(b.writes[0].p_deleted, true);
  assert.equal(b.writes[0].p_metadata.sourceUpdatedAt, '2026-08-27T10:00:00Z');
});

test('historical import bounds new OCR calls and keeps over-budget entries unresolved', async () => {
  const b = bridge({ historyCount: 12 });
  await b.redeem({ uid: 'firebase-user' });
  assert.equal(b.ocrCalls(), 10);
  assert.equal(b.writes.length, 12);
  assert.equal(b.writes.filter(row => row.p_metadata.payment.source === 'unresolved').length, 2);
});

test('pairing still requires authentication and vehicle membership before importing', async () => {
  const b = bridge();
  await assert.rejects(b.redeem(null));
  await assert.rejects(b.redeem({ uid: 'not-a-member' }));
  assert.equal(b.writes.length, 0);
  assert.equal(b.ocrCalls(), 0);
});
