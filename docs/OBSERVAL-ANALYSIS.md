# observal.io, taken apart

A six-lens review of https://observal.io/, run as a council: art direction,
narrative and copy, motion and choreography, information architecture, technical
build, and the render pipeline. Each lens worked from one shared evidence set
(nine desktop screenshots across the scroll, five mobile, the rendered DOM, a
computed type scale and palette, the network manifest, the page copy) and most
then probed the live site directly.

Captured 2026-10-02. Everything below is measured unless marked as inference.

---

## 1. Correction first

I had earlier described this site as hand-drawn 2D vector illustration. That is
wrong, and it matters, because a decision was made partly on it.

**The hero is a WebGL 3D scene authored in Blender.** The element census of the
rendered page is one `<canvas>`, eleven static SVG icons, four images, zero
video. The canvas declares itself `data-engine="three.js r186"`.

The scene arrives as `/_astro/scene.Czob-XJl.glb`, 665 KB, Draco-compressed,
exported by `Khronos glTF Blender I/O v5.2.40`. Node names read like a set list:
`dome`, `rib`, `slit_rail`, `parapet`, `desk_top`, `laptop_base`, `screen`,
`trackpad`, `sofa_seat`, `rug`, `mug`, `pot`, `leaf`, a character mesh called
`maya`, and a `skyline` backdrop.

The file contains **58 meshes, 133,004 triangles, and zero materials, zero
images, zero animations, zero cameras.** Geometry only. Everything else is
decided at runtime.

So the instinct to go to Blender was right. What was wrong was the half I chose:
I pre-rendered frames. They ship the geometry and render live.

---

## 2. The render pipeline

This was the specific question asked, so it gets the most detail.

### How the flat look is produced

The art direction is a shader, not a drawing. The trick is worth stating plainly
because it is reusable:

- The directional "moon" light is coloured **pure blue `(0,0,1)`**.
- The lamp spotlight is coloured **pure red `(1,0,0)`**.
- A `MeshLambertMaterial` patched through `onBeforeCompile` then reads
  `outgoingLight.b` back as moonlight intensity and `outgoingLight.r` as lamp
  intensity, quantises the blue into three bands by two thresholds, and mixes
  toward amber where red exceeds 0.12.

The renderer is being used as a two-channel lighting solver, and all colour is
decided in a band lookup. That is how you keep real shadows, occlusion and
perspective while the result still reads as illustration. `customProgramCacheKey`
collapses thirty-plus materials into **twelve linked programs and twenty-four
shader compiles** for the whole page.

### How a frame is produced

Scroll produces one number. That number is everything.

```
i = (seg + smoothstep(frac)) / nSegments
camera.position.copy(posCurve.getPoint(i))
camera.lookAt(atCurve.getPoint(i))
camera.fov = lerp(kf[n].fov, kf[n+1].fov, t)
```

Two `CatmullRomCurve3` splines, one for position and one for look-at, with eight
keyframes in the hero and five in the finale, plus a field-of-view lerp.

**The easing lives in the path, not in the scroll mapping.** Scroll maps linearly
onto the spline, but each segment is smoothstepped independently, so the camera
decelerates into and accelerates out of every waypoint. Nobody authored a hold.
The holds are a property of the parameterisation. This is the single most
elegant idea in the build.

### The canvas never moves

Across fifteen sampled scroll positions, `#stage3d` has identical transform,
opacity, class and inline style. It is never touched. All motion happens inside
the GL context.

The measured consequence, from CDP `Performance.getMetrics` deltas over five
seconds of wheel scrolling:

| Zone | Layouts | Layout ms | Style recalcs | Style ms |
|---|---:|---:|---:|---:|
| Hero 3D scrub | **0** | **0.0** | 7 | 2.1 |
| Dashboard chapters | 39 | 66.5 | 453 | 98.2 |
| Finale 3D scrub | 10 | 3.7 | 77 | 14.8 |
| Plain DOM sections | 1 | 0.7 | 22 | 4.8 |

The most cinematically ambitious part of the page is the cheapest part for the
layout engine. The scripted demo, which looks far simpler, is where the layout
cost lives, because every cursor hop calls `getBoundingClientRect()` on elements
inside a cross-document iframe.

### What actually moves in the DOM

