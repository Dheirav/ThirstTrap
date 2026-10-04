# ThirstTrap — Weight-Based Watering Model

Requirements item 17. This is the feature the app exists for: no mainstream
plant app does weight-based watering, and the method itself is long-established
horticultural practice rather than something invented here. Sources in section
0, including what they do and do not support.

All of this lives in **`core:domain`** as pure functions over lists of
readings. No Android, no database, no I/O. That makes it exhaustively testable,
and testing it exhaustively is the point — a wrong prediction here is the whole
product failing.

---

## 0. Where this comes from, and what it does not prove

This section exists because the line it replaces said the method was "validated
commercial greenhouse practice" and cited nothing. The claim is true. It also
proves less than it sounds, and the difference matters on a public page.

### What the literature does support

**Gravimetric irrigation scheduling is standard practice, not a novelty.**
Dumroese, Montville and Pinto describe it for container nurseries in terms that
match this app almost line for line: weigh the container at field capacity,
meaning watered until saturated then allowed to drain freely for 30 to 60
minutes, reweigh at intervals, and irrigate when the weight falls to a target
proportion. That is the wet anchor procedure in section 2, including the drain
time, which was chosen here before the paper was read and turns out to be the
same number.

They also state the case against the finger test better than the landing page
does: tactile assessment "require[s] a great deal of experience by the
individual to be consistent", and "what one staff person considers 'dry'
another might consider 'moist'".

**The trigger fraction is the MAD framework.** Management Allowed Depletion is
the portion of plant-available water that may be depleted before stress begins,
and it is crop-specific: about 30% for sensitive vegetables, 50% for corn, 50 to
60% for alfalfa, 65% for cotton. `DEFAULT_DEPLETION_TRIGGER_PCT` is 50, which
sits in that band rather than being picked by feel.

**And the arithmetic matches the right definition.** `triggerWeight` is
`wet - trigger x (wet - dry)`, a fraction of the water between the anchors, not
a fraction of the pot's total weight. Dumroese warns specifically that these two
get confused, and shows the size of the mistake: at a nominal 70% target the two
readings differ by 80% in the water actually supplied, while the target weights
differ by only 22%. A plausible-looking off-by-a-definition here would be nearly
invisible and would halve or double the water. This app is on the MAD side,
which is the one the framework means.

**It saves water at commercial scale.** Hunt and McDonald report 30 to 70%
savings from a weight-based controller in production nurseries, depending on the
system.

### What the literature does not support

None of the above is evidence for this app. The gap is not small:

- Those studies use **load cells logging continuously**. This has a kitchen
  scale and a person who has to remember. Reading frequency is the single
  biggest difference and it is entirely on the wrong side.
- They run **uniform crops under controlled light, temperature and humidity**,
  on a bench. This runs four unlike plants on a windowsill in a flat.
- They measure **water saved against a grower's existing regime at scale**. That
  is not the same as beating one person's habit with four pots, where the habit
  may already be fine.
- Nothing in them validates the choices that are actually ours: the linear fit
  over recent readings, the EWMA smoothing, the segmentation, the suppression
  table, or any of the confidence rules.

So the honest reading is that the method is sound in principle and the
implementation is unproven. That is why `PredictionEvaluation.kt` scores this
app against a calendar fitted to the user's own interval, from their own diary.
Published work answers whether weighing can work. Only that scoring answers
whether it is working here.

### Sources

- Dumroese RK, Montville ME, Pinto JR. 2015. Using container weights to
  determine irrigation needs: a simple method. *Native Plants Journal*
  16(1):67-71.
  <https://research.fs.usda.gov/treesearch/download/48087.pdf>
- Hunt DS, McDonald J. 2015. Automating irrigation scheduling in production
  nurseries using a weight-based irrigation controller. *Acta Horticulturae*
  1104, chapter 8. <https://doi.org/10.17660/ActaHortic.2015.1104.8>
- Shortridge J, Porter W. Scheduling Agricultural Irrigation Based on Soil
  Moisture Content: Interpreting and Using Sensor Data. Virginia Cooperative
  Extension BSE-339P.
  <https://www.pubs.ext.vt.edu/BSE/BSE-339/BSE-339.html>

---

