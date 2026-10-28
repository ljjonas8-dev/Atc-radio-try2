package com.atclive.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
        } catch (_: Exception) {
            loadError = "Weltweite Flughafendaten konnten nicht geladen werden. Prüfe die Internetverbindung oder lade erneut."
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

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("ATC Live")
                        Text(
                            if (isLoading) "Lade weltweite Flughäfen …" else "Weltweite Flughafensuche",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                })
            },
            bottomBar = {
                NavigationBar {
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
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Flughafen suchen") },
                    placeholder = { Text("ICAO, IATA, Name oder Stadt · z. B. LHR") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null) }
                )
                if (isLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Flughafendaten werden geladen …")
                    }
                }
                loadError?.let { message ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(message, style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { reloadToken++ }) {
                                Text("Erneut laden")
                            }
                        }
                    }
                }
                when (selectedTab) {
                    Tab.OVERVIEW -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Flughäfen", style = MaterialTheme.typography.titleMedium)
                            Text("${airportList.size} geladen", style = MaterialTheme.typography.labelMedium)
                        }
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text("Flugbewegungen", style = MaterialTheme.typography.titleLarge)
                        AirportSummary(selectedAirport)
                        Text("Die Live-Ankünfte und -Abflüge werden als nächster Schritt direkt angebunden.")
                        Button(onClick = {
                            val code = selectedAirport.iata.takeIf { it.matches(Regex("[A-Z]{3}")) }
                                ?: selectedAirport.icao
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flightradar24.com/data/airports/${code.lowercase(Locale.ROOT)}")))
                        }) { Text("Flüge extern öffnen") }
                    }
                    Tab.MAP -> {
                        Text("Flugzeugkarte", style = MaterialTheme.typography.titleLarge)
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
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(if (mapStyle == "Straße") "Straßenkarte" else "Satellitenkarte", style = MaterialTheme.typography.titleLarge)
                                Text("Kartendarstellung: Auswahl und Bildschirm sind vorbereitet.")
                                Text("Kartendaten und Live-Flugzeugpositionen folgen im nächsten Ausbau.")
                                Text("Ausgewählter Flughafen: ${selectedAirport.icao}")
                            }
                        }
                    }
                    Tab.AUDIO -> {
                        Text("ATC-Audio", style = MaterialTheme.typography.titleLarge)
                        AirportSummary(selectedAirport)
                        Text("Audio-Streams werden im nächsten Schritt mit den vorhandenen Website-Quellen abgeglichen.")
                        Button(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flightradar24.com/blog/airport-live-streams/")))
                        }) { Text("Offizielle Airport-Livestreams öffnen") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AirportCard(airport: Airport, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${airport.icao} · ${airport.name}", style = MaterialTheme.typography.titleMedium)
            Text("${airport.city}, ${airport.country} · ${airport.iata}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AirportSummary(airport: Airport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(airport.name, style = MaterialTheme.typography.titleMedium)
            Text("${airport.icao} / ${airport.iata} · ${airport.city}, ${airport.country}")
            Text(airport.type, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun fetchWorldwideAirports(): List<Airport> {
    val connection = (URL("https://airportmiles.com/data/airports.json").openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 25_000
        requestMethod = "GET"
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "ATCLive-Android/0.2")
    }
    try {
        connection.connect()
        if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObjectOrArray(body)
        val rows = when (root) {
            is JSONArray -> root
            is JSONObject -> when {
                root.optJSONArray("airports") != null -> root.optJSONArray("airports")!!
                root.optJSONArray("data") != null -> root.optJSONArray("data")!!
                else -> JSONArray()
            }
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
            val name = firstNonBlank(item.optString("name", ""), item.optString("served_city", ""), icao)
            val city = firstNonBlank(item.optString("served_city", ""), item.optString("municipality", ""), item.optString("city", ""), name)
            val country = firstNonBlank(item.optString("country_name", ""), item.optString("country", ""), "Land unbekannt")
            val latitude = item.optDouble("latitude", Double.NaN).takeIf { it.isFinite() }
                ?: item.optDouble("latitude_deg", Double.NaN).takeIf { it.isFinite() }
            val longitude = item.optDouble("longitude", Double.NaN).takeIf { it.isFinite() }
                ?: item.optDouble("longitude_deg", Double.NaN).takeIf { it.isFinite() }
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
        return airports.sortedWith(compareBy<Airport> { it.icao != "EDDF" }.thenBy { it.name })
    } finally {
        connection.disconnect()
    }
}

private fun JSONObjectOrArray(body: String): Any {
    val trimmed = body.trim()
    return if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
}

private fun firstNonBlank(vararg values: String): String =
    values.firstOrNull { it.isNotBlank() && it != "null" }?.trim().orEmpty()
