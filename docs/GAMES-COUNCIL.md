# Council review: teaching plant care, against `docs/GAMES.md`

2026-10-17. Five lenses, run in parallel, each given only its own domain so they
could not converge on the same obvious complaint. Each was required to name at
least one claim the document made that it thought was wrong. 1,682 seconds,
roughly 680k tokens, no agent errors.

The domains were: learning-science evidence, the competitive and adjacent-product
landscape, this project's recorded anti-goals, code feasibility in this
repository, and the cost to the app's primary flow.

Every claim repeated below was checked before being written down. Two were
checked by running something rather than by reading. Where a lens was wrong, that
is recorded too.

## The verdict

**D3, the simulator, is dead.** Three lenses refused it on independent grounds and
the two that did not refuse it declined to support it. **D2, replaying the plants
already lost, is dead as specified** and survives only as two separate smaller
things. **D1, committing before the app answers, survives every lens**, but not in
the shape the document proposed and not in the place it proposed putting it.

| | Learning | Competitive | Anti-goals | Feasibility | Primary flow |
|---|---|---|---|---|---|
| D1 guess first | BUILD, two changes | BUILD | BUILD LATER, narrower | BUILD LATER, 2 to 4 days | DO NOT BUILD here, BUILD relocated |
| D2 replay the lost | BUILD LATER, restructured | BUILD LATER | DO NOT BUILD the counterfactual | BUILD, 3 to 5 days | BUILD |
| D3 simulator | DO NOT BUILD | DO NOT BUILD | DO NOT BUILD | BUILD LATER, 2 weeks | BUILD LATER |

## The five findings worth more than the rest

### The replay is blind to the death the feature exists for

Two defeats, found by two lenses that could not see each other's work.

`predictWatering` returns early with `PLANT_IS_GONE` when `plant.status.isGone`
(`Prediction.kt:81-83`), `assembleWeightState` reaches it with the caller's own
plant on every path (`WeightAssembly.kt:26`, `:76`, `:117`), and a sample is only
recorded when the prediction is an `Eta` (`PredictionEvaluation.kt:119`). Proven by
probe: identical readings and identical watering history give **5 samples when the
plant is `ACTIVE` and 0 when it is `DEAD` or `GIVEN_AWAY`**, 5 again when the
status is put back.

The second is worse and is not a bug. Ground truth is the pot crossing its
trigger, so a cycle watered before it gets there is skipped: `crossingTime`
returns null and the loop does `continue` (`PredictionEvaluation.kt:107`,
`:155-166`), with `PredictionEvaluationTest.kt:69` asserting zero samples for
exactly that case, on the stated and correct grounds that "a censored cycle is
missing data, not evidence of error". **Chronic overwatering is nothing but cycles
watered above the trigger.** So the replay is blind to the overwatering death, and
works on the neglect death, which is the guilt-maximal case the app least wants
to narrate.

### The same defect silently filters the number that justifies the method

The probe above is not only about D2. `StatsViewModel.kt:23-26` passes
`includeArchived = true` with a comment saying archived and dead plants are
included on purpose, because "leaving out what died would make the outcomes table
a lie", and `computeStats` hands every one of them to `evaluatePredictions`
(`Stats.kt:179-190`).

Every dead plant therefore contributes zero prediction samples, and the Figures
screen's prediction error is measured only over plants that are still alive. **That
is a survivorship filter on the one number that justifies weighing pots at all**,
sitting directly beneath a comment refusing a survivorship filter on the table
next to it. The test helper `plant()` takes no `status` parameter
(`TestSupport.kt:22-36`), which is why no test caught it.

This is unrelated to teaching and should be fixed whatever happens to D1, D2 and
D3. The fix wants a status-blind entry point rather than a loosened guard, because
`PLANT_IS_GONE` is deliberate and correct for prediction: a replay is a different
speech act from a forecast.

### D2 as written is the control condition from the study that supports D1

The document's own argument is that commitment before the answer is what teaches.
The one study that isolates that variable is Brod, Hasselhorn and Bunge (2018),
which compares prediction against **postdiction**, where the only difference is
whether the commitment comes before or after the answer appears. Verified from the
paper: the response was one tap on a five-point scale, "Far left: clearly the left
country, Left: probably the left country, Middle: don't know, Right: probably the
right country, Far right: clearly the right country". In the episodic task there
was a condition by expectancy interaction, F(1, 29) = 6.28, p = .018, with better
memory in the prediction condition for expectancy-violating outcomes
(t(29) = 2.34, p = .013) and **no benefit at all for outcomes that matched the
prediction** (p = .13). Pupil surprise correlated r = .34 with knowledge gain in
the prediction condition and r = -.007 in postdiction.

D2 shows a counterfactual about a plant whose death is already known. That is
postdiction: the control condition, which produced no surprise response and no
benefit. So D2 as specified has no learning mechanism behind it, by the same
evidence that supports D1.

### Three lenses independently demanded the same change, and none could see the others

