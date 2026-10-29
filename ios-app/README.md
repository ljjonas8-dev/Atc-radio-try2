# ATC Live – native iPadOS app

This SwiftUI app lives in a separate `ipad-native` branch and under `ios-app/`; it does not replace or modify the website on `main`.

## Current prototype
- Native SwiftUI interface designed for iPad with a sidebar and spacious airport dashboard.
- Airport search across ICAO/IATA, name, city and country.
- Loads the existing public airport dataset from `https://airportmiles.com/data/airports.json`, with a built-in fallback airport list.
- Native Apple MapKit map with standard and satellite styles.
- Flight-status links and curated public YouTube airport-stream links based on the existing website.
- Live aircraft overlays from Airplanes.live, with adsb.fi fallback, showing callsign, type, registration, altitude, speed, track and squawk where provided.
- Live aircraft search by callsign or registration.
- Selectable markers for each country's major OurAirports category and detected capital airport; the searchable catalogue loads the OurAirports global CSV.
- Aviationstack arrivals/departures are wired through a secure Cloudflare Worker proxy. Set the Worker URL in the source after deployment.
- Public radio directories are opened externally; direct raw third-party audio integration awaits provider permission.

## Build
GitHub Actions builds an iOS Simulator app on a macOS runner and publishes `atc-live-ipad-simulator` as an artifact. A simulator app is for testing and cannot be installed directly on a physical iPad. Installing on an iPad requires Apple signing/provisioning (normally through Xcode on a Mac or an appropriate Apple distribution workflow).

Minimum deployment target: iPadOS 17.

Build workflow runs on changes to `ipad-native` and `global-radar-upgrade`.

Xcode project product name explicitly configured for the native target.
