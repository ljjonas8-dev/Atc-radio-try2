import SwiftUI
import MapKit

private enum ATCStyle {
    static let background = Color(red: 7/255, green: 19/255, blue: 33/255)
    static let panel = Color(red: 16/255, green: 34/255, blue: 53/255)
    static let line = Color(red: 33/255, green: 59/255, blue: 80/255)
    static let green = Color(red: 98/255, green: 243/255, blue: 165/255)
    static let blue = Color(red: 112/255, green: 186/255, blue: 255/255)
    static let muted = Color(red: 155/255, green: 177/255, blue: 197/255)
}

struct Airport: Identifiable, Hashable {
    let icao: String
    let iata: String
    let name: String
    let country: String
    let city: String
    let latitude: Double
    let longitude: Double
    let elevation: String
    let type: String
    let terminals: String
    let countryCode: String
    var id: String { icao }

    init(icao: String, iata: String, name: String, country: String, city: String, latitude: Double, longitude: Double, elevation: String, type: String, terminals: String, countryCode: String = "") {
        self.icao = icao; self.iata = iata; self.name = name; self.country = country; self.city = city
        self.latitude = latitude; self.longitude = longitude; self.elevation = elevation; self.type = type
        self.terminals = terminals; self.countryCode = countryCode
    }

    static let featured: [Airport] = [
        .init(icao:"EDDF",iata:"FRA",name:"Frankfurt am Main",country:"Deutschland",city:"Frankfurt",latitude:50.0379,longitude:8.5622,elevation:"111 m",type:"Großer Verkehrsflughafen",terminals:"2"),
        .init(icao:"EDDB",iata:"BER",name:"Berlin Brandenburg",country:"Deutschland",city:"Berlin",latitude:52.3667,longitude:13.5033,elevation:"48 m",type:"Internationaler Flughafen",terminals:"2"),
        .init(icao:"EDDM",iata:"MUC",name:"München",country:"Deutschland",city:"München",latitude:48.3538,longitude:11.7861,elevation:"453 m",type:"Internationaler Flughafen",terminals:"2"),
        .init(icao:"EHAM",iata:"AMS",name:"Amsterdam Schiphol",country:"Niederlande",city:"Amsterdam",latitude:52.3105,longitude:4.7683,elevation:"−3 m",type:"Internationaler Flughafen",terminals:"1"),
        .init(icao:"EGLL",iata:"LHR",name:"London Heathrow",country:"Vereinigtes Königreich",city:"London",latitude:51.4700,longitude:-0.4543,elevation:"25 m",type:"Internationaler Flughafen",terminals:"4"),
        .init(icao:"KJFK",iata:"JFK",name:"New York JFK",country:"USA",city:"New York",latitude:40.6413,longitude:-73.7781,elevation:"4 m",type:"Internationaler Flughafen",terminals:"6"),
        .init(icao:"KLAX",iata:"LAX",name:"Los Angeles",country:"USA",city:"Los Angeles",latitude:33.9416,longitude:-118.4085,elevation:"38 m",type:"Internationaler Flughafen",terminals:"9"),
        .init(icao:"KLAS",iata:"LAS",name:"Harry Reid International",country:"USA",city:"Las Vegas",latitude:36.0840,longitude:-115.1537,elevation:"665 m",type:"Internationaler Flughafen",terminals:"2"),
        .init(icao:"KSFO",iata:"SFO",name:"San Francisco International",country:"USA",city:"San Francisco",latitude:37.6213,longitude:-122.3790,elevation:"4 m",type:"Internationaler Flughafen",terminals:"4"),
        .init(icao:"TNCM",iata:"SXM",name:"Princess Juliana",country:"Sint Maarten",city:"Sint Maarten",latitude:18.0409,longitude:-63.1089,elevation:"4 m",type:"Internationaler Flughafen",terminals:"1")
    ]
}

