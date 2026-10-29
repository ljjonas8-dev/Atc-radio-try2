package com.atclive.android

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

private val Navy = Color(0xFF071321)
private val Panel = Color(0xFF102235)
private val Mint = Color(0xFF62F3A5)
private val Muted = Color(0xFF9BB1C5)
private val http = OkHttpClient()

private data class Airport(val icao: String, val iata: String, val name: String, val country: String, val city: String, val lat: Double, val lon: Double, val elevation: String = "—", val terminals: String = "Keine Angabe", val countryCode: String = "", val type: String = "large_airport", val isCountryMajor: Boolean = false, val isCapitalAirport: Boolean = false) {
    val position get() = LatLng(lat, lon)
}
private data class Plane(val hex: String, val callsign: String, val registration: String, val type: String, val lat: Double, val lon: Double, val altitude: String, val speed: String, val track: Float, val squawk: String)

private val airports = listOf(
    Airport("EDDF","FRA","Frankfurt am Main","Deutschland","Frankfurt",50.0379,8.5622,"111 m","2"),
    Airport("EDDB","BER","Berlin Brandenburg","Deutschland","Berlin",52.3667,13.5033,"48 m","2"),
    Airport("EDDM","MUC","München","Deutschland","München",48.3538,11.7861,"453 m","2"),
    Airport("EHAM","AMS","Amsterdam Schiphol","Niederlande","Amsterdam",52.3105,4.7683,"−3 m","1"),
    Airport("EGLL","LHR","London Heathrow","Vereinigtes Königreich","London",51.4700,-0.4543,"25 m","4"),
    Airport("KJFK","JFK","New York JFK","USA","New York",40.6413,-73.7781,"4 m","6"),
    Airport("KLAX","LAX","Los Angeles International","USA","Los Angeles",33.9416,-118.4085,"38 m","9"),
    Airport("KLAS","LAS","Harry Reid International","USA","Las Vegas",36.0840,-115.1537,"665 m","2"),
    Airport("KSFO","SFO","San Francisco International","USA","San Francisco",37.6213,-122.3790,"4 m","4"),
    Airport("OMDB","DXB","Dubai International","Vereinigte Arabische Emirate","Dubai",25.2532,55.3657,"19 m","3"),
    Airport("RJTT","HND","Tokyo Haneda","Japan","Tokio",35.5494,139.7798,"6 m","3"),
    Airport("YSSY","SYD","Sydney Kingsford Smith","Australien","Sydney",-33.9399,151.1753,"6 m","3")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ATCLiveApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ATCLiveApp() {
    var allAirports by remember { mutableStateOf(airports) }
    var featuredAirports by remember { mutableStateOf(airports) }
    var airportDataStatus by remember { mutableStateOf("Beispielauswahl – weltweite Daten werden geladen …") }
    var query by remember { mutableStateOf("") }
    var flightQuery by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(airports.first()) }
    var planes by remember { mutableStateOf(emptyList<Plane>()) }
    var chosenPlane by remember { mutableStateOf<Plane?>(null) }
    var status by remember { mutableStateOf("Lade Live-Flugzeuge …") }
    var flightStatus by remember { mutableStateOf("Live-Suche: Rufzeichen oder Registrierung") }
    var scheduleStatus by remember { mutableStateOf("Flugstatus-Proxy noch nicht konfiguriert.") }
    var scheduleRows by remember { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Übersicht") }
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(selected.position, 6.5f) }
    val matches = remember(query, allAirports) { allAirports.filter { listOf(it.icao,it.iata,it.name,it.city,it.country).any { value -> value.contains(query, true) } }.take(50) }

    LaunchedEffect(Unit) {
        try {
            val catalog = loadWorldwideAirportCatalog()
            allAirports = catalog.first
            featuredAirports = catalog.second
            selected = allAirports.firstOrNull { it.icao == selected.icao } ?: allAirports.firstOrNull() ?: selected
            airportDataStatus = "OurAirports: ${allAirports.size} Flughäfen · große Kategorie + Hauptstadt hervorgehoben"
        } catch (_: Exception) { airportDataStatus = "Weltweite Daten nicht erreichbar – Beispielauswahl aktiv" }
    }

    LaunchedEffect(selected.icao) {
        camera.position = CameraPosition.fromLatLngZoom(selected.position, 7.5f)
        while (isActive) {
            try {
                planes = fetchPlanes(selected)
                status = "${planes.size} Flugzeuge · Airplanes.live · Aktualisierung alle 20 s"
            } catch (_: Exception) { status = "Live-Positionen aktuell nicht verfügbar" }
            delay(20_000)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Navy, surface = Panel, onPrimary = Navy, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Navy, topBar = {
            TopAppBar(title = { Column { Text("ATC LIVE", fontWeight = FontWeight.Black); Text("GLOBAL AVIATION", style = MaterialTheme.typography.labelSmall, color = Mint) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White))
        }) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
                item {
                    OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Flughafen suchen") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                }
                items(matches.take(8)) { airport ->
                    TextButton(onClick = { selected = airport; chosenPlane = null }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text("${airport.icao} / ${airport.iata} · ${airport.name}", color = if (airport.icao == selected.icao) Mint else Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${airport.city}, ${airport.country}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Übersicht", "Karte", "Flüge", "Funk").forEach { label ->
                            FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                        }
                    }
                }
                item { Text(airportDataStatus, color = Muted, style = MaterialTheme.typography.bodySmall) }
                item { Text(selected.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                item { Text("${selected.icao} / ${selected.iata} · ${selected.city}, ${selected.country}", color = Muted) }
                if (tab == "Karte" || tab == "Übersicht") {
                    item {
                        GoogleMap(modifier = Modifier.fillMaxWidth().height(420.dp), cameraPositionState = camera, properties = MapProperties(mapType = MapType.SATELLITE), uiSettings = MapUiSettings(zoomControlsEnabled = true)) {
                            Marker(state = MarkerState(position = selected.position), title = selected.name, snippet = "${selected.icao} · Höhe ${selected.elevation}")
                            featuredAirports.forEach { airport ->
                                Marker(state = MarkerState(position = airport.position), title = airport.name, snippet = listOf(if (airport.isCountryMajor) "Hauptflughafen-Kategorie" else "", if (airport.isCapitalAirport) "Hauptstadtflughafen" else "").filter { it.isNotEmpty() }.joinToString(" · "), onClick = { selected = airport; chosenPlane = null; true })
                            }
                            planes.forEach { plane ->
                                Marker(state = MarkerState(position = LatLng(plane.lat, plane.lon)), title = plane.callsign.ifBlank { plane.hex }, snippet = "Höhe ${plane.altitude} · ${plane.speed}", onClick = { chosenPlane = plane; true })
                            }
                        }
                        Text(status, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    chosenPlane?.let { plane ->
                        item {
                            InfoCard("FLUGZEUG · ${plane.callsign.ifBlank { "Unbekanntes Rufzeichen" }}", listOf("Typ" to plane.type.ifBlank { "Keine Angabe" }, "Registrierung" to plane.registration.ifBlank { "Keine Angabe" }, "Höhe" to plane.altitude, "Geschwindigkeit" to plane.speed, "Kurs" to "${plane.track.toInt()}°", "Squawk" to plane.squawk.ifBlank { "Keine Angabe" }, "Start / Ziel" to "Nicht zuverlässig in ADS-B-Daten enthalten"))
                        }
                    }
                }
                if (tab == "Übersicht") item {
                    InfoCard("FLUGHAFENINFORMATIONEN", listOf("ICAO / IATA" to "${selected.icao} / ${selected.iata}", "Land / Stadt" to "${selected.country} · ${selected.city}", "Höhe über Meer" to selected.elevation, "Terminals" to selected.terminals, "Koordinaten" to String.format(Locale.US, "%.4f, %.4f", selected.lat, selected.lon), "Tägliche Abflüge" to "Keine verifizierte offene Live-Datenquelle"))
                }
                if (tab == "Flüge") item {
                    OutlinedTextField(value = flightQuery, onValueChange = { flightQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Flugnummer / Rufzeichen / Registrierung") }, singleLine = true)
                    Button(onClick = {
                        val q = flightQuery.trim().uppercase(Locale.ROOT)
                        if (q.length < 2 || !q.matches(Regex("[A-Z0-9 -]{2,12}"))) {
                            flightStatus = "Bitte eine gültige Flugnummer oder Registrierung eingeben."
                        } else {
                            scope.launch {
                                flightStatus = "Suche läuft …"
                                try {
                                    val found = searchPlane(q)
                                    if (found == null) flightStatus = "Kein passendes Flugzeug wird aktuell empfangen."
                                    else {
                                        chosenPlane = found
                                        camera.position = CameraPosition.fromLatLngZoom(LatLng(found.lat, found.lon), 7.5f)
                                        flightStatus = "Live-Position gefunden: " + found.callsign.ifBlank { found.hex }
                                    }
                                } catch (_: Exception) { flightStatus = "Flugsuche momentan nicht verfügbar." }
                            }
                        }
                    }) { Text("Flug suchen") }
                    Text(flightStatus, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { scope.launch { val result = loadFlightBoard(selected.iata, "arrival"); scheduleStatus = result.first; scheduleRows = result.second } }) { Text("Ankünfte") }
                        Button(onClick = { scope.launch { val result = loadFlightBoard(selected.iata, "departure"); scheduleStatus = result.first; scheduleRows = result.second } }) { Text("Abflüge") }
                    }
                    Text(scheduleStatus, color = Muted)
                    scheduleRows.forEach { Text(it, color = Color.White, modifier = Modifier.padding(vertical = 4.dp)) }
                    Text("Live-Positionen und geplante Flüge stammen aus unterschiedlichen Datenquellen.", color = Muted)
                }
                if (tab == "Funk") item {
                    InfoCard("ÖFFENTLICHER FLUGFUNK", listOf("Status" to "Streams müssen pro Flughafen lizenziert bzw. offiziell veröffentlicht sein.", "Hinweis" to "ATC Live hostet keine fremden Audiofeeds."))
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.atc.com/"))) }) { Text("ATC.com / Funkverzeichnis öffnen") }
                }
                item { Text("ATC Live ist ein unabhängiges Hobbyprojekt. Live-Daten können unvollständig oder verzögert sein und sind nicht für Flugentscheidungen geeignet.", color = Muted, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Mint, fontWeight = FontWeight.Bold)
            rows.forEach { (key, value) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(key, color = Muted); Text(value, color = Color.White) } }
        }
    }
}

private const val FLIGHT_STATUS_API_BASE_URL = "https://YOUR-CLOUDFLARE-WORKER.workers.dev"

private suspend fun loadFlightBoard(iata: String, direction: String): Pair<String, List<String>> = withContext(Dispatchers.IO) {
    if (iata.length != 3 || iata == "—") return@withContext "Für diesen Flughafen fehlt ein IATA-Code." to emptyList()
    if (FLIGHT_STATUS_API_BASE_URL.contains("YOUR-CLOUDFLARE-WORKER")) return@withContext "Proxy noch nicht eingerichtet. Siehe api-proxy/README.md; den API-Schlüssel nicht in die App eintragen." to emptyList()
    val url = "$FLIGHT_STATUS_API_BASE_URL/api/flights?airport=$iata&direction=$direction&limit=5"
    try {
        val response = http.newCall(Request.Builder().url(url).build()).execute()
        response.use {
            if (!it.isSuccessful) error("Flight status HTTP ${it.code}")
            val root = JSONObject(it.body?.string() ?: "{}")
            val rows = root.optJSONArray("data") ?: org.json.JSONArray()
            val result = buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val dep = row.optJSONObject("departure") ?: JSONObject()
                    val arr = row.optJSONObject("arrival") ?: JSONObject()
                    add("${row.optString("flight", "—")} · ${row.optString("status", "unbekannt")} · ${dep.optString("iata", "—")} → ${arr.optString("iata", "—")} · ${dep.optString("estimated", dep.optString("scheduled", "—"))}")
                }
            }
            (if (result.isEmpty()) "Keine Flüge gefunden." else "${result.size} Flüge · Aviationstack") to result
        }
    } catch (_: Exception) { "Flugstatus nicht verfügbar. Prüfe Worker-URL, API-Key und Anbieter-Kontingent." to emptyList() }
}

private suspend fun loadWorldwideAirportCatalog(): Pair<List<Airport>, List<Airport>> = withContext(Dispatchers.IO) {
    val countryNames = mutableMapOf<String, String>()
    val capitals = mutableMapOf<String, String>()
    try {
        val response = http.newCall(Request.Builder().url("https://restcountries.com/v3.1/all?fields=cca2,capital,name").build()).execute()
        response.use {
            if (it.isSuccessful) {
                val rows = org.json.JSONArray(it.body?.string() ?: "[]")
                for (i in 0 until rows.length()) {
                    val item = rows.optJSONObject(i) ?: continue
                    val code = item.optString("cca2").uppercase(Locale.ROOT)
                    if (code.isBlank()) continue
                    countryNames[code] = item.optJSONObject("name")?.optString("common") ?: code
                    val capitalArray = item.optJSONArray("capital")
                    if (capitalArray != null && capitalArray.length() > 0) capitals[code] = capitalArray.optString(0).trim().lowercase(Locale.ROOT)
                }
            }
        }
    } catch (_: Exception) { }
    val csvResponse = http.newCall(Request.Builder().url("https://davidmegginson.github.io/ourairports-data/airports.csv").build()).execute()
    val csv = csvResponse.use { if (!it.isSuccessful) error("OurAirports HTTP ${it.code}") else it.body?.string() ?: error("Leere Flughafendaten") }
    val rows = parseAirportCsv(csv)
    val parsed = rows.mapNotNull { row ->
        val type = row["type"].orEmpty()
        if (type !in setOf("large_airport", "medium_airport", "small_airport")) return@mapNotNull null
        val lat = row["latitude_deg"]?.toDoubleOrNull() ?: return@mapNotNull null
        val lon = row["longitude_deg"]?.toDoubleOrNull() ?: return@mapNotNull null
        if (kotlin.math.abs(lat) > 90 || kotlin.math.abs(lon) > 180) return@mapNotNull null
        val code = row["icao_code"].orEmpty().ifBlank { row["gps_code"].orEmpty().ifBlank { row["ident"].orEmpty() } }
        if (code.isBlank()) return@mapNotNull null
        val countryCode = row["iso_country"].orEmpty().uppercase(Locale.ROOT)
        Airport(code.uppercase(Locale.ROOT), row["iata_code"].orEmpty().uppercase(Locale.ROOT), row["name"].orEmpty().ifBlank { code }, countryNames[countryCode] ?: countryCode, row["municipality"].orEmpty(), lat, lon, row["elevation_ft"]?.toDoubleOrNull()?.toInt()?.let { "$it ft" } ?: "—", "Nicht zentral erfasst", countryCode, type)
    }.distinctBy { it.icao }
    val major = mutableMapOf<String, Airport>()
    val capital = mutableMapOf<String, Airport>()
    fun rank(type: String) = when (type) { "large_airport" -> 3; "medium_airport" -> 2; else -> 1 }
    for (airport in parsed) {
        val oldMajor = major[airport.countryCode]
        if (oldMajor == null || rank(airport.type) > rank(oldMajor.type)) major[airport.countryCode] = airport
        val city = airport.city.trim().lowercase(Locale.ROOT)
        val capitalName = capitals[airport.countryCode]
        if (capitalName != null && city == capitalName) {
            val old = capital[airport.countryCode]
            if (old == null || rank(airport.type) > rank(old.type)) capital[airport.countryCode] = airport
        }
    }
    val majorCodes = major.values.map { it.icao }.toSet()
    val capitalCodes = capital.values.map { it.icao }.toSet()
    val featured = parsed.filter { it.icao in majorCodes || it.icao in capitalCodes }.map { it.copy(isCountryMajor = it.icao in majorCodes, isCapitalAirport = it.icao in capitalCodes) }
    parsed to featured
}

private fun parseAirportCsv(text: String): List<Map<String, String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (quoted) {
            if (ch == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ }
                else quoted = false
            } else field.append(ch)
        } else when (ch) {
            '"' -> quoted = true
            ',' -> { row.add(field.toString()); field.setLength(0) }
            '\n' -> { row.add(field.toString().trimEnd('\r')); rows.add(row); row = mutableListOf(); field.setLength(0) }
            else -> field.append(ch)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString().trimEnd('\r')); rows.add(row) }
    if (rows.isEmpty()) return emptyList()
    val headers = rows.first()
    return rows.drop(1).filter { it.any(String::isNotEmpty) }.map { values -> headers.mapIndexed { index, key -> key to values.getOrElse(index) { "" } }.toMap() }
}

private suspend fun fetchPlanes(airport: Airport): List<Plane> = withContext(Dispatchers.IO) {
    val primary = "https://api.airplanes.live/v2/point/${airport.lat}/${airport.lon}/100"
    val fallback = "https://opendata.adsb.fi/api/v3/lat/${airport.lat}/lon/${airport.lon}/dist/100"
    val body = try {
        http.newCall(Request.Builder().url(primary).build()).execute().use { response ->
            if (!response.isSuccessful) error("Airplanes.live HTTP ${response.code}")
            response.body?.string() ?: "{}"
        }
    } catch (_: Exception) {
        http.newCall(Request.Builder().url(fallback).build()).execute().use { response ->
            if (!response.isSuccessful) error("Airplanes.live und adsb.fi nicht erreichbar")
            response.body?.string() ?: "{}"
        }
    }
    val root = JSONObject(body)
    val array = root.optJSONArray("ac") ?: root.optJSONArray("aircraft") ?: return@withContext emptyList()
    buildList {
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            if (!row.has("lat") || !row.has("lon")) continue
            if (row.optDouble("seen_pos", 0.0) > 120) continue
            add(Plane(row.optString("hex"), row.optString("flight").trim(), row.optString("r"), row.optString("t"), row.optDouble("lat"), row.optDouble("lon"), if (row.optString("alt_baro") == "ground") "Am Boden" else if (row.has("alt_baro")) row.optString("alt_baro") + " ft" else "Keine Angabe", if (row.has("gs")) row.optString("gs") + " kt" else "Keine Angabe", row.optDouble("track", 0.0).toFloat(), row.optString("squawk")))
        }
    }.take(500)
}


private suspend fun searchPlane(query: String): Plane? = withContext(Dispatchers.IO) {
    val endpoint = if (query.contains("-")) "reg" else "callsign"
    val safeQuery = URLEncoder.encode(query.replace(" ", ""), "UTF-8")
    val response = http.newCall(Request.Builder().url("https://api.airplanes.live/v2/" + endpoint + "/" + safeQuery).build()).execute()
    response.use {
        if (!it.isSuccessful) error("Airplanes.live HTTP " + it.code)
        val root = JSONObject(it.body?.string() ?: "{}")
        val row = root.optJSONArray("ac")?.optJSONObject(0) ?: return@withContext null
        if (!row.has("lat") || !row.has("lon")) return@withContext null
        val altitude = if (row.optString("alt_baro") == "ground") "Am Boden" else if (row.has("alt_baro")) row.optString("alt_baro") + " ft" else "Keine Angabe"
        val speed = if (row.has("gs")) row.optString("gs") + " kt" else "Keine Angabe"
        Plane(row.optString("hex"), row.optString("flight").trim(), row.optString("r"), row.optString("t"),
            row.optDouble("lat"), row.optDouble("lon"), altitude, speed, row.optDouble("track", 0.0).toFloat(), row.optString("squawk"))
    }
}
