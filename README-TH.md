# FuelLog Pro v5.1.1 Stable

รุ่นนี้ใช้ฐาน Full Restore v5.1 เดิมและแก้เฉพาะ Firebase API key ให้ตรงกับรุ่น v5.0.1 ที่ยืนยันแล้วว่าเข้าสู่ระบบได้ เพื่อไม่ให้ฟังก์ชันเดิมหรือระบบนำเข้า Fuelio หายไป

## ติดตั้ง
ลบไฟล์เดิมทั้งหมดใน branch firebase-dev ยกเว้นโฟลเดอร์ .git แล้ววางไฟล์ชุดนี้แทน จากนั้น Commit และ Push

ไม่ต้องแก้ Firestore Rules หรือ Storage Rules เพิ่ม หาก Rules เดิมใช้งานได้อยู่แล้ว
