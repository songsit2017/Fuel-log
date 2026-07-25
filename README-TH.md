# FuelLog Pro v6.1 Secure Family

## สิ่งที่แก้ในรุ่นนี้
- Firestore Rules จำกัดข้อมูลตามสมาชิกของรถจริง
- Storage Rules จำกัดรูป/เอกสารตามสมาชิกของรถจริง
- Owner: จัดการรถ สมาชิก และลบข้อมูลได้
- Editor: เพิ่ม/แก้ข้อมูลและรูปได้; ลบได้เฉพาะไฟล์ที่ตนเองอัปโหลด
- Viewer: ดูข้อมูลและรูปได้อย่างเดียว
- แก้ระบบรหัสเชิญให้ทำงานร่วมกับ Security Rules แบบปลอดภัย
- สมาชิกต้องเปิดแอปและล็อกอิน Google อย่างน้อย 1 ครั้งก่อน เจ้าของจึงค้นหา Gmail และสร้างรหัสเชิญได้

## ขั้นตอนติดตั้ง
1. สำรองข้อมูลเดิมด้วย Export JSON
2. อัปโหลดทุกไฟล์และทุกโฟลเดอร์ขึ้น root ของ GitHub Pages
3. Firebase Console → Firestore → Rules
   - Ctrl+A ลบทั้งหมด
   - คัดลอกเนื้อหา `firestore.rules` ไปวาง
   - กด Publish
4. Firebase Console → Storage → Rules
   - Ctrl+A ลบทั้งหมด
   - คัดลอกเนื้อหา `storage.rules` ไปวาง
   - กด Publish
5. Authentication → Settings → Authorized domains เพิ่ม `songsit2017.github.io`
6. ปิด PWA และเปิดใหม่ หรือถอนติดตั้งแล้วติดตั้งใหม่ เพื่อรับ Service Worker รุ่นใหม่

## วิธีแชร์รถ
1. สมาชิกทุกคนเปิดแอปและล็อกอิน Google อย่างน้อย 1 ครั้ง
2. Owner เปิดเมนู แชร์ → นำรถขึ้น Cloud
3. ใส่ Gmail สมาชิก เลือก Editor หรือ Viewer แล้วสร้างรหัสเชิญ
4. สมาชิกล็อกอินด้วย Gmail เดียวกัน แล้วกรอกรหัสเชิญ

## ความปลอดภัย
- ผู้ที่ไม่ใช่สมาชิกของรถจะอ่าน Firestore และไฟล์ Storage ไม่ได้
- Firebase Config ในเว็บไม่ใช่รหัสลับ สิทธิ์จริงควบคุมด้วย Authentication + Rules
- ห้ามนำ Service Account JSON, Private Key หรือ API Secret ขึ้น GitHub
- แนะนำเปิด App Check หลังระบบหลักทดสอบผ่านแล้ว
