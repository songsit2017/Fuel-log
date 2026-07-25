FuelLog Pro v3.7 — Stable Oil Price & Drive Session

ราคาน้ำมัน
1. อัปโหลดไฟล์ทั้งหมด รวมโฟลเดอร์ .github และ scripts ขึ้น Repository
2. ไปที่ GitHub > Actions > Update oil prices > Run workflow หนึ่งครั้ง
3. Workflow จะดึงข้อมูลจาก Web Service ทางการของบางจากไปเก็บเป็น oil-prices.json
4. หลังจากนั้นระบบอัปเดตทุก 30 นาทีโดยอัตโนมัติ

วิธีนี้แก้ปัญหา CORS เพราะหน้าแอปอ่าน oil-prices.json จากโดเมน GitHub Pages เดียวกัน
หาก Actions Push ไม่ได้: Settings > Actions > General > Workflow permissions > Read and write permissions

Google Drive
- Access token ถูกจำทั้ง sessionStorage และ localStorage จนกว่า token จะหมดอายุ
- ปิดแล้วเปิด PWA ใหม่ภายในอายุ token ไม่ต้องกดเชื่อมต่ออีก
- เมื่อ token หมดอายุ Google กำหนดให้ผู้ใช้แตะปุ่มเพื่อออก token ใหม่สำหรับเว็บแบบไม่มี backend
- รายการที่บันทึกขณะ token หมดอายุจะถูกทำเครื่องหมายรอซิงก์ และอัปโหลดหลังเชื่อมต่อใหม่