Nine elements during the entire hero, all of them captions, plus ten harness
labels whose positions come from `Vector3.project(camera)` rather than from
independent animation. There is no parallax layer stack. Depth comes from the 3D
scene, not from stacked DOM.

### The match cut is solved arithmetically

The final hero keyframe is computed, not authored:

```
dist = min(0.30/2 / (tan(19°) · aspect), 0.19/2 / tan(19°)) × 0.94
```

That places the camera exactly far enough from the 0.31 m by 0.19 m screen quad
for it to overfill the viewport by about six percent, and it is recomputed on
every resize. Then a fixed opaque layer cross-dissolves over the canvas across
**three percent of the 800vh track, about 189 px of scroll**, and at opacity
above 0.99 the WebGL loop is stopped outright.

So "zoom into the laptop and the screen becomes the real UI" is a 3D dolly sized
to end frame-filling, followed by a plain opacity crossfade of two aligned
full-screen layers. No morph, no shared-element transition, no clip-path.

### Cost control

- Dirty-flagged render loop: rAF always, `renderer.render` only when progress changed.
- Ambient motion throttled by timestamp, not frame: 41 ms while active (24 fps), 120 ms otherwise (8 fps).
- `renderer.pause()` when an opaque DOM layer covers the canvas. Measured idle draws: hero 5 to 7 frames in 3 s, finale 15, **dashboard 0, static sections 0**. Genuinely stopped, not throttled.
- `pixelRatio = min(devicePixelRatio, 1.5)` with `antialias: pixelRatio < 1.5`.
- Zero textures in the GLB, so no image decode, no mipmap memory, no upload stall.

### The one large miss

**The 2048 by 2048 shadow map is re-rendered every frame.** `shadowMap.autoUpdate`
is left at its default of true, and the light genuinely moves during the descent,
so it cannot be cached as written. That is **4.19 Mpx of depth per frame against
1.30 Mpx of colour at dpr 1, a ratio of 3.2 to 1**, and 58 of roughly 110 draw
calls per frame exist only to feed it.

### The product demo is a different pipeline

`/dash/index.html` is a **1,066,026 byte static HTML document with zero script
tags**: 14 `<template id="v-...">` views, 87 `data-t` hook attributes, inlined
Tailwind, two self-hosted variable fonts. It is loaded same-origin in an iframe
and driven entirely from the parent, which swaps views with
`root.replaceChildren(template.content.cloneNode(true))`.

A fake cursor flies quadratic Bézier paths with a perpendicular control offset of
twenty percent of the travel distance, so it arcs and reads as a hand. A click
fires three cues at once: the cursor scales to 0.82 for 70 ms, a ripple expands
and fades over 450 ms, and the target gets `data-pressed` for 140 ms. Typing is
per-character with jitter from a **seeded** LCG, so the randomness is reproducible
across loads.

The iframe is laid out at a fixed logical size and CSS-scaled,
`c = clamp(min(vw/1600, vh/940), 0.75, 1.35)`, so one layout serves every desktop
size. **Below 768 px, `c = 1`** and the app's own responsive layout takes over, so
mobile gets a real mobile dashboard rather than a shrunken desktop one.

### Scroll selects, time plays

The camera flight is **scrubbed**, so the reader owns it. The five demo chapters
are **triggered**: entering a 900 px band restarts a paused GSAP timeline that
then plays on wall-clock. Reverse entry is defined as `progress(1)`, the end
state, because a scripted demo has no coherent backwards.

The reasoning is that half a click is incoherent. Scrub what is spatial, trigger
what is temporal.

### Reverse-scroll correctness is testable, and passes

Sixteen positions sampled going down, then the same sixteen coming up. **Every
scalar matched exactly**: stage opacity, finale copy opacity, active chapter, dot
count, body classes, all ten label opacities.

This works because the scene is a pure function of one number with nothing
accumulated per frame. That makes reverse correctness an assertion you can write,
rather than something you hope for.

### To rebuild it

