---
title: "PRD: Пръв на Място (First on the Scene)"
status: draft
created: 2026-06-24
updated: 2026-06-24
---

# PRD: Пръв на Място (First on the Scene)

_Working title — confirm._

## 0. Document Purpose

This PRD is for **Ivayla** (builder) and any reviewer of the project — most concretely, a hiring manager evaluating it as part of a junior-developer application. It defines the product at two altitudes: the **real problem and full vision** as context, and the **MVP that actually builds and runs** as the scoped requirements. Functional requirements (§4) describe only what the working Spring Boot application does today; the broader commercial product (subscriptions, accounts, mobile app) lives in **§11 Future / Vision** and is explicitly _not_ part of the MVP. Tech choices, which normally belong in an addendum, are summarised in **§10 Architecture & Key Decisions** because they are a primary signal for the intended dev-role reader; deeper technical provenance lives in `addendum.md`. Vocabulary is anchored in the **§3 Glossary**; assumptions are tagged inline and indexed in §9. This PRD builds on the product brief `brief-parv-na-myasto-2026-06-19`.

## 1. Vision

In the Bulgarian towing business, **the first truck to reach an accident gets the job** — everyone else drove for nothing. Operators run that race today with tools never built for it: Waze on a personal phone, Viber groups, police contacts, and gut feel about which junctions produce work. Knowing about a job _early_ is the entire game, and the current way of knowing is luck.

**Пръв на Място** turns that race into an advantage. It watches live traffic-incident data across Bulgaria, and the instant an accident or hazard appears **inside an operator's coverage zone**, it surfaces that incident on the operator's screen — with the location, type, distance, and one-tap navigation. The promise in one line: _something happened 4 km from you, go._

The clever core is deliberately small and visible: a single geometric question — _which operators have this incident inside their coverage circle?_ — answered thousands of times a day, and the answer delivered live the moment it changes. The MVP is a self-contained app that proves exactly that loop end to end: poll → match → push.

## 2. Target User

### 2.1 Jobs To Be Done

- **Functional:** "Tell me about reachable jobs the instant they happen, not after a competitor has them." `[ASSUMPTION: speed-to-knowledge is the dominant unmet need vs. e.g. job coordination]`
- **Functional:** "Show me only incidents I can actually reach — filter the firehose down to my zone."
- **Emotional:** "Stop me feeling like I'm losing jobs to luck and other people's contacts."
- **Contextual:** "Work on a phone, on a windshield mount, while I'm driving — glance, don't read."
- **Builder's JTBD (for this artifact):** "Demonstrate that I can take a real problem and ship a working, well-reasoned solution end to end."

### 2.2 Non-Users (v1)

- **Drivers / the public** — this is a professional tool for operators, not a consumer safety app.
- **Dispatchers coordinating multiple trucks** — fleet dispatch is vision, not MVP (see §11).
- **Operators outside Bulgaria** — coverage is the Bulgaria bounding box only.

### 2.3 Key User Journeys

- **UJ-1. Mitko sees a job before anyone calls him.**
  - **Persona + context:** Mitko, an independent tow operator based in central Sofia, one truck, income tied directly to how many jobs he reaches first.
  - **Entry state:** App open on the dashboard in the browser (windshield-mounted phone or laptop at base). No login — he has selected his operator profile from the list.
  - **Path:** (1) The map shows his base and a coverage circle. (2) A new accident appears live on бул. Цариградско шосе. (3) It pops into the side list at the top, flashing, marked **1.5 km** away. (4) He taps **Навигация →**.
  - **Climax:** Waze opens, routed to the scene — seconds after the incident appeared, with no refresh and no phone call. Realizes UJ-1.
  - **Resolution:** Mitko is driving toward a job his competitors may not know about yet.
  - **Edge case:** if the live feed is briefly unavailable or simply quiet, the dashboard shows an honest "no incidents in your area right now" message rather than fabricating data — Mitko is never sent to a job that isn't real.

