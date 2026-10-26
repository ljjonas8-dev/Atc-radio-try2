ATC Live – Flughafenübersicht (Android / PWA)

AKTUELLE FUNKTIONEN
- Flughafen nach Name, ICAO-Code oder IATA-Code suchen.
- Eine Flughafenübersicht mit Stammdaten und Koordinaten öffnen.
- Eine Live-Flugzeugkarte von AvioADSB auf den ausgewählten Flughafen zentrieren.
- Für ausgewählte Flughäfen offizielle Flugzeug-Livestreams per YouTube-Einbettung abspielen. ATC-Audio ist nur enthalten, wenn der jeweilige Sender es liefert.
- Für Flughäfen ohne passenden bestätigten Stream wird klar angezeigt, dass derzeit kein passender eingebetteter Audio-Stream hinterlegt ist.
- Mobilfreundliches Layout und installierbare PWA über HTTPS.

AUDIO UND QUELLEN
Die eingebetteten Livestreams werden vom offiziellen YouTube-Player abgespielt. ATC Live kopiert, hostet oder proxyt die Audiodaten nicht. Die Verfügbarkeit des Streams und des Flugfunktons liegt beim jeweiligen Sender. Es werden keine LiveATC-Streams direkt eingebunden.
Offizielle Übersicht der Flightradar24-Airport-Livestreams: https://www.flightradar24.com/blog/airport-live-streams/

LIVE-KARTE
Die Karte zeigt Flugzeugpositionen, die das AvioADSB-Community-Netzwerk empfängt. Abdeckung hängt von nahegelegenen Empfangsstationen ab. Die Karte zeigt nicht, welches Flugzeug gerade spricht. Der AvioADSB-Hinweis muss sichtbar bleiben.
Quelle: https://avioadsb.org/flight-tracker-widget

AUF ANDROID TESTEN
1. Ordner auf einem HTTPS-Static-Host bereitstellen (z. B. GitHub Pages).
2. Gehostete URL in Chrome auf Android öffnen.
3. Chrome-Menü (⋮) > Zum Startbildschirm hinzufügen / App installieren.

Flughäfen und Stream-IDs werden im `airports`-Array in index.html gepflegt. Flughafen-Stammdaten sind redaktionell hinterlegt; die Flugzeugkarte aktualisiert sich live.
