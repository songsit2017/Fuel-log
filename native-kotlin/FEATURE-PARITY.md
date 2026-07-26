# FuelLog Pro Native feature parity

## Required from the current product

- Google Login with Credential Manager and Firebase Auth.
- Automatic first-session Cloud vehicle restore.
- Multiple vehicles and a global vehicle selector.
- Family owner/editor/viewer sharing and driver selection.
- Fuel records, full/partial/missed-fill handling and consumption intervals.
- Expense, income, maintenance, reminders and trips.
- Receipt and odometer photos with previews and Firebase Storage.
- Fuelio import that updates matching date/time/odometer records without duplicates.
- Import of all Fuelio fuel and expense photos.
- Secure Claude OCR through Firebase Functions; no API key in the APK.
- Optional Open-Meteo weather attached to each fuel record.
- Bangchak, PTT/OR and Shell daily price comparison.
- Currency, decimals, theme, weather and automatic OCR settings.
- Search, reports, JSON/CSV export and printable reports.
- Offline-first operation and background conflict-safe synchronization.
- Thai/English-ready resources, accessibility and adaptive phone/tablet layouts.

## Native additions planned

- Room as the single offline source of truth.
- WorkManager upload/download queue with retry and network constraints.
- Android Photo Picker and CameraX.
- Android notification channels for maintenance reminders.
- Encrypted local preferences for non-secret account settings.
- Structured sync diagnostics and a user-visible conflict log.
- Crash-safe Fuelio import transaction with preview and rollback.
- Database backup file compatible with future app migrations.

No feature is considered migrated until its result matches the current app
against the same exported test dataset.