## 1. The idea in one paragraph

A pot loses water almost entirely by evaporation and transpiration, both of
which are roughly steady over a day. So pot weight falls near-linearly between
waterings. If you know the weight when it is fully watered (**wet anchor**) and
the weight at which it needs water again (**dry anchor**), then every weighing
places the plant somewhere on that line, and the slope of the last few readings
tells you when it will hit the threshold. That is the entire model. It needs no
machine learning and about fifty lines of maths.

---

## 2. Anchors and calibration

### Wet anchor `W`

Water thoroughly, let it drain 30–60 minutes, weigh. That is `W` — the pot at
container capacity.

`W` is **re-captured on every post-watering weigh-in** (`context = post_water`).
Substrate settles, plants grow, pots collect mineral crust; the anchor drifts.
Use the most recent post-water reading rather than the original calibration
value.

### Dry anchor `D`

Never force a dry-out to find it. That means stressing a plant to calibrate a
convenience feature, which is backwards.

**Provisional estimate:**
```
D₀ = W × (1 − f)      f = 0.40 by default
```
The requirements give 35–45% for peat/coco mixes; 0.40 is the midpoint. Expose
`f` only in an advanced setting — most users should never see it.

**Adaptive replacement:** every time the user actually waters, the weight
immediately before watering (`context = pre_water`) is evidence about where
"dry enough for this person and this plant" really is. Take the running
minimum:

```
D ← min(D_current, w_pre_water)
```

with three guards:
- **Reject implausible readings**: ignore any `w_pre_water < 0.30 × W`. That is
  almost certainly a mis-weigh (pot lifted off the scale, wrong plant), not a
  genuinely bone-dry pot.
- **And the mirror of it**: ignore any `w_pre_water > 0.95 × W`, or any reading
  leaving `W − w_pre_water < 1 g`. A pot weighed just before watering and still
  at container capacity says nothing about how dry this person lets a pot get,
  so it is a reading filed under the wrong context. The guard is not cosmetic:
  the provisional branch below replaces its guess outright even upward, so
  without a ceiling one such reading puts `D` on top of `W`, `R` becomes 0 and
  depletion becomes NaN. See D43.
- Once at least one real `pre_water` reading has been folded in, set
  `dry_anchor_provisional = 0`. The UI can then stop hedging its language.

The wet end needs the same protection from the other side. A `post_water`
reading below the measured `D` cannot be the result of watering, so the pot
itself changed weight (pruned back, soil lost in a division, a different tare)
and the measured `D` describes a pot that no longer exists. Re-derive `D₀` from
the new `W` rather than keep an impossible pair.

A pair with `W − D < 1 g` is **not a calibration**. Treat it as none at all:
draw the readings, suppress the prediction with `not_calibrated`, and ask for a
post-water weigh. Anchors also arrive straight from a stored row, so this is
checked where they are read and not only where they are folded.

### Range and depletion

```
R = W − D                       the usable water range, in grams
depletion(w) = (W − w) / R      0 = just watered, 1 = at the dry anchor
```

Clamp `depletion` to `[0, 1.2]` for display — above 1.0 is possible and means
drier than the recorded anchor, which is worth showing rather than hiding.

### Trigger

```
w_trigger = W − t × R
```

`t` is the plant's `depletion_trigger`, default **0.5** — the commercial MAD
guideline. Per-plant slider with presets:

| Plant type | `t` |
|---|---|
| Succulents, cacti, sansevieria | 0.70–0.80 |
| Most foliage houseplants (pothos, monstera, philodendron) | 0.50 |
| Ferns, calathea, moisture-lovers | 0.30 |

### Recalibration

Set `needs_recalibration = 1` and clear both anchors on: `repotted`,
`medium_changed`, or an edit to `container_desc`. The pot itself changed
weight; every previous reading is now meaningless. Do **not** silently keep
predicting — show "recalibrate after repotting" instead.

---

## 3. Segmentation

**Never fit a line across a watering.** This is the single most important rule
in the model; violating it produces a slope averaged over a sawtooth, which is
meaningless.

A new `drying_segment` opens when any of these occurs:

