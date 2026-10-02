# Direction 4: INSTRUMENT

*One of five competing plans. The data is the hero.*

## 1. THE IDEA

The hero is one sheet of squared paper holding one real drying cycle: ten
weighings of one pot over nine days, falling from 298 g to 218 g, with the line
fitted in front of the reader and the prediction crossing the trigger weight at a
named evening. Scrolling is not a camera flying through a bedroom, it is the
sheet filling up with evidence, and the last frame of the hero is literally the
app's own chart screen.

This fits ThirstTrap specifically because the product's whole claim is that
measurement beats intuition. **A page that opens on a cosy illustrated room is
arguing the opposite case in its own voice**: it says "trust the vibe of a plant
on a windowsill" for eight viewports and then asks you to put a pot on a scale.
An illustration can only assert that weighing works, while a plot of the actual
readings proves it, and the proof is already in the repo. The anti-goals follow
for free: a page that is calm, numeric and unemotional cannot accidentally gamify
anything, because there is nothing on it that can be full, empty, or a streak.

## 2. PALETTE AND TYPE

| Token | Hex | Role | Contrast on paper |
|---|---|---|---|
| paper | `#F5F0E2` | the sheet, every caption plate, the page background | base |
| ink | `#22201A` | all prose, all numerals, the fitted line, the axis frame | 14.3:1 |
| dim | `#5F5847` | tick labels, units, and everything that is *not measured* | 6.2:1 |
| hairline | `#C9C0A8` | the grid, droplines, rules. Deliberately at 1.6:1 so it reads as paper texture and carries no information | 1.6:1 |
| leaf | `#2F5D3A` | measured readings: the ten dots, the running list in the gutter | 5.9:1 |
| terracotta | `#8C3A2A` | the trigger weight line and label, and the one reading below it | 6.7:1 |
| warm accent | `#C98A3A` | exactly one thing: the predicted crossing | 2.6:1, so never used for type |

**The accent rule.** Warm appears once in the entire hero, at S6, as a 9 px disc
on the point where the extrapolated line meets the trigger line, plus a 2 px
underline beneath its date tag. About 494 px² against a budget of 1166 px². It
means one thing only: *this is a claim about the future*. Everything measured is
leaf or ink; everything predicted is warm. Because warm measures 2.6:1 on paper
it fails even the non-text threshold, so the disc is drawn as a warm fill with a
1.25 px ink ring: **the edge carries the contrast, the fill carries the
meaning**, and the date beside it is ink.

Terracotta is a data colour, not an accent, and is capped separately. Build
`tools/check-accent-area.py` to rasterise each shot at both viewports and count
pixels within ΔE 3 of each token, failing the build if warm exceeds 0.09 percent.
**The rule was established by measurement, so it should be enforced by
measurement.**

**Type.** Three bands with a real hole in the middle.

- Mono with tabular figures: 11 px at 0.18em uppercase for labels; 13 px for ticks and units; 15 px for the gutter list and the arithmetic block.
- Body, Hanken Grotesk: 17 px desktop, 16 px portrait. Band top is 19 px.
- Display, Newsreader 400 and 600: 34 px for sub-captions, 54 px for captions, `clamp(2.2rem, 5vw, 4.5rem)` for the one big line.

Body tops out at 19 px and display starts at 34 px, a **1.79x empty gap**. The
split is also the direction's emotional engine: **serif says why, mono says how
much.** Every human sentence is Newsreader, every number is mono, and they never
swap.

**Numeric register.** Tabular figures everywhere; numbers right-aligned in a
fixed gutter column so digits line up vertically down the page the way a logbook
does; units in dim at 0.78em following the value; the minus sign is U+2212 and
never a hyphen; dates in the diary's own format. **Nothing on this page is ever a
rounded, friendly number.**

## 3. SHOT LIST

One track, `--len: 940vh`. A single generated SVG sheet with two layout groups,
landscape and portrait, and named empty rects as fit targets.

The series: R01 16 Sept 298 g (just watered), R02 289, R03 277, R04 266, R05 257,
R06 251, R07 240, R08 23 Sept 233, R09 24 Sept 226, R10 25 Sept 218. Trigger
221 g. Fitted slope 8.8 g/day.

