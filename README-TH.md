# FuelLog Pro V7 — main

**Target branch:** `main`  
**Base:** `Fuel-log-5.03.zip`

> V7 ใช้ `main` เป็นฐานและเป้าหมายเดียว

## ตั้งค่า Claude OCR ที่ปลอดภัย

1. ติดตั้ง Firebase CLI และล็อกอินโปรเจกต์ production
2. รัน `firebase functions:secrets:set ANTHROPIC_API_KEY`
3. รัน `firebase deploy --only functions:scanReceipt`
4. ผู้ใช้ต้อง Google Login ก่อนใช้ Claude OCR; หาก backend ไม่พร้อม แอปจะ fallback ไป Tesseract ในเครื่อง

API key จะอยู่ใน Firebase Secret เท่านั้น ไม่อยู่ใน `app.js`, localStorage หรือไฟล์ export

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


## v6.0.1
- เพิ่มปุ่มลบในหน้าแก้ไขรายการเติมน้ำมัน ค่าใช้จ่าย และรายการเตือน
- แยกปุ่มรูปเป็น “ถ่ายด้วยกล้อง” และ “เลือกจาก Gallery”
- ไม่เปลี่ยน Firebase Rules


## v6.1 Main + Android Ready
- ตัวเลือกรูปแบบ Bottom Sheet: กล้อง / แกลเลอรี / ยกเลิก
- โฟลเดอร์ `mobile/` สำหรับ Capacitor และ Android Studio
- เว็บไซต์บน GitHub Pages ยัง deploy จากไฟล์รากเหมือนเดิม


## v6.2
เพิ่มข้อมูลในรายการเติมน้ำมัน:
- ผู้ขับขี่
- วิธีการชำระเงิน
- เหตุผล/วัตถุประสงค์
- ทำเครื่องหมายว่าพลาดการบันทึกการเติมครั้งก่อน
- แนบรูปหรือ PDF เพิ่มเติม
- หมายเหตุ

ข้อมูลใหม่ถูกเก็บใน Local storage, Firebase Sync, JSON Export และ CSV Export
