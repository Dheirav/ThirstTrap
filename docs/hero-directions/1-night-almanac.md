# Direction 1: NIGHT ALMANAC

*One of five competing plans. Evolve the existing night interior.*
Target files: `tools/make-hero-svg.py`, `site/index.html`, `site/engine/story.css`.

## 1. THE IDEA

A person tests a plant with a finger at eleven at night, in the dark, with nobody
watching, and the test is both wrong and destructive; then the lamp comes on, the
pot goes on a scale, and the room turns into an almanac page where nine dated
weights make a line. The hero is one continuous night, and the only cut in it is
the moment the light changes.

Why it fits this product specifically:

- **Night is when the behaviour actually happens.** Nobody weighs a pot at 2pm on a Tuesday. You notice a plant on the way to bed, which is exactly the moment the finger test gets made and the moment a reminder fires.
- **Darkness is the visual form of "no account, no server".** An offline app's whole promise is that this episode is unobserved. A lit, open, airy scene implies an audience.
- **Nothing in a night scene can be full or empty.** A dark register has no bright state to withhold, so there is no visual vocabulary available for a streak or a reward.
- **It buys the page its only real payoff.** The product is cream paper. If the hero is cream too, arriving at the app is a crossfade between two similar images. Nine viewports of night means the handoff into the app's paper is the moment the lights come on, and that moment is free.

## 2. PALETTE

Illustration palette, three hue families, hard ceiling per family: warm room
**0.52**, green **0.52**, cool window **0.42**. The cool family is the only hue
outside the brand's two families, so it is capped tighter and only appears
through glass, which is why it reads as "outside" rather than as a third brand
colour.

| Token | Hex | Hue | Sat | Where |
|---|---|---|---|---|
| `sky` | `#25333F` | 208 | 0.413 | night outside the window, Act 1 only |
| `sky_lo` | `#1A242D` | 208 | 0.422 | lower half of the sky |
| `glass` | `#2B3C49` | 206 | 0.411 | pane reflection |
| `moon` | `#E8E4D6` | 47 | 0.078 | moon disc, stars |
| `wall` | `#2A241C` | 34 | 0.333 | room wall |
| `wall_lo` | `#1A1510` | 30 | 0.385 | wall in shadow |
| `sill` | `#4A3F32` | 33 | 0.324 | sill top, moonlit |
| `sill_front` | `#241D16` | 30 | 0.389 | sill front face: the caption floor |
| `pot` | `#8C4F43` | 10 | 0.521 | pot body, hue-locked to terracotta |
| `pot_lo` | `#5E352D` | 10 | 0.521 | pot shadow side |
| `rim` | `#A86251` | 12 | 0.518 | rim band |
| `crust` | `#8A6E4C` | 33 | 0.449 | the dry top two centimetres |
| `wet` | `#2B2016` | 29 | 0.488 | wet core |
| `wet_lo` | `#17110B` | 30 | 0.522 | deepest core |
| `table` | `#3A2E22` | 30 | 0.414 | table top, Act 2 |
| `table_front` | `#15120D` | 38 | 0.381 | table front edge: the caption floor |
| `lamp_pool` | `#6A5233` | 34 | 0.519 | lamp pool on the table |
| `rim_light` | `#9A744A` | 32 | 0.519 | lamp rim on pot and arm |
| `silhouette` | `#100D09` | 34 | 0.438 | the arm, backlit |
| `steel` | `#C7BFA8` | 45 | 0.156 | scale platform |
| `lcd_on` | `#7FBE8C` | 132 | 0.332 | LCD digits, hue-locked to leaf |

Highest art saturation is 0.522, against brand terracotta 0.700 and accent 0.711.
Variety is bought in hue (208, 134, 10, 30 to 45) and in value (luminance 0.004
to 0.775), never in saturation.

**Accent budget.** Three marks, never more than two at once: the `2 cm` depth
tick in the cutaway (about 230 px of art-space ink), the "today" dot on the
fitted line (95 px), and a 2 px underline on the nav CTA (240 px). Worst
concurrent case at 1920x1080 is **0.027 percent**, against the 0.09 ceiling.

