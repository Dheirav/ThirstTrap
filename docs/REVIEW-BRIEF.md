# ThirstTrap: notes for a reviewer

Written 2026-09-30, to go with the APK. The point of this is to save you time on
the things that are deliberate, so your attention lands on the things that are
not.

## What it is, in two lines

An offline Android plant diary whose differentiator is weight: you put a pot on
a kitchen scale now and then, it fits a drying curve to the readings, and it
tells you the day the plant actually needs water instead of nagging you against
a calendar.

Kotlin, Compose, Room, Hilt, WorkManager. Four modules: `:app`, `:core:domain`
(pure JVM, no Android), `:core:data`, `:core:ui`. 272 JVM tests and 15
instrumented, all passing. Schema at v14, every step an auto-migration with the
exported JSON committed.

## Deliberate, so please argue with the decision rather than filing it

- **No account, no cloud, no sync.** Closed as a decision, not deferred: see D33
  in `docs/HANDOVER.md`. Every other feature was built on "nothing leaves this
  phone" and the Settings screen promises it in those words. The export is one
  zip you keep, and the import is idempotent and tested.
- **The one network call** is an opt-in plant-name lookup against GBIF, off by
  default, which sends the name you typed and nothing else. It can never fetch
  care advice, so it cannot produce a wrong watering schedule.
- **Reminders are inexact.** No `SCHEDULE_EXACT_ALARM`: a watering check does not
  need to land at 09:00:00, and the permission is a Play policy problem for this
  category. D4.
- **No streaks, no counts of what you missed, no scoreboard.** "Still wet, left
  it alone" is a first-class answer with the same visual weight as "watered".
  This is the constraint the rest of the design bends around, including why the
  home-screen widget is read-only: a widget row cannot carry both answers, and
  carrying only "watered" would teach that watering is the correct one.
- **The app refuses to predict** when it cannot, and names which of several
  reasons it has. That will look like missing functionality in a screenshot. It
  is the product.
- **minSdk 26**, for `java.time` with no desugaring and notification channels as
  a first-class concept.
- **Design is deliberately not Material-default.** Paper and ink, square
  corners, ruled panels, two typefaces bundled as variable fonts. The palette is
  measured for WCAG AA against the surface each text sits on, with a test suite
  that fails if a role drifts. `docs/DESIGN-RESEARCH.md`.

## Known unfinished, no need to report

- **Plant identification** (offline classifier, and an opt-in online tier) is the
  only product feature not built. The offline one starts with an evaluation that
  may honestly conclude "do not ship this".
- **The plant list flashes empty for a frame** on cold start while Room answers.
- **No Play listing, no privacy policy page** yet. GitHub first, Play later.
- If the APK you have is a **debug build**, Settings shows a "Debug tools" entry
  and the app is unminified. Both disappear in release.

## Where I would most value your eye

1. **Is the premise legible?** Someone who does not learn that this app weighs
   pots has a worse diary than a paper notebook. There is a first-run page that
   tries to land exactly that, and one line in it about tipping a heavy pot onto
   one edge, which is the trick that makes the feature work on floor plants.
2. **Discoverability.** Seven features used to live behind the Settings gear.
   They now sit in an ordered overflow on the plants list, jobs above a rule and
   reference below it. I argued for a fourth tab and lost the argument; I would
   like a second opinion on whether the overflow is findable.
3. **The refusals.** Do they read as trustworthy or as the app being broken? That
   judgement decides whether the whole design approach is right.
4. **Anything in `:core:domain`.** It is pure Kotlin with no Android in it, about
   fifty lines of real arithmetic under a lot of tests, and it is where a wrong
   answer would actually hurt someone's plant. `docs/WATERING-MODEL.md` is the
   algorithm in full.

## Things I already know are interesting problems

- A pot too heavy for a kitchen scale looked like it excluded the feature. It
  does not: depletion is a ratio and the ETA is remaining over slope, so a
  constant factor cancels and any measurement *proportional* to the weight
  carries the same information. `PartialWeightInvarianceTest` proves it at 1.0
  down to 0.1 of the pot. What does not cancel is the instrument, so the noise
  floor is derived from the scale's step rather than a constant. D36.
- The same defect has appeared eight times: logic that is correct, tested, reads
  properly, and is wired to nothing. There is an audit in D30a and it is still
  the thing I would bet the next bug on.

## Installing

Allow install from unknown sources once, then the APK. It needs no permissions
at install time. It will ask for notifications on first launch, and for the
camera only when you first attach a photo.

`docs/HANDOVER.md` is the live state of the project and its decisions log is the
interesting half: it records what running this on a real phone against real
plants actually found, including the mistakes.