**The holdout is the spine of the hero.** The line is fitted on R01 to R08 only,
predicts the crossing, and then R09 and R10 land and confirm it. Label that on
screen; it is a retrodiction on real data and saying so is the whole point.

| # | Scroll | On screen | Camera | Animates | Caption | Value floor |
|---|---|---|---|---|---|---|
| S1 | 0.00 to 0.08 | Empty sheet: hairline grid, axis frame, g scale 210 to 300 by tens, date axis 16 to 26 Sept, header `PEPEROMIA · POT 3 · EAST SILL · SEPT 2026`. Hero title overlays left | rests on `#sheet-all` | grid draws itself left to right, 600 ms, on first paint. Nothing scrubs | h1 + lede | title on paper, 14.3:1. **No scrim anywhere on this page** |
| S2 | 0.08 to 0.18 | A mono scale readout panel top right, then R01 landing with hairline droplines to each axis | fit `#readout`, hold, ease out to `#plot-head` | readout settles like a real scale: 301, 297, 298. Then R01's dot lands with a 1.3x ink-settle overshoot | `READING 01` / "One number off a kitchen scale, written down. That is the entire input." | opaque paper plate, 1 px hairline top rule, 24 px padding |
| S3 | 0.18 to 0.34 | R02 to R08 landing one at a time; date ticks appear; the gutter builds a right-aligned mono list | pulls back to `#plot` | one dot per 0.02 of scroll, each a discrete 160 ms ink-settle, triggered | "Eight weighings over nine days, on a kitchen scale that cost less than the plant." | plate |
| S4 | 0.34 to 0.46 | **The pencil of slopes:** 28 faint chords, one per pair, then the fold to the median, then the ink line. Label `THEIL-SEN · MEDIAN OF 28 PAIRWISE SLOPES` and a narrowing bracket | holds `#plot`, slow push to 1.15 | see section 4. The centrepiece | "Every pair of readings proposes a slope, and the middle one wins, which is why a wet saucer left under the pot cannot bend the line." | plate |
| S5 | 0.46 to 0.58 | The terracotta trigger line draws in at 221 g. The gutter runs the arithmetic as four mono lines: `233 g now`, `− 221 g trigger`, `= 12 g left`, `12 ÷ 8.8 = 1.4 days` | fit `#plot-lower` | trigger line draws right to left; the four arithmetic lines land one per 0.02 scroll | "The prediction is a division. There is no plant database in it, and no averages from somebody else's windowsill." | plate |
| S6 | 0.58 to 0.68 | The fit line extends past R08 as a dashed ray and meets the trigger. The warm disc lands on the crossing with an ink ring, and a mono tag types `24 Sept, evening` | fit `#cross`, the tightest shot | dashed ray 400 ms; disc lands with a single 1.4x settle; tag types at 28 ms a character | **big centred:** "It names an evening, and shows you the arithmetic that produced it." | a full-width paper band with 1 px rules above and below |
| S7 | 0.68 to 0.76 | **The holdout lands:** R09 at 226, then R10 at 218 in terracotta, below the trigger. A hairline bracket measures predicted against measured, in hours | pulls back to `#plot` | two triggered dot-settles; the bracket draws last | `FITTED ON THE FIRST EIGHT · THE LAST TWO WERE NOT SHOWN TO THE LINE` / "It said the 24th. The pot crossed on the 24th." | plate |
| S8 | 0.76 to 0.84 | The same axes redrawn as the finger test: the y axis collapses to two bands, "feels dry" and "feels wet", and a dim hatched step flips to dry on day 4 while the weight's crossing stays at day 8.6. The five-day gap is hatched | fit `#finger-panel`, which occupies the rect the plot did | the axis collapse is **scrubbed** (spatial); the step draws triggered. Everything dim and hatched, labelled `NOT MEASURED · WHAT THE FINGER REPORTS` | "The top two centimetres dry first, so the finger says yes about five days early. And the test is not free: each push tears the fine roots it is measuring, and leaves a hole that dries faster." | plate |
| S9 | 0.84 to 0.92 | **The sheet inverts to ink.** The plot shrinks to a hairline ghost and the prediction field reads "no date yet". Below it, the seven refusals as a numbered ledger in the app's own wording | fit `#ledger` | the plate inverts by scrubbed opacity; the seven rows land one per 0.009 of scroll | "Seven times it will refuse to answer rather than name a day it cannot support, and each refusal says what would fix it." | paper type on ink, 14.3:1. **The only dark frame on the page and the only one with no data on it, which is the argument** |
| S10 | 0.92 to 1.00 | Back to paper. The sheet resolves into a pixel-exact reconstruction of the app's `answer` screen | fit `#fit-handoff` | the sheet's furniture fades so only the app column remains; demo stage crossfades 0.972 to 0.996 | mono, small: "and this is that chart, in the app." | plate until 0.97 |

