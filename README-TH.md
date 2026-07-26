# FuelLog Pro v6 Main Complete

**Target branch:** `main`  
**Base:** `Fuel-log-5.03.zip`

ชุดนี้เป็นไฟล์เต็มสำหรับแทนที่โปรเจกต์เดิมทั้งชุด ไม่ใช่ Patch

## รวมฟังก์ชัน
- Google Login ผ่าน Firebase
- Family Owner / Editor / Viewer และรหัสเชิญ
- สถานะ Join และ Sync แสดงในหน้าต่าง
- Import `.fuelio/.zip` หลายรถ ค่าใช้จ่าย และรูปภาพ
- กล้องหรือ Gallery สำหรับรูปใบเสร็จและเรือนไมล์
- OCR สแกนบิลในหน้าเติมน้ำมันและค่าใช้จ่าย
- ราคาน้ำมันจาก `oil-prices.json` พร้อม API และ cache สำรอง
- ปั๊มใกล้ฉัน
- รถหลายคัน รายงาน ทริป Gallery และ PWA
- ส่วนลดการเติมน้ำมันแบบ Fuelio

## การคิดเงินเติมน้ำมัน
- `grossTotal` = ยอดก่อนส่วนลด
- `discount` = ส่วนลด
- `total` = ยอดสุทธิจริง
- รายงานและบาท/กม. ใช้ `total`

## วิธีติดตั้งแบบสะอาด
1. Checkout branch `main`
2. สำรอง Repository ก่อน
3. ลบทุกไฟล์ใน Repository ยกเว้นโฟลเดอร์ `.git`
4. คัดลอกไฟล์ทั้งหมดภายในชุดนี้ไปไว้ที่ราก Repository
5. Commit และ Push
6. ไม่ต้องเปลี่ยน Firebase Rules ถ้า Rules 5.0.4 ที่ Join ได้ถูก Publish อยู่แล้ว
7. เปิดครั้งแรกด้วย `https://songsit2017.github.io/Fuel-log/?v=600`

## การตรวจที่ทำแล้ว
- `node --check app.js`
- ตรวจ path หลักจาก `index.html`
- ตรวจว่าไฟล์ GitHub Action, oil-prices.json, Firebase config, Rules และ PWA อยู่ครบ

OCR และ Firebase ต้องทดสอบบนเว็บจริง เพราะต้องใช้ CDN, สิทธิ์บัญชี, กล้อง และเครือข่ายภายนอก
