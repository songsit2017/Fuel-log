# FuelLog Pro Native (Kotlin) — migration workspace

This directory is the parallel native Android implementation. The current
Capacitor APK remains the production fallback until each native feature passes
data-parity tests.

All application-owned native source is Kotlin. See `KOTLIN-POLICY.md`.

## Current preview includes

- Domain models compatible with the existing Firestore direction.
- Credential Manager and Firebase repository foundations.
- Room-backed multiple vehicles with a safe v1-to-v2 migration.
- Vehicle-scoped fuel entry, deletion and full-tank consumption summary.
- Vehicle-scoped expenses, income, recurrence, payment reminders and net total.
- Maintenance reminders by date or odometer with overdue/soon status and recurrence.
- Daily Android notifications for upcoming date-based maintenance.
- Immediate odometer reminder refresh with separate date, mileage and payment toggles.
- Minimal operating-cost report with safe UTF-8 CSV export through Android's file picker.
- Offline trips with distance, fuel, toll, parking, food and other costs.
- Versioned JSON backup/restore for every Room collection using Android's file picker.
- Manual Google/Firebase sync that preserves divergent copies as explicit conflicts.
- Persistent conflict cards where the user explicitly chooses the Local or Cloud copy.
- Persistent deletion tombstones prevent removed records from returning on another device.
- A Thai minimal Compose dashboard and launcher icon.
- A separate prerelease APK workflow that cannot replace Stable.

## Migration order

1. Authentication and Cloud vehicle discovery.
2. Room offline database and conflict-safe sync.
3. Fuel list/editor and full-tank efficiency calculation.
4. Expense, maintenance, photos and Fuelio import.
5. Family sharing, OCR, weather, reports and export.

The native app must use the existing Firebase project, package
`com.songsit.fuellogpro`, release keystore and Firestore document IDs. Never
create a second production schema.
