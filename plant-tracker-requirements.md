# Plant Tracker — Requirements

A mobile app for tracking watering, care, propagation, and experiments across a
small personal plant collection. Written for a solo user (initially), offline-first,
zero-cost to run.

## Market research summary (Sept 2026)

Four research passes (competitor apps, plant-ID APIs, gravimetric watering,
mobile stack) confirmed the positioning. Full stack report:
`mobile-stack-comparison.md` in this directory.

**The gap is real.** No mainstream app (Planta, Greg, Blossom, PictureThis,
PlantIn…) supports weight-based watering. The closest niche apps: ColtivApp
logs pot weight and graphs it but does no drying-curve *prediction*; Evapotrack
tracks water volume, not weight. Weight-log → fitted drying curve → predicted
watering date does not exist for consumers, despite being validated commercial
greenhouse practice (Control Dekk OASIS, 30MHz load-cell scales).

**Category-wide complaints to design against** (from review analysis):
1. Calendar/climate schedules that ignore the actual pot ("Greg said 6–14
   days, plants needed daily water") — measurement beats modeling.
2. Billing hostility: auto-renewing trials, paywall creep (PictureThis,
   Planta, Blossom). Trust positioning — free/one-time + full export — is
   held only by tiny indies.
3. Reminder hygiene: no snooze (Blossom), piled-up overdue nags (Planta).
4. Data lock-in: Greg and Planta have no real export; the Vera app shutdown
   (2024) stranded all its users' data — the standing cautionary tale.

**Validated-elsewhere features**: propagation tracking and photo-timeline
diagnosis are each spawning single-purpose indie apps (Rootling, Propago,
Propagation Journal; Planta's Progress Event) — demand is proven, but nobody
has folded them into one care tracker.

## Why (design drivers)

These come from real usage patterns, not guesses:

- Watering is decided by **pot weight / dryness checks**, not a fixed calendar.
  The app must support "check reminders" and logged observations — not just
  "water every N days" alarms that train you to ignore the plant.
- Diagnosis happens through **photo comparison over time** (is the brown spot
  bigger than yesterday? is the moss greener than last week?). Time-stamped
  photo history per plant is a first-class feature, not an attachment.
- Plants have **life events**: repotted, cut back, rot surgery, moved to water,
  transplanted to soil, died. The history of a plant is a timeline of events,
  not just waterings.
- Some "plants" are **experiments** (banana-water germination test, volunteer
  seedling identification). The app should handle short-lived subjects cheaply.

## Core entities (data model)

### Plant
- `id`, `name` (user's nickname, e.g. "marbled pothos"), `species` (free text +
  optional photo-identified suggestion), `acquired_date`, `source` (bought /
  cutting / gift / volunteer)
- `medium`: soil | water | sphagnum | semi-hydro — **changeable over time, and
  changes are logged as events** (medium conversion is a high-risk operation
  worth flagging in history)
- `location` (free text or picklist: desk, windowsill, terrarium, balcony)
- `status`: active | dormant | dead | given-away (dead plants keep their
  history — post-mortems are half the value)
- `container`: pot size / bottle / jar, drainage yes/no
- Optional per-plant care profile: target dryness before watering
  (e.g. "top 2–3 cm dry" / "nearly weightless" / "keep damp"), light needs,
  fertilizer cadence

### Care Event (the central log)
- `plant_id`, `timestamp`, `type`, `note` (free text), `photos[]`
- Types (extensible enum):
  - `watered` — with optional amount (mL) and method (top / bottom-soak)
  - `checked` — dryness/weight check with result ("still heavy", "light, water
    tomorrow"); checks that result in *not* watering are worth logging too
  - `fertilized` — what and dilution (e.g. "banana tea 1:5", "NPK pinch/L")
  - `water_changed` — for water-propagation subjects
  - `repotted` / `medium_changed` / `pruned` / `treated` (peroxide, cinnamon,
    fungicide) / `pest_or_disease` / `weeded` (volunteer pulled)
  - `observation` — free-form: "new leaf", "browning on stem", "fuzz on roots"
  - `milestone` — rooted, first new leaf, flowered, transplanted
  - `died` — with cause guess (post-mortem note)

### Photo
- Attached to events; every photo carries timestamp + plant automatically.
- **Compare view requirement**: pick any two photos of the same plant and see
  them side-by-side (this is the diagnosis workflow).

### Reminder
- Per plant, two kinds:
  - **Check reminder**: "it's been N days since last watering — go lift the
    pot" (N learned or user-set). Completing a check logs a `checked` event.
  - **Task reminder**: one-off ("remove humidity cover in 2 weeks", "transplant
    when roots hit 3–5 cm — check Friday").
- Snooze and "checked, still wet" must be one tap. The reminder is a prompt to
  *assess*, never an instruction to water blindly.
- **Reminder hygiene requirements** (top category complaints — Blossom ships
  with no snooze; Planta piles up overdue nags): every reminder is snoozable,
  overdue reminders can be bulk-cleared, and a missed reminder collapses
  silently into the next one — no guilt stack.

### Experiment (optional, phase 2)
- Named container for a short study: subjects, start date, variable being
  tested, linked photos/notes per day, conclusion field.
- Example: "flax germination — banana water vs plain, 30 seeds each."

## Functional requirements

### MVP (phase 1)
1. Add/edit/archive plants with photo and medium.
2. Log care events with ≤3 taps for the common case (watered / checked).
3. Per-plant timeline view: events + photos, newest first.
4. Photo capture in-app, auto-attached to plant + date.
5. Side-by-side photo compare for one plant.
6. Check reminders with per-plant interval; local notifications.
7. Dashboard/home: all plants as cards showing name, thumbnail, days since
   last watered, and any due reminders — sorted by "most needs attention".
8. Watering cadence display: computed average interval from the log
   ("waters roughly every 8 days") shown per plant.
9. All data local on device; works fully offline.
10. Export: full data as JSON + photos to a zip/folder (backup and portability
    are non-negotiable — this is a diary, losing it hurts).

### Phase 2
11. Experiments module (above).
12. Notes with `[[plant]]` cross-links (e.g. one rot saga referencing events
    across cuttings).
13. Light/placement notes per location; "moved plant" event.
14. Fertilizer inventory + dilution calculator (mL of concentrate per L).
15. Simple stats: waterings per month, survival rate, average days-to-root for
    propagations.
16. Cloud backup (optional account) — never required for core use.

### Phase 1.5 — the differentiator: weight-based watering
*Promoted from "enhancement" to the feature the app exists for: research
confirmed nothing on the market does this, and the method is validated
commercial greenhouse practice (weigh-to-target irrigation; the MAD /
managed-allowable-depletion framework).*

17. **Weight logging + drying curve + prediction.**
    - **Calibration per plant** (one-time, ~2 min + one drying cycle):
      water thoroughly, drain 30–60 min, weigh → **wet anchor** (container
      capacity). No forced dry-out for the dry anchor: start with a provisional
      estimate (wet minus ~35–45% for peat/coco mixes) and adaptively replace
      it with the lowest weight at which the user actually watered. Re-capture
      the wet anchor at every post-watering weigh; prompt recalibration on
      repot.
    - **Trigger**: depletion fraction of the wet–dry range, default 0.5
      (the commercial MAD guideline), per-species slider (succulents 0.7–0.8,
      ferns/moisture-lovers ~0.3).
    - **Prediction model** (~50 lines, no ML): segment readings by watering
      events (a jump >5–10% of range starts a new segment; never fit across a
      watering); within a segment, Theil–Sen or recency-weighted linear fit
      over the last 3–5 readings (drying is near-linear in the pre-trigger
      regime, and a linear fit errs conservatively — predicts dry slightly
      early); ETA = (now − threshold)/|slope|, clamped, capped (">14 days"),
      and **never displayed with fewer than 2–3 post-watering points or a
      near-zero slope** — show "need another reading" instead. Keep a
      per-plant EWMA of past segments' slopes as a prior so day 1 after
      watering already has a rough estimate; the EWMA also tracks the 2–5×
      seasonal drying-rate swing automatically.
    - **Diagnostics for free**: a sudden rate *increase* flags cracked/
      channeling substrate (water bypassing the root ball); a rate *collapse*
      (pot staying heavy) flags possible root rot. Surface both as gentle
      alerts — the drying curve is a health monitor, not just a timer.
    - **UX**: show weight as a bar/fraction between the anchors, not raw
      grams; log flow is "weigh before watering, weigh after". Design for
      5–10 g scale resolution (a ₹300–800 1 g kitchen scale is a luxury, not
      a requirement — a 6-inch pot swings 300–400 g between wet and dry, so
      the signal dwarfs the noise). Repeatability matters more than
      resolution: same scale, same placement, not right after misting.
    - **Manual-weigh only, no hardware dependency**: the consumer smart-
      sensor graveyard (Parrot Flower Power et al.) shows cloud/hardware-
      dependent products strand their users; a dumb scale plus this app has
      no dead-server failure mode. Cheap capacitive moisture probes may
      later join as a qualitative cross-check, never the primary signal
      (resistive probes corrode in weeks; skip entirely).
18. **Timelapse builder**: auto-assemble a plant's photo history into a
    scrubbable timelapse (growth or symptom progression).
19. **Diagnosis checklists**: guided decision trees for common panics —
    fuzz (dunk test: root hairs vs mould), browning (firm vs mushy),
    cut-face reading (clean vs brown ring), leaf drop triage.
20. **Propagation pipeline board**: kanban view — cutting → callusing →
    rooting → potted → established, with days-in-stage per plant.
21. **QR stickers on pots**: scan → that plant's quick-log screen opens.
    Physical answer to the 3-tap problem.
22. **Light meter**: use the phone's ambient light sensor to measure lux at
    each location and rate it against the plant's needs.
23. **Ambient context**: room temp/humidity per location (manual entry or free
    weather API) so seasonal drying-rate changes are explainable.
24. **Post-mortem template**: on marking a plant dead — timeline recap,
    suspected cause, "what I'd do differently". Auto-links the plant's photo
    history.
25. **Plant ID (free API)** — verified Sept 2026: **Pl@ntNet** free tier is
    500 IDs/day (one free account per person; no commercial-use prohibition on
    the free tier; attribution encouraged, not required). Single multipart
    `POST /v2/identify/all?api-key=…` with up to 5 images and per-image
    `organs` tags (`leaf|flower|fruit|bark|auto`); response includes
    confidence scores, common names, and remaining quota (show the quota
    in-app). Bonus: a `/v2/diseases/identify` endpoint under the same quota —
    limited species/pathology coverage, but free disease suggestions.
    Design rule unchanged: ID is a *suggestion, never a requirement* —
    online-only, optional, app fully functional without it.
    - Accuracy (per published studies): ~70% to species when its own
      confidence is high, much worse without flowers; poor on cuttings and
      seedlings. In-app guidance: ID from a healthy adult leaf/flower,
      photograph symptoms separately.
    - **Power-user disease option**: Kindwise plant.health (548+ disease
      classes, ~€0.05/scan, no free tier) as a bring-your-own-API-key
      settings field — costs the app nothing, unlocks best-in-class
      diagnosis for users who want it. Treat all disease output as
      suggestion: field accuracy of such models drops steeply vs lab.
    - **iNaturalist's CV API is not publicly available** (case-by-case,
      fee-based) — do not plan around it. Google Lens has no API; Bing
      visual search is retired.
    - **Offline fallback (optional)**: ship a small TFLite classifier —
      Google's AIY `plants_V1` (~7 MB, ~2,100 taxa, permissive license,
      zero training effort) or a self-trained MobileNet on Pl@ntNet-300K
      (~15 MB, 1,081 species) — labeled "rough guess; go online for a
      better ID". Genus-level trust at best.

### Explicit non-goals
- No social feed, no gamification streaks (a missed day must not feel like
  failure — plants aren't Duolingo).
- No auto-watering hardware integration (v1).
- No *dependency* on any plant-ID API in the core loop — free-tier ID as an
  optional suggestion is welcome (see item 25); a required or paid one is not.

## Non-functional requirements
- **Offline-first**: the core loop — logging, reminders, timeline, photos,
  weight prediction — works with no network, ever. Online features (plant ID,
  optional backup) are additive and degrade gracefully when absent. Sync, if
  ever, is additive.
  - *As built (2026-09-08):* verified offline with no network at all — cold
    launch, logging, export and import all work. One exception: **QR scanning**
    needs a network the first time, because its scanner is a Play Services
    module fetched on demand. It is prewarmed at startup and says so when it
    cannot. See handover D10 for why the app now carries `INTERNET`.
- **Fast logging**: cold open → watering logged in under 10 seconds.
- **Cheap storage**: photos compressed (~200–500 KB each); a 3-year history of
  10 plants should fit in < 1 GB.
- **Privacy**: photos and notes never leave the device without explicit export
  or opt-in backup.
- **Android first** (your device), iOS later if ever.

## Stack (revised 2026-09-06 — see `docs/HANDOVER.md` decision D1)

> **Superseded.** This section originally specified Flutter. The stack is now
> **native Kotlin**. The reasoning is recorded in `docs/HANDOVER.md` (D1) and
> the detail lives in `docs/ARCHITECTURE.md`. The original Flutter text is kept
> below the line for provenance — do not build from it.

- **Native Kotlin**, Jetpack Compose, Material 3. The original research ranked
  Kotlin the runner-up and rejected it solely for "a platform learning tax with
  no cross-platform payoff" — but the developer already knows Kotlin and
  Android, so that tax is zero, and iOS is only a maybe. What remains of that
  sentence ("most reliable, smallest APK") favours Kotlin.
- Packages: **Room** (SQLite), **WorkManager** + `NotificationManagerCompat`,
  Photo Picker + `TakePicture`, Compose `Canvas`/**Vico** (weight curves),
  **ML Kit** `GmsBarcodeScanning` (QR), `ZipOutputStream` + SAF (export),
  **Hilt**, **Coil**, **Ktor**. Photos in app-private storage, DB stores
  relative paths.
- `:core:domain` is a pure JVM module with no Android on its classpath, so a
  future iOS port is a KMP configuration change rather than a rewrite.
- **Notification reality on Android 14/15**: exact-alarm permission is denied
  by default and needs a user grant — so ThirstTrap **defaults to inexact
  WorkManager scheduling** and offers exact alarms as an opt-in. See
  `docs/NOTIFICATIONS.md` for the full reasoning and the manual test matrix.
  Still the app's hardest feature; budget real time for it.

*Still valid from the original research, and not Flutter-specific:* Expo is
eliminated (`expo-notifications` has open correctness bugs in Android local
scheduling — the one feature that must not fail — and Expo Go can't even test
notifications since SDK 53). PWA is disqualified (no web standard for offline
scheduled notifications).

<details>
<summary>Original Flutter decision (superseded — do not build from this)</summary>

- **Flutter.** Expo is eliminated: `expo-notifications` has open correctness
  bugs in Android local scheduling — the one feature that must not fail — and
  Expo Go can't even test notifications since SDK 53. PWA is disqualified (no
  web standard for offline scheduled notifications). Native Kotlin is the
  runner-up (most reliable, smallest APK) but costs a platform learning tax
  with no cross-platform payoff.
- Packages: `drift` (SQLite), `flutter_local_notifications` + `timezone`,
  `image_picker`, `fl_chart`, `mobile_scanner`, `share_plus` + `archive`.
- Notification advice: use `exactAllowWhileIdle` scheduling; register the boot
  receiver so reminders survive restarts.

</details>

## Name: **ThirstTrap**

Chosen Sept 2026. A watering-reminder app named after the thing that catches
thirst. Collision-checked: no existing plant app or GitHub project under this
name (the crowded names in the space are "Water My Plants" variants and
"Water Me"); the only namespace neighbour is the social-media term itself,
which is the joke.

Plan: open source. Repo name `thirsttrap` (lowercase, one word).

Runners-up kept for reference: GreenGram (gram = weight unit + moong, the
germination-test bean), DrydownDiary, PotLedger, Tula/Thulam, Heft, Drydown,
Node. (LeafLedger was liked but is taken by an existing plant-care app;
Verdant is saturated across categories.)

## The two hard UX problems (worth prototyping first)
1. **The 3-tap log**: home screen → plant card → "watered ✓". If logging is
   slower than a paper note, the app loses to paper and dies in a week.
2. **Reminder tone**: reminders must ask "check the pot", and make "checked —
   not needed yet" as satisfying to tap as "watered". Get this wrong and the
   app trains calendar-watering, which is the exact failure mode the weight
   method exists to avoid.
