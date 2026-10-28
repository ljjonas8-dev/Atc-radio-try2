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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class Airport(val icao: String, val name: String, val city: String, val country: String)

private val airports = listOf(
    Airport("EDDF", "Frankfurt Airport", "Frankfurt", "Deutschland"),
    Airport("EDDB", "Berlin Brandenburg", "Berlin", "Deutschland"),
    Airport("EDDM", "Munich Airport", "München", "Deutschland"),
    Airport("EGLL", "Heathrow Airport", "London", "Vereinigtes Königreich"),
    Airport("EHAM", "Amsterdam Airport Schiphol", "Amsterdam", "Niederlande"),
    Airport("KJFK", "John F. Kennedy International", "New York", "USA"),
    Airport("KLAX", "Los Angeles International", "Los Angeles", "USA"),
    Airport("KLAS", "Harry Reid International", "Las Vegas", "USA")
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
    var selectedAirport by remember { mutableStateOf(airports.first()) }
    var selectedTab by remember { mutableStateOf(Tab.OVERVIEW) }
    var mapStyle by remember { mutableStateOf("Straße") }
    val context = LocalContext.current
    val filtered = airports.filter {
        "${it.icao} ${it.name} ${it.city} ${it.country}".contains(query, ignoreCase = true)
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("ATC Live")
                        Text("Native Android · Grundversion", style = MaterialTheme.typography.labelSmall)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Flughafen suchen") },
                    placeholder = { Text("ICAO, Name oder Stadt · z. B. EDDF") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null) }
                )
                when (selectedTab) {
                    Tab.OVERVIEW -> {
                        Text("Flughäfen", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered, key = { it.icao }) { airport ->
                                AirportCard(
                                    airport = airport,
                                    selected = selectedAirport.icao == airport.icao,
                                    onClick = { selectedAirport = airport }
                                )
                            }
                        }
                    }
                    Tab.FLIGHTS -> {
                        Text("Flugbewegungen", style = MaterialTheme.typography.titleLarge)
                        AirportSummary(selectedAirport)
                        Text("Nächster Schritt: echte Ankünfte und Abflüge über eine verlässliche Datenquelle anbinden.")
                        Button(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flightradar24.com/airport/${selectedAirport.icao.lowercase()}")))
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
                                Text("Als nächstes werden Kartenkacheln, Flughafenmarker und Live-Flugzeugpositionen integriert.")
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
            Text("${airport.city}, ${airport.country}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AirportSummary(airport: Airport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(airport.name, style = MaterialTheme.typography.titleMedium)
            Text("${airport.icao} · ${airport.city}, ${airport.country}")
        }
    }
}