- **UJ-2. A reviewer runs it in one command.** _(builder/evaluator journey, lighter)_
  - A hiring manager clones the repo, runs `./mvnw spring-boot:run` (no Maven install needed — the wrapper is bundled), opens `http://localhost:8080`, picks an operator, and immediately sees the proximity match working — no accounts, no cloud setup, no API keys.

## 3. Glossary

- **Incident** — A single live traffic event from the data source, of type **Accident** or **Hazard**, with a latitude/longitude, optional street/city, and a report time. Identified by the source's own alert id (used for dedup). One Incident may match zero or more Operators.
- **Operator** — A tow-truck business with a **Base Location** and a **Coverage Radius**. In the MVP, Operators are a fixed seeded set; signup is out of scope.
- **Base Location** — The fixed point (lat/lng) an Operator works from.
- **Coverage Radius** — The distance in kilometres an Operator is willing to drive. Base Location + Coverage Radius define the **Coverage Zone**.
- **Coverage Zone** — The circle (Base Location, Coverage Radius) inside which an Incident is considered reachable for that Operator.
- **In Range / Match** — An Incident is _In Range_ of an Operator when the great-circle (haversine) distance from the Operator's Base Location to the Incident is ≤ the Coverage Radius. The set of such pairings is the **Match**.
- **Distance** — Great-circle (haversine) distance in kilometres between a Base Location and an Incident.
- **Live Push** — Server-to-client delivery of a new Incident over Server-Sent Events (SSE), so the dashboard updates without polling.
- **Data Source** — The live TomTom Traffic Incidents API for the Bulgaria bounding box, behind a provider-independent `IncidentSource` seam.
- **Empty State** — The message the dashboard shows when a selected Operator has no Incidents In Range. The system shows this rather than any fabricated/sample data.

## 4. Features

### 4.1 Live Incident Ingestion

**Description:** On a fixed schedule the system pulls the **Data Source**, keeps only **Incidents** of type Accident or Hazard, and stores genuinely new ones. Because storage is keyed on the source's alert id, the same live Incident re-reported on every poll is recognised once — only new Incidents flow downstream to matching and **Live Push**. A failed poll never stops the schedule, and the system never fabricates Incidents: if the Data Source returns nothing, nothing is shown. Realizes UJ-1.

**Functional Requirements:**

#### FR-1: Scheduled ingestion of live incidents

The system polls the Data Source on a fixed interval and ingests Accident/Hazard Incidents.

**Consequences (testable):**

- Polls every `POLLER_FIXED_DELAY_MS` (default 90 000 ms) after an initial delay (default 3 000 ms).
- Only alerts with type ∈ {ACCIDENT, HAZARD} are ingested; all others are discarded.
- An alert missing coordinates is skipped, not stored.
- A failed poll (e.g. the Data Source is unreachable, or no API key is set) logs a single concise warning and the schedule continues; the next tick retries.

#### FR-2: Deduplication on source id

The system stores each real-world Incident once, keyed on the source alert id.

**Consequences (testable):**

- Ingesting an Incident whose id already exists is a no-op; the store count does not increase.
- Only the first appearance of an id triggers matching (FR-3) and Live Push (FR-6).

### 4.2 Proximity Matching _(the core)_

**Description:** For any new Incident, the system determines which Operators have it In Range, and for any Operator it can list the Incidents inside that Operator's Coverage Zone, nearest first. This is the heart of the product: one haversine distance test against each Operator's Coverage Radius. Realizes UJ-1.

**Functional Requirements:**

#### FR-3: Match an incident to operators in range

Given an Incident, the system returns the Operators whose Coverage Zone contains it.

**Consequences (testable):**

- An Operator is returned iff `haversine(BaseLocation, Incident) ≤ CoverageRadius`.
- Operators whose Coverage Zone does not contain the Incident are never returned.
- The match count for each new Incident is logged.

#### FR-4: List incidents in an operator's coverage

The system exposes the Incidents In Range of a given Operator, sorted nearest-first with Distance.

**Consequences (testable):**

