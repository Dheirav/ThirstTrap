# ThirstTrap — Handover

**This is the live state document.** Read it first, every session. The README
is the pitch and may lag; `plant-tracker-requirements.md` is the *what and
why*; this file is the *where we are*.

Last updated: 2026-09-30

---

## Status

**Phase: feature-complete, not defect-free. All 102 features are built or
deliberately closed. Schema v14, 277 JVM tests and 16 instrumented tests
passing, running daily on the target phone.**

**The council review is closed.** A twelve-agent council confirmed 21 findings
on 2026-09-30 (19 distinct, two pairs being the same defect found twice) and all
19 are now fixed: D43, D46 and D47. `docs/COUNCIL-REVIEW.md` keeps the full list
with its evidence, because the reasoning is the useful part and several findings
corrected their own original claim.

Repo: https://github.com/Dheirav/ThirstTrap (branch `main`).

- **277 JVM tests** across `:core:domain` and `:core:ui`, running in about a
  second with no device attached.
- **13 instrumented tests** in `:core:data`, covering schema migrations, the
  reminder planning that gathers its own inputs, and the export/import round
  trip. These need a phone.
- Schema is at **v11**, every step an auto-migration, `exportSchema` on and
  `schemas/*.json` committed. `fallbackToDestructiveMigration` appears nowhere.
- The app runs daily on a Redmi Note 15 Pro against four real plants. Most of
  the defects in the decisions log below were found that way rather than at the
  desk, which is the single most useful habit this project has.

### What is left

Six features, all M4 Phase 2, plus one decision:

- ~~**F11** experiments, **F12** `[[plant]]` cross-links~~ - both shipped
  2026-09-27, see D34. Remaining: **F25c** offline plant identification with
  **F25b** as an opt-in second tier (F25 itself dropped, see D29), ~~the GMS
  scanner replacement~~ (done, see D35), and the
  widget ideas parked in the backlog section.
- ~~**F16 cloud backup** is unstarted *and undecided*.~~ Decided no, 2026-09-27,
  see D33. Local-only is the product, not a gap in it.
- ~~The per-plant depletion trigger has no control since D28 removed the dialog
  that was its only one.~~ Resolved 2026-09-11, see D30: a slider in Edit
  plant, shown only where weight means anything. Saving an edit now also
  replans the check reminder, because the trigger moves the prediction that
  the interval is derived from.

### The habit worth keeping

Six times now the same defect has appeared: logic that is correct, tested,
reads properly, and is wired to nothing. `X4` reduce-motion, `CareEventType.MOVED`,
`deleteReading`, `resolveIntervalDays` at eight call sites, a calibration dialog
nothing could open, and a duplicate button. It comes from changing one end of a
path and not walking the other. A deliberate pass through `:core:domain` asking
"who calls this, and with what" is worth more than the next feature.

That pass was made on 2026-09-11 (see D30): every public domain function was
checked for production callers, and every repository method, event type,
route and reading context for a reachable entry point. No seventh instance
found. The audit also replayed the phone's real diary through
`assembleWeightState` and the pipeline computed correct anchors, slopes and
ETAs end to end, which retired a suspicion that derived-on-read state was
another unwired path - it is a design, and it works.

## What exists

| File | Role |
|---|---|
| `plant-tracker-requirements.md` | Product requirements. The *what* and *why*. Stable. |
| `docs/HANDOVER.md` | This file. Current state. |
| `docs/ARCHITECTURE.md` | Module layout, layering, DI, build config, testing strategy. |
| `docs/DATA-MODEL.md` | Every table, column, index, migration rule. |
| `docs/WATERING-MODEL.md` | The drying-curve algorithm in full. The differentiator. |
| `docs/NOTIFICATIONS.md` | Reminder scheduling and Android delivery reality. |
| `docs/UI-SPEC.md` | Screen-by-screen specification. |
| `docs/ROADMAP.md` | Build order, milestones, definition of done. |
| `docs/FEATURES.md` | Flat checklist of all 101 features, IDs mapped to requirements items. |
| `docs/DEVICE.md` | adb reality, phone constraints, toolchain. Read before debugging anything. |
| `docs/DESIGN-RESEARCH.md` | The measured basis for the colour system and the almanac typography. |
| `docs/SPECIES-CATALOGUE.md` | How the 164-species bundled catalogue is generated, and from what. |
| `README.md` | The public pitch. Written for someone who has never seen the repo. |

## What is missing

- **`mobile-stack-comparison.md`** — cited by `plant-tracker-requirements.md`
  as living "in this directory". It does not exist and has been **written off**
  (2026-09-06). The stack section of the requirements doc stands as
  summary-only; decision D1 supersedes its conclusion anyway.
- **`docs/ROADMAP.md` still describes M0 as upcoming.** Its milestone breakdown
  was written before any of it was built and has not been revised since. The
  build order it lays out was followed; treat it as the original plan rather
  than a status report, and read the Status section above instead.

---

## Decisions log

Decisions that are settled, with the reasoning, so they are not relitigated.

### D1 — Native Kotlin, not Flutter (2026-09-06) — *supersedes the requirements doc*

The requirements doc specified Flutter. That is now **overridden**.

The doc's own sentence was: *"Native Kotlin is the runner-up (most reliable,
smallest APK) but costs a platform learning tax with no cross-platform
payoff."* The developer already knows Kotlin and Android, so the learning tax
— the only argument against it — is zero. What remains of that sentence
favours Kotlin.

- iOS is "maybe, if it goes well" — a conditional on a project that does not
  yet exist. Not a reason to pay abstraction cost today.
- The reliability advantage lands exactly on notifications, which the doc
  itself calls the app's hardest feature.
- Item 22 (light meter) had no maintained Flutter plugin and would have needed
  a Kotlin platform channel anyway. Native, it is a few lines.

**Still valid from the original research:** Expo is eliminated
(`expo-notifications` Android scheduling bugs; Expo Go cannot test
notifications since SDK 53). PWA is eliminated (no web standard for offline
scheduled notifications). Those eliminations were never Flutter-specific.

### D2 — Keep `:core:domain` free of Android (2026-09-06)

The KMP hedge, bought cheaply. `:core:domain` is a **pure `kotlin("jvm")`
module** — not an Android library — so the Android SDK is invisible to it at
compile time and the rule is enforced by the compiler rather than by
discipline. If iOS ever happens, that module moves to `commonMain` and gets a
SwiftUI layer, instead of being rewritten.

Do **not** set up Kotlin Multiplatform now. Just do not poison the module.

### D3 — Offline-first confirmed (2026-09-06)

Questioned and re-affirmed. It is not extra work here — it is the default
shape of a single-user app with a local database. Making it online-first would
mean adding auth, a hosted DB, sync conflict resolution and a hosting bill,
which contradicts "zero-cost to run" and reintroduces the exact Vera-shutdown
failure mode the requirements doc names as its cautionary tale.

Cloud backup stays as requirements item 16: optional, never required.

### D4 — Inexact alarms by default (2026-09-06) — *deviates from the requirements doc*

The requirements doc says to use `exactAllowWhileIdle`. **Reconsidered.** A
watering *check* reminder is not time-critical to the minute, and exact alarms
carry the single worst permission story on modern Android. Defaulting to
WorkManager + inexact windowed alarms removes the app's hardest permission
problem for no real loss in usefulness. Exact scheduling becomes an opt-in.

Full reasoning and the fallback ladder: `docs/NOTIFICATIONS.md`.

### D5 — UUID text primary keys (2026-09-06)

Not autoincrement integers. Costs a slightly larger index; buys collision-free
IDs if optional sync (item 16) is ever built, and makes export/import round
trips safe. See `docs/DATA-MODEL.md`.

### D6 — Target device: REDMI Note 15 Pro 5G (2026-09-06)

`25080RABDG`, **Android 16 / API 36**, HyperOS OS3.0, 7.6 GB, arm64-v8a,
1280×2772 @ 520 dpi. Connected over **USB via the Windows adb server** —
verified, not assumed. Full detail: `docs/DEVICE.md`.

Two consequences that pull in opposite directions:

- **Good:** `input` injection, unfiltered `logcat`, `exec-out screencap` and
  `uiautomator dump` all work here. UI flows can be driven programmatically,
  so testing does not need a human for every iteration — only for the
  judgement calls that were always the user's.
- **Bad:** it is still HyperOS. **Autostart is off by default** and the system
  kills scheduled work whichever scheduling approach the app uses. The
  "Reminders not arriving?" help screen is therefore an **M1** deliverable
  (`X7`), and the NOTIFICATIONS §7 matrix must be run with Autostart both on
  and off — the "off" run is what an ordinary user gets.

`compileSdk`/`targetSdk` stay at **35** through M1 despite the device being API
36; only `platforms/android-35` is installed, and targeting 36 forces
edge-to-edge and predictive back. Revisit at M2. See `docs/DEVICE.md` §2.

`minSdk 26` stands — it costs little and nothing on this device depends on it.

The **Redmi Note 12 Pro** (Android 13) remains available as an older-Android
compatibility check. It cannot do input injection at all (no SIM → no Mi
account → no INJECT_EVENTS); `docs/DEVICE.md` §3 has its constraints.

### D7 — Import ships in M1 alongside export (2026-09-06)

`F10.6` confirmed in scope for the MVP. An export that cannot be re-imported is
an archive, not a backup, and the requirements call backup non-negotiable.

### D8 — Package name `dev.dheirav.thirsttrap` (2026-09-06)

Confirmed.

### D10 — The app now holds INTERNET, deliberately (2026-09-08)

Adding the in-app QR scanner (`F21`) pulled `play-services-code-scanner`, which
brings `INTERNET` transitively via `datatransport:transport-backend-cct` -
Google's telemetry upload backend - along with a `TransportBackendDiscovery`
service. The app's own manifest still declares only `POST_NOTIFICATIONS` and
`VIBRATE`.

This was raised as a regression and accepted by the user, who preferred an
in-app scan to a two-app one. What it changes:

- The offline claim weakens from **"the OS would refuse a network call"** to
  **"the app does not make one"**. Still true of our code, no longer enforced
  from outside it. Any future session verifying offline behaviour should test
  the behaviour rather than trusting the permission list.
- **Scanning specifically does not work offline the first time.** The scanner is
  a Play Services module fetched on demand, so it is the one feature in the app
  that needs a network. It is prewarmed at startup and says so plainly when the
  module is missing.

The alternative considered was CameraX plus the ZXing already present for
generation - no `INTERNET`, but a `CAMERA` runtime permission and roughly 200
lines. Worth revisiting if the telemetry component ever matters more than the
convenience.

Note for a future iOS port: the scanner is the *least* portable part of this.
`play-services-code-scanner` is Android-only and would be rewritten against
AVFoundation. What ports for free is the URI itself - `thirsttrap://plant/{id}`
- which iOS handles natively through `CFBundleURLTypes`.

### D11 — Two-tier species catalogue, generated from public-domain data (2026-09-08)

The 48 hand-written entries covered the collection and almost nothing else.
Rather than an API with a key, a quota and a shutdown date, the catalogue is
seeded from `biologiste95/plant-dataset` (Unlicense) plus the ASPCA toxicity
list, both vendored under `tools/species-sources/`. 49 curated + 115 generated,
with 497 aliases.

The two tiers are marked and rendered differently. A curated entry always wins,
unconditionally - only it knows what actually kills the plant. Full reasoning,
the code legend and its calibration in `docs/SPECIES-CATALOGUE.md`.

What the dataset actually contains is worth recording, because its README
oversells it: toxicity is filled on **10 of 250** rows, general care prose on
**3**, problems on **2**, and the advertised commercial-light column on **none**.
The real payload is a botanical name, common names, and four *undocumented*
ordinal codes. The codes are genuinely useful - watering maps straight onto
`depletionTrigger` - but the legend had to be reverse-engineered and is only
trustworthy because it agrees with the hand-written tier, which was written
earlier and independently.

Two rules keep the tiers honest. A generated entry the curated tier already
answers is **dropped** (102 of 217), which makes a contradiction structurally
impossible; and the dropped entry **donates its aliases** to the curated entry
that shadowed it (186 of them), so an old name on a plant label still resolves.

The cross-check found a real bug in the hand-written tier: `Cactus` applied a
0.85 trigger to Christmas cactus while its own light field said Christmas cactus
was the exception. *Schlumbergera* is now a separate curated entry at 0.5.

### D12 — Online lookup resolves names, never care (2026-09-08)

The fallback for an unrecognised name is GBIF plus a Wikipedia link, not a care
API. `SpeciesLookupService` has no field for care advice, so a wrong answer
costs a wrong link and can never produce a wrong watering schedule. Nothing it
returns feeds `SpeciesCare`, `depletionTrigger` or any prediction.

Off by default, under Settings → Network, and consent is checked inside the
service rather than at call sites so there is one place to get it wrong. It
sends the typed species name and nothing else. `INTERNET` and
`ACCESS_NETWORK_STATE` are now declared explicitly in the manifest rather than
inherited through the merge described in D10 - the app uses one deliberately, so
it should say so.

Built on `HttpURLConnection`. Two calls to two keyless public APIs do not
justify pulling OkHttp and its interceptor stack into an app whose whole
argument is that it does not need a network.

### D13 — Every colour role is set explicitly (2026-09-08)

