# ตั้งค่า FuelLog Pro v4 Family Sharing

## 1) สร้าง Firebase Project
1. เปิด Firebase Console และสร้าง Project ใหม่
2. เพิ่ม Web App ชื่อ `FuelLog Pro`
3. คัดลอกค่า `firebaseConfig`
4. เปิดไฟล์ `firebase-config.js` แล้วแทนค่าช่องว่างทั้งหมด

## 2) เปิด Google Login
1. Firebase Console → Authentication → Get started
2. Sign-in method → Google → Enable
3. ใส่อีเมล Support แล้ว Save
4. Authentication → Settings → Authorized domains
5. เพิ่ม `songsit2017.github.io`

## 3) เปิด Firestore
1. Firestore Database → Create database
2. เลือก Production mode
3. เลือก Region ใกล้ไทย เช่น `asia-southeast1`
4. เปิดแท็บ Rules แล้วนำเนื้อหาจากไฟล์ `firestore.rules` ไปวาง จากนั้น Publish

## 4) อัปโหลดขึ้น GitHub
ต้องอัปโหลดไฟล์ใหม่เหล่านี้ด้วย:
- `firebase-config.js`
- `family-sharing.js`
- `firestore.rules` (เก็บเป็นสำเนา ไม่ถูกโหลดโดยหน้าเว็บ)
- ไฟล์ `index.html` และ `sw.js` ที่อัปเดตแล้ว

## วิธีใช้งาน
1. เจ้าของเปิด ตั้งค่า → แชร์ยานพาหนะกับครอบครัว
2. เข้าสู่ระบบ Google
3. เลือกรถแล้วกด “นำรถคันนี้ขึ้น Cloud”
4. กดสร้างรหัสเชิญแล้วส่งรหัส 8 ตัวให้สมาชิก
5. สมาชิกเปิดแอป → เข้าสู่ระบบ Google → กรอกรหัส → เข้าร่วม

## สิทธิ์
- Owner: จัดการและแชร์รถ รวมทั้งเพิ่ม/แก้ไข/ลบข้อมูล
- Editor: เพิ่ม/แก้ไข/ลบรายการเติมน้ำมัน ค่าใช้จ่าย และการเตือน
- Viewer: โครงสร้างรองรับ แต่ UI สำหรับเปลี่ยนสิทธิ์จะเพิ่มในเวอร์ชันถัดไป

## หมายเหตุ
- รูปใบเสร็จและรูปเรือนไมล์ยังเก็บอยู่ในเครื่องเดิม ไม่ได้อัปโหลด Firestore
- ข้อมูลตัวเลขและข้อความซิงก์แบบเรียลไทม์และใช้งานออฟไลน์ได้
- Google Drive เดิมยังใช้สำรองข้อมูลส่วนตัวได้ตามปกติ
