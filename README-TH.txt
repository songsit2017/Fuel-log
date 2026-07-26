FuelLog 5.0.4 Family Join Patch

ไฟล์นี้เป็น PATCH เท่านั้น ไม่ใช่โปรเจกต์ใหม่
ให้นำ 4 ไฟล์ไปทับไฟล์ชื่อเดียวกันที่ root ของ branch firebase-dev:
- app.js
- index.html
- sw.js
- firestore.rules

จากนั้น Commit + Push และนำ firestore.rules ไป Publish ที่ Firebase Console > Firestore Database > Rules หนึ่งครั้ง

สิ่งที่แก้:
1) สมาชิกไม่พยายามอ่าน vehicle ก่อนเข้าร่วมอีกต่อไป
2) ใช้ updateDoc แบบ dot-path เพิ่มเฉพาะ members.<uid> ไม่เขียนทับ members ทั้งก้อน
3) Rules อนุญาต Join เฉพาะอีเมล/รถ/บทบาท/วันหมดอายุที่ตรงกับ Invite
4) จำกัดการเปลี่ยนแปลงตอน Join ไว้เฉพาะ members ของ UID ตัวเอง, lastJoinCode และ updatedAt
5) bump cache เป็น 5.0.4 ป้องกัน app.js เก่าค้าง