`DarkScheme` and `LightScheme` set `surface` and `background` and left the
`surfaceContainer*` family to `darkColorScheme()`, whose defaults are M3's
purple-tinted baseline neutrals. Material's filled `Card` takes its container
from those roles, so **every card in dark mode rendered at hue ~300 against a
hue ~156 background** - about 145 degrees out - at 1.08:1 contrast, below
perceptual threshold. With `elevation = 1.dp` drawing only a shadow, invisible
on near-black, dark mode had no visible card at all.

An unset role does not fall back to something neutral. It falls back to someone
else's palette. Both schemes are now complete, including `surfaceDim`,
`surfaceBright`, `outlineVariant`, `scrim`, the inverse roles and the error
family.

Two contrast failures were fixed while measuring: light `tertiary` `#C0603F` was
4.02:1 on the background (now `#A4482A`, 5.67:1), and light `primary` `#2E7D4F`
was 3.91:1 *on a card* - a failure that only existed once cards became visible
(now `#276B44`, 4.97:1). Dark `primary` dropped from OKLCH L 82 to 76: on a
near-black screen the depletion bar made the old value the brightest object in
the room.

`core/ui` now has a unit test suite that measures the palette in OKLCH -
hue consistency, a monotonic surface ramp, a card distinguishable from its
background, a real lightness ladder in the text roles, and WCAG AA for body
text on both the background and a card. It was verified against the bug: with
the roles removed it fails with *"dark surfaceContainerLowest is 144 degrees off
the background hue (300 vs 155)"*.

Background: `docs/DESIGN-RESEARCH.md`, change 1 of 12.

### D14 — Three bugs the device found that the tests did not (2026-09-08)

All three shipped through a green build and a passing suite. Recorded because
each is a *category* of thing unit tests here cannot see.

**Doubled status-bar inset, on every screen.** `MainActivity`'s Scaffold has no
`topBar`, but a Scaffold hands its content the status-bar inset regardless, and
that padding went onto the `NavHost`. Every screen inside then has its own
Scaffold whose `TopAppBar` applies the same inset again. Measured on device:
**268px of dead space above every title, 82dp at this density**, down to 116px /
36dp - normal M3 title padding - once the outer Scaffold was given
`contentWindowInsets = WindowInsets(0, 0, 0, 0)`. It only places the bottom nav;
`NavigationBar` handles its own inset. Found by the user looking at the screen,
not by anything automated.

**The online lookup was unreachable.** `PlantDetailScreen` gated both routes to
the care screen on `hasSpeciesCare()` - correct when there was no fallback, and
exactly backwards once the care screen's empty state became the only way to
reach the name lookup. The feature was gated off for precisely the plants it
exists for. The menu item is now always enabled; the "not on file" hint stays.

**GBIF answers everything, including nonsense.** Asked for "Flax seeds" it
returns `matchType: HIGHERRANK`, `rank: KINGDOM`, `canonicalName: Plantae` at
99 confidence. The service only rejected `matchType == "NONE"`, so the screen
rendered *"Plantae"* with an encyclopaedia article about photosynthesis, framed
as the resolved identity of the plant. The generator already guarded this exact
trap (`Begonia President` resolving to `Bigonia`); the guard was never carried
to runtime. Now `isUsableMatch` in `:core:domain` requires a real taxon rank -
species through genus, never family or above - and rejects weak fuzzy matches,
with `SpeciesLookupTest` covering it.

The pattern in all three: a green suite and a screen nobody had looked at. The
palette test suite added in D13 exists for the same reason and would not have
caught any of these.

### D15 — Design changes 2-5, and a contrast rule worth keeping (2026-09-08)

**Shape (change 4).** Eight hardcoded radii - 4, 5, 6, 7, 8, 10, 12, 24 - and
zero references to `MaterialTheme.shapes`. Nothing distinguished a 5 from a 6 to
a reader; it only meant nobody chose. Now one `Shapes` in the theme mapped to
what the app contains: badges 4, thumbnails 8, cards 12, sheets 16, FAB 28. At
most three appear on a screen.

**Hairline (change 5).** The plant card was the only elevated surface, at
`1.dp` - a shadow, invisible on near-black, which was half of why it had no
visible edge. Now `elevation = 0.dp` with a 1dp `outlineVariant` border.
Measured on device: `#414A44` dark, `#C1CAC0` light. A shadow implies a floating
object; a hairline implies a page.

**The card (change 3).** Seven stacked text rows down to four - name, the
answer, the state object, and one dim context line. Five of the seven were
11-12sp in the same grey, which is a wall rather than a hierarchy. Cadence moved
off the card entirely; the detail screen already showed it, and that is where
someone goes to ask that question. Row 2 carries the prediction when there is
one and the most recent fact otherwise, so the card always answers something.
The "checked, not thirsty" credit survives as a coloured span inside the dim
row rather than a row of its own - restraint still gets visible credit.

**The contrast rule (change 2).** The research specified the dim tier as
`#8D948E` at "5.2:1, still AA". That figure is against the *background*. The
text sits on a *card*, where the same colour measures **3.92:1** - a fail, and
one that shipped and was caught by measuring the device screenshot afterwards.

Same class of error as light `primary` in D13, which was only exposed because
completing the surface roles created the card it failed against. The rule:
**quote contrast against the surface the text is actually drawn on, and in this
palette that is always the card, never the background.**

The AA floor on the card is also what sets the ladder spacing. It caps the dark
scheme's usable text range at roughly 19 OKLCH L-points, so ~9.5 between tiers
is what is available rather than a free choice: 90.0 / 81.0 / 71.4 dark,
22.5 / 39.6 / 50.1 light. `ThemeTest` now checks `outline` at AA as well, since
it carries text.

**A light-mode bug found on the way.** `targetSdk = 35` makes edge-to-edge
mandatory, so the app draws behind the status bar - and nothing ever set
`isAppearanceLightStatusBars`. In light mode the system icons stayed white on a
`#F7FBF3` background, measuring **1.0:1**. Invisible. Now set from
`isSystemInDarkTheme()`; measured at 10.11:1 after. It had been there since
targetSdk went to 35 and no test could see it, because it is a property of the
window rather than of the app's own drawing.

### D16 — Design changes 6-12 (2026-09-08)

**Icons (10).** Off `material-icons-extended`, which Google documents as "no
longer maintained or recommended" and warns "can also increase the build time of
your apps significantly". The app used sixteen of its several thousand icons.
Now sixteen Material Symbols Rounded vector drawables vendored by
`tools/fetch-icons.py`. **APK 21.2MB to 14.2MB.**

The point was not the size. The two log actions - the app's central gesture -
were `TouchApp` and `WaterDrop`: filled, `primary`, the same size, side by side,
with nothing saying which was the restraint action. They are now a hand held
back from the pot and a drop, which is a picture of each action rather than a
picture of the UI. Outlined is the default everywhere and **filled means exactly
one thing**: a watering just logged.

**Motion (7).** `X4` claimed reduce-motion support in a codebase with zero
animations, which was true only by vacuum. `Motion` now reads
`ANIMATOR_DURATION_SCALE` and collapses every spec to `snap()` when the user has
turned animations off - not a shortened duration, off. Everything is ease-out
`tween`, never a spring: cozy-game guidance wants overshoot and calm-technology
guidance forbids it, and a diary of slow biological processes should not bounce.

UI-SPEC section 7 requires "Still wet" to get the same confirmation as
"Watered". Both now get one identical 220ms scale, which is the point - the app
must not celebrate watering and stay silent about restraint.

**The ring (8).** A full-width bar filling toward 100% is the grammar of task
completion, the one grammar this app exists to avoid: it turns "this pot is
drying normally" into "you are 62% of the way to doing your job". The depletion
is now a ring around the plant's photo, with the trigger as a tick rather than a
percentage. It reads as a level, costs no vertical space, and is what let the
card lose three rows.

**Typeface (6).** Newsreader and Hanken Grotesk, bundled as variable TTFs.
Chosen by measuring the font binaries: **Fraunces has no OpenType numeral
features at all**, so its digits cannot be made to align, and the same is true
of DM Sans and Instrument Serif. This app is full of grams, millilitres and day
counts. Both chosen families are tabular by default, so no feature plumbing and
no digit can jitter. Downloadable fonts were rejected outright - the API does
not support variable fonts and requires Play Services.

**Event colours (9).** Planta's best idea: the colour names the activity, not
the urgency. All within a few lightness points of each other so none can read as
an alarm, which keeps a timeline scannable in an app where nothing is a failure.

**Grain and the empty state (11).** The first grain attempt did not render at
all - a tile already at 6% alpha, multiplied by another 3%, quantises to zero on
a near-black surface. Replaced with Gaussian noise centred on mid-grey composited
with `BlendMode.Overlay`, which leaves a mid-grey pixel neutral and is therefore
zero-mean by construction. Measured on device: background mean 15.00 to 15.01,
standard deviation 0 to 1.69, one distinct colour to forty-four.

The empty state is a branching monoline form, not a mascot. Mid-complexity
fractals at **D between 1.3 and 1.5** measured roughly 60% better stress recovery
by skin conductance, so that is the spec; box-counting the actual render gives
**D = 1.441**. No mascot on purpose: "Dark Patterns of Cuteness" (Springer)
argues cuteness's association with vulnerability stimulates trust responses and
can be operationalised to inspire uncritical acceptance, which makes a cartoon
face a persuasion channel rather than decoration.

**Photography (12), partly.** Dashboard thumbnail 56 to 64dp with a hairline
inset border, which resolves the hard cut-out edge a bright photo has on a dark
card. The plant-detail full-bleed hero is still outstanding.

### D17 — The hero, and captions for things already on screen (2026-09-08)

**Change 12, finished.** Plant detail opens on a 300dp full-bleed photo running
under the status bar, name and chips on a gradient scrim that resolves into the
page. The dashboard thumbnail went 56 to 88dp.

Three things came out of building it.

`Modifier.padding(-16.dp)` to cancel a `LazyColumn`'s `contentPadding` throws
**`IllegalArgumentException: Padding must be non-negative` at runtime**, not at
compile time - so it built, installed, and crashed the app on opening any plant.
`Modifier.fullBleed()` in `:core:ui` measures past the gutter and places back,
which is the supported way.

**Medium is no longer printed under a photograph of it.** "soil" appeared on
every dashboard card, captioning something already on screen, and it was one of
the four rows the card was trying to fit. It now appears only where it is *not*
soil - semi-hydro or water is a fact about the pot you cannot see, and it changes
how the weight model reads. Same rule for species: suppressed when it equals the
plant's name, because people name a plant after what it is and the hero read
"Fittonia" over "Fittonia".

**A ring's margin was being reserved with no ring to draw.** Every card paid
12dp of height for an indicator that only appears once a pot has weight
readings, which pushed the last card under the FAB - 4dp of clearance to a
48dp tap target. Now reserved only when there is a ring: 43dp.

### D18 — Ambient context explains, it never predicts (2026-09-08)

Requirement 23 asks for room temperature and humidity "so seasonal drying-rate
changes are explainable", and **explainable is the entire scope**. Nothing in
`Ambient.kt` feeds the prediction, the depletion trigger or the slope fit. A pot
that dries faster dries faster whether or not the app knows why, and building a
temperature correction on two thermometer readings would be a model resting on
nothing.

What it does instead is separate "this plant changed" from "the room changed",
which is the difference between a diagnostic worth reading and one worth
ignoring. `AmbientVerdict.ROOM_UNCHANGED` is the valuable one: it is the only
verdict that makes a diagnostic *more* worth acting on, because it rules out the
boring explanation and leaves the interesting ones - a shrunken root ball, a
rootbound pot, rot.

`explainDryingChange` returns null - says nothing at all - when either period
has fewer than two readings, when the drying rate has not actually moved, when
the two periods measured different things (temperature before, humidity after),
or when the room moved both ways at once. Same discipline as `Prediction`: an
app that confidently names a wrong cause sends someone to repot a healthy plant.

**Keyed by location, not by plant.** Four pots on one windowsill share a
windowsill; logging the same measurement against each would be four times the
work for one fact, and would lose it when a plant is deleted - exactly when the
history of that spot becomes interesting. The cost is that renaming a location
orphans its readings, which is the right trade against a locations table nobody
asked for. F13 is where that would belong if it lands.

### Two things found on the way

**Weight readings were never in the backup.** `ExportBundle` carried plants,
events, photos and reminders and nothing else. Nothing had caught it because no
pot has been weighed yet - but an export/import round trip would have silently
destroyed the entire drying history, the one thing in this app that cannot be
reconstructed from memory. Both `weightReadings` and `ambient` are in the bundle
now, defaulting to empty so older backups still import.

**There were no migration tests.** `ThirstTrapDatabase` says every migration
should be covered by a `MigrationTestHelper` test; the `androidTest` directory
existed and was empty. Room validates an auto-migration against the exported
schemas at compile time, which catches a malformed migration and says nothing
about whether the rows on someone's phone survive it. `MigrationTest` now covers
6 to 7 with a real plant and a real weight reading, plus a 1-to-7 walk. Both pass
on the device.

Running them needs a detour: Gradle's `connectedAndroidTest` installer trips
MIUI's `INSTALL_FAILED_USER_RESTRICTED`, because the test APK is a *new* package
rather than an update. `adb install -r -t` of
`core/data/build/outputs/apk/androidTest/debug/data-debug-androidTest.apk`
followed by `am instrument -w -e class dev.dheirav.thirsttrap.data.MigrationTest
dev.dheirav.thirsttrap.data.test/androidx.test.runner.AndroidJUnitRunner` works.

