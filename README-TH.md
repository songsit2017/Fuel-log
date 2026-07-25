# FuelLog Pro v5 Clean

ชุดนี้ใช้แทนไฟล์เก่าใน Branch `firebase-dev` ได้ทั้งโฟลเดอร์

## วิธีติดตั้ง
1. สำรอง Branch เดิมไว้ก่อน
2. ลบไฟล์และโฟลเดอร์เก่าทั้งหมดในโฟลเดอร์ Repository **ยกเว้น `.git`**
3. คัดลอกไฟล์ทั้งหมดจาก ZIP นี้ไปวาง
4. Commit และ Push ไป `firebase-dev`
5. คัดลอก `firestore.rules` ไป Firebase > Firestore > Rules แล้ว Publish
6. คัดลอก `storage.rules` ไป Firebase > Storage > Rules แล้ว Publish
7. เปิด `https://songsit2017.github.io/Fuel-log/diagnostics.html`

## ระบบ Google
Firebase Login เป็นบัญชีหลักระบบเดียวสำหรับแชร์รถ Firestore และ Storage
ไม่มี Cloudinary และไม่มี Google Drive Login ซ้ำในชุดนี้

## ข้อมูลเดิม
แอปจะย้ายข้อมูลจาก localStorage เดิม (`fuel-vehicles`, `fuel-entries`, `fuel-costs`, `fuel-reminders`) อัตโนมัติเมื่อเปิดครั้งแรก
รองรับนำเข้า JSON และไฟล์ `.fuelio/.zip` แบบ CSV/JSON ทั่วไป
