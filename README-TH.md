# FuelLog Pro v5.1 Full Feature Restore

เวอร์ชันนี้นำแอปเต็มเดิมกลับมาและแก้เฉพาะจุดที่มีปัญหา โดยไม่ลดฟังก์ชัน

## ฟังก์ชันหลัก
- รถหลายคัน, เติมน้ำมัน, ค่าใช้จ่าย, บำรุงรักษา, รายงาน, OCR, รูปใบเสร็จ/เรือนไมล์
- นำเข้า Fuelio `.fuelio` / `.zip` / `.csv` หลายคัน พร้อม `pictures.data`
- Firebase Google Login, Family Sharing, Firestore Sync และ Storage
- Google Drive Backup เดิม, ปั๊มใกล้ฉัน, ราคาน้ำมัน, PWA

## การติดตั้ง
ลบไฟล์เดิมใน branch `firebase-dev` ทั้งหมด ยกเว้น `.git` แล้ววางไฟล์ชุดนี้แทน จากนั้น Commit และ Push

Rules ไม่ต้อง Publish ซ้ำเมื่อ Rules ปัจจุบันใช้งานได้และไม่พบ `permission-denied`.