1. Model the set in Blender as one scene, one object per prop, no materials, no lights, no cameras, no animation. Put `kind` and `tone` strings in each object's custom properties. Export GLB with Draco.
2. At load, traverse and read `userData.tone`, assign a shared material patched to quantise lighting into bands. Light with one blue directional and one red spot; read the channels back in the fragment shader.
3. Bake every static mesh into world space, strip to position and normal, group by material, merge into one mesh per material.
4. Draw flat detail (boards, posters, screens) on offscreen 2D canvases at 1200 to 1650 px and upload as textures. If a 2D layout implies 3D objects, have the generator return coordinates and build geometry from them.
5. Camera as data: keyframe arrays, two Catmull-Rom curves, per-segment smoothstep.
6. Solve the handoff distance from the quad size, fov and aspect. Recompute on resize.
7. Dirty-flag the loop, pause it when covered, throttle ambient motion by timestamp.
8. Prerender frame 0 to a 16 KB webp. Preload it, put it fixed behind the canvas, fade the canvas in on ready. It is also your no-WebGL fallback with one CSS rule.

---

## 3. What all six lenses agreed on

Convergence is the strongest signal in a council, because the lenses did not talk
to each other.

### Text over illustration is not legible. Three lenses, independently.

The entire contrast strategy for the narrative captions is
`text-shadow: 0 1px 0 var(--ink)`. A one-pixel hard offset is a typographic
flourish; it provides no measurable protection against a mid-value field.

Measured three different ways, all agreeing:

- Art direction, median background behind each caption: **3.42:1**, 4.14:1, 4.56:1, against 12.07:1 for the same component over plain ground.
- Technical, per-glyph against the local background with a 3 px dilated ring: **11 to 41 percent of glyph area below 4.5:1**, up to 33 percent below 3:1, **minimum 1.70:1** where a caption crosses a near-white book in the room scene.
- Motion, mechanism: "decoration, not contrast".

It is worse on mobile, because captions sit at `bottom: 9vh`, which is the
busiest region of a portrait frame.

The damning detail is that the site solves this correctly elsewhere. `.install`
sets a solid ink background with a cream border, so the button holds 12.11:1 no
matter where the camera points. The same reasoning was never carried to the text
that carries the story.

### The handoff is a dissolve dressed as a match cut

The six percent framing margin that makes the camera distance safe at every
aspect ratio also guarantees the illustrated screen is still keystoned and inside
a visible bezel when the crossfade begins, while the live UI is axis-aligned and
roughly fifteen percent larger. At scroll speed it reads as a soft morph. Parked,
it is plainly a dissolve, with three overlapping copies of the same page at
different scales visible mid-fade.

The reverse handoff is worse, and this one is a content bug rather than a framing
one: **the 3D screen texture is fixed on the Agents grid while the walkthrough
ends on the insights report**, so scrolling back dissolves between two different
pages. A scale mismatch under motion is forgivable. A content mismatch is not.

### Long stretches with nothing to read

- **2,306 px, about 2.6 viewports**, in the finale between the dashboard leaving and the headline arriving. Just a dashed line and stars.
- `desktop-068.png` and `mobile-070.png` each contain **zero words**.
- On a 20-viewport page with no progress indicator, a text-free screen is indistinguishable from a broken page or the end of the content.

### The assembled payoff frame lives for 380 px

The finale builds for thousands of pixels toward one image: headline, ten
labelled stars, trails, dome. Then `onLeave` strips the labels while the headline
is still at 0.96 opacity. The shot the whole act exists to produce occupies
**0.4 of a viewport**.

---

## 4. The design system

The tokens, verbatim from their `:root`:

```
--ink:#0e2a33   --ink-2:#143843   --teal:#018399   --teal-dark:#01505e
--cream:#efe6d2 --paper:#f6e7c8   --amber:#f2a43a
--display:"Bricolage Grotesque"   --mono:"Martian Mono"   --pixel:"Silkscreen"
--gutter:clamp(16px, 4vw, 56px)
```

Usage counts in their own CSS: `--cream` 34, `--amber` 21, `--teal` 5. Teal
barely exists outside the logo and the starfield. Seven colours, three fonts plus
Caveat in exactly one rule, one spacing primitive.

### Four rules worth stealing, all measurable

**Give the accent an area budget in percent.** Amber pixels counted across all
nine desktop frames: maximum **0.090 percent of the viewport**. At that density
every amber mark is a pointer, so the eye reads amber as an instruction. Past
roughly one percent it becomes decoration and stops pointing at anything.