**The manifest recorded the wrong schema version.** `DATABASE_VERSION` was a
private constant in `ExportRepositoryImpl` that stopped at 4 while the database
went to 7, so every backup since has carried a stale number - and the importer's
"written by a newer version of the app" warning has been comparing against it.
It now lives beside the `@Database` annotation and feeds both.

### Verified on the device

The v6-to-v7 migration ran against the real database with a backup taken first:
4 plants, 13 events, 6 photos, 4 reminders, identical before and after, no
crash. The ambient screen records, persists, exports and deletes; the test row
was removed afterwards, so nothing is left behind.

One thing to know for future device work: `adb shell input tap` fires
ACTION_DOWN and ACTION_UP about 8ms apart, and some Compose buttons do not
register it. `input swipe x y x y 120` presses for 120ms and does. A control
that looks broken under `input tap` is worth re-testing that way before being
called a bug.

The grain overlay added in D16 also means screenshots no longer contain exact
theme hex values - every pixel is perturbed by a point or two. Pixel assertions
against the palette need a tolerance now.

### Not built

The free-weather-API half of requirement 23's "or". Manual entry satisfies the
requirement, and `AmbientSource.WEATHER` and the "not the same as the room"
labelling are already in place for it. Open-Meteo is the obvious fit - free,
keyless, and geocodable by city name, so it needs no location permission. Worth
doing, because nobody logs a thermometer by hand for six months.

### D19 — F15 counts, it does not score (2026-09-08)

Requirement 15 asks for waterings per month, a survival rate and an average
days-to-root. Two of those are plain measurements. **"Survival rate" is a
failure count wearing a percentage**, and the requirements list gamification and
anything that makes a missed day feel like failure as an explicit anti-goal.

It is built, because it was asked for and because it is the thing somebody
genuinely wants to know after a year. Three things keep it from becoming a
score. It is computed **only over plants that have actually left** - a living
plant is not a pending failure, so it is not in the divisor. It is **null until
something has left**, because a collection where nothing has died does not have
a 100% survival rate, it has no data, and showing 100% invites watching it fall.
And it is written as a **sentence about the ones that went**, not a percentage
on its own line.

**Days-to-root needed a schema change first.** A stage move was recorded only as
the free-text note "Moved to rooting". Deriving a statistic by parsing that
would break silently the first time somebody reworded it, and a wrong average is
worse than none - so `care_events` gained a nullable `propagation_stage` column
(v8, auto-migration). Events written before it are counted as `untracked` and
stated on screen rather than averaged over as zero.

The median, not the mean: one cutting left in a jar for five months would drag
an average somewhere useless, and at these sample sizes that is likely rather
than rare.

**The month table starts at the first watering**, not a fixed twelve months
back. A new diary was showing eleven rows of dashes for a year it did not exist
in, which buried the single row that had anything in it. Gaps *inside* the
record are still kept, because a quiet spell is the interesting part.

### D20 — A stray tap deleted one of the user's care events (2026-09-08)

During the screen sweep, one of the synthetic taps used to drive the app landed
on a timeline row and deleted an OBSERVATION on the Creeping fig from 8 Sep
01:54. It was noticed only because the stats screen reported 12 entries while a
backup taken earlier in the session recorded 13.

Restored from that backup via the debug import, after diffing to confirm the
restore was a no-op for everything else: plants, photos and reminders were
byte-identical, and the twelve surviving events differed only by the new
`propagationStage` field being absent rather than null.

The lesson is not "be careful". It is that **driving somebody's real diary with
synthetic taps will eventually write to it**, and the only reason this was
recoverable was that a backup had been taken before the migration earlier that
day. Take one before any tap-driven session, and check the counts afterwards.

### D21 — Weighing must not require calibrating first (2026-09-09)

Reported from the phone while trying to actually start M2: "i am only able to
set the watered weights and not otherwise". That was exactly true, and it made
the app's central feature unusable.

`assembleWeightState` returned early on `plant.anchors == null`, so with no
anchor there were no segments, no chart and nothing to show; the weight screen
therefore offered only "Set the watered weight". A wet anchor is *the pot just
after watering*, so the only way to begin was to be standing at the plant having
just watered it, holding a phone. Any other moment, the app refused the reading.

Three changes, in order of how much they matter:

**A post-water reading is the calibration.** `POST_WATER` already re-anchors the
wet end - that is what the context means. So when there is no anchor, one is
derived from the first non-excluded post-water reading rather than demanded as
a separate ceremony. Weigh it after watering, tap "just watered", done.

**Derived on read, not stored.** Same argument the file already makes for the
EWMA: a stored anchor drifts from the readings it came from. Excluding a bad
post-water weigh now un-calibrates the plant, which is the correct behaviour and
would be impossible with a written-back value. Only readings after the last
repot count, so a repot still invalidates the old anchor and
`needsRecalibration` clears itself once a new post-water weigh exists rather
than nagging for something already done.

**Readings without an anchor are still readings.** They are recorded, segmented
and charted as raw grams. The prediction and the depletion stay suppressed -
`SuppressionReason.NOT_CALIBRATED` already said so - but the curve is real and
drawing it is how somebody starts. The chart drops the anchor bands and the
trigger line rather than inventing reference lines it does not have.

The keypad sheet also grew to 88% of the screen with keys that fill the space,
and opens fully expanded. It is used standing at a windowsill holding a pot; a
keypad you have to drag open first is worse than no sheet, and the save button
had been reachable only by scrolling past twelve keys.

### D31a — F14: the inventory and the calculator are one question (2026-09-09)

Requirements item 14 asks for a fertiliser inventory and a dilution calculator,
which sound like two screens. They are one question asked at one moment: you are
standing at the sink holding a bottle and a can, and you want to know how much
to pour. A calculator you have to feed a bottle into is a worse version of a
table that already knows.

So "Feeding" is the cupboard, with a can size chosen at the top and a Pour
column. Change the can and the whole cupboard re-reads.

**Two refusals, which are the part worth having.** `parseDilution` accepts 1:200
and 5 ml/L and nothing else. "A capful", "1 tsp per gallon" and "as directed" are
all real things printed on real bottles, and each would be a different number if
guessed at, so they return null and the row says the dilution is not in a form
it can work with. Separately, 1:2000 into a 250 ml can is 0.125 ml: arithmetically
correct and useless, because nothing in a kitchen measures a tenth of a
millilitre. That returns `TooSmall` carrying the volume that *would* be
measurable, because "use a bigger can" without a number is not advice.

The typed text is stored, not the parse. The text is the fact and the parse is a
reading of it, so a bottle the parser cannot handle today still reads back
unchanged and a better parser later reinterprets old rows for free.

Wired into logging rather than left as an island: picking a fertiliser when
logging a feed fills the name and the dilution, with the free-text fields still
underneath, because a one-off feed is a real thing and not everything poured has
to be inventoried first.

Schema v11 adds the `fertilizers` table, and the backup carries it. That last
part is deliberate: weight readings were once absent from the bundle and nobody
noticed until an export/import round trip would have destroyed them.

### D30a — The call-site audit, and what it found (2026-09-09)

Six instances of "correct, tested, wired to nothing" made it a pattern rather
than bad luck, so this was a deliberate pass rather than another accident.
Method: enumerate every top-level declaration in `:core:domain`, every enum
value, and every method on a domain interface, then count who actually
references each one across all main sources. Three heuristics, each aimed at a
shape the project had already produced.

**The module is in better health than the pattern suggested.** 72 top-level
declarations, 55 interface methods: one dead function and three dead methods.
Every `SuppressionReason`, every `AmbientVerdict` and every `Confidence` value
is both produced and consumed. Both surviving `resolveIntervalDays(null, null,
null)` calls are the deliberate create-then-replan pair from D23 and are
correctly followed by `rescheduleFromModel`.

**The one that mattered.** Settings offers 30/50/75, labelled "how dry a new
plant is allowed to get before it is worth watering", stores the choice and
shows it back highlighted. Nothing read it. Every plant was created on the data
class default of 0.5 whatever the user picked, and the only reason any plant
differs is the species catalogue writing its own trigger. A control that looks
like it works and does nothing is worse than an absent one, because it spends
the user's attention and returns a false belief. `PlantEditViewModel` now reads
it when creating; an existing plant keeps whatever it has.

**Dead, removed.** `averageDaysToRoot`, superseded by `rootingStat`, which
derives its own durations and takes a median rather than a mean, so the old
helper's only test was testing arithmetic nothing runs.
`WeightRepository.markNeedsRecalibration`, superseded by the repot path writing
the flag directly. `AmbientRepository.knownLocations` and its DAO query,
superseded by `AmbientViewModel` computing the list from plants and readings so
that places never measured before still appear.

**Two left open, both judgement rather than tidying.**

- `PlantRepository.updateEvent` is implemented and never called. It is not
  really dead code, it is a missing feature: you can delete a diary entry but
  not correct one, which after D25 is exactly the wrong way round. Deleting the
  method and building the editor are both defensible; doing neither is not.
- The `useExactAlarms` toggle writes a preference and asks the user to grant
  SCHEDULE_EXACT_ALARM, and nothing ever schedules an exact alarm, because D4
  decided inexact deliberately. The app is asking for a permission it does not
  use. Either honour the setting or remove the toggle.

**What the audit is worth repeating for.** Both real findings were settings and
methods that *look* wired from every angle except the one that counts. Grep for
a name and it appears; read the screen and it works. Only counting the direction
of the reference, produced versus consumed, separates them.

### D35 — The scanner comes home, and the export gets a clock (2026-09-27)

*Numbering note: two 2026-09-09 entries below carry `a` suffixes, because a
later session reused D30 and D31 before the earlier ones were pushed. Renaming
the newer ones would break the cross-references that point at them.*

**The GMS scanner is gone.** Scanning a pot sticker now runs entirely
in-process: CameraX preview plus a zxing analyzer over the Y plane, decoding
the same QR format zxing already generates for the stickers. The trade is one
runtime CAMERA permission prompt (the GMS scanner ran the camera inside Play
Services and needed none) for a scanner where no frame, and no fact about
scanning, ever leaves the process. Verified the way the audit found it:
`:app:dependencies` on the debug runtime classpath now matches zero lines for
datatransport, firebase, mlkit or play-services. The
`com.google.android.datatransport.events` file already in app data is inert
leftover from the old dependency. INTERNET permission stays, for the
off-by-default weather and species-lookup features only.

**Last export, said quietly.** D33 made the user the backup system, so the app
owes them the one fact that system runs on: Settings now shows "Last export:
N days ago. The diary lives only on this phone." under the backup button.
Stored in DataStore, written only by a successful export. Quiet text, never a
badge or a notification - the anti-goals apply to guilt about backups too.

### D50 — "Something looks wrong" belongs on the plant (2026-10-01)

Section 1 of `docs/NAVIGATION.md` says nobody looks in Settings for a feature,
so for a new user the seven behind the gear icon do not exist. Seven were moved
out. Then Help went in, and one of Help's four entries was a feature.

"A plant does not look right" was the diagnosis tree: a thing somebody reaches
for at the moment they are worried about a plant, sitting four taps deep behind
Settings, then behind a word meaning "I am confused", then in a list beside
three pieces of documentation. It is now **"Something looks wrong"**, second in
the plant's own menu under "Log something", because it is about that plant and
that is where you are when you want it.

The user spotted this within an hour of D49 shipping, which is the part worth
recording: D49 repeated the mistake it was itself documenting.

**The move uncovered a button nobody has ever seen.** Every leaf of the tree has
had "Log what you found" since it was written, and it has never rendered once,
because nothing passed `onLogEvent`. Nothing could: the only route in came from
Help, which does not know which plant you mean. The route is plant-scoped now,
so walking the tree to an answer and logging what you found against that plant
is a path that exists for the first time. Third dangling callback this week, and
the first one that was dangling for a structural reason rather than an
oversight.

Help gains a rule it can be held to:

> If an entry does something rather than explaining something, it does not
> belong in Help.

Still open, and the user's own observation on seeing it: the four entries left
are not one category either. Three of them (what this app is for, weighing a
pot, finding your way around) are how the app works, and one (reminders are not
arriving) is something being broken. Separation still needed.

### D49 — A map, and the two gestures the app cannot explain itself (2026-10-01)

The user asked about a tutorial, and then clarified: not what the app is for,
but how to get around it. Those are different questions and only the first had
an answer.

**`docs/NAVIGATION.md` was describing an app that no longer existed.** It opens
by saying it was written before any code, which is honest, so sections 1 to 6
stay as the plan they were and a section 7 records what got built.

Its own section 6, "What this does not fix", set two things aside as not really
navigation. Both are now done: the weight screen is called "When it needs water"
rather than "Weight and prediction", and a tap on a diary row opens the entry
instead of a delete menu. Those entries are left in rather than deleted, because
the useful fact about that document is that the two things it dismissed are the
two the user noticed first.

Route count went 28 to 28 and is not the same 28: `compare` and `timelapse` are
gone into the photo viewer, `intro` and `help` arrived. The viewer has no route
at all, which is worth recording, since it is an overlay in the activity's own
window rather than a destination.

