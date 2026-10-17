# Teaching plant care in the app, and whether a game is the way to do it

2026-10-17. Asked whether ThirstTrap could teach plant-care skills through
games, what the existing landscape looks like, and whether the gap is worth
filling. This is the research and the decisions that came out of it. Nothing
here is built.

Every claim about this codebase below was checked against the code before being
written down. The external evidence is cited, and where it is thin that is said
rather than smoothed over.

## 0. The reframing, which is the useful part

The question started as "can we make games that teach plant care". It moved,
because of one observation: *I learnt a lot more from my plant dying than from
reading what to do, but the cost is a plant.*

Taking that seriously changes the design. **The death is not what teaches. Being
wrong is what teaches, and the death is only the receipt.**

Nobody learns a new fact from a dead plant. You already knew overwatering kills
things, it is on every care label and on [every gardening site in the
world](https://marylandgrows.umd.edu/2021/01/22/top-10-ways-to-kill-a-houseplant/).
What the death updates is your belief about your own judgement, on that pot, in
that corner, in that month, at a moment when you were confident you were right.

**The clean split that followed was drawn backwards and is corrected here.** The
draft said facts and self-belief are separate axes and a dead plant hits only the
second. In the mechanism this document is reaching for, the outcome measured was
memory for facts: country populations and football scorelines, recalled better
when the learner had committed first and been surprised. Hypercorrection is also
about facts, where confidently held errors get corrected more often than
hesitant ones. So being confidently wrong is in the first instance a fact-learning
device, and the reason a dead plant feels different is that the fact it teaches is
about your own conditions rather than about plants in general.

This matters because it decides what D1 should measure. If the mechanism is better
encoding of the revealed number, the test is whether unaided estimates converge on
the scale over weeks, not whether the user reports feeling more confident.

So the design goal is not "make a game". It is **manufacture being wrong,
cheaply, with the mechanism visible afterwards.** A simulator is one way to do
that and it turns out to be the weakest of the three available here.

## 1. What already exists

### Standalone games

There are more than expected, and one is aimed at exactly this.

- [Houseplant Care Simulator](https://kriskevel.itch.io/houseplant-care-simulator),
  a University of Tartu student project, built explicitly to teach plant care.
  You read an in-game encyclopedia and then apply it by managing light and
  watering. Free, Windows, listed as in development.
- [Plant Parent](https://trialbyfiddleleaffig.itch.io/plant-parent/purchase).
  The first draft called this quiz-style and about rescuing an ailing plant. It is
  a released cozy pixel narrative piece about a character's first plant, with no
  stated educational goal and no quiz framing anywhere on its page.
- [Leafy Corner](https://gamedev.net/news/4838-how-real-houseplant-horticulture-inspired-leafy-corner/),
  commercial, says its systems come from real horticulture and that growth
  responds to over-watering, under-watering and pot size, but the care itself is
  light and it is a cozy management game rather than a sim.
- [Viridi](https://gamingtrend.com/reviews/plant-it-in-the-cold-cold-ground-viridi-review/),
  the famous one, teaches nothing. One spray lasts about two days and reviewers
  say the only variable that matters is how damp the soil is.

### Teaching inside a plant-care app

**This section said the space was taken. It is not, and the app it named is
dead.** The first draft cited Planton, a free plant-care app in the App Store's
Education category selling short courses with interactive lessons, games, quizzes
and puzzles, plus quiz tournaments and achievements, and concluded that teaching
inside a plant app was "taken, and taken in the form this project would refuse".

Planton is delisted. Apple's own lookup API returns `resultCount 0` for it in both
the US and Indian storefronts, both its App Store and Play pages return 404, and
the last APK anyone mirrors is dated August 2024. Planta, checked as a live
control, returns 1. The document's only source for it was an aggregator page that
still reproduces the old listing.

A sweep of roughly 600 App Store listings across 18 query terms found the
surviving alternatives are a 19-rating botany quiz and a 16-rating herbalism app,
and **no plant app at all that asks the user to commit to a judgement about their
own plant before revealing an answer.** The same sweep found no plant app shipping
weight-based watering, including the 114,000-rating Planta and the
1.1-million-rating PictureThis.

So the teaching layer in plant apps is not crowded. It is empty, and the one real
attempt at it died.

### The mainstream apps

None of them teach, and one of them is explicitly built not to.

- [Planta](https://gardencentermag.com/news/how-this-swedish-plant-care-app-planta-can-help-plant-parents-at-any-stage)
  has care guides, a plant doctor and a knowledge base, which is reference
  material rather than instruction.
- [Greg](https://fueled.com/blog/greg-helps-earn-green-thumb/) infers watering
  from other users growing the same plant in similar light, and states its
  mission as making people "fluent in plants" by making care easy. That is the
  opposite move: it removes the need to understand anything.
- Blossom adjusts reminders from the local weather forecast.

## 2. Whether games teach this at all

One proper trial exists and it is close to the question.
[Farmily](https://pmc.ncbi.nlm.nih.gov/articles/PMC11346082/) is a persuasive
simulation game for novice home gardeners, tested as a randomised controlled
trial with 50 participants against an expert-led lecture workshop of the same
90-minute length.

| Outcome | Game, pre to post | Workshop, pre to post | Between groups |
|---|---|---|---|
| Knowledge, of 23 | 10.88 to 14.08 | 11.12 to 16.12 | p = .09, not significant |
| Hard questions only | 4.76 | 5.76 | p = .005, workshop better |
| Attitude, 1 to 5 | 3.68 to 4.48 | 4.32 to 4.27 | p = .006 |
| Self-efficacy | 3.48 to 4.23 | 3.78 to 3.69 | p = .03 |
| Intention to start | 1.89 to 3.91 | 3.01 to 2.83 | p < .001 |

The fair reading is that the game matched a lecture on facts, lost on the hard
material, and moved attitude, self-efficacy and intention where the lecture
moved none of them and slightly went backwards on all three.

Caveats, as the authors state them: 50 people, a single 90-minute session,
participants who were university students and staff reporting high prior gaming
experience, a knowledge questionnaire built for the study, and **no retention or
follow-up measurement at all**. The broader [systematic review of 237 papers on
serious games in agriculture](https://www.sciencedirect.com/science/article/pii/S0308521X24003287)
finds the same shape, where games come out roughly level with conventional
instruction on near transfer and durable far transfer is thin.

**What this implies for this app.** A game is not a better way to deliver facts
than the prose already shipping. The Almanac and the Help pages are the lecture,
and on this evidence they are doing the knowledge job at least as well as a game
would. What a game buys that prose cannot is the willingness to act, which is
also the thing that is actually missing: people do not lack information about
overwatering, they lack the confidence to leave a drooping plant alone when
overwatering and underwatering look identical.

It also matches the original observation exactly. A dead plant teaches no facts
and transforms self-belief, which is the same axis the game won on and the
lecture lost on.

## 3. The two real gaps

The first draft said "nobody teaches plant care in an app" was false because
Planton did. Planton is delisted, so the plain claim stands: nobody teaches plant
care in an app. Two further things are true, and the second is narrower than the
draft claimed.

**Nothing teaches by measurement.** Every game above teaches a rule: read that
pothos likes to dry out, then apply it. None has a scale, so none can teach the
reading of a number. This app's whole thesis is that the rule is unknowable per
pot while the measurement is not, so a game teaching rules would contradict the
app it sits inside, and one teaching curve-reading teaches the only thing the app
claims.

**Nothing closes the loop.** A standalone game teaches in a sandbox and then
leaves the player to transfer it to their own windowsill unaided, which is the
transfer problem the literature is weakest on. An app holding both the real
plants and the exercise can use identical vocabulary in both, so "pot staying
heavy" means the same sentence in the lesson and on a real plant's page three
weeks later. No itch.io game and no Planton course can copy that, and it is worth
more than the game design itself.

## 4. The curriculum, and what each lesson actually costs

| The belief a death corrects | Cheapest honest delivery |
|---|---|
| Drooping always means thirsty | Guess-first on a real pot that is heavy and drooping |
| Dry topsoil means a dry pot | Guess-first: guess from the surface, then weigh |
| Every Sunday is a reasonable schedule | The pot's own summer and winter drying rates, already segmented |
| More water is more care | Simulator, because this must not be staged on a real plant |

**The first row was wrong in the first draft and is narrowed here.** It said
"drooping means thirsty" was the misconception, and the app's own catalogue says
otherwise: drooping *is* thirst for a peace lily (`SpeciesCatalogue.kt:471`, "it
perks up within a few hours") and for a fern (`:439`, "it does this often and it
is not dying"), while limp wrinkled leaves on a succulent mean root loss from
overwatering (`:301`). The misconception is that the symptom reads the same way on
every plant, not that drooping never means thirst. An exercise built on the
original wording would have taught a worse rule than the one already shipping.

Three of the four need no game. They need the app to stop answering for one tap.
Only the last needs a sandbox, because a slow overwatering death takes two
months and the entire point is not to cause one.

## 5. The three proposals

### D1. Guess before the app answers

The app currently prevents this learning by accident: it gives the answer before
the user has committed to one. You weigh the pot, it says 62 percent depleted and
due Thursday, and because you were never on the hook you cannot have been wrong,
so nothing updates.

Reverse the order. Before the reading is shown, ask whether this pot wants water.
One tap, three options, then reveal the number.

This is a plant dying without the plant: committed, wrong, mechanism visible in
grams, about the user's own pot in their own room.

**The claim that followed, that there is therefore no transfer problem at all, was
wrong twice and is withdrawn.** The pretesting effect is roughly g = 0.54 on the
specific material that was pretested and g = 0.04 on related material that was
not, across 91 comparisons. D1 pretests "does this pot want water today", while
section 4's curriculum wants general rules spanning pots, rooms and months, and
the general effect is estimated at approximately zero. So specificity is what
makes the effect measurable on that pot and also the reason it may not reach the
next one.

And there is a transfer problem, the obvious one: every occasion D1 creates is
cued by an app screen that then hands over the answer in grams, while the skill
section 0 actually wants is judging a pot you have not weighed. Nothing in this
literature establishes transfer from instrument-cued judgement to unaided
judgement.

One tap is enough, though, and that was the right call for the wrong reason. The
study that isolates this variable used a single tap on a five-point scale, so the
format objection does not apply. But it has to be five points and not three:
surprise is a function of confidence in the wrong belief, so a flat three-option
tap is the guessing format rather than the predicting one.

**What exists already: nothing, and this claim was wrong.** The first draft said
`Usage.kt` holds the before and after of every figure the user changed, amounting
to a log of their judgement disagreeing with the measurement. Both halves are
false and the sign is inverted.

There are two emitters of `SUGGESTION_OVERRIDDEN` in the repo,
`WeightViewModel.kt:134-142` and `WeighingViewModel.kt:190-199`, and both write a
`ReadingContext` enum pair such as `"ROUTINE->PRE_WATER"`. `Usage.kt:46`
constrains the field to "Enum names only, never user text" and `Usage.kt:8-13`
excludes the rest: "Actions only: no notes, no amounts, no grams, no photo
paths." So no figure is stored anywhere, and the disagreement recorded is about
*what kind of moment this is*, whether a weigh is routine or pre-water, not about
whether the pot is thirsty.

The inversion matters more than the error. `Usage.kt:31-32` says a high override
rate "means the suggestion logic is wrong for how this person actually works", so
the one existing record of user-against-app disagreement has the app as its
subject. D1 has no data asset behind it, and the file it was supposed to come from
was written specifically to exclude the class of data D1 wants.

**Cost.** Small. A pre-reveal state in `WeightViewModel`, three buttons, and a
comparison line. No new domain code.

### D2. Replay the plants already lost

`PostMortem.kt` exists and its docstring makes the argument before this document
did: *"the answer is usually already in the log, and nobody reads a log
unprompted. Killing a plant is the moment you are most willing to look, and the
least likely to remember what happened two months ago."* So the app already
catches the teachable moment, and what it hands over is a recap of what happened.

What it does not hand over is the counterfactual. `evaluatePredictions` replays
the model over a weight history using only the data the model held at each point,
refusing explicitly to grade itself with hindsight anchors, and the first draft
said it could therefore produce the counterfactual today. **It cannot, for two
independent reasons, and the second one is worse than the first.**

*It returns nothing for a plant marked dead.* `predictWatering` returns early with
`PLANT_IS_GONE` when `plant.status.isGone` (`Prediction.kt:81-83`),
`assembleWeightState` reaches it with the caller's own plant on every path
(`WeightAssembly.kt:26`, `:76`, `:117`), and a sample is only recorded when the
prediction is an `Eta` (`PredictionEvaluation.kt:119`). Proven by probe: identical
readings and identical watering history give 5 samples when the plant is `ACTIVE`
and 0 when it is `DEAD` or `GIVEN_AWAY`, 5 again when the status is put back.

*It returns nothing for the death this feature is for.* Ground truth is the moment
the series crosses the trigger, and a segment watered before the pot ever gets
there is skipped: `crossingTime` returns null and the loop does `continue`
(`PredictionEvaluation.kt:107`, `:155-166`). The docstring states the rule as a
virtue, "a censored cycle is missing data, not evidence of error" (`:76-77`), and
`PredictionEvaluationTest.kt:69` asserts exactly zero samples for it. **Chronic
overwatering is nothing but cycles watered above the trigger.** So the replay is
blind to the overwatering death, which section 4 calls the one lesson worth a
simulator, and it works on the neglect death, which is the guilt-maximal case and
the one the app least wants to narrate.

Two of the three sentences D2 wanted are unsupported anyway. `PredictionSample`
carries no timestamp (`PredictionEvaluation.kt:18-39`), so "what the app would
have said on day 9" needs a new field, and per-segment drying rates are folded
into an EWMA and discarded (`WeightAssembly.kt:108-114`), so "the rate halved
three weeks before any leaf changed" has no data structure behind it at all.

The tuition for those lessons is already paid. The receipts are in the database,
unread.

**Cost.** Small to medium. The replay and the recap both exist; this is a screen
and a careful set of sentences.

**Constraint.** It stays descriptive. `buildPostMortemObservations` already holds
that line in its own docstring: the record can say a plant was watered every
three days, it cannot say that is what killed it. "The app would have told you to
wait" is a fact about the model. "You killed it" is not a fact at all, and it is
the exact thing this app exists not to say.

### D3. A pot you cannot kill

A simulated plant, time stepping a day at a time, water or wait, watching the
weight curve and the plant respond. Water it every third day and the curve
flattens before the plant looks ill, which is the sequence people miss in life.

Reduced in scope by section 4: it carries **one** lesson, that more water is not
more care, because that is the only lesson that cannot be delivered on a real
plant without killing it.

**Cost.** The largest of the three, and it is not a reuse job. The app's model
runs backwards from what a sim needs: `SlopeFit` and `Segmentation` fit a drying
rate out of readings already taken, while a simulator has to generate readings
out of a plant's state. That forward model is new domain code, call it 150 lines
with its own tests, because a sim that dries a pot wrongly teaches the wrong
thing, which is worse than teaching nothing.

The inputs for it exist, which is what makes it tractable:
`SpeciesCare.depletionTrigger` and `medium` across 164 catalogue entries,
`classifyLux` bands for light, `AmbientSummary` for temperature and humidity, and
`Anchors` for the pot's range. The reveal can run the real `predictWatering` over
the synthetic readings, so it teaches the actual model rather than a cartoon of
it.

## 6. What this is not

Dropped on the evidence rather than on taste:

- **A quiz or course layer.** Still dropped, but not for the reason the draft
  gave. It said the market was crowded and Planton owned it, and Planton is
  delisted. The reasons that survive are the evidence, since a lecture beat the
  game on exactly this material and the app's own prose is already the lecture,
  and reuse, since the diagnosis trees and the catalogue already carry the
  content.
- **Symptom-diagnosis as a game.** Good content, but `feature/diagnose` already
  ships four decision trees that encode which questions discriminate, and 164
  catalogue entries carry `commonProblems`. A quiz over material the app already
  presents better is a reformatting, not a feature.
- **Triage as a game.** Teaches the reasoning the Due tab already performs. The
  lesson is real but not load-bearing.
- **A dilution drill.** `Fertilizer.kt` already does the arithmetic and already
  refuses doses too small to pour. A five-minute addition, not a workstream.

## 7. The anti-goals line, drawn where it actually matters

The recorded non-goal is "No gamification or streaks", with the reason attached:
*a missed day must not feel like failure* (`docs/FEATURES.md:188`). The refusal
table at `docs/DESIGN-COHERENCE-PLAN.md:539` extends it down to refusing a count
on the Due tab, because a counter that can read zero is a score with one value.

A game as a teaching medium is not what that non-goal is about. But the first
draft drew the line in the wrong place, and the codebase proves it wrong in both
directions.

It said the test was **whether the record follows the user home**, with a session
readout allowed and a persisted figure refused. The app already ships a persisted
accuracy figure that lives next to the real plants: "Typical prediction error" in
days on the Figures screen, defended in its own docstring as "the app grading
itself, measured, not asserted, and shown even when unflattering, because a
tracker that hides its own error rate is asking to be trusted on vibes"
(`StatsScreen.kt:188-192`). It also ships a survival percentage about outcomes the
user is responsible for (`:168-180`). Meanwhile
`docs/DESIGN-COHERENCE-PLAN.md:544` refuses "0 due today", which is transient and
derived and persists nothing. So persistence is neither sufficient nor necessary.

**The test this repository actually applies is who the subject is, and whether the
display can show a failure value.** The prediction error ships because the subject
is the app. The survival rate ships because it is null until something has left,
since "100% would invite watching it fall" (`StatsTest.kt:68-75`). The empty due
count is refused because the subject is the user and zero is a value they can
fail to beat. `HERO-NARRATIVE.md:162` states it as the project's own structural
test: there is no guilt because "the almanac layout has no container that can sit
empty, so there is nothing for a reader to fail at", and structural claims are
checkable where character claims are not.

By that test D1 as originally written fails, because it introduces the app's first
container that can read "wrong" about the present user, and it fails in the
instant rather than on the ledger. What survives the test is a version with no
verdict in it: commit the guess, then state two facts and let the gap speak. "You
said ready. It is 48 percent down, and the mark is at 60." Never "correct", never
"not quite". That is the grammar `DashboardScreen.kt:703-705` already uses, where
what matters is whether the pot has passed the mark.

There is also a shipped-copy constraint that outranks the argument.
`IntroScreen.kt:112-115` promises, on the first run and re-readable from Help:
"It is a diary, not a scoreboard. No streaks, no counts of what you missed, no
tone of voice about it." An untotalled guess survives "no counts". It does not
survive "no tone of voice" if the reveal evaluates the person.

## 8. Two honest problems

**D1 cannot invent occasions, and the first draft got the reason wrong.** It
said the friction report found `weigh` recording nothing, and inferred that people
may not weigh often enough for the lesson to fire. That inference is the exact
error `93d2f5d` was written to correct, and that commit message says so: "82
readings on this device and zero usage rows ... The instrument was measuring
curiosity and I called it friction." Zero rows meant the instrument was absent,
not the behaviour.

The real open question is narrower and answerable. The readings split between the
single-plant weigh screen and the weighing round is unknown, because both only
started recording flows in `93d2f5d`. It matters because
`WeighingViewModel.kt:150-154` declines to assemble a weight state per plant in
the round, on the stated grounds that it would mean doing it "for every plant on
every emission". No prediction in the round means nothing to reveal, so D1 cannot
be shown there at all. If the round carries most readings, D1's only available
surface is the minority one.

**A pre-reveal tap is a cost on the primary utility.** The weight screen's job is
to answer a question quickly. Putting a question in front of the answer makes the
app slower at the thing it is for, every single time, in exchange for a lesson
that only lands the first several times. Any version of D1 needs a way to stop
asking, and the decision of when to stop asking is the actual design problem.

## 9. What could not be established

Stated so it is not mistaken for a gap in the reasoning:

- **Who opens an optional learning mode in a utility app.** Searched for and not
  found. This is the main commercial risk and no number is guessed here.
- **Whether any of this survives a week.** The Farmily trial measured immediate
  effects only, and the agriculture review says durable far transfer is thin
  across the literature.
- ~~**Whether the commit-then-reveal structure has direct support.**~~
  **Withdrawn: it does, and this was the document's best-evidenced point stated as
  its weakest.** Brod, Hasselhorn and Bunge (2018) compare prediction against
  postdiction, where the only difference is whether the one-tap commitment comes
  before or after the answer. Verified from the paper: the benefit appears only for
  outcomes that violated the prediction (t(29) = 2.34, p = .013) and is absent for
  outcomes that matched it (p = .13), with pupil surprise correlating r = .34 with
  knowledge gain under prediction against r = -.007 under postdiction. Separately,
  Blood Glucose Awareness Training has taught this exact structure since the 1980s,
  committing to an estimate of an invisible internal quantity from perceptible
  cues, then measuring, then comparing the signed error on a grid, with randomised
  trials behind it and a web version evaluated in Diabetes Care.
- **What remains genuinely unknown is retention.** The pretesting delay evidence
  tops out at 48 hours and the prediction evidence has no delayed test at all, so
  "whether any of this survives a week" is unanswered in the literature rather
  than merely unsearched.

## 10. Recommendation, after the council

Five lenses reviewed this document on 2026-10-17. The record is
`docs/GAMES-COUNCIL.md`, and the recommendation below is theirs rather than the
draft's, which had D1 first for the wrong reasons, D2 second on a claim that was
false, and D3 last when it should not be on the list.

**1. Fix the survivorship filter in the prediction score.** Nothing to do with
teaching. Every dead plant contributes zero prediction samples, so the Figures
screen's model error is measured only over plants still alive, directly beneath a
comment refusing exactly that filter on the table beside it. Smallest change in
the set and it repairs the app's headline measurement.

**2. Make "Good call" honest.** `DashboardScreen.kt:352-356` praises a judgement
the app has not checked, including for a pot the scale puts past its trigger. That
is a bug, and fixing it is D1's reveal with no taps added anywhere.

**3. Then D1, relocated to the check.** Not at the scale. `CheckResult` is already
a one-tap ordinal judgement about whether a pot wants water
(`Model.kt:62-67`), committed from the dashboard row, the Due list and a
notification action, at the plant with a hand in the soil. The commitment exists
and is free; only the comparison is missing. Constraints, each demanded
independently by a different lens: no verdict in the reveal, two facts and a mark
instead; magnitude or confidence carried in the tap, because flat options are the
guessing format; no asking when the answer is obvious, because a confirmed
prediction teaches nothing; and a private estimate that drives the asking rate and
is never rendered, which is how lichess resolves the same tension.

**4. The factual half of D2, after the `isGone` fix.** Segment slopes, dates, where
the trigger crossing fell. No counterfactual: it would make shipped first-run copy
false, it fails `PostMortem.kt:23-26`'s own indicative test, and showing it after
the outcome is known is postdiction, which is the control condition that taught
nothing. If a counterfactual is ever wanted, ask the user to commit before
revealing it, at which point it is D1 again on occasions already paid for.

**5. Not D3.** Three lenses refused it independently: the evidence puts simulation
games at d = 0.28 declarative and 0.22 retention for adults and favours them as
repeatable supplements rather than one-lesson sandboxes; the catalogue already
teaches "more water is not more care" thirteen times against species the user
owns; it is a rule engine inside an app whose thesis is that the per-pot rule is
unknowable; and the forward model is 600 to 900 lines rather than 150, with its
one load-bearing parameter, the damage curve, having nothing in the repository to
ground it.

**One leg of that refusal does not hold and is withdrawn.** The competitive lens
also argued that Leafy Corner ships the lesson better, having launched on four
platforms in July 2026 with per-species overwatering sensitivity. But it is a paid
title at 9.99 USD while this is a free Android app, so the audiences barely
overlap, and the lens compared products without ever checking users. At the free
end the competition is a 2021 student thesis project and two tiny releases. The
refusal survives on the four reasons above, which is a different argument from
"someone else did it better" and the only one of the two that was ever true.

Build none of the quiz layer.

## Sources

- [Farmily RCT, JMIR Serious Games 2024](https://pmc.ncbi.nlm.nih.gov/articles/PMC11346082/)
- [Systematic review of serious games in agriculture, Agricultural Systems 2024](https://www.sciencedirect.com/science/article/pii/S0308521X24003287)
- [Houseplant Care Simulator](https://kriskevel.itch.io/houseplant-care-simulator)
- [Plant Parent](https://trialbyfiddleleaffig.itch.io/plant-parent/purchase)
- [Leafy Corner and real horticulture](https://gamedev.net/news/4838-how-real-houseplant-horticulture-inspired-leafy-corner/)
- [Viridi review](https://gamingtrend.com/reviews/plant-it-in-the-cold-cold-ground-viridi-review/)
- [Planton listing](https://similarweb.com/app/apple/6477597907), **delisted,
  kept only to show where the original claim came from**
- [Brod, Hasselhorn and Bunge 2018, prediction against postdiction](https://escholarship.org/content/qt0qn99739/qt0qn99739.pdf)
- [St. Hilaire et al., prequestion meta-analysis](https://link.springer.com/article/10.3758/s13423-023-02353-8)
- [Stone and Opel 2000, calibration against discrimination](https://home.csulb.edu/~cwallis/382/certainty/overconfidence/Training%20to%20Improve%20Calibration.pdf)
- [BGAT delivered over the internet, Diabetes Care 2008](https://diabetesjournals.org/care/article/31/8/1527/28459/)
- [Leafy Corner, the developer on real horticulture](https://news.xbox.com/en-us/2026/07/31/how-real-houseplant-horticulture-inspired-leafy-corner/)
- [Planta features](https://gardencentermag.com/news/how-this-swedish-plant-care-app-planta-can-help-plant-parents-at-any-stage)
- [Greg features](https://fueled.com/blog/greg-helps-earn-green-thumb/)
- [Top ways to kill a houseplant, University of Maryland Extension](https://marylandgrows.umd.edu/2021/01/22/top-10-ways-to-kill-a-houseplant/)
