# ThirstTrap — UI Specification

Jetpack Compose, Material 3, dynamic colour, dark theme mandatory.

The requirements name two hard UX problems and say to prototype them first.
Sections 2 and 7 of this document are those problems. Everything else is
comparatively easy and should not be built until those two feel right.

---

## 1. Screen inventory

| # | Screen | Milestone |
|---|---|---|
| 1 | Dashboard (home) | M1 |
| 2 | Plant detail — Timeline / Weight / Photos | M1 |
| 3 | Quick-log bottom sheet | M1 |
| 4 | Event entry (type-specific) | M1 |
| 5 | Add / edit plant | M1 |
| 6 | Photo compare | M1 |
| 7 | Reminder list & editor | M1 |
| 8 | Export | M1 |
| 9 | Weight entry | M2 |
| 10 | Calibration wizard | M2 |
| 11 | Weight history chart | M2 |
| 12 | Settings | M1, grows |
| 13 | "Reminders not arriving?" help | M2 |
| 14 | Propagation kanban | M3 |
| 15 | Post-mortem | M3 |
| 16 | Experiments | M4 |

Navigation: Navigation Compose with type-safe serializable routes. Bottom bar
with three destinations — **Plants**, **Due**, **Settings**. Everything else is
pushed on top.

---

## 2. Hard problem #1 — the 3-tap log

> *"If logging is slower than a paper note, the app loses to paper and dies in
> a week."*

The requirements set ≤3 taps and cold-open-to-logged in under 10 seconds. The
target is better than that.

### The primary path is one tap

Each dashboard card has a **water droplet button on the card itself**.

```
cold open → tap droplet on the card = LOGGED
```

That is one tap, no navigation, no confirmation dialog. A **snackbar with UNDO**
appears for 5 seconds. This is the correct pattern for a frequent, low-stakes,
easily-reversed action — a confirmation dialog on every watering would be the
single worst decision in the app.

### The secondary path is three taps

For anything that is not a plain watering:

```
tap card → quick-log sheet → choose type = LOGGED
```

The bottom sheet shows four large targets — **Watered**, **Checked**,
**Photo**, **More** — each at least 64dp tall. "More" opens the full event-type
list.

### Details that decide whether this works

- **No loading spinner on the dashboard.** Room `Flow` + a Compose `key` on the
  plant ID; the list must be on screen the instant the activity draws. If the
  first frame shows a spinner, the ten-second budget is already half gone.
- Writes are fire-and-forget into `viewModelScope`. The UI updates optimistically
  from the Flow; it never waits on the database.
- **Undo is a real delete**, not a tombstone — within the snackbar window the
  row is removed outright.
- A **long-press** on the droplet opens the amount/method detail instead of
  logging immediately, for the times you want to record 500 ml bottom-soak.
- QR sticker scan (requirements item 21) deep-links straight to that plant's
  quick-log sheet. Physical answer to the same problem.

---

## 3. Dashboard

A vertical list of plant cards, sorted by **attention score** — the
requirements' *"most needs attention"*.

### Card contents

```
┌──────────────────────────────────────────────┐
│ [photo]  Marbled pothos              [💧]    │
│  64dp    Desk · soil                         │
│          Watered 6 days ago                  │
│          [=============▐······] 62%          │
│          Water in about 2 days               │
└──────────────────────────────────────────────┘
```

- Thumbnail (64dp, rounded), name, location · medium.
- Days since last watered, in plain words.
- Depletion bar **only if calibrated** — otherwise the row is simply absent.
  Never show an empty or zeroed bar; absence is honest, a zeroed bar is a lie.
- Prediction line, subject to every suppression rule in WATERING-MODEL §6.
- Droplet quick-log button.
- A due badge if a reminder is outstanding.

### Attention score

Descending priority:

1. Overdue reminders — most overdue first
2. Depletion ≥ trigger (calibrated plants that need water now)
3. Predicted to need water within 24h
4. Days since last check, descending
5. Name, alphabetically (stable tiebreak)

Dead and given-away plants are excluded by default; a filter chip reveals them.

**No counts of failures anywhere.** No "3 overdue!" badge in red. The
requirements are explicit about this.

---

## 4. Plant detail

Collapsing header — cover photo, name, species, medium chip, location chip,
status chip. Three tabs.

### Timeline tab

Newest first, grouped by day with sticky date headers. Each row: icon by event
type, relative time, note, thumbnail strip if photos are attached. Tapping an
event expands it in place; long-press offers edit/delete.

Events that changed the plant's nature — `repotted`, `medium_changed`, `died` —
render as **full-width dividers with heavier weight**, not ordinary rows.
Requirements flag medium conversion as high-risk and worth surfacing in
history; the timeline should make those moments findable at a glance while
scrolling fast.

### Weight tab

Chart plus history. Details in §6.

### Photos tab

Reverse-chronological grid, 3 columns. Long-press selects; selecting exactly
two enables **Compare**.

---

## 5. Photo compare

The diagnosis workflow. *"Is the brown spot bigger than yesterday?"*

