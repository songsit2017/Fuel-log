FuelLog Pro v3.6
- ราคาน้ำมันเปลี่ยนไปใช้ Web Service ทางการของบางจาก: /ApiOilPrice2/th
- แสดงราคาวันนี้/พรุ่งนี้และเวลาอัปเดต พร้อม cache เมื่อออฟไลน์
- Google Drive access token ถูกเก็บใน sessionStorage จึงไม่หายเมื่อรีโหลด/PWA ถูกพักแล้วกลับมา
- เมื่อ token หมดอายุ แอปใช้การแตะ/กดปุ่มครั้งแรกเพื่อขอต่ออายุแบบ prompt:'' หลังเคยให้สิทธิ์แล้ว
ข้อจำกัด: Google Identity Services สำหรับเว็บแบบ static ไม่ออก refresh token ถาวร จึงอาจต้องแตะเชื่อมต่อใหม่เมื่อ access token หมดอายุหรือ Google session สิ้นสุด
