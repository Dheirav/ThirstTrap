# The hero narrative, before any art

Written deliberately before choosing a direction, so the story does not get
shaped by whichever picture is easiest to draw. Every beat below should work
whether the art ends up a night interior, a botanical plate, a cross-section or
a chart. If a beat only works in one of those, it is a picture pretending to be
a story and it should be cut.

---

## 1. What the current five beats get wrong

The committed page runs:

| at | kicker | line |
|---|---|---|
| 0.14 | Tuesday, 11pm | Dheirav pushes a finger into the soil. Dry. He reaches for the watering can. |
| 0.32 | The top three centimetres | They dry first. Underneath, it has been wet for nine days. |
| 0.50 | Root rot | More houseplants are killed by kindness than by drought. |
| 0.68 | Four pots, one kitchen scale | He stopped trusting his finger and started writing the weights down. |
| 0.83 | | A pot loses weight as it dries. Steadily enough to fit a line to. |

### A. Nothing is lost, so nothing is at stake

The story is: a man pokes soil, then weighs instead. No plant dies, no money
goes, nobody is wrong about anything that mattered. Compare the opener the
council measured as the reference site's best line: *"Three teams wrote the same
code review skill this quarter. None of them knew."* Four words name the cost,
and the cost is ignorance rather than waste.

Our beat 1 ends *"Dry. He reaches for the watering can."* That is not a cost, it
is the next action. A reader who leaves at beat 1 has lost nothing, because
nothing was lost in the story either.

### B. Half the product has no beat, which is the biggest fault

The app's three most distinctive things are that it **refuses to answer** in
eight named situations, that it has **no streaks and no guilt**, and that the
diary is **yours, offline, in a zip you keep**.

The five beats set up exactly one of those: weigh instead of poke. So the
refusal chapter of the walkthrough, the anti-goals section and the backup
chapter are all answering a question the story never asked. The reader has no
slot to put them in.

This is precisely the fault the council found at the reference site, where the
story was about duplication and half the product was telemetry. It is cheap to
fix and it needs two more beats.

### C. Three kickers repeat their own line

*"The top three centimetres"* then *"They dry first"*: the line's subject is the
kicker. *"Root rot"* then *"killed by kindness than by drought"*: the same idea
twice. A kicker is a free slot for a second piece of information, and we are
spending it on an echo.

### D. The one real stake is sitting in the data, unused

The plant list in the walkthrough reads **"Flax seeds, this one has gone"**, and
the model has a refusal reason literally called `PLANT_IS_GONE`. A plant in the
real diary died. The hero never mentions it.

That is the opening. It is true, it is specific, and it is the only thing on the
page with a consequence in it.

### E. What is already right, and must survive

**The order.** The turn comes before the thesis and the thesis before the
walkthrough. The reference site resolved before it demonstrated, so its product
tour had nothing at stake. Ours does not make that mistake. Keep it.

**The register.** Plain verbs, no marketing abstractions, short second
sentences. That is working.

---

## 2. The proposed beat sheet

Eight beats, in three movements: two habits, two reasons the second one fails,
then the change and what kind of thing the app turns out to be.

The arc is **every day, then by feel, then by weight**. Each stage is less wrong
than the last, so a reader can join at whichever one they are actually standing
on.

| # | at | kicker | line |
|---|---|---|---|
| 1 | 0.09 | `Every evening` | He watered all four, because that is what looking after something feels like. It is also how you drown one. |
| 2 | 0.20 | `Then he started checking` | Finger in the soil, water only the ones that are dry. That was a real improvement. |
| 3 | 0.31 | `Two centimetres down` | The finger reaches an inch. The pot is twelve deep, and the bottom has been wet for nine days. |
| 4 | 0.41 | `And the test is not free` | Each push tears the fine roots it is measuring. |
| 5 | 0.54 | `One kitchen scale` | Water has weight. He stopped asking the soil and started writing the number down. |
| 6 | 0.67 | `Nine days, eighty grams` | A pot loses weight as it dries. Steadily enough to fit a line to. **(large)** |
| 7 | 0.79 | `And when it cannot tell` | It says so, in eight different ways, each one naming what would fix it. |
| 8 | 0.90 | `It keeps no score` | A missed day is a day when nothing needed doing. Nobody is counting. |

Then the close, below the walkthrough rather than in the hero:

