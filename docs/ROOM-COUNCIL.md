# Council review: the Blender room against the painted plates

2026-10-05. Five lenses, run in parallel, each given only its own domain so they
could not all converge on the same obvious complaints. Each was asked for one
claim the previous work had made that it thought was wrong.

Every code and arithmetic claim below was checked before being written down.
Where a lens was wrong, that is recorded too.

## The verdict, which was unanimous in effect

**Do not swap the renders for the plates today.** The narrative lens put it
hardest and nothing in the other four contradicts it: four of seven renders
lose their beat, and the four lost are beats 2, 5, 6 and 7, the watering, the
cost, the turn and the payoff. That is the argument's spine.

One render is better than its plate: `depth`. The sectioned pot states the
inch-against-twelve ratio that the painting only implies. Take that one alone.

## The three findings that are worth more than the rest

### The soil has no dent, and the product is about pressing a finger into soil

`parts.dress_soil` and `parts.add_dimple_key` exist. Verified by grep: they are
called from `build.py`, `match_ref.py` and two test scripts, and **never from
`world.py`**. `facet_pot` calls `_soil_grid` and leaves the grid mirror flat.

So in `finger-test` and `depth`, the two shots whose entire job is to show a
finger pressed into soil, the finger leaves no mark. The capability was written,
and then not wired in. `parts.build_hand` is dead the same way: only
`shot1_wide_test.py` calls it, so the shipping shots use a finger and a cone.

### The HDRI I documented as useless is shipping, and is the largest colour error

`textured()`'s docstring says an HDRI world "changed the numbers not at all,
because this is an enclosed room". `reset()` loads that HDRI and ships it at
strength 0.22. Verified at `look.py:85` and `look.py:90`.

The colour lens measured what it is doing: median saturation below value 0.18 is
0.46 to 0.59 in the renders against 0.28 to 0.37 in the plates. It is a warm
ambient holding chroma in every shadow in the room. The thing written off as
inert is causing the largest measured deviation in the set.

### The plates never clip. The renders do.

Measured over full-resolution files:

    shelf-evening   plate 0.00% of pixels with a channel at 255   render 0.94%
    phone-closeup   plate 0.00%                                   render 3.35%
    finger-test     plate 0.00%                                   render 0.07%

Zero, in every plate. Standard view transform has no highlight rolloff, so
emitter strength is the only clip control there is, and the bulb at 7.0, the
city windows at 4.0 and the moon at 5.0 were all set by eye.

It fails the accent rule in both directions at once. Pixels near the accent
`#C98A3A`: plates 0.9 to 3.7%, renders **0.0%, absent entirely in five of
seven**. The warm accent was not lost, it was replaced by a hole.

## Where the lenses agreed without being able to compare notes

**Foliage is the worst thing in the set.** The colour lens measured render
greens at hue 73 to 85 against the plates' 90 to 108 and a spec of 134, and
diagnosed the cause as the saturated warm lamp dragging a hue-96 albedo to 81.
The form lens, judging only silhouette, independently said the plant is the
wrong species: `leafy_plant` builds a rosette where every plate shows a
branching shrub with leaves at four or five heights. Two lenses, two methods,
same object.

**The terracotta pot is the one thing to leave alone.** Three lenses said so
unprompted. Measured on `finger-test`: render hue 20 sat 0.60 val 0.46 against
plate hue 22 sat 0.59 val 0.43.

## What the council caught in my own reasoning

**"Tearing is not a density."** I wrote in the fix plan that `roots` needed
scatter tuning rather than anything structural. The narrative lens: more roots
gives a denser net, not a torn one. The plate reads as damage because roots
**end** mid-frame with frayed fibre at the break. I spent five passes adding
roots when the fix was to break them.

**The mug fix caused the problem it solved.** I swept mug positions against both
table cameras and reported finding one visible in each as a win. The composition
lens: optimising one prop to be visible in two cameras is exactly what welded
those two shots into one. The plates use the mug differently per beat, a cropped
foreground mass in one and a small midground marker in the other. The answer was
two positions, not one compromise.

**The "done when" criteria were prop checklists, not beat criteria.** Which is
how `phone-closeup` scored done in the same paragraph that admitted the mug was
absent and the phone was not the largest shape.

**"The renders are posterised, just not on purpose."** My own docstring reasons
from 49,421 distinct plate colours to "this is NOT a posterised cel look", then
concludes surfaces should be one number. Renders carry 7,036 to 12,908 distinct
colours against the plates' 37,235 to 84,090.

**The stippling is a denoising bug, not a sampling one.** Verified:
`use_denoising = True` denoises the Combined pass, while `posterise()` consumes
raw `DiffDir` and `DiffInd` with no Denoise node anywhere. A constant ramp over
a noisy signal turns per-pixel noise into per-pixel step assignment, which is
why shadow edges are speckle rather than polygon. Hardening the lights made it
worse by raising the noise floor on a sparser source.

## Where a lens was wrong

The lighting lens built its central argument on `gamma=0.85` and concluded the
first ramp boundary fires at raw diffuse 0.92, stranding roughly 57% of the
frame in the bottom step. The shipped invocation uses **gamma 0.65**. Recomputed
against the light distribution measured earlier by `probe_light.py`, the first
boundary fires at 0.56 and the percentiles land across all four steps: p10 and
p25 in step 0, p50 in step 1, p75 in step 2, p90 upward in step 3.

Its parameters are therefore not as broken as claimed. Its *measurement* of the
output stands regardless and is the more useful half: a pot patch resolves 2
value plateaus in the render against 5 in the plate. With four global steps a
single object can only ever hold a few of them, so the recommendation that
survives is more steps and non-uniform step positions, not a different exposure.

It also noticed that `render_plate.py` ships `exposure=0.21, gamma=0.65` while
`plate.apply()` defaults to `0.28, 0.55`. Verified. The documented defaults have
never been what rendered.

## The finding that undercuts the whole exercise

The room was built for consistency. In value terms it is **less** consistent
than the thing it replaces: background medians across the seven plates span
0.154 to 0.185, a range of 0.031; across the seven renders they span 0.079 to
0.188, a range of 0.109. Three and a half times wider.

The plates drift in props, which one person noticed after a day of staring. The
renders drift in value, which is the dimension a viewer actually perceives
first. We have been trading a drift nobody can see for one everybody can.

## One thing the renders genuinely do better

Subject-to-background separation. The renders hold the night field below the
interior: in `shelf-evening`, background median 0.079 against a lit sill of 0.25
to 0.35. The plate's night field is 0.158 while its sill runs 0.14 to 0.26, so
the window sits *inside* the subject's value range and reads as behind the room
only by hue. Across all seven the renders' background is below the subject band
in six; the plates' is inside it in most. Zeroing the base colour on emissive
materials is what bought this, and it is correct.

## Recommended order, if this is ever resumed

Cheapest and most damaging first. Not a commitment to resume.

1. Wire `dress_soil` and the dimple into `facet_pot`. The premise of the product
   is currently invisible in the two shots that exist to show it.
2. Add a Denoise node on the lighting pass in `posterise()`. One node, and it is
   the cause of every stippled edge.
3. Pull the emitter strengths down until no plate-matching frame clips.
4. Replace the HDRI ambient with a cool flat fill, or remove it as documented.
5. Separate wood from terracotta in value and saturation.
6. Rebuild `leafy_plant` as a branching stem, and `leaf()` with a petiole and
   lobes.
7. Only then the cameras, because fixing a composition on top of a broken value
   structure is how four of today's five passes were spent.
