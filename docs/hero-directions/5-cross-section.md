# Direction 5: CROSS-SECTION

*One of five competing plans. Show the invisible thing.*
New generator: `tools/make-section-svg.py`.

## 1. THE IDEA

The whole hero is one terracotta pot cut in half and seen from the side, drawn as
a plate from a soil-science manual, and the only thing that happens is that the
water leaves: the top two centimetres pale out while the bottom four stay dark,
the finger arrives from above and stops in the part that is already empty, and a
kitchen scale under the section counts the grams going.

This fits because the product's one claim is that **the top of the pot is not the
pot**, and that is a claim about a hidden distribution. Every other direction can
only assert it with a caption. A section proves it, and then it earns the leap to
grams for free, because **the number on the scale is computed from the drawing:
integrate the water in the section and you get the weight, in the same frame,
with no cut.**

## 2. PALETTE

The hero runs in the **day register, on paper**. A section is a printed plate, so
the ground is paper and the ink is ink. That one decision gives the captions a
14.3:1 contrast floor without a scrim anywhere except two shots.

| Role | Hex | Notes |
|---|---|---|
| Ground | `#F5F0E2` | brand paper, full bleed |
| Ink: captions, outlines | `#22201A` | 14.3:1, measured |
| Hairline: ticks, rules, depth scale | `#C9C0A8` | brand hairline |
| Mono labels | `#5F5847` | 6.2:1 |
| Pot wall, section hatching | `#8C4F43` | terracotta capped. Sat 0.70 to 0.52 |
| **Soil, saturated** | `#2B2016` | sat 0.49. 14.0:1 against paper |
| **Soil, drained (stipple)** | `#C4B58F` | sat 0.27 |
| Root geometry | `#CBB285` | sat 0.34 |
| Root hairs | `#D8C49B` | sat 0.28, finer and paler than structural roots |
| Drainage gravel | `#9A9484` | neutral, so it does not read as soil |
| Scale body | `#B8B2A0` | flat, no steel highlight |
| Scale readout | `#C9C0A8` with ink digits | **not** a glowing LCD |
| **Trigger line and the live reading dot** | `#8C3A2A` | uncapped brand terracotta. **The only full-saturation thing on screen** |

**Moisture without blue.** Moisture is encoded as **value and stipple density**,
not hue, which is both on-brand and physically true: **wet soil is literally
darker than dry soil**. The soil body is one flat dark fill. Dryness is drawn *on
top* as a field of pale stipple squares, and the fraction turned on at a given
depth is the water that has left that depth. Soil travels `#2B2016` to roughly
`#A89B7C` as stipple accumulates, a 7x luminance change with no second hue and no
gradient element. House style says no gradients; **a stipple field whose density
varies is an engraving, not a gradient**, and it is the correct register for a
soil diagram anyway.

Two notes on the accent. First, warm `#C98A3A` **does not appear in the hero at
all**. The accent means one thing here and that thing is the trigger weight: the
dashed terracotta rule and the dot on the live reading. Second, the scale's
readout is deliberately not a glowing LCD. A glow would be the only light source
in a flat plate, it reads as a second accent, and it eats the budget.

**Accent area, measured.** Budget at 1440x900 is 1,166 px.

- trigger rule, 520 px long (the section's width, not the viewport's), 1.6 px, dashed so duty cycle 4/7: **475 px**
- `221 g WATERS HERE`, 17 chars of 11 px mono: **197 px**
- live reading dot: **36 px**
- total **708 px, 61 percent of budget.** A full-viewport-width trigger rule would be 1,317 px on its own, 118 percent of budget, **which is why the rule stops at the section's edge.**

Portrait's budget is 296 px, so there the rule drops to 1.3 px and the label goes
dim instead of terracotta: 251 px, 85 percent.

## 3. SHOT LIST

Track `--len: 900vh`. One camera layer, one SVG. Captions use `win: 0.055`.

