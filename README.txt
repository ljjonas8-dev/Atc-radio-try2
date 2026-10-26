ATC Live – Android starter app

WHAT IT DOES
- Search a small directory of airport codes/names.
- Open the public stream provider's search page for each airport.
- Responsive, dark-mode-friendly interface.
- Can be installed as a PWA when hosted over HTTPS.

IMPORTANT
This starter app does not embed or relay audio itself. It links to LiveATC.net search pages; stream availability varies and is controlled by the provider. Check provider terms and local rules. Airport list is an editable starter list, not a guarantee that each feed is available.

HOW TO TRY IT ON ANDROID
1. Extract the ZIP and host the folder on an HTTPS static host (for example GitHub Pages).
2. Open the hosted URL in Chrome on Android.
3. Chrome menu (⋮) > Add to Home screen / Install app.
4. Tap "Stream suchen" to open the provider's page.

To edit the airport list, open index.html and modify the `airports` array.