The plan's real blind spot was treating navigation as a tab-shape problem. The
tabs were the easy half. The hard half was that a tap had accumulated four
different meanings in this app and two of them opened destructive menus, so
section 7 states the rule that now applies everywhere:

> A tap opens the thing you tapped. A long press is a shortcut, and only ever to
> something that is also reachable by tapping.

**The help page is `WayfindingScreen`**, first in the Help index and the only
entry there that is not about something being broken. Help exists because three
troubleshooters needed one door, and "where is the thing I want" is a fair
question to arrive with too.

It lists the three tabs, both overflow menus item by item, and then the part
that earns its place: the **two** gestures nothing on screen can convey. Holding
a plant row gives the quick sheet; holding the water drop asks for amount and
method. Everything else in the app is a plain tap and therefore learnable by
trying, which was the whole point of D48, so those two are the entire syllabus.
It closes by saying so explicitly, and that tapping a photo or an entry cannot
delete anything without asking, because until D48 it could.

**Not coach marks**, and that was a recommendation rather than a shortcut.
Overlay tooltips pointing at buttons on first run would fight every other
decision here: no chrome, no nagging, no tone of voice. They also get dismissed
unread. The intro page handles what the app is for, this handles where things
are, and neither ambushes anybody.

Every menu name on the page was checked against the source rather than written
from memory, thirteen of thirteen present. A map that is wrong is worse than no
map, and this one describes nine menu items that were renamed, moved or added in
the last two days.

**Still not built: the landing page with the scripted demo**, the other half of
the "Both" answer from D36. That one is for people who have not installed the
app, which is a different audience and the one that matters before sharing.

### D48 — The five UI items, and two things they uncovered (2026-09-30)

The list that stood under Next actions, cleared. Each came from the user using
the app rather than from reading it, and two of them turned up defects that were
not part of the ask.

**A diary entry opens.** A tap and a long press both opened a dropdown whose
only item was Delete, drawn over the row it belonged to. That is the shape D44
took off the photos and it is the mechanism that destroyed three real entries
during development: a mis-aimed tap landing on a destructive item in a menu
positioned above the thing it was about.

Now a tap opens the entry: its note, its day, its photos (tappable through to
the viewer), and Delete inside it, flipping the same dialog into a confirmation
the way the weigh-in editor already does. Two editors for two rows in two lists
should not be two different interactions.

Only the note and the day are editable. Not the type, because "this was a
watering, not a check" is a different entry rather than an edit of this one, and
not the clock time, because an entry backdated to a day carries no claim about
the minute. Moving the day keeps the time of day, so Tuesday corrected to Monday
stays at 18:40 rather than jumping to midnight.

Two things fell out of it, neither part of the ask.
`PlantRepository.updateEvent` had been written, implemented, exposed on the
interface and never called, which is why there was no way to fix a wrong note or
a timestamp at all: the entry editor is the first caller it has ever had. And
once something finally called it, it had a bug. It passed `createdAt = now`, so
every edit would have reset the row's age, and an entry backdated to last week
and corrected today would have looked like it was added today. It reads
`createdAtOf` now, which is D27's rule and the one the importer already follows.

**Tapping a plant opens the plant.** It opened the quick-log sheet, with the
plant's own page behind a long press, which is backwards from every list on the
phone and made the page the undiscoverable half.

The swap needed one thing first, which is why it was not just a swap: **the
plant's own page had no way to log anything.** Logging a watering with an amount,
a check, a repot or a feed was reachable only through that sheet, so swapping the
gestures would have moved the full log screen behind a hidden one. "Log
something" is now the first item in the plant page's menu, where the app's
central action arguably belonged anyway.

The sheet keeps the long press, and that is defensible where the diary row's was
not: nothing in the sheet is only in the sheet. The two commonest actions are
visible icons on the row and everything else is on the page the tap now opens.

**`AlmanacSheet`, and there were three sheets rather than four.** The Due screen
matched the search on `Snackbar`, not on a sheet, and Next actions said four on
the strength of that. The three real ones, the dashboard's quick log, the weight
keypad and the weighing round, now use the page's own surface with a hairline
along the top edge and no lift. They had already given up their drag handles on
the grounds that a printed page has none; they were still rounding a corner
through `shapes.large`, which was the last place in the app rounding one by
choice rather than by Material's default.

**Compare folded into the viewer.** `CompareScreen.kt` and `CompareViewModel.kt`
are deleted with their route, the second screen to go this way after the
timelapse. "Compare with this" pins the photo on screen and swiping moves the
other half against that fixed reference.

Both panes share one transform, so a pinch goes into the same leaf on both at
once. That is the move the whole feature exists for and the old screen made it a
lock you had to find and switch on. The two filmstrips are gone, which also
retires the picker whose missing separation started this session's UI work: the
tidiest version of that fix turned out to be not having the screen.

"Compare photos" on the plant page opens the viewer already split, oldest pinned
and latest in the moving half, which is the pair the old screen defaulted to.

**Required fields marked** in the fertiliser form, the new-experiment dialog and
the arm dialog, and the fertiliser form's "NPK (optional)" and "Note (optional)"
suffixes are gone. Same rule add-plant adopted in D44: mark what save is gated
on, say nothing about the rest.

Built and installed; the entry editor and the pinned comparison are for the user
to look at on the phone.

### D47 — The rest of the council list, all sixteen (2026-09-30)

The remaining findings, closed in one pass. Grouped by what they turned out to
be rather than by severity, because the grouping is the finding.

**Logic wired to nothing.** Six of the sixteen, which is the pattern this
project keeps producing.

- *The notification's "Snooze 1 day" never wrote anything.* `ACTION_SNOOZE`
  mapped to a null event type and `if (type == null) return` sat above the only
  coroutine block in the file, so the receiver dismissed the notification, said
  "Snoozed for a day", and left `snoozed_until` null. The row was still due on
  the next sweep and the notification came back the next morning. The reason it
  needed more than a one-line fix is that the receiver carries a plant id while
  `snooze()` takes a reminder id, so it does a lookup and snoozes every reminder
  the plant has, since the notification speaks for all of them.
- *The exact-alarm switch is gone.* D30a left it open with "honour the setting
  or remove the toggle" and it could not be honoured: nothing read
  `useExactAlarms`, the scheduler takes no settings dependency, and neither
  manifest declares `SCHEDULE_EXACT_ALARM`, so `canScheduleExactAlarms()` cannot
  return true on API 31+. It was a switch that could not latch on, would have
  gated nothing if it had, and sent the user to a system page this app cannot
  appear on. The field, the setter, the permission prompt and
  `openExactAlarmSettings` went with it rather than being left as dead API. The
  DataStore key stays in anybody's store and is ignored, which costs one unread
  boolean and saves a migration.
- *Two notification channels promised categories that could not deliver.* Task
  reminders cannot be created at all, because both `Reminder` constructions in
  the app pass `CHECK`, and the health diagnostic is only ever rendered in the
  weight screen. Both are deleted, and `createChannels` now deletes them from
  installs that already have them. A category in the system settings whose
  description promises notifications that cannot arrive is a small lie told in
  the one place somebody went looking for the truth.
- *Deleting a plant left its photos on disk.* `PhotoStore.deleteForPlant` was
  written for exactly this and had no caller anywhere. Called after the row, not
  before: if the delete fails the files are still wanted, and an orphaned file
  is recoverable while a missing one is not.
- *`LocationDao.all()` had zero callers*, which is the backup finding below.
- *The reminders troubleshooter gave a false all-clear.* `fireTestReminder`
  posted in-process, so it proved the permission and the channel and nothing
  about background work, while the screen said "if it does, background work is
  allowed". `ReminderScheduler.runSweepNow` already existed and the help screen
  did not even inject the scheduler. There are two buttons now, because they
  answer two different questions: one posts a notification, the other queues the
  real sweep through WorkManager, which is the thing an OEM blocks. The second
  says plainly that nothing arrives if no plant is due, and that this is not a
  failure.

**The backup carries the places now.** The high one. `location_notes` was the
one table `ExportBundle` never had, and `LocationDao` was not a constructor
argument of the export repository, so the rows could not reach the archive even
by accident. Added the field, injected the dao, put the count in the manifest
(which is what you read to decide whether a backup is whole), and wrote the rows
back on import. `CURRENT_EXPORT_FORMAT` is 2; a version 1 archive still imports
and simply has no places, which is what every defaulted field in that bundle is
for.

The import keys by the lowercased place name rather than an id, and unlike every
other table it takes `updated_at` from the file, because this row actually
carries one and D27's reasoning (the backup does not say the row changed) does
not apply.

**The idempotence test could not see four of the eleven tables.** Which is
exactly why the experiments import stamping `updated_at = now` went unnoticed
for a whole feature: `snapshot()` listed seven tables and experiments was not
one, and `seed()` never created an experiment, so even widening the snapshot
would have compared two empty lists. It now covers all eleven, ordered by the
key each table actually has, because `location_notes` is keyed by name and a
blanket `ORDER BY id` could never have included it. `seed()` creates a row in
each of the four it was missing. `ExperimentDao` got the `updatedAtOf` its
siblings all had, which is why the importer had been stamping `now`.

**Import no longer truncates a live photo.** It wrote straight to the live path,
and `FileOutputStream` truncates on open, so a damaged entry or a process death
mid-copy left the photo row pointing at an empty JPEG, which `orphanFiles` would
never flag because the row still exists. It stages to `.part` and renames.
Harmless for a clean archive, unrecoverable for a bad one, and the export side
by contrast never touches an original.

**A watering logged after the last weigh-in is no longer invisible.**
Segmentation only opens a new segment for a watering between two readings, so
one logged after the most recent reading left the model predicting from the
pre-watering weight and the card reading "Needs water now" about a pot watered
an hour ago. Rather than teach segmentation about it, the prediction refuses:
`predictWatering` takes the last watering time and returns a new
`WATERED_SINCE_LAST_READING` suppression when it is newer than the latest
reading. The weight the model is holding describes the pot before the can, so
the honest answer is "weigh it to start the new cycle", not a number.

**Three surviving D38 sites**, all dividing elapsed time by 86,400,000 where
they should difference two local day indices: the Due list's "checked today
already", the notification body, and the dashboard's dim context line, which the
original claim missed and the skeptic found. While in there, the notification
also stopped saying "It's been 1 days".

**A capped ETA now keeps its confidence.** `prediction.capped` was the first arm
of the whole `when`, so it swallowed the confidence entirely and a capped ETA
from a low-confidence prior read exactly like one from a high-confidence fit,
while the weight screen went on calling the same object "estimated from past
cycles". Capped decides the phrase, confidence decides the qualifier, and both
always apply. Worth noting the obvious fix was wrong: simply reordering the arms
produced "Water more than 2 weeks".

**The override instrumentation recorded a placeholder.** Both call sites read
`"${'$'}{s.name}->${'$'}{saved.name}"`, where `${'$'}` yields one dollar sign
and the rest is ordinary text, so every row in `usage_events` stored the literal
string instead of the two enum names. They agreed with each other, which is why
nothing looked wrong. D34's instrumentation knew a chip had been overridden and
not to what.

**Four tests that did not hold the code they were named for.** All four were
found by the council mutating a line and watching all 256 tests pass, and all
four are now checked the same way. Planting all five mutants at once fails
exactly six tests, each the one written for it:

- the ETA's wall-clock decay term: every test asserting an `Eta` put `now` on
  the latest reading, so the term was multiplied by zero every time. Two tests
  now, one for the countdown and one for it reaching zero.
- the slope fit's recency rule: the fixture was a straight 20-reading line, so
  any five gave the same answer and `take` passed as happily as `takeLast`. The
  run bends now, 40 g/day then 5 g/day, so the two disagree.
- `EWMA_ALPHA`: one test sat on the fixed point, where `a*x + (1-a)*x = x` for
  every alpha, and the other asserted a bound satisfied by any alpha above
  0.206. One step from a known prior pins it: `0.3 x -30 + 0.7 x -10 = -16.0`
  and no other alpha gives that.
- the survival rate: two given away against two dead made 0.5 the answer for
  either numerator. Three against one can only be 0.75 if the rate counts the
  ones that lived.

**Where it stands.** 267 JVM tests in `:core:domain` and 10 in `:core:ui`, all
passing. 16 instrumented tests in `:core:data`, run on the phone, all passing,
which is the only thing that can prove the backup fix: the new test deletes the
place row, asserts it is gone, imports, and reads it back with its note and its
lux value.

One honest gap. I meant to mutation-check the backup fix as well, by removing
the import line and watching the new test fail, and MIUI refused to install the
test APK for that run (`INSTALL_FAILED_USER_RESTRICTED`). The mutant was removed
from the tree immediately and the suite re-run green. The test still cannot pass
without the fix, because it calls `.single()` on the restored rows and that
throws on an empty list, but that is an argument rather than an observation and
should be recorded as one.

### D46 — The photo button did crash, and the backup gap is real but harmless so far (2026-09-30)

The two remaining high findings from the council, both tested rather than
argued, because the council itself flagged one as reasoned-only and the other
was cheap to check against a real file.

**The camera crash is real.** Reproduced by revoking the runtime permission,
which puts the app in exactly the state a fresh install is in and touches no
data at all. One tap on the photo button:

