# UI audit: the Android app

2026-11-18. Run with the `impeccable` native audit against its Material 3
reference, on a connected Xiaomi 25080RABDG running Android 16. Evidence is
screenshots from that device in dark, light and at a 1.3 font scale, plus
source checks. Performance and landscape were **not measured**, which is said
again where it affects a score rather than hidden in a number.

## Audit health score

| # | Dimension | Score | Key finding |
|---|---|---|---|
| 1 | Accessibility | 3 | Labels, targets and scaling are sound; one separator can start a line at large type |
| 2 | Performance | 3 | Lists are lazy and motion respects the system setting, but nothing was profiled |
| 3 | Appearance and theming | 4 | Zero raw hex outside the theme, and both schemes are designed rather than inverted |
| 4 | Platform conformance | 3 | Material throughout, except four in-app toasts where snackbars belong |
| 5 | Adaptivity | 2 | No window size classes at all, so a tablet gets a stretched phone |
| **Total** | | **15/20** | **Good: address the weak dimensions** |

## Platform conformance verdict

**Pass.** This reads as a native Android app, not a ported web page. Navigation
is a three-destination Material navigation bar, the top app bar carries screen
context, sheets are Material bottom sheets, chips and buttons are Material
components, and predictive Back was verified working on a dirty edit form
earlier in the project on both gesture and button navigation. The brand is
expressed through the type scale and colour roles rather than by replacing
components, which is the right way round.

The violations are narrow and listed below.

## Detailed findings

### [P1] A tablet or unfolded foldable gets a stretched phone layout
**Location**: whole app. No `WindowSizeClass` or `calculateWindowSizeClass`
anywhere in `app/src/main`.
**Category**: Adaptivity
**Impact**: On expanded width the three-destination bottom bar and the
single-column list both stretch rather than becoming a navigation rail and a
two-pane layout. Line lengths go well past comfortable reading on a tablet.
**Guideline**: Material, navigation bar on compact width, navigation rail or
drawer on expanded; never ship a phone bottom bar untouched on a tablet.
**Recommendation**: This is only worth doing if tablets are a target. The app
is a phone diary used standing at a windowsill, so the honest answer may be to
declare phone-only rather than to build a rail nobody asked for. Decide it
deliberately instead of leaving it unhandled.
**Suggested command**: `$impeccable adapt`

### [P2] Four in-app toasts where Material wants a snackbar
**Location**: `feature/qr/StickerSheetSheet.kt:67` and `:84`,
`ui/PhotoViewer.kt:263` and `:285`
**Category**: Conformance
**Impact**: A toast cannot carry an action, cannot be undone, and ignores the
app's own theming, so these four moments look and behave unlike every other
confirmation in the app, which uses snackbars.
**Guideline**: Material, snackbars for transient feedback, never a toast for it.
**Note**: The fifth toast, `reminder/ReminderActionReceiver.kt:60`, is correct
and should stay. It fires from a notification action with no app UI on screen,
where there is no snackbar host to use.
**Suggested command**: `$impeccable harden`

### [P2] Plants sharing one pot read as three separate chores
**Location**: `feature/dashboard/DashboardScreen.kt`, the plant list
**Category**: Conformance and hierarchy
**Impact**: Three Fittonias in one container produce three visually identical
rows, each saying "Watered 2 days ago, checked yesterday", because watering one
fans out to all three. The list implies three jobs where there is one pot. The
app already knows this: the plant page says "shares a pot with 2 others".
**Law**: Gestalt common region and proximity. Things that act as one should be
bounded or spaced as one. Miller's law compounds it, since every pot-mate
spends another row of the seven or so a person can hold at once.
**Recommendation**: Group pot-mates under one common region, or mark the rows
so the relationship is visible before the user taps through to find it.
**Suggested command**: `$impeccable layout`

### [P2] The thumbnail stops sharing a baseline when a row grows
**Location**: `feature/dashboard/DashboardScreen.kt`, plant row
**Evidence**: `04-peperomia-font13.png`. The three-line rows align the photo's
top edge with the plant name, while the four-line Peperomia row centres the
photo, so its name floats above the image and the left edge of the list goes
ragged.
**Category**: Adaptivity and layout
**Impact**: Cosmetic at 1.0, visible at 1.3, and it breaks the vertical rhythm
that makes the list scannable.
**Recommendation**: Align the thumbnail to the top of the text block rather
than centring it, so the grid holds whatever the row contains.
**Suggested command**: `$impeccable layout`

