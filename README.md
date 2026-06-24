# Пръв на Място (First on the Scene)

Real-time incident radar for tow-truck operators in Bulgaria.

**The problem.** After a road accident, the tow operator who arrives *first* usually
gets the job. So the operator who *hears about it first* wins. Today they find out by
luck — a phone call, a police scanner, word of mouth.

**The idea.** Watch live traffic incidents, and the moment a new accident appears,
instantly alert exactly the operators whose coverage area contains it — with a one-tap
"Navigate" link. Speed is the entire product.

**The clever bit.** It all comes down to one geometric question asked thousands of times
a day: *which operators have this incident inside their coverage circle?* That proximity
match is the heart of the system (`GeoMatcher`), and everything else just feeds it and
delivers its answer live.

---

## Quick start

One command — no database, no cloud accounts, **no Maven install**:

```bash
cd backend
./mvnw spring-boot:run      # Windows: .\mvnw.cmd spring-boot:run
```

For **real live incidents**, set a free TomTom key first (one-time, ~2 min at
[developer.tomtom.com](https://developer.tomtom.com)):

```bash
# PowerShell:  $env:TOMTOM_API_KEY="your-free-key"
# bash:        export TOMTOM_API_KEY=your-free-key
```

Without a key the app still runs — it just shows the honest "no incidents" empty state.

Then open **http://localhost:8080**. Pick an operator from the dropdown: the map draws
their coverage circle and the side panel lists the live incidents inside it, nearest
first, updating automatically as new ones arrive. When nothing is in range, the panel
says so plainly — the app only ever shows real incidents, never fabricated ones.

> Needs only **JDK 17+**. The bundled **Maven Wrapper** (`mvnw` / `mvnw.cmd`) downloads the
> right Maven version automatically on first run — you do not need Maven installed. You can
> also open the `backend/` folder in IntelliJ IDEA / VS Code and run `ParvNaMyastoApplication`
> directly.

---

## How it works

```
                ┌──────────────────────── backend (one Spring Boot app) ───────────────────────┐
  live TomTom ──▶│  IncidentPoller  ──▶  IncidentStore  ──▶  GeoMatcher  ──▶  IncidentStream     │──▶ browser
   (every 90s)  │  (poll + filter)      (dedup, in-mem)     (haversine match)   (SSE push)      │   (Leaflet map)
                └──────────────────────────────────────────────────────────────────────────────┘
```

1. **Poll** — `IncidentPoller` asks the `IncidentSource` (a `TomTomClient`) for live
   incidents on a schedule and keeps only the actual tow jobs: crashes and broken-down
   vehicles. Weather, jams, road works and other hazards are dropped as noise.
2. **Dedup** — `IncidentStore` is keyed by Waze's own alert id, so the same incident seen
   on every poll is recognised once; only genuinely new ones move on.
3. **Match** — `GeoMatcher` answers *which operators have this incident in range?* with a
   plain haversine distance check against each operator's coverage radius.
4. **Push** — `IncidentStream` broadcasts new incidents to every connected browser over
   Server-Sent Events, so the map updates with no refresh.

The whole pipeline is a handful of small, readable classes — the "magic" is just geometry
and a live channel.

## Tech stack

- **Backend:** Java 17 + Spring Boot 3 (`backend/`) — one self-contained app.
- **Frontend:** a single static page (`backend/src/main/resources/static/index.html`) with
  Leaflet for the map. Served by Spring itself — no Node/React build step.
- **Data source:** **Waze livemap API (MVP), TomTom (production).** The MVP design targeted
  Waze's livemap — the richest crowd-sourced incident feed, including its roadside-help
  reports — but Waze hard-blocks automated requests with `403`, so the production app polls
  TomTom's sanctioned Traffic Incidents API (live, free key) instead. Both live behind one
  `IncidentSource` interface, so the provider is a one-class swap. Only real incidents are
  ever shown.
- **State:** in-memory by default (a `ConcurrentHashMap`); set `APP_STORAGE=supabase` to
  persist incidents + operators in **Supabase Postgres** (JDBC behind the same
  `IncidentStore` / `OperatorStore` interface — the matching logic never changes). See
  `backend/README.md` for the connection setup.

## REST API

| Endpoint | What it returns |
| --- | --- |
| `GET /api/operators` | the operators and their coverage circles |
| `GET /api/incidents` | every incident seen so far |
| `GET /api/operators/{id}/incidents` | incidents inside one operator's radius, nearest first |
| `POST /api/incidents/{id}/on-scene` | mark/clear that a roadside helper is on scene (Waze-style) |
| `GET /api/stream` | live feed of new incidents (Server-Sent Events) |

## Notes

- The operators are seeded in `OperatorStore` (Sofia, Plovdiv, Varna, Burgas, Veliko
  Tarnovo) so the matching is visible immediately. In production this would be a signup
  flow backed by a database.
- **"Helper on scene" (Waze-style roadside help):** any operator can mark an incident as
  already being attended (`POST /api/incidents/{id}/on-scene`), and every other operator's
  panel relabels it live. The claim records *which* operator set it (`onSceneBy`) so it is
  attributable, and the incident always stays visible — a claim labels the job, it never
  hides it, so a wrong claim can't make a job disappear. (Waze's own roadside-help markers
  can't be reused directly — Waze blocks automated access — so this is the same idea rebuilt
  on our own data.) Real operator identity/auth is the next step to make claims trustworthy.
- **About the data source:** the app uses the TomTom Traffic Incidents API. Set
  `TOMTOM_API_KEY` for live data; without it (or if a poll fails) the poller logs a short
  warning and the dashboard shows an honest empty-state message ("no incidents in your area
  right now") rather than any fake data. Real incidents appear as soon as a poll succeeds.
  (The original prototype scraped Waze's livemap, but Waze hard-blocks automated requests
  with `403`, so this uses TomTom's sanctioned API instead — behind a provider-independent
  `IncidentSource` seam.)