**Beats 1 to 5 are rewritten wholesale.** The finger argument survives, demoted
to one comparison panel at S8 where it belongs, because in this direction it is
the counter-example and not the opening. Dheirav as a character is cut from the
hero entirely and returns in the walkthrough as the cursor, which is the right
place for him: **he is the operator of the instrument, not its subject.**

**The CTA rule is satisfied structurally**, not by periodic plates. The sheet has
a fixed footer rail, 36 px, hairline top rule, mono: `Get the APK · Read the code
· Apache-2.0, offline, no account`. On screen for all 940vh. An instrument sheet
has a footer; this is the one direction where a permanent CTA does not look like
a sales banner.

## 4. THE ANIMATION

| Element | Property | Mechanism | Range |
|---|---|---|---|
| camera | transform | scrubbed, fit stops with dwell pairs | 0 to 1 |
| axis collapse at S8 | y positions of the two bands | **scrubbed**, it is a spatial remap of the same axes | 0.76 to 0.80 |
| dark plate at S9 | opacity of the ink rect | scrubbed | 0.84 to 0.86 |
| every dot landing | opacity plus a 1.3x radius overshoot | triggered, one per reading, with `onLeaveBack` reset | its own fraction |
| scale readout | text content | triggered, 700 ms | 0.10 |
| the fit | see below | triggered timeline, 2.2 s | 0.36 |
| ledger rows | opacity plus 8 px rise | triggered, one per row | 0.855 to 0.915 |

All easing lives in the camera path, which `parallax.js` already smoothsteps per
segment; the track mapping stays linear and no trigger carries an ease.

**How the line gets fitted, specifically.** A polyline drawn left to right with a
dash offset is the generic chart animation and we are not doing it. Theil-Sen is
the median of all pairwise slopes, so **the animation is the algorithm**:

1. Draw all 28 chords, one through each pair of the eight readings, extended to the plot's edges, at 0.6 px and opacity 0.10. They form a visible pencil of lines crossing near the centroid. Steeper and flatter chords are plainly different, which is the information.
2. Rank the 28 by slope. Fade and rotate them out **inward from both ends of the ranking simultaneously**, 24 ms apart, each rotating about the median point as it leaves. The viewer watches the extremes get discarded first. **That is what taking a median looks like, and nothing else on the web animates this way.**
3. As the fan narrows, a mono label narrows with it: "between −11.9 and −6.4 g/day", then "between −10.4 and −7.6", then "−8.8 g/day". Compute the real bracket values; do not fake the sequence.
4. The last surviving chord thickens from 0.6 px to 1.8 px and becomes the fit. **It does not get redrawn; it is one of the 28, promoted.**

## 5. THE HANDOFF

The hero ends on the app's chart screen because the hero *is* that chart. One
Python script emits both the hero sheet and the `answer` template's chart from the
same readings through the same projection function, so the geometry on both sides
of the cut is identical to the pixel.

Mechanics: `#fit-handoff` is an empty rect that JS sizes on resize to the
viewport's aspect ratio. **Register that resize handler before calling
`keyframed()`**, because `parallax.js` adds its own resize listener inside
`keyframed()` and rebuilds its stops there. Without this the cover-fit crops the
chart's ends on wide viewports.

The demo chapters reorder so the walkthrough starts where the hero stopped:
`curve` first (the cursor arrives at the same last dot the hero just plotted),
then `reading` (exclude a bad weigh-in, the first thing the app can do that the
sheet could not), then `refuse` and `why`, then `weigh`, then `list`, then
`private`. **Chapter 1 is not orientation, it is recognition.**

## 6. PORTRAIT (390x844)

