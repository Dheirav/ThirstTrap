# ThirstTrap — design research

Researched 2026-09-08. Roughly half of this is amendments to `UI-SPEC.md` §8.

Marks: **[V]** verified from a fetched source · **[M]** measured from this
codebase or from font binaries · **[J]** design judgement.

---

## 1. What the code does today, measured

**Dark cards are the wrong hue. [M]**

`DarkScheme` sets `surface` and `background` but none of
`surfaceContainerLowest/Low/Container/High/Highest`, `surfaceDim`,
`surfaceBright`, `outlineVariant`, `scrim` or the error roles. `darkColorScheme()`
fills those from M3's baseline neutral palette, which is purple-tinted. M3
`Card` takes its container from a `surfaceContainer*` role.

| | L | C | H |
|---|---|---|---|
| our `background` `#0F1511` | 18.8 | 0.012 | **155.9°** green |
| M3 default `surfaceContainerLow` `#1D1B20` | 22.7 | 0.010 | **303.7°** purple |
| M3 default `surfaceContainerHighest` `#36343B` | 33.0 | 0.012 | **298.8°** purple |

~145° of hue error on every card. Card-vs-background contrast is **1.08:1**,
below perceptual threshold, and `cardElevation(1.dp)` draws only a shadow,
invisible on a near-black ground. **Dark mode currently has no visible card.**

`#0F1511` is the exact hue-155 equivalent of M3's `Neutral6` `#141218` — step
one of the ramp was built correctly, then abandoned.

**No lightness hierarchy in dark mode. [M]**
OKLCH L of the dark foreground roles: `onSurface` 91.4, `tertiary` 83.7,
`onSurfaceVariant` 82.6, `secondary` 82.4, `primary` 81.7. Four of five inside
2 L-points. Hue does all the differentiating, lightness none — the mechanical
reason a card reads as an undifferentiated wall.

**The type scale is squashed into 3sp. [M]**
`bodySmall` (12sp) used 40×, `bodyMedium` (14sp) 37×, `titleMedium` 22×,
`labelSmall` 9×; `headlineSmall` 6×, `displayLarge` 1×. A `PlantCard` stacks up
to **seven** text rows, five at 11–12sp in the same grey.

**No shape system. [M]** Eight hardcoded radii — 4/5/6/7/8/10/12/24dp — and
zero references to `MaterialTheme.shapes`. 5, 6 and 7 are arbitrary.

**No animation anywhere. [M]** A grep for every Compose animation API over
`app/` and `core/` returns nothing. Coil's crossfade is off by default and not
enabled. `UI-SPEC` §7 requires "Still wet" to get *the same confirmation
animation* as "Watered"; both get none, so the parity is accidental.

**Icons are on a deprecated artifact. [V]** All icons are `Icons.Filled` from
`material-icons-extended`, which Google documents as *"no longer maintained or
recommended… can also increase the build time of your apps significantly"*.

---

## 2. References worth stealing from

### Vera — the closest match to this app's brief [V]

Bloomscape's plant diary, dead Feb 2024, already cited in
`plant-tracker-requirements.md` as the data-lock-in cautionary tale.

Warm cream canvas `#fbf6f0`, secondary tint `#f4ecdf`, muted sage header, dark
forest text. **The only app in the set pairing a display serif with a sans body**
— serif on "Log Activity", "Journal Entry" and headlines; humanist sans for
labels. Thin monoline outline icons inside hairline circles. Editorial layout:
full-bleed photo, headline on a tinted panel, hairline rules, **zero card
shadows**.

**It had no IAP and no subscription, ever** — it was a marketing channel for
plant sales. That is *why* it looks like that: no funnel to design around.

**Vera is the reference, not Planta.** Planta is beautiful and is a subscription
product; Vera was a diary.

### Planta — one idea worth taking outright [V]

From its live production CSS: canvas `#e7edde` pale warm sage (not white), text
and CTA `#234823` deep forest, CTA pairing `#e0ffc2` on `#234823`. Typeface is
GT America. Flat — separation by white-on-sage, no shadows. **Progress is a ring,
not a bar.**