1. A reading is **≥ 8% of `R` higher** than the previous reading (a watering
   jump). The requirements say 5–10%; 8% is the midpoint and is far above
   scale noise for a real pot.
2. A `watered` care event exists between two readings — authoritative, and
   catches the case where the user watered but did not weigh.
3. A `repotted` or `medium_changed` event — `start_reason = repot`.
4. **A gap of more than 21 days** between readings — `start_reason = gap`. The
   old segment is stale; conditions have changed.

The previous segment closes: `ended_at` is set, its final slope is computed and
stored, and that slope updates the EWMA (section 5).

---

## 4. Fitting the current segment

### Input selection

Take readings in the current open segment where `excluded = 0`, most recent
first, capped at **5**. Fewer than 5 is fine; fewer than 2 is handled by the
suppression rules.

### Theil–Sen estimator

For all pairs `i < j`, compute the pairwise slope and take the median:

```
slope = median over all i<j of  (w_j − w_i) / (t_j − t_i)
```

with time in days. With n = 5 that is 10 pairs — trivial to compute.

**Why Theil–Sen and not least squares:** the real failure mode is one bad
weigh-in — the pot set down half on the scale, a wet saucer included, a misting
five minutes earlier. Least squares lets a single outlier drag the whole fit
and, worse, does so invisibly. The median of pairwise slopes ignores it. It
tolerates up to ~29% corrupted points. With only 3–5 readings this robustness
matters more than the theoretical efficiency loss on clean data.

For n = 2, fall back to the two-point slope and record
`slope_method = two_point` — usable, but flagged low-confidence.

### Sanity gate

The slope must be **negative** — pots dry out. Define a dead band:

```
ε = max(1.0 g/day, 0.005 × R per day)
if slope > −ε  →  "no measurable drying yet"
```

A near-zero or positive slope means either not enough time has passed, or
something is wrong (see diagnostics). Either way, **do not show an ETA**.

---

## 5. The EWMA prior

Day 1 after watering has one reading and therefore no slope. Rather than show
nothing, use the plant's own history.

On every segment close:

```
ewma ← α × final_slope + (1 − α) × ewma        α = 0.3
```

Seeded with the first closed segment's slope.

**Why α = 0.3:** roughly three segments to substantially adapt. At an ~8-day
watering cycle that is ~24 days — fast enough to track the 2–5× seasonal
drying-rate swing the requirements call out, slow enough that one weird week
does not throw it.

The prior is used when the current segment has fewer than 2 readings. The
prediction is then labelled **"estimated from this plant's history"** — never
presented with the same confidence as a measured fit.

---

## 6. Prediction

```
eta_days = (w_now − w_trigger) / |slope|
```

Then:

- **Clamp** at 0 — negative means already past the trigger; show "water now".
- **Cap** at 14 — beyond that, display "more than 2 weeks" rather than a
  number. A 30-day extrapolation from 5 readings is fiction.
- **Round for display:** under 1 day → "today" / "tomorrow"; 1–3 days → nearest
  half day; beyond → whole days.

### Suppression rules — when to show nothing

Show **"need another reading"** instead of a prediction when:

| Condition | Reason |
|---|---|
| No wet anchor | Not calibrated |
| `needs_recalibration = 1` | Pot changed; old data invalid |
| 0 readings in segment | Nothing to go on |
| 1 reading and no EWMA | Nothing to go on |
| `slope > −ε` | No measurable drying |
| `medium = water` | Weight is meaningless for a water-propagation subject |

This list is a feature. The requirements are explicit that a prediction must
**never** be displayed with fewer than 2–3 post-watering points or a near-zero
slope. An app that confidently states a wrong date is worse than one that says
it does not know yet — it trains exactly the calendar-watering behaviour the
weight method exists to replace.

### Confidence tiers

| Tier | Condition | UI wording |
|---|---|---|
| High | ≥ 4 readings, Theil–Sen | "Water in about 3 days" |
| Medium | 2–3 readings | "Roughly 3 days — still learning" |
| Low | Prior only | "Maybe 3 days, from past cycles" |

---

## 7. Diagnostics for free

The drying curve is a health monitor, not just a timer. Both alerts below
require an established EWMA (**≥ 2 closed segments**) — without a baseline
there is nothing to deviate from.

