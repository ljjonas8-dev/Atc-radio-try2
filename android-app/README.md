# ATC Live – native Android app

Dieses Verzeichnis enthält das separate Android-Projekt. Die bestehende Website im Repository-Root bleibt davon unabhängig.

## Aktueller Stand

- Native Android-Oberfläche mit Jetpack Compose
- Tabs für Flughafenübersicht, Flüge, Karte und Audio
- Lokale Startliste von Beispiel-Flughäfen und Suche nach ICAO-Code, Name oder Stadt
- Vorbereitete Umschaltung zwischen Straßen- und Satellitenkartenmodus (die eigentlichen Kartenkacheln und Live-Flugzeugpositionen folgen in einem nächsten Schritt)
- GitHub Actions Workflow zum Erstellen eines installierbaren Debug-APK

## APK auf dem Handy erstellen

1. Öffne im Browser die Registerkarte **Actions** des Repositorys.
2. Wähle **Build Android APK** und starte **Run workflow** auf dem Branch `android-native`.
3. Öffne den abgeschlossenen Workflow-Lauf und lade das Artefakt `atc-live-debug-apk` herunter.
4. Entpacke die ZIP-Datei und installiere `app-debug.apk`. Android kann nach der Erlaubnis fragen, Apps aus dieser Downloadquelle zu installieren.

Ein Debug-APK ist zum privaten Testen geeignet. Für eine öffentliche Veröffentlichung ist später ein signierter Release-Build erforderlich.

## Geplante Umsetzung

1. Echte Flughafendaten und Suche aus einer stabilen Datenquelle anbinden.
2. Straßen- und Satellitenkartenquelle samt Attribution und Nutzungsbedingungen integrieren.
3. Flughafenmarker und Live-Flugzeugpositionen auf der Karte anzeigen.
4. Ankünfte/Abflüge und passende ATC-/Airport-Audioquellen einbinden.
5. Ladezustände, Fehlerfälle und Offline-Verhalten ergänzen.

## Build lokal

Das Projekt nutzt Java 17 und Gradle 8.11.1. In GitHub Actions wird Gradle eingerichtet und anschließend `gradle -p android-app :app:assembleDebug` ausgeführt.
