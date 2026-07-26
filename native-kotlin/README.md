# FuelLog Pro Native (Kotlin) — migration workspace

This directory is the primary Android implementation. GitHub Actions builds
this Kotlin/Compose project as the downloadable Android artifact. The current
Capacitor project remains a compatibility fallback until each native feature
passes data-parity tests.

All application-owned native source is Kotlin. See `KOTLIN-POLICY.md`.

## Phase 1 included

- Domain models matching the existing Firestore schema.
- Repository contract that keeps UI code independent from Firebase.
- Firebase vehicle discovery for records owned by the signed-in UID.
- A ViewModel with automatic first-session Cloud synchronization.
- A Compose vehicle screen using unidirectional data flow.

## Migration order

1. Authentication and Cloud vehicle discovery.
2. Room offline database and conflict-safe sync.
3. Fuel list/editor and full-tank efficiency calculation.
4. Expense, maintenance, photos and Fuelio import.
5. Family sharing, OCR, weather, reports and export.

The native app must use the existing Firebase project, package
`com.songsit.fuellogpro`, release keystore and Firestore document IDs. Never
create a second production schema.
