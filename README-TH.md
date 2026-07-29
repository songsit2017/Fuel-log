# FuelLog Pro

แอป Android native Kotlin (Jetpack Compose) — package `com.songsit.fuellogpro`
Firebase project: `fuellog-pro-f2a12`

โค้ดแอปทั้งหมดอยู่ใน [`native-kotlin/`](native-kotlin/README.md) ส่วน backend
(Cloud Functions สำหรับ OCR ใบเสร็จด้วย Claude) อยู่ใน [`functions/`](functions/index.js)

## รวมฟังก์ชัน
- Google Login ผ่าน Firebase
- Family Owner / Editor / Viewer และรหัสเชิญ
- Import Fuelio (`.fuelio`/`.zip`) หลายรถ ค่าใช้จ่าย และรูปภาพ
- กล้องหรือแกลเลอรี่สำหรับรูปใบเสร็จและเรือนไมล์ พร้อมสแกนบิลด้วย Claude OCR
- ราคาน้ำมันจาก `oil-prices.json` (อัปเดตอัตโนมัติผ่าน GitHub Actions)
- ปั๊มใกล้ฉัน, สถิติ/รายงาน, ทริปพร้อมบันทึก GPS

## ตั้งค่า Claude OCR

1. ติดตั้ง Firebase CLI และล็อกอินโปรเจกต์ production
2. รัน `firebase functions:secrets:set ANTHROPIC_API_KEY`
3. รัน `firebase deploy --only functions:scanReceipt --project fuellog-pro-f2a12`
4. ผู้ใช้ต้อง Google Login ก่อนใช้ Claude OCR; หาก backend ไม่พร้อม แอปจะ fallback ไป OCR บนเครื่อง

API key อยู่ใน Firebase Secret Manager เท่านั้น ไม่อยู่ในโค้ดแอปหรือไฟล์ export

## Build

APK preview build ผ่าน GitHub Actions (`.github/workflows/build-native-preview.yml`)
เมื่อ push ที่แตะ `native-kotlin/**` — ไม่ build/test บนเครื่อง local
