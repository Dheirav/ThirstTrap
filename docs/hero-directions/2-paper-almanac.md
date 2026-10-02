# Direction 2: PAPER ALMANAC

*One of five competing plans. The page IS the book.*
No WebGL, no GLB, no Blender, no new dependencies.

## 1. THE IDEA

The hero is not a film about a man and a plant, it is **a page of the almanac the
app already is**: a cream leaf of paper carrying a botanical plate, a numbered
key, a cross-section, marginalia in his own hand, and on the facing page a drying
curve plotted on graph paper. The camera is a loupe moving across and into that
page, and at the end the page turns and the ruled table on the verso simply *is*
the app's plant list, because both are cream paper with hairline rules and dated
entries.

It fits because the app's interface is already this, and the current dark
cinematic hero is therefore the one dishonest surface in the whole project: a
visitor is sold a film and handed an almanac. A paper register also carries the
anti-goals structurally rather than by assertion. A dark room with a single warm
light is a *mood*, moods imply stakes, and stakes are what streaks and guilt are
made of, while a page with a blank line on it reproaches nobody. And botanical
plate is the historical form for this exact subject: someone observing one plant
repeatedly, writing numbers down, and ruling a line through them. Weight-based
watering is 19th century horticultural method with a kitchen scale, so the page
should look like the literature it belongs to.

## 2. PALETTE

| Token | Hex | Sat | vs paper | Role |
|---|---|---|---|---|
| `--paper` | `#F5F0E2` | 0.096 | - | the ground, hero and app both, never varied |
| `--paper-2` | `#F0EAD9` | 0.096 | 1.06:1 | paper laid on paper only: caption slips, specimen label, verso. One step, so it reads as a second sheet and not a tint |
| `--ink` | `#22201A` | 0.235 | **14.30:1** | all line work, all text. One ink in the whole file |
| `--ink-2` | `#413B30` | 0.262 | 9.74:1 | second-rank line work: key leaders, dimension lines |
| `--mid` | `#5F5847` | 0.253 | 6.20:1 | the system voice: mono labels, running heads, axis ticks |
| `--hairline` | `#C9C0A8` | 0.164 | 1.59:1 | every decorative rule, every grid line, the plate-mark |
| `--leaf` | `#2F5D3A` | **0.495** | 6.71:1 | the living plant, and the fitted line. Brand |
| `--terracotta` | `#8C3A2A` | **0.700** | 6.70:1 | the pot, and the 221 g trigger line. Brand |
| `--warm` | `#C98A3A` | **0.711** | 2.57:1 | the pointer. Four marks in the entire hero |
| `--foxing` | `#C2A880` | 0.340 | 2.00:1 | age blotches in the margins |
| `--pressed` | `#6E7049` | 0.348 | 4.52:1 | the pressed specimen in the gutter, deliberately **below** `--leaf`, so the dead specimen is measurably less alive than the drawn one |
| `--tape` | `#D9CBA8` | 0.226 | 1.41:1 | the gummed strip over the pressed stem |

**Change `SAT_CEILING` from 0.52 to 0.38** and add the brand colours to
`_EXEMPT`. Every illustration colour then sits at or below 0.348 while the brand
floor is leaf at 0.495, a clear 0.147 gap. Hatching is not a colour at all: it is
`--ink` at an opacity, so there is no grey anywhere in the file to drift.

**Hue lock.** Two families, no third. Warm runs hue 10 to 65, taking in ink 45,
paper 44, hairline 44, foxing 36, accent 35. Green is leaf at 137, alone.
Pressed at 65 is the bridge, which is why a dried leaf sits next to a drawn one
without looking pasted on.

**How text stays legible on a light ground.** The failure inverts: dark ink over
mid-value hatching. A 55 percent hatch field drops `--ink` from 14.30:1 to roughly
4.5:1, and worse per-glyph where a hatch line crosses a stem. The fix is not a
shadow and not a translucent scrim, it is a **tipped-in slip**: every caption
sits on an *opaque* `--paper` rectangle with a 1 px hairline border and a second
hairline 5 px inside the top edge, for letterpress register. No radius, no
shadow, because the app has no cards. Contrast is 14.30:1 by construction,
independent of the art behind it, and a pasted caption slip is a real artefact of
the form rather than a device bolted on.

The slip must be opaque at *every* scroll position, not just at peak, or mid-fade
puts 50 percent text over art. Give `.beat` a `.slip` child and drive
`slip.style.opacity = min(1, textOpacity * 2.4)`. **The paper arrives before the
text and leaves after it.**

