# ThirstTrap — Handover

**This is the live state document.** Read it first, every session. The README
(when it exists) is the pitch and may lag; `plant-tracker-requirements.md` is
the *what and why*; this file is the *where we are*.

Last updated: 2026-09-06

---

## Status

**Phase: M0 prototype built, installed and driven on the target phone.**

Repo: https://github.com/Dheirav/ThirstTrap (branch `main`).

- `:core:domain` carries the complete watering model under **57 JVM tests, all
  passing** in ~1s with no device.
- The M0 app runs on the Redmi Note 15 Pro. One-tap logging, undo, the quick-log
  sheet, and the reminder with its three shade actions are all verified working
  on device.
- **M0's actual question is still open**: does this beat a paper note, and does
  "Still wet" feel as good to tap as "Watered"? That needs three days of real
  use with real plants, and only the user can answer it.

### What running it on the phone found

Five defects, none visible at the desk:

1. **Undo did not undo** - the log row was removed but the derived
   `lastWatered` was never restored, so the card still read "Watered today".
2. **The list re-sorted on log**, moving cards out from under the finger.
   Reproduced live: logging one plant and reaching for UNDO hit a different
   plant's droplet. The order is now frozen until the undo window closes.
3. **Receiver writes were invisible to the UI** - the screen used a local tick
   instead of collecting the shared flow, so "Still wet" from the shade never
   reached the dashboard.
4. **A check was recorded as a watering** - "Still wet" made the card say
   "Watered today", the exact conflation this app exists to prevent.
5. Display faults: "Watered 1 days ago"; a water-propagation cutting told to
   "weigh once more"; a past-trigger plant showing "40% toward watering" beside
   "Needs water now"; content clipped under the navigation bar.

Two of the 57 domain tests also failed on their first run - one a real bug (a
provisional dry anchor was never replaced by a higher real observation), one a
worthless test (its outlier sat mid-series, where it has no leverage on a
least-squares slope, so both estimators passed and it proved nothing).

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
| `docs/FEATURES.md` | Flat checklist of all 98 features, IDs mapped to requirements items. |
| `docs/DEVICE.md` | adb reality, phone constraints, toolchain. Read before debugging anything. |

## What is missing

- **`mobile-stack-comparison.md`** — cited by `plant-tracker-requirements.md`
  as living "in this directory". It does not exist and has been **written off**
  (2026-09-06). The stack section of the requirements doc stands as
  summary-only; decision D1 supersedes its conclusion anyway.
- **No git repository.** `git init` has not been run.

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

### D9 — MIT licence (2026-09-06)

`LICENSE` to be added at `git init`. Copyright holder: the repo owner, under
`dheirav2005.com`.

---

## Next actions

In order. See `docs/ROADMAP.md` for the full milestone breakdown.

1. `git init`, `.gitignore`, MIT `LICENSE`, initial commit of the docs.
   **No toolchain setup is needed** — the JDK pin and SDK are already in place
   from earlier projects. See `docs/DEVICE.md` §4.
2. **M0 — prototype the two hard UX problems before building anything else.**
   The requirements doc names them: the 3-tap log, and reminder tone. If
   logging is slower than a paper note, the app dies regardless of how good
   the drying-curve maths is. Prototype these as throwaway Compose screens
   with fake data.
3. M1 — MVP scaffold and requirements items 1–10.

## Conventions

- Package root: `dev.dheirav.thirsttrap`
- Repo name when created: `thirsttrap` (lowercase, one word)
- All timestamps stored UTC epoch millis **plus** a local UTC-offset column —
  see `docs/DATA-MODEL.md` for why.
- Times shown to the user are IST.
