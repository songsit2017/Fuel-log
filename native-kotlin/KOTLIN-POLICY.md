# Kotlin-first policy

FuelLog Pro Native application code is Kotlin-first and Kotlin-only.

- New domain, data, sync, ViewModel and Compose UI code must use `.kt`.
- Do not add application-owned `.java` files under `native-kotlin/app/src`.
- Java dependencies may be consumed from Kotlin when an Android library does
  not provide Kotlin source; this does not make our application code Java.
- Firebase uses the supported main Android modules, not the retired `-ktx`
  artifacts.
- The native Kotlin/Compose project is the primary Android build. Capacitor is
  retained only as a temporary compatibility fallback. Generated Capacitor Java
  bridge files are not copied into the native project.
- A feature replaces its Capacitor counterpart only after data-parity,
  offline, authentication and UI tests pass.
