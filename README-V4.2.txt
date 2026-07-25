FuelLog Pro v4.2 — Firebase + Cloudinary Family Photos

เพิ่ม/เปลี่ยนจาก v4.1:
- ไม่ต้องเปิด Firebase Storage หรืออัปเกรด Blaze
- แชร์รูปใบเสร็จและรูปเรือนไมล์ผ่าน Cloudinary Unsigned Upload
- Firestore เก็บ URL รูป และสมาชิกดาวน์โหลดรูปมาเก็บ IndexedDB อัตโนมัติ
- ยังคง Family Sharing, Owner/Editor/Viewer และซิงก์แบบเรียลไทม์
- เพิ่ม cloudinary-config.js และคู่มือ FIREBASE-CLOUDINARY-SETUP-TH.md

คำเตือน:
Cloudinary Unsigned Upload บนเว็บ Static ไม่ใช่ Private Storage ผู้ที่มี URL โดยตรงอาจเปิดรูปได้ และการลบไฟล์จาก Cloudinary ต้องทำใน Media Library หรือใช้ Backend