The best idea in the category: **semantic task colours that name the activity,
not the state** — water `#2c3f54` slate, soil `#59491c` brown, pruning `#55624b`
olive, light `#f2e6bb` straw. Colour carries *meaning*, so it never has to carry
*urgency*.

### Gentler Streak — the closest thesis-match anywhere [V]

2024 Apple Design Award, Best Social Impact. A fitness tracker built on exactly
this app's premise: measurement over guilt.

- *"soft, cozy, warm, homey"* achieved through *"illustrations, transitions, and
  animations"* — motion named as a carrier of the feeling.
- Copy: *"supportive but not cheesy, motivating but not fake-hyped"*.
- **"Translating stats into words"** — presenting data as a status, not a metric.
- Progress relative to your own history, never an external benchmark. Rest days
  log without resetting anything.

"Water in about 2 days" is already this move. "62% toward watering" is not.

### Oura's three tiers [V]

(1) abstract at-a-glance indicator, (2) mid-level focused metric, (3) precise
interactive view. Their CEO: *"people just want to be told the meaning. They
don't want the data, they want the insight."* Maps onto depletion bar →
prediction sentence → weight chart. Never show tier 3 on tier 1's surface.

---

## 3. Calm vs cozy — they contradict, and calm wins

**Calm technology** is a citable framework [V]: *"technology should require the
smallest possible amount of attention"*, *"can communicate but doesn't need to
speak"*, *"should work even when it fails"*. It publishes a notification
vocabulary ranked by intrusiveness — haptics and trend graphs at the quiet end,
popups and timed triggers at the loud end. Calm Tech Certified™ (2025) requires
**all but the most crucial notifications off by default**.

*Our `ReminderNotifier` uses `IMPORTANCE_DEFAULT` on the checks channel, which
makes a sound. Worth a deliberate decision.* [M]

**The sharpest finding:** cozy-game UI recommends **bounce and overshoot**;
calm design forbids exactly that. **Motion is where the two vocabularies
contradict.** Take calm's motion, borrow cozy's colour temperature and texture.

What survives from cozy: warm-shifted neutrals (never pure `#FFF`/`#000`); grain
at the threshold of invisibility — *"if users notice it, it's probably too
much"*; 200–400ms durations, ease-out entering. **Do not buy softness with
contrast** — neumorphism is an accessibility liability. [J]

**Where it tips into twee, with a citation.** *"Dark Patterns of Cuteness"*
(Springer) argues cuteness's association with vulnerability stimulates trust
responses and can be *"operationalised to inspire uncritical acceptance"*.
**Cuteness is a persuasion channel, not neutral decoration.** For an app whose
ethical position is "no manipulation", a mascot is not a free choice. **No
mascot.**

**The anti-pattern has a paper too.** Mogavi et al. (2022) on Duolingo
gamification misuse: named failure categories include negative well-being
effects, competitiveness-driven misuse, and **self-recrimination**. A quoted
user: *"I felt that I was cheating, but simply did not care. I am nothing
without my streak."* Forest *"frames the interaction not as wasting time, but as
committing plant murder"*. Finch's counter-move: the bird **never dies**.

The anti-streak cohort all **change the unit of accounting** from the
consecutive day to the rate — which mathematically removes the loss-aversion
cliff. This app already does that by accounting in *pot weight*. Worth saying so
somewhere in the app.

**Biophilic design, honestly.** Most of it is agency marketing; the report's own
caveat is that stress recovery is 1.6× faster with a real window than a
simulation. The one screen-usable number: mid-complexity fractals at **fractal
dimension D ≈ 1.3–1.5** gave ~60% better stress recovery by skin conductance.
That is a spec for an empty-state illustration — a branching form, not a potted
plant. [V]

---

## 4. Typography

Measured from the variable TTFs with fontTools. [M]

