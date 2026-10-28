package com.atclive.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private data class Airport(
    val icao: String,
    val name: String,
    val city: String,
    val country: String,
    val iata: String = "—",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: String = "Flughafen"
)

private val sampleAirports = listOf(
    Airport("EDDF", "Frankfurt Airport", "Frankfurt", "Deutschland", "FRA", 50.0379, 8.5622, "Großer Flughafen"),
    Airport("EDDB", "Berlin Brandenburg", "Berlin", "Deutschland", "BER", 52.3667, 13.5033, "Großer Flughafen"),
    Airport("EDDM", "Munich Airport", "München", "Deutschland", "MUC", 48.3538, 11.7861, "Großer Flughafen"),
    Airport("EGLL", "Heathrow Airport", "London", "Vereinigtes Königreich", "LHR", 51.4700, -0.4543, "Großer Flughafen"),
    Airport("EHAM", "Amsterdam Airport Schiphol", "Amsterdam", "Niederlande", "AMS", 52.3105, 4.7683, "Großer Flughafen"),
    Airport("KJFK", "John F. Kennedy International", "New York", "USA", "JFK", 40.6413, -73.7781, "Großer Flughafen"),
    Airport("KLAX", "Los Angeles International", "Los Angeles", "USA", "LAX", 33.9416, -118.4085, "Großer Flughafen"),
    Airport("KLAS", "Harry Reid International", "Las Vegas", "USA", "LAS", 36.0840, -115.1537, "Großer Flughafen")
)

