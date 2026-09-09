# ThirstTrap

An offline Android plant diary that tells you when a pot actually needs water,
by weighing it.

Every plant app asks you to pick a schedule and then nags you against it. A
calendar knows nothing about your window, your winter, or the fact that the pot
by the radiator dries in three days while the one in the hall takes nine.
ThirstTrap measures instead: you put the pot on a kitchen scale now and then,
and it fits a drying curve to the readings.

Kotlin, Jetpack Compose, Room. No account, no server, no telemetry.

---

## How the weighing works

A pot loses water by evaporation and transpiration, both roughly steady over a
day, so its weight falls near-linearly between waterings. Two numbers pin the
line:

- the **wet anchor**, what it weighs just after watering and draining
- the **dry anchor**, what it weighs when it genuinely needs water again

Every weighing places the plant somewhere between them, and the slope of the
recent readings says when it will reach the trigger. That is the whole model:
roughly fifty lines of arithmetic, no machine learning, and it is the same
managed-allowable-depletion framework commercial greenhouses irrigate by.

What makes it survive contact with a real shelf is the handling around the
edges:

- **Theil-Sen regression** rather than least squares, because one reading taken
  with a wet saucer still under the pot should not bend the line.
- **The dry anchor improves itself.** It starts as an estimate and walks down
  toward wherever this particular plant is actually watered, so nobody has to
  let a plant wilt to calibrate a convenience feature.
- **Segmentation.** A watering resets the cycle, a repot invalidates every
  reading before it, and the app says so at the moment you log the repot rather
  than letting you discover it days later.
- **It refuses to guess.** With one reading, a contradictory slope, or a pot
  that is not drying measurably, it says which of those it is instead of
  printing a confident date. A closed terrarium can be marked as not worth
  weighing at all.

There is no calibration ceremony. Weigh a pot just after watering it and the
anchor sets itself, and the app suggests that reading context on its own when
it can see you watered recently.

---

## What else is in it

- **A diary, not a checklist.** Waterings, checks, repots, fertiliser, pests,
  photos, deaths, with a full timeline per plant and a photo timelapse.
- **A weighing round.** Weighing is not a per-plant errand: the scale comes out
  once and every pot goes on it, so there is one screen that walks the round and
  advances as you save.
- **164 indoor species** bundled offline, in two tiers that the interface keeps
  visibly apart: 49 hand-written entries that will tell you what usually kills
  a plant, and 115 generated from open data that will not pretend to. Toxicity
  comes from the ASPCA list. An optional online lookup resolves *names* against
  GBIF and links Wikipedia; it can never fetch care advice, so it cannot invent
  a watering schedule.
- **Reminders that track the pot**, not the calendar: the interval comes from
  the measured prediction where there is one, the log where there is not, and a
  week only as a last resort.
- **Places**, a gazetteer of the spots plants live in, with a phone light-meter
  reading recorded against the place rather than thrown away.
- **Backup you own.** One zip through the system file picker, containing every
  entry and every photo, importable back and idempotent by id.

---

## What it will not do

- No streaks, no gamification, no failure counts. **A missed day must not feel
  like failure.** This is the constraint the rest of the design bends around.
- No account, ever. No social feed. No sharing.
- No moisture probes: the cheap resistive ones corrode within weeks and the app
  would be reporting their decay.
- No sensor hardware for the weight model. A kitchen scale you already own is
  the whole apparatus.

The one thing that ever touches the network is the species name lookup, it is
off by default, and it sends the name you typed and nothing else.

---

## Building

```
./gradlew :app:assembleDebug          # the app
./gradlew :core:domain:test           # the watering model, ~1s, no device
./gradlew :core:data:connectedCheck   # migrations and import round trip, needs a phone
```

Four modules: `:app`, `:core:domain` (pure Kotlin, no Android), `:core:data`
(Room, WorkManager, DataStore), `:core:ui` (theme and shared components). The
watering model is deliberately isolated in a JVM module so it can be tested
exhaustively without a device, which is where most of its tests live.

---

## Where the documentation is

`docs/HANDOVER.md` is the live state of the project and the file to read first.
`docs/WATERING-MODEL.md` is the algorithm in full. `plant-tracker-requirements.md`
is the original product brief. The decisions log at the bottom of the handover
records what running this on a real phone against real plants actually found,
including the mistakes.

MIT licensed.
