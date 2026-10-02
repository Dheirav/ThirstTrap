# Direction 3: LIVE 3D TOON

*One of five competing plans. Model the space, fly a camera.*
Target: `site/` + `tools/blender/`.

## 1. THE IDEA

One unbroken camera move, from a finger pushing into a pot on a windowsill at
11pm, down *inside* that pot to the wet soil two centimetres under the dry crust,
back out through the hole the finger left, across the dark room to a kitchen
scale, and into the phone sitting next to it. The product's whole claim is that
the pot is a physical object with a weight and an interior your finger cannot
see, so the hero is the one presentation where the pot is actually a physical
object with an interior: real depth, real occlusion, and a section through the
same geometry you were just looking at from outside.

That last clause is the whole case. **A 2D hero draws the pot, then draws a
different picture called a cross-section, and the reader has to accept that the
two pictures are the same pot. A live 3D scene cuts the pot you are looking at,
with the camera's own motion, in one continuous shot.** For a product whose pitch
is "the surface lies, measure the whole thing", the difference between "here is a
diagram of the inside" and "we are now inside" is the difference between an
argument and a demonstration.

## 2. THE LOOK

### The lighting solver

| Light | Type | Colour | Intensity | Shadow | Job |
|---|---|---|---|---|---|
| `moon` | directional, from outside the window | `(0,0,1)` pure blue | 2.2 | **yes**, the only caster | band index |
| `fill` | ambient | `(0,0,1)` pure blue | 0.32 | - | floor under the dark band |
| `lamp` | spot over the table | `(1,0,0)` pure red | 9.0, decay 1.6 | no | warm mix |
| `glow` | point at the phone screen | `(1,0,0)` pure red | 0.35 | no | the phone lights its own patch of table |

**Every Lambert material's colour is pure white.** This is non-optional and easy
to get wrong: colour comes *entirely* from the band lookup, so `outgoingLight.b`
has to be light intensity and nothing else. Any albedo tint multiplies into the
band index and the thresholds stop meaning anything.

### The band shader

```glsl
float m = clamp(outgoingLight.b * uGain, 0.0, 1.0);              // moon channel
float w = clamp((outgoingLight.r * uGain - uW0) / uW1, 0.0, 1.0); // lamp channel
float e = fwidth(m) * 0.75;                                       // 1.5px band AA
float b1 = smoothstep(uT1 - e, uT1 + e, m);
float b2 = smoothstep(uT2 - e, uT2 + e, m);
vec3 toon = mix(mix(uDark, uMid, b1), uLit, b2);
toon = mix(toon, uWarm, w * uWarmAmt);
gl_FragColor = vec4(toon, 1.0);
```

**The `fwidth` term is the difference between a cel shader and a broken render.**
Without it the band boundary crawls in hard stair-steps whenever the camera moves,
which is the characteristic ugly failure of this technique and the first thing
anyone notices. One screen-space derivative per fragment buys a clean boundary.

### Band thresholds, and why these numbers

Default `uT1 = 0.22`, `uT2 = 0.56`. Solving back: `uT1` sits at 83.9 degrees off
the light, so **the dark band only catches surfaces within six degrees of facing
away**, plus anything in shadow. `uT2` sits at 60.3 degrees, where a cylinder's
curvature is steepest and the step reads as form.

**The reasoning is the value floor, not taste:** dark pixels are where text dies,
so the thresholds make the dark band small and the lit band generous. A
paper-dominant frame is also what stops the canvas reading as a dark rectangle
pasted into an almanac.

Two families override the defaults, because the inside of a pot at 11pm must
genuinely be dark: `soil_interior` and `section` use `uT1 = 0.46`, `uT2 = 0.78`,
inverting it so they are mostly dark and lit only at the crust.

### The palette table, keyed by `tone`

