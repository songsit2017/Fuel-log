FuelLog Pro v3.9 Minimal Family RC

ใช้สำหรับ Branch: firebase-dev เท่านั้น
อย่า Merge เข้า main จนกว่าจะทดสอบครบ

สิ่งที่ปรับจาก v3.8
- เปลี่ยน Service Worker cache เป็น v3.9
- เพิ่มไฟล์ CSS/JS/addons ลง Offline Cache
- เพิ่ม diagnostics.html สำหรับตรวจไฟล์หาย/Path ผิด
- ปรับชื่อเวอร์ชันในหน้าเว็บและ Manifest
- คงฟังก์ชันเดิมของ v3.7 และโมดูล Family/Trip/Storage

หลัง Copy ไฟล์และ Push:
1) เปิด https://songsit2017.github.io/Fuel-log/diagnostics.html
2) ทุกไฟล์ต้องขึ้นเครื่องหมายถูก
3) ทดสอบ Import Fuelio
4) ทดสอบ Google Drive เดิม
5) ทดสอบ Firebase Login
6) ทดสอบสร้าง/เข้าร่วมรหัสเชิญ
7) ทดสอบอัปโหลดรูป
8) ทดสอบมือถือและ PWA

หมายเหตุ GitHub Pages ปกติแสดงจาก main เท่านั้น ถ้าต้องการดู firebase-dev ให้เปลี่ยน Pages source ชั่วคราว หรือทดสอบด้วย localhost ก่อน