Acceptance test: per-glyph contrast against the local background with a 3 px
dilated ring must report **0.0 percent of glyph area below 4.5:1**.

**How the accent still points on paper.** `--warm` is only 2.57:1 against paper,
so a thin accent hairline on cream is invisible. The rule: **on paper the accent
is a mark, never a line.** Minimum 8 px across, always a filled area. This
enforces the area budget automatically, because marks cannot accumulate area the
way a 1300 px dashed line can.

Four accent marks exist in the whole hero: a circled numeral **2** on the
cross-section (380 px²), the `x10` arc on Fig. 3 (110 px²), today's reading on the
graph (113 px²), and the active-row tick in the handoff table (54 px²). Maximum
simultaneously is **167 px², or 0.008 percent of a 1920x1080 viewport**, eleven
times under budget. That is the point: on paper the constraint is not area, it is
that there is room to spare and the discipline is to spend it on exactly one
thing per frame.

**Type scale.** The current page fails the 1.7x gap on phones: at 390 px both
clamps hit their floors at 20 px and 28.8 px, a ratio of **1.44x**. Replace with
four steps that hold at every width:

| Band | Face | Size |
|---|---|---|
| system voice | mono, +0.18em, uppercase, `--mid` | `clamp(0.66rem, 1.5vw, 0.78rem)` |
| body | Hanken Grotesk 400 | `clamp(1rem, 2vw, 1.25rem)` |
| lead-in | Newsreader 500 | `clamp(1.3rem, 2.6vw, 1.75rem)` |
| display | Newsreader 600 | `clamp(2.4rem, 5.2vw, 4rem)` |

Lead-in to display: **1.85x at 390 px, 1.90x at 760 px, 2.29x at 1440 px.** Body
to lead-in is only 1.30x, and that is correct: the 1.7x hole is required
*between* bands, and within a band you differentiate by typeface. Serif against
sans is exactly how an almanac does it, so the form pays for the rule.

Never set Newsreader at 400 for body on cream; it washes out on cheap panels.

## 3. SHOT LIST

Track stays at **820vh**. Two camera layers, DOM order **verso first, recto
second**, so the recto can turn away and reveal the verso beneath.

**Build rule the engine does not enforce:** every landscape `fit` rect must be
16:9 and every portrait one 0.462. `fitStop()` covers, so a rect of the wrong
aspect silently crops on one axis and the shot you authored is not the shot you
get.