**The finding that decides it: Fraunces has no OpenType numeral features at
all** — its feature set is `case, kern, liga, rvrn, ss01`, digits proportional
with advances 1024–1461 units. **Fraunces numbers cannot be made to align.**
Same trap in DM Sans, Commissioner and Instrument Serif. This app shows grams,
percentages, day counts, ml and chart labels — that disqualifies four otherwise
attractive faces.

| Family | Axes | x-height/em | Default digits |
|---|---|---|---|
| **Newsreader** | opsz 6–72, wght 200–800 | 0.426 | **tabular** |
| **Hanken Grotesk** | wght 100–900 | 0.493 | **tabular** |
| Source Serif 4 | opsz 8–60, wght 200–900 | 0.475 | tabular |
| Literata | opsz 7–72, wght 200–900 | 0.507 | proportional, has `tnum` |
| Fraunces | SOFT/WONK/opsz/wght | 0.482 | **proportional, no `tnum`** |
| Lora | wght **400–700 only** | 0.500 | proportional |
| Roboto (current) | wdth/wght | 0.528 | tabular |

**Recommendation: Newsreader (display) + Hanken Grotesk (body).** [J] Both
tabular by default, so no `fontFeatureSettings` plumbing anywhere and no risk of
a number jittering in a chart label. Newsreader's low 0.426 x-height reads calm
and editorial at 22–28sp; its `opsz` axis means a plant name at 22sp and a chart
label at 11sp use genuinely different drawings.

Alternatives: Source Serif 4 + Public Sans (warmest safe option); Literata +
Inter (widest range, but Inter is the most "every startup in 2023" choice
available). Avoid Outfit (cold, 0.460 x-height), Lora (no light weights),
Young Serif / Instrument Serif (single-weight statics — no hierarchy possible).

**Is a custom face worth it?** Trade Me measured their brand grotesque against
Roboto and concluded it was *"probably not distinctly visually different to most
users"* [V]. **That holds for swapping one sans for another and collapses for a
serif**, which is unmistakably not the system font. Vera and Blossom both use a
serif *only* on plant names and section headings.

**Ship it bundled, not downloadable.** [V] Three independent disqualifiers for
the downloadable-fonts API: variable fonts are not supported (statics only); it
requires Google Play Services, and this app is offline and private; new fonts
lag the API by months. One variable TTF replaces 6–9 statics. `minSdk 26` is
exactly the floor for `FontVariation.Settings`, so no fallback branch is needed.

---

## 5. Material 3 Expressive — take two things, refuse the rest

Announced May 2025; 3 years, 46 studies, 18,000+ participants; claimed +34%
perceived modernity, +32% subculture, **+30% "rebelliousness"** [V]. Read those
metrics: Google optimised for modernity, rebelliousness and subculture. Not our
values. The transferable half is *"key elements spotted up to 4× faster"*, driven
by containment and scale rather than colour or motion.

*We are on Compose BOM 2024.12.01 → material3 ~1.3.1; 1.4.0 is stable.* [M]

**Take:** the **emphasized type scale** (30 styles — hierarchy through weight
rather than colour or size; free, silent, no motion cost). And
**`MotionScheme.standard()` set explicitly** — do *not* call
`MaterialExpressiveTheme`, whose purpose is to install lower-damped springs with
visible overshoot app-wide.

**Refuse:** expressive springs (overshoot reads as excitement; a diary of slow
biological processes should not overshoot), the vibrant palette, shape morphing,
`WavyProgressIndicator`, FAB menus.

**Context, and it is on our side:** NN/g's teardown of Apple's Liquid Glass
found insufficient contrast over images, "text on top of text", tap targets
below minimum, and animation that *"serves no functional purpose"*. Both of the
world's largest design systems shipped an expressiveness push in 2025 and both
drew the same criticism. Refusing it is not being behind. The trend that *is*
ours: texture, grain and warmth as a reaction to AI-generated visual sameness.

---

## 6. The twelve changes, ordered by leverage

Values generated in OKLCH and contrast-checked. [J] from [M] data.