enum ATCTab: String, CaseIterable, Identifiable {
    case overview = "Übersicht"
    case flights = "Flüge"
    case map = "Karte"
    case audio = "ATC-Audio"
    var id: String { rawValue }
    var symbol: String {
        switch self {
        case .overview: "airplane"
        case .flights: "arrow.left.arrow.right"
        case .map: "map"
        case .audio: "dot.radiowaves.left.and.right"
        }
    }
}

@MainActor
final class AirportStore: ObservableObject {
    @Published var airports = Airport.featured
    @Published var featuredAirports = Airport.featured
    @Published var isLoading = false
    @Published var status = "Beispielauswahl geladen"
    private let airportsURL = URL(string: "https://davidmegginson.github.io/ourairports-data/airports.csv")!
    private let countriesURL = URL(string: "https://restcountries.com/v3.1/all?fields=cca2,capital")!

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            var request = URLRequest(url: airportsURL)
            request.timeoutInterval = 35
            request.cachePolicy = .reloadRevalidatingCacheData
            let (data, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200,
                  let csv = String(data: data, encoding: .utf8) else { throw URLError(.badServerResponse) }
            let rows = Self.parseCSV(csv)
            var countryNames: [String: String] = [:]
            var capitals: [String: String] = [:]
            if let (countryData, countryResponse) = try? await URLSession.shared.data(from: countriesURL),
               (countryResponse as? HTTPURLResponse)?.statusCode == 200,
               let countryRows = try? JSONSerialization.jsonObject(with: countryData) as? [[String: Any]] {
                for row in countryRows {
                    guard let code = row["cca2"] as? String else { continue }
                    if let names = row["capital"] as? [String], let capital = names.first {
                        capitals[code.uppercased()] = capital.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
                    }
                }
            }
            let parsed = rows.compactMap { row -> Airport? in
                let kind = row["type"] ?? ""
                guard ["large_airport", "medium_airport", "small_airport"].contains(kind),
                      let lat = Double(row["latitude_deg"] ?? ""), let lon = Double(row["longitude_deg"] ?? ""),
                      abs(lat) <= 90, abs(lon) <= 180 else { return nil }
                let code = (row["icao_code"]?.isEmpty == false ? row["icao_code"] : nil)
                    ?? (row["gps_code"]?.isEmpty == false ? row["gps_code"] : nil)
                    ?? (row["ident"] ?? "")
                guard !code.isEmpty else { return nil }
                let countryCode = row["iso_country"] ?? ""
                let country = countryNames[countryCode] ?? countryCode
                let municipality = row["municipality"] ?? ""
                let elev = row["elevation_ft"].flatMap(Int.init).map { "\($0) ft" } ?? "—"
                return Airport(icao: code.uppercased(), iata: (row["iata_code"] ?? "").uppercased(), name: row["name"] ?? code, country: country, city: municipality, latitude: lat, longitude: lon, elevation: elev, type: kind, terminals: "Nicht zentral erfasst", countryCode: countryCode.uppercased())
            }
            guard !parsed.isEmpty else { throw URLError(.cannotParseResponse) }
            airports = parsed.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }

