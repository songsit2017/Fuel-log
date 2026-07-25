# ตั้งค่า FuelLog Pro v4.2: Firebase + Cloudinary

ระบบแบ่งหน้าที่ดังนี้:
- Firebase Authentication: เข้าสู่ระบบ Google
- Cloud Firestore: รถ สมาชิก รายการเติมน้ำมัน ค่าใช้จ่าย และงานบำรุงรักษา
- Cloudinary: รูปใบเสร็จและรูปเรือนไมล์

## ส่วน A — ใส่ Firebase Config

1. Firebase Console → Project settings → General
2. เลื่อนลงไปที่ Your apps และเลือก Web App `FuelLog`
3. ที่ SDK setup and configuration เลือก `Config`
4. เปิดไฟล์ `firebase-config.js`
5. คัดลอกค่าจาก Firebase ใส่ให้ครบทุกช่อง แล้วบันทึก

ตัวอย่างรูปแบบ:

```js
window.FUELLOG_FIREBASE_CONFIG = {
  apiKey: "...",
  authDomain: "fuellog-pro-f2a12.firebaseapp.com",
  projectId: "fuellog-pro-f2a12",
  storageBucket: "fuellog-pro-f2a12.firebasestorage.app",
  messagingSenderId: "...",
  appId: "..."
};
```

แม้ v4.2 ไม่ใช้ Firebase Storage ให้คง `storageBucket` ตาม Config ได้ตามปกติ

## ส่วน B — Authorized Domain

1. Firebase Console → Authentication → Settings
2. Authorized domains → Add domain
3. เพิ่ม `songsit2017.github.io`

## ส่วน C — Firestore Rules

1. Firebase Console → Firestore → Rules
2. ลบ Rules เดิม
3. คัดลอกทั้งหมดจากไฟล์ `firestore.rules`
4. กด Publish

ไม่ต้องสร้าง Collection เอง แอปจะสร้างเมื่อเจ้าของกดนำรถขึ้น Cloud

## ส่วน D — สร้าง Cloudinary ฟรี

1. สมัครหรือเข้าสู่ระบบ Cloudinary
2. หน้า Dashboard คัดลอกค่า `Cloud name`
3. ไปที่ Settings → Upload
4. เลื่อนหา Upload presets แล้วกด Add upload preset
5. ตั้ง Signing mode เป็น `Unsigned`
6. ตั้งชื่อ เช่น `fuellog_unsigned`
7. แนะนำให้ตั้ง:
   - Folder: `fuellog`
   - Allowed formats: `jpg,jpeg,png,webp`
   - Maximum file size: 5 MB
8. กด Save

## ส่วน E — ใส่ Cloudinary Config

เปิด `cloudinary-config.js` แล้วใส่:

```js
window.FUELLOG_CLOUDINARY_CONFIG = {
  cloudName: "ชื่อ Cloud name",
  uploadPreset: "fuellog_unsigned",
  folder: "fuellog"
};
```

ห้ามใส่ API Secret ลงในไฟล์เว็บเด็ดขาด

## ส่วน F — อัปโหลดขึ้น GitHub

อัปโหลดไฟล์ทั้งหมดใน ZIP ไปทับของเดิม โดยต้องมีไฟล์ใหม่:
- `cloudinary-config.js`
- `family-sharing.js`
- `firebase-config.js`
- `firestore.rules`
- `index.html`
- `sw.js`

หลัง Push ให้ปิด PWA แล้วเปิดใหม่ หรือเข้า Chrome → Site settings → Clear data เพื่อให้ Service Worker โหลด v4.2

## วิธีใช้งาน

1. ตั้งค่า → แชร์ยานพาหนะกับครอบครัว
2. เข้าสู่ระบบ Google
3. เจ้าของเลือกรถ → นำรถคันนี้ขึ้น Cloud
4. สร้างรหัสเชิญ แล้วส่งให้สมาชิก
5. สมาชิกล็อกอินและใส่รหัส
6. รายการและรูปที่บันทึกใหม่จะซิงก์ให้สมาชิก

## ข้อจำกัดด้านความเป็นส่วนตัวของ Cloudinary แบบฟรี

Unsigned Upload เหมาะกับเว็บ Static และไม่ต้องมี Backend แต่รูปเป็น URL บน CDN:
- URL มีความยาวและเดายาก แต่ผู้ที่ได้รับ URL โดยตรงอาจเปิดรูปได้
- Firestore จำกัดว่าใครเห็น URL ผ่านแอป แต่ไม่สามารถบังคับสิทธิ์หลัง URL หลุดออกไป
- การลบรูปเก่าจาก Cloudinary อย่างปลอดภัยต้องใช้ API Secret บน Backend ดังนั้น v4.2 จะลบลิงก์ออกจากรายการ แต่ไฟล์เก่าอาจยังค้างใน Cloudinary
- สามารถเข้า Cloudinary Media Library เพื่อลบไฟล์ค้างด้วยตนเอง

ถ้าต้องการรูป Private จริงและลบอัตโนมัติ ต้องใช้ Backend/Cloud Function หรืออัปเกรด Firebase Storage