> **He used to water four pots every evening.**
> Now he waters the one that asks.

Same subject, opposite behaviour, closing the loop beat 1 opened. The ask stops
being "adopt a measurement discipline" and becomes "do less", which is a far
smaller thing to say yes to.

### Why each beat is there

**1 is the real habit, and the real cost.** Not a dead plant: the flax cup died
because he cut it, which he knew at the time, so a mystery death would have been
a lie. The true stake is better anyway. He watered four pots **every evening**,
and daily watering is not a small error, it is the error. The second sentence
does the work: *it is also how you drown one*. The cost is named without anybody
being told off, because the subject is him.

The clause "that is what looking after something feels like" is the whole
emotional logic of overwatering in eight words. People do not overwater from
carelessness, they overwater from attention. That is why "killed by kindness"
was the right idea in the old beat 3 and the wrong place for it: there it was a
statistic about houseplants, and here it is a thing he did.

**2 is the concession, and it must stay a concession.** Checking the soil really
is better than a daily schedule, and the beat says so plainly: *that was a real
improvement*. Four words, and they are sincere. A page that only tells you what
you are doing wrong is a page you argue with; one that grants you the step you
already took has somewhere to lead you next. Beats 3 and 4 then undercut it on
evidence rather than on attitude.

This also means **the finger is no longer the opening**, which it should never
have been. It is the second habit, not the first, and it is the one the reader
is most likely to be standing on right now.

**3 is the physical fact**, and kicker and line now do different work. The
kicker names the depth, the line names the ratio. An inch against twelve
centimetres is the whole argument in one comparison.

**4 is the cost of the test**, kept to one clause because the length drop is
what makes it land. The old version ran on into "and leaves a hole that dries
faster", which is a second idea competing with the first.

**5 is the turn.** "Stopped asking the soil" names the mechanism where "stopped
trusting his finger" named a feeling. Three verbs the reader will perform: weigh
it, write it down, let the pot answer.

**6 is the thesis**, now with a number in the kicker so the big line is not the
first time a quantity appears.

**7 is new and it is load-bearing.** It gives the refusal chapter a slot. Without
it, the most distinctive thing the app does arrives as a surprise feature rather
than as an answer to something the story set up. "Eight different ways" is a
real count: `NOT_CALIBRATED`, `NEEDS_RECALIBRATION`, `NO_READINGS`,
`ONE_READING_NO_HISTORY`, `NO_MEASURABLE_DRYING`, `WEIGHT_MEANINGLESS_FOR_MEDIUM`,
`WATERED_SINCE_LAST_READING`, `PLANT_IS_GONE`.

**8 is new and it answers the objection the turn creates.** The moment a reader
hears "start writing the number down", the instinct is *that sounds like a chore
I will fall behind on*. Every habit app they have ever deleted taught them that.
Leaving it unanswered until a prose section eight viewports later is leaving it
unanswered.

Three things make the beat work rather than just assert:

- **It names a mechanism, not a promise.** "Nothing here can be full, or empty" is the reason there is no guilt: the almanac layout has no container that can sit empty, so there is nothing for a reader to fail at. A page that says "we would never shame you" is making a character claim. This one is making a structural one, and structural claims are checkable.
- **The length drop carries it.** Nine words, then three. "Nobody is counting" lands as a verdict rather than a feature.
- **Those three words are true twice over.** There is no streak counting, and there is also no server counting, because the app is offline and has no account. One clause does the anti-goals and the privacy claim at once, which is why it earns its place this late in the hero rather than needing two beats.

It goes last, after the refusals, because 7 and 8 are a pair: 7 is the app being
honest about what it knows, 8 is the app being honest about you. Ending on the
reader rather than on the product is also the right handover into a walkthrough
that is about to show them using it.

### Spacing

Beats sit at 0.09, 0.20, 0.31, 0.41, 0.54, 0.67, 0.79, 0.90. Largest gap is
0.13, which at a 900vh track is about 1.2 viewports, so with a 0.09 fade window
either side no stretch is ever text-free for more than a fraction of a screen.
The handoff still has 0.90 to 0.965 clear.

---

## 3. One sentence that complicates the pitch

The council found that the reference site's most trustworthy prose was the one
sentence that admitted a distinction rather than simplifying: a page that
complicates its own claim buys credit for everything near it.