- Two panes, side by side in portrait (stacked vertically is wrong — plants are
  taller than they are wide).
- A filmstrip under each pane to swap that side independently.
- Elapsed time between the two shown prominently between the panes:
  **"11 days apart"**.
- Pinch-zoom per pane, with a **lock toggle** that syncs both panes' zoom and
  pan — the actual comparison move is zooming into the same leaf on both.
- Default selection when entering from a plant: **oldest and newest**.

---

## 6. Weight screens

### Calibration wizard

Four steps, roughly two minutes plus one drying cycle.

1. **Explain** — what this does and why, in three sentences. Include the honest
   caveat that it takes one full drying cycle to become accurate.
2. **Water thoroughly, drain 30–60 min.** A timer with a notification when it
   elapses, so the user can walk away.
3. **Weigh and enter** → `wet_anchor_g`, `context = calibration`.
4. **Set the trigger** — a slider defaulting to 0.5, with the plant-type
   presets from WATERING-MODEL §2 as one-tap chips.

The dry anchor is never asked for. It starts provisional and improves itself.
Say so explicitly on step 1 so the user understands why early predictions hedge.

### Weight entry

Large numeric keypad — this is used standing at a windowsill holding a pot, one
handed. Big targets, no small controls.

- Remembers the last value as a hint so a typo is obvious.
- Context selector: **Routine / Before watering / After watering**, defaulting
  intelligently (if the plant is past its trigger, default to "Before
  watering").
- After saving, immediately show the updated bar and prediction. The feedback
  loop is the reward for weighing.

### Weight history chart

- X: time. Y: grams.
- **Horizontal lines** for the wet anchor, dry anchor and trigger threshold.
- **Vertical markers** at watering events — these are the segment boundaries,
  and showing them makes the model's behaviour legible.
- The fitted line drawn over the current segment only, dashed forward to the
  trigger crossing.
- Compose `Canvas` first; reach for Vico only if this becomes fiddly. It is a
  line, three horizontals and some markers.

---

## 7. Hard problem #2 — reminder tone

> *"Reminders must ask 'check the pot', and make 'checked — not needed yet' as
> satisfying to tap as 'watered'. Get this wrong and the app trains
> calendar-watering, which is the exact failure mode the weight method exists
> to avoid."*

This is a copywriting and micro-interaction problem, not an engineering one,
and it is the easiest thing in the project to get subtly wrong.

### Wording

| Never | Always |
|---|---|
| "Water your pothos today!" | "Time to check the pothos" |
| "Overdue by 3 days" | "It's been 9 days since you checked" |
| "You missed 2 waterings" | *(nothing — say nothing)* |
| "Don't let your plant die!" | "Lift the pot — does it feel light?" |

The notification asks the user to **assess**, never instructs them to water.

### Parity of the two answers

The two responses must be visually and haptically equal:

- Same size, same weight, side by side, neither styled as primary or secondary.
- **"Still wet" gets the same confirmation animation and the same haptic tick
  as "Watered."** If "Watered" gets a satisfying check animation and "Still
  wet" gets a grey dismissal, the app has taught the user that watering is the
  correct answer — which is the failure mode.
- The confirmation copy for "Still wet" is positive and specific: **"Good call
  — checked, not thirsty yet."** Restraint is the skill being rewarded.

### Never-do list

- No streaks, no counters, no badges counting misses.
- No red anywhere in the reminder flow. Overdue is not an error state.
- No push notification that is not user-initiated. The app never messages the
  user about anything other than a reminder they created.

---

## 8. Design system

- Material 3 with dynamic colour on Android 12+, falling back to a defined
  green-based scheme. Green, but muted — avoid the saturated "gardening app"
  palette.
- Full dark theme. Not an afterthought: plants get checked in the evening.
- Type scale: default M3. No custom fonts in v1.
- Corner radius 12dp on cards, 16dp on sheets.
- Spacing on a 4dp grid; 16dp screen margins.

### Accessibility — not optional

- Every interactive element has a `contentDescription`. The droplet button
  reads "Log watering for marbled pothos", not "button".
- Minimum 48dp touch targets throughout.
- The depletion bar must not encode meaning in colour alone — it carries a
  percentage label and a marked threshold, so it works for a red-green
  colour-blind user and in greyscale.
- Support up to 200% font scale without clipping. Test it; cards with fixed
  heights will break.
- Every animation respects the system reduce-motion setting.

---

## 9. Empty and error states

| State | Treatment |
|---|---|
| No plants yet | Illustration + "Add your first plant". Not a blank screen. |
| Plant with no events | "No history yet — log a watering or a check." |
| Not calibrated | An inline card in the Weight tab explaining what calibration buys, with a start button. |
| Prediction suppressed | Plain text: "Weigh once more to predict" — never a fake number, never a spinner |
| Photo file missing | Broken-image placeholder in the grid. **Never crash.** |
| Pl@ntNet offline / quota spent | "ID unavailable — you can type the species instead." The core loop never depends on it. |
| Export failed | Specific reason and a retry. Export failure is serious; do not swallow it. |