| `tone` | dark | mid | lit | max S |
|---|---|---|---|---|
| `wall` | `#3A362C` | `#A89E86` | `#EDE6D4` | 0.240 |
| `floor` | `#2B271F` | `#7E7260` | `#B9AE97` | 0.238 |
| `wood` | `#332C22` | `#8A7658` | `#C2AC85` | 0.362 |
| `terracotta` | `#4A3A33` | `#99705F` | `#CC9485` | 0.348 |
| `soil` | `#1F1A14` | `#4A3E2E` | `#6A5942` | 0.378 |
| `soil_interior` / `section` | `#17130E` | `#352C20` | `#574833` | 0.378 |
| `leaf` | `#22332A` | `#3F6049` | `#6B9474` | 0.344 |
| `root` | `#2A241A` | `#6E604A` | `#A89676` | 0.302 |
| `skin` | `#4A3A2E` | `#9C7D66` | `#C9A585` | 0.378 |
| `metal` | `#35322B` | `#8C8779` | `#CFC9B8` | 0.136 |
| `sky` | `#2A2A26` | `#2A2A26` | `#2A2A26` | 0.095 |

Warm mix target `#C7A47B`, the brand accent hue-locked at half saturation, at
0.65 strength. **Except `leaf`, where the target is `#86A074` at 0.30, because
mixing a green toward amber walks the hue out of both families and the plant goes
olive and sickly.**

### How the saturation ceiling holds in a lit scene

This is harder in 3D than in flat vector, and it holds for a structural reason
rather than by discipline: **there is no path from a light to the output colour.**
The only colours that can appear are the 33 hex values in that table plus the warm
targets, because the band lookup *replaces* `outgoingLight` instead of modulating
it. No specular, no gradients, no tone mapping, no light tint. **The blue moon and
the red lamp never reach a pixel.**

So the ceiling is enforced by the table, whose maximum is **S 0.382**, against
leaf at 0.495, terracotta 0.700 and accent 0.711. Checkable by reading a constant
file rather than by sampling a screenshot.

Two consequences worth stating plainly. **The terracotta pot in the scene is not
brand terracotta**; it is hue 13 at S 0.35, and the only fully saturated
terracotta on the page is the words "Needs water now" in the UI. **The subject of
the illustration deliberately loses to the product's own type.** And the night sky
is a near-neutral `#2A2A26` rather than a blue, because the sky is the one place a
third hue family could sneak in.

### What bypasses the solver

Four unlit `MeshBasicMaterial` emitters, **and they are the entire accent
budget**: the LCD recess, the LCD text at **full brand accent `#C98A3A`**, the
phone's paper screen, and the screen's type.

**The only pixels on the page allowed brand saturation are the number the app
exists to produce.** That is the accent rule stated as an architecture rather than
as a guideline.

## 3. SHOT LIST

Track `--len: 900vh`. Eleven keyframes, ten segments, so keyframe *i* rests at
`p = i/10`. Easing is in the path, so every keyframe is a hold nobody authored.

| p | camera intent | fov | in frame | animates | caption | value floor |
|---|---|---|---|---|---|---|
| **0.00** | low, floor-level, the room in one shot | 42 | window and sill pot left, lamp and table right, watering can on the floor | leaf sway only | hero title + lede | paper type on `floor` dark, **17.1:1** |
| **0.10** | `sill_wide` | 41 | the sill pot, moonlit from behind; forearm entering top right | finger descends from −45 mm to −18 mm | **B1** `Tuesday, 11pm` | paper plate, bottom left, **14.3:1** |
| **0.20** | `sill_tight` | 23 | fingertip at the soil, 8 cm wide frame | finger reaches 0 and pushes 10 mm in; dimple opens | - | - |
| **0.30** | swung round to the pot's profile | 26 | **the sectioned pot.** Full height, cut face square to lens | section plane sweeps across p 0.26 to 0.33, peeling the near wall away; dry front at 20 mm | **B2** `Two centimetres down` + a projected `20 mm` dimension rule | paper on `soil_interior` dark, **16.4:1** |
| **0.40** | 15 cm from the face, root scale | 34 | the finger shaft crossing the section, fine roots either side, wet core below | roots within the shaft's radius bend and go slack | **B3** `And the test is not free` | **16.4:1** |
| **0.50** | up and over, looking down into the hole | 24 | the hole from above, dry halo around it | section plane closes; dry halo grows 26 to 44 mm | **B4** `What it leaves behind` | paper on `soil` mid, **9.1:1** |
| **0.60** | lift, turn, cross the dark room | 46 | window receding, lamp and table arriving; the four pots count out | leaf sway; warm mix climbing as we enter the cone | **B5** `Four pots, one kitchen scale` | **framing:** the caption rect is authored over the unlit wall, **13.8:1** |
| **0.70** | `table_wide` | 33 | four pots, the scale with one pot on it, the phone, the lamp | readout reads `298 g`, dated 16 Sept | **B6** `The whole of it` | paper plate, **14.3:1** |
| **0.80** | `table_scale`. **Locked.** 0.784 m from the scale | 24 | pot on the scale plate, readout, phone edge-on | **the nine-day pass begins** | **B7** large centred, rises from 0.74 | paper plate with hairline rule, **14.3:1** |
| **0.90** | same look-at, position crept 25 mm. Still locked | 24 | identical framing | pass ends on `218 g`, 25 Sept | **B7** holds to 0.89 | as above |
| **1.00** | computed: on the screen's normal through its centre, `d = 0.4835 m` | 24 | the phone square to lens, screen 72 percent of viewport height | nothing | **B8** at 0.95, mono, the real numbers | ink on paper screen, **14.3:1** |

