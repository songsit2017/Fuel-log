FuelLog Pro v4.0 Minimal Family OCR

ฐาน: v3.7 Stable + UI Minimal

เพิ่มในรอบนี้
- แก้โครงสร้าง index.html และ Export Excel ที่เคยมีสคริปต์แทรกกลางข้อความ
- Firebase Login แบบจำบัญชี
- Family Sharing Owner / Editor / Viewer
- รายชื่อสมาชิกแบบเรียลไทม์ และ Owner นำสมาชิกออกได้
- ซิงก์ข้อมูลเติมน้ำมัน ค่าใช้จ่าย งานบำรุง และทริป
- Firebase Storage พร้อม Gallery รูปใบเสร็จ เรือนไมล์ งานซ่อม และเอกสาร
- OCR ใบเสร็จและเลขไมล์ด้วย Tesseract.js (tha+eng)
- นำผล OCR ไปกรอกฟอร์มเติมน้ำมันเดิมได้
- Trip Log และต้นทุนต่อทริป
- Service Worker cache v4.0

วิธีติดตั้งใน Branch firebase-dev
1) แตก ZIP และคัดลอกทุกไฟล์ทับโฟลเดอร์ Repository
2) Commit to firebase-dev แล้ว Push origin
3) คัดลอก firestore.rules ไปวางใน Firestore Rules (ลบของเดิมทั้งหมดก่อน)
4) คัดลอก storage.rules ไปวางใน Storage Rules (ลบของเดิมทั้งหมดก่อน)
5) เปิด diagnostics.html ตรวจไฟล์

ข้อควรทราบ
- OCR โหลด Tesseract.js จาก CDN ครั้งแรก จึงต้องมีอินเทอร์เน็ต
- ควรตรวจตัวเลข OCR ก่อนบันทึกทุกครั้ง
- ยังไม่ควร Merge เข้า main จนกว่าจะทดสอบครบ