**Set a saturation ceiling for the art and keep the brand above it.** Nothing in
the illustration exceeds saturation 0.60. Amber is 0.76, teal is 0.99. The art
can use as many hues as a scene needs (sage, oxblood, dusty blue, mustard)
because none of them can out-shout the brand. Buy variety in hue, never in
saturation.

**Hue-lock the art to the brand's hues.** Top fills sample at hue 30 to 35 and
hue 194 to 203. Brand amber is hue 35, brand ink hue 194, brand teal hue 189. The
art and the chrome are the same two hue families at different saturations, which
is why a muted brown room sits under a saturated teal UI without looking pasted
on. There is no third hue family anywhere.

**Leave a gap in the type scale.** Sizes present: 9, 10, 11, 12, 13, 13.5, 14, 15,
16, 17.5, 18.72, 21, 23, 25, then nothing until 44.64, 60.48, 68, 72, 244.8. A
1.8x hole between the body band and the display band means two pieces of text can
never be close enough in size to make a reader ask which outranks which. The ramp
is driven by `clamp()` on viewport width, so the gap holds at every size rather
than collapsing at one breakpoint.

### Tracking is directional and the direction means something

Negative tracking scales with size: `-0.045em` at 244.8 px down to `-0.01em` at
21 px. Positive tracking goes with uppercase: `+0.06em` on the pixel labels.

Negative plus mixed case plus weight 800 plus cream is **the human voice**.
Positive plus uppercase plus Silkscreen plus amber is **the system voice**. The
two never meet, so a reader learns within one screen that an amber spaced-out
caps line is a machine-generated label and stops reading it as prose.

### Four typefaces, zero overlap in job

Bricolage Grotesque is things a person said. Martian Mono is things you type or
click. Silkscreen is labels the system emits. Caveat is annotated by hand. Four
faces is normally a smell; it survives here because each owns a category and
never borrows another's.

### The illustration rules

- **No contour lines anywhere.** Form separates by value step alone.
- **Shadows are straight-edged polygons**, not gradients. Gradients are reserved for emitted light only.
- **Edges are deckled**, with small irregular notches, applied uniformly so it works as a texture rule rather than an effect on one asset.
- **Two to three values per object, maximum.**

Because the art has no outlines, the only outlines on the page are interactive
objects. A line means "you can touch this". Border widths are 1 px for structural
dividers and 1.5 px for object edges, and nothing else.

### Grain, and why it works

```css
.grain {
  position: fixed; inset: -50%;
  background: url(/grain.png);
  mix-blend-mode: overlay;
  opacity: .11;
  animation: .9s steps(3, end) infinite;
}
```

Three mechanisms. **Overlay blend** modulates existing values instead of laying a
flat grey veil, so it bites in the mid-tones and nearly vanishes in the near-black
ground. **`steps(3)`** gives three discrete frames, so it reads as film stock
rather than a shader. **Fixed with `inset: -50%`** locks it to the viewport rather
than the scrolling art, so the whole page sits behind one piece of glass, which is
what fuses a WebGL render, a dark product UI and flat HTML into one surface.

Dropped to `0.05` on mobile, because the same alpha reads twice as strong at 3x
device pixel ratio. Stopped entirely under `prefers-reduced-motion`.

### Where the system breaks its own rules

- **Two competing horizontal axes.** Film sections hang type on the 56 px viewport gutter; document sections hang it on a centred 1180 px container, starting at 189 px. The shift is 130 px and one frame holds both axes 600 px apart vertically, so the left margin visibly jumps mid-scroll.
- **Opacity does double duty** as both reveal state and emphasis. One frame has two headlines of identical rank, same face, same weight, 5.4x apart in contrast, visible together. Every scroll position is a position someone can stop on.
- **Silkscreen below its minimum size.** Used at 10 px and 9 px at 0.7 alpha. It is bitmap-derived, so its stems fall under one device pixel; the word "COPY" renders as glyph mush.
- **The dashboard abandons the palette** for about a quarter of the page. `#0c0d0f`, a neutral near-black sharing no hue with `--ink`. A near-black carrying a trace of hue 194 would have been a join instead of a cut, at no cost to the product's own design.

---

## 5. The narrative

### The beats

