'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { explicitPayment, resolvePayment, receiptObjectPath, validateReceiptEvidence } = require('../fuel-payment');
const { inspectPaymentReceipt } = require('../payment-ocr');
const photo = { bytes: Buffer.from('synthetic-image'), contentType: 'image/jpeg' };
const receipt = (method = 'CREDIT_CARD', provider = 'firstchoice') => ({ hasReceipt: true, method, provider, confidence: 'high' });

for (const [label, method] of [['เงินสด','CASH'], ['cash','CASH'], ['บัตรเครดิต','CREDIT_CARD'],
  ['บัตรเครดิตกสิกรไทย','CREDIT_CARD'], ['FirstChoice','CREDIT_CARD'], ['ธนาคารกสิกรไทย','BANK'],
  ['PromptPay','BANK'], ['TrueMoney','E_WALLET'], ['My custom account','UNKNOWN']]) {
  test(`explicit method: ${label}`, () => assert.equal(explicitPayment(label).method, method));
}
test('explicit selection wins even over unavailable attachments without calling OCR', async () => {
  const result = await resolvePayment({ paymentMethod: 'เงินสด' }, [null], () => assert.fail('OCR must not run'));
  assert.equal(result.source, 'fuel_log'); assert.equal(result.method, 'CASH');
});
test('missing field and no attachments means cash', async () => {
  assert.equal((await resolvePayment({}, [], () => assert.fail())).source, 'no_receipt');
});
test('unknown placeholder inspects receipts', async () => {
  assert.equal((await resolvePayment({ paymentMethod: 'ไม่ระบุ' }, [photo], async () => receipt())).source, 'receipt');
});
test('all non-receipt photos means cash', async () => {
  const result = await resolvePayment({}, [photo, photo], async () => ({ hasReceipt: false, confidence: 'high' }));
  assert.equal(result.method, 'CASH'); assert.equal(result.source, 'no_receipt');
});
test('one receipt and one odometer uses receipt', async () => {
  let count = 0;
  const result = await resolvePayment({}, [photo, photo], async () => count++ ? receipt() : { hasReceipt: false, confidence: 'high' });
  assert.equal(result.method, 'CREDIT_CARD'); assert.equal(result.label, 'FirstChoice');
});
test('conflicting receipts are held for review', async () => {
  let count = 0;
  assert.equal((await resolvePayment({}, [photo, photo], async () => count++ ? receipt('CASH', null) : receipt())).reason, 'receipt_conflict');
});
for (const [label, images, inspect] of [
  ['download failed',[null],async () => receipt()],
  ['OCR failed',[photo],async () => { throw new Error('offline'); }],
  ['unreadable',[photo],async () => ({ hasReceipt: true, confidence: 'low' })],
  ['unsupported PDF',[{ ...photo, contentType: 'application/pdf' }],async () => receipt()],
  ['too many photos',Array(7).fill(photo),async () => receipt()],
]) test(`${label} never defaults to cash`, async () => {
  assert.equal((await resolvePayment({}, images, inspect)).source, 'unresolved');
});
test('model output is restricted to known fields and providers', () => {
  assert.equal(validateReceiptEvidence({ ...receipt(), provider: 'account number 123456' }), null);
  assert.deepEqual(validateReceiptEvidence({ ...receipt(), rawText: 'private' }), receipt());
});
test('receipt loader allows only the exact bucket and entry object namespace', () => {
  const path = 'vehicles/car/photos/entry/receipt.jpg';
  const url = `https://firebasestorage.googleapis.com/v0/b/test-bucket/o/${encodeURIComponent(path)}?alt=media&token=ignored`;
  assert.equal(receiptObjectPath(url, 'test-bucket', 'car', 'entry'), path);
  for (const bad of [url.replace('test-bucket', 'other'), url.replace('car%2F', 'other%2F'),
    url.replace('firebasestorage.googleapis.com', 'localhost'), url.replace('https:', 'http:')]) {
    assert.throws(() => receiptObjectPath(bad, 'test-bucket', 'car', 'entry'));
  }
});
test('OCR sends bounded image evidence, rejects errors and strips unexpected fields', async () => {
  const result = await inspectPaymentReceipt(photo, 'synthetic-key', async (url, options) => {
    assert.equal(url, 'https://api.anthropic.com/v1/messages');
    const body = JSON.parse(options.body);
    assert.equal(body.max_tokens, 180);
    assert.match(body.messages[0].content[1].text, /merchant receiving bank/);
    return { ok: true, json: async () => ({ content: [{ type: 'text', text: JSON.stringify({ ...receipt(), secret: 'discard' }) }] }) };
  });
  assert.deepEqual(result, receipt());
  await assert.rejects(inspectPaymentReceipt(photo, 'synthetic', async () => ({ ok: false, status: 503 })), /503/);
});
