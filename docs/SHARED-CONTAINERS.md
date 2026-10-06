# Shared containers

Status: points 1 to 3 built 2026-10-06, schema v15. Point 4 still deferred.

Built as described below, with two departures worth recording. The fan-out
lives in its own `ContainerSharing` class rather than inside the repository,
because the weight backfill writes a watering too and two copies of the logic
would have drifted. And the grouping and naming the reminder worker needs were
pulled into `core/domain/Containers.kt`, because the worker is an Android class
that needs a device while the decision it makes needs nothing.

Verified on the phone (REDMI Note 15 Pro, 2026-10-07): 23 instrumented tests
pass, including the 6 ContainerSharingTest cases and the v14 to v15 migration.
302 JVM tests pass. See docs/DEVICE.md section 0 for how the tests were run,
because Gradle's own installer cannot reach this phone.

## The gap

Two plants can live in one pot or one jar, sharing a bowl, a water supply and a
climate, and the app treats them as unrelated. Every watering has to be logged
twice. Once both have a check reminder, the same jar nags you twice.

The terrarium is the case that exposed it: a Fittonia and a cutting in one jar.
The database does not actually record the container, `container_desc` being null
on both, so "jar" here is the owner's description and not something the app
knows. This is live, not hypothetical. The cutting was taken on the evening
of 2026-10-06: "Fittonia v2" was created at 21:58, watered at 22:02 and moved
through callusing to rooting at 22:04, with an "after the cutting" observation
on the parent at 21:58.

The double reminder is already scheduled. Both plants have an enabled CHECK
reminder, and they are not even aligned: the parent is next due Oct 7 07:52 and
the cutting Oct 13 22:02, because the cutting's interval counts from when it was
added. So the jar will ask to be assessed tomorrow morning and again a week
later, as two unrelated plants.

## A note on the watering, which I got wrong

An earlier version of this document argued that the jar was being overwatered,
that the near daily 10 ml was the cause of fogged glass and mould, and that the
reminder interval resolving to 1.00 days was the app amplifying a bad habit.
That was wrong, and it is recorded here rather than deleted because the way it
was wrong is the useful part.

The measurements were right: 21 waterings of 10 ml between Sep 6 and Oct 6, one
every 1.48 days, with the median gap tightening from 1.4 days in September to
1.0 in October. The conclusion drawn from them was not.

Three things in the same database contradict it, none of which were checked
before the advice was given:

- **`container_desc` is null.** Nothing in the record says this is a terrarium
  or a sealed jar at all. "Sealed jar" came from conversation, and the rule
  about sealed jars recycling their own water was then applied to it as though
  the database had said so.
- **The app's own species guidance for Fittonia says the opposite.** It is
  stored on the plant: "Keep consistently damp, never wet, never dry. It faints
  dramatically when thirsty and usually recovers within an hour of watering."
  A small daily watering is the correct regime for that, not a symptom.
- **No `DIED`, `PEST_OR_DISEASE`, `TREATED` or `PRUNED` event exists on either
  Fittonia**, across a month of records, and 11 photos. Nothing in the log
  reports a plant in trouble.

And the strongest evidence was in the event that prompted the whole question: a
cutting was taken from this plant on the evening of Oct 6. You propagate from a
plant that is doing well. The owner's "this has been working for a long time"
is a better signal than a general rule about jars, and the log agrees with it.

So the reminder resolving to a 1.00 day interval is not amplification, it is
`resolveIntervalDays` doing its job: with no weight evidence it fell through to
the logged average and learned the cadence that works. The only honest thing
left to say about that path is weaker and is not a defect: the log average can
ratify an existing cadence but can never contradict it, because it is computed
from the cadence itself. For a weighed pot the weight model supplies an
independent signal; for an unweighed plant there is none, so the interval
should be read as an echo of what the user already does rather than as evidence
about the plant. That is a limit worth knowing, not a thing to fix.

**What this changes for the feature below: nothing, except its priority.** If
the jar genuinely wants 10 ml a day and now holds two plants, that is a
duplicate log every single day, on the order of 700 a year. Shared containers
stops being a tidiness feature and becomes the main thing.

## The design

Group plants that live in the same container.

1. **Shared logging.** Log a watering or a check on any plant in the container
   and it is recorded on all of them with the same timestamp, so each plant's
   own timeline stays complete and readable on its own.
2. **One reminder per container.** The terrarium gets a single notification
   rather than one per plant.
3. **Taking a plant out ends the link.** Clear its container and it keeps every
   event it already has; from then on its watering is its own.
4. **Weight belongs to the container, not the plant.** A pot weighs as one
   object. Confirmed not to affect the terrarium: both Fittonias have
   `weight_tracked = 0` and neither has a single weight reading, while the two
   weighed plants, the Peperomia and the Creeping fig, are in their own pots.
   It would matter for a multi-plant pot that is weighed. Left out until such a
   pot exists.