| # | Kicker | Line |
|---|---|---|
| 1 | `01:47 am` | "Three teams wrote the same code review skill this quarter. None of them knew." |
| 2 | `Claude Code · Codex · Cursor · Kiro · Goose` | "Her company runs all of them. Every team keeps its own copies." |
| 3 | `Maya, platform team` | "She decided that was the last time." |
| 4 | (none) | "Every skill, MCP and agent her team uses now lives in one place." |

### What works

**It opens on an event, not a condition.** A specific count, a specific artefact,
a specific window, then the pain carried by a four-word sentence that names
*ignorance* rather than duplication. The waste is not the second copy, it is that
nobody could have known. Compare the sentence nobody wrote: "teams waste time
duplicating work."

**The clock in the eyebrow.** The kicker slot usually holds a category word.
`01:47 am` dates the scene, says this person is awake at 1:47 in the morning, and
sets the register with no adjectives. The corkboard in the art carries `01:47` in
the same amber, so copy and set dressing agree on a detail.

**Delayed naming.** Beat 1 has no person. Beat 2 introduces her as a pronoun.
Beat 3 finally labels her, and the kicker does the naming so the prose never
stops for an introduction.

**Long sentence, then short, as the base unit.** "Three teams wrote the same code
review skill this quarter. **None of them knew.**" The length drop makes the
second land as a verdict. Same rhythm every time, which is why the page reads as
having one author.

**Vocabulary discipline.** Verbs across the whole page: wrote, knew, keeps, lives,
lands, pull, ship, fix, prove, run. No empower, seamless, unlock, leverage. The
jargon budget is spent entirely on the domain and none of it on marketing.

**The closer inverts the opening pain into an asset.** "Your team already built
the good stuff. Give it one place to live." Same fact, opposite polarity. The ask
becomes collection rather than remediation, which is a much smaller thing to say
yes to.

### The structural fault

**The problem the story sets up and the product being sold are two different
products.** The pain is duplication and invisibility, solved by chapters 01 to 03.
But the company name, the lede's "with an insight engine built in" and the two
most ambitious chapters are all telemetry. Nothing in Maya's night sets up a need
to know whether agents are working. There is no beat where she says "and we have
no idea which of the three is any good." Half the product answers a question the
story never asks.

**And it resolves before it demonstrates.** Beat 4 of 4 asserts the outcome, and
only then does the five-chapter tour begin. The tour has nothing at stake.

One more beat would fix the first problem. Moving the resolution after the tour
would fix the second.

---

## 6. Architecture and conversion

### Scroll budget

| Section | Height | Share |
|---|---:|---:|
| hero-track | 7,200 px | **39.2%** |
| dash-track | 4,500 px | 24.5% |
| finale-track | 3,780 px | **20.6%** |
| agentic | 1,172 px | 6.4% |
| selfhost | 920 px | 5.0% |
| closer | 635 px | 3.5% |

Narrative sections take **59.8 percent of the scroll to deliver about 98 words**.
The finale spends roughly 290 px of scroll per word; the self-host block spends
17 px per word. **A 17x spread**, allocated by how cinematic a section is rather
than by how much the reader needs from it.

### Time to comprehension

Excellent at the fold and then flat. Category, licence, deployment model, audience
and the install command are all above the fold with no scroll. But the first
concrete feature claim is **8.1 viewport-heights down**, and between the fold and
that point the visitor learns exactly one new fact.

### Conversion

**86 percent of the page has no call to action.** The last hero action sits at
about y=840. The next is at about y=17,000. That is a gap of roughly 15,900 px,
17.7 screen-heights, and it covers the entire demo section, which is the point of
peak intent.

**The primary CTA cannot be clicked.** `pipx install observal-cli` is a
`<button data-copy>` with no `href`. It cannot be middle-clicked, is invisible
without JavaScript, is not crawlable, produces no measurable conversion, and
leaves the reader with no next step once the clipboard is full.

**The most persuasive asset is sealed.** The demo iframe is `aria-hidden="true"`,
`tabindex="-1"`, `pointer-events: none`, with no link to it anywhere. It is a
real, deployed page nobody can click into or send to a colleague. Meanwhile the
stage above it takes `pointer-events: auto`, so for 4,500 px a real pointer over
real-looking buttons does nothing.