```
FATAL EXCEPTION: main
java.lang.SecurityException: Permission Denial: starting Intent
  { act=android.media.action.IMAGE_CAPTURE ... } with revoked permission
  android.permission.CAMERA
    at ...PhotoCaptureKt.rememberPhotoCapture$lambda(PhotoCapture.kt:66)
```

`PROCESS GONE`, phone back on the launcher, stack trace ending on the exact line
the council named. The reason nobody had seen it is that the QR scanner had
already been opened on this phone, so CAMERA was granted; anyone installing
fresh and attaching a photo first hits it immediately.

Android's rule is asymmetric, which is what made this so easy to get wrong:
`ACTION_IMAGE_CAPTURE` is refused for a caller that *declares* CAMERA without
holding it, and allowed for one that never declares it at all. So D35 adding the
declaration for the scanner broke the photo path without touching a line of it,
and the comment in `PhotoCapture.kt` saying "needs no camera permission" stayed
true-looking and became false.

The fix asks on the photo path rather than relying on the scanner to have asked
first. A refusal is not a crash and not a nagging second dialog: it logs and
stops, because the gallery import needs no permission and is the better answer
for somebody who has just said no to a camera. `awaitingPermission` is
`rememberSaveable` for the same reason `pendingUri` is, since MIUI will destroy
the Activity behind the permission dialog too.

Verified the whole path on the device: revoked gives the system prompt instead
of a crash, granting opens `com.android.camera.OneShotImageCapture`, and Back
returns to the app with `I/TTPhoto: capture cancelled`. The permission was left
granted, which is where it started.

One nice accident: `docs/REVIEW-BRIEF.md` already told the reviewer the app
"will ask for the camera only when you first attach a photo". That was a
description of the intent and was false about the code. It is now true, so the
brief needed no edit.

**The friend's APK is the one that crashes.** It predates this fix, so he should
be sent a new build before he spends time on it.

**The backup gap is confirmed, and has cost nothing yet.** Proved from the real
export sitting in Downloads rather than by restoring anything, which is the
cheap way: if the table is not in the archive, opening the zip is the whole
proof. Its `thirsttrap.json` carries ten keys, `plants, events, photos,
reminders, weightReadings, ambient, fertilizers, usageEvents, experiments,
experimentSubjects`, and no locations, and the manifest's `counts` map lists
nine tables with locations absent, so the archive cannot even report the gap.

The correction to the finding is the impact, not the mechanism: `location_notes`
currently holds **zero rows**. No place note has ever been saved and no light
reading has ever been filed against a place, so an export-wipe-import cycle
today would lose nothing. `ambient_readings` has one row and that one is in the
bundle. So this is a bug waiting for the first use of the Places screen rather
than damage already done, which makes it the right thing to fix and not the
urgent thing, and means it can be fixed and verified with nothing at risk.

Still to do for it: a field on `ExportBundle`, `LocationDao` injected into
`ExportRepositoryImpl`, the import path writing rows, the archive format version
bumped, and a row in the idempotence test's snapshot, which orders by id and
would need a different key for a name-keyed table.

### D45 — Prompts and menus set like the pages they interrupt (2026-09-30)

The user's observation, and it was right: the screens look like an almanac and
the dialogs looked like Material. A prompt is where somebody actually stops and
reads, so it is the worst place for the app to change voice.

What made them different was not the shape, which was already square from
`AppShapes`. It was everything else Material does and this app deliberately does
not: a raised tonal container, a shadow, a sentence-case headline, and no rules
at all. Next to a page built from paper, hairlines and letterspaced caps that
reads as a panel from another application.

`AlmanacDialog` in `core/ui/Almanac.kt` is the answer, and all **twelve**
`AlertDialog` call sites use it:

- the page's own `surface` colour rather than a lifted one
- a hairline border instead of a shadow, the same decision the FAB already made
  ("a ruled block, not a floating one")
- the title in the running-head voice over a `DoubleRule`, so a prompt is set
  like a title block
- a `Rule` above the actions, and `dismiss` drawn left of `confirm` so muscle
  memory still works

It is built on `BasicAlertDialog`, because `AlertDialog` exposes `tonalElevation`
and no way at all to turn its shadow off. `DialogText` came with it, because
every dialog body wants the same style and passing it at twelve call sites is
how twelve call sites drift apart.

`AlmanacMenu` does the same for the four `DropdownMenu`s. M3 1.3's overload
takes `shape`, `containerColor`, `border` and both elevations, so this one is a
wrapper rather than a rebuild.

Two things fell out of the conversion that were not styling. The fertiliser
dialog's "Remove" and the weight dialog's "Delete" were rendering in the default
colour while every other destructive confirm in the app uses `error`; they
match now. And the care prompt was rewritten rather than restyled: the title is
the species on its own over the rule instead of a headline sentence, the body
lost "Worth a minute now" (the app telling somebody what to feel, which the
anti-goals are against), and "Don't ask again" stopped being a third button
competing with the two that matter. It is a quiet line inside the body.

**Applying care notes now closes the page.** `applySuggestions` takes an
`onDone` and the screen passes `onBack`. Applying is the last thing anybody
comes to that screen to do, so staying put and turning the button into
"Applied" left the user pressing Back for no reason. No acknowledgement is lost:
the page they land on shows the plant with its new trigger, which is a better
confirmation than a greyed-out button. The `_applied` flow went with it rather
than being left behind as state nothing could observe.

Verified on the device: the dashboard menu and the caption dialog. The dialog's
bottom padding is 10 against 20 at the top, because the action row carries its
own button padding and a symmetric 20 looked bottom-heavy.

**The light meter got the same treatment**, after a pass over every screen with
a terminal action. Saving is the only reason to open the meter, so it now
returns and the button reads "Save to this place" rather than becoming "Saved".
Its `saved` flag went the way of `_applied`.

Three screens already did this and were left alone: logging an event, the post
mortem, and the QR scanner, which navigates on the first frame that decodes.

Three should not, and the reasons are worth keeping so nobody "fixes" them:

- **Ambient conditions** is a log page. The list of readings under the form is
  the point, and the new row appearing in it is the acknowledgement.
- **The weighing round** is a worksheet you work down. Saving already advances to
  the next pot, and at the end "All weighed. Tap any row to change one." plus
  the column of numbers you just wrote is the useful state, not a dead end.
- **Backup** and **Diagnose** produce a result to read. Leaving would throw away
  the output.

### D44 — A photo is a page, and five screens got tidier (2026-09-30)

A session of small things the user found by using the app, which is the pattern
this project keeps proving: the defects that matter are found by opening a
screen, not by reasoning about one.

**Tapping a photo now shows the photo.** It did not. In the plant's photo strip
a tap and a long press both opened the caption-and-delete dialog, so the one
thing a tap could not do was look at the picture, and a mis-aimed tap landed on
Delete. That is how three real diary entries were lost during development, D20,
D25 and again this session. Diary-row photos were inert and the cover photo,
the largest image on the screen, was not clickable at all.

`ui/PhotoViewer.kt` is the page: full screen, pinch and double-tap to zoom, and
it pages across the whole set because the reason to open a photo is usually to
put it next to an older one. Caption, cover and delete moved onto it, behind an
overflow menu, and Delete asks. There is no long press on a photo anywhere any
more, which was the user's call and the right one: one gesture, and the
destructive action is a visible control on a page you meant to open.

Two things about it were wrong until the device said so, and neither would have
shown up in a compile.

As a `Dialog` the caption band was invisible. The window manager fits a dialog
window inside the system bars whatever `DialogProperties` says: the frame came
back as `[0,152][1280,2619]` and a bottom-aligned child of a `fillMaxSize` box
inside it landed where nothing could draw it. `decorFitsSystemWindows = false`
did not change it. It is now an overlay in the activity's own window, which is
already edge to edge, with a `BackHandler` instead of dialog dismissal.

And the swipe did nothing. `detectTransformGestures` consumes a one-finger drag
as a pan whether or not there is anything to pan, so the pager never saw a
horizontal drag. The gesture handling is hand-rolled now and consumes a drag
only when it has work to do: two fingers, or one finger while zoomed in.

**The timelapse screen is gone.** Two full-screen photo viewers is one too many.
`TimelapseScreen.kt` and `TimelapseViewModel.kt` are deleted, the route with
them, and the menu entry opens the viewer at the oldest photo. What came across:
chronological order from the domain's `buildTimelapse`, so a restored backup
cannot play a plant's life back in file order, the "day 22" label, and Play.

What did not come across is the scrubber, deliberately. A slider is a second way
to do what the swipe already does and over a photo it is the heaviest thing on
screen, a thick white bar across the picture the page exists to show. The user
asked for it out, then pointed out the hole that leaves: a swipe moves one
frame, so the first photo is twenty-one swipes away. So the band has "Oldest"
and "Latest" jumps, which are the two positions anybody actually names, dimmed
rather than hidden at the end they point at so the row does not move.

The bottom band is a gradient scrim rather than a flat 0.55 black. Flat was
legible over soil and marginal over a bright wall; a scrim that deepens
downward reads at any exposure. Same treatment as the hero on the plant page.

**Add a plant asks for one thing.** `canSave` is `name.isNotBlank()` and nothing
else in the form is validated at all, but seven labels read "(optional)", which
said so seven times without ever saying which one was not. The name is `Name *`,
the suffixes are gone, and one line at the top says it once.

**Care notes are offered when a plant is added.** On by default, and only when
the catalogue has something for that species. The moment you have just typed a
species name is the moment the notes are worth reading, and nobody goes looking
in a menu for something they do not know is there. "Don't ask again" in the
prompt turns it off, and Settings can turn it back on: `offerCareOnAdd` in
`AppSettings`, defaulting true, so the absent key has to mean on.

Note on the two care-notes entry points, which looked like a duplicate: the
in-page button appears only when there are notes, while the overflow item was
never disabled, because the care screen's empty state is the only route to the
online species lookup and disabling it made the lookup unreachable for exactly
the plants it exists for.

Both stayed at the time, which was the wrong call. The user asked why the menu
needed a "Care notes" item at all, and the honest answer is that it did not: it
was not a duplicate, it was mislabelled. The two are exclusive now and each says
what it does. Notes on file: the button on the page and nothing in the menu.
Nothing on file: a menu item reading "Look up this species", named for the one
thing that screen can actually offer. Exactly one route either way.

**The compare screen's two filmstrips were one strip with a gap in it.** Two
adjacent `LazyRow`s of identical thumbnails, no boundary, and nothing saying
which half drove which pane, with both dates in a shared row underneath where
they sat next to each other and the labels sat nowhere. It is a two-column table
now: a rule down the middle, `LEFT` and `RIGHT` over each column in the
column-head voice, and each date in its own column beside its heading. The
elapsed time is on its own band between the panes and the picker.

The separator is drawn on the right column's leading edge rather than placed as
a sibling divider, because a filmstrip is a `LazyRow` and has no intrinsic
height: `IntrinsicSize.Min` on the row would throw at runtime and a
`fillMaxHeight` divider would resolve against the screen and swallow the panes
above. Both compile.

**The propagation board grows down, not sideways.** A board of columns is a
desktop shape. On a phone it showed one 260dp column at a time, so the pipeline
it exists to display was the one thing you could not see. It is one `LazyColumn`
with the stages flattened into it, because a lazy list inside a lazy list of the
same orientation has no height to measure against. Read top to bottom the stages
are in order, which is the information the columns were carrying, in the
direction a phone is held. The cutting card became a row at the same time: at
full width the stacked layout was a tall box with a name in one corner and an
arrow in the other.

`VerticalRule` joined `Rule` in `Almanac.kt`, and `ic_close` was added through
`tools/fetch-icons.py` rather than by hand.

**Verified on the device**, which is the only reason two of these are right: the
viewer opening, its caption band, the swipe, the "Oldest" jump landing on "the
first photo, 8 Sept 2026", the compare screen's columns and that "8 Sept, 01:53"
fits on one line at half width, the propagation board, and the add-plant form.

**Not verified:** the care-notes prompt after adding a plant. Seeing it needs a
plant actually created in the live diary, and after three accidental deletions
this session that is not a thing to do on the user's own data for a screenshot.
It compiles and the pieces are wired; it wants one throwaway plant to confirm.

### D43 — Both anchor bugs the council found (2026-09-30)

A twelve-agent review across six dimensions, every claim handed to a separate
skeptic to refute: 21 confirmed, 5 refuted. The full list is in
`docs/COUNCIL-REVIEW.md`. These are the two high findings in the watering model,
and they are the same shape: a guard that exists at one end of the range and not
the other.

**The dry anchor survived a repot.** `WeightAssembly.kt` filters readings to
those after the last repot, which is what stops an old weigh re-anchoring the
new pot. That filter was applied to the derived wet anchor only. The replay loop
below it folded every non-excluded reading with no timestamp predicate, so a
PRE_WATER reading from the old pot kept setting the new pot's dry end, and
because a measured anchor is a running minimum, the old pot's lighter value won
forever.

The visible cost was the trigger weight, not the number on the screen. In the
scenario the skeptic ran, `Anchors(3000, 700)` instead of `Anchors(3000, 1800)`,
which puts the trigger at 1850 g rather than 2400 g: the app waits until the pot
is about 96 percent down its true range before saying anything. Late is the
unsafe direction, section 9 of WATERING-MODEL says so, and the code's own
comment three lines above says the filter exists for exactly this. The existing
repot regression test passed because all its pre-repot readings are POST_WATER,
so the dry end was never touched.

