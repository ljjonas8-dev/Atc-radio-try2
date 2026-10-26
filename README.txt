ATC Live – Android starter app

WHAT IT DOES
- Search a small directory of airport codes/names.
- Open the public stream provider's search page for each airport.
- Includes an in-page HTML audio player for direct HTTPS audio-stream URLs, when the provider permits embedding.
- Responsive, dark-mode-friendly interface.
- Can be installed as a PWA when hosted over HTTPS.

IMPORTANT
The player does not bypass provider restrictions. LiveATC audio is not embedded; use the official provider link for those feeds. The in-page player works only with direct HTTPS audio URLs whose providers allow browser playback. Some URLs may be playlists, web pages, or protected streams and may not play. Stream availability is controlled by each provider. Check provider terms and local rules.

HOW TO TRY IT ON ANDROID
1. Host the folder on an HTTPS static host (for example GitHub Pages).
2. Open the hosted URL in Chrome on Android.
3. Chrome menu (⋮) > Add to Home screen / Install app.
4. Use the airport links to visit the official stream provider, or enter an authorized direct HTTPS audio URL in the in-page player.

To edit the airport list, open index.html and modify the `airports` array.