| # | Scroll | In frame | Camera, and what it rests on | Animates | Caption at | Value floor |
|---|---|---|---|---|---|---|
| S1 | 0.000 to 0.100 | Whole recto: plate-mark, running head `PLATE IV`, three figures, numbered key, specimen label, two marginalia, pressed leaf and tape in the gutter | scale 1.08 to 1.00, a settle not a zoom. Rests 0.06 to 0.10 | nothing; the page is already drawn | h1 + lede + two CTAs | opaque paper panel, left 42 percent, single hairline right edge. 14.30:1 |
| S2 | 0.100 to 0.200 | Fig. 1, the pot in elevation, finger entering from the right at the rim | push to `#fit-fig1`. Rests 0.16 to 0.20 | finger descends 78 units along its own axis; soil dishes under it | **0.12** `TUESDAY, 11 P.M.` / "He pushes a finger into the pot. Dry on top, so out comes the can." | slip |
| S3 | 0.200 to 0.300 | Fig. 2, the pot in vertical section: hatched cut face, root network, moisture as stipple, depth ticks left, stipple key right | `#fit-fig1` to `#fit-fig2` with a 6 percent scale dip at 0.25, so it reads as lifting the loupe and setting it down. Rests 0.28 to 0.32 | the dry front descends: 14 stipple bands move the boundary from the surface to 2 cm; cut-face cross-hatch ramps 0 to 0.55 | **0.26** `TWO CENTIMETRES DOWN` / "The surface dries first. Underneath it has been wet for nine days." | slip |
| S4 | 0.300 to 0.400 | The top band: dry crust, the `2 cm` dimension line with arrowheads, circled **1** and **2** with leader lines | `#fit-fig2-band`. Rests 0.35 to 0.40 | numerals and leaders fade in, opacity only. The dimension arrowheads tick once, a 3-unit nudge: the only thing on the page that moves like a mechanism | **0.38** `FIG. 2, THE WHOLE POT` / "The finger asks about the top two centimetres. The question was about the whole pot." | slip |
| S5 | 0.400 to 0.500 | Fig. 3, the magnified roundel: fine roots at x10, one torn end, accent `x10` arc | pull back to 0.82 at 0.43, then push to `#fit-fig3`. **A pull-then-push is how a loupe is actually moved.** Rests 0.46 to 0.50 | torn root separates, 9-unit translate plus 14 deg rotate; cross-hatch under the tear deepens | **0.48** `AND THE TEST IS NOT FREE` / "Each push tears the fine roots it is measuring, and leaves a hole that dries faster." | slip |
| S6 | 0.500 to 0.620 | Whole recto again, now with all three circled numerals placed, so the key at bottom right is legible at page scale | back to scale 1.00, arriving 0.57, resting to 0.62 | the specimen label's border thickens 1.0 to 1.4 units, because it is about to become the subject | **0.555** `THE KEY` / "1 a dry crust. 2 the wet body. 3 the fine roots, torn." | slip |
| S7 | 0.620 to 0.700 | The recto turning about the spine, verso showing through, the data page beneath | **static.** A moving camera over a turning page is unreadable | the page turn: θ 0 to π smoothstepped, `scaleX = cos θ`, shear `k = 0.05 sin θ`. Hatching drops to 0 for `abs(sx) < 0.18`. Cast-shadow wedge peaks at θ = π/2 | **0.65** `FOUR POTS, ONE KITCHEN SCALE` / "So he stopped asking the soil and started weighing the pot." | slip, over the turning page |
| S8 | 0.700 to 0.840 | Verso: `PLATE V, THE WEIGHT OF A POT`. Left, the ruled table of four pots. Right, graph paper with axes, crosses, the 221 g and 298 g lines | whole verso 0.70 to 0.74, then push to `#fit-graph`. Rests 0.80 to 0.84 | ten plotted crosses appear in date order, staggered, opacity only, each cross whole. Today's accent disc last. **The terracotta 221 g line is present from the first frame, because the trigger weight is a property of the pot, not a result** | **0.78** `TEN READINGS, NINE DAYS` / "Sixteenth of September to the twenty-fifth. Every number on this page is one he actually wrote down." | slip |
| S9 | 0.840 to 0.930 | The graph with its fitted line, `8.8 g a day` in the margin, a brace reading `nine days` under the x axis | holds, then eases out to 0.96 by 0.93, putting the display caption over the bottom margin | **the line is drawn.** Triggered at p ≥ 0.84, a 0.95 s dasharray tween with a 2.5-unit ink reservoir at the pen tip fading on arrival. Caveat annotation at 0.87 | **0.88**, held to 0.92, display, centred: "A pot loses weight as it dries. Steadily enough to fit a line to." | **compositional.** The camera is directed so the display type lands on bare paper. Test: at 0.88 the 24 percent of viewport the type occupies contains no ink above 12 percent coverage. Slip is the fallback only |
| S10 | 0.930 to 1.000 | The ruled table alone on bare paper, then the real app | slide to `#fit-handoff`, arriving 0.965 | `#page-furniture` fades to 0 over 0.955 to 0.975. The table's own rules hold at full. Stage crossfades 0.972 to 0.996 | **0.945** `AND THAT IS THE WHOLE INPUT` / "One number, written down." | slip |

**Beat changes.** Nothing is cut. Three added, two reworded. Beat 4 becomes "So
he stopped asking the soil and started weighing the pot", because "stopped
trusting the finger" names a feeling while "stopped asking the soil" names the
mechanism. Added: `FIG. 2, THE WHOLE POT` at 0.38, which is the beat the current
page is missing and the whole reason a cross-section exists; `THE KEY` at 0.555,
closing a 123vh caption-free gap; `TEN READINGS, NINE DAYS` at 0.78, promoting
the credibility claim currently buried in a footer.

Nine captions is more than the dark direction needs, and that is a real
consequence: **there is no atmosphere to carry a silent stretch, so paper has to
keep talking.**

**The running footer, which solves the CTA rule properly.** A fixed footer at the
bottom of every page of the book: 11 px mono, left `THIRSTTRAP · APACHE-2.0`,
right `GET THE APK` as a real link, centre the plate numeral driven from scroll
progress, flipping `PLATE IV` to `PLATE V` at p = 0.66. It satisfies the
two-viewport rule continuously at about 0.004 percent of the viewport, *and*
doubles as the progress indicator the council found missing, *and* a page number
is the thing that tells a reader a text-free screen is a page rather than a
broken load. **Only available to a direction where the page is a book.**

Related: `.nav` stops hiding and becomes the book's running head.

## 4. THE ANIMATION

The governing decision: **nothing draws itself, because the page is already
drawn.** Every line exists at full opacity at p = 0. Roughly 70 percent of this
hero's apparent animation is camera only, which is what makes it affordable.