The anti-goals lens, reading only this repository, concluded that D1 must contain
no verdict: commit the guess, then state two facts and let the gap speak, "You
said ready. It is 48 percent down, and the mark is at 60", never "correct" or
"not quite", because `HERO-NARRATIVE.md:162` makes the project's no-guilt claim
structural rather than characterological, and D1 as written introduces the app's
first container that can read "wrong" about the present user.

The competitive lens, reading Blood Glucose Awareness Training, concluded that the
instrument should be a signed error on a grid rather than a right-or-wrong, because
direction and magnitude are what a cue correction needs.

The learning lens, reading Brod (2021), concluded that a flat three-option tap is
the *guessing* format rather than the *predicting* format, since surprise is a
function of confidence in the wrong belief, and "incorrect guesses, which are by
definition provided with low confidence, are highly unlikely to elicit strong
surprise reactions".

Three domains, three routes, one answer: **no right-or-wrong, and carry magnitude
or confidence in the same tap.**

### D1's instrument already ships, and the app is already grading with it

The strongest single finding in the council. `CheckResult` is already a
three-option ordinal judgement about whether a pot wants water:
`STILL_HEAVY("Still heavy")`, `GETTING_LIGHT("Getting light")`,
`DRY_WATERED("Dry, so I watered")` (`Model.kt:62-67`). It is committed in one tap
from three separate surfaces: the dashboard row's "still wet" action
(`DashboardScreen.kt:586-594`), the Due list (`DueViewModel.kt:79-80`), and a
reminder notification action with no app launch at all
(`ReminderActionReceiver.kt:88`). The quick sheet even asks it in words, "How does
the pot feel?" (`DashboardScreen.kt:873`).

So the commitment exists, it is free, and it is collected at the plant with a hand
in the soil rather than at a scale. What is missing is only the comparison
afterwards, and the material for it is already in the same UI state, since
`PlantAttention` carries `depletion` and `prediction` (`Attention.kt:12-13`).

And the app is already grading that judgement without checking it:
`DashboardScreen.kt:352-356` announces "Good call" unconditionally, including for
a pot the scale puts past its trigger. **Making that line honest is a bug fix
wearing a feature's clothes**, and it is the whole of D1 at zero added taps.

Worth noting what the two lenses could not see about each other here: the existing
three options are an ordinal scale on the exact dimension being measured, which is
closer to what the error-grid argument wants than a confidence scale would be, but
they carry no confidence, which is what the prediction mechanism needs. Those are
reconcilable and the reconciliation is a design question, not a research one.

## Where the lenses agreed without being able to compare notes

**My anti-goals line was wrong in both directions, found twice.** The document said
the test was whether a record follows the user home. The anti-goals lens found the
app already ships a persisted accuracy figure beside the real plants, defended at
`StatsScreen.kt:188-192` as "the app grading itself, measured, not asserted, and
shown even when unflattering", while `DESIGN-COHERENCE-PLAN.md:544` refuses "0 due
today", which persists nothing. The competitive lens reached the same conclusion
from lichess, which computes a puzzle rating whose stated purpose is to pick
appropriate puzzles, lets the user hide all ratings because hiding "only affects
what you see", and ships the line "Note that puzzles are not a competition".

**The real test is who the subject is and whether the display can show a failure
value**, and the resolution of the stop-asking problem follows from it: keep a
private estimate whose only consumer is the asking rate, and never render it.

**D1 must not fire when the answer is obvious, found twice.** The learning lens
derived it from Brod's null for expectancy-consistent outcomes. The primary-flow
lens derived it from the code, since the page behind the sheet already announces
the surprising thing when `s.diagnostic != null` (`WeightScreen.kt:211-217`), so
asking after the answer has been given is the exact failure D1 exists to fix.

## The deepest unresolved tension, now named

The anti-goals lens says no container may read "wrong" about the user. The
learning lens says per-item outcome feedback is precisely the intervention that
improves discrimination while leaving calibration untouched, and that the only
thing anyone has shown to reduce overconfidence is the **aggregate**, your own
calibration curve over many judgements (Stone and Opel 2000, where substantive
knowledge feedback significantly *worsened* overconfidence).

So the version of D1 the anti-goals permit is, on the best available evidence, the
version that makes people better at reading pots and no better at judging their
own judgement, which is the thing the dead plant taught. The document presented
that line as costless. It is not. The competitive lens supplies the only exit:
persist the estimate, never display it, let it drive the asking rate. That
satisfies all three lenses and is the shape to build.

## Where a lens was wrong

**The competitive lens said Leafy Corner models root rot.** It does not. The
developer's own account says growth speed is affected by overwatering and
underwatering, that each plant type has its own watering needs, and that
"succulents and cacti need less water and are more sensitive to overwatering", and
mentions root rot only as a real-world risk. The lens's conclusion survives the
correction, because per-species overwatering sensitivity is D3's entire lesson.

**The competitive lens reported a Farmily arithmetic impossibility I did not
verify.** It states the control arm's intention standard deviations are
impossible on a three-item five-point scale. I did not check the paper's SD
table, so that one is recorded as unverified rather than confirmed.