**Beat changes, stated explicitly.** Beat 3 (the test is not free) moves from 0.42
to **0.19**, so it lands while the finger is actually in the soil rather than two
shots later. Beat 2 moves to 0.29 and is rewritten, because the section can show
the ratio 20:120 and the old wording could only assert it. Beats 1 and 4 keep
their text. Beat 5 keeps its text and moves to 0.86. **Three beats are added:** the
wet anchor at 0.41, the anchors and trigger at 0.62, and at 0.71 the one that is
this direction's entire argument.

| # | Scroll | In frame | Camera (what it rests on) | Animates in the section | Caption | Value floor |
|---|---|---|---|---|---|---|
| 1 | 0.00 to 0.10 | The top 30 mm of the cut face, filling the frame. **So tight that it reads as ground, not as a section.** Stipple already pale in the top 8 mm | holds `#band-top` at scale 4.1, drifting down 2vh | nothing yet; stipple at depletion 0.07 | **0.10** `Tuesday, 11pm` / "He pushes a finger into the pot. Dry on top, so out comes the watering can." | right margin on bare paper. 14.3:1. No scrim |
| 2 | 0.10 to 0.20 | Same framing. The finger enters from the top and stops at 20 mm | still on `#band-top` | finger descends along its own axis; soil dishes; two surface root hairs tear and drift; the hole's walls stipple pale faster than their surroundings | **0.19** `The test is not free` / "It tears the fine roots it is measuring, and leaves a hole that dries faster than the soil around it." | as above |
| 3 | 0.20 to 0.34 | **The pull-back.** The ground becomes a cut face. Hatched pot wall, 120 mm of soil, root mass in the lower two thirds, saucer, drainage gravel | `#band-top` to `#section-full`, log-space zoom out through 4.1x to 1.35x | depth ticks and mono labels fade in down the left, 0 to 120 mm; the 20 mm tick goes ink and gets a leader; the finger's hole stays and is now visibly a dimple in a column | **0.29** `Twenty of a hundred and twenty` / "The finger reached two centimetres. The pot is twelve deep, and the bottom half has been wet for nine days." | as above |
| 4 | 0.34 to 0.44 | Full section, the grams bar to its right, the scale beneath | rests on `#section-and-bar` | **time rewinds to the watering:** stipple clears top-down to zero, three drips leave the drainage hole into the saucer, the grams bar fills to its top, scale settles 298 g with one overshoot | **0.41** `The full mark` / "Weigh it an hour after watering and you have the top of the range. 298 grams, this pot, this soil." | as above |
| 5 | 0.44 to 0.57 | Same frame, held | **parks** and does not move | **triggered, not scrubbed.** Nine days run on their own clock: the drying front descends, the grams bar falls, the scale counts 298 to 218, a mono date steps `TUE 23 SEP` to `THU 2 OCT`, nine tick marks accumulate | **0.52** `Nine days, in grams` / "Soil does not evaporate. Roots and leaves move the water out, and it leaves as weight: 8.8 grams a day for this pot in this room." | as above |
| 6 | 0.57 to 0.65 | Same frame. The grams bar gains three rules: wet anchor at the top, dry anchor at the bottom, trigger between | small push to `#gauge`, section still legible at the left edge | the two anchors draw as hairline dashes with mono labels; then the trigger draws in terracotta, left to right, and the live dot lands on it | **0.62** `Where the line goes` / "Full at 298, wilting at 175. It waters at 221, which is a weight, not a weekday." | as above |
| 7 | 0.65 to 0.74 | Back to the full section. The finger returns from the top | `#gauge` out to `#section-full` | the finger descends to exactly 20 mm and stops; a hairline bracket closes around the top 20 mm with the figure `8%`; a second bracket spans 80 to 120 mm with `55%`; the old hole is still there, paler | **0.71** `What the finger sampled` / "At 221 grams the top two centimetres have 8 percent of their water left, while the bottom four still have 55. The finger only ever reaches the part that already dried." | as above |
| 8 | 0.74 to 0.84 | Four sections in a row, each at a different depletion, on one continuous scale line | `#section-full` out to `#four-pots` | `#quad` crossfades in over `#section-full`, and **the second pot in the quad is the same geometry with the same seed at the same place, so nothing moves across the dissolve.** Four readouts settle: 298, 251, 221, 283 | **0.80** `Four pots, one kitchen scale` / "He stopped trusting the finger and started writing the weights down." | as above |
| 9 | 0.84 to 0.92 | The quad recedes. One section and its grams bar. Then the section slides out left and the bar stays | pushes to `#chart-plate` | the three horizontal rules extend rightwards off the bar and across the empty plate, becoming a y axis with a wet anchor, a trigger and a floor | **0.86** `A pot loses weight as it dries.` / `Steadily enough to fit a line to.` large, centred | caption crosses the art. Paper wash overlay at 0.72, measured ink contrast **8.1:1** over the darkest soil |
| 10 | 0.92 to 1.00 | Nine readings walk in from the left along time. The plate is revealed to be inside the phone. Screen fills the viewport | `#chart-plate` to `#phone-plate` to `#screen-main` | the nine dots draw in sequence; the last is terracotta and sits 3 g under the trigger; the plate's hairlines resolve into the app chart's own dashes | **0.93** `Nine readings` / "The same quantity, plotted against time instead of depth." | wash 0.45 fading to 0 |

