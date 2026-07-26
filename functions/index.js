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
  const allowed = ['date', 'liters', 'pricePerLiter', 'total', 'station', 'title', 'amount'];
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
  if (!['fuel', 'expense'].includes(type)) throw new HttpsError('invalid-argument', 'ประเภทเอกสารไม่ถูกต้อง');
  if (typeof imageBase64 !== 'string' || imageBase64.length < 100 || imageBase64.length > 7_000_000) {
    throw new HttpsError('invalid-argument', 'รูปมีขนาดไม่ถูกต้อง');
  }

  const isFuel = type === 'fuel';
  const schema = isFuel
    ? '{"date":"YYYY-MM-DD or null","liters":number|null,"pricePerLiter":number|null,"total":number|null,"station":string|null}'
    : '{"date":"YYYY-MM-DD or null","title":string|null,"amount":number|null}';
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
          { type: 'text', text: `Extract this ${isFuel ? 'fuel ' : ''}receipt. Return JSON only using ${schema}. Use null when unreadable, convert Buddhist years to Gregorian${isFuel ? ', and normalize fuel volume to liters and unit price to price per liter' : ''}.` }
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