Charts die in portrait because people squeeze a landscape chart into a portrait
hole. **We rotate the data instead of the layout: in portrait, time runs down the
page.** Dates descend the left edge as a logbook column, weight runs across. Well
logs, tide tables and hospital observation charts all read this way, so it looks
like a convention rather than a broken desktop chart.

- A separate group in the same SVG with the axes transposed, shown by media query. Both carry the same `data-day` and `data-reading` attributes, so the trigger JS drives whichever is visible without branching.
- The gutter list becomes a single updating line above the plot.
- **The pencil of slopes actually reads better vertically:** 28 chords fanning down a tall plot have more angular separation than in a wide one.
- S8's two-band collapse becomes two vertical bands, the one place portrait is weaker; give that shot 10 percent more scroll distance.
- Track length drops to 780vh, since each shot needs less dwell when the plot is nearer the eye.

## 7. HOW I AVOID A SPREADSHEET

1. **The register split does the feeling.** Every caption is a human sentence in Newsreader at 54 px about a person and a plant, next to numbers in 13 px mono. The page is never uniformly numeric at any one scale; it is warm prose with cold evidence underneath, which is how a good field notebook reads.
2. **Named specifics, not abstractions.** The header says "Peperomia · pot 3 · east sill". The crossing is "24 Sept, evening", not "T+8.5d". Specific time and place is warmth; a generic axis is what makes dashboards cold.
3. **Marks are placed, not plotted.** Every dot lands with a 1.3x overshoot and settles. Ten marks arriving one at a time with weight reads as somebody writing in a book.
4. **One admitted mistake.** If the real diary contains an excluded reading, put it in as a hollow dot with a mono aside ("weighed with the saucer still under it, left out of the fit"). A person admitting an error in their own data is the most human thing that can be on a page of numbers, and no illustrated bedroom can do it. **If the diary has no excluded reading, do not invent one.** The integrity of this direction is that nothing drawn as data is unmeasured.
5. **The one dark frame.** S9 is the only shot with no data on it and the only one that is ink instead of paper. The page's one moment of emotional weight is the app's silence.
6. **The finger panel is drawn as an opinion.** Dim, hatched, labelled "not measured". That one piece of visual honesty tells the reader the rest of the page is measured.

## 8. COST AND RISK

One Python script extended to emit both sheets and the app chart from one data
table, roughly 500 lines, most of it axis and tick generation. About 200 lines of
page JS. The chord fan is 28 lines and one timeline. No illustrator, no WebGL, no
Blender. Two to three focused days.

**The real cost is sunk.** This direction throws away the room illustration, the
Blender work, and the five-beat script. That should be deleted rather than kept
as a second hero.

**Risks.** The fan can read as visual noise; bound it at 28 chords, 0.6 px,
opacity 0.10, and if it still reads as scribble, drop to the 12 chords adjacent in
the ranking, which still shows the median being taken. SVG text nodes for the grid
can get expensive, so flatten the grid to one path and use a pattern for the
squares. **The holdout framing can be misread as cherry-picking**, so the on-screen
label has to be unmissable at S7. Font loading matters more here, because a mono
fallback changes the digit alignment the whole layout depends on: `font-display:
swap` is wrong, use `optional` plus a metric-compatible fallback stack.

## 9. WHAT THIS DIRECTION IS BAD AT

- **It has no person in it.** It cannot do "I built this because I kept killing plants". The empathy beat has to be carried by prose further down the page, and if that prose is weak the hero has no way to rescue it.
- **It asks the reader to read numbers in the first three seconds.** That filters out the casual plant-app audience, who are most of the plant-app audience. This wins the person who already suspects their watering schedule is nonsense, and loses the person browsing.
- **S8 is the weakest frame on the page**, because it is the only panel built on a claim rather than a reading, and in a hero made of evidence the one piece of non-evidence is conspicuous.
- **It makes a bad thumbnail.** The OG image and any six-second video preview are a chart, and a chart loses to a lit room every time in a feed. Budget a separate OG image.
- **Execution quality carries all the differentiation.** Charts on hairline grids are what every analytics product does. If the mono discipline, the type hole, or the single-accent rule slips even slightly, this stops looking like a scientific instrument and starts looking like a dashboard template, and **there is no illustration underneath to fall back on.**
