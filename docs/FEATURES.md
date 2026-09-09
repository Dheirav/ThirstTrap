# ThirstTrap — Feature List

Every feature, flat, with a stable ID and a pointer to where it is specified.
This is the checklist you tick off while building; `docs/ROADMAP.md` is the
*order* and the done-criteria.

**IDs `F1`–`F25` map exactly to the numbered items in
`plant-tracker-requirements.md`.** They are deliberately not renumbered, even
though the source doc lists them out of order (MVP 1–10, Phase 2 11–16,
Phase 1.5 17–25), because every cross-reference in the other docs depends on
them. Sub-items (`F2.3`) and `X`-prefixed items are new here.

**⊕ marks a feature not in the original requirements doc** — surfaced while
writing the specs. Nine of them, listed together in §7.

Status key: `[ ]` not started · `[~]` in progress · `[x]` done

---

## 1. M0 — Prototypes (throwaway)

*Fake data, no database, no architecture. Output is a decision, not code.*

- [x] **P1** One-tap dashboard log with undo snackbar — *UI-SPEC §2*
- [x] **P2** Reminder notification with three actions + "Still wet" confirmation — *UI-SPEC §7*

---

## 2. M1 — MVP

### F1 — Plants *(req. item 1)*
- [x] **F1.1** Add plant: name, species, medium, location, container, drainage, source, acquired date — *DATA-MODEL `plants`*
- [x] **F1.2** Edit plant
- [x] **F1.3** Archive plant — status → `dead` / `given_away`, history retained
- [x] **F1.4** Cover photo selection
- [x] **F1.5** Per-plant care profile: target dryness, light needs, fertilizer cadence
- [x] **F1.6** ⊕ Hard delete — typed confirmation, offers export first, cascades to photo files — *DATA-MODEL §Deletion*

### F2 — Care event logging *(req. item 2 — ≤3 taps)*
- [x] **F2.1** One-tap water from the dashboard card + 5s undo snackbar — *UI-SPEC §2*
- [x] **F2.2** Quick-log bottom sheet: Watered / Checked / Photo / More (64dp targets)
- [x] **F2.3** Full event-type picker — 14 types — *DATA-MODEL `care_events`*
- [x] **F2.4** Type-specific fields: amount + method, check result, fertilizer + dilution, medium from/to, milestone kind, cause of death
- [x] **F2.5** Long-press droplet → detailed watering entry (amount, bottom-soak)
- [x] **F2.6** Edit / delete an existing event

### F3 — Per-plant timeline *(req. item 3)*
- [x] **F3.1** Reverse-chronological list with sticky day headers
- [x] **F3.2** Per-type icons
- [x] **F3.3** Expand-in-place on tap
- [x] **F3.4** Life events (`repotted`, `medium_changed`, `died`) render as full-width heavy dividers — *UI-SPEC §4*
- [x] **F3.5** Inline photo thumbnail strip on events that have photos

### F4 — Photos *(req. item 4)*
- [x] **F4.1** In-app camera capture, auto-attached to plant + date
- [x] **F4.2** Gallery import via Photo Picker — preserves original `taken_at`, no storage permission
- [x] **F4.3** Compress on write — longest edge ~1600px, JPEG q80, 200–500 KB — *ARCHITECTURE §5*
- [x] **F4.4** EXIF strip (location metadata must not leak into an export)
- [x] **F4.5** Photo grid per plant, 3 columns, reverse-chronological
- [x] **F4.6** Per-photo caption

### F5 — Photo compare *(req. item 5)*
- [x] **F5.1** Two-pane side-by-side (portrait, never stacked)
- [x] **F5.2** Independent filmstrip per pane
- [x] **F5.3** Elapsed time between the two, shown prominently
- [x] **F5.4** Pinch-zoom per pane + sync lock toggle
- [x] **F5.5** Defaults to oldest + newest on entry

### F6 — Reminders *(req. item 6)*
- [x] **F6.1** Per-plant check reminder with interval — *NOTIFICATIONS §5*
- [x] **F6.2** One-off task reminders with a title
- [x] **F6.3** WorkManager daily scheduler at the user's chosen hour (default 09:00 IST)
- [x] **F6.4** Three notification channels: watering checks, tasks, health alerts
- [x] **F6.5** Shade quick actions: **Watered** / **Still wet** / **Snooze 1 day**
- [x] **F6.6** Collapse — one notification per plant, maximum, ever (stable ID) — **implemented (stable id per plant); not verified with several plants overdue**
- [x] **F6.7** Bulk-clear overdue, undoable via snackbar
- [x] **F6.8** In-app **Due** list (bottom-bar destination)
- [x] **F6.9** Graceful degradation when `POST_NOTIFICATIONS` is denied — app stays fully usable
- [x] **F6.10** Reminder-tone copy pass — wording table + response parity — *UI-SPEC §7*