**Beat changes.** B1, B2, B3, B5 and the big B7 are unchanged in wording. **B4 is
new, split out of B3:** `What it leaves behind` / "A channel to the roots, open to
the air. Next time the top two centimetres dry faster still." B3 states the cost,
B4 states the consequence. **B6 is new:** `The whole of it` / "Put the pot on the
scale. Write the number down. That is the entire ritual." Nothing in the current
beat list says what the user actually *does*. **B8 is new and it is the handoff:**
`16 to 25 September` / "Ten readings. 298 g down to 218 g. 8.8 grams a day."
Nothing is cut.

## 4. THE ANIMATION

**The rule I would restate.** "Scrub what is spatial, trigger what is temporal" is
nearly right but it mis-draws the line. The real line is: **scrub anything that is
a pure function of p; trigger anything with internal state or its own clock.** A
scripted cursor has state, so half a click is incoherent. A pot drying over nine
days has no state, it is a continuous physical function of time, and **the
product's entire claim is that it is smooth enough to fit a line to.** Scrubbing it
is not a violation of the rule, it is the rule applied correctly. I am scrubbing a
nine-day time lapse on purpose and I will defend it.

| Thing | Mechanism | Range |
|---|---|---|
| **Finger push** | along the finger's own axis `(-0.611, 0.442, -0.659)`, already derived in `animate.py` | p 0.10 → 0.22 |
| **The hole** | vertex shader on the soil grid, not a morph target, using `dress_soil`'s own formula. **Normals must be perturbed analytically**, or the band shader will not see the hole at all | depth 0 → 10 mm over 0.14 to 0.24 |
| **Dry front** | a uniform in metres below the surface; the section quad paints pale bands above and dark below | 20 mm at 0.30, 52 mm by 0.90 |
| **Dry halo** | a ring of pale band around the hole, because a hole dries faster | 0 → 44 mm over 0.44 to 0.54 |
| **Section plane** | one `THREE.Plane` with `localClippingEnabled`, sweeping so the wall peels away from the camera's side as the camera arrives | 0.26 to 0.33, reversed 0.44 to 0.52 |
| **Root slack** | per-vertex `_TEAR` and `_BREAKDIR` baked in Blender. **The roots bend and go slack; they never snap.** A break is a discontinuity and discontinuities scrub badly backwards; slack is continuous and still reads as damage | 0.18 to 0.26 |
| **Leaf droop** | rotates each leaf about its baked base by up to 5 degrees as the pot dries | 0.78 to 0.89 |
| **The readout** | ten discrete steps through the real diary. Discrete is fine: it is still a pure step function of p, and **real readings are discrete events, one a day** | 0.78 to 0.89 |
| **Moon azimuth** | nine discrete steps, one per day, during the locked pass. **The only time the shadow-casting light moves**, which is what makes the shadow map affordable | 0.78 to 0.89 |

The ten weights: **298, 289, 277, 266, 257, 251, 240, 233, 226, 218.** Eighty
grams over nine days, 8.89 g/day, against a trigger of 221 g.