Points 1 to 3 are the build. Point 4 is noted and deferred.

## What it actually touches

Checked against the code rather than from memory, and two things came out
differently from the first sketch.

**The schema is the easy part.** One nullable `container_id` on `plants`, which
is exactly the auto-migratable case, so v14 to v15 is one `AutoMigration` line
in `ThirstTrapDatabase.kt` alongside the other thirteen. Export carries it for
free, because `ExportRepositoryImpl` serialises the domain `Plant` and every
field on it has a default.

**The reminder merge is not a data-model change at all.** The first sketch said
"one reminder per container", which would have meant reworking `Reminder` and
its planning. It does not need that. `ReminderWorker.doWork()` already loops
over what is due and posts one notification per plant, so grouping that loop by
container is a handful of lines in one file and the reminder rows stay exactly
as they are, one per plant. They will also stay in step by themselves, because
`computeNextDue` measures from the last assessment in the log and shared
logging puts that event on every plant in the container.

**Fan-out has to be type-selective.** Only `WATERED`, `CHECKED` and `FERTILIZED`
belong to the container. `REPOTTED`, `PRUNED`, `OBSERVATION`, `MILESTONE`,
`MOVED`, `DIED` and the propagation stages are facts about one plant and must
not be copied, or the cutting's timeline fills with the Fittonia's history.

## The traps

These are the parts that would go wrong, and they are the same shape as the
defect this project keeps finding: changing one end of a path and not walking
the other.

**There are five write paths for a care event, not one.** `logEvent` is the
obvious one and covers all eight of its callers including the notification
action, but `eventDao.insert` is also called directly in four more places:
`upsertPlant` writes `medium_changed`, `setPropagationStage` writes the stage
move, and `WeightRepositoryImpl.addReading` backfills an inferred `WATERED`
when a post-water weighing implies a watering nobody logged. The first two are
per-plant and correctly skip the fan-out. **The third is a real watering and
must fan out**, and it is the one that would be missed, because it is in a
different file from the feature.

**`deleteEvent` and `updateEvent` need the same fan-out.** Log a watering on the
terrarium, correct the amount, and without this you have one corrected event
and one stale copy. Deleting leaves an orphan on the other plant. The copies
need a shared group id so the set can be found again, which means the fan-out
cannot just be "insert N rows and forget".

**Weight double-counts.** `weightTracked` is per plant and defaults true, so two
weight-tracked plants sharing a container would each build a model of the same
pot. Either refuse to let more than one plant in a container be weight-tracked,
or leave it and accept it until point 4 is built. Refusing is three lines and
honest; leaving it is a trap for later.

## The UI, which the first draft left out entirely

Everything above is plumbing, and plumbing with no tap is one of this project's
recurring defects: logic that is correct, tested, reads properly and is wired to
nothing. There has to be a way to say "these two share a jar".

- **Edit plant** gains one control: which container this plant is in. The
  options are "its own" plus every container already in use, named by the
  plants in it, since there is no separate container record to name. That is
  one field on a 554 line screen that already has a dozen.
- **Plant detail** says so, quietly. A line near the container description
  reading "shares a pot with Fittonia", tappable to get there. Without this the
  shared waterings appearing on a plant you did not touch look like a bug.
- **Logging** says so too, at the moment it matters: when a watering will be
  written to more than one plant, the sheet says which. Silently writing rows
  to a plant the user did not select is the kind of surprise that makes someone
  distrust the log.

Removing a plant from its container is the same control set back to "its own",
which is why rule 3 needs no separate screen.

## Tests

- Migration v14 to v15 in `:core:data` instrumented tests, alongside the
  existing ones.
- Fan-out writes to every plant in the container, with the same timestamp.
- Fan-out does not fire for the per-plant event types.
- Edit and delete reach every copy.
- A plant removed from a container keeps its history and stops receiving.
- The worker posts one notification for a container with two due plants.
- The weight backfill path fans out.

## Size

Bigger than the swipe feature, which was the first estimate and was wrong.

Swipe was 123 lines across 5 files, one new domain file and its test, plus one
defect found later on the device: `detectTransformGestures` consumes a
one-finger drag whether or not there is anything to pan, so the pager never saw
a swipe. It touched no schema file, needed no migration and no instrumented
test, and added no new control to any screen.

This needs all of those. A column and a migration with its schema json and an
instrumented migration test, a fan-out with a group id threaded through five
write paths in three files, edit and delete fan-out, a change in the reminder
worker, and three UI touches. Call it two to three times swipe. The reminder
merge really is small; the group id and the UI are what the first estimate
missed, and the UI was missing because the first draft had no UI section at
all.