**Hatching.** Two `<pattern>` sets at +45 and -45 degrees, 6-unit pitch, ink at
0.55. To deepen a value I reveal the second set rather than add lines: two
overlaid hatch sets read as a real engraver's cross-hatch, and the transition
reads as *value changing*, not as motion.

**Stipple, which is the real animator.** 420 dots whose radius grows with depth,
fine near the surface and coarse at the base, so even a frozen frame reads as
wetter below. Binned into **14 depth bands**, so per-frame cost is 14 writes
rather than 420. Band k's opacity is a smoothstep over a window centred at k/14
with width 2.5/14, so three bands are mid-fade at any moment and the dry front
reads continuous rather than stepping.

This is the answer to the cheap-draw-in problem. A line getting longer always
reads as an effect. Stipple is already visually noisy, so dots thinning out reads
as the material changing, which is the thing the beat is about.

**Ink weight under magnification.** `vector-effect="non-scaling-stroke"` on the
figure *outlines*, so they stay crisp at 8x instead of becoming blobs.
Deliberately **not** on the hatching, so hatching opens up into separable lines as
the camera pushes in and closes to a tone as it pulls out. That is how a real
plate behaves under a loupe, it is one attribute, and it is the single largest
"this is actually paper" win in the build.

**The page turn.** An in-scene group transform, not a camera move, because the
recto group must reveal the verso layer beneath it.

```js
sx = Math.cos(theta)
k  = 0.05 * Math.sin(theta)
leaf.setAttribute('transform', `matrix(${sx} ${k} 0 1 0 0)`)   // hinge at x = 0
```

`cos θ` gives the physical acceleration through vertical for free, no easing
curve needed, and `sx = 0` at θ = π/2 means the page passes edge-on and is
genuinely invisible for one frame. Hatching goes to 0 while `abs(sx) < 0.18`,
because compressed outlines read as a page while compressed hatching reads as
moiré. The cast shadow is **the one soft value permitted on this page**, and it
is permitted because a turning page really does cast one. Flag it in a comment so
a later reviewer does not read it as a drop-shadow violation.

**The one legitimate draw-on.** The fitted line, and only it. A plotted line is
the one line a person actually draws in real time with a straightedge. The
defence is threefold: it happens exactly once, on the one object whose referent
is hand-drawn, and the points are already there before it starts, because dots
first and line second is the real order of plotting.

**Scrub versus trigger.** The turn **scrubs**, because it is an object moving in
space, and scrubbing it is the best thing in the hero: a reader can hold the page
half-turned. The line draw **triggers**, because it is an act, and its reverse is
its end state.

**Paper, not film.** Three one-liners: `.grain` becomes `mix-blend-mode: multiply`
at 0.06, because overlay on a light ground lifts the highlights and makes cream
look washed while multiply deposits fibre; kill `animation: grainshift`, because
animated grain is a film convention and on paper it reads as a failing screen;
keep it `position: fixed`, so fibre does not scale to visible blobs at 8x.

**Per-frame cost:** about 30 writes. No shader compilation, no texture upload, no
GLB.

## 5. THE HANDOFF

**Why it is structurally better.** The current page has to flip ink and paper,
hide the nav, halve the grain and cut from a keystoned phone screen to a flat DOM
screen, all within 0.024 of scroll. Every one of those is a visible event. **In
this direction there is no state change at the cut at all.** The hero is already
cream paper with hairlines and Newsreader names. There is no phone, no bezel, no
keystone, no screen glare, no moment where the page says "and now, a device".

**The target is not a phone screen: it is the ruled table on the verso.** The
app's plant list already is a ruled table, so draw the SVG version to the app's
own geometry: a 2 px ink header rule, 1 px hairline row rules, a 52x52 initial
square, Newsreader 17 px names, Hanken 12.5 px subs, four rows, the same four
plants including "This one has gone".

**Do not try to match the drawn table to the app pixel for pixel by arithmetic.**
It is not solvable in closed form: `fitStop()` covers, so making the drawn
620-unit column land at the app's 560 px reading column would need a fit rect
1.107 x viewport-width in user units, which exceeds the 1600-unit viewBox on any
desktop. **Instead, calibrate.** Add a `?handoff-debug` flag, about 20 lines, that
overlays the real `.app` at 50 percent on the SVG at p = 0.97 and prints the pixel
delta between the two header rules. Target within 12 px vertically and 6 percent
on width. It depends on viewport, so it must be checked rather than derived, and
this is the honest cost of the tightest handoff available.