Worst caption-free gap is 0.01 of 900vh, which is nine hundred pixels short of
nine hundred. The rule is met by caption spacing, and no two captions are ever
both above 0.1 opacity.

## 4. THE ANIMATION

### The moisture field, which is also the scale reading

Depth is discretised into 120 slices of 1 mm. Slice *i* has capacity proportional
to the pot's half-width at that depth, normalised so capacities sum to 1. The pot
holds **R = 123 g of available water**: 298 g at container capacity, 175 g at the
dry anchor. Water below the dry anchor never leaves and is the flat dark base
fill, **which is why the base fill never animates. The drawing is not allowed to
imply that a wilting pot is bone dry.**

Drying is weighted by depth, `w_i = 0.18 + exp(-i/26)`, so the top millimetre
dries about 6x faster than the bottom and the deep soil still loses water because
roots drink there. Removal is proportional to `w_i * remaining_i`, so no slice goes
negative and the shortfall redistributes downward, producing a descending front
rather than a uniform fade.

That algorithm is stateful, and a scroll story must be a pure function of
progress. So **the generator runs the simulation offline** at 60 depletion steps,
collapses each profile to 16 depth bands, and emits a 61x16 table as a
`data-profiles` attribute, about 1 KB. At runtime it is an index and a lerp.
Exact, cheap, symmetric, and the artist and the engine cannot drift apart.

**Measured output, which is where the captions' numbers come from:**

| depletion | scale | 0 to 20 mm | 20 to 40 | 40 to 80 | 80 to 120 |
|---|---|---|---|---|---|
| 0.00 | 298.0 g | 100% | 100% | 100% | 100% |
| 0.10 | 285.7 g | 79% | 88% | 93% | 95% |
| 0.25 | 267.2 g | 52% | 69% | 81% | 86% |
| 0.50 | 236.5 g | 19% | 38% | 58% | 68% |
| **0.63 (trigger, 221 g)** | **220.1 g** | **8%** | **23%** | **43%** | **55%** |
| 1.00 (dry anchor) | 175.0 g | 0% | 0% | 0% | 0% |

The scale readout is `175 + 123 * sum(profile)`, rounded. **It is never hardcoded
per shot. The number on the scale is the integral of the picture, and that one
line of code is the argument for this whole direction.**

### Rendering the stipple so it is cheap

About 2,800 stipple marks on a jittered grid inside the soil polygon, each a
1.6-unit **square** (squares, not circles: stipple squares read as engraving and
halve the path data). Each gets a stable uniform `u` from a seeded LCG. Marks are
bucketed by **16 depth bands x 8 u-buckets = 128 groups**, each emitted as **one
path**. So 128 DOM nodes, not 2,800.