Portrait is where the budget bites: at 390x844 the ceiling is 296 px and the
depth tick alone is about 216. **So the nav CTA underline is hairline, not warm,
below 768 px, and portrait shows at most one accent mark at a time.** That is the
budget changing a decision rather than decorating one.

## 3. SHOT LIST

Track `--len: 1040vh`, up from 820. Two camera layers as today. Every camera move
is a pair of keyframes, arrive and hold, so easing lives in the camera path.

Captions: `beats(root, { win: 0.035 })` with `data-until="at + 0.05"`. Each is
fully opaque for 0.05 of the track and fades over 0.035 each side. Successive
beats are 0.10 to 0.13 apart, so **no stretch is ever text-free for more than
about 10 vh**.

| # | Scroll | In frame | Camera (and where it rests) | Animates | Caption | Value floor |
|---|---|---|---|---|---|---|
| 1 | 0.00 to 0.17 | Wide: wall, window with moon and six stars, sill, pot three quarters right, arm not yet in frame | scale 1 to 1.07, **rests 0.05 to 0.17** | leaf drift only | `0.13` "Tuesday, 11pm" / "A plant gets checked the same way by everyone. Push a finger in, feel for dry, fetch the can." | over `sill_front` `#241D16`, paper on it is **15.9:1** unaided |
| 2 | 0.17 to 0.30 | The pot and the arm entering right as a **silhouette** with a moon rim | fit `#sill-mid`, **rests 0.22 to 0.30** | finger travels along its own axis; soil dishes under it | `0.24` "Dry on top" / "Two seconds of contact, and the decision is already made." | scrim at 1.0 over the lower 38 percent |
| 3 | 0.30 to 0.44 | **The cutaway.** The near wall sections away on a 6 degree diagonal wipe: `crust` across the top 2 cm, `wet` below, root threads, drainage hole. Mono depth ticks at 0, 2, 12 cm; the 2 cm tick is the accent | fit `#cutaway`, **rests 0.34 to 0.44** | wipe reveal; wet line gets a dated mono label | `0.35` "Two centimetres down" / "The crust dries in a day. Twelve centimetres down it has been wet since the twenty-first, which is nine days." | over `wet_lo`, **17.0:1** |
| 4 | 0.44 to 0.54 | Macro: the hole the finger left, and one root thread across it, taut then parted | fit `#hole-macro`, **rests 0.47 to 0.54** | the root parts, once, latched; hole edge crumbs settle | `0.48` "And the test is not free" / "Each push tears the fine roots it is measuring, and leaves a hole that dries faster than the soil around it." | `wet_lo`, 17.0:1 |
| 5 | 0.54 to 0.62 | **The match cut.** Same pot, same pixels, different light: moon-cool becomes lamp-warm, the sill becomes a scale platform | layers crossfade 0.54 to 0.56, **both rest 0.56 to 0.62** | the crossfade is the only change; the hole from shot 4 is still there | `0.58` "Four pots, one kitchen scale" / "He stopped trusting the finger and started writing the weights down." | `table_front`, **16.4:1** |
| 6 | 0.62 to 0.74 | Pull out: pot on the scale, LCD legible, phone face down at the right edge, lamp pool | fit `#scale-wide`, **rests 0.66 to 0.74** | LCD wakes and settles with one overshoot; lamp pool breathes | `0.69` "218 g" (mono, system voice) / "Nothing to interpret. One number, written down." | `table_front`, 16.4:1 |
| 7 | 0.74 to 0.86 | **The ledger.** An almanac page in the lamp pool: hairline rules, nine dated entries in mono, nine plotted points, a fitted line, `8.8 g/day` in the margin | fit `#ledger`, **rests 0.77 to 0.86** | the line draws; the "today" dot arrives on the accent | `0.79` **large, centred**: "A pot loses weight as it dries. Steadily enough to fit a line to." | this shot is cream paper, so the floor inverts: an **ink panel** behind the big beat, 16.4:1 |
| 8 | 0.86 to 0.94 | The phone on the ledger, showing **the same chart as the drawing**, same nine points, same slope | fit `#phone-wide`, **rests 0.90 to 0.94** | screen fades up from off to on | `0.90` "Nine days of that, and the pot can say when." | ink panel, 16.4:1 |
| 9 | 0.94 to 1.00 | Handoff | fit `#screen`; DOM crossfades 0.978 to 0.997 | see section 5 | none | n/a |

