# FuelLog Pro v6

## ก่อนอัปโหลด
1. สำรองข้อมูลเดิมด้วย Export JSON
2. อัปโหลดทุกไฟล์และทุกโฟลเดอร์ขึ้น root ของ GitHub Pages
3. Firebase Console > Firestore > Rules: วาง `firestore.rules` แล้ว Publish
4. Firebase Console > Storage > Rules: วาง `storage.rules` แล้ว Publish
5. Authentication > Settings > Authorized domains: เพิ่ม `songsit2017.github.io`

## ฟังก์ชัน
- รถหลายคันและโปรไฟล์รถ
- เติมน้ำมัน พร้อมรูปใบเสร็จ/เรือนไมล์
- OCR ด้วย Tesseract.js (ไทย+อังกฤษ) และให้ตรวจข้อมูลก่อนบันทึก
- ค่าใช้จ่ายและบำรุงรักษา
- Trip/Jobs และต้นทุนต่อหน้างาน
- Family Sharing: Owner / Editor / Viewer
- Firestore realtime-ready + Firebase Storage private
- Gallery/เอกสารรถ
- Activity Log
- Dashboard และ Insight วิเคราะห์ในเครื่อง
- นำข้อมูลเดิมจาก localStorage มาใช้โดยอัตโนมัติ

## หมายเหตุ OCR
OCR ทำงานในเบราว์เซอร์ จึงไม่ส่งรูปไปบริการ AI ภายนอก แต่ครั้งแรกต้องดาวน์โหลดโมเดลภาษาไทย/อังกฤษจาก CDN และความแม่นยำขึ้นกับความคมชัดของภาพ