**1. Complete the dark colour scheme.** Highest leverage by a wide margin.
**DONE 2026-09-08** - both schemes, all roles, plus a measuring test suite in
`core/ui/src/test`. See handover D13. Two contrast failures fell out of doing
it: light `tertiary` at 4.02:1, and light `primary` at 3.91:1 *on a card*, a
failure that only existed once cards became visible.

```kotlin
surfaceContainerLowest  = Color(0xFF090D0A)
surface / background    = Color(0xFF0F1410)
surfaceContainerLow     = Color(0xFF151B17)
surfaceContainer        = Color(0xFF1B221E)
surfaceContainerHigh    = Color(0xFF242C27)   // 1.30:1 vs background
surfaceContainerHighest = Color(0xFF2E3731)   // 1.51:1 — perceptible
outlineVariant          = Color(0xFF414A44)
outline                 = Color(0xFF79837C)
```
Also set `surfaceDim`, `surfaceBright`, `scrim`, `inverseSurface`,
`inverseOnSurface` and the four error roles — anything unset inherits purple.
Light mode likewise. Light `tertiary #C0603F` measures **4.02:1** on `#F7FBF3`,
failing AA — darken it or restrict it to fills.

**2. Build a lightness ladder; stop using colour for hierarchy.**
**DONE 2026-09-08**, with the values corrected. See the note below on which
surface a contrast figure is quoted against.
`onSurface #DAE0DA` (L 90, 12.1:1) · `onSurfaceVariant #ACB3AD` (L 76, 7.6:1) ·
a third dim tier `#8D948E` (L 66, 5.2:1, still AA). Drop dark `primary` from
`#7DDB9C` (L 81.7) to **`#81C394`** (L 76, 7.9:1) — currently it is the
brightest object on a near-black screen and the depletion bar makes it a
full-width glowing stripe. This is what "muted but alive" actually means.

**3. Cut the plant card from seven text rows to four.** **DONE 2026-09-08** -
three rows in practice, since the bar only appears for a weight-tracked plant.
Row 1 plant name `titleMedium`. Row 2 **the answer** — "Water in about 2 days" —
`bodyMedium`. Row 3 the state object. Row 4 everything else on one line in the
dim tier. Row 2 is the Gentler Streak move and the row people actually read.
Move cadence to the detail screen.

**4. Systematise shape.** **DONE 2026-09-08** - eight radii to five roles, at
most three on screen. Set `MaterialTheme(shapes = …)` once: card 12, thumb 8,
chips 4, sheets 16, FAB 28. Three radii on screen, not eight.

**5. Replace elevation with a hairline.** **DONE 2026-09-08** - measured on
device at `#414A44` dark and `#C1CAC0` light, 1dp. `elevation = 0.dp, border =
BorderStroke(1.dp, outlineVariant)` — `#414A44` measures 2.03:1 on the
background. What Vera does, and calmer: shadows imply floating objects,
hairlines imply a page.

**6. Add the typeface.** **DONE 2026-09-08.** Newsreader for `displayLarge`…`titleLarge`, Hanken
Grotesk below. Set `letterSpacing = 0.sp` on body/label — M3's +0.4/+0.5sp is
Roboto-tuned and looks loose on a humanist face. Increase line height: 12/18 and
14/22.

**7. Add motion — to both log actions simultaneously.** **DONE 2026-09-08**, including actually implementing reduce-motion, which `X4` had claimed in a codebase with no animations. `animateFloatAsState` on
the depletion fraction (400ms); `AnimatedContent` on the prediction line; **one
identical 220ms scale-and-fade** on the tapped icon for *both* droplet and check,
plus the existing haptic; Coil `crossfade(220)`. Gate on reduce-motion, which
`FEATURES.md` X4 already claims. No overshoot.

