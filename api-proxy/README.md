# ATC Live API integration

## 1. Flight status: Aviationstack

The app uses Aviationstack for scheduled/estimated arrivals and departures. Its key is kept in a Cloudflare Worker secret, never in website JavaScript or a mobile app binary.

### Deploy the proxy
1. Create a Cloudflare account and install Wrangler.
2. From this folder, run `wrangler login`.
3. Add your provider key as a secret: `wrangler secret put AVIATIONSTACK_ACCESS_KEY`.
4. Deploy: `wrangler deploy`.
5. Test `https://YOUR-WORKER-URL/health`.
6. Set `window.ATC_API_BASE_URL` in the root `api-config.js` to the Worker URL.
7. Replace `YOUR-CLOUDFLARE-WORKER` in `ios-app/ATCLive/ContentView.swift` and `android-app/app/src/main/java/com/atclive/android/MainActivity.kt` with the same Worker base URL, then commit/rebuild.

The Worker supports:
- `GET /api/flights?airport=FRA&direction=departure&limit=5`
- `GET /api/flights?airport=FRA&direction=arrival&limit=5`
- `GET /api/flight?ident=LH400`
- `GET /health`

Use a three-letter IATA code for airport requests. Configure Cloudflare rate limiting for the Worker before sharing it publicly; otherwise third parties could consume the account's request quota. The default CORS setting is permissive for setup convenience. For production, restrict website origins and apply Cloudflare rate limits. The API key itself is never sent to clients.

### Provider choice and costs
Aviationstack offers a free tier for personal/non-commercial testing (100 requests/month on the published plan) and paid plans for commercial use. Its published Basic plan is $49.99/month for 10,000 requests, with overages listed on its pricing page. Data can be delayed by roughly 30–60 seconds, so do not present it as a certified real-time aviation source. Verify current plan limits, overage settings and licence terms before release:
- https://aviationstack.com/pricing
- https://aviationstack.com/faq

## 2. Live aircraft positions: primary + fallback
- Primary: Airplanes.live, community ADS-B/MLAT aggregation.
- Fallback: adsb.fi open-data API, using its documented v3 position endpoint when Airplanes.live fails.
- Both are best-effort, rate-limited, and may have different coverage. adsb.fi's public API is for personal/non-commercial use and documents a limit of one request per second. The app uses fallback only after a primary failure; it does not fan out every poll to both providers.
- Airplanes.live: https://airplanes.live/faq/
- adsb.fi API and terms: https://github.com/adsbfi/opendata

## 3. Worldwide airports
- The apps use OurAirports' public-domain airport CSV as the worldwide searchable catalogue.
- REST Countries supplies country names and capital-city names for the marker overlay.
- Each country's highlighted "major airport" is selected by the OurAirports airport category (large, then medium, then small); this is **not** a verified passenger-traffic ranking. A capital-city airport is highlighted when the airport municipality matches the country capital. Some countries may not match automatically because city names and airport municipalities differ.
- Dataset: https://ourairports.com/data/
- Country metadata: https://restcountries.com/

## 4. ATC radio audio: permission is required for direct stream integration
I did not find a public, documented global ATC audio API whose terms clearly allow unrestricted third-party embedding in a competing app.
- LiveATC's streams should not be scraped, proxied or embedded without permission; contact the provider for an explicit licence.
- ATC.com has its own stream service, but its terms restrict raw API-data redistribution and using its content to develop a competing service. Do not reverse engineer its app or copy its stream URLs.
- OpenSky's separate ATC audio/transcript API requires a written agreement for operational REST API integration.
- Until a provider grants permission and supplies an approved API/stream contract, ATC Live keeps official-provider links and existing curated official video embeds rather than relaying raw radio audio.

Sources:
- https://www.atc.com/terms
- https://api.atc.opensky-network.org/q/swagger-ui/
- https://opensky-network.org/about/terms-of-use
- https://www.liveatc.net/legal/

## Release checklist
- Deploy the Worker, set the secret, configure rate limiting and replace client Worker URL placeholders.
- Verify provider account permits the intended hobby/public app use.
- Test departures, arrivals, flight lookup, rate-limit errors, no-data responses and provider outage fallback.
- Validate radio rights with the provider before adding an in-app raw audio player.
- Run all GitHub Actions workflows. Successful compilation/syntax checks do not by themselves prove data-provider access, account entitlement, or runtime availability.
