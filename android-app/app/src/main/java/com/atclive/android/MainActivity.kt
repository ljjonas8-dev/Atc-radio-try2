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

private data class Airport(val icao: String, val iata: String, val name: String, val country: String, val city: String, val lat: Double, val lon: Double, val elevation: String = "—", val terminals: String = "Keine Angabe") {
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
    var query by remember { mutableStateOf("") }
    var flightQuery by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(airports.first()) }
    var planes by remember { mutableStateOf(emptyList<Plane>()) }
    var chosenPlane by remember { mutableStateOf<Plane?>(null) }
    var status by remember { mutableStateOf("Lade Live-Flugzeuge …") }
    var flightStatus by remember { mutableStateOf("Live-Suche: Rufzeichen oder Registrierung") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Übersicht") }
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(selected.position, 6.5f) }
    val matches = remember(query) { airports.filter { listOf(it.icao,it.iata,it.name,it.city,it.country).any { value -> value.contains(query, true) } } }

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
                item {
                    Text(selected.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${selected.icao} / ${selected.iata} · ${selected.city}, ${selected.country}", color = Muted)
                }
                if (tab == "Karte" || tab == "Übersicht") {
                    item {
                        GoogleMap(modifier = Modifier.fillMaxWidth().height(420.dp), cameraPositionState = camera, properties = MapProperties(mapType = MapType.SATELLITE), uiSettings = MapUiSettings(zoomControlsEnabled = true)) {
                            Marker(state = MarkerState(position = selected.position), title = selected.name, snippet = "${selected.icao} · Höhe ${selected.elevation}")
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
                    Text("Live-Suche erfordert eine aktuelle ADS-B-Kennung; geplante Flüge sind davon getrennt.", color = Muted)
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

private suspend fun fetchPlanes(airport: Airport): List<Plane> = withContext(Dispatchers.IO) {
    val url = "https://api.airplanes.live/v2/point/${airport.lat}/${airport.lon}/100"
    val response = http.newCall(Request.Builder().url(url).build()).execute()
    response.use {
        if (!it.isSuccessful) error("Airplanes.live HTTP ${it.code}")
        val root = JSONObject(it.body?.string() ?: "{}")
        val array = root.optJSONArray("ac") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until array.length()) {
                val row = array.optJSONObject(i) ?: continue
                if (!row.has("lat") || !row.has("lon")) continue
                val seen = row.optDouble("seen_pos", 0.0)
                if (seen > 120) continue
                add(Plane(row.optString("hex"), row.optString("flight").trim(), row.optString("r"), row.optString("t"), row.optDouble("lat"), row.optDouble("lon"), if (row.optString("alt_baro") == "ground") "Am Boden" else if (row.has("alt_baro")) row.optString("alt_baro") + " ft" else "Keine Angabe", if (row.has("gs")) row.optString("gs") + " kt" else "Keine Angabe", row.optDouble("track", 0.0).toFloat(), row.optString("squawk")))
            }
        }.take(500)
    }
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