**Beat changes.** Old beats 1 and 2 merge into the new 1 and 2, because "he
pushes a finger in" and "dry on top" were the same information twice. One beat is
added, shot 6's "218 g", because the page claimed a number mattered and never
once showed one being taken. The large centred beat keeps its words and now lands
over a drawing of the thing it describes rather than over a pan of furniture.

**The match cut is a build requirement, not a hope.** Our current page has the
same defect the council found at the reference site: `#thepot` and `#potonscale`
are both 240x180, but the two pots are drawn from different `pot()` calls at
different coordinates with different plant seeds, so the crossfade is a dissolve
between two drawings. Fix it mechanically: emit **one** `pot()` call with
identical geometry into both SVGs, give both match rects identical size and
identical offset from the pot's base centre, and use the same plant seed. Inside
the match frame the two images then agree on roughly 85 percent of their pixels.
Verify by rasterising both crops and diffing them.

## 4. THE ANIMATION

"Latched" means the value only ever increases, so an irreversible event stays
done when the reader scrolls back up. **A camera move is spatial and belongs to
the scroll position; damage is temporal and does not un-happen.**

| id | Thing | Property | Range | Notes |
|---|---|---|---|---|
| L1 | Leaves | rotate per leaf group, ±0.6 deg, phase offset per index | continuous, 0.00 to 0.62 | a sine of **scroll**, not of time, so it stops when the reader stops. Any more than a degree and a flat-vector plant looks like it is in a breeze the rest of the room cannot feel |
| A1 | Finger | translate along the finger's own axis, 0 to 78 px | in 0.185 to 0.24, out 0.50 to 0.545 | reuse the existing `AX/AY/REACH` constants |
| A2 | Soil dish | opacity 0 to 1 on `#dish` | 0.215 to 0.255 | |
| A3 | Cutaway | clip-path inset wiping on a 6 deg diagonal | 0.300 to 0.360 | scrubbed, reversible, spatial: this is the camera's x-ray, not an event |
| A4 | Wet-line date label | opacity plus a 10 px rise | 0.345 to 0.375 | mono, hairline, "watered 21 Sept" |
| A5 | Root thread | dashoffset taut to parted, plus 16 deg rotation of the lower stub | **latched**, 0.455 to 0.495 | monotonic on the maximum of p seen |
| A6 | Hole crumbs | three circles, cy +6 px | latched, 0.470 to 0.520 | |
| A7 | The hole, in Act 2 | opacity of `#dish2` tied to A2's latch | from 0.54 | same hole, same place, across the cut. This is what makes the cut feel like the same evening |
| A8 | LCD | textContent 0 to 231 to 218 g, one overshoot | 0.620 to 0.700 | already implemented; keep the overshoot, it is the most physical moment in the page. **Latch the wake-up** |
| A9 | Lamp pool | opacity 0.88 to 1.0, sine | continuous, 0.56 to 1.00 | amplitude 0.06. No movement reads as a flat fill; more reads as a fire |
| A10 | Fitted line | dashoffset over the nine points | 0.770 to 0.840 | the nine points appear on a 0.004 stagger just ahead of the line head |
| A11 | "today" dot | r 0 to 5.5, accent | 0.840 to 0.860 | the only accent mark in this shot |
| A12 | Phone screen | opacity 0 to 1 | 0.880 to 0.910 | lights up **before** the camera arrives, so the camera arrives at something already on |

## 5. THE HANDOFF

A **graphic match on the chart**, built so a content mismatch is impossible in
either direction.

1. **One source of truth.** `site/curve.json`: nine `{date, grams}` readings, the fitted slope, the intercept, the plotted geometry. The generator reads it for the ledger chart and the phone-screen chart. The walkthrough's `answer` template reads the same file at build time. The drawn chart and the live chart are then the same nine points at the same positions.
2. **Both sides of the cut are already cream.** By shot 7 the frame is a paper ledger in a lamp pool, so the luminance jump from night to `#F5F0E2` happens **inside the artwork**, between shots 6 and 7, motivated by a lamp and a sheet of paper.
3. **The cut.** `fit:"#screen"` at 0.985, then the stage crossfade, as today.
4. **The walkthrough starts where the hero ended.** First chapter becomes a cold open on the chart, and the cursor's first act is to back out of the chart into the plant list. That resolves the match, teaches the navigation, and makes reverse scroll correct: scrolling up out of the walkthrough lands on the chart, which is the hero's last frame.