Per frame, for band *b* with dryness `d` and bucket centre `u_j`:

```
opacity = clamp((d - u_j) * 8 + 0.5, 0, 1)
```

128 attribute writes, under 0.4 ms. The soft threshold means marks fade in rather
than pop, and the visible fraction tracks `d` linearly.

### The roots, generated

A recursive branch generator, seeded per pot. Four primaries leave the stem base
at 61, 78, 96 and 112 degrees below horizontal, length 0.52 of height, width 5.0.
At each step: gravitropic bias of 0.22 toward +y plus angular jitter of ±14
degrees; children number 2 with probability `p_branch`, else 1, at ±18 to 42
degrees; length x 0.71; width x 0.64; stop at depth 7 or width below 0.55.

`p_branch` is 0.25 for the first two levels and **0.85 from level 3**, which is
what puts the root mass in the lower two thirds without hardcoding a density map.
**A branch whose next segment would cross the pot wall reflects its direction
instead**, so roots run down and coil along the wall, which is what a root-bound
pot actually looks like and is the detail that will make a plant owner trust the
drawing.

Terminals at depth 5 or more sprout 6 to 11 root hairs. Roughly 380 structural
segments and 900 hairs, hairs batched into one path per depth band.

Two extra near-horizontal laterals run just under the surface through the top
20 mm. **Those are the ones the finger tears, and they are there because surface
laterals are real, not because the shot needs a victim.**

### Reduced motion

No Lenis, the triggered nine-day run jumps to its end state on first entry, and
the stagger collapses to a single fade. The camera path is unchanged, **because
the camera is scroll, not animation.**

## 5. THE HANDOFF

The hinge is the **grams bar**, and it exists from shot 4 onward so that by the
time it has to carry the handoff the reader has already learned to read it.

Water in soil is a distribution, not a level, so the anchors are never drawn as
waterlines across the soil. **That would be a lie and a soil scientist would spot
it in a second.** Instead a 36-unit bar stands immediately right of the section,
exactly 1:1 in height with the 120 mm soil column, and its fill is the *sum* of
the stipple field. Distribution on the left, total on the right, and the anchor
lines live on the bar where they are honest.

1. **0.84 to 0.88.** The section slides out left. The bar does not move. Its three rules stay exactly where they are.
2. **0.88 to 0.92.** The rules extend rightwards across empty paper. The bar stops being a bar and becomes a y axis, **and the axis it becomes is already correct**, because the app's chart uses the same two labels at the same two weights. Mapping is linear and known: 298 g is chart y 17.7, 221 g is chart y 89.6, so 0.934 chart units per gram. The generator emits the bar at that exact ratio, so when the plate becomes the chart, **nothing has to move or rescale.**
3. **0.92 to 1.00.** The nine readings walk in from the left along a time axis that was not there before, and the real DOM crossfades in on the existing range.

The last dot lands at 218 g, 3 g under the trigger, which is exactly what the
walkthrough's first chapter then explains. **The sentence that was previously a
cut, "and now here is the app", becomes a geometric identity.**

**It also deletes a cut.** The current page crossfades between two separately
drawn SVG layers and has to make the pot land in the same place at the same size
in both. One section, one camera, means that problem does not exist. The only
dissolve left is the quad, and that one has an identical fixed point by
construction.

## 6. PORTRAIT

**Portrait is the native orientation for this direction and landscape is the
compromise.** Saying that plainly, because it is the one orientation claim in this
brief that is actually true rather than polite.

The subject's long axis is vertical and the quantity being explained varies along
it. At 390x844 the composition is a 300x700 stack, aspect 0.43, which nests inside
a 0.462 viewport with an 8 percent margin: depth scale, section, grams bar, scale
slab, and captions in the bottom 26 percent on bare paper at 14.3:1 with no scrim.

