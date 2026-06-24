---
project_name: 'parv-na-myasto'
artifact: 'Product Brief'
author: 'Ivayla'
method: 'BMAD — bmad-product-brief'
date: '2026-06-24'
status: 'reconstructed from the shipped MVP'
---

# Product Brief — Пръв на Място (First on the Scene)

> A short, decision-oriented brief: the problem, who has it, the bet, and the
> smallest thing worth building. Produced with the BMAD analyst/brief workflow and
> reconciled against the shipped code so it describes what actually exists.

## 1. The problem

After a road accident in Bulgaria, the tow-truck operator who **arrives first**
usually wins the job. Arrival is downstream of *information*: the operator who
**hears about the accident first** is the one who can get there first. Today that
information arrives by luck — a phone call from a contact, a police scanner, word
of mouth. There is no fair, fast, location-aware signal that says *"there is a job
inside your coverage area, right now."*

## 2. Who has it

Independent and small-fleet **road-assistance / tow operators**, each effectively
covering a city and its ring road (Sofia, Plovdiv, Varna, Burgas, Veliko Tarnovo,
Ruse…). They are mobile, time-pressured, and care about exactly one thing during
a shift: *is there a job near me that I can reach before anyone else?*

## 3. The bet (value hypothesis)

> **Speed is the entire product.** If we cut the time between "an accident appears
> on a live traffic feed" and "the right operator's screen lights up" to near zero,
> we change who arrives first — and therefore who earns.

Everything in the system is justified only insofar as it reduces that latency or
keeps the signal trustworthy.

## 4. The core insight

Stripped down, the whole product is **one geometric question asked thousands of
times a day**:

> *Which operators have this incident inside their coverage circle?*

That proximity match (`GeoMatcher`, a haversine distance test against each
operator's radius) is the heart of the system. Everything else exists only to
**feed** that question (poll + filter live incidents) and **deliver** its answer
**live** (server-sent events to a map). The "magic" is deliberately ordinary:
geometry plus a live channel.

## 5. Scope of the MVP

**In:**
- Poll a live traffic-incident feed on a schedule and keep only the real tow jobs
  (crashes + broken-down vehicles); drop jams, road works, weather as noise.
- Dedup incidents so the same one seen on every poll is recognised once.
- Match each new incident to operators by coverage radius, nearest first.
- Push new incidents to a live map (Leaflet) with a one-tap "Navigate" link.
- A Waze-style **"helper on scene"** flag any operator can set, which relabels the
  job live for everyone (attributable, and never hides the job).
- Seeded operators so the matching is visible out of the box.

**Out (deliberately deferred):**
- Operator signup / authentication / trustworthy claim identity.
- Persistence beyond a process restart (optional Supabase mode exists but in-memory
  is the default).
- Multi-region / multi-country expansion.
- Mobile-native app (the MVP is a single served web page).

## 6. Honesty constraint (a product principle, not a footnote)

The app **only ever shows real incidents**. With no data-source key, or on a failed
poll, it shows an honest empty state ("no incidents in your area right now") rather
than any fabricated data. A wrong "on-scene" claim labels a job but can never make
it disappear. Trust is treated as a feature because operators will not act on a feed
they suspect is invented.

## 7. Data source reality

The MVP design targeted **Waze's livemap** — the richest crowd-sourced feed,
including its roadside-help reports — but Waze hard-blocks automated requests with
`403`. The shipped app therefore polls **TomTom's sanctioned Traffic Incident
Details API** (live, free key). Both sit behind one `IncidentSource` interface, so
the provider is a one-class swap.

## 8. How we'll know it worked

- A real incident on the live feed appears on the correct operator's map within one
  poll cycle (≤ ~90s), nearest-first, with a working navigation link.
- The empty state is shown truthfully whenever the feed is genuinely empty.
- Swapping the data provider touches exactly one class.

## 9. Method note

This brief, the [requirements](requirements.md), and the
[architecture decisions](architecture.md) were produced using **BMAD** specialized
agents (analyst → PM → architect → dev), with `project-context.md` codifying the
rules downstream code generation had to follow. The role of the agents was to
**accelerate and structure** the path from idea → documentation → architecture →
implementation; the core idea (a proximity check) stayed deliberately simple.