private enum class Tab(val label: String) {
    OVERVIEW("Flughafen"), FLIGHTS("Flüge"), MAP("Karte"), AUDIO("Audio")
}

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
    var airportList by remember { mutableStateOf(sampleAirports) }
    var selectedAirport by remember { mutableStateOf(sampleAirports.first()) }
    var selectedTab by remember { mutableStateOf(Tab.OVERVIEW) }
    var mapStyle by remember { mutableStateOf("Straße") }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    val context = LocalContext.current

    suspend fun loadAirports() {
        isLoading = true
        loadError = null
        try {
            val loaded = withContext(Dispatchers.IO) { fetchWorldwideAirports() }
            if (loaded.isNotEmpty()) {
                airportList = loaded
                selectedAirport = loaded.firstOrNull { it.icao == selectedAirport.icao }
                    ?: loaded.firstOrNull { it.icao == "EDDF" }
                    ?: loaded.first()
            } else {
                loadError = "Die Datenquelle hat keine passenden Flughäfen geliefert. Die Beispieldaten bleiben verfügbar."
            }
        } catch (e: Exception) {
            val detail = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
            loadError = "Flughafendaten konnten von den Datenquellen nicht geladen werden. Technischer Fehler: $detail"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(reloadToken) { loadAirports() }

    val filtered = remember(airportList, query) {
        if (query.isBlank()) airportList.take(60)
        else airportList.asSequence()
            .filter {
                "${it.icao} ${it.iata} ${it.name} ${it.city} ${it.country}"
                    .contains(query.trim(), ignoreCase = true)
            }
            .take(100)
            .toList()
    }

    val atcColors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFB7F34A),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF17211A),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFF30472A),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE7F9D4),
        secondary = androidx.compose.ui.graphics.Color(0xFF59D6D1),
        onSecondary = androidx.compose.ui.graphics.Color(0xFF062B2A),
        background = androidx.compose.ui.graphics.Color(0xFF0B1118),
        onBackground = androidx.compose.ui.graphics.Color(0xFFEAF0F5),
        surface = androidx.compose.ui.graphics.Color(0xFF141E28),
        onSurface = androidx.compose.ui.graphics.Color(0xFFEAF0F5),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF202D39),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB5C2CD),
        outline = androidx.compose.ui.graphics.Color(0xFF40515F)
    )
    MaterialTheme(colorScheme = atcColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(color = androidx.compose.ui.graphics.Color(0xFF111B25)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ATC LIVE", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
                            Text("GLOBAL AVIATION INTELLIGENCE", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = androidx.compose.ui.graphics.Color(0xFF263B29)
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(Modifier.padding(top = 5.dp).height(6.dp), color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {}
                                Text(if (isLoading) "SYNC" else "SYSTEM ONLINE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF111B25),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Tab.entries.forEach { tab ->
                        val icon = when (tab) {
                            Tab.OVERVIEW -> Icons.Default.Air
                            Tab.FLIGHTS -> Icons.Default.AirplanemodeActive
                            Tab.MAP -> Icons.Default.Map
                            Tab.AUDIO -> Icons.Default.Headphones
                        }
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { androidx.compose.material3.Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF30472A),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("AIRPORT SEARCH") },
                    placeholder = { Text("ICAO, IATA, Name oder Stadt") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (isLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Synchronisiere Flughafendaten …", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                loadError?.let { message ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF392622))) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(message, style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { reloadToken++ }) { Text("Erneut versuchen") }
                        }
                    }
                }
                when (selectedTab) {
                    Tab.OVERVIEW -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("AIRPORTS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                Text("Flughafen entdecken", style = MaterialTheme.typography.headlineSmall)
                            }
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text("${airportList.size} geladen", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        AirportSummary(selectedAirport)
                        Text("ALLE STANDORTE", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.icao + it.name }) { airport ->
                                AirportCard(
                                    airport = airport,
                                    selected = selectedAirport.icao == airport.icao,
                                    onClick = { selectedAirport = airport }
                                )
                            }
                            if (filtered.isEmpty()) {
                                item { Text("Keine Treffer. Suche nach ICAO-/IATA-Code, Name oder Stadt.") }
                            }
                        }
                    }
                    Tab.FLIGHTS -> {
                        Text("FLIGHT OPERATIONS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text("Flugbewegungen", style = MaterialTheme.typography.headlineSmall)
                        AirportSummary(selectedAirport)
                        Text("Live-Ankünfte und -Abflüge sind noch nicht direkt angebunden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = {
                            val code = selectedAirport.iata.takeIf { it.matches(Regex("[A-Z]{3}")) } ?: selectedAirport.icao
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flightradar24.com/data/airports/${code.lowercase(Locale.ROOT)}")))
                        }) { Text("Flüge extern öffnen") }
                    }
                    Tab.MAP -> {
                        Text("LIVE MONITOR", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text("Flughafenkarte", style = MaterialTheme.typography.headlineSmall)
                        AirportSummary(selectedAirport)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Straße", "Satellit").forEach { style ->
                                FilterChip(
                                    selected = mapStyle == style,
                                    onClick = { mapStyle = style },
                                    label = { Text(style) }
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            key(selectedAirport.icao, selectedAirport.latitude, selectedAirport.longitude, mapStyle) {
                                AirportMap(airport = selectedAirport, mapStyle = mapStyle, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    Tab.AUDIO -> {
                        Text("RADIO FREQUENCY", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text("ATC-Audio", style = MaterialTheme.typography.headlineSmall)
                        AirportSummary(selectedAirport)
                        Text("Audio-Streams werden noch mit den verfügbaren Quellen verbunden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flightradar24.com/blog/airport-live-streams/")))
                        }) { Text("Airport-Livestreams öffnen") }
                    }
                }
            }
        }
    }
}

 
@Composable
private fun AirportMap(airport: Airport, mapStyle: String, modifier: Modifier = Modifier) {
    val latitude = airport.latitude ?: 52.3667
    val longitude = airport.longitude ?: 13.5033
    val mapType = if (mapStyle == "Satellit") "k" else "m"
    val mapHtml = remember(airport.icao, latitude, longitude, mapType) {
        val url = "https://maps.google.com/maps?q=$latitude,$longitude&z=12&t=$mapType&output=embed"
        """
        <!doctype html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>html,body,iframe{margin:0;padding:0;width:100%;height:100%;border:0;overflow:hidden;background:#eaf1f8}</style>
        </head><body><iframe title="Google Maps" src="$url" allowfullscreen loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe></body></html>
        """.trimIndent()
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                setBackgroundColor(android.graphics.Color.rgb(234, 241, 248))
                loadDataWithBaseURL("https://maps.google.com/", mapHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != mapHtml) {
                webView.tag = mapHtml
                webView.loadDataWithBaseURL("https://maps.google.com/", mapHtml, "text/html", "UTF-8", null)
            }
        }
    )
}

