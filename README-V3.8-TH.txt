FuelLog Pro v3.8 Minimal Family

ฐาน: v3.7 Stable ที่ฟังก์ชันเดิมครบ
เพิ่มแบบไม่รื้อของเดิม:
- UI minimal override
- Firebase Login จำบัญชี
- แชร์รถ Owner/Editor/Viewer ผ่านรหัสเชิญ
- Sync รายการเติมน้ำมัน ค่าใช้จ่าย รอบบำรุง และ Trip Log
- Firebase Storage สำหรับใบเสร็จ เรือนไมล์ งานซ่อม และเอกสาร
- Trip Log/ต้นทุนหน้างาน
- OCR/สแกนบิลเดิมของ v3.7 ยังคงอยู่

ติดตั้ง:
1) อัปโหลดทุกไฟล์/โฟลเดอร์ทับบน Branch ทดสอบ
2) Firestore > Rules: ลบทั้งหมด แล้ววาง firestore.rules
3) Storage > Rules: ลบทั้งหมด แล้ววาง storage.rules
4) Authentication > Settings > Authorized domains ต้องมี songsit2017.github.io
5) เปิดแอป กดปุ่ม 👥 ด้านขวาล่าง

ความปลอดภัย:
- ห้ามใส่ Service Account, Private Key หรือ API Secret ใน GitHub
- firebaseConfig ฝั่งเว็บเปิดเผยได้ตามการออกแบบ Firebase; สิทธิ์จริงคุมด้วย Rules