**Portrait is where it is tightest**, which is the right way round, because the
app actually lives on a phone. The portrait verso's table is 190 user units wide,
which fits to 390 px, and the app's body padding gives 362 px of content. A
near-exact match with no calibration at all.

## 6. PORTRAIT (390x844)

Run the numbers first. `story.css` forces `.cam` to 1500x844 px, so a 1600x900
viewBox renders at 1 unit = 0.9375 px, and the 390 px viewport shows **only 416
user units, 26 percent of the width**, while all 900 units of height are visible
at scale 1. There is almost no vertical room to pan, and three figures laid out
horizontally are simply not reachable.

Re-framing cannot fix this. A real portrait layout is required, and **generated
art is what makes that affordable**: the figure functions take a target box and
scale into it, so portrait is a layout call, not a redraw. A hand-illustrated
direction would have to draw the plate twice.

- Two more camera layers, emitted from the same figure functions.
- **The plate becomes one column**, 190 units wide, 860 tall. Camera at scale 2.2, giving 2.2 screens of vertical travel.
- **The camera becomes a vertical pan**, which matches the scroll direction and is better than the landscape lateral slide.
- **Line weights survive for free** because outlines already carry `non-scaling-stroke` and the hatch patterns are `userSpaceOnUse`.
- **The turn's hinge moves to the top edge**, so the leaf lifts like a flip pad. A left-hinged turn sweeps the entire 390 px reading area and is disorienting.
- **The verso stacks**: graph on top, which makes it a portrait-shaped sheet of graph paper and better than the landscape one. Table below.
- Beat fractions shift, which needs `data-at-portrait` support, six lines in `beats()`, following the `data-kf-portrait` precedent already in `parallax.js`.

## 7. COST AND RISK

`tools/make-hero-svg.py` goes from 288 lines to roughly 700 to 800. Output
roughly 2500 SVG nodes, 180 to 260 KB inline, 30 to 45 KB gzipped.

**Hard:** the cross-section with a believable moving dry front (120 lines of
Python, and if the stipple distribution is wrong it reads as noise); the page turn
(only 25 lines, but high visual risk at mid-flip); handoff calibration across
viewports; hand-lettered labels, since Caveat is weak at small sizes over a grid,
so restrict it to 18 px and up, in margins only.

**Could look bad:** paper at 8x, so foxing needs a keep-out list for every fit
rect. **It could read as a stock "vintage" filter**, which is the worst failure
mode, and the only defence is that every element has a beat pointing at it.
**Acceptance criterion: if an element has no beat pointing at it, cut it.**

**Cut order:** the page turn first, falling back to a crossfade with a 2 percent
scale-down against a 2 percent scale-up, which reads as one page set down and
another picked up. Then Fig. 3's roundel. Then foxing and the pressed leaf. Then
the verso of the turned leaf. **Never cut:** the ruled table, the graph paper, the
opaque caption slips, the running footer.

**Three incidental bugs this direction forces you to fix.** `html { background:
var(--ink) }`, so overscroll shows black and would flash against a cream page.
`.loader` is dark, so the page opens with a dark flash before cream. And
`<meta name="theme-color" content="#22201A">` should be `#F5F0E2`.

## 8. WHAT THIS DIRECTION IS BAD AT

1. **There is no night, and no 11 p.m.** The strongest line in the current beat list is "Tuesday, 11pm", and a cream page cannot be eleven at night. A dark interior sells the specific human moment; a botanical plate sells the *category of knowledge*. I lose the moment and buy the authority. **If the brief is "make someone feel recognised in the first three seconds", this direction loses and should not be picked.**
2. **There is no atmosphere to hide behind.** A dark page can carry a mediocre drawing because 60 percent of it is in shadow. Cream shows every bad curve at 14:1. This has a much higher floor of required draughtsmanship, and there is no illustrator. The earlier "pot that was a trapezoid" comment in the generator is evidence of exactly how it fails.
3. **The form has a strong prior, and the prior is "nice".** Seed-catalogue aesthetics are everywhere. The honesty argument is powerful to someone who has used the app and completely invisible to a first-time visitor, who may read it as a pretty vintage theme.
4. **Low dynamic range leaves the accent with less work to do.** On cream at 2.57:1 the accent is a muted ochre, so attention control rests almost entirely on scale and position.
5. **A cross-section is read, not felt.** A finger going into soil and a root snapping is a physical event; mine is an illustration of one, a step removed.
6. **Paper does not move.** Across 820vh the only things that happen are zooms, one turn, and stipple thinning. Quiet pages lose readers who were scrolling fast.