**The pot does not visibly change across those ten steps, and that is the point of
the shot. Nine days of drying looks like nothing. Only the number moves. The
caption then names the thing you could not see.**

Ambient is leaf sway only, 0.6 Hz, 1.2 degrees, throttled at 41 ms moving and
120 ms parked. All 30 leaves and stems are **one merged mesh and one draw call**.

**What is rigged:** only the finger and its arm, as a parent transform. No
skinning, no armatures, no glTF animations. That matters because there is no 3D
artist: **a uniform is a line of JavaScript a programmer can reason about, and a
rig is not.**

## 5. THE HANDOFF

The reference solved this backwards and it cost them. They computed a distance
that made the screen overfill by six percent, which guarantees the illustrated
screen is still keystoned and inside a bezel when the dissolve starts, and that
the live UI is roughly fifteen percent larger.

**I invert it: size the DOM to the camera, not the camera to the viewport.**

Square to the screen quad, through its centre, so keystone is zero by
construction. With fill fractions `f_h = 0.72`, `f_w = 0.78` and fov 24: the
height term gives **0.4835 m**, the width term gives 0.1348 m at 16:10 and
0.4667 m at 390x844. `max` in both cases is **0.4835 m. One distance serves
landscape and portrait**, because the screen's 2.07 aspect sits between them.

Then project the quad's four corners with `Vector3.project(camera)`, which
`camera-rail.js` already does every rendered frame. That gives an axis-aligned CSS
rect in pixels, exactly where the illustrated screen is.

1. **p 0.90 → 0.965:** the demo stage's paper panel is positioned and sized to that projected rect. Same paper, same mono label, same hairline rule at the same fraction of panel height, same four rows. Opacity 0.
2. **p 0.965 → 1.000:** plain opacity crossfade of two layers **already pixel-aligned**. At opacity above 0.99, `rail.pause()`.
3. **First 200 px of the demo track:** the panel grows from that rect to full-bleed. Both states are paper with a centred column, so the growth is a pure scale with nothing to misalign, and the reader watches the phone's screen become the page.

**The content-mismatch bug, pre-empted.** The reference's worst fault was that the
3D screen showed one page while the walkthrough ended on another. **One source for
both sides:** the phone screen's canvas texture is generated at load from the same
row data the `list` template holds, from one JS array, in the same two fonts. And
the walkthrough's chapter one must both start *and end* on `list`, which is also
just the correct rule for any chapter: **a chapter's end state is what you see
scrolling back up into it.**

## 6. BUDGET

**Geometry: about 56,000 triangles**, 42 percent of the reference's 133,004,
because their set was a whole roof terrace and ours is one room and five pots. The
soil grid is the biggest single line and it earns it: it is the thing the finger
deforms, the thing the section cuts and the thing the product measures.

**Bytes: I am not asking for 870 KB. I am asking for 452 KB**, and the biggest
single reason is that I would not ship Draco.

