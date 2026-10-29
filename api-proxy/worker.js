const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Cache-Control": "no-store"
};

function json(body, status = 200, extra = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json; charset=utf-8", ...extra }
  });
}

function validAirportCode(value) {
  return /^[A-Z]{3}$/.test(value || "");
}
function validFlightIdent(value) {
  return /^[A-Z0-9]{2,3}[ -]?[0-9]{1,4}[A-Z]?$/.test(value || "") || /^[A-Z0-9-]{2,12}$/.test(value || "");
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: corsHeaders });
    if (request.method !== "GET") return json({ error: "Method not allowed" }, 405);
    if (url.pathname === "/health") return json({ ok: true, provider: "Aviationstack", configured: Boolean(env.AVIATIONSTACK_ACCESS_KEY) });
    if (!env.AVIATIONSTACK_ACCESS_KEY) return json({ error: "Flight-status provider is not configured. Add AVIATIONSTACK_ACCESS_KEY as a Worker secret." }, 503);

    const cache = caches.default;
    const cacheKey = new Request(url.toString(), request);
    const cached = await cache.match(cacheKey);
    if (cached) return cached;

    let providerURL;
    if (url.pathname === "/api/flights") {
      const airport = String(url.searchParams.get("airport") || "").trim().toUpperCase();
      const direction = String(url.searchParams.get("direction") || "departure").toLowerCase();
      const limit = Math.max(1, Math.min(10, Number(url.searchParams.get("limit") || 5)));
      if (!validAirportCode(airport)) return json({ error: "airport must be a three-letter IATA code" }, 400);
      if (!["departure", "arrival"].includes(direction)) return json({ error: "direction must be departure or arrival" }, 400);
      const query = new URLSearchParams({ access_key: env.AVIATIONSTACK_ACCESS_KEY, limit: String(limit) });
      query.set(direction === "departure" ? "dep_iata" : "arr_iata", airport);
      providerURL = "https://api.aviationstack.com/v1/flights?" + query.toString();
    } else if (url.pathname === "/api/flight") {
      const ident = String(url.searchParams.get("ident") || "").trim().toUpperCase().replace(/\s+/g, "");
      if (!validFlightIdent(ident)) return json({ error: "ident must be a valid flight identifier" }, 400);
      const query = new URLSearchParams({ access_key: env.AVIATIONSTACK_ACCESS_KEY, flight_iata: ident, limit: "5" });
      providerURL = "https://api.aviationstack.com/v1/flights?" + query.toString();
    } else {
      return json({ error: "Not found" }, 404);
    }

    try {
      const upstream = await fetch(providerURL, { headers: { "Accept": "application/json" } });
      const payload = await upstream.json();
      if (!upstream.ok || payload.error) return json({ error: "Flight-status provider request failed", details: payload.error || upstream.status }, upstream.status || 502);
      const flights = Array.isArray(payload.data) ? payload.data.map(item => ({
        flight: item.flight?.iata || item.flight?.icao || null,
        airline: item.airline?.name || null,
        status: item.flight_status || null,
        departure: {
          airport: item.departure?.airport || null,
          iata: item.departure?.iata || null,
          scheduled: item.departure?.scheduled || null,
          estimated: item.departure?.estimated || null,
          actual: item.departure?.actual || null,
          terminal: item.departure?.terminal || null,
          gate: item.departure?.gate || null
        },
        arrival: {
          airport: item.arrival?.airport || null,
          iata: item.arrival?.iata || null,
          scheduled: item.arrival?.scheduled || null,
          estimated: item.arrival?.estimated || null,
          actual: item.arrival?.actual || null,
          terminal: item.arrival?.terminal || null,
          gate: item.arrival?.gate || null
        },
        aircraft: { registration: item.aircraft?.registration || null, iata: item.aircraft?.iata || null }
      })) : [];
      const result = json({ provider: "Aviationstack", pagination: payload.pagination || null, data: flights }, 200, { "Cache-Control": "public, max-age=45" });
      ctx.waitUntil(cache.put(cacheKey, result.clone()));
      return result;
    } catch {
      return json({ error: "Could not reach flight-status provider" }, 502);
    }
  }
};