- `GET /api/operators/{id}/incidents` returns only In-Range Incidents, ordered by ascending Distance, each annotated with `distanceKm`.
- An unknown operator id returns HTTP 404.
- When no Incident is In Range, the result is an empty list (which drives the empty-state in FR-8) — never fabricated data.
- Verified during development against a controlled set of test incidents: an Operator returns only the Incidents inside its radius, nearest-first (e.g. a Sofia Operator excludes Plovdiv/Varna Incidents).

### 4.3 Live Operator Dashboard

**Description:** A single web page (served by the backend) shows a map of Bulgaria, lets the user pick an Operator, draws that Operator's Base Location and Coverage Zone, and lists In-Range Incidents nearest-first. New Incidents arrive over Live Push and appear without a refresh; each Incident offers one-tap navigation. Realizes UJ-1, UJ-2.

**Functional Requirements:**

#### FR-5: Operator selection and coverage visualization

The user can select an Operator and see its Base Location and Coverage Zone on the map.

**Consequences (testable):**

- The Operator dropdown is populated from `GET /api/operators`.
- Selecting an Operator centres the map on its Base Location and draws a circle of radius = Coverage Radius.
- Deselecting returns the map to the national view.

#### FR-6: Live push of new incidents

New Incidents are delivered to the dashboard in real time.

**Consequences (testable):**

- The client subscribes to `GET /api/stream` (SSE); each new Incident is sent as a named `incident` event.
- On receipt, a map marker is added; if an Operator is selected and the Incident is In Range, the list refreshes and the new entry is visually flagged.
- No client-side polling is used for new Incidents.

#### FR-7: One-tap navigation

Each listed Incident links directly to turn-by-turn navigation to its location.

**Consequences (testable):**

- Each list entry exposes a navigation link of the form `https://waze.com/ul?ll={lat},{lng}&navigate=yes`.

#### FR-8: Honest empty state — no fabricated data

When a selected Operator has no Incidents In Range, the dashboard shows a clear empty-state message instead of fabricated data or a blank screen. The system never inserts sample or demo Incidents. Realizes UJ-1 (edge case).

**Consequences (testable):**

- When `GET /api/operators/{id}/incidents` returns an empty list, the dashboard shows an explicit message (e.g. "Няма активни инциденти в обхвата ви в момента.").
- No sample or demo Incidents are ever created; the only Incidents shown are real ones from the Data Source.
- If the Data Source yields nothing (e.g. no API key set, or the source is unreachable), the map and list are simply empty under the empty-state message — honest, not fabricated.

**Feature-specific NFRs:**

- Mobile-responsive: usable on a phone (windshield use); list collapses appropriately on narrow screens.
- No build step: the UI is a single static page served from the backend; no separate frontend toolchain.

## 5. Non-Goals (Explicit)

- **Not** an auth/accounts product in the MVP — Operators are seeded, there is no signup or login.
- **Not** a billing/subscription system in the MVP — no tiers, trials, or payments.
- **Not** a job-coordination/dispatch tool — it does not claim jobs or prevent two trucks chasing one Incident.
- **Not** a multi-region product — Bulgaria bounding box only.
- **Not** a guaranteed-uptime production service — single-node, in-memory, intended to demonstrate the core loop.
- **Not** dependent on any cloud account or external managed service to run.

## 6. MVP Scope

### 6.1 In Scope

- Scheduled ingestion of live Accident/Hazard Incidents from the Data Source, with dedup (FR-1, FR-2).
- Proximity Matching: incident→operators and operator→incidents nearest-first (FR-3, FR-4).
- Live Operator Dashboard: map, Operator selection, Coverage Zone, In-Range list, Live Push, one-tap navigation, and an honest Empty State when nothing is in range (FR-5–FR-8).
- A small REST + SSE API surface (see §10).
- One-command run (`./mvnw spring-boot:run`, no Maven install needed); no external services required to run.
- Optional Supabase Postgres persistence for Incidents + Operators, selected with `APP_STORAGE=supabase` (default is in-memory).

### 6.2 Out of Scope for MVP

