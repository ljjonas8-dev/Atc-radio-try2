# ATC Live Android app

Native Android app built with Kotlin, Jetpack Compose and Google Maps Compose.

## Features
- Satellite map using Google Maps.
- Airport search across the bundled worldwide airport reference data fallback list.
- Live aircraft positions from Airplanes.live within 100 nautical miles of the selected airport.
- Tap an aircraft marker to inspect its callsign, aircraft type, registration, altitude, speed, track and squawk when provided.
- Search currently received aircraft by callsign or registration.
- ATC.com radio directory opens externally; ATC Live does not re-host third-party raw audio.

## Google Maps key
Google Maps requires a Maps SDK for Android API key and billing configuration in the Google Cloud project. Add `MAPS_API_KEY=your_key` to `android-app/local.properties` for local builds, or configure a GitHub Actions repository secret named `MAPS_API_KEY`. Do not commit a real key to the repository.

Without a key, the APK can build but the Google map tiles will not load.

## Build
GitHub Actions builds a debug APK and publishes it as the `atc-live-android-debug` artifact. Live ADS-B coverage and API availability vary. Airplanes.live documents a limit of one API request per second and does not provide an uptime guarantee.
