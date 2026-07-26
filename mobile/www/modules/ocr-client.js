const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const ACCEPTED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

export async function imageToPayload(file) {
  if (!ACCEPTED_TYPES.has(file.type)) throw new Error('รองรับเฉพาะ JPEG, PNG หรือ WebP');
  if (file.size > MAX_IMAGE_BYTES) throw new Error('รูปต้องมีขนาดไม่เกิน 5 MB');
  const dataUrl = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(reader.error || new Error('อ่านรูปไม่สำเร็จ'));
    reader.readAsDataURL(file);
  });
  return { mediaType: file.type, imageBase64: String(dataUrl).split(',')[1] };
}

export async function scanWithSecureBackend(file, type, callable) {
  if (typeof callable !== 'function') throw new Error('กรุณาเข้าสู่ระบบเพื่อใช้ Claude OCR');
  const payload = await imageToPayload(file);
  const response = await callable({ ...payload, type });
  return response?.data || response;
}
