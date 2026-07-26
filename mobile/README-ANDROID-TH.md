# FuelLog Pro Android / Play Store

โฟลเดอร์นี้เป็น Capacitor wrapper แยกจากเว็บบน GitHub Pages

## สร้าง Android project ครั้งแรก
```bash
cd mobile
npm install
npm run android:add
npm run android:open
```

## หลังแก้เว็บ
```bash
cd mobile
npm run android:sync
```

## สร้างไฟล์ทดสอบ APK
```bash
cd mobile
npm run android:build:debug
```
ไฟล์จะอยู่ใต้ `mobile/android/app/build/outputs/apk/debug/`

## สร้าง Play Store AAB
ต้องสร้าง signing keystore และตั้งค่า release signing ใน Android Studio ก่อน แล้วใช้:
```bash
cd mobile
npm run android:build:release
```

## สิ่งที่ต้องทำก่อนขึ้น Play Store
- เปลี่ยน package id หากต้องการ ก่อน `cap add android`
- เพิ่มไอคอน adaptive icon และ splash screen
- ทำ Privacy Policy
- กรอก Data safety form
- ตั้ง Firebase Android app + SHA-1/SHA-256 สำหรับ Native Google Sign-In
- ทดสอบกล้อง แกลเลอรี ไฟล์ และตำแหน่งบน Android จริง

หมายเหตุ: เว็บปัจจุบันใช้ Firebase Web Auth ผ่าน popup/redirect การทำ APK Production ควรเปลี่ยน Google Login เป็น Native plugin ก่อนส่ง Play Store เพื่อความเสถียร