### F7 — Dashboard *(req. item 7)*
- [x] **F7.1** Plant cards: thumbnail, name, location · medium, days since watered
- [x] **F7.2** Attention sorting — 5-level priority — *UI-SPEC §3*
- [x] **F7.3** Due badges (never red, never a failure count)
- [x] **F7.4** Filter chips to reveal archived / dead plants
- [x] **F7.5** No loading spinner on first frame — Room Flow straight to the list

### F8 — Watering cadence *(req. item 8)*
- [x] **F8.1** Computed average interval from the log — "waters roughly every 8 days"

### F9 — Offline *(req. item 9)*
- [x] **F9.1** Verify: airplane mode, every M1 feature exercised, zero degradation

### F10 — Export *(req. item 10)*
- [x] **F10.1** Zip written via SAF `CreateDocument` — user picks destination — *driven on device 2026-09-09: picker opens with the suggested name, 1.49 MB archive written, zip valid, manifest counts match its contents*
- [x] **F10.2** `thirsttrap.json` — domain entities, not Room rows
- [x] **F10.3** `manifest.json` — schema version, app version, timestamp, row counts
- [x] **F10.4** Photos included at their relative paths
- [x] **F10.5** Streaming write — never build the archive in memory
- [x] **F10.6** ⊕ **Import** — idempotent upsert by UUID; re-importing changes nothing — *DATA-MODEL §Export format* — *driven on device 2026-09-09; it was not actually idempotent until then, see D27*

### X — Cross-cutting (M1)
- [x] **X1** ⊕ Settings screen — theme, reminder hour, default trigger, API keys, export, storage
- [x] **X2** Material 3 theme — dynamic colour, full dark theme, muted green fallback
- [x] **X3** ⊕ Empty & error states — 7 defined cases — *UI-SPEC §9*
- [x] **X4** ⊕ Accessibility pass — content descriptions, 48dp targets, 200% font scale, no colour-only meaning, reduce-motion
- [x] **X5** ⊕ Storage screen — usage, photo count, "clean up now" (orphan files + orphan rows) — *DATA-MODEL §Maintenance*
- [x] **X6** ⊕ Debug menu (debug builds only) — fire reminder now / in 10s, fast-forward due date, dump WorkManager queue
- [x] **X7** ⊕ "Reminders not arriving?" help — per-OEM instructions, battery-optimisation suggestion, test-fire button. **M1, not M2** — the target device is a Redmi on HyperOS, where reminders appear broken without it — *NOTIFICATIONS §6*

---

## 3. M2 — Weight-based watering *(req. item 17)*

*The differentiator. Full spec: `docs/WATERING-MODEL.md`. Domain and tests
complete before any UI is written.*

### Domain
- [x] **F17.1** `weight_readings` + `drying_segments` tables and migration
- [x] **F17.2** Wet anchor capture + re-capture on every `post_water` reading — *§2*
- [x] **F17.3** Provisional dry anchor (`W × 0.60`) + adaptive running minimum with the `0.30 × W` implausibility guard — *§2*
- [x] **F17.4** Segmentation — 8%-of-range jump, `watered` event, repot, 21-day gap — *§3*
- [x] **F17.5** Theil–Sen fit over the last ≤5 readings; two-point fallback — *§4*
- [x] **F17.6** Slope sanity gate (dead band ε) — *§4*
- [x] **F17.7** EWMA prior across closed segments, α = 0.3 — *§5*
- [x] **F17.8** ETA calculation with clamp (0) and cap (14 days) — *§6*
- [x] **F17.9** Suppression rules — all six conditions — *§6*
- [x] **F17.10** Confidence tiers (high / medium / low) driving UI wording — *§6*
- [x] **F17.11** Diagnostic: drying much faster than usual (>1.8× baseline) — *§7*
- [x] **F17.12** Diagnostic: pot staying heavy (<0.4× baseline) — *§7*
- [x] **F17.13** Full §9 test plan passing — correctness, robustness, segmentation, anchors, suppression, EWMA, properties

