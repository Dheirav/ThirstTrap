# Five directions for the hero

Five art directors were briefed separately on the same product, the same engine,
the same measured rules, and the same five story beats. Each was told to argue
for its own direction and not to hedge toward a compromise, so these are five
different pages rather than five shades of one. Each was also required to end
with a section naming what its own direction is bad at, so you are not choosing
from five sales pitches.

Read the plans. This file is only the map.

| | Direction | Hero is | Ground |
|---|---|---|---|
| 1 | [Night almanac](1-night-almanac.md) | one continuous night, sill to table | dark |
| 2 | [Paper almanac](2-paper-almanac.md) | a page of the book the app already is | paper |
| 3 | [Live 3D toon](3-live-3d-toon.md) | one camera move, through a modelled room and into the pot | dark |
| 4 | [Instrument](4-instrument.md) | one sheet of squared paper holding a real drying cycle | paper |
| 5 | [Cross-section](5-cross-section.md) | one pot cut in half, and the water leaving it | paper |

---

## What converged, which matters more than any single plan

Three directors independently chose a **paper ground**. Three independently
reached for a **section through the pot**. Nobody was told to do either. When
directors who cannot see each other's work pick the same device, that is usually
the real idea in the material rather than a preference.

The section appears at four different scales, which is itself the decision:

- **Night almanac** makes it shot 3 of nine, a diagonal wipe that peels the near wall away.
- **Live 3D toon** makes it a real clipping plane, so the camera cuts the pot it was just looking at from outside, in one unbroken move.
- **Cross-section** makes it the entire hero.
- **Paper almanac** makes it Fig. 2 on a plate, with a numbered key.

Two directors independently proposed a **single source of truth for the chart
numbers** shared between the hero and the app, so the handoff cannot drift. That
is a direct fix for the defect the earlier council found at the reference site,
where the frozen screen showed one page while the walkthrough ended on another.
Adopt it whichever direction wins.

Four of five **demote the finger from the opening**. Only Night almanac keeps it
as the first thing you see.

---

## The axes that actually separate them

**Does a person appear?** Night almanac yes, as a backlit silhouette. Live 3D
toon yes, as a hand. Paper almanac only as handwriting in a margin. Cross-section
only as a finger entering frame. Instrument not at all, deliberately.

**Where does the proof live?** Instrument and Cross-section put it in measured
numbers: Cross-section actually ran a drying simulation and reports that at the
trigger weight the top 20 mm holds 8 percent of its water while the bottom 40 mm
holds 55. Night almanac and Paper almanac put it in a drawing. Live 3D toon puts
it in the camera, arguing that cutting the pot you are already looking at is a
demonstration where a separate diagram is only an argument.

**How hard is the handoff?** Paper almanac claims there is no state change at
all, because both sides are already cream paper with hairlines. Cross-section
turns its grams bar into the chart's y axis with no rescale. Live 3D toon sizes
the DOM to the camera rather than the camera to the viewport, which inverts the
mistake the reference made. Night almanac and Instrument both route through a
shared chart.

**What does it cost?** Four of the five are generated SVG and cost a few hundred
lines of Python. Live 3D toon costs 452 KB and a GPU context, though it argues
the downside is bounded because the 2D page already exists as its fallback and it
can be deleted in one commit.

**What does it risk?** Paper almanac and Cross-section both have a high floor of
required draughtsmanship, because cream shows every bad curve at 14:1 and there
is no illustrator. Night almanac can hide a mediocre drawing in shadow.
Instrument has no illustration to fall back on at all, so execution quality
carries everything.

---

## The honest weaknesses, in each director's own words

- **Night almanac:** "It spends nine viewports in a register the product never uses." The app is cream paper in daylight.
- **Paper almanac:** "There is no night, and no 11 p.m." It loses the moment and buys the authority.
- **Live 3D toon:** "452 KB and a GPU context, to sell an offline, no-account, no-server app." A reader who notices the dissonance is reading the page correctly.
- **Instrument:** "It has no person in it." It cannot do "I built this because I kept killing plants".
- **Cross-section:** "It is an explanation, and explanations are not stories." No arc, no reversal, nothing at stake.

---

## Things found while planning that are worth fixing regardless

Two directors audited the current code as a side effect and found real defects.
Both are verified:

1. **The saturation cap had a rounding bug.** `cap()` computed the right value then rounded to 8-bit hex, and rounding could push it back over the ceiling. Three colours were still over. Fixed by rounding non-maximum channels toward the maximum, which can only reduce saturation.
2. **The type scale fails the 1.7x gap on phones.** At 390px both `.beat` and `.beat-big` hit their clamp floors, giving 20.0px and 28.8px, a ratio of **1.44**. It holds at 1.90 at 760px and 2.00 at 1440px. The rule breaks exactly where most people read.

And three latent bugs that only bite if a **light** direction is chosen, which is
three of the five: `html { background: var(--ink) }` so overscroll shows black,
`.loader` opens on a dark field, and `<meta name="theme-color" content="#22201A">`.

---

## How I would read these

Not a recommendation, because the choice is yours, but the question I think
decides it:

**Does the page need to make someone recognise themselves before it teaches them
anything?**

If yes, Night almanac or Live 3D toon, because only those two have a person in a
room at eleven at night. If no, and the job is to make someone believe a fact
about soil they have been getting wrong for years, then Cross-section proves it
most directly and Instrument proves it with the strongest evidence.

Paper almanac is the only one that argues the current page is *dishonest* rather
than merely weak: the app is an almanac, the hero is a film, and a visitor is
sold one and handed the other.