## 6. PORTRAIT (390x844)

The camera keeps the art at 16:9 and overflows on the short axis, so portrait
sees a slice about 26 percent of the art's width. Panning a landscape room across
that slice shows nothing, so **portrait is a different cut of the same film**.

- **Shots 1 and 6 are cut**, becoming holds. Shot 1 becomes a tall framing of the window's right half plus the pot.
- **Shots 3 and 4 are better in portrait than in landscape.** A soil column in section is a vertical object; the cutaway frames crust, core and roots top to bottom with depth ticks down the left edge. Give it more dwell, 0.30 to 0.46, because it is the best frame on the phone.
- **Shot 7 crops to the chart, not the page.**
- **Captions dock differently.** `bottom: 9vh` is the busiest region of a portrait frame, and a gradient over a busy region is still a gradient. In portrait, captions sit in an **opaque surface**: full-bleed panel at 0.96, a 1 px hairline along its top edge, docked to the bottom. 16.4:1 regardless of what the camera is pointing at, and a dark block under a hairline rule is the app's own almanac grammar rather than a video-player overlay.

## 7. COST AND RISK

Build order:

1. **The cutaway** is the most new code and the highest payoff. Half a day. Risk: it can read as a diagram pasted onto a picture. Keep the sawn edge thick, in `rim` colour, with the drainage hole visible in section, so the eye reads "someone cut this pot in half" rather than "someone opened a chart".
2. **The ledger and `curve.json`.** Mostly maths that already exists in the app's chart code.
3. **The match cut** is a refactor, not new art. An hour, plus a rasterise-and-diff check.
4. **The arm as a silhouette is a deletion.** The current arm is the weakest asset on the page: capped skin tones make a hand look prosthetic. Backlighting it removes every modelling problem, costs less code, and raises the caption floor in shots 2 and 4.
5. **Blender is not used in this direction at all.** The register is flat vector with two values per object; a path-traced pot would break it, and 9 seconds a frame against a scrubbed timeline means a render farm for a scroll position that changes continuously.

**What could look bad.** Crushed blacks on cheap panels: several values sit under
luminance 0.01, and on a poor phone screen `#1A1510` and `#15120D` become one
black rectangle. Floor every fill at `#100D09` or lighter and keep at least 0.012
luminance between adjacent fills that must read as separate objects. Banding in
the lamp pool: build it as three stepped hard-edged ellipses rather than a radial
gradient, which is on-style and cannot band. Grain at 0.085 overlay on a dark
field lifts blacks; drop to 0.065 if the night fields look milky.

**Cut order:** shot 4 first, folding its caption onto the end of the cutaway rest.
Then shot 8, fitting straight from `#ledger` into `#screen`. Then A9 and L1. **Do
not cut the cutaway, the match cut, or `curve.json`;** without those three this is
the current page with better colours.

## 8. WHAT THIS DIRECTION IS BAD AT

- **It spends nine viewports in a register the product never uses.** The app is cream paper in daylight. A reader who leaves at 30 percent has formed an impression of a moody indie thing, not of an almanac.
- **Dark heroes photograph badly everywhere else.** The OG image, the search thumbnail, the GitHub social card and the F-Droid screenshot will all be murky, and a dark frame at 1200x630 loses the cutaway's depth ticks entirely.
- **Every caption needs a manufactured floor.** In a night scene there is no naturally bright region to put dark text on, so legibility depends on scrims and opaque panels in all nine shots. That is engineering a light direction would not need to do at all.
- **The product's actual subject is a line on a chart, and a night scene is the worst possible place to draw one.** That is why shot 7 has to turn the lights on, which means the direction concedes its own register right before the payoff.
- **It tells a story about a mistake before it tells one about a product.** Five shots and six viewports before anything the app does appears on screen.
