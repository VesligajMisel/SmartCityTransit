# SmartCityTransit

Projekt pri predmetu RRI.  
LibGDX aplikacija, ki vizualizira pametni mestni promet na interaktivnem zemljevidu Ljubljane.

---

## Funkcionalnosti

### Zemljevid
- prikaz raster zemljevida (CARTO basemap tiles),
- gladko premikanje (pan),
- približevanje/oddaljevanje (zoom),
- asinhrono nalaganje tile-ov + placeholder med nalaganjem.

### Prometni podatki (DEMO / GTFS)
- nalaganje podatkov iz:
    - DEMO vira (Sample data),
    - GTFS direktorija (`Data/`) (preklop v HUD-u),
- prikaz avtobusnih linij (polilinije),
- prikaz postaj,
- animacija avtobusov po linijah,
- postanki na postajah (waiting / ETA prikaz).

### Interakcija
- klik na avtobus → prikaže info + sledi avtobusu (follow mode),
- klik na postajo → prikaže info + prikaže linije, ki gredo čez postajo,
- klik na linijo → izbere linijo + kamera se centrirana na linijo,
- HUD panel:
    - “Show only selected line”
    - “Reset selection”
    - “Data: DEMO / GTFS (click)”
    - paginacija linij
    - VIEW / EDIT mode

### Edit način
- dodajanje postaje (HUD → Add stop → klik na map),
- vnos imena postaje (tipkanje na tipkovnici),
- “Hide selected stop” (skrije postajo iz prikaza),
- “Potuj do…”: izbereš začetno postajo → klikneš gumb → izbereš ciljno postajo → ustvari se bus vožnja.

---

## Kontrole

- **Levi klik**: izbira elementov (bus/stop/route) ali akcije v HUD-u
- **Drag**: premikanje zemljevida
- **Scroll**: zoom
- **EDIT mode**:
    - Add stop → klik na map doda postajo

---

## Struktura kode (glavno)

- `SmartCityTransit` — main aplikacija (render, input, logika)
- `HudPanel` — UI panel (risanje + klik logika)
- `Bus`, `BusRoute`, `Stop`, `RouteStopIndex` — prometni model + simulacija
- `GeoUtils`, `TileManager` — tile koordinatni sistem + nalaganje map tiles

---

## Tehnologije
- Java
- libGDX
- OpenStreetMap / CARTO map tiles
- GTFS (opcijsko, iz lokalnega direktorija `Data/`)
