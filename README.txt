ATC Live – Android starter app

CURRENT FEATURES
- Search airport names and ICAO codes.
- Open the official stream provider's search page for an airport.
- Live aircraft map powered by the AvioADSB widget, with Berlin, Frankfurt and world views.
- Direct HTML audio player for direct HTTPS audio URLs when the source provider permits embedding.
- Mobile-friendly layout and installable PWA support over HTTPS.

AUDIO PLAYER LIMITS
The player needs a direct HTTPS audio-stream URL, not a website page or a search-results link. Some playlist formats and protected streams do not work in the browser. The app does not embed LiveATC audio because LiveATC's published notice says its audio streams may not be used in third-party products. The player does not bypass provider restrictions. For listening without a direct stream URL, use an official provider's own player in its website.

LIVE MAP
The map shows aircraft positions received by the AvioADSB community network and refreshes on its own. It cannot determine which aircraft are currently speaking to ATC. Coverage depends on nearby volunteer receivers. The free map widget must retain its provider attribution.

HOW TO TRY IT ON ANDROID
1. Host the folder on an HTTPS static host (for example GitHub Pages).
2. Open the hosted URL in Chrome on Android.
3. Chrome menu (⋮) > Add to Home screen / Install app.

To edit the airport list, open index.html and modify the `airports` array.