### Gaps a serious evaluator cannot close

No proof of any kind: no stars, no adopters, no logos, no version, no maintainer.
Every number on the page is seeded demo data, which anyone technical recognises
instantly. No supply-chain or governance claim, although the product clearly has
the feature (Review, Pending review, Approved badges are all visible in the demo).
No pricing or business model beyond the licence. No operating cost, sizing,
retention or upgrade story, for software that wants three databases on your
infrastructure. And the harness compatibility list, which is the hardest
qualifying question on the page, exists only in the `og:description` meta tag and
as transient animated labels that never render legibly in any captured frame.

---

## 7. Engineering

### Weight

| Group | Bytes | Share |
|---|---:|---:|
| 3D geometry (Draco GLB) | 665,463 | 42.5% |
| 3D decoder (Draco wasm + wrapper) | 204,809 | 13.1% |
| 3D runtime (three.js + loaders) | 170,321 | 10.9% |
| Google Fonts, 4 woff2 | 161,026 | 10.3% |
| Dashboard iframe assets | 142,393 | 9.1% |
| Dashboard iframe document | 108,281 | 6.9% |
| Page JS (GSAP, ScrollTrigger, Lenis) | 57,273 | 3.7% |
| Poster, grain, logo | 41,290 | 2.6% |
| HTML + CSS | 12,004 | 0.8% |
| **Total** | **1,564,950** | |

### Configuration mistakes, all cheap to fix

- **The GLB is served with no compression at all.** `Accept-Encoding: gzip` and `br` both return the full 665,180 bytes with no `Content-Encoding`. Gzip alone would save 68,169 bytes.
- **The server does not support brotli.** Brotli-11 across the four largest assets would save 104,782 bytes, or 11.7 percent of them.
- **Duplicate, conflicting `Cache-Control` headers** on every hashed asset: `public, max-age=0` followed by `public, max-age=31536000, immutable`. On a repeat visit, 10 of 16 same-origin resources came back as conditional requests at 130 to 150 ms each, on content-hashed filenames that can never change.
- **HTTP/1.1 only.** ALPN advertises `http/1.1` and nothing else, so there is no stream prioritisation.
- The static HTML is served `no-cache, no-store, must-revalidate`, which also costs back/forward cache restore.

### The 33-second dead zone

Throttled to 400 kbps with 400 ms RTT: first contentful paint 2,016 ms, load
11,955 ms, **canvas ready at 33,662 ms**.

The cause is architectural, not incidental. The hero `ScrollTrigger` is created
*after* `await createScene(...)`, and that one trigger drives both the camera and
the caption opacities. So until the GLB, the Draco decoder and three.js have all
arrived, scrolling produces nothing. At six seconds, scrolled to twelve percent,
all four beats read `opacity: 0` and the hero title had already faded out. The
screen was the bare poster with no text on it, and stayed that way for another 28
seconds. It looks broken rather than loading.

**The text narrative does not need the 3D and should not wait for it.** Create the
caption trigger immediately, attach the camera to the same trigger when it
resolves.

Compounding it: `<link rel="preload" as="fetch" href=".../scene.glb">` sits
unconditionally in `<head>`, so it fires before the WebGL2 capability test. With
WebGL stubbed out, `scene.js` and Draco were correctly skipped but **the 665 KB
GLB still downloaded**. That is 43 percent of page weight wasted on exactly the
clients least able to afford it.

### Accessibility

Better than most sites that ship a scroll film, with two real holes.

What is right: one `<h1>`, full landmark set, zero images missing `alt`, all
decorative subtrees correctly `aria-hidden`, the iframe removed from the tab order
(verified by tabbing 22 times without entering it). Crucially, **the narrative
text is real DOM at `opacity: 0`**, so a screen reader gets the whole story in
document order even though it never gets the visuals. The visual layer and the
semantic layer are two renderings of one content source.

`prefers-reduced-motion` is honoured in five independent places: Lenis is never
constructed, `getAnimations()` drops from 39 to 0, typewriter delays are skipped,
scrub goes from 0.6 to exact, and the live-editing theatrics are suppressed.

The two holes:

- **Reduced motion saves zero bytes and keeps the biggest motion.** Identical 1,564,950 bytes over 28 requests in both modes. The camera still dives through a dome and into a laptop screen, which is exactly the vestibular trigger the media query exists for. Serving the poster and skipping the 3D would be both more correct and a 1.04 MB saving.
- **85 percent of the page is dead with JavaScript off.** All the copy renders and both CTAs work, but the beats are `opacity: 0` in CSS and the three tracks keep their full 15,480 px. There is no `<noscript>` anywhere. Three lines collapsing the track heights would turn 15,000 px of frozen nothing into a short readable page.

Smaller: three footer links fall outside the four selectors defining the amber
focus ring. No skip link, on a page where tabbing from the hero CTA jumps the
viewport 16,906 px in one keystroke. `aria-live="polite"` on the chapter note can
never fire, because switching only toggles classes while all five texts stay in
the DOM. Reflow fails at 320 px (`scrollWidth` 337) because of `white-space:
nowrap` on the install button.

### Contrast, flat palette

| Pair | Ratio | AA normal |
|---|---:|---|
| cream on ink (437 uses) | 12.11 | pass |
| amber on ink | 7.26 | pass |
| slate on ink | 5.69 | pass |
| slate on panel `#2C5260` | 3.20 | **fail** |
| amber on panel `#2C5260` | 4.09 | **fail** |
| panel border on ink | 1.78 | fails 3:1 non-text |

The flat palette is in good shape. The failures are secondary text on the lighter
panel, and borders too faint to count as a boundary.

---

## 8. What transfers

Ordered by how much it would change a page.

1. **Drive every layer from one scalar.** Compute progress once, fan it out to the camera and the captions. No second timeline to drift. Scrubbing works both ways for free, and any frame is reproducible by setting one number.
2. **Create the DOM part of the sequence before awaiting the heavy asset.** One `await` in the wrong place held their entire narrative hostage to their largest file for 33 seconds.
3. **Put the easing in the path, not the scroll mapping.** `i = (seg + smoothstep(frac)) / nSegments`. Holds become a property of the parameterisation.
4. **Text that must be readable gets its own opaque surface.** A scrim, a plate, or a camera framing that guarantees a quiet region. `text-shadow` is not a legibility strategy, and three independent lenses caught them on it.
5. **Measure contrast per glyph against the local background, at peak opacity.** Classify glyph pixels, dilate about 3 px for the surrounding ring, report the percentage of glyph area below 4.5:1. A single swatch pair is meaningless over moving imagery.
6. **Give the accent an area budget in percent and the illustration a saturation ceiling.** Both are measurable, both are checkable from a screenshot.
7. **Scrub what is spatial, trigger what is temporal.** Never scrub a scripted interaction. Define its reverse as its end state.
8. **Solve match cuts arithmetically.** Compute the camera distance that frames the handoff object at the current aspect, recompute on resize, then the transition is a plain crossfade of two aligned layers, which always works.
9. **Author caption positions against the camera's rest points, not against arbitrary track fractions.** Theirs live in two coordinate systems and drift apart.
10. **Count your caption-free scroll distance and keep it under one viewport.** Longer reads as a stall.
11. **Hold the assembled payoff for at least a viewport before dismantling it.**
12. **No stretch longer than two viewports without a call to action.** Measure the pixel gap between consecutive CTAs the way you measure load time.
13. **A primary CTA needs an `href`.**
14. **Keep narrative text in the DOM at `opacity: 0`** rather than injecting it. One source gives you the screen reader experience, the crawlable document and the no-JS fallback.
15. **Treat `prefers-reduced-motion` as a budget signal, not just a motion signal.** If the reduced path transfers the same bytes and runs the same camera, you have honoured the letter and missed the point.
16. **Audit response headers as carefully as bundle sizes.** A missing `Content-Encoding` on one file cost them 68 KB; duplicate `Cache-Control` costs a round trip per asset per visit.
17. **Collapse your scroll tracks in `<noscript>`.** Three lines.
18. **Seed your randomness.** Deterministic jitter makes a piece reproducible and visual-regression testable.
19. **Label a scripted cursor with a name tag.** It costs one span and converts "why is something moving my pointer" into "I am watching someone work".
20. **Audit the shadow map before anything else in a 3D scene.** It was 3.2x the visible pixels and 53 percent of their draw calls.