Ours should be, somewhere in the prose below the hero:

> Weighing only works if the pot is the only thing that changed. Repot it, move
> it to a colder room, or leave the saucer full, and the line you fitted is
> about a pot that no longer exists. The app starts again rather than carrying
> the old slope forward, which is why it sometimes has nothing to say.

That is true, it is the actual behaviour of the segmentation code, and it turns
a limitation into evidence of care.

---

## 4. One verifiable dare

The reference site's best single device was a claim the reader could check from
where they were standing. Ours could be:

> Every number on this page came out of the diary on my phone. Sixteenth to the
> twenty-fifth of September, 298 grams down to 218, 8.8 grams a day. The export
> is a zip and the format is in the repo.

It costs nothing, it is checkable, and it is the opposite of a seeded demo
dashboard.

---

## 5. What I need from you before writing final copy

Four things I will not invent, because the opener rests on them:

All four are answered, and one of them changed the opening.

1. **The flax cup died because he cut it**, and he knew that at the time. So it is not the opening: a loss you can explain proves nothing. It stays where it already is, in the walkthrough's plant list and behind the `PLANT_IS_GONE` refusal, which is the honest place for it.
2. **The habit was daily watering, then checking the soil by hand.** Both real, both now in the beat sheet, and the progression between them is the spine of the story. **The lede must lose "guessing on a Sunday"**: it was never true, and a real habit is sitting right there.
3. **Named, third person, past habit.** Dheirav, and "used to", because the loop closes on him having changed rather than on the reader being wrong.
4. **Four.** Beat 1 and the close both count them.

---

## 6. Decisions this does not make

This is the story, not the picture. It deliberately says nothing about whether
the page is dark or cream, drawn or rendered, a room or a plate. The five art
directions in `hero-directions/` all remain available, and three of them would
carry these beats without changing a word.

What the beat sheet does constrain: any direction must be able to show **a pot
that is gone** (beat 1), **a depth comparison** (beat 3), and **a refusal**
(beat 7). The last one is the hardest and none of the five plans currently
handles it, which is worth knowing before picking one.

---

## 7. Shot transitions

Section 6 said this document was the story and not the picture. That is no
longer true: the picture is five generated plates in `tools/hero-gen/out`, so
the joins between them are now a narrative decision and belong here.

### What the plates measure

Two numbers decide every join.

**Light direction alternates.** The brightest-pixel centroid sits at x=0.03 in
`shelf-evening`, 0.95 in `finger-test`, 0.05 in `section`, 0.84 in
`scale-table` and 0.42 in `ledger`. The lamp swaps sides between consecutive
shots, which kills any join meant to be invisible unless a plate is flipped.

**Three are dark grounds and one is a light ground.** Near-black covers 49.6%
of `shelf-evening`, 45.7% of `finger-test`, 34.8% of `scale-table` and 34.0%
of `ledger`, but only 10.0% of `section`. Dark cannot be crossfaded into light, because the midpoint
of the dissolve is a grey wash across the whole screen and it announces itself.

So the plan is not to make everything continuous. Observal gets its continuity
from one camera flying one spline through one modelled room, where the dome and
the desk and the laptop all coexist. Five separate rasters cannot do that, and
pretending otherwise produces mush. The plan is to decide which joins hide and
which ones declare themselves, and to let the beat decide which is which.

### The joins

| scroll | join | device |
|---|---|---|
| 0.00 to 0.09 | open on `ledger` | pull back from the sheet to the room, no cut |
| 0.09 | `ledger` to `shelf-evening` | dissolve aligned on the moon |
| 0.20 | `shelf-evening` to `finger-test` | invisible match on one pot, hidden in the dark lower third |
| 0.31 | `finger-test` to `section` | push into the soil until it fills frame, then open the section out of the black with a radial mask |
| 0.41 | inside `section` | no join, push in on the root zone |
| 0.54 | `section` to `scale-table` | declared cut: hold the pot silhouette, swap the world under it |
| 0.67 to 0.90 | inside `scale-table` | no new art, the phone screen carries beats 6, 7 and 8 |

**The ledger opens the page.** It shares the window, the moon, the city and the
sill with `shelf-evening`, so the two are the same room and the camera can move
between them freely. Starting tight on the sheet and pulling back also gives
the viewer something to read while they are still deciding whether to keep
scrolling, which a wide establishing shot does not. Align the dissolve on the
moon, because it appears in both frames and the eye tracks it, so a move from
the wall to the sill reads as one pan rather than as two pictures.