### [P2] A separator can begin a line
**Location**: `feature/dashboard/DashboardScreen.kt:566`, `append(" · ")`
**Evidence**: at 1.3 the status line wraps to "Watered 2 days ago" then
"· checked yesterday", so the separator reads as a bullet.
**Category**: Accessibility, because it only appears at larger type
**Recommendation**: Use a non-breaking space before the separator so the dot
cannot start a line.
**Suggested command**: `$impeccable typeset`

### [P3] A hyphen stands in for a dash in dashboard prose
**Location**: `feature/dashboard/DashboardScreen.kt:686`,
`Confidence.MEDIUM -> " - still learning"`
**Impact**: Renders as "Water in about 3 days - still learning". The project's
own writing rule excludes dashes in prose, and the weight screen already words
the same idea without one, as "still learning this plant". So this is an
inconsistency with the app's own voice rather than a matter of taste.
**Suggested command**: `$impeccable clarify`

### [P3] The only filled container on the screen is the least important control
**Location**: `feature/dashboard/DashboardScreen.kt`, the "2 archived" chip
**Law**: Von Restorff. The one element that differs is the one that gets
noticed, and here the isolation is spent on archived plants rather than on
anything the visitor came to do.
**Suggested command**: `$impeccable quieter`

## Patterns and systemic issues

There is no systemic sloppiness to report, which is unusual. The findings are
six specific places rather than a habit repeated across screens. The one
pattern worth naming is that **every weakness is at the edges of the envelope**:
large font scale, shared containers, expanded window widths, and the two
screens that reach for a toast. The common centre of the app is consistent.

## Positive findings

These are the things to protect, since they are what make the rest cheap.

- **The theme is airtight.** Zero `Color(0x` anywhere in `app/src/main`: all 88
  raw values live in `core/ui`, where a palette belongs, and `ThemeTest` fails
  the build on contrast, ramp and tier collapse. Dark and light are both
  designed, with a warm near-black rather than a quick invert.
- **Type is in sp throughout**, with zero sizes in dp and only five deliberate
  `fontSize` overrides across the app, so the Material type scale is doing the
  work. The layout survives 1.3 without clipping, which the screenshots show.
- **Motion already honours the system setting.** `ui/Motion.kt:42` reads
  `ANIMATOR_DURATION_SCALE`, so "remove animations" is respected rather than
  ignored, which most apps of this size skip.
- **Touch targets are clean.** No `.size()` under 48 dp on any touchable.
- **Accessibility labels are deliberate.** 37 content descriptions set against
  six nulls, and three of those six are navigation bar icons, where a null is
  correct because the label already names the destination.
- **The two log actions are weighted equally on purpose**, with the same size,
  the same haptic and the same confirmation, so the app does not quietly teach
  that watering is the right answer.

## Recommended actions

1. **[P1] `$impeccable adapt`**: decide the tablet and foldable story, even if
   the decision is to declare phone-only.
2. **[P2] `$impeccable harden`**: replace the four in-app toasts with snackbars,
   keeping the notification one.
3. **[P2] `$impeccable layout`**: group pot-mates, and top-align the thumbnail
   so rows of different heights still share a grid.
4. **[P2] `$impeccable typeset`**: stop the separator starting a line.
5. **[P3] `$impeccable clarify`**: remove the hyphen from the dashboard
   prediction line.
6. **[P3] `$impeccable quieter`**: take the filled treatment off "2 archived".
7. **`$impeccable polish`**: final pass once the above land.

## What was not checked

Said plainly so the score is not read as more than it is.

- **Performance was not profiled.** No frame timing, no startup trace, no
  recomposition counts. The score reflects reading the code, where lists are
  lazy in 27 places and nothing obvious blocks the main thread.
- **Landscape was not opened.** The manifest sets no orientation lock, so it
  rotates, and nobody has looked at what it does.
- **The single-plant weigh keypad was not driven** in this pass, only the
  round's. They share the construction.
- **Screens not opened at all**: Figures, Experiments, Propagation, Places,
  Fertiliser, the sticker sheet and the post-mortem.