- Operator signup, authentication, profiles, and editing Coverage Zone in-app — _deferred to vision; seeded list suffices to prove the loop._
- Subscriptions, tiers, billing — _vision (§11)._ `[NOTE FOR PM: pricing is emotionally load-bearing for a customer pitch but irrelevant to a dev-role demo]`
- Native/installable mobile app and OS-level push notifications while the app is closed — _MVP uses an open web page + in-page Live Push._
- Multiple simultaneous Data Sources / provider failover — _single source (TomTom) for now; the `IncidentSource` seam makes adding HERE etc. straightforward later._

## 7. Success Metrics

**Primary**

- **SM-1 — Match correctness.** For a given Operator, the dashboard lists exactly the In-Range Incidents, nearest-first, and no out-of-range Incidents. Target: 100% correct on a defined test set. Validates FR-3, FR-4. _(Verified against a controlled test set during development.)_
- **SM-2 — Time-to-screen.** Elapsed time from a new Incident being ingested to it appearing on a connected dashboard. Target: < 1 s over Live Push (excluding the upstream poll interval). Validates FR-6.

**Secondary**

- **SM-3 — One-command start.** A reviewer can go from clone to a working dashboard with a single command and no accounts. Target: yes. Validates §6.
- **SM-4 (product/vision) — First-to-scene rate.** Share of jobs an Operator reaches first while using the product. Target: TBD. Validates the vision, not the MVP. `[ASSUMPTION: requires real operators; not measurable in the demo]`

**Counter-metrics (do not optimize)**

- **SM-C1 — Alert volume.** Do _not_ maximise the number of Incidents shown. The product's value is precision (only reachable jobs), not recall of every Incident in the country. Counterbalances any push toward "show more." Counterbalances SM-2's "more live updates is better" reading.

## 8. Open Questions

1. How do operators _actually_ find jobs today, and what does being second cost them in a real number? (From brief; unchanged.)
2. Real pricing per tier and what distinguishes tiers — only relevant if the vision (§11) is pursued.
3. Beachhead: independent operators vs. fleets first; rough counts in Bulgaria.
4. Is there any real traction (pilot operator, demo interest)? `[ASSUMPTION: none yet — treated as a portfolio/learning project]`
5. The single-sentence hook that makes an operator care (working version: _"something happened 4 km from you, go."_).
6. Data-source longevity: the app uses TomTom's free tier (which resolved Waze's 403 blocking); confirm its quota/licensing is adequate for real use, or whether HERE / a paid tier is needed.

## 9. Assumptions Index

- §2.1 — Speed-to-knowledge is the dominant unmet need (vs. coordination/other).
- §4.1 / FR-1 — Waze's livemap is unusable for automated use (403, observed this session); the app uses TomTom's Traffic API (free key) behind an `IncidentSource` seam.
- §6.2 — Pricing/tiers are load-bearing for a pitch but not for a dev-role demo.
- §7 / SM-4 — First-to-scene rate needs real operators; not measurable in the demo.
- §8 Q4 — No real traction yet; treated as a portfolio/learning project.
- §11 — Commercial vision items are aspirational and largely unbuilt.

## 10. Architecture & Key Decisions

> Included in the PRD (rather than only the addendum) because the engineering reasoning is a primary signal for the intended dev-role reader. Deeper provenance is in `addendum.md`.

**The bet:** the proximity Match _is_ the product, so it should be the most readable, testable thing in the codebase — not hidden in infrastructure.

**Shape:** one self-contained Spring Boot app owns the whole loop and also serves the UI:

```
live TomTom ─▶ IncidentPoller ─▶ IncidentStore ─▶ GeoMatcher ─▶ IncidentStream ─▶ browser
             (poll + filter)    (mem | Supabase)  (haversine)    (SSE push)     (static Leaflet map)
```

**Key decisions (what changed from the original prototype and why):**

