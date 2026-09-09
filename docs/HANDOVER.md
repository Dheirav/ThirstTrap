# ThirstTrap — Handover

**This is the live state document.** Read it first, every session. The README
is the pitch and may lag; `plant-tracker-requirements.md` is the *what and
why*; this file is the *where we are*.

Last updated: 2026-09-09

---

## Status

**Phase: M0 through M3 complete. M4 partly done. 95 of 101 features built,
all of it running on the target phone against a real plant diary.**

Repo: https://github.com/Dheirav/ThirstTrap (branch `main`).

- **214 JVM tests** across `:core:domain` and `:core:ui`, running in about a
  second with no device attached.
- **13 instrumented tests** in `:core:data`, covering schema migrations, the
  reminder planning that gathers its own inputs, and the export/import round
  trip. These need a phone.
- Schema is at **v10**, every step an auto-migration, `exportSchema` on and
  `schemas/*.json` committed. `fallbackToDestructiveMigration` appears nowhere.
- The app runs daily on a Redmi Note 15 Pro against four real plants. Most of
  the defects in the decisions log below were found that way rather than at the
  desk, which is the single most useful habit this project has.

### What is left

Six features, all M4 Phase 2, plus one decision:

- **F11** experiments, **F12** `[[plant]]` cross-links, **F14** fertiliser
  dilution calculator, **F25c** offline plant identification with **F25b** as an
  opt-in second tier. F25 itself is dropped, see D29.
- **F16 cloud backup** is unstarted *and undecided*. Every other feature was
  built on "nothing leaves this phone", and Settings says so in those words. An
  account changes what the app is, so it wants a conversation before code.
- The per-plant depletion trigger has no control since D28 removed the dialog
  that was its only one. Either it gets one in Edit plant, or the field stops
  pretending to be per-plant.

### The habit worth keeping

Six times now the same defect has appeared: logic that is correct, tested,
reads properly, and is wired to nothing. `X4` reduce-motion, `CareEventType.MOVED`,
`deleteReading`, `resolveIntervalDays` at eight call sites, a calibration dialog
nothing could open, and a duplicate button. It comes from changing one end of a
path and not walking the other. A deliberate pass through `:core:domain` asking
"who calls this, and with what" is worth more than the next feature.

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

In order, and only the first is uncontroversial.

1. **The call-site audit** described under Status. Six instances of one defect
   is a pattern, not bad luck, and it is cheaper to find the seventh on purpose
   than to have a plant find it.
2. **F14, the fertiliser dilution calculator.** Small, self-contained, no new
   concepts, and the last easy win in M4.
3. **F11, experiments.** The largest remaining piece and the one that pays off
   the weight model: "does the north window dry it slower" becomes a measured
   answer rather than an impression.
4. **Decide F16 and F25 before writing either.** Both cross the line the app has
   held since the first commit. F16 wants an account; F25 and F25b send a photo
   off the device. F25c, the offline classifier, does not, which is the whole
   reason it is listed separately.

## Conventions

- Package root: `dev.dheirav.thirsttrap`
- Repo name when created: `thirsttrap` (lowercase, one word)
- All timestamps stored UTC epoch millis **plus** a local UTC-offset column —
  see `docs/DATA-MODEL.md` for why.
- Times shown to the user are IST.