### Drying much faster than usual

```
|slope| > 1.8 × |ewma|      and  ≥ 3 readings in the segment
```

Suggests water channelling down the sides of a shrunken root ball, a
root-bound pot, or a sharp change in heat/light. Water is bypassing the roots.

### Pot staying heavy

```
|slope| < 0.4 × |ewma|      and  ≥ 5 days since watering
```

Suggests root rot, over-potting, or a cold/dark spell — the roots have stopped
drinking. This is the one worth catching early, because by the time it is
visible above the soil it is often too late.

### Rules for both

- **At most one diagnostic alert per plant per segment.** No nagging.
- Phrase as a question, never a diagnosis: *"This pot is drying faster than
  usual — worth checking whether water is running down the sides?"* The app has
  a slope, not a stethoscope.
- Dismissible, and dismissal persists for that segment.

---

## 8. Presentation

**Show a bar, not grams.** The user does not care that the pot weighs 1,240 g.
They care that it is 60% of the way to needing water.

```
just watered ─────────────●──────────┤ water
             [========================]
                          ▲ now (62%)
                                   ▲ trigger (50%)  ← marked on the bar
```

Raw grams belong on the weight-history screen and in the chart, not on the
dashboard card.

### Logging flow

"Weigh before watering, weigh after." Two readings per cycle is the minimum
that keeps both anchors fresh. Routine mid-cycle weighs are what sharpen the
prediction, and the app should make them a two-tap action, but never nag for
them.

### Scale guidance (in-app help)

- 5–10 g resolution is plenty. A 6-inch pot swings 300–400 g between wet and
  dry, so the signal dwarfs the noise. A ₹300–800 1 g kitchen scale is a
  luxury, not a requirement.
- **Repeatability matters more than resolution.** Same scale, same spot on the
  platter, same saucer situation, and not right after misting.
- Warn if the observed `R` is under ~100 g: the pot is too small for this
  method to beat simply lifting it.

### Hardware

**Manual weighing only. No sensor integration, ever, as a dependency.** The
consumer smart-sensor graveyard — Parrot Flower Power and friends — is what
happens when a plant product needs a live server. A dumb scale plus this app
has no dead-server failure mode. Cheap capacitive probes may later join as a
qualitative cross-check, never as the primary signal; resistive probes corrode
within weeks and should be skipped entirely.

---

## 9. Test plan

Pure functions, so this is cheap and there is no excuse for skipping it.

**Correctness**
- Synthetic linear decay + Gaussian noise (σ = 5 g) → predicted ETA within
  ±0.5 day of ground truth.
- Non-linear tail (drying slows as it approaches dry) → the linear fit must err
  **early**, never late. Predicting dry slightly early is the safe direction.

**Robustness**
- One wild outlier in five readings → ETA moves by less than 10%. This is the
  test that justifies Theil–Sen; write it first and watch least squares fail it.
- Duplicate timestamps → no division by zero.
- Readings out of chronological order → sorted before fitting.

**Segmentation**
- A watering jump mid-series → assert two segments, and assert **no fit spans
  the boundary**. This is the model's cardinal rule; test it directly.
- A `watered` event with no accompanying weight jump → still splits.
- A 25-day gap → new segment with `start_reason = gap`.

**Anchors**
- Adaptive `D` decreases monotonically and never below `0.30 × W`.
- `D` is never raised to `0.95 × W` or above, including from the provisional
  state, and `R` is never 0 or negative for any sequence of readings.
- `W` updates on each `post_water` reading.
- Repot clears both and sets `needs_recalibration`. Assert this for the **dry**
  end specifically, with a pre-repot `pre_water` reading in the fixture: a
  fixture of post-water readings only cannot see the bug D43 fixed.
- A stored pair spanning no range suppresses the prediction rather than
  producing one.

**Suppression**
- Each row of the suppression table gets a test asserting no ETA is produced.

**EWMA**
- After 3 segments at a fixed slope, `ewma` is within 10% of it.
- A 3× step change in drying rate is tracked within ~3 segments.

**Properties**
- For any monotonically decreasing series, `eta_days ≥ 0`.
- ETA is never `NaN` or infinite for any input, including empty lists,
  single readings, and identical weights.