| Concern          | Original prototype                    | MVP decision                                    | Why                                                                                                      |
| ---------------- | ------------------------------------- | ----------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| Proximity match  | PostGIS `ST_DWithin` in SQL           | **Haversine in plain Java (`GeoMatcher`)**      | Makes the core idea visible and unit-testable; removes a DB/extension dependency for a single-node demo. |
| Data source      | Waze livemap (scraped)                | **TomTom Traffic API (sanctioned, free key)**   | Waze hard-blocks automated requests (403); TomTom is reliable, behind an `IncidentSource` seam.          |
| Storage          | Supabase Postgres (only)              | **In-memory by default; optional Supabase Postgres** | Runs with zero config; `app.storage=supabase` persists Incidents + Operators behind the same `IncidentStore` / `OperatorStore` interface, no change to the match. |
| Live delivery    | Supabase Realtime (hosted WS)         | **Server-Sent Events (`IncidentStream`)**       | Same instant-push behaviour with no external service; one-directional push is all the UI needs.          |
| Frontend         | React + Vite + MapLibre               | **Single static Leaflet page served by Spring** | No build step; `./mvnw spring-boot:run` is the only command.                                             |
| Backends         | Java Spring _and_ Deno edge functions | **One Spring Boot app**                         | Removed duplication; one language, one process.                                                          |
| Accounts/billing | Supabase Auth + subscription tiers    | **Removed from MVP**                            | Not needed to prove the core loop; belongs to the vision.                                                |

**API surface (MVP):**

| Endpoint                            | Returns                                                             |
| ----------------------------------- | ------------------------------------------------------------------- |
| `GET /api/operators`                | Operators and their Coverage Zones                                  |
| `GET /api/incidents`                | All Incidents seen so far                                           |
| `GET /api/operators/{id}/incidents` | In-Range Incidents for one Operator, nearest-first (404 if unknown) |
| `GET /api/stream`                   | Live Push of new Incidents (SSE)                                    |

**Accepted trade-offs:** the default in-memory mode has no persistence across restart (switch on Supabase to persist) and is single-node; haversine is a flat-distance approximation (fine at city scale); SSE is one-directional (fine — the client only receives). **Path to scale** (vision): add PostGIS with an indexed `ST_DWithin` behind the existing `IncidentStore` when Operator counts grow, and a managed pub/sub behind `IncidentStream`.

## 11. Cross-Cutting NFRs

- **Performance:** Live Push delivers a new Incident to connected clients in < 1 s (SM-2). Matching is O(operators) per Incident — trivial at demo scale.
- **Reliability / graceful degradation:** a Data Source failure must never crash or stop ingestion; when the feed is empty or blocked the dashboard shows an honest Empty State message rather than fabricated data or a blank error.
- **Observability:** each poll logs fetched/new counts; each new Incident logs its match count; Data Source failures log a single concise warning (not a stack-trace dump).
- **Operability:** runs with one command, JDK 17+, no external dependencies; all cadence/flags are environment-overridable.
- **Portability:** no cloud account or managed service required to build, run, or demo (default in-memory mode).
- **Persistence (optional):** `app.storage=supabase` persists Incidents + Operators in Postgres behind the same store interface; tables are created and operators seeded automatically on first run.

## 12. Future / Vision

_Aspirational; not part of the MVP. Kept qualitative per decision — numbers are illustrative only._

If the core loop proves valuable, the product grows along two axes:

- **Deeper:** operator signup and self-managed Coverage Zones; richer history and analytics over the persisted data; native/installable app with OS push so alerts arrive with the app closed; performance stats ("jobs reached first"); additional incident providers / PostGIS spatial queries at scale.
- **Wider:** fleet view and light job-coordination so two trucks from the same company don't chase one Incident; more incident types; neighbouring markets; partnerships with insurers / road-assistance brands.

**Commercial model (illustrative, from the brief — `[ASSUMPTION]`):** a monthly subscription with a free trial and tiers (Starter / Pro / Fleet), priced well below the value of a single tow job so the math is trivial for the operator. Real prices and what distinguishes the tiers are open questions (§8).

The long-term position: **the real-time layer between "an incident happened" and "the right truck is on its way."**
