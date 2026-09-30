# Council review, 2026-09-30

Twelve agents reviewed the app across six dimensions, then every claim was handed
to a separate skeptic whose job was to refute it. 26 claims went in, 21 came out
confirmed and 5 were refuted. 874 seconds, 1.42M tokens, no agent errors.

The dimensions were: the watering model, wired-or-not (logic with no caller), data
safety, Android runtime behaviour, says-what-it-does (docs and UI copy against the
code), and test quality.

The 19 percent refutation rate is the number to judge the rest by. The skeptics did
not rubber-stamp: they threw out four findings as real-but-harmless and one as
already covered by another test, and they corrected an overstatement inside roughly
half the findings they confirmed. Several confirmations came from running a probe or
mutating a line and re-running the suite rather than from reading, which is why I
trust them.

Two pairs are the same defect found twice by different dimensions (#7 with #15 on
the backup, #6 with #16 on the exact-alarm switch), so there are 19 distinct
defects.

## HIGH

**All 19 are fixed.** D43 (findings 1 and 2), D46 (3 and the diagnosis of 4),
D47 (4 and the remaining fifteen). The findings below are kept as written,
because the reasoning is the useful part and several of them corrected the
original claim.

**1. The dry anchor survives a repot.** `WeightAssembly.kt:80` **(fixed, D43)**
The `sinceRepot` filter applies only to the derived wet anchor, while the replay
loop folds every non-excluded reading into the anchors with no timestamp predicate,
so a PRE_WATER reading from the old pot keeps the old pot's lighter dry weight
forever. The skeptic ran it and got `Anchors(wet=3000, dry=700, provisional=false)`
where the correct post-repot answer is `Anchors(3000, 1800, provisional=true)`. The
damage is the trigger weight: 1850 g instead of 2400 g, which means the app waits
until the pot is about 96 percent down its true range before prompting, and that is
the unsafe direction. The dashboard ring also shows the wrong depletion percentage.
This contradicts WATERING-MODEL section 2 and the code's own comment three lines
above. The existing repot regression test passes only because its pre-repot readings
are all POST_WATER, so the dry end is never touched.

**2. Nothing bounds the dry anchor from above, so the range can be zero.**
`Anchors.kt:80` **(fixed, D43)**
The provisional-replacement branch guards only the low side, so a PRE_WATER reading
at or above the wet weight sets `dryGrams == wetGrams`, `rangeGrams` becomes 0 and
`depletionAt` returns NaN. Measured: `Anchors(1000, 1000, false) range=0.0
depletion=NaN pred=WaterNow`. Kotlin's `Double.coerceIn` passes NaN straight through
because both comparisons are false, so the NaN reaches the dashboard ring sweep and
`(NaN*100).toInt()` prints 0 percent on the weight screen. The trigger equals the wet
weight, so WaterNow becomes permanent. Reachable because `suggestReadingContext`
returns PRE_WATER whenever the state is WaterNow, and nothing backfills a PRE_WATER
filed at the wet weight. This breaks the stated invariant that the ETA is never NaN
for any input.

**3. Photo capture crashes on first use.** `PhotoCapture.kt:66` **(reproduced and fixed, D46)**
D35 added an unconditional CAMERA declaration to the manifest for the in-process QR
scanner. The photo path uses the stock `TakePicture` contract, which fires
`ACTION_IMAGE_CAPTURE`, and the platform aborts that intent with SecurityException
precisely when the caller declares CAMERA without holding it. It is a no-op for apps
that do not declare it, which is why this only became reachable with D35. The only
runtime camera requests in the repo are the three in `ScanPotScreen.kt`, so anyone
who attaches a photo before ever opening the scanner hits an uncaught crash. The
comment at `PhotoCapture.kt:21` still says "needs no camera permission", and the
review brief I wrote for the APK repeats it.

This one was argued from platform behaviour rather than observed. It has since
been reproduced on the device by revoking CAMERA, which is the same state as a
fresh install, and the crash is exactly as described. Fixed, see D46.

**4. The backup silently loses every location note and lux reading.** **(fixed, D47)**
`ExportRepositoryImpl.kt:104`
`ExportBundle` has ten fields and none of them is locations. `LocationDao` is not
even a constructor parameter of the export repository, so the table cannot reach the
archive by accident, and `importFrom` never writes a location row. `LocationDao.all()`
has zero callers anywhere. The data is user-authored and unreconstructable: the note,
the lux value and when it was measured, written from the Places screen and the light
meter. The exported ambient list is not a second copy, because `AmbientReading` has
no lux field. Export, wipe, import reports success and the notes are gone. What is
lost is the latest lux per place rather than a history, since the table is one row
per name.

## MEDIUM

**5. A watering logged after the last reading is invisible.** **(fixed, D47)** `Segmentation.kt:57`
The watering predicate is evaluated only between consecutive readings, so an event
after the most recent reading never opens a segment. Measured: a pot watered on day
8.1 and viewed on day 9 still reads `depletion=1.0 pred=WaterNow`. The dashboard
comment says the prediction wins over the fact, so the card says "Needs water now"
for a pot watered an hour ago, the widget agrees, and attention rank pins it at 1.
Not raised to high because the reminder clock runs off the WATERED event separately
and the state self-corrects on the next weigh-in.

**6. "Snooze 1 day" on the notification does nothing.** **(fixed, D47)**
`ReminderActionReceiver.kt:59`
`ACTION_SNOOZE` maps to `type = null`, the toast says "Snoozed for a day", and the
`if (type == null) return` sits above the only coroutine block in the file, so the
repository is never touched. `snoozed_until` stays null, the row is still due on the
next sweep, and the notification returns tomorrow. `docs/NOTIFICATIONS.md:113`
specifies the opposite, so this is a gap and not a closed decision. Note for the fix:
the receiver carries only the plant id while `snooze()` takes a reminder id, so it
needs a lookup, not a one-line change.

**7. The exact-alarm switch gates nothing and cannot be turned on.** **(fixed, D47)**
`SettingsScreen.kt:138` and `:148`
`useExactAlarms` is written, read back into its own switch, and read nowhere that
affects scheduling. `ReminderScheduler` does not inject settings at all. Neither
manifest declares SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM, so on API 31 and up
`canScheduleExactAlarms()` cannot return true, the setter takes its early return
without storing, and the subtitle permanently shows the "needs the permission"
branch while promising "on makes them land on the minute". `openExactAlarmSettings`
sends the user to a system page the app cannot appear on. This is the item D30a left
open: honour it or remove it.

**8. Import overwrites a live photo in place.** **(fixed, D47)** `ExportRepositoryImpl.kt:217`
The import target is byte-for-byte the live path, and `target.outputStream()`
truncates on open, so the local file is zeroed before the first inflated byte
arrives. A truncated entry or a process death mid-copy leaves the photo row pointing
at an empty JPEG, and `orphanFiles` will not flag it because the row still exists.
Harmless for a clean zip, which is why it is medium and not high, but the export side
by contrast never touches an original. A staging file plus a rename fixes it.

**9. The reminders troubleshooter gives a false all-clear.** **(fixed, D47)**
`RemindersHelpScreen.kt:108`
`fireTestReminder` calls the notifier directly inside `viewModelScope`, so the test
exercises the notification permission and the channel and nothing else. The screen
then tells the user that a delivered test means background work is allowed, which the
code cannot support. `ReminderScheduler.runSweepNow` already exists and is exactly
the right primitive; the help screen does not even inject the scheduler. The user
acts on the false conclusion by leaving Autostart off.

**10. Three surviving instances of the D38 calendar-day bug.** **(fixed, D47)** `DueViewModel.kt:56`,
`ReminderWorker.kt:42`, `DashboardScreen.kt:534`
All three divide an elapsed duration by 86,400,000 instead of differencing two local
day indices, so "Checked today already" and "It's been 1 days since you checked" are
wrong for most of every day. `calendarDaysAgo` has exactly two non-test callers, both
in `DashboardScreen`, so the D38 fix landed only on `relativeDays`. The claim found
two sites and the skeptic found the third. The notification string is also always
plural.

**11. The wall-clock decay term in the ETA has no test.** **(fixed, D47)** `Prediction.kt:95`
Proved by mutation: replacing the term with `daysFromLatest + 0.0 * elapsedSinceLatest`
left all 256 tests passing. Every test asserting an Eta value passes `nowMillis` equal
to the latest reading's timestamp, so `elapsedSinceLatest` is always 0. The shipped
code is correct, so this is an unpinned term rather than a defect, but it is the whole
countdown the dashboard shows between weigh-ins. Half the original claim was wrong and
dropped: the sign flip is caught, by the partial-invariance test which happens to put
`now` a day before the last reading.

**12. The recency rule in the slope fit has no test.** **(fixed, D47)** `SlopeFitTest.kt:89`
The 20-reading fixture is an exact line, so any five readings give the same slope and
the test asserts a count rather than which five. Proved by mutation: changing
`takeLast(5)` to `take(5)` left all 256 tests passing. Recency is documented behaviour
in WATERING-MODEL section 4, and with a 21-day stale gap a daily-weighed pot stays in
one segment long enough for the two to diverge.

**13. EWMA_ALPHA is unpinned.** **(fixed, D47)** `SlopeFitTest.kt:108`
One test sits exactly on the fixed point, where `a*x + (1-a)*x = x` for every alpha,
and the other asserts a bound that holds for any alpha above 0.206. Setting
`EWMA_ALPHA = 1.0`, which is no smoothing at all, left all 256 tests passing. At alpha
1.0 the prior collapses to the previous segment's slope, which also suppresses the
drying-faster-than-usual diagnostic. The doc is complicit here rather than
contradicted: section 9 asks for exactly the two properties these tests check, and the
inertia that justifies 0.3 was never turned into an assertion.

## LOW

**14. Two notification channels exist with nothing posting to them.** **(fixed, D47)**
`ReminderNotifier.kt:29`
`CHANNEL_TASKS` and `CHANNEL_HEALTH` are created at first launch and appear in system
settings. No task reminder can be created, because both `Reminder` constructions in
main sources pass `CHECK`, and the worker posts to the checks channel regardless of
kind anyway. The health diagnostic is only ever rendered in-app. So the cost is two
system-settings categories promising notifications that cannot arrive.

**15. Experiments import with `updatedAt = now` unconditionally.** **(fixed, D47)**
`ExportRepositoryImpl.kt:284`
Every neighbouring table passes `updatedAtOf(id) ?: now`. `ExperimentDao` has
`createdAtOf` and no `updatedAtOf`, so there was nothing to call. The idempotence
test snapshots seven tables and experiments is not one of them, and `seed()` never
creates an experiment, so the fix needs a fixture too. Write-only column, nothing
user-visible changes; it is in the list because it breaks the D27 rule for the table
added after the fix and the test cannot see it.

**16. Deleting a plant leaves its photos on disk.** **(fixed, D47)** `PlantRepositoryImpl.kt:192`
`deletePlant` is a log line and a DAO call; `PhotoStore` is not injected, so no
filesystem work is possible there. `PhotoStore.deleteForPlant` has exactly one
reference in the repo, its own declaration. The Maintenance orphan sweep does recover
the space, so this leaks rather than loses.

**17. The override instrumentation stores a literal placeholder.** **(fixed, D47)**
`WeightViewModel.kt:128` and `WeighingViewModel.kt:170`
Both lines read `"${'$'}{s.name}->${'$'}{saved.name}"`, where `${'$'}` yields a single
dollar sign and the rest is ordinary text, so every override row records the constant
string instead of the two enum names. Verified with `cat -A`. Both call sites agree
with each other, which is why it looks fine at a glance. D34's instrumentation knows
a chip was overridden but not to what.

**18. A capped ETA hides its confidence on the dashboard.** **(fixed, D47)**
`DashboardScreen.kt:623`
`prediction.capped` is the first arm of the `when`, ahead of the confidence arms, so
a capped ETA from a low-confidence EWMA prior renders identically to one from a
high-confidence fit, while the weight screen keeps the "estimated from past cycles"
subtitle. Reachable on a slow prior over a freshly watered pot. Low because "More
than 2 weeks" implies no action either way; the cost is the disagreement between the
two screens.

**19. The survival-rate fixture is symmetric.** **(fixed, D47)** `StatsTest.kt:88`
Two given away and two dead, so 0.5 holds for either numerator. Changing the
numerator from `givenAway` to `died` left all 256 tests passing. One label on one
screen, and an asymmetric fixture is a one-line fix.

## Refuted, and why

Worth reading, because these are the shapes that look like findings and are not.

1. **Four uncalled repository methods plus an unused `Masthead` composable.** The
   reference counting was right but the asserted consequence was wrong: the Places
   screen unions three sources, so `LocationRepository.delete` would strip only the
   note row and a typo'd name would still be listed. Tidiness with no wrong output.

2. **Import reverts newer edits while keeping the local `updated_at`.** That is the
   D27 decision, stated in the handover and in the code comment: the backup does not
   carry `updated_at`, so last-write-wins is not implementable and upsert-by-id is
   the documented rule. The watering half was wrong on the code too, since stored
   anchors and the stored EWMA do not drive any prediction.

3. **`SettingsViewModel` seeds from defaults and corrects a frame later.** Real, but
   `MainViewModel` collects the same flow eagerly from app start, so DataStore's cache
   is warm before Settings can be reached. Acting on the seed would need a tap one
   frame after the screen appears, which is inside human reaction time.

4. **`observeDashboard` has no `flowOn`, so the transform runs on the main thread.**
   Every fact checked out. The transform is pure in-memory work, `absoluteFile` is
   path arithmetic with no disk touch, and at tens of plants it is well under a frame.
   No measurement of a dropped frame was offered, so it is an architecture preference.

5. **`PredictionEvaluationTest` wraps its only assertion in `isNotEmpty()`.** True,
   but the property is pinned unconditionally by `PredictionTest:168`, which asserts
   a hard no-late-prediction bound on the same decelerating tail. Emptying the fixture
   would need the noise floor raised past about 40 g/day.

## What this says about the codebase

Two things stand out. The first is that the recurring pattern named all session,
logic that is correct, tested, reads properly and is wired to nothing, accounts for
a third of the confirmed list: the snooze action, the exact-alarm switch, the two
notification channels, the location table in the backup, `deleteForPlant`,
`runSweepNow`. None of those are bugs in the sense of wrong arithmetic. They are
seams between a piece of logic and the thing that should call it.

The second is that four findings were proved by mutating a line and watching all 256
tests still pass. That is a different signal from the rest: the suite is large and it
is green, but on those four terms it is not actually holding the code in place. Three
of them are in the watering model, which is the part of the app that has no
ground-truth validation yet either.