The fix is the same predicate in the replay loop. Removing it again fails two of
the three new tests, which is how I know they hold it.

**Nothing stopped the dry anchor being set at or above the wet one.** The
PRE_WATER branch guarded the low side (`0.30 x W`, a mis-weigh) and not the
high side, and the provisional branch replaces the guess outright even upward,
which is the route in: file a PRE_WATER at the pot's full weight and
`dryGrams == wetGrams`. Then `rangeGrams` is 0, depletion is NaN, and NaN
survives `coerceIn` because every comparison against it is false, so it reaches
the dashboard ring sweep and prints as 0 percent. The trigger equals the wet
weight, so WaterNow is permanent. That breaks the section 9 property "ETA is
never NaN or infinite for any input".

It is two taps away rather than exotic: `suggestReadingContext` proposes
PRE_WATER whenever the state is WaterNow.

Three changes, because a reading is not the only way in:

- `DRY_ANCHOR_CEILING_FRACTION = 0.95`, the floor's mirror. A pot weighed just
  before watering and still at container capacity is a reading filed under the
  wrong context, not evidence about how dry this person lets a pot get. Paired
  with an absolute `MIN_ANCHOR_RANGE_GRAMS = 1.0` on the same two-floor
  reasoning as the slope guards: the fraction asks whether the range means
  anything for this pot, the gram asks whether it can be divided by at all.
- The POST_WATER branch had the same hole from the other end. A post-water
  weight below the measured dry anchor cannot happen by watering, so the pot
  itself changed (pruned back, soil lost in a division, a different tare) and
  the measured dry end describes a pot that no longer exists. It goes back to a
  provisional estimate rather than being kept as half of an impossible pair.
- `Anchors.isUsable`, checked where anchors are read rather than only where they
  are written, because a pair also arrives straight from a database row. An
  unusable pair now counts as no calibration: readings are still drawn, the
  prediction says NOT_CALIBRATED, and the app asks for a post-water weigh. The
  floor inside `rangeGrams` stays as a backstop, since that is where every
  division happens.

`isCalibrated` moved with it, or the screen would have claimed a calibration the
prediction was refusing to use.

262 JVM tests in `:core:domain`, all passing. Seven are new, and I checked them
by mutation rather than by reading: reverting either fix fails three of them.

The Status count above was wrong before this, not just stale. It read 260 for
`:core:domain` plus `:core:ui` together, while the two actually held 256 and 10.
It now reads 272, which is 262 and 10 as measured.

### D42 — Plant identification closed entirely (2026-09-30)

F25c was closed by measurement, D41. F25b is closed by choice, which is the
user's call and the right one.

It would have been cheap: a settings field for an API key, one HTTP call, no
bundled model, no size cost. The reasons not to are about what the app is.

**It sends a photo of someone's home to a third party.** The app transmits
exactly one thing today, a plant name the user typed, off by default, and
Settings says so in those words. A photograph is a different category of data
and no amount of opt-in wording makes it the same promise.

**It is the one feature every competitor has**, and ours would be the worse
version: an API key the user has to go and get, which almost nobody will, in
front of a result the app would then have to hedge.

**The gap it leaves is smaller than it looks.** Identification answers "I have
no idea what this is". The app already resolves a name you *type* against GBIF,
which covers half-knowing, and the 164-species catalogue covers recognising it
once named. Someone who genuinely has no idea is better served by asking a
person or one of the many apps that specialise in it.

**The through-line, now that three things have been closed the same way.** F16
cloud backup, F25 identification, and gamification from the very beginning. This
app is defined as much by what it refuses as by what it does: it will not keep
score, it will not take your data off the phone, and it will not guess at a
plant. The one thing it does that nothing else does, it does by measurement. An
honest "I do not know" is the product, and every closure has been an instance of
that rather than a gap in it.

All 102 features are now either built or deliberately closed.

### D41 — Plant identification: evaluated, and closed (2026-09-30)

D29 chose the offline classifier and said the first task was an evaluation
rather than a feature, because a genus-level guesser has to earn its place in an
app whose character is refusing to guess. The evaluation is done and the answer
is no. Full write-up in `docs/PLANT-ID-EVALUATION.md`, tooling in
`tools/plantid/`.

**0 of 48 identifiable photos.** Google's AIY `plants_V1` against this diary's
own 55 photos, ground truth taken from the plant each is attached to. Not one
correct.

**The cheapest check was the decisive one, and it cost nothing.** Before running
any inference: does the label set contain houseplants? Its largest genera are
*Quercus*, *Pinus*, *Asclepias*, *Viola*, *Acer*. It is a North American field
guide. Of 59 common indoor genera, 40 are absent, including Philodendron,
Sansevieria, Dracaena, Epipremnum and Peperomia. Two of this diary's four plants
cannot be named by it at any accuracy whatsoever.

**Inference then answered the question that actually mattered.** Coverage rules
out two thirds of the shelf; the remaining question was whether the model knows
when it does not know, because a reliable abstention would still leave a usable
feature for the third it covers. It does abstain: 38 of 55 photos came back
"background", and exactly one photo was named at 70% confidence or above and
wrong. So it is honest and unusable, which are separate properties, and the
honesty is why a confidence threshold cannot rescue it. There is nothing correct
underneath to threshold.

**Domain, not just labels.** For `Ficus`, which it does have, it abstained on all
18 photos of a Ficus. A seedling in a terracotta pot on a desk beside a kitchen
scale is out of distribution for a model trained on plants growing outdoors. Our
own photo habit compounds it: the app encourages photographing the pot on the
scale, which is the least identifiable framing available.

**Why this generalises past one model.** Open pretrained plant classifiers come
from citizen science, and citizen science photographs wild plants. Houseplants
are a smaller, separate, far less open domain. So this is not "pick a better
model", it is "train one", which is a different project.

**I was wrong about the numbers first.** My first read was "confidently wrong on
the plant it had a class for", from a median top-1 confidence of 0.87 on Creeping
fig. That 0.87 was confidence in *background*, which is the opposite of
overconfidence. Recorded because the corrected finding is the better one.

**What remains:** F25b, online identification with the user's own API key, which
is little work and no size cost but sends a photo off the device and requires the
user to go and get a key. Or close identification entirely the way F16 was
closed. The app already resolves names you *type* against GBIF, which covers
half-knowing what you have, and "it does not guess at plants either" is a
coherent position.

### D40 — A widget, and the one thing it deliberately will not do (2026-09-30)

Never specified: not in the requirements, not among the 101 features, no
receiver in the manifest. It came up because Luna has one, so its absence read
as a gap.

It earns a place because the daily loop is a single question, "does anything
need me today", and answering it cost three actions: unlock, find the app, read
the list. A widget answers it at zero. Everything it needs already existed,
`observeDashboard` for the sorted attention list and a working
`thirsttrap://plant/{id}` deep link, so the widget reuses the path the reminder
notification has been using for weeks rather than inventing a second way in.

**Read-only, on purpose, and this is the interesting decision.** Logging from
outside the app already exists on the reminder notification, which offers
"Watered" and "Still wet" as equals, deliberately identical in weight. A widget
row cannot carry both, and carrying only "Watered" would teach that watering is
the correct answer and restraint is not. That is the exact conflation this app
was built to prevent, so the widget shows and does not act.

Two smaller choices in the same spirit. It lists only what is genuinely due or
past its measured trigger, rather than every plant sorted, because a widget
that shows everything every day is wallpaper. And when nothing is due it says
"Nothing is asking for you today" rather than a tick, a count or "all done",
because the app does not keep score and neither should the thing on the home
screen.

Painted in the app's own palette, not the system widget grey: `LightScheme` and
`DarkScheme` in `core:ui` became public and the widget wraps itself in
`ColorProviders(light, dark)`. One source of truth, because a second copy of the
hex values here would drift from the app within a month. Aged paper, ink, the
spot green for the reason line, and a hairline rule under a caps running head,
which is most of what makes the app look printed. Glance's `TextStyle` has no
letter spacing, so the caps carry it alone. Two cells by two rather than three
by two, which fits three plants without scrolling.

Glance does not observe flows, so something has to tell it the world moved. The
Application collects the dashboard, distinct-until-changed on the ids, due times
and predictions, and calls `updateAll`. The 30-minute `updatePeriodMillis` is
the fallback, not the mechanism: a widget half an hour stale after you water
something teaches people not to trust it.

**Also removed:** the Debug entry on the Due page, which appeared twice and sat
directly under a real help link so a developer tool and "Reminders not
arriving?" read as siblings. It was already gated on `BuildConfig.DEBUG` and so
never shipped, but Settings is the one place it belongs.

### D39 — The feature the app exists for was three taps deep and named after its plumbing (2026-09-30)

"Weight and prediction", in a plant's overflow menu. A screen title rather than
an invitation, describing the mechanism instead of the question. It is now
**"When it needs water"**, which is what someone actually wants to know.

The name was the smaller half. The real fix is placement: **"Weigh it" is in the
quick-log sheet**, one tap from the list, beside Photo and More. Weighing was
reachable only by opening a plant, then its menu, then a screen named after an
implementation detail, which is a strange place to put the one thing no other
plant app does. It is hidden where `isWeightTrackable` is false, so the closed
terrarium does not offer it.

**The Advanced toggle** gates the three genuinely specialist things: pot stickers
and the scanner, experiments, and logging room temperature by hand. Off by
default. None of them is useless and none is for everybody, which is the
distinction that matters: stickers pay off at thirty pots and a printer, and
experiments assume you want to run a controlled test on a houseplant. Someone
with four plants should not have to read past them.

Read in three places and written in one, checked by grep rather than assumed,
because a flag that gates nothing is the failure this codebase keeps producing.

### D38 — "Watered today" did not mean today (2026-09-30)

Found by following up a remark rather than by looking for it. The user mentioned
being in Oman (+04) for a break while normally in India (+05:30), which turns
the app's timezone handling from a design principle into a live case. Checking
what actually reads the stored per-entry offset turned up two bugs, one of them
nothing to do with travel.