At 1440x900 that same composition leaves 73 percent of the frame as paper. Rather
than fight it, **landscape uses it as a plate**: the left third carries the depth
scale and a mono apparatus block (`AVAILABLE WATER 123 g · WET ANCHOR 298 g · DRY
ANCHOR 175 g · TRIGGER 221 g`), the right third carries the captions. The frame
becomes a page from a manual, which is the house style, and the captions never
touch the art.

Portrait differences: shots 1 and 2 use a tall band rect at a lower zoom so the
stipple stays crisp; **shot 8 drops to two pots**, because four sections at 0.46
aspect are 75 px wide each, below the width at which a stipple field reads; shots
9 and 10 use a narrower plate, and the app chart is 300x120 inside a 390-wide
phone, so **the fit is tighter than landscape and the match is better**.

## 7. COST AND RISK

**Hard, in order:** the front simulation plus the profile table (half a day, and
already prototyped, since the table above is real output); the root generator with
wall reflection (a day, and reflection off a tapered wall is fiddly and needs a
visual check at four seeds because a bad seed produces lightning rather than
roots); the stipple batching (half a day, low risk); the pull-back at shot 3
(technically trivial but it is the shot the whole direction rests on and will need
hand-tuning); the quad (a day, mostly placement).

**What could look bad.** Stipple reading as dirt or JPEG noise: the failure mode
is marks too small and too uniform, so specify a minimum rendered mark size of
1.4 px and **raise the mark size with the camera's zoom-out so density stays
constant in screen space rather than in SVG space.** The section reading as cold:
a plate has no weather in it, and this is a real risk. **The 20 mm bracket at shot
7 being too subtle:** the whole argument is in that shot, and if `8%` and `55%` do
not land, the page has drawn a diagram and proved nothing. The finger looking like
a diagram of a finger: it should be drawn **in section too**, cut off flat at the
top of frame with the same hatching convention as the pot wall. That is a strong,
odd, manual-like image if it works and an anatomy illustration if it does not.

**Cut order:** the quad first, which removes the hero's only dissolve. Then the
rewind at shot 4, which is elegant but the one place a reader could get confused
about which way the clock runs. Then the date stepper. Then the drainage gravel
and saucer drips, which are pure pleasure, first to go and last to be missed.

## 8. WHAT THIS DIRECTION IS BAD AT

Honestly: **it is an explanation, and explanations are not stories.**

A section has no room, no night, no 11pm, no Dheirav. The finger is the only human
presence in nine shots and it enters as an instrument rather than as a person. The
current page's best moment is that someone is standing in a dark kitchen at eleven
at night doing something slightly irrational to a plant, and I am giving that up
almost entirely to buy the proof. **If the instinct is that the page should make
someone recognise themselves before it teaches them anything, a different
direction is correct and this one should lose.**

- **A diagram can carry an argument, not an arc.** No reversal, nothing at stake. The reader goes from "I did not know that" to "now I do", a flat curve. The pull-back at shot 3 is the only genuine surprise, and after it fires, shots 5 through 8 are elaboration. The strongest number sits at 0.71 specifically to give the back half something to do, but it is a second peak, not a climax.
- **It cannot show the product's identity at all.** Offline, no account, no server, no streaks, no guilt: none of that is a physical fact about soil. Those live entirely in the prose below the hero. **This direction sells the method and says nothing about the politics, which is half of why this app exists.**
- **The plant is barely in it.** A cross-section is a picture of a pot. There is a stem and a few leaves above the cut line, so the page never once shows a plant looking well. For a plant-care product that is a genuine sacrifice, and the thing a reviewer would flag first.
- **A section is a learned convention.** Hatched walls and depth scales read instantly to anyone who has opened a textbook, and read as "chart, scroll past" to anyone who has not. Shots 1 and 2 are the defence, establishing the surface before revealing the cut, so the reader arrives at the convention by being moved rather than by being shown it. A good defence, not a guarantee.
- **It is the most expensive of the plausible directions to get wrong.** A stipple field that looks like noise, or roots that look like cracks, has no fallback. A scene-based direction degrades gracefully when the art is mediocre. **This one is either a beautiful plate or it is a mess, and there is no illustrator to rescue it.**
