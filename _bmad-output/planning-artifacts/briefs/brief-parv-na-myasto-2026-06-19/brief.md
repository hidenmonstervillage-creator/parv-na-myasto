---
title: "Product Brief: Пръв на Място (First on the Scene)"
status: draft
purpose: customer-partner-pitch
created: 2026-06-19
updated: 2026-06-19
---

# Product Brief: Пръв на Място (First on the Scene)

> **Pitch context:** This brief is written to be read by a **tow-truck operator or fleet manager** — the customer. Its job is to make them feel the pain, see the payoff, and understand the price. Items marked `[ASSUMPTION]` are inferred from the code and market logic and need your confirmation.

---

## Executive Summary

In the towing business in Bulgaria, **the first truck to reach an accident gets the job.** Everyone else drove for nothing. Today operators fight that race with tools that weren't built for it — Waze on a personal phone, Viber groups, police contacts, and gut instinct about which junctions tend to produce work.

**Пръв на Място** turns that race into an unfair advantage. It watches live traffic-incident data across Bulgaria in real time, and the instant an accident or road hazard appears **inside an operator's coverage zone**, it pushes an alert to their phone — with the location, the type of incident, the distance, and one-tap navigation. No scrolling feeds, no waiting for a phone call, no luck. Just: *something happened 4 km from you, go.*

It is a mobile app (installable, works on any phone) sold as a simple monthly subscription. An operator signs up, sets their base location and how far they're willing to drive, and from that moment they are first to know about every job in their zone. `[ASSUMPTION: positioning is "be first, win more jobs" — confirm this is the hook that resonates with operators]`

---

## The Problem

A tow operator's income is bounded by one thing: **how many jobs they reach before a competitor does.** The work exists — accidents happen every day — but knowing about them *early* is the whole game.

How operators find work today: `[ASSUMPTION — needs your confirmation]`
- **Watching Waze / live maps themselves** on a personal phone, refreshing, scanning the whole city instead of just their zone.
- **Viber / Facebook groups** where tips get shared — but slowly, and everyone in the group sees them at once.
- **Police, insurer, and personal contacts** who call when they remember to.
- **Parking near known hotspots** and burning fuel and hours waiting for something to happen.

The cost of the status quo:
- **Missed jobs** — a competitor with a better tip or a closer position arrives first.
- **Wasted fuel and time** — driving toward incidents already taken, or idling at junctions hoping.
- **No coverage discipline** — an operator can't watch the whole map *and* drive; alerts outside their realistic range are noise, and ones inside it get missed.
- **Reactive, not proactive** — they learn about the best jobs last, not first.

`[ASSUMPTION: quantify this — e.g. "an operator misses X jobs/week to being second" or "spends Y лв/month on fuel circling." A real number here is the most persuasive line in the whole pitch.]`

---

## The Solution

Пръв на Място is a **personal incident radar tuned to one operator's coverage zone.**

The experience:
1. **Set your zone once** — your base location and how far you'll drive (default 15 km radius).
2. **Get alerted instantly** — when an accident or hazard appears inside your zone, your phone sounds an alert and shows a notification, even if the app is closed.
3. **See only what matters** — incidents are sorted nearest-first, with type (accident / hazard), street, city, and exact distance from you.
4. **Navigate in one tap** — straight into Waze or Google Maps to the scene.

What the operator does *not* have to do: watch a map all day, monitor group chats, or guess. The system watches for them and only speaks up when there's a real, reachable job.

---

## What Makes This Different

- **Zone-filtered, not firehose.** A Viber group or raw Waze shows everything everywhere. Пръв на Място shows *only* incidents the operator can actually reach — signal, not noise.
- **Push, not pull.** The operator doesn't go looking; the job comes to them the second it appears. Speed is the product.
- **Built for the windshield, not the desk.** Mobile-first, one-tap navigation, audible alerts while driving.
- **Honest about the moat:** the defensibility here is **execution and being first to this niche**, not proprietary technology — the incident data is public-ish and the matching is standard geospatial work. The advantage is shipping a focused tool operators actually adopt, and the network/habit that forms once they rely on it daily. `[ASSUMPTION: confirm — if you have an exclusive data source, a community of operators, or insurer partnerships, the moat story gets much stronger]`