@Composable
private fun AirportCard(airport: Airport, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) androidx.compose.ui.graphics.Color(0xFF263B29) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(airport.icao, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(airport.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("${airport.city}, ${airport.country}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("IATA  ${airport.iata}     •     ${airport.type}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            if (selected) {
                androidx.compose.material3.Icon(Icons.Default.AirplanemodeActive, contentDescription = "Ausgewählt", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
@Composable
private fun AirportSummary(airport: Airport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SELECTED AIRPORT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Text(airport.name, style = MaterialTheme.typography.titleLarge)
            Text("${airport.city}, ${airport.country}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("ICAO  ${airport.icao}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("IATA  ${airport.iata}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(airport.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
private fun fetchWorldwideAirports(): List<Airport> {
    val sources = listOf(
        "https://airportmiles.com/data/airports.json",
        "https://aerocodes.org/data/airports.json"
    )
    var lastError: Exception? = null
    for (source in sources) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(source).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) ATCLive/0.3")
                instanceFollowRedirects = true
            }
            connection.connect()
            if (connection.responseCode !in 200..299) error("$source antwortet mit HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObjectOrArray(body)
            val rows = when (root) {
                is JSONArray -> root
                is JSONObject -> root.optJSONArray("airports")
                    ?: root.optJSONArray("data")
                    ?: JSONArray()
                else -> JSONArray()
            }
            val airports = ArrayList<Airport>(rows.length())
            val seen = HashSet<String>()
            for (i in 0 until rows.length()) {
                val item = rows.optJSONObject(i) ?: continue
                val iata = item.optString("iata", item.optString("iata_code", "—")).trim().uppercase(Locale.ROOT).ifBlank { "—" }
                val icao = firstNonBlank(
                    item.optString("icao", ""),
                    item.optString("icao_code", ""),
                    item.optString("ident", ""),
                    item.optString("gps_code", ""),
                    item.optString("local_code", ""),
                    iata.takeIf { it != "—" } ?: ""
                ).uppercase(Locale.ROOT)
                val name = firstNonBlank(item.optString("name", ""), item.optString("name_international", ""), item.optString("served_city", ""), icao)
                val city = firstNonBlank(item.optString("served_city", ""), item.optString("municipality", ""), item.optString("city", ""), name)
                val country = firstNonBlank(item.optString("country_name", ""), item.optString("country", ""), "Land unbekannt")
                val latitude = item.optDouble("latitude", Double.NaN).takeIf { it.isFinite() }
                    ?: item.optDouble("latitude_deg", Double.NaN).takeIf { it.isFinite() }
                    ?: item.optDouble("lat", Double.NaN).takeIf { it.isFinite() }
                val longitude = item.optDouble("longitude", Double.NaN).takeIf { it.isFinite() }
                    ?: item.optDouble("longitude_deg", Double.NaN).takeIf { it.isFinite() }
                    ?: item.optDouble("lon", Double.NaN).takeIf { it.isFinite() }
                if (icao.isBlank() || name.isBlank() || latitude == null || longitude == null ||
                    latitude !in -90.0..90.0 || longitude !in -180.0..180.0 || !seen.add(icao)) continue
                val size = item.optString("size", "")
                val type = when (size) {
                    "large" -> "Großer Flughafen"
                    "medium" -> "Mittlerer Flughafen"
                    "small" -> "Kleiner Flughafen"
                    else -> item.optString("type", "Flughafen").replace('_', ' ')
                }
                airports.add(Airport(icao, name, city, country, iata, latitude, longitude, type))
            }
            if (airports.isNotEmpty()) {
                return airports.sortedWith(compareBy<Airport> { it.icao != "EDDF" }.thenBy { it.name })
            }
            lastError = IllegalStateException("$source lieferte keine verwertbaren Flughafendaten")
        } catch (e: Exception) {
            lastError = e
        } finally {
            connection?.disconnect()
        }
    }
    throw IllegalStateException("Alle Flughafendatenquellen fehlgeschlagen: ${lastError?.message ?: "unbekannter Fehler"}", lastError)
}

private fun JSONObjectOrArray(body: String): Any {
    val trimmed = body.trim()
    return if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
}

private fun firstNonBlank(vararg values: String): String =
    values.firstOrNull { it.isNotBlank() && it != "null" }?.trim().orEmpty()
