# Kotlin-first policy

FuelLog Pro Native application code is Kotlin-first and Kotlin-only.

- New domain, data, sync, ViewModel and Compose UI code must use `.kt`.
- Do not add application-owned `.java` files under `native-kotlin/app/src`.
- Java dependencies may be consumed from Kotlin when an Android library does
  not provide Kotlin source; this does not make our application code Java.
- Firebase uses the supported main Android modules, not the retired `-ktx`
  artifacts.