### UI
- [x] **F17.14** Calibration wizard — 4 steps, drain timer with notification — *UI-SPEC §6*
- [x] **F17.15** Weight entry — large one-handed keypad, smart context default
- [x] **F17.16** Depletion bar on the dashboard card (percentage-labelled, threshold marked, hidden when uncalibrated)
- [x] **F17.17** Weight history chart — anchors, trigger line, segment markers, dashed forward projection
- [x] **F17.18** Recalibration prompt on repot / medium change / container edit
- [x] **F17.19** Mark a reading as excluded (bad weigh-in)
- [x] **F17.20** ⊕ Scale-guidance help — resolution vs repeatability, small-pot warning

### Reminders (M2 additions)
- [x] **F17.21** Prediction-driven reminder intervals, rescheduled on each new reading — *NOTIFICATIONS §5*
- [x] **F17.22** Exact-alarm opt-in toggle + in-app explainer + capability re-check with silent fallback — *NOTIFICATIONS §1*

---

## 4. M3 — Depth

*In value order. Timelapse is deliberately last.*

- [x] **F20** Propagation pipeline board *(req. 20)* — kanban: cutting → callusing → rooting → potted → established, with days-in-stage
- [x] **F24** Post-mortem template *(req. 24)* — on marking dead: timeline recap, suspected cause, "what I'd do differently", auto-linked photo history
- [x] **F21** QR stickers on pots *(req. 21)* — ML Kit Code Scanner (no camera permission) → deep-link to that plant's quick-log
- [x] **F19** Diagnosis checklists *(req. 19)* — guided trees: fuzz (dunk test: root hairs vs mould), browning (firm vs mushy), cut-face reading (clean vs brown ring), leaf-drop triage
- [x] **F22** Light meter *(req. 22)* — `SensorManager` + `TYPE_LIGHT`, lux per location, rated against the plant's needs
- [x] **F23** Ambient context *(req. 23)* — room temp/humidity **per location** (not per plant), manual entry. Feeds `explainDryingChange`, which sits beside the drying diagnostic and says whether the room accounts for a change. Explains a drying rate; never predicts one. The free-weather-API half of the requirement's "or" is not built — see handover D18
- [x] **F18** Timelapse builder *(req. 18)* — a plate series with a scrubber and a play control. Renders no video and writes no file: the requirement asks for *scrubbable*, and an encoder would be the first thing in the app that could fail silently on a device. Captions count days, not frames
- [x] **F26** Species care notes — two tiers, 49 hand-written + 115 generated from `biologiste95/plant-dataset` (Unlicense) and the ASPCA toxicity list, 497 aliases. Curated always wins; the tiers are marked differently in the UI. *`docs/SPECIES-CATALOGUE.md`*
- [x] **F26.1** Build-time GBIF name resolution — 62 outdated botanical names rewritten to accepted ones, so an old plant label still finds the right plant
- [x] **F26.2** Online name lookup, opt-in and off by default — GBIF + a Wikipedia link when the catalogue has nothing. Resolves *names*, never care advice, so it cannot produce a wrong watering schedule

---

## 5. M4 — Phase 2

- [ ] **F11** Experiments module *(req. 11)* — subjects, start date, variable tested, per-day notes/photos, conclusion
- [ ] **F12** Notes with `[[plant]]` cross-links *(req. 12)*
- [x] **F13** Location light/placement notes + `moved` event *(req. 13)* — "Places": a gazetteer of the spots plants live in, with a note and the last light reading, which the F22 meter now records instead of discarding. `CareEventType.MOVED` had existed since the first schema and was **never emitted by anything**; a plant's location changing now writes one
- [x] **F14** Fertilizer inventory + dilution calculator *(req. 14)* — "Feeding": the cupboard and the arithmetic on one page. Pick a can size and every bottle shows its dose. Refuses on a dilution it cannot parse and on a dose too small to pour, rather than printing a number nobody can act on
- [x] **F15** Stats *(req. 15)* — "Figures": waterings by month, what became of them, days-to-root. Tables with column heads and rules; the only chart is a proportional rule. The survival rate is computed **only over plants that have actually left** and is written as a sentence, not a percentage on its own line — a living plant is not a pending failure
- [ ] **F16** Cloud backup *(req. 16)* — optional account, **never required for core use**
- [ ] **F25c** Offline TFLite classifier — AIY `plants_V1` (~7 MB) or MobileNet on Pl@ntNet-300K; labelled "rough guess", genus-level trust at best. **The chosen route (2026-09-09, D29)**
- [ ] **F25b** Kindwise plant.health — bring-your-own-API-key settings field, costs the app nothing. Opt-in second tier, only if F25c proves too weak to be useful
- [x] ~~**F25** Pl@ntNet ID~~ — **dropped 2026-09-09, D29.** A shared key shipped in the APK gets extracted and its quota burned, so in practice the user must bring their own key, at which point F25 and F25b are the same feature

