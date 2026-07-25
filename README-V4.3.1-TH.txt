FuelLog Pro v4.3.1 Google Auth Hotfix

แก้ปุ่มเข้าสู่ระบบ Google บนมือถือ/PWA
- PWA ใช้ signInWithRedirect
- Browser ใช้ Popup และ fallback เป็น Redirect
- แสดงข้อความ Error ในหน้า ไม่เงียบ
- หากขึ้น unauthorized-domain ให้เพิ่ม songsit2017.github.io ที่ Firebase Authentication > Settings > Authorized domains