**The learning lens gave the prequestion split as g = 0.54 against g = 0.04.** The
0.04 for non-pretested material is corroborated independently; the pretested
estimate is reported elsewhere as 0.58. The split is the decision-relevant part
and it is real either way.

## What the council found wrong in `docs/GAMES.md`

All eight are corrected in that document in place, with the correction recorded
rather than the error quietly removed.

1. **`SUGGESTION_OVERRIDDEN` holds no figures and has the app as its subject.**
   Named by three lenses independently and verified directly. Two emitters, both
   writing a `ReadingContext` enum pair. D1 has no existing data asset.
2. **`evaluatePredictions` cannot replay a dead plant, and is blind to the
   overwatering death.** Two independent defeats, one proven by probe.
3. **Planton is delisted.** `resultCount 0` from Apple's lookup API in the US and
   Indian storefronts, 404 on both store pages, last APK August 2024, with Planta
   as a live control returning 1. The teaching layer in plant apps is not crowded,
   it is empty, and the one attempt at it died.
4. **The anti-goals line was drawn at persistence, which is neither sufficient nor
   necessary.**
5. **"No transfer problem at all" is wrong twice.** The pretesting effect is
   g = 0.54 on the specific material pretested and g = 0.04 on related material
   that was not, so specificity is what makes the effect measurable on that pot
   and also the reason it may not reach the next one. And there is a transfer
   problem, from scale-assisted judgement to unaided judgement, which is the skill
   actually wanted.
6. **The facts-versus-self-belief split was assigned backwards.** Brod's outcome
   measures were memory for facts, so being confidently wrong is in the first
   instance a fact-learning device.
7. **"Drooping means thirsty" is not the app's own position.** The catalogue says a
   peace lily's drooping flat leaves mean thirst (`SpeciesCatalogue.kt:471`) and a
   fern wilting flat is thirst and "is not dying" (`:439`), against limp wrinkled
   succulent leaves meaning root loss (`:301`). The misconception is that the
   symptom reads the same on every plant.
8. **The weigh-frequency inference repeated an error its own commit corrects.**
   `93d2f5d`: "82 readings on this device and zero usage rows ... The instrument
   was measuring curiosity and I called it friction."

## What to do, in order

1. **Fix the survivorship filter in the prediction score.** Smallest change here,
   unrelated to teaching, and it repairs the app's own headline measurement.
2. **Make "Good call" honest.** The app currently praises a judgement it has not
   checked. This is a bug, and fixing it is D1's reveal at zero added taps.
3. **Then D1, relocated to the check, with no verdict in it**, carrying magnitude,
   not firing when the answer is obvious, with a private unrendered estimate
   driving the asking rate.
4. **The factual half of D2**, as record rather than counterfactual, after the
   `isGone` fix. Ask before revealing, or it is postdiction.
5. **Not D3.**

## A correction to the D3 refusal, found after the council closed

The competitive lens refused D3 partly on the grounds that Leafy Corner ships the
same lesson better. The owner pointed out what five lenses missed: **Leafy Corner
is a paid console and PC title at 9.99 USD and ThirstTrap is a free Android app**,
so the two audiences barely overlap, and overlap less again in India where paid
console and Steam gaming is a far smaller base than free Android. A better product
is only a substitute if the user would otherwise choose it, and most of this app's
users will never know that game exists.

The lens compared products and never checked audiences. It verified the release,
the platforms, the price and the mechanics, then reasoned as though those players
and these users were the same people. It also handed over the counter-evidence
without noticing: it reported that the standalone plant-game space is thin apart
from Leafy Corner and that the itch.io houseplant tag holds very little, so at the
free end, where these users are, the competition is a 2021 student thesis project
and two tiny releases.

**The refusal stands on its other four legs**, none of which mention a competitor:
the evidence on simulators for adult learners, the transfer problem that applies
at full strength only to a fictional pot, the rule-engine incoherence with the
app's own thesis, and the cost at 600 to 900 lines with an ungroundable damage
parameter. Plus the sharper version of the owner's own point, which cuts the other
way: the person who uses this app has already chosen a kitchen scale over a cozy
game, which is revealed preference for measurement over simulation from exactly
the population in question.

So D3 is refused because it would not work well and because the app already says
its one lesson thirteen times, not because somebody else got there first.

## What remains unknown

The honest list, after five lenses looked.

- **Whether any of this survives a week.** The pretesting delay evidence tops out
  at 48 hours, the prediction evidence has no delayed test at all, and the one RCT
  in this domain measured immediate effects only. Unanswered in the literature
  rather than merely unsearched.
- **Who opens an optional learning mode in a utility app.** Nothing in two
  research lenses improved on this. One inference does apply: learners who
  perceive a strategy as effortful rate it less effective even when it works
  better, which argues for on by default with an easy off rather than opt-in.
- **How readings split between the single-plant weigh screen and the round.** Both
  only started recording flows in `93d2f5d`. It decides whether D1 has occasions,
  because the round cannot show a prediction at all
  (`WeighingViewModel.kt:150-154`).
- **Whether a confidence-graded tap beats a flat one.** No study compares them.
  The council could not find one, which makes it a genuinely open question rather
  than a settled design choice.
