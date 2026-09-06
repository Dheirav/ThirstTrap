# ThirstTrap — Roadmap

Build order with a definition of done per milestone. The ordering is not
arbitrary: it front-loads the two risks that can kill the project and defers
everything that is merely nice.

---

## M0 — Prototype the two hard problems

**Before any architecture, any Room schema, any module split.**

The requirements name two UX problems as *"worth prototyping first"*. They are
the two ways this app fails regardless of code quality: if logging is slower
than a paper note it dies in a week, and if the reminder tone is wrong it
trains exactly the calendar-watering behaviour the whole weight model exists to
replace.

**Scope:** a single-Activity Compose throwaway with hardcoded fake data. No
database, no DI, no modules. Two screens:

1. The dashboard with the one-tap droplet log and undo snackbar (UI-SPEC §2).
2. A reminder notification with its three actions, and the "Still wet"
   confirmation (UI-SPEC §7).

**Done when:** you have used it on your own phone, against your own plants, for
**three days**. Not when it compiles. The question is whether you actually
reach for it instead of not bothering — and you cannot answer that at a desk.

Expect to throw this code away. That is the point; the output is a decision,
not a codebase.

---

## M1 — MVP

Requirements items 1–10.

**Foundation**
- `git init`, `.gitignore`, version catalog, four-module skeleton
  (ARCHITECTURE §1)
- Room schema for `plants`, `care_events`, `photos`, `reminders`
  (DATA-MODEL) — `exportSchema = true`, `app/schemas/` committed
- Hilt, theme, navigation, bottom bar

**Features**
- [1] Add / edit / archive plants with photo and medium
- [2] Log care events, ≤3 taps for the common case
- [3] Per-plant timeline, newest first
- [4] In-app photo capture, auto-attached to plant + date
- [5] Side-by-side photo compare
- [6] Check reminders, per-plant interval, local notifications
      (NOTIFICATIONS — WorkManager path only; exact alarms are M2)
- [X7] "Reminders not arriving?" OEM help screen — **M1 because the target
      device is a Redmi on HyperOS**; see handover D6
- [7] Dashboard with attention sorting
- [8] Computed average watering interval per plant
- [9] Fully offline
- [10] Export to zip (JSON + photos) via SAF

**Done when:**
- All ten items work on a real device.
- Export produces a zip that **re-imports to a byte-identical database state**.
  Test this explicitly — it is the promise the app is making about data
  ownership, and the requirements call backup non-negotiable.
- Notification manual tests 1–11 (NOTIFICATIONS §7) pass on a physical phone.
- `./gradlew :core:domain:test` green.
- You have been using it daily for two weeks with real plants.

**Do not start M2 until that fortnight has happened.** Real usage will change
the schema, and changing it before there is data to migrate is free.

---

## M2 — The differentiator: weight-based watering

Requirements item 17. Full spec in `docs/WATERING-MODEL.md`.

**Domain first, and complete, before any UI:**
- `weight_readings` and `drying_segments` tables + migration
- Segmentation (WATERING-MODEL §3)
- Theil–Sen fit (§4)
- Adaptive anchors (§2)
- EWMA prior (§5)
- Prediction with every suppression rule (§6)
- Diagnostics (§7)
- **The entire §9 test plan, passing.** This is not optional and it is not
  something to retrofit. Write the outlier-robustness test first and watch a
  least-squares implementation fail it — that is the test that proves the
  choice of estimator.

**Then UI:**
- Calibration wizard
- Weight entry with the large keypad
- Depletion bar on the dashboard card
- Weight history chart with anchors, thresholds and segment markers
- Reminders switch to prediction-driven intervals (NOTIFICATIONS §5)
- Exact-alarm opt-in (the help screen already shipped in M1)

**Done when:** the app has predicted a watering date for a real plant, and the
prediction was right within a day, across **at least three drying cycles on two
different plants**. One lucky cycle is not evidence.

---

## M3 — Depth

Requirements items 18–24, roughly in value order:

- [20] Propagation kanban — cutting → callusing → rooting → potted →
      established, with days-in-stage
- [24] Post-mortem template on marking a plant dead
- [21] QR stickers on pots → deep-link to quick-log
- [19] Diagnosis checklists (fuzz dunk test, firm-vs-mushy, cut-face reading)
- [22] Light meter via `SensorManager` / `TYPE_LIGHT`
- [23] Ambient temp/humidity per location
- [18] Timelapse builder

Timelapse is last deliberately — it is the most fun to build and the least
useful, which is exactly the combination that derails projects.

---

## M4 — Phase 2

Requirements items 11–16:

- [11] Experiments module
- [12] Notes with `[[plant]]` cross-links
- [13] Location light/placement notes, "moved plant" event
- [14] Fertilizer inventory + dilution calculator
- [15] Stats — waterings/month, survival rate, average days-to-root
- [16] Optional cloud backup — **never required for core use**

Plus, whenever it makes sense:
- [25] Pl@ntNet ID as an optional suggestion, with the in-app quota display
- [25b] Kindwise bring-your-own-key disease field

---

## Deferred / undecided

| Thing | Status |
|---|---|
| iOS | Only if the project "goes well". The `core:domain` purity rule (D2) keeps the door open at no ongoing cost. |
| Kotlin Multiplatform setup | Not now. It is a build-config change later, not a rewrite. |
| Play Store release | Undecided. Open source and an APK on GitHub Releases may be enough. |
| Offline TFLite classifier | Requirements item 25 fallback. Only if Pl@ntNet quota proves limiting, which at 500/day it will not for one person. |
| Multi-user / sharing | Out of scope. |

---

## Risk register

| Risk | Severity | Mitigation |
|---|---|---|
| Logging is slower than a paper note | **Fatal** | M0 prototype, three days of real use before anything is built |
| Reminder tone trains calendar-watering | **Fatal** | M0 prototype; UI-SPEC §7 parity rules |
| Notifications silently killed by OEM | **High — target device is HyperOS, the worst case** | WorkManager default (D4), help screen in M1 (X7), test-fire button, matrix run with Autostart off |
| Drying model gives confidently wrong dates | High | Suppression rules are mandatory; "need another reading" beats a wrong number |
| Photo storage bloat | Medium | Compress on write; storage screen; requirements budget is <1 GB for 3 years × 10 plants |
| Room migration loses data | **Fatal** | Committed schemas, migration tests, no destructive fallback in release |
| Scope creep into M3/M4 before M1 ships | High | This document. Items 18–25 do not exist until the MVP has run for a fortnight. |

---

## Working agreements

- `docs/HANDOVER.md` is the live state. Update it when a decision changes.
- Decisions go in the handover decisions log with their reasoning, so they are
  not relitigated three months later.
- No commits without being asked.
- Every commit authored as `dheirav2005@gmail.com`.
