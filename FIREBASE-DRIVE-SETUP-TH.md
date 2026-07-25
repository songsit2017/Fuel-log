# ตั้งค่า FuelLog Pro v5 Family Drive Edition

## ระบบที่ใช้
- GitHub Pages: หน้าแอป
- Firebase Authentication: Login Google
- Cloud Firestore: ข้อมูลที่แชร์ในครอบครัว
- Google Drive: รูปใบเสร็จ รูปเรือนไมล์ และเอกสาร

ไม่ต้องเปิด Firebase Storage และไม่ต้องใช้แผน Blaze

## 1. Firebase
1. เปิด Authentication > Sign-in method > Google
2. เพิ่ม `songsit2017.github.io` ใน Authorized domains
3. เปิด Firestore แบบ Production
4. นำเนื้อหา `firestore.rules` ไปวางใน Firestore > Rules แล้ว Publish
5. ค่า Firebase ของโปรเจกต์ `fuellog-pro-f2a12` ถูกใส่ใน `firebase-config.js` แล้ว

## 2. Google OAuth สำหรับ Drive
แอปใช้ Client ID เดิมจากหน้า Settings ของ FuelLog

ใน Google Cloud Console:
1. APIs & Services > Library > เปิด Google Drive API
2. OAuth consent screen: เพิ่ม Gmail สมาชิกเป็น Test users หากแอปยังอยู่ Testing
3. Credentials > OAuth 2.0 Client IDs > Web application
4. Authorized JavaScript origins เพิ่ม `https://songsit2017.github.io`
5. นำ Client ID ใส่ในแอป Settings > Google OAuth Client ID

## 3. วิธีแชร์รถและรูป
1. เจ้าของ Login Firebase และเชื่อมต่อ Google Drive
2. เปิด Settings > แชร์ยานพาหนะกับครอบครัว
3. กดนำรถขึ้น Cloud
4. ใส่ Gmail สมาชิก แล้วกดสร้างรหัสและแชร์ Drive
5. ระบบสร้างโฟลเดอร์ส่วนตัว `FuelLog Family/<ชื่อรถ>` และแชร์ให้ Gmail นั้น
6. สมาชิก Login ด้วย Gmail เดียวกัน ใส่รหัสเชิญ และเชื่อมต่อ Drive

## 4. ความเป็นส่วนตัว
- รูปไม่อยู่ใน GitHub
- รูปไม่เป็น Public URL
- เฉพาะบัญชี Google ที่ได้รับสิทธิ์ในโฟลเดอร์จึงเปิดได้
- ห้ามใส่ Client Secret, Service Account JSON หรือ Private Key ลง GitHub
- Firebase Web config และ OAuth Client ID เปิดเผยในเว็บได้ แต่ Security Rules ต้องถูกต้อง

## 5. ข้อจำกัด
Google Drive access token มีอายุจำกัด สมาชิกอาจต้องกดเชื่อมต่อ Drive ใหม่เมื่อ token หมดอายุ แต่ Firebase Login จะจำบัญชีไว้