---

## 6. Explicit non-goals

From the requirements doc, restated so they stay decided:

- ✗ No social feed
- ✗ No gamification or streaks — *a missed day must not feel like failure*
- ✗ No auto-watering hardware integration (v1)
- ✗ No *dependency* on any plant-ID API in the core loop
- ✗ No required account, ever
- ✗ No sensor/hardware dependency for the weight model — manual weighing only
- ✗ No resistive moisture probes (they corrode within weeks)
- ✗ No multi-user or sharing
- ✗ No `USE_EXACT_ALARM` — Play Store policy violation for this app category

---

## 7. The nine additions ⊕

Not in `plant-tracker-requirements.md`; surfaced while writing the specs.
Flagged so you can reject any of them.

| ID | Addition | Why |
|---|---|---|
| **F1.6** | Hard delete with export-first warning | Archive alone leaves no way to remove a mistaken entry |
| **F10.6** | **Import** | The requirements specify export only. Export without import is a backup you cannot restore — it does not actually deliver the portability promise |
| **X1** | Settings screen | Implied by API keys, theme, reminder hour, trigger default; never stated |
| **X3** | Empty & error states | 7 cases that otherwise become blank screens or crashes |
| **X4** | Accessibility pass | Named as an explicit deliverable rather than assumed |
| **X5** | Storage screen + orphan cleanup | SQLite cascade cannot delete files; orphans accumulate silently |
| **X6** | Debug menu | Notification testing is manual; without this each test costs a day of waiting |
| **F17.20** | Scale-guidance help | The method fails silently if the user weighs inconsistently |
| **X7** | "Reminders not arriving?" help | OEM battery killers are unfixable in code; the only answer is guiding the user |

**F10.6 (import) is the one I would push back on hardest if you cut it.** The
requirements call backup and portability non-negotiable and cite the Vera
shutdown as the cautionary tale — but an export you cannot re-import is not a
backup, it is an archive.

---

## Counts

| Milestone | Items |
|---|---|
| M0 prototypes | 2 |
| M1 MVP | 58 |
| M2 weight | 22 |
| M3 depth | 7 |
| M4 phase 2 | 9 |
| **Total** | **98** |

M3 and M4 items are single-line because they are not specified in detail yet —
they will expand into sub-items when their milestone is planned. Do not treat
"7 items" as "7 units of work".

---

## Progress — 2026-09-06

Counted from the code, not from memory.

| Milestone | Done | In progress | Not started |
|---|---|---|---|
| M0 prototypes | 2 | 0 | 0 |
| M1 MVP | 6 | 7 | 45 |
| M2 weight | 13 | 0 | 9 |
| M3 depth | 0 | 0 | 7 |
| M4 phase 2 | 0 | 0 | 9 |
| **Total** | **21** | **7** | **70** |

**The M2 domain is essentially complete** — the entire drying model, under 57
passing JVM tests that need no device. What remains in M2 is persistence
(`F17.1`) and the weight UI.

**M1 is barely started**, and everything that exists runs on in-memory fake
data that does not survive the process dying. Absent from the codebase
entirely: Room, WorkManager, Hilt, DataStore, Coil, the Photo Picker,
navigation, and the export zip.

**Update, later the same day:** M0.5 steps 1-4 landed. `F6.3` is now done for
real - a WorkManager periodic job, registered with the system JobScheduler and
verified there. Room persistence, plant management, the Due list and the X7
help screen all ship. Recount below.

## Progress — 2026-09-06, after M0.5 steps 1-4

| | Count |
|---|---|
| Done | 34 |
| In progress | 6 |
| Not started | 58 |

Verified on the phone: real plants survived the v1→v2 auto-migration; the
daily sweep is registered with the system JobScheduler; a notification action
tapped from the shade writes a real `care_event` and the card reflects it.

Still open: step 5 (timeline, `F3.1`), and the whole M2 weight UI.

## Progress — 2026-09-06, M0.5 complete

| | Count |
|---|---|
| Done | 39 |
| In progress | 5 |
| Not started | 54 |

M0.5 steps 1-5 all shipped and were verified on the target phone. The M2
domain remains complete-but-headless: the drying model is built and tested,
with no persistence or UI in front of it.