| Asset | KB |
|---|---|
| `scene.meshopt.glb` | 280 |
| `meshopt_decoder.js` | 5 (versus Draco's 205) |
| three.js subset, rollup + brotli | 115 (the estimate I trust least; measure before committing) |
| posters, 2 webp | 34 |
| engine JS, brotli | 18 |
| **total** | **452** |

Draco would give a smaller GLB, around 200 KB, but costs a 205 KB decoder and the
reference's measured 1.3 second synchronous block. **Meshopt wins on bytes and on
blocking time simultaneously**, which makes it the easiest call in this plan.

**Textures: two, both generated at runtime, zero texture bytes shipped.** The LCD
readout is a 512x128 canvas redrawn only when the number changes, twelve times in
the whole hero. The phone screen is a 768x1536 canvas drawn once at load from the
same row data the templates use. **Both carry the app's real typefaces, which is
the whole reason they exist. The number is the product; it does not get to be a
placeholder.**

**The shadow map**, where the reference lost. Their 2048² map re-rendered every
frame was 4.19 Mpx of depth against 1.30 Mpx of colour, and 58 of about 110 draw
calls. My plan: **one caster ever** (the lamp casts nothing; in a band shader an
unshadowed warm light just means the warm mix is slightly generous, which is a
look, not a bug); **1024², `autoUpdate = false`**, so 1.05 Mpx against 1.30 Mpx of
colour, down from 3.2:1 to **0.8:1**; and `needsUpdate` set **exactly eleven times
across the whole hero**. Every other frame costs zero shadow draws.

The shadow's only real job is the contact shadow where the pot meets the sill and
the scale plate, **because "the pot is on the scale" is the entire product and a
floating pot kills it.** The hole needs no shadow: in a band shader a depression
reads dark purely from its normals.

**Draw calls: 21**, against the reference's roughly 110.

**Cold load.** **Create the caption `ScrollTrigger` first, before any `await`.**
This is the reference's single worst bug: one `await createScene()` in the wrong
place held their narrative hostage to their largest file for 33.7 seconds on a
throttled connection, and the page looked broken rather than loading. **Capability
test before the fetch**, because the reference preloaded 665 KB unconditionally in
`<head>` before their own WebGL2 test. Merge pass in `requestIdleCallback` after
first paint, under the poster. Blocking total about 150 ms against 1,300.

**No-WebGL and reduced motion.** **The fallback is already built, reviewed and
committed:** the complete 2D SVG scene currently in `site/index.html`, with its own
camera tracks. Move it to a fragment fetched only when WebGL2 is absent. No-WebGL
readers get the full scrubbing story rather than a frozen first frame, and it
costs nobody else a byte.

For `prefers-reduced-motion`, **serve no GLB at all.** Five posters at the five
beat positions, stacked, with the captions under them, and the track heights
collapsed. That saves 452 KB and removes a camera that dives into soil, which is
precisely the vestibular trigger the media query exists for. **The reference was
called out for shipping identical bytes and the same camera in both modes:
honouring the letter and missing the point.**

## 7. PORTRAIT

A separate keyframe array, not a tweak. three.js FOV is vertical, so at 390x844 the
same FOV keeps the vertical framing and loses 71 percent of the horizontal field.
**That changes what each shot is for, not just where the camera stands.** Matching
the landscape framing would put the camera 2.7 times further back, which is why
you do not match it.

- **0.00** The room-wide is unusable; there is no horizontal field to put a room in. Replace with a tall shot: window filling the top, sill pot centred, floor at the bottom. **Give up the left-to-right geography and buy the window-to-floor vertical instead.**
- **0.10 to 0.20** **Frame the arm, not the pot.** The pot fills 93 percent of the frame width in the bottom third, and 24 cm of descending forearm fills the upper two thirds. **A composition landscape cannot hold, and a better version of this beat.**
- **0.30 to 0.40** The section **gains** in portrait: a cut pot is a tall object and the dimension rule reads better in a tall frame.
- **0.60** A lateral traverse is the worst possible portrait move. Replace it with a vertical one: lift off the sill until the frame is filled with unlit wall, then descend to the table. **B5's caption lands in that empty field, so portrait's weakness becomes the one place the page actually wants a quiet full-bleed plate for type.**
- **0.80 to 0.90** Stack it: readout in the lower third, pot above, lamp light from the top. Better than landscape.

Two portrait-specific rules:

**Captions go in the upper third, in a paper plate, never at `bottom: 9vh`.** The
reference put them at the bottom of a portrait frame, the busiest region of any
portrait composition, and measured per-glyph contrast down to 1.70:1 as a result.

**The accent changes form, because the budget does not bend.** At 390x844 the
budget is 296 px and the LCD's amber glyphs at a readable size are about 2,500 px,
eight times over. Pushing the camera back to 1.28 m to comply would destroy the
shot. **So in portrait the readout is paper on an ink display, a value signal
rather than a hue one**, and the accent appears only as a 2 mm lit hairline under
the number. The accent's job is to point at the number, and it still does. Only
its form changed.

In landscape the budget is comfortable at **0.070 percent**, which is also where
the constraint bites back as a camera rule: **the camera may never come closer
than 0.55 m to the scale**, or the accent budget breaks. A measured rule turning
into a hard constraint on the shot list is the sign the rule is real.

## 8. COST AND RISK

**Hard, in order.** Capping the clipping plane: `localClippingEnabled` gives you a
hole, not a cut face, so it needs a dedicated coplanar section quad with
double-sided wall handling. The fiddliest hour in the build and the one most
likely to look wrong first. **Normals:** a band shader is brutally honest about bad
normals, and every smoothing artefact becomes a visible wobble in the band
boundary, so compute the soil's normals analytically from the noise function
rather than from the mesh. **Band crawl**, already addressed by `fwidth`, but it
will look bad for the first hour before that line goes in. **The moon sweep** is
the most beautiful thing in the shot and the most likely to read as a bug.
**Centripetal Catmull-Rom with near-coincident control points** divides by the
distance between them, so give every hold a real 25 mm creep, which is why
keyframe 9 is offset from keyframe 8 and keeps the shot alive at the same time.
**Everything is parametric, so everything is symmetrical:** a scripted room looks
scripted, no wear, no dirt, no asymmetry. The band shader hides a great deal of
this, which is a real reason this look suits a programmer-built scene, but seed
per-pot variation anyway.

**What could look bad.** The section reading as a science diagram rather than a
diary. It is the strongest idea here and also the least domestic. **The mitigation
is that the finger stays in frame throughout the section shots: it is a diagram
with a person's hand in it.**

**Cut order:** the moon sweep; then the roots and the tear; then shadows entirely,
replaced with baked contact decals; then **the section** (fourth and not first,
because it is the only thing on this list that 2D cannot do); then the
establishing wide; then **the 3D entirely**, which is one `if` away from the 2D
SVG scene that is already written and committed.

**That last line is the real argument for picking this direction. The downside is
bounded, because the fallback already exists.** This ships as a progressive
enhancement over a page that is already done, and if it does not look good enough
it is deleted in one commit with nothing lost.

**Production fit.** The existing `tools/blender/` scripts already build everything
this needs, in one room, with both pots present. The new work is an export script
setting `tone` on every object and baking the vertex attributes, a `gltfpack` build
step, a 30-line change to `toon.js`, and a projected-rect handoff plus a clipping
hook in `camera-rail.js`, whose splines, smoothstep, dirty loop, `pause()`,
`project()` and keyframe capture are all already written.

And the production advantage worth naming, given that pre-rendered Cycles frames
were abandoned: **`common.py` records that EEVEE cannot open a GPU context under
WSL, so the flat look cannot be previewed in Blender at all.** Under this direction
that stops mattering, because the look lives in three.js. Blender renders exactly
two Cycles stills, the posters, and never renders the hero. **The look is iterated
by reloading a browser tab instead of waiting on a render pass.**

## 9. WHAT THIS DIRECTION IS BAD AT

**452 KB and a GPU context, to sell an offline, no-account, no-server app.** The
product's pitch is restraint and this would be the least restrained thing on the
page. That dissonance is real and I will not argue it away: **a reader who notices
it is reading the page correctly.** The only honest defences are that nothing is
loaded for reduced-motion or no-WebGL readers, that the text and both CTAs are
readable at 12 KB of HTML with the 3D stripped, and that 452 KB is roughly half
what the reference spends for a scene twice the size.

**Battery and thermals on a mid-range Android**, which is exactly this audience. A
phone warming up while its owner reads about plant care is a bad joke. Mitigations
exist, **but I have not measured any of this on a real device and I would want to
before shipping.**

**The spatial argument is unavailable to a screen reader.** Captions as real DOM
at `opacity: 0` gets the whole story in document order, but the *picture's*
argument lives only in pixels. Each beat needs one extra visually-hidden sentence
describing what the frame shows. That is honest alt text for a film and **it is
still a worse experience than the sighted one. There is no version of this where
that gap closes.**

**A programmer-built 3D scene reads as a programmer-built 3D scene.** The band
shader hides more of this than a photoreal render would, which is the main reason
this technique is the right one here, **but it hides it rather than fixing it.**

**No art direction without a code change.** Every tweak is an edit and a reload,
there is no file a designer can open, and the `.blend` deliberately does not exist.

**The section is the one idea here nobody asked for.** It is the reason to pick
this direction and it is also the single biggest risk in it. **If it reads as a
textbook rather than as a Tuesday night, the hero's best shot is also its worst.**