**The plant card computed elapsed time and called it a calendar day.**

    when (val d = ((now - then) / 86_400_000L).toInt()) { 0 -> "$verb today"

That is 0 for anything inside 24 hours, so a plant watered at 23:00 read
"Watered today" at 08:00 the next morning. It is the most-read line in the app
and it was wrong for most of every day. It survived because it is right whenever
you happen to open the app at the same hour you watered, which is often enough
to feel correct.

Now `calendarDaysAgo(now, then, offsetMinutes)`, which differences two day
indices instead of dividing a duration. `CalendarDaysTest` pins the disagreement
directly: the old division returns 0 where the answer is 1.

**The timeline compared two day numbers from different frames.** Events were
grouped by `localDayIndex(timestamp, entry's own offset)`, which is right,
because the honest answer to "which day did this happen" is the day where you
were standing. But "Today" was then decided against `localDayIndex(now, the
phone's current offset)`. Two day indices in different reference frames are not
the same kind of number, and the difference between them is not a count of days.
Invisible while the phone stays in one place, off by one for entries near
midnight as soon as the diary moves. Each group is now labelled in its own
frame.

Both fixes are one line of arithmetic each, which is the point: this class of
bug is cheap to make, invisible in a screenshot, and only shows up if you ask
what a number actually means.

### D37 — A first-run page, and Settings stops being a junk drawer (2026-09-30)

docs/NAVIGATION.md has the flow analysis written before any code, which was the
right order: the conclusion was not the one I expected going in.

**The per-plant group was already correct** and nothing changed there. A thing
lives where you are when you want it, which is why logging is on the plant.
Everything else was wrong in one identical way: Feeding, Places, Room
conditions, Figures, Experiments, Diagnose and the reminders help were all
reached from Settings, where nobody looks for a feature.

Three tabs kept rather than four, which was the user's call against my
recommendation. The mitigation for the discoverability objection is that the
overflow is ordered and divided, jobs then a rule then the record, and the
first-run page points at it in one line.

**Two merges removed screens instead of moving them.** Places absorbed Room
conditions, because light and temperature are both facts about a location and
splitting them across two screens is why neither read as useful alone. The three
help doors became one Help screen.

**Caught by looking rather than by building.** Stripping the Settings links left
`Routes.AMBIENT` unreachable, this project's signature defect, about to be
committed. And then Places itself was empty while a reading of 23 C existed for
"Hostel Room", because `knownLocations` drew on plants and notes only. Since
Places had just become the only door to conditions, an empty Places meant the
readings were unreachable entirely. Measuring somewhere is as good a claim that
it is a place as putting a plant there, so it takes a third source now, and
Places carries the action in its bar rather than only inside a row.

**The first-run page is one page, three points, one button.** Not a carousel,
because those get skipped and the thing that has to land is a premise rather
than a feature tour: this app weighs pots. It also carries the tipping trick,
which nobody will invent themselves. That makes it reference rather than a
greeting, so Help can open it again with `markSeen = false`.

Its button first read "Add your first plant", which was wrong twice: it only
dismisses, and anyone seeing the page after an update already has plants.

`introSeen` is read where it is written, which is worth stating in this codebase
because D30a found a setting that was written, shown back, and consulted by
nothing.

### D36 — Big pots are not excluded, because the model never needed real grams (2026-09-30)

Weighing assumed a kitchen scale, which ruled out the plants people care most
about: the floor-standing Monstera, the ficus, anything too heavy to lift or too
wide for the platform. That looked like a hard limit on the differentiator.

It is not, and the reason is arithmetic. Depletion is (wet - now) / (wet - dry)
and the ETA is remaining over slope, so a constant factor cancels in both. Any
measurement *proportional* to the pot's weight carries the same information as
the weight. `PartialWeightInvarianceTest` runs the real assembly at 1.0, 0.6,
0.38, 0.25 and 0.1 of the pot and gets the same predicted day to within a
microsecond, so this is tested rather than argued.

Which makes the practical method: tip the pot onto one edge with the scale under
that edge and read whatever it says, roughly a third of the weight, lifting
nothing. Tip it the same way each time and the number is comparable. A bathroom
scale works too, and a luggage scale for anything hanging.

**What does not cancel is the instrument.** `Anchors.minMeaningfulSlope` was
`max(1 g, 0.5% of the range)`. The fraction scales with the pot; the 1 g is a
claim about a kitchen scale's own noise and does not. For a bathroom scale in
100 g steps it is about a hundred times too low, so the model would fit a drying
curve to rounding. It now takes the instrument's step: `max(max(step, 1 g),
0.5% of range)`. The test proves both directions, that 40 g a day is a real
slope on a kitchen scale and unsupportable on a 100 g one, and that the same
coarse scale works fine under a pot moving 400 g a day.

So `Plant` gains `weighingMethod` and `weighingStepGrams`, schema v14, both
defaulting to the whole pot on a 1 g scale, which is exactly what every existing
plant was implicitly using. All 260 JVM tests passed unchanged after the
threading, which is the evidence that the default is truly a no-op.

A change of method invalidates the anchors exactly as a repot does, because they
describe a measurement and not a plant, and the edit form says so before saving
rather than letting it be discovered later. Same reasoning as D24.

**Found on the way:** `ImportIdempotenceTest` had not compiled since the F11/F12
commit, which added `usageDao`, `experimentDao` and a `UsageRepository` to
`ExportRepositoryImpl` without updating the test's construction of it. The whole
instrumented suite has therefore been unrunnable since then. Fixed here.

### D34 — Five in one sitting: evaluation, instrumentation, F11, F12, honest confidence (2026-09-27)

**Prediction evaluation (the mission statement, measured).** `evaluatePredictions`
replays history to each past reading, takes the ETA the model would have given
standing there with only the data it had, and scores it against when the pot
really crossed the trigger (interpolated; cycles watered before the trigger are
censored, not errors). Stats screen gains "Does the model work?" - median
absolute error and lean. First real grade, on the live diary: the fig scored
0.26 days median error with no bias across 7 predictions; the peperomia 1.75
days, biased late - all from its provisional-anchor era, scored against the
anchor measured later. Honest, and self-explaining.

**Usage instrumentation (schema v12).** `usage_events`: actions only, never
content. FLOW_OPENED/COMPLETED/ABANDONED on the log-event sheet (abandonment is
the strongest friction signal and the one that leaves no other row), and
SUGGESTION_OVERRIDDEN on both weighing keypads when the saved chip differs from
the suggested one. Deliberately not on Edit plant - people open it to *read*,
and counting that as abandonment would be noise. In the export bundle, and the
friction report grew a section for it.

**F11 experiments (schema v13).** Experiment + arm-labelled subject
memberships; observations stay on the subject plants (the page is the index
card, not the folder). Concluding is one-way on purpose - a conclusion that can
be rewritten later is a lab notebook in pencil. In the export bundle.

**F12 [[plant]] links.** Resolved by name at render time, case-insensitive,
never rewritten into ids: the note is the user's text. Unresolved links read as
the text they are, wiki-style. Rendered as tap-throughs on the timeline.

**Confidence said out loud.** The dashboard flattened every ETA to "water in
about N days"; a Theil-Sen fit and a prior-only guess read identically, which
is the false confidence the tiers exist to avoid. Now: high = "Water in about
3 days", medium = "- still learning", low = "Maybe 3 days, from past cycles".

### The dependency audit found the one thing that phones home

`com.google.android.gms:play-services-code-scanner` (pot QR *scanning*) pulls
`transport-backend-cct` - Google's Clearcut telemetry uploader. ML Kit's
scanner reports usage to Google, which "nothing leaves this phone" cannot
honestly coexist with. QR *generation* is already pure zxing and offline.
**Backlog: replace the GMS scanner with a CameraX + zxing analyzer** (zxing is
already a dependency), then re-run the audit until the runtime classpath has no
transport backend. Until then Settings' privacy line deserves an asterisk about
the scanner.

### Backlog: home-screen widgets (noted 2026-09-27, not started)

Ideas only, parked deliberately:
- A "due today" glance widget - which pots want checking, nothing else.
- A one-tap deep link into the weighing round, for the scale-side ritual.
- A per-plant depletion ring for the one plant someone worries about.
- A next-prediction line ("Peperomia: water around Tuesday"), confidence
  tier included, same wording rules as the dashboard.
- The reminder-hygiene anti-goals apply doubly on a home screen: no counts of
  overdue anything, no red badges, and the all-quiet state should look like
  good news rather than an empty task list.

### D33 — F16 cloud backup: decided no (2026-09-27)

Every other feature was built on "nothing leaves this phone", Settings
promises it in those words, and the market research found that trust position
is the category's rarest asset - the Vera shutdown stranding its users is the
standing cautionary tale on the other side. An account would change what the
app is, to solve a problem the tested export/import round trip plus any
user's own syncthing or Drive folder already solves. F16 is closed, not
deferred: the README should say "local-only, by design", not "cloud sync
coming".

### D32 — A weigh-in is an assessment, and archive silences the reminder (2026-09-27)

Two defects, both found by `tools/friction-report.py` on its first run against
the live diary (the script generalises the D31 discovery method: compare
parallel streams, report the gaps).

First: the check clock counted only WATERED and CHECKED events, so reminders
sat 3+ days overdue on plants that were being weighed every single morning.
Nagging the most diligent user hardest is reminder fatigue by design.
`latestAssessmentMillis` in domain now folds weigh-ins into "last assessed",
used by both the reminder planner and the dashboard's attention sort.

Second: archiving a plant left its reminder enabled - the binned flax cup was
still scheduled for checks, 9.7 days overdue and climbing. `archivePlant` now
disables the plant's reminders and re-enables them on unarchive (the stale due
date self-corrects on the next replan). Existing archived plants need one
unarchive-archive toggle to pick up the fix; the flax is the only one.

### D31 — A post-water weigh-in logs the watering it implies (2026-09-27)

Eighteen days of real diary showed the failure mode: weighing is two taps and
happens daily, logging a watering is a separate chore, and it quietly stopped
on Sep 14 - four waterings arrived as bare POST_WATER readings with no WATERED
event. Segmentation self-heals through the jump rule, so predictions stayed
right, but "last watered", the average interval and the reminder clock all
count from events, and all three went stale without anything looking wrong.

So the weigh-in now carries the watering: `addReading` with POST_WATER and no
WATERED event inside the anchor window (24h, the same constant that decides
whether a reading counts as the wet anchor - one definition, not two) writes
the missing event. Decision function `impliesUnloggedWatering` in domain,
tested. Details that matter: the backfilled event is timestamped between the
previous reading and this one so segmentation keeps the boundary on the right
side of a pre-water weigh taken moments earlier; the amount is the plant's
default or the last poured amount, the watering sheet's own fallback; the note
says where the event came from; and the per-plant screen announces the
backfill in the existing hint slot, because a write the user only discovers
later on the timeline reads as the app inventing history. The weighing round
stays silent (no hint surface there) - the event's note covers it.

### D30 — The depletion trigger gets its control back, and the audit came up clean (2026-09-11)

D28 left the per-plant `depletionTrigger` readable everywhere and writable
nowhere except the species-care apply flow. It now has a slider in Edit plant
(20-80%, steps of 5), visible only when the plant is weight-tracked and not in
water, because for those plants the number drives nothing and a control that
does nothing is the defect this project keeps finding. A new plant's form
starts on the settings default so what the user sees is what a plain save
stores; an existing plant's form starts on its stored value. Saving an edit
now calls `rescheduleFromModel`, because the trigger moves the predicted
watering date and the check interval is derived from that prediction - without
the replan an edited trigger kept the old date until the next watering.

Two conversion details worth keeping: the form holds the trigger as a whole
percent, not a Double, and every Double-to-percent conversion in the app now
uses `roundToInt`. `(0.29 * 100).toInt()` is 28 - truncation walked 29%, 57%
and 58% down one point per open-and-save cycle, and showed the same off-by-one
in the two screens that print the percentage.

The same session made the deliberate audit pass the previous entry asks for:
every public `:core:domain` function checked for production callers, every
repository method, care-event type, route and reading context checked for a
reachable entry point, and the phone's real diary replayed through
`assembleWeightState` (correct anchors, slopes and ETAs for all three
weighed plants). No seventh wired-to-nothing instance. The one suspicion the
replay retired: the NULL anchor and EWMA columns in the database are the
derive-on-read design working as documented, not an unwired write path.

### D29 — Plant identification goes offline first, and F25 is dropped (2026-09-09)

Pl@ntNet's API is genuinely free at around 500 identifications a day, run by a
public research consortium rather than a startup, so it is unlikely to vanish or
start charging abruptly. The price was never the problem.

Two things decide it. A shared API key shipped inside the APK can be extracted
from the binary and its quota spent by anyone, so any honest version of F25
makes the user bring their own key, and at that point F25 and F25b are the same
code with a different logo. F25 is therefore dropped rather than deferred.

The larger objection is that identification sends a photo of somebody's home to
a third party. The app currently transmits exactly one thing, a species name the
user typed, off by default, and Settings says so in those words. A photo is a
different category of data and the feature would have to say so at the moment it
is used, not in a settings paragraph.

So **F25c leads**: a bundled TFLite classifier, no network, labelled as a rough
guess and trusted at genus level at best. **F25b stays as an opt-in second
tier** for anyone who wants better answers and accepts the trade, and only if
F25c turns out too weak to be worth shipping.

**The open question, which is not about plumbing.** This app's character is
that it refuses to guess: the weight model says which of three reasons it has
for staying quiet rather than inventing a date, and the species catalogue keeps
its generated tier visibly weaker than its curated one. A classifier that is
genus-level at best is, by construction, a guesser. It earns its place only if
its output is held to the same standard as everything else here, which means
showing a confidence, refusing below a threshold, and never letting a guess
reach the care advice. If it cannot clear that bar it should not ship, and
finding that out is the first task rather than the last.

### D28 — There is no calibration step any more, and there had not been for a while (2026-09-09)

D21 made a pot weighable before it had anchors, by deriving the wet anchor from
the most recent post-water reading. That quietly retired the calibration
ceremony: "Set the watered weight" was repointed at the keypad, and the dialog
behind it was left in place with nothing able to open it. `showCalibration` was
declared, the `if (showCalibration)` block was there, and no line anywhere set
it to true. `CalibrationDialog`, `WeightViewModel.calibrate` and
`WeightRepository.calibrate` were all unreachable.

That is the fifth instance of this project's recurring pattern and the first in
the mirror: not logic nothing calls, but a whole screen nobody can open. Both
shapes come from the same habit of changing one end of a path and not walking
the other.

Deleted, ninety lines of it. The "Not set up yet" card stays, because it is
still the page explaining why there is no prediction and what to do about it,
but its button now says "Weigh it now", which is what it does. There is no
setup step to name any more: weighing a pot just after watering makes the
anchor, from the plant page or from the weighing round, and D24's chip rule
means the app suggests that context itself at the moment it applies.

The card had also grown a second "Weigh it" button, visible on screen at the
same time as the page's own one and doing the same thing: another leftover from
when weighing here was a setup step rather than the ordinary action. The panel
explains, the page acts, and the whole screen now fits without scrolling.

`ReadingContext.CALIBRATION` stays in the enum and is now documented as legacy.
Nothing writes it, databases written before D21 still contain it, and every
place that derives an anchor already treats it exactly like POST_WATER.

**Left open on purpose.** That dialog was the only way to set a per-plant
depletion trigger by hand. The trigger now comes from the global default at
creation and from the species catalogue when curated care is applied, so a
plant sitting on a non-default value cannot be changed from inside the app.
Either it gets a control in Edit plant, next to the free-text "How dry before
watering" that drives nothing, or the field stops pretending to be per-plant.
Not decided yet.

### D27 — "Importing the same file twice changes nothing" was not true (2026-09-09)

The restore screen makes that promise, F10.6 was ticked off on it, and the row
counts backed it up: import the app's own backup and you still have 4 plants, 24
entries, 12 photos. Hashing the whole table content rather than counting it
showed three columns being rewritten on every import:

- `created_at` was stamped with the import's clock for every row. On a restore
  to a new phone that is right, since the row really is entering that database
  for the first time. On a re-import it rewrote the date every plant was added.
  It now keeps the value a row already has and only uses the clock for rows that
  are genuinely new.
- `updated_at` the same, with one more reason: the backup does not carry it, so
  there is nothing in the file saying the row changed. Writing "now" was
  inventing a modification.
- `weight_readings.context` came back in a different case. The app writes
  `ReadingContext.name`, while the import mapper wrote `.name.lowercase()`. It
  reads back correctly because the reader uppercases, so nothing broke and
  nothing showed, and a round trip quietly rewrote every stored value.

None of the three is visible in the UI, which is why they survived a feature
being marked done. The lesson is the assertion, not the bug: an idempotence
claim has to be tested by comparing content, because counts are exactly what an
upsert keyed by id will always get right.

`ImportIdempotenceTest` exports, imports twice, and compares whole rows across
every table. Verified on the device against the real diary as well: after the
fix, two consecutive imports left all seven tables byte-identical.

### D26 — A delete that asks, a pot that opts out, and a save that stopped eating fields (2026-09-09)

**Deleting a diary entry now asks.** It used to fire on the tap, with no
confirmation and no undo, on the least reconstructable data in the app. The row
opens its menu on a plain tap as well as a long press, the menu's only item is
the delete, and the menu renders *above* the row it belongs to rather than
below it, so what you are about to delete is not where you think it is. That
combination cost a real entry (D25). The confirm names the entry it is about to
remove, which is the part that matters: it turns a mis-aimed tap into something
you can see before it commits.

**A pot can opt out of being weighed.** `isWeightTrackable` was derived from the
medium alone, and the medium cannot rule out a closed terrarium: it recycles its
own water, loses almost nothing, and the model would say "not drying measurably"
forever while the weighing round kept asking. Schema v10 adds `weight_tracked`
with a default of 1, so every existing pot keeps the behaviour it had, and the
edit form carries the switch. The Fittonia is the first pot to use it.

**Two bugs found on the way, both in saving a plant.** `PlantEditViewModel`
built a fresh `Plant` from the form and handed it to `upsertPlant`, which writes
the whole row. Every field the form does not show, the depletion trigger, the
cover photo, the propagation stage, the pot measurements, was reset to its
default on any save. Nothing had noticed because the plants edited so far
happened to be sitting on the defaults. It now edits the stored plant with
`copy` instead. Separately `upsertPlant` stamped `created_at` with "now" on
every write, so editing a name reset the date the plant was added.

**The backup manifest was undercounting itself.** It listed plants, events,
photos and reminders while the archive also carried the weight readings and the
room log, which are the two most recent additions and the least reconstructable
things in it. The manifest is what you read to decide whether a backup is
complete, so it now counts everything it holds.

**F10.1 is exercised at last.** The SAF `CreateDocument` picker opens with the
suggested filename, saving writes a 1.49 MB archive, the zip passes an integrity
check, and the manifest's counts match the archive's actual contents including
the new `weightTracked` field round-tripping. The one thing still not driven is
import, which is F10.6 rather than F10.1.

### D25 — A stray synthetic tap deleted a care event again (2026-09-09)

Same failure as D20, same cause, one day later. Verifying D24 on the device
meant giving a plant a location and taking it away again, which left two MOVED
events behind. Deleting those through the app's own history needed a long press
and then a tap on "Delete this entry" in the menu that opens under it. The menu
position was assumed rather than read off the screenshot, the tap landed on the
wrong row, and it deleted a real entry: `e0db6a86`, an OBSERVATION on Creeping
fig at 09 Sep 17:27, the one carrying a photo.

Caught the same way as last time, by diffing the care event ids against a
backup taken before the session rather than by trusting the count, which is why
the backup is taken first every time now.

The repair: reinsert that exact row from the backup, and remove the two MOVED
artifacts, so the restored file's care event id set is identical to the backup.
The photo row was never touched, so restoring the event restores the link.

The rule that keeps being violated: a synthetic tap on a menu whose position
was inferred is a guess, and a guess aimed at a delete control is a data loss
waiting to happen. Screenshot first, read the coordinates off the image, and
where a mis-tap deletes something, prefer a path that has no delete control on
it at all.

### D24 — Three places where the app's flow and the actual workflow disagreed (2026-09-09)

All three are the same shape as D23: the logic was right and the moment was
wrong.

**Light belongs to a place, not to a plant.** You go and hold the phone at the
window because you want to know about the window. The meter could only be
opened from a plant, so measuring an empty corner meant picking some unrelated
pot and pretending the reading was about it. Places can now start a measurement
directly, and in that mode the screen lists every plant living there with
whether the spot suits it, which is the question you were actually asking. The
keyword rule that decides "suits it" is now `lightFitFor`, with
`assessLightFor` as its one-plant wording, because Places needs the same
judgement in two words rather than a sentence and two copies of a keyword match
would have drifted.

**Watering and weighing are one moment the app modelled as two.** You water a
plant from the list, carry it to the scale, and the keypad opens on ROUTINE, so
the reading that should have become the new wet anchor is filed as an ordinary
sample. Nothing looks broken. The anchor just never gets recaptured and quietly
goes stale as the plant grows. `suggestReadingContext` now starts the chip on
"after watering" when the pot was watered within the last day and has not been
weighed since, and the screen says why it moved rather than letting a
preselected control change what a number means in silence. A day is the ceiling
because drainage finishes in an hour while people weigh when they get round to
it, and a pot weighed three days later has visibly dried. The weighing round
does the same per pot and flags the ones that owe a wet mark, since after a
watering round those are the readings worth the most.

**A repot invalidates the calibration and the user found out days later.**
Logging a repot cleared the anchors on save, said nothing, and the next visit to
that plant's weight screen had gone back to asking to be set up. The log form
now says so while the type is selected, along with what to do about it: water it
in, weigh it once, and the full mark sets itself again, which is true because of
the derived wet anchor from D21.

Driving the three on the device found three more bugs that the unit tests could
not have, and all three were about a value being read from the wrong place:

- The repot warning asked `plant.anchors`, the stored column. Since D21 made
  calibration derived from a post-water reading, that column is null for every
  plant, so the warning would never have appeared once. It now asks the
  assembled `WeightState.isCalibrated`.
- The place-mode light screen reported "nothing lives here" for a place with a
  plant in it. Its `residents` flow used `WhileSubscribed` while nothing ever
  collects it: it is read synchronously out of a sensor callback, so it sat at
  its initial empty value forever. `Eagerly` is the right sharing policy for a
  flow nobody subscribes to.
- The weighing round's "just watered" row flag had no time window while the
  keypad's chip rule had one, so a pot watered two days ago would have been
  labelled "just watered" above a keypad that had already decided otherwise.
  Both now use `POST_WATER_WINDOW_MILLIS`.

Four more sites were logging care events without replanning the reminder, found
while doing the above and fixed with them: the notification's own "Watered"
button, which is the least friction the app offers and was the one path that
left the schedule untouched entirely; the detailed log screen; deleting an event
from a plant's history; and the weighing round, which never rescheduled at all,
so weighing everything in one sitting moved no due dates. That makes D23's
count wrong in the right direction: it was not four call sites, it was eight.

### D23 — The reminder interval was computed but almost never used (2026-09-09)

The whole premise of the app is that a reminder tracks the measured pot rather
than a calendar, and `resolveIntervalDays` implements exactly that priority:
the user's explicit setting, then the weight prediction, then the log, then a
week. It was correct and unit tested, while three of its four callers passed it
nothing:

    ReminderBackfill      resolveIntervalDays(null, null, null)         -> a week
    PlantEditViewModel    resolveIntervalDays(null, null, null)         -> a week
    DueViewModel          resolveIntervalDays(explicit, null, null)     -> yours, or a week
    WeightViewModel       the only site that passed a real prediction

`DashboardViewModel` did not reference reminders at all, so watering or checking
a plant from the list never replanned anything. On the real database every plant
sat at a flat seven days with no interval set, which is the app quietly
degrading to the calendar reminder it was built to replace.

The fix is a single scheduling entry point, `ReminderRepository.rescheduleFromModel`,
which gathers the plant, its events and its readings, assembles the weight state,
and resolves the interval in one place. Every caller now goes through it, so
there is exactly one implementation of "when is this plant next due" instead of
four partial ones. This is the fourth instance of the same failure in this
project: logic that exists, is tested, reads correctly, and is not wired to
anything. Reviewing the call sites of a pure function is not optional.

A stale due date is its own bug, so the backfill now replans every plant on
launch rather than only the ones it creates. A default week is the state of a
plant nobody has touched, which is precisely when the default is least likely
to be right, and waiting for the user to open each plant to fix it defeats the
purpose.

Running it against the real database surfaced a second problem. Fittonia had two
waterings 1.32 days apart, the average said a 1 day rhythm, and the plant fell
due the next morning. Two waterings a day apart are one episode, a top-up or a
correction, not a cycle. Scheduling now uses `checkIntervalFromLogDays`, which
collapses gaps under half a day, requires at least two real gaps before the log
is allowed to speak, and takes the median so one holiday does not double the
interval. `averageWateringIntervalDays` is unchanged, because the plain average
is still the honest thing to show someone on the plant page. What is right to
display and what is right to plan on are not the same statistic.

Verified on the device, not just in tests: `ReminderPlanningTest`, 7 instrumented
tests against a real Room database, all passing, plus the four real plants
replanning on launch with Flax seeds picking up a measured 5 day Eta.

### D22 — Weighing is a round, not a per-plant errand (2026-09-09)

Reported after the first real weighing session: doing every pot meant plant,
menu, weight, back, plant, menu, weight, four taps of navigation overhead per
reading on the app's most-repeated action.

The data is per-plant but the activity is not. The scale comes out once and
every pot goes on it, so the round is the unit of work. "Weighing", from the
plants list, is the running sheet: every weight-trackable pot, what it weighed
last time, and what it weighs now. Saving advances to the next pot that has not
been done this round, so you put one down, pick the next one up, and the app is
already asking for the right number.

"Done" means done in this round, not ever, which is why the view model records
its own start time rather than looking at whether a reading exists. Otherwise
the list would show everything as finished the moment a plant had ever been
weighed.

Plants in water are left out. Weight says nothing about a cutting in a jar, and
the round should not ask for a number that means nothing.

### D9 — MIT licence (2026-09-06)

`LICENSE` to be added at `git init`. Copyright holder: the repo owner, under
`dheirav2005.com`.

---

## Next actions

The council review is closed, all 19 findings fixed across D43, D46 and D47,
and the four items that stood here before it are done or closed too: the
call-site audit (D30a), F14, F11 and F12, and F16 with F25 (D41, D42).

What is left is the things code cannot settle by itself, plus the UI work the
user has been finding by using the app.

The five UI items that stood here are done, D48. Two of them uncovered defects
that were not part of the ask, which is the argument for doing this kind of work
by using the app rather than by reading it.

**Help is in the wrong place and is not one category.** Raised by the user
2026-10-01, deferred by them, not yet designed.

Two problems, and the second is the sharper one. Help sits behind Settings,
which is the exact arrangement `docs/NAVIGATION.md` section 1 was written to
undo: "nobody looks in Settings for a feature, so for a new user those seven do
not exist." Seven things were moved out of there, and then Help was put in.

And Help is four different kinds of thing behind one word. "A plant does not
look right" is a **feature**, the diagnosis tree, and the only entry that does
something rather than explains something. "Reminders are not arriving" is
support. "What this app is for" is the first-run explainer. "Finding your way
around" is a map, D49. Somebody worried about a plant is not looking for help,
they are trying to do something, and that something is currently four taps deep
behind a word meaning "I am confused".

So D49 repeated the mistake it was documenting, one level down. The fix is
probably to pull the diagnosis out of Help entirely and put it where a worried
person already is, which is the plant, and let Help be documentation only. Not
decided, and the user wants to design it rather than have it designed.

**Signing.** Parked at the user's request, D39. The keystore needs a password
only the user can choose, and until it exists there is no upgradeable install
and no Play listing.

**A week of ordinary use.** The one thing still genuinely unverified. Every
prediction property is tested against synthetic curves, and `PredictionEvaluation`
replays real readings, but nobody has yet watched the app say "four days" and
counted four days. The model's accuracy is a claim, not a measurement.

**The friend's review.** The debug APK and `docs/REVIEW-BRIEF.md` are with an
Android dev, along with a restore of real data. His feedback is the first
outside read the app has had.

**The landing page.** The user chose both halves of the tutorial question. The
in-app half is built (D36's intro page and the help screens); the page with a
scripted demo is not.

## Features worth considering

Not committed to, and deliberately after the list above, because the accuracy
panel is already built and has nothing in it yet: every idea here is a guess
until the core claim has been measured once.

**An away sheet.** The model knows each pot's ETA, so it can answer the question
somebody actually has before a trip: over the next N days, which plants will
need water, roughly when, and how much. Shareable as text for whoever is looking
after them. No calendar-based app can do this well, which is the point.

**Make the prediction arguable.** Tap the ETA and see why: the readings it
fitted, the slope, the trigger weight, and why the confidence tier is what it
is. The weight screen has the chart but the number itself is not tappable. It
suits an app whose stance is honest uncertainty, and it needs no new data.

**Decide about ambient conditions.** `Ambient.kt` holds real logic for "this pot
is drying faster than the room explains" and the live database has one ambient
reading in it. So either the manual form is too much friction or the payoff is
invisible. That is a decision to make, not code to write: reduce the friction or
close the feature the way F16 and F25 were closed. Half-used is the worst of the
three.

## Conventions

- Package root: `dev.dheirav.thirsttrap`
- Repo name when created: `thirsttrap` (lowercase, one word)
- All timestamps stored UTC epoch millis **plus** a local UTC-offset column —
  see `docs/DATA-MODEL.md` for why.
- Times shown to the user are IST.