---

## Who This Serves

**Primary — independent tow operators.** One or a few trucks, owner-driver, income directly tied to job volume. Phone-native, price-sensitive, motivated by anything that wins them more jobs. `[ASSUMPTION: independent operators are the beachhead customer — confirm vs. fleets first]`

**Secondary — tow fleets / road-assistance companies.** Several trucks across a region; care about coverage, dispatch efficiency, and not having two of their own trucks chase the same job. The `fleet` subscription tier in the product points at this segment.

`[ASSUMPTION: rough market size in Bulgaria — number of independent operators vs. fleets — to be filled in; this is a key pitch number for a partner.]`

**Potential partners (not buyers, but stakeholders):** insurers and road-assistance brands who could white-label or sponsor coverage. `[ASSUMPTION — only relevant if partner pitch extends beyond operators themselves]`

---

## Success Criteria

**For the operator (their "is this worth it?"):**
- They reach jobs they would otherwise have missed — ideally **first**.
- Less time and fuel wasted on dead-end driving.
- The subscription pays for itself in **one extra job per month**. `[ASSUMPTION: confirm price ≪ value of a single tow job, which makes the pitch math trivial]`

**For the business:**
- Trial-to-paid conversion (the schema already tracks `trial → starter/pro/fleet`).
- Operators who keep the app open and act on alerts (the schema tracks `seen` and `converted` per notification).
- Monthly retention — operators who rely on it daily don't churn.

`[ASSUMPTION: set target numbers — e.g. "50 paying operators in 6 months," "30% trial→paid."]`

---

## Pricing & Packaging

Tiered monthly subscription with a free trial (tiers confirmed in the product schema; **numbers are placeholders**):

| Tier | For | Price | What gates it |
|------|-----|-------|---------------|
| Trial | New signups | Free | Time-limited full access |
| Starter | Single independent operator | `[ASSUMPTION: ~29 лв/mo]` | `[ASSUMPTION: one zone, standard radius]` |
| Pro | Active independent operator | `[ASSUMPTION: ~59 лв/mo]` | `[ASSUMPTION: larger/multiple zones, priority alerts]` |
| Fleet | Multi-truck companies | `[ASSUMPTION: custom]` | `[ASSUMPTION: multiple trucks/zones, dispatch view]` |

`[ASSUMPTION: real prices and what actually distinguishes the tiers (radius? number of trucks? alert volume? speed?) need your input — this table is the heart of a customer pitch.]`

---

## Scope (First Version)

**In:**
- Live incident detection across Bulgaria (accidents + hazards).
- Zone matching by operator base + radius.
- Real-time push alert to the operator's phone (sound + notification).
- Nearest-first incident list with type, street, city, distance.
- One-tap navigation to the scene.
- Operator signup with base location, radius, and subscription tier.

**Out (for now):**
- In-app job claiming / dispatch coordination between operators.
- Payments/billing automation `[ASSUMPTION: trials may be managed manually at first]`.
- Coverage beyond Bulgaria.
- Insurer/partner integrations.

---

## Vision

If it works, Пръв на Място becomes **the default way tow operators in Bulgaria find work** — the app that's always open on the dashboard. From there it grows in two directions: **deeper** (job coordination so two trucks from the same fleet don't collide, performance stats, better data sources like TomTom), and **wider** (more incident types, neighboring markets, and partnerships with insurers and road-assistance brands who want guaranteed fast response). The long-term position: the real-time layer between "an incident happened" and "the right truck is on its way."

---

## Open Questions (to firm up the pitch)

1. How do operators *actually* find jobs today, and what does being second cost them (a real number)?
2. Real pricing per tier, and what distinguishes the tiers?
3. Beachhead: independents or fleets first? Rough counts in Bulgaria?
4. Any traction — pilot users, operators who'd pay, a demo, towing-world connections?
5. The single-sentence hook that makes an operator care.
