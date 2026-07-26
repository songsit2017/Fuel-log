# FuelLog Pro Android APK / Play Store

โฟลเดอร์ `mobile/` ใช้ Capacitor 7.6.4 สร้าง Android APK จาก source เดียวกับเว็บ  
Package ID: `com.songsit.fuellogpro`

## ดาวน์โหลด APK ทดสอบจาก GitHub Actions

1. Push โปรเจกต์ขึ้น branch `main`
2. เปิด GitHub → **Actions** → **Build Android APK**
3. กด **Run workflow**
4. เมื่อสำเร็จ เปิด workflow run แล้วดาวน์โหลด Artifact ชื่อ `FuelLog-Pro-Android-*`

ถ้ายังไม่ได้ตั้ง signing secret จะได้ไฟล์ `*-TEST.apk` สำหรับติดตั้งทดสอบเท่านั้น  
APK ทดสอบจากคนละ workflow run อาจอัปเดตทับกันไม่ได้ เพราะใช้ debug signing key คนละชุด

## ตั้ง signing key สำหรับ APK ที่อัปเดตทับกันได้

สร้าง upload keystore เพียงครั้งเดียวบน Windows:

```powershell
keytool -genkeypair -v -keystore fuellog-upload.jks -alias fuellog `
  -keyalg RSA -keysize 2048 -validity 10000
```

แปลง keystore เป็น Base64 แล้วคัดลอก:

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes((Resolve-Path ".\fuellog-upload.jks"))
) | Set-Clipboard
```

ใน GitHub เปิด **Settings → Secrets and variables → Actions** แล้วเพิ่ม:

- `APK_KEYSTORE_BASE64` — ค่าจาก Clipboard
- `APK_KEYSTORE_PASSWORD` — รหัส keystore
- `APK_KEY_ALIAS` — `fuellog`
- `APK_KEY_PASSWORD` — รหัส key

เก็บไฟล์ `fuellog-upload.jks` และรหัสไว้ในที่ปลอดภัย ห้าม commit หรือส่งในแชต  
ถ้าทำหายจะไม่สามารถออก APK ที่อัปเดตทับแอปเดิมได้

## สร้าง GitHub Release ให้ผู้ใช้ดาวน์โหลด

หลังตั้ง Secret และ push `main` แล้ว:

```powershell
git tag v8.0.0-beta.1
git push origin v8.0.0-beta.1
```

Workflow จะสร้าง:

- `FuelLog-Pro-v8.0.0-beta.1.apk` สำหรับติดตั้งตรง
- `FuelLog-Pro-v8.0.0-beta.1.aab` สำหรับ Play Console ในอนาคต
- GitHub Release พร้อมปุ่มดาวน์โหลด

## ก่อนทดสอบ APK

- Android: อนุญาต “ติดตั้งแอปที่ไม่รู้จัก” ให้ Chrome หรือ Files
- Firebase Console: เพิ่ม Android app package `com.songsit.fuellogpro`
- เพิ่ม SHA-1 และ SHA-256 ของ signing key ใน Firebase
- ดาวน์โหลด `google-services.json` เมื่อเริ่มเปลี่ยน Google Login เป็น native
- ทดสอบ Google Login, Family Sharing, กล้อง, Gallery, OCR, GPS, Sync และ Export บนเครื่องจริง

## ก่อนขึ้น Play Store

- ใช้ AAB ที่เซ็นด้วย upload key เดิม
- เปิด Play App Signing
- เปลี่ยน Google Login จาก Web popup/redirect เป็น Native Firebase Authentication
- เพิ่ม adaptive icon และ splash screen ความละเอียดจริง
- จัดทำ Privacy Policy และ Data safety
- ใช้ Internal testing ก่อน Closed/Open/Production

> รุ่น APK ช่วงแรกเป็น Beta เพราะ Google Web Auth ใน Android WebView ยังต้องทดสอบและเปลี่ยนเป็น native ก่อนเผยแพร่ Play Store