            var majorByCountry: [String: Airport] = [:]
            var capitalByCountry: [String: Airport] = [:]
            func rank(_ type: String) -> Int { type == "large_airport" ? 3 : (type == "medium_airport" ? 2 : 1) }
            for airport in parsed {
                let code = airport.countryCode
                if let old = majorByCountry[code] {
                    if rank(airport.type) > rank(old.type) || (rank(airport.type) == rank(old.type) && airport.iata != "—" && old.iata.isEmpty) { majorByCountry[code] = airport }
                } else { majorByCountry[code] = airport }
                if let capital = capitals[code],
                   airport.city.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current) == capital {
                    if let old = capitalByCountry[code] {
                        if rank(airport.type) > rank(old.type) { capitalByCountry[code] = airport }
                    } else { capitalByCountry[code] = airport }
                }
            }
            var selected: [String: Airport] = [:]
            for airport in majorByCountry.values { selected[airport.icao] = airport }
            for airport in capitalByCountry.values { selected[airport.icao] = airport }
            featuredAirports = selected.values.sorted { $0.name < $1.name }
            status = "OurAirports: \(airports.count.formatted()) Flughäfen · hervorgehoben: große Kategorie + Hauptstadt"
        } catch {
            status = "Weltweite Daten nicht erreichbar – Beispielauswahl aktiv"
        }
    }

    private static func parseCSV(_ text: String) -> [[String: String]] {
        var rows: [[String: String]] = []
        var row: [String] = []
        var field = ""
        var quoted = false
        let chars = Array(text)
        var i = 0
        while i < chars.count {
            let ch = chars[i]
            if quoted {
                if ch == "\"" {
                    if i + 1 < chars.count && chars[i + 1] == "\"" { field.append("\""); i += 1 }
                    else { quoted = false }
                } else { field.append(ch) }
            } else if ch == "\"" { quoted = true }
            else if ch == "," { row.append(field); field = "" }
            else if ch == "\n" {
                row.append(field.trimmingCharacters(in: .newlines)); rows.append(row); row = []; field = ""
            } else { field.append(ch) }
            i += 1
        }
        if !field.isEmpty || !row.isEmpty { row.append(field); rows.append(row) }
        guard let headerRow = rows.first else { return [] }
        let headers = headerRow.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        return rows.dropFirst().filter { $0.contains(where: { !$0.isEmpty }) }.map { values in
            Dictionary(uniqueKeysWithValues: headers.enumerated().map { index, key in
                (key, index < values.count ? values[index].trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "\r")) : "")
            })
        }
    }
}

private struct LiveAircraft: Identifiable {
    let hex: String; let callsign: String; let registration: String; let type: String
    let latitude: Double; let longitude: Double; let altitude: Int?; let speed: Int?; let track: Int?; let squawk: String
    var id: String { hex.isEmpty ? callsign + String(latitude) : hex }
    var altitudeText: String { altitude.map { String($0) + " ft" } ?? "Keine Angabe" }
}

