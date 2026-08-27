'use strict';

const METHODS = new Set(['CASH', 'BANK', 'CREDIT_CARD', 'E_WALLET']);
const PROVIDERS = Object.freeze({
  kbank: 'กสิกรไทย', scb: 'ไทยพาณิชย์', ktb: 'กรุงไทย', bbl: 'กรุงเทพ',
  bay: 'กรุงศรี', ttb: 'ttb', gsb: 'ออมสิน', baac: 'ธ.ก.ส.',
  ktc: 'KTC', firstchoice: 'FirstChoice', amex: 'American Express',
  truemoney: 'TrueMoney', shopeepay: 'ShopeePay', spaylater: 'SPayLater',
});

function payment(method, label, source, reason) {
  return { version: 1, method, label, source, ...(reason ? { reason } : {}) };
}

function explicitPayment(value) {
  const label = typeof value === 'string' ? value.trim().slice(0, 120) : '';
  const key = label.toLowerCase().replace(/[\s._()-]/g, '');
  if (!key || ['ไม่ระบุ', 'อื่นๆ', 'อื่น', 'unknown', 'other', '-'].includes(key)) return null;
  if (['เงินสด', 'cash'].includes(key)) return payment('CASH', null, 'fuel_log');
  if (['บัตรเครดิต', 'creditcard'].includes(key)) return payment('CREDIT_CARD', null, 'fuel_log');
  if (['โอน', 'โอนเงิน', 'พร้อมเพย์', 'promptpay', 'banktransfer', 'debitcard', 'บัตรเดบิต'].includes(key)) {
    return payment('BANK', null, 'fuel_log');
  }
  const method = /credit|เครดิต|firstchoice|เฟิร์สช้อยส์|ktc|americanexpress|amex|spaylater|บัตรกดเงินสด/i.test(key)
    ? 'CREDIT_CARD'
    : /wallet|truemoney|shopeepay|วอลเล็ท|วอลเล็ต|ทรูมันนี่/i.test(key) ? 'E_WALLET'
      : /ธนาคาร|กสิกร|ไทยพาณิชย์|กรุงไทย|กรุงเทพ|กรุงศรี|ออมสิน|ธกส|ttb|kbank|kasikorn|scb|bangkokbank|krungthai|krungsri|debit|เดบิต/i.test(key)
        ? 'BANK' : 'UNKNOWN';
  // Unknown custom names can still match one exact, active account in the RPC.
  return payment(method, label, 'fuel_log');
}

function validateReceiptEvidence(value) {
  if (!value || value.confidence !== 'high' || typeof value.hasReceipt !== 'boolean') return null;
  if (!value.hasReceipt) return { hasReceipt: false, confidence: 'high' };
  if (!METHODS.has(value.method)) return null;
  if (value.provider != null && !Object.hasOwn(PROVIDERS, value.provider)) return null;
  return { hasReceipt: true, confidence: 'high', method: value.method, provider: value.provider || null };
}

async function resolvePayment(entry, attachments, inspect) {
  const explicit = explicitPayment(entry.paymentMethod);
  if (explicit) return explicit;
  if (!attachments.length) return payment('CASH', null, 'no_receipt');
  if (attachments.length > 6) return payment('UNKNOWN', null, 'unresolved', 'too_many_images');
  const evidence = [];
  for (const attachment of attachments) {
    if (!attachment || !/^image\/(jpeg|png|webp)$/.test(attachment.contentType) || attachment.bytes.length > 5 * 1024 * 1024) {
      return payment('UNKNOWN', null, 'unresolved', 'receipt_unavailable');
    }
    try {
      const result = validateReceiptEvidence(await inspect(attachment));
      if (!result) return payment('UNKNOWN', null, 'unresolved', 'receipt_unclear');
      if (result.hasReceipt) evidence.push(result);
    } catch {
      return payment('UNKNOWN', null, 'unresolved', 'receipt_unavailable');
    }
  }
  if (!evidence.length) return payment('CASH', null, 'no_receipt');
  const methods = new Set(evidence.map(item => item.method));
  const providers = new Set(evidence.map(item => item.provider).filter(Boolean));
  if (methods.size !== 1 || providers.size > 1) return payment('UNKNOWN', null, 'unresolved', 'receipt_conflict');
  const method = evidence[0].method;
  return payment(method, method === 'CASH' ? null : PROVIDERS[[...providers][0]] || null, 'receipt');
}

// Never fetch arbitrary Firestore-supplied URLs or trust their download tokens.
// Resolve only objects in this Firebase bucket, under this vehicle/entry path.
function receiptObjectPath(url, bucket, vehicleId, entryId) {
  const parsed = new URL(url);
  if (parsed.protocol !== 'https:' || parsed.hostname !== 'firebasestorage.googleapis.com' || parsed.port || parsed.username || parsed.password) {
    throw new Error('Untrusted receipt host');
  }
  const prefix = `/v0/b/${bucket}/o/`;
  if (!parsed.pathname.startsWith(prefix)) throw new Error('Untrusted receipt bucket');
  const path = decodeURIComponent(parsed.pathname.slice(prefix.length));
  if (!path.startsWith(`vehicles/${vehicleId}/photos/${entryId}/`) || path.split('/').some(part => !part || part === '.' || part === '..')) {
    throw new Error('Receipt does not belong to entry');
  }
  return path;
}

module.exports = { explicitPayment, resolvePayment, validateReceiptEvidence, receiptObjectPath, PROVIDERS };
