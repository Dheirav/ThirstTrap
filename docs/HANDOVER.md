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