struct ContentView: View {
    @StateObject private var store = AirportStore()
    @State private var selectedAirport = Airport.featured[0]
    @State private var selectedTab: ATCTab = .overview
    @State private var search = ""
    @State private var mapStyle = 0
    @State private var mapPosition: MapCameraPosition = .region(MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: Airport.featured[0].latitude, longitude: Airport.featured[0].longitude), span: MKCoordinateSpan(latitudeDelta: 1.8, longitudeDelta: 1.8)))
    @State private var liveAircraft: [LiveAircraft] = []
    @State private var selectedAircraft: LiveAircraft?
    @State private var aircraftStatus = "Live-Positionen werden geladen …"
    @State private var flightSearch = ""
    @State private var flightSearchStatus = "Suche nach aktuellem Rufzeichen oder Registrierung"

    private var filtered: [Airport] {
        guard !search.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return store.airports }
        return store.airports.filter {
            $0.name.localizedCaseInsensitiveContains(search) ||
            $0.icao.localizedCaseInsensitiveContains(search) ||
            $0.iata.localizedCaseInsensitiveContains(search) ||
            $0.city.localizedCaseInsensitiveContains(search) ||
            $0.country.localizedCaseInsensitiveContains(search)
        }
    }

    var body: some View {
        NavigationSplitView {
            sidebar
                .navigationTitle("ATC LIVE")
                .toolbarBackground(ATCStyle.background, for: .navigationBar)
        } detail: {
            detail
                .background(ATCStyle.background)
                .toolbarBackground(ATCStyle.background, for: .navigationBar)
        }
        .tint(ATCStyle.green)
        .task { await store.load() }
    }

    private var sidebar: some View {
        VStack(spacing: 14) {
            HStack(spacing: 10) {
                Image(systemName: "airplane")
                    .font(.title2.bold()).foregroundStyle(ATCStyle.background)
                    .frame(width: 42, height: 42).background(ATCStyle.green, in: RoundedRectangle(cornerRadius: 13))
                VStack(alignment: .leading, spacing: 3) {
                    Text("ATC LIVE").font(.headline).tracking(2)
                    Text("GLOBAL AVIATION").font(.caption2).tracking(1.5).foregroundStyle(ATCStyle.green)
                }
                Spacer()
            }.padding(.top, 10)

            HStack {
                Image(systemName: "magnifyingglass").foregroundStyle(ATCStyle.muted)
                TextField("Flughafen suchen", text: $search)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
            }
            .padding(12).background(ATCStyle.background, in: RoundedRectangle(cornerRadius: 13))
            .overlay(RoundedRectangle(cornerRadius: 13).stroke(ATCStyle.line))

            VStack(alignment: .leading, spacing: 6) {
                Text("BEREICHE").font(.caption.bold()).tracking(1.5).foregroundStyle(ATCStyle.muted).padding(.horizontal, 5)
                ForEach(ATCTab.allCases) { tab in
                    Button { selectedTab = tab } label: {
                        HStack(spacing: 12) {
                            Image(systemName: tab.symbol).frame(width: 22)
                            Text(tab.rawValue)
                            Spacer()
                            if selectedTab == tab { Circle().fill(ATCStyle.green).frame(width: 7, height: 7) }
                        }
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(selectedTab == tab ? ATCStyle.green : ATCStyle.muted)
                        .padding(12)
                        .background(selectedTab == tab ? ATCStyle.green.opacity(0.12) : .clear, in: RoundedRectangle(cornerRadius: 12))
                    }.buttonStyle(.plain)
                }
            }

            HStack {
                Circle().fill(ATCStyle.green).frame(width: 8, height: 8)
                Text(store.isLoading ? "DATEN WERDEN GELADEN" : store.status.uppercased())
                    .font(.system(size: 9, weight: .semibold, design: .monospaced))
                    .foregroundStyle(ATCStyle.muted).lineLimit(2)
            }.frame(maxWidth: .infinity, alignment: .leading).padding(.top, 4)
            Divider().overlay(ATCStyle.line)
            HStack {
                Text("FLUGHÄFEN").font(.caption.bold()).tracking(1.5).foregroundStyle(ATCStyle.muted)
                Spacer()
                Text("\(filtered.count)").font(.caption.monospacedDigit()).foregroundStyle(ATCStyle.green)
            }
            List(selection: Binding(get: { selectedAirport.id }, set: { id in
                if let airport = filtered.first(where: { $0.id == id }) { selectedAirport = airport }
            })) {
                ForEach(filtered) { airport in
                    Button { selectedAirport = airport } label: {
                        HStack(spacing: 11) {
                            Text(airport.icao).font(.system(.caption, design: .monospaced).bold())
                                .foregroundStyle(ATCStyle.green).frame(width: 48, height: 38)
                                .background(ATCStyle.background, in: RoundedRectangle(cornerRadius: 9))
                            VStack(alignment: .leading, spacing: 3) {
                                Text(airport.name).font(.subheadline.weight(.semibold)).lineLimit(1)
                                Text("\(airport.city) · \(airport.iata.isEmpty ? "—" : airport.iata)").font(.caption).foregroundStyle(ATCStyle.muted).lineLimit(1)
                            }
                        }.padding(.vertical, 3)
                    }.buttonStyle(.plain).listRowBackground(selectedAirport.id == airport.id ? ATCStyle.green.opacity(0.10) : ATCStyle.panel)
                }
            }
            .scrollContentBackground(.hidden)
            .listStyle(.plain)
        }
        .padding(.horizontal, 14)
        .background(ATCStyle.background)
        .navigationSplitViewColumnWidth(min: 280, ideal: 320, max: 390)
    }

    private var detail: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 7) {
                        Text(selectedTab == .overview ? "AIRPORT INTELLIGENCE" : selectedTab.rawValue.uppercased())
                            .font(.caption.bold()).tracking(2).foregroundStyle(ATCStyle.green)
                        Text(selectedAirport.name).font(.system(size: 32, weight: .bold, design: .rounded)).tracking(-0.8)
                        Text([selectedAirport.city, selectedAirport.country].filter { !$0.isEmpty }.joined(separator: " · "))
                            .foregroundStyle(ATCStyle.muted)
                    }
                    Spacer(minLength: 16)
                    Text(selectedAirport.icao).font(.system(.title3, design: .monospaced).bold())
                        .foregroundStyle(ATCStyle.green).padding(.horizontal, 15).padding(.vertical, 11)
                        .background(ATCStyle.panel, in: RoundedRectangle(cornerRadius: 14))
                }

                switch selectedTab {
                case .overview: overview
                case .flights: flights
                case .map: airportMap
                case .audio: audio
                }

                Text("ATC Live ist ein unabhängiges Hobbyprojekt. Daten können unvollständig oder verzögert sein und sind nicht für Flugentscheidungen geeignet.")
                    .font(.caption).foregroundStyle(ATCStyle.muted).padding(.top, 6)
            }
            .padding(24)
            .frame(maxWidth: 1100, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .center)
        }
        .background(
            RadialGradient(colors: [Color(red: 0.06, green: 0.15, blue: 0.23), ATCStyle.background], center: .topLeading, startRadius: 20, endRadius: 850)
        )
        .navigationTitle(selectedAirport.icao)
    }

    private var overview: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 14) {
                metric("ICAO / IATA", value: "\(selectedAirport.icao) / \(selectedAirport.iata.isEmpty ? "—" : selectedAirport.iata)", symbol: "number")
                metric("HÖHE ÜBER MEER", value: selectedAirport.elevation, symbol: "mountain.2")
                metric("TERMINALS", value: selectedAirport.terminals, symbol: "building.2")
            }
            HStack(spacing: 14) {
                metric("FLUGHAFENTYP", value: selectedAirport.type, symbol: "airplane.departure")
                metric("POSITION", value: String(format: "%.4f, %.4f", selectedAirport.latitude, selectedAirport.longitude), symbol: "location")
            }
            HStack(spacing: 12) {
                Image(systemName: "info.circle.fill").foregroundStyle(ATCStyle.blue)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Flughafen-Stammdaten").font(.headline)
                    Text("Die Daten werden nach Möglichkeit von airportmiles.com geladen. Für bestätigte Terminals, Startbahnen und Flugstatus sind separate strukturierte Datenquellen nötig.")
                        .font(.subheadline).foregroundStyle(ATCStyle.muted)
                }
            }.padding(18).frame(maxWidth: .infinity, alignment: .leading).atcCard()
        }
    }

    private var flights: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Aktuelle Ankünfte und Abflüge").font(.title2.bold())
            Text("Ein verlässlicher direkter Flugstatus benötigt einen geeigneten Anbieter und gegebenenfalls einen API-Schlüssel. Bis dahin öffnen diese Links die aktuelle externe Flughafenübersicht.")
                .foregroundStyle(ATCStyle.muted)
            HStack(spacing: 12) {
                externalButton("Ankünfte live", symbol: "airplane.arrival", url: "https://www.flightaware.com/live/airport/\(selectedAirport.icao)/arrivals")
                externalButton("Abflüge live", symbol: "airplane.departure", url: "https://www.flightaware.com/live/airport/\(selectedAirport.icao)/departures")
            }
            HStack(spacing: 12) {
                externalButton("Flightradar24", symbol: "dot.radiowaves.left.and.right", url: "https://www.flightradar24.com/data/airports/\(selectedAirport.iata.lowercased())")
                externalButton("Live-Radar", symbol: "scope", url: "https://globe.airplanes.live/?airport=\(selectedAirport.icao)&hideSidebar")
            }
        }.atcCard()
    }

    private var airportMap: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Label("Live-Flugradar", systemImage: "map.fill").font(.title2.bold())
                Spacer()
                Picker("Kartentyp", selection: $mapStyle) { Text("Standard").tag(0); Text("Satellit").tag(1) }.pickerStyle(.segmented).frame(maxWidth: 240)
            }
            Map(position: $mapPosition) {
                Marker(selectedAirport.name, coordinate: CLLocationCoordinate2D(latitude: selectedAirport.latitude, longitude: selectedAirport.longitude)).tint(ATCStyle.green)
                ForEach(store.featuredAirports) { airport in
                    Annotation(airport.icao, coordinate: CLLocationCoordinate2D(latitude: airport.latitude, longitude: airport.longitude)) {
                        Button { selectedAirport = airport; selectedAircraft = nil } label: {
                            Image(systemName: "building.2.crop.circle.fill").font(.system(size: 18)).foregroundStyle(ATCStyle.blue).padding(4).background(ATCStyle.background.opacity(0.85), in: Circle())
                        }.buttonStyle(.plain).accessibilityLabel(airport.name)
                    }
                }
                ForEach(liveAircraft) { aircraft in
                    Annotation(aircraft.callsign.isEmpty ? aircraft.hex : aircraft.callsign, coordinate: CLLocationCoordinate2D(latitude: aircraft.latitude, longitude: aircraft.longitude)) {
                        Button { selectedAircraft = aircraft } label: { Image(systemName: "airplane").rotationEffect(.degrees(Double(aircraft.track ?? 0))).foregroundStyle(ATCStyle.background).padding(7).background(ATCStyle.green, in: Circle()) }
                            .buttonStyle(.plain)
                    }
                }
            }
            .mapStyle(mapStyle == 0 ? .standard(elevation: .realistic) : .imagery(elevation: .realistic))
            .frame(minHeight: 430).clipShape(RoundedRectangle(cornerRadius: 18))
            .onChange(of: selectedAirport.id) { _, _ in mapPosition = .region(MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: selectedAirport.latitude, longitude: selectedAirport.longitude), span: MKCoordinateSpan(latitudeDelta: 1.8, longitudeDelta: 1.8))) }
            .task(id: selectedAirport.id) {
                await refreshLiveAircraft()
                while !Task.isCancelled { try? await Task.sleep(nanoseconds: 20_000_000_000); if !Task.isCancelled { await refreshLiveAircraft() } }
            }
            Text(aircraftStatus).font(.caption).foregroundStyle(ATCStyle.muted)
            if let aircraft = selectedAircraft {
                VStack(alignment: .leading, spacing: 8) {
                    Text(aircraft.callsign.isEmpty ? "Unbekanntes Rufzeichen" : aircraft.callsign).font(.headline)
                    aircraftInfo("Typ / Registrierung", [aircraft.type, aircraft.registration].filter { !$0.isEmpty }.joined(separator: " · ").isEmpty ? "Keine Angabe" : [aircraft.type, aircraft.registration].filter { !$0.isEmpty }.joined(separator: " · "))
                    aircraftInfo("Höhe", aircraft.altitudeText)
                    aircraftInfo("Geschwindigkeit", aircraft.speed.map { String($0) + " kt" } ?? "Keine Angabe")
                    aircraftInfo("Kurs", aircraft.track.map { String($0) + "°" } ?? "Keine Angabe")
                    aircraftInfo("Squawk", aircraft.squawk.isEmpty ? "Keine Angabe" : aircraft.squawk)
                    aircraftInfo("Start / Ziel", "In ADS-B-Positionsdaten nicht zuverlässig enthalten")
                }.padding(14).atcCard()
            }
            VStack(alignment: .leading, spacing: 8) {
                Text("Flugzeug suchen").font(.headline)
                HStack { TextField("Rufzeichen oder Registrierung", text: $flightSearch).textInputAutocapitalization(.characters).autocorrectionDisabled().padding(10).background(ATCStyle.background, in: RoundedRectangle(cornerRadius: 10)); Button("Suchen") { Task { await searchLiveAircraft() } }.buttonStyle(.borderedProminent).tint(ATCStyle.green).foregroundStyle(ATCStyle.background) }
                Text(flightSearchStatus).font(.caption).foregroundStyle(ATCStyle.muted)
            }.padding(14).atcCard()
            externalButton("Live-Radar extern öffnen", symbol: "scope", url: "https://globe.airplanes.live/?airport=" + selectedAirport.icao + "&hideSidebar")
        }.atcCard()
    }

    private func aircraftInfo(_ title: String, _ value: String) -> some View {
        HStack { Text(title).foregroundStyle(ATCStyle.muted); Spacer(); Text(value).multilineTextAlignment(.trailing) }.font(.subheadline)
    }

    @MainActor private func refreshLiveAircraft() async {
        let primary = "https://api.airplanes.live/v2/point/\(selectedAirport.latitude)/\(selectedAirport.longitude)/100"
        let fallback = "https://opendata.adsb.fi/api/v3/lat/\(selectedAirport.latitude)/lon/\(selectedAirport.longitude)/dist/100"
        var data: Data?
        var sourceName = "Airplanes.live"
        do {
            guard let url = URL(string: primary) else { throw URLError(.badURL) }
            var request = URLRequest(url: url); request.timeoutInterval = 12; request.cachePolicy = .reloadIgnoringLocalCacheData
            let (result, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            data = result
        } catch {
            do {
                guard let url = URL(string: fallback) else { throw URLError(.badURL) }
                var request = URLRequest(url: url); request.timeoutInterval = 12; request.cachePolicy = .reloadIgnoringLocalCacheData
                let (result, response) = try await URLSession.shared.data(for: request)
                guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
                data = result; sourceName = "adsb.fi (Fallback)"
            } catch {
                aircraftStatus = "Live-Positionen nicht verfügbar (beide Datenquellen nicht erreichbar)."; return
            }
        }
        do {
            let raw = try JSONSerialization.jsonObject(with: data ?? Data()) as? [String: Any]
            let rows = raw?["ac"] as? [[String: Any]] ?? raw?["aircraft"] as? [[String: Any]] ?? []
            func string(_ row: [String: Any], _ key: String) -> String { String(describing: row[key] ?? "").trimmingCharacters(in: .whitespacesAndNewlines) }
            func number(_ row: [String: Any], _ key: String) -> Double? { (row[key] as? NSNumber)?.doubleValue ?? (row[key] as? String).flatMap(Double.init) }
            liveAircraft = rows.compactMap { row in
                guard let lat = number(row, "lat"), let lon = number(row, "lon"), abs(lat) <= 90, abs(lon) <= 180 else { return nil }
                if let seen = number(row, "seen_pos"), seen > 120 { return nil }
                return LiveAircraft(hex: string(row, "hex"), callsign: string(row, "flight"), registration: string(row, "r"), type: string(row, "t"), latitude: lat, longitude: lon, altitude: number(row, "alt_baro").map(Int.init), speed: number(row, "gs").map(Int.init), track: number(row, "track").map(Int.init), squawk: string(row, "squawk"))
            }.prefix(500).map { $0 }
            aircraftStatus = "\(liveAircraft.count) Flugzeuge empfangen · Quelle: \(sourceName) · Aktualisierung ca. alle 20 s"
        } catch { aircraftStatus = "Live-Daten konnten nicht verarbeitet werden." }
    }

    @MainActor private func searchLiveAircraft() async {
        let q = flightSearch.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard q.count >= 2, q.count <= 12, q.range(of: "^[A-Z0-9 -]+$", options: .regularExpression) != nil else { flightSearchStatus = "Bitte gültiges Rufzeichen oder Registrierung eingeben."; return }
        flightSearchStatus = "Suche nach aktuell empfangenen Flugzeugen …"
        let endpoint = q.contains("-") ? "reg" : "callsign"
        guard let url = URL(string: "https://api.airplanes.live/v2/" + endpoint + "/" + q.replacingOccurrences(of: " ", with: "")) else { return }
        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            let raw = try JSONSerialization.jsonObject(with: data) as? [String: Any]; let rows = raw?["ac"] as? [[String: Any]] ?? []
            guard let first = rows.first else { flightSearchStatus = "Kein Flugzeug mit dieser Kennung aktuell empfangen."; return }
            await refreshLiveAircraft()
            if let hex = first["hex"] as? String, let found = liveAircraft.first(where: { $0.hex.caseInsensitiveCompare(hex) == .orderedSame }) { selectedAircraft = found }
            flightSearchStatus = String(rows.count) + " aktuell empfangene Treffer gefunden."
        } catch { flightSearchStatus = "Flugsuche momentan nicht verfügbar." }
    }

    private var audio: some View {
        VStack(alignment: .leading, spacing: 14) {
            Label("Öffentliche Airport-Streams", systemImage: "dot.radiowaves.left.and.right").font(.title2.bold())
            Text("Die Website enthält kuratierte Video-IDs für einige Flughäfen. Wähle einen verfügbaren öffentlichen Stream; ATC Live hostet oder leitet das Audio nicht selbst weiter.")
                .foregroundStyle(ATCStyle.muted)
            let streams: [(String, String, String)] = [
                ("Las Vegas · KLAS", "V7_orOtu-oo", "KLAS"),
                ("New York LaGuardia · KLGA", "T_QsY01tIK0", "KLGA"),
                ("New Orleans · KMSY", "iZRbml7fnXI", "KMSY"),
                ("Sint Maarten · TNCM", "2IQmpCXbOmM", "TNCM"),
                ("Reno–Tahoe · KRNO", "fFl0qD108sk", "KRNO"),
                ("Milwaukee · KMKE", "sJl7PsdHxWM", "KMKE"),
                ("Lanzarote · GCRR", "4nC8r5neaow", "GCRR")
            ]
            ForEach(streams, id: \.1) { item in
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.0).font(.headline)
                        Text(item.2).font(.caption.monospaced()).foregroundStyle(ATCStyle.muted)
                    }
                    Spacer()
                    externalButton("Stream öffnen", symbol: "play.fill", url: "https://www.youtube.com/watch?v=\(item.1)")
                }.padding(12).background(ATCStyle.background, in: RoundedRectangle(cornerRadius: 12))
            }
        }.atcCard()
    }

    private func metric(_ title: String, value: String, symbol: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: symbol).font(.title3).foregroundStyle(ATCStyle.green)
            Text(title).font(.caption.bold()).tracking(0.8).foregroundStyle(ATCStyle.muted)
            Text(value.isEmpty ? "—" : value).font(.headline).foregroundStyle(.white).fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, minHeight: 110, alignment: .leading)
        .padding(18).atcCard()
    }

    private func externalButton(_ title: String, symbol: String, url: String) -> some View {
        Link(destination: URL(string: url)!) {
            Label(title, systemImage: symbol)
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: 48)
                .padding(.horizontal, 10)
                .background(ATCStyle.green, in: RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(ATCStyle.background)
        }
    }
}

private struct ATCCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.padding(18)
            .background(ATCStyle.panel.opacity(0.94), in: RoundedRectangle(cornerRadius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(ATCStyle.line, lineWidth: 1))
    }
}

private extension View {
    func atcCard() -> some View { modifier(ATCCardModifier()) }
}