**8. Rethink the depletion bar as a state object.** **DONE 2026-09-08** — a ring around the thumbnail, with the trigger as a tick rather than a percentage. A full-width bar filling to
100% is the grammar of *task completion* — the one grammar this app exists to
avoid. Prefer a **ring around the plant thumbnail** (Planta and Oura both use
rings; a ring reads as state, and costs no vertical space in a card that has too
many rows). If keeping the bar: 40–56dp wide inline, not full-width.

**9. Adopt semantic event colours.** **DONE 2026-09-08.** Planta's best idea — muted hues named after
the activity, all equal in lightness so none reads as an alarm. Lets the
timeline be scannable by colour without any colour meaning "bad".

**10. Move off `material-icons-extended`** **DONE 2026-09-08** — APK 21.2MB → 14.2MB. to Material Symbols Rounded,
outlined, ~16 XMLs. Reserve *filled* for exactly one thing — the droplet in its
logged state. That also fixes a real ambiguity: `TouchApp` and `WaterDrop`
currently sit side by side, both filled, both `primary`, with nothing saying
which is the restraint action.

**11. Grain, and a branching empty state.** **DONE 2026-09-08** — measured D = 1.441 by box-counting the render. A 2–4% alpha noise overlay on large
dark surfaces — kills dark-mode banding, adds a paper quality, costs one tiled
drawable. For the empty state: a single-weight monoline **branching form at
fractal dimension ~1.3–1.5** in `outlineVariant`. No pot, no face, no mascot.

**12. Photography: bigger.** **DONE 2026-09-08** — dashboard thumbnail 56→88dp
with a hairline inset, and a full-bleed 300dp hero on plant detail running under
the status bar with the name and chips on a scrim. Plant detail gets a full-bleed hero under the
status bar with name and chips on a scrim — `UI-SPEC` §4 already specifies a
collapsing header. Dashboard thumbnail to 64dp with a 1dp `outlineVariant` inset
border; a bright photo on a near-black card has a hard cut-out edge and a
hairline resolves it.

### A correction to change 2, found by implementing it

The dim tier was specified as `#8D948E` at "5.2:1, still AA". **That figure is
against the background.** The text it carries sits on a *card*, and on
`surfaceContainerHighest` the same colour measures **3.92:1** - a fail, measured
on the device after shipping it.

The same mistake appears in change 1's light `primary`, caught there only
because completing the surface roles is what created the card to fail against.
A contrast ratio quoted against the wrong surface is not a contrast ratio, and
in this palette the card is always the harder surface.

Corrected ladder, every tier AA on a card:

| | dark | L | on card | light | L | on card |
|---|---|---|---|---|---|---|
| `onSurface` | `#DAE0DA` | 90.0 | 9.07:1 | `#191D18` | 22.5 | 13.22:1 |
| `onSurfaceVariant` | `#BCC3BD` | 81.0 | 6.77:1 | `#414942` | 39.6 | 7.21:1 |
| `outline` (dim) | `#9EA59F` | 71.4 | 4.83:1 | `#5E665E` | 50.1 | 4.60:1 |

The AA floor on the card is what sets the spacing: it caps the dark ladder at
roughly 19 L-points of usable range, so ~9.5 between tiers is the most that is
available, not a free choice. The palette test now checks `outline` at AA too,
since it is text.

### Two things not to do

**Do not add a warm cream "cozy" canvas to dark mode.** Vera's and Planta's
warmth lives in *light* mode. The dark-mode equivalent is hue-consistent
low-chroma surfaces plus grain plus a serif — not a lighter background.

**Do not leave long-press-for-detail on the droplet undiscoverable.** It is
structurally the same failure as Planta's most-cited design complaint — a
paywall firing from a control that was never marked as locked. A control whose
affordance promises one outcome and delivers two.

---

## Not verified

Greg's, Vera's, PictureThis's and Plantum's typefaces. Whether Planta's in-app
dark mode is premium-gated, or whether its runtime uses the same tokens as its
marketing site (values matched screenshots to within a hex digit — strong but
circumstantial). The Calm Tech 81-point list. That Android 15/16 ships system
Roboto as a variable font — no Google source found for this.