The moon makes this join solvable arithmetically instead of by eye, which is
the same thing Observal does with its match cut. Measured, the ledger's moon
sits at x=0.905, y=0.121 and is 0.0586 of the frame wide; shelf-evening's sits
at x=0.729, y=0.109 and is 0.0658 wide.

Equalising those two directly does not work, and it is worth writing down why,
because it is the trap in every match cut built from separate images. Scaling
the ledger by 1.123x makes the moons the same size, but it also puts the frame's
right edge 245 px outside the picture, because the ledger's moon is much nearer
its own right edge. **Both crops have to lie inside their own images**, and that
second constraint is what actually picks the framing. Writing the moon's
position as a fraction of the frame, each plate admits a range of framings, and
the two ranges only overlap once both plates are pushed in past about 1.3x. At a
moon width of 9% of frame the ledger's admissible range is 0.854 to 1.0 and
shelf-evening's is 0.629 to 0.998, so they meet. The join is therefore
**ledger at 1.54x and shelf-evening at 1.37x**, moon at 0.88 across and 0.14
down in both, which is what the keyframes now say.

In portrait there is no moon match at all. A 16:9 plate cropped to a phone's
9:19.5 keeps about a quarter of its width, so matching the moon would mean a
318 px crop, and the join is a plain dissolve between two compatible framings
instead.

**`shelf-evening` into `finger-test` cannot be a size match**, which is the
other thing measuring caught. In the row shot a pot is 11.5% of the frame; in
the close-up it is 43%. Matching them would be a 5.8x push and the plate holds
about 1.6x. So this join is a dissolve hidden in the dark lower third rather
than an object match: the pot stays near the centre of frame across the cut and
the sizes jump by about 2.4x, which darkness absorbs and a clean dissolve would
not. It still needs `finger-test` flipped horizontally first so its lamp moves
to the same side. The cost is that the hand becomes a left hand, which
nobody reads as wrong. Flip only this plate: standardising the light across all
five would be tidier and would buy nothing, because every other join is
supposed to be felt. Then push from the row of four to one pot, landing so that
pot's rim sits in the same box as `finger-test`'s, and cross over while the
bottom third of the frame is 80% near-black going out and 68% coming in.
Darkness is where you hide a cut.

**`finger-test` into `section` is the hard one**, and it is the one place worth
spending a mask rather than an opacity fade. Keep pushing past the fingertip
until the soil fills the frame, where it is near-uniform and carries no detail
that can mismatch, then grow the section out of that black with a radial mask
centred on where the fingertip was. The subject never leaves the middle of the
screen, so it reads as cutting that pot open rather than as a new picture
arriving, and the dark-to-light problem never comes up because nothing
dissolves.

**`section` into `scale-table` should stay visible on purpose.** The beat
changes its mind there: he stops interrogating the soil and starts measuring
the pot. Hiding that join would waste it. Mask the section down to the pot
silhouette alone, swap the ground under it from paper to the dark table while
the silhouette holds still, then pull back to reveal the scale and the phone.
Continuous on the object and discontinuous on the world, which is exactly what
the sentence is doing. The light flipping from left to right across this cut
now helps instead of hurting.

**After 0.67 there is no new art.** Push to the phone in `scale-table` and let
the screen content change instead of the camera. That also fixes what is wrong
with that plate, which is that its readout is unreadable: give the screen
something worth reading. And it makes the 0.90 handoff to the live demo
invisible, because the hero's last frame is already the demo's first view at
the same size and position.

### Two constraints on building it

**Alignment is solved by measuring, not composed by eye.** Every join above
depends on a shared object landing in the same box in both frames. Hand
computed offsets were tried once already and only matched at one window size,
because `scale` keyframes zoom about a layer's top left rather than its centre.
Measure the pot's bounding box in each plate and solve the transform, the way
the engine's existing `fit` stops do.

**Push-ins are limited to about 1.6x.** The plates are 1672x941 against a
1440x900 viewport, which is roughly 1.05x before leaving native resolution.
They are painted with no fine detail, so they tolerate being upsampled further
than a photograph would, but past about 1.6x they go soft. Every move listed
above is inside that. Anything tighter needs its own plate.
