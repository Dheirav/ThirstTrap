# Design coherence: breaking down the friend's review

A council of six independent reviewers read the code against the eight lines of
review a developer friend gave. This document is the adjudicated result: what he
saw, what is actually in the files, the one cause underneath it, and the order to
fix it in. Effort is S (under an hour), M (a few hours), L (a day or more).

Nothing in this plan is a restyle. His first line was "app is nice", and that is
the constraint: the job is to find what makes a nice app read as incoherent, not
to redecorate it.

---

## 1. What the reviewer got right, and what he got wrong

### Right, and worth acting on

| His words | What it actually is | Evidence |
|---|---|---|
| "Just looks a bit weird" | The most accurate line he gave, and it has mechanical causes he could see without reading code. Pills sitting under square blocks, two different FABs, and one fully rounded pill in the bottom nav of every tab. | `DashboardScreen.kt:817-828` are square 56dp OutlinedButtons; `:836-837` are fully rounded 40dp TextButtons inside the same sheet. `DashboardScreen.kt:261-270` squares and flattens its FAB; `FertilizerScreen.kt:79` is a bare `FloatingActionButton(onClick = ...)` keeping Material's 16dp corner and 6dp shadow. `MainActivity.kt:140` is `NavigationBar {` with every default taken. |
| "Design language is all over the place" | True, but one layer down from where it sounds. The decisions are coherent and documented; the adoption is not. One role, "this is a part of the page", has five appearances. | `Almanac.kt:122` SectionHead is called 17 times across 5 screens only. `SettingsScreen.kt:280` defines a private `SectionHeader` used 7 times. `AmbientScreen.kt:108` and `CareScreen.kt:118` use titleSmall SemiBold. Bare `labelLarge` is used as a heading 18 times. `Almanac.kt:86` Masthead has zero call sites anywhere in `app/` or `core/`. |
| "Like buttons" | Fill weight, not hue. The same committing action is a filled Button on nine screens and a FilledTonalButton on two, and the heaviest coloured mass in the app is twelve keypad digits. | Filled commits at `LogEventScreen.kt:245`, `PlantEditScreen.kt:337`, `PostMortemScreen.kt:151`, `WeightScreen.kt:402`, `WeighingScreen.kt:240`, `BackupScreen.kt:92`, `LightMeterScreen.kt:218`, `IntroScreen.kt:104`, `ExperimentDetailScreen.kt:168`. Tonal commits at `AmbientScreen.kt:171` and `CareScreen.kt:151`. Twelve tonal keys at `WeightScreen.kt:614` and again at `WeighingScreen.kt:275`. |
| "Some pages have wayyy too many forms" | True of one screen, and it is not the one the brief ranked first. Add plant renders 15 input controls in a single flat scroll with no sections and one uniform 16dp gap. | `PlantEditScreen.kt:84` sets `spacedBy(16.dp)` for the whole form. Nine OutlinedTextFields at `:99, :107, :114, :122, :131, :141, :154, :162, :170`, four chip groups at `:181, :247, :301, :326`, a Switch at `:210`, a Slider at `:234`. Save gate is `canSave get() = name.isNotBlank()` (`PlantEditViewModel.kt:80`). |
| "Use progressive disclosure" | Right advice, wrong target. It is already in the app in four places, and the thing missing is not the pattern but the component: there is no collapse or expand primitive in `core/ui` at all, so every screen that wanted disclosure reached for an `if` on state, which hides a field without telling the user it exists. | Gates at `LogEventScreen.kt:138, :158, :175, :209, :228`, each keyed to one `CareEventType` (`LogEventViewModel.kt:55-59`). No `AnimatedVisibility`, `Collapsible`, `Expandable` or `ExpandableSection` anywhere in `app/src/main` or `core/ui/src/main`. |
| "Fire name lol" | Keep the name. |  |

### Wrong, and acting on it literally would waste a day

**"Color schemes" does not survive reading the files.** There are zero `Color(0x`
literals in `app/`. All 88 in the project are in two `core/ui` files, 76 in
`Theme.kt` which is the palette itself and 12 in `EventColors.kt:25-37` which is a
six-category categorical palette with light and dark variants, deliberately held
within a few OKLCH lightness points of each other so no event type can read as a
warning. Every surface in 192 files goes through `MaterialTheme.colorScheme` across
200 call sites, and `core/ui/src/test/kotlin/.../ThemeTest.kt` fails the build when
body text drops below 4.5:1 on the surface it is actually drawn on (`:145-164`),
when the surface ramp stops climbing (`:94`), or when the text tiers collapse into
one lightness (`:126`). Primary tint is applied to an icon exactly once in the whole
app, at `DashboardScreen.kt:906`, so a primary-tinted glyph reliably means "tap to
log". Repicking the palette would spend a day changing the strongest part of the
codebase. What he saw is the distribution of filled area and the two uncontrolled
tonal surfaces, which is workstream 1.

**Typography is not it either.** 268 `MaterialTheme.typography` calls over 12 styles
with 5 explicit `fontSize` overrides, and both families were chosen by measuring the
font binaries for tabular digits (`Type.kt:11-46`). Type is single-sourced. The
heading problem in section 1 is which style a heading picks, not what the styles are.

**"All over the place" overstates two areas.** The icon vocabulary is disciplined:
30 of 32 IconButton sites are top-bar navigation, the only exceptions being
`PropagationScreen.kt:209` and `:217`. FilterChip is consistent at every site, and
the destructive-action pattern is consistent at 8 of 9. The real pattern is narrower
than his phrase, which matters because a narrow list is finishable and "all over the
place" is not.

**"Look up laws of UX" is advice to a project that already did the equivalent and
wrote it down.** `Almanac.kt:30-42` argues the rule-versus-card case,
`BranchingMark.kt:14-28` cites the fractal-dimension stress-recovery result and the
Dark Patterns of Cuteness argument for refusing a mascot, `DepletionThumbnail` at
`DashboardScreen.kt:673-684` rejects a progress bar because a bar filling to full is
the grammar of task completion, and `Grain.kt:23` states the "if a user notices it
there is too much of it" rule. The defect is not that the principles are unknown, it
is that screens bypass the components those principles were encoded into. A generic
laws-of-UX pass is also actively risky here, because the standard onboarding
recommendation is a setup checklist with a completion indicator, and that is a
progress meter this project has already argued itself out of.

### Corrections to the brief's own measurements

Quoting these back later would be quoting wrong numbers.

| Brief says | Actual | How it was checked |
|---|---|---|
| ZERO hardcoded `Color(0x...)` anywhere | Zero in `app/`, 88 in `core/ui` (76 in `Theme.kt`, 12 in `EventColors.kt`) | `grep -rn "Color(0x" app/ core/` |
| Chip 20, FilterChip 19, AssistChip 1 | `Chip(` matches as a substring of the other two. Bare `Chip(` matches zero times. The app has 7 action affordances, not 8 | `grep -o "[^rt]Chip("` returns nothing |
| 31 distinct `.dp` literals | 15 distinct values across gap-shaped call sites. The other 16 are content dimensions that legitimately vary (200dp charts, 110dp table columns, 300dp hero, 96dp thumbnails) | Restricting the grep to `padding(` and `spacedBy(` gives 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 48 |
| LogEventScreen 17 controls is the densest form | 17 is the static declaration count. Five of the blocks are mutually exclusive by construction, so a watering renders about four controls | `LogEventViewModel.kt:55-59` keys each gate to exactly one type |
| PlantEditScreen 12 controls | 15 input controls, plus Save and three lifecycle buttons | counted above |

---

## 2. The one root cause

There is one, and all six lenses are describing it from different angles.

**`core/ui` is a real design system with its decisions written down, and it is
half adopted, because adoption is per file and voluntary.**

Every component in `core/ui` shadows a Material name so that a screen converts by
changing an import rather than by editing call sites: `Button`, `FilledTonalButton`,
`OutlinedButton` and `FilterChip` in `Buttons.kt`, `Card` in `Panel.kt`, `Rule`,
`DoubleRule`, `SectionHead`, `ColumnHead`, `ScreenTitle` and `Masthead` in
`Almanac.kt`. That design makes conversion cheap, and it also makes non-conversion
invisible, so drift is proportional to how many screens were written after the
component existed and nobody went back. The evidence is all the same shape:

- `Masthead` has zero call sites, and the intro and the dashboard each hand-roll it.
- `ScreenTitle` is imported by 24 files and called by 19. Five files import it and
  never call it: `PlantEditScreen.kt:3`, plus PlantDetail, Weight, Experiments and
  ExperimentDetail.
- `SectionHead` is called 17 times across 5 screens, while 18 bare `labelLarge`
  Texts and three private heading composables do the same job elsewhere.
- `Rule` exists, and there are still 19 raw `HorizontalDivider` calls in `app/`,
  7 of them in SettingsScreen alone.
- `ui.Card` is imported by eleven screens, and `ExperimentsScreen.kt:11` still
  imports `androidx.compose.material3.Card`, which is the one page in the app that
  still renders filled tonal slabs.
- `Buttons.kt` fixes shape and height for four of the seven affordances. TextButton,
  IconButton and FloatingActionButton were never wrapped, so they resolve to Material
  and keep the pill.
- `asTargetDryness()` at `SpeciesCare.kt:60` is documented as "prefills the plant's
  own care profile from this entry" and has zero callers.

There is one axis where this is not an adoption failure but an absence: **spacing
never got a component at all.** There is no `Space.kt`, no `Dimens` object and no
spacing constant anywhere in `core/ui`; the only dimension constant in the module is
`BlockHeight = 52.dp` in `Buttons.kt:37`, which is a button height. So every author
re-decided every gap by hand, 15 distinct values across roughly 318 call sites, with
2, 6, 10, 14 and 18dp accounting for about 22 percent of them against the project's
own written 4dp grid (`UI-SPEC.md:264`). That is why colour and type look right while
the app still reads as approximate: those two have a single source of truth and
spacing has none.

The second-order consequence, visible in `DashboardScreen.kt:848` where
`SheetBlock = 56.dp` silently overrides `BlockHeight = 52.dp`, is that a fix applied
at a call site reopens an issue the wrapper had already closed. That is the mechanism
to kill, not any individual mismatch.

---

## 3. Workstreams, ordered by impression change per unit of effort

| # | Workstream | Effort | Impression gain |
|---|---|---|---|
| 1 | Shape and fill: finish the wrapper layer and the four stray imports | M | Highest. It is literally what "looks a bit weird" names, and it touches every screen |
| 2 | One heading voice | M | Highest per hour. Five voices collapse to one, and it is the clearest cause of "all over the place" |
| 3 | One container grammar for lists, and one watering-answer component | M | High. Fixes the tab-to-tab change the reviewer would have met in his first thirty seconds |
| 4 | The spacing scale, then the conversion pass | S then L | High but slow. Nothing moves more than 4dp, which is why it reads as tightening rather than redesign |
| 5 | The Disclosure primitive, then Add plant | M then L | High on the one screen he was right about, and it unblocks four other findings |
| 6 | The first-run path | M | High for a new reader, zero for an existing user |
| 7 | Safety and gesture collisions | S each | Low visual gain, real correctness gain. Do it because it is cheap, not because he saw it |
| 8 | The navigation layer | L | High, and the most likely to break things. Needs its own week |
| 9 | Write-only fields: decide the direction | M | Removes form cost rather than changing appearance |
| 10 | Docs refresh | S | Zero user-visible gain, and the next plan is made from these documents |

### 1. Shape and fill coherence

**What it is.** Finish `core/ui/Buttons.kt` so that all seven affordances are wrapped,
then fix the four one-line import slips and the two uncontrolled Material surfaces.
Add `TextButton`, `IconButton` and a squared flat `FloatingActionButton` with
`shape = MaterialTheme.shapes.small`, zero elevation and `heightIn(min = 48.dp)` on
the text variant. Delete `SheetBlock` and raise `BlockHeight` to 56dp so there is one
height again. Pass `containerColor = MaterialTheme.colorScheme.surface,
tonalElevation = 0.dp` to the NavigationBar, replace its pill with
`NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)` plus a 2dp
`onSurface` rule above the selected item, and put a `Rule()` along the bar's top
edge. Make the keypad keys ruled rather than filled, and lift the keypad into
`core/ui` because `WeightScreen.kt:598-636` and `WeighingScreen.kt:262-289` are the
same composable twice.

**Files.** `core/ui/.../Buttons.kt`, `MainActivity.kt:140-158`,
`FertilizerScreen.kt:79`, `ExperimentsScreen.kt:11`, `ExperimentDetailScreen` chip
import, `PropagationScreen.kt:21` (unused `RoundedCornerShape` import),
`DashboardScreen.kt:848`, `WeightScreen.kt:614`, `WeighingScreen.kt:275`, plus the
roughly 20 files that already import the other wrappers.

**Effort.** M. No call site needs editing beyond its imports, which is the point of
the shadowing pattern.

**What it fixes.** The pill-under-block mismatch, the two different FABs, the one
rounded shape and the one unearned tonal band at the bottom of all three tabs, the
second height constant, and the largest block of coloured area in the app sitting on
twelve input characters.

**What it does not fix.** It does not say which fill means what. That is the written
rule in workstream 2's commit, and without it the same drift returns on the next
screen.

**Anti-goal check.** A rule marking which tab you are on is state, not score. It
introduces nothing resembling a streak or a progress mark.

### 2. One heading voice, and one written vocabulary

**What it is.** Two halves that belong in one change. First, delete the five local
heading variants and route every in-page heading through `SectionHead`, with an
optional `rule: Boolean = true` for the two places that want a quieter label. Call
`ScreenTitle` in the five files that already import it. Either adopt `Masthead` on
the intro and the dashboard so the two mastheads cannot drift, or delete it, because
leaving it unused is the condition that produced this. Replace the 19 raw
`HorizontalDivider` calls with `Rule`, excluding the legitimate menu divider at
`DashboardScreen.kt:238`. Second, write one sentence per affordance into
`docs/UI-SPEC.md` and into `Buttons.kt`'s KDoc, then make the code match it:

- filled `Button` writes the record, one per surface
- `FilledTonalButton` starts a job that commits elsewhere, or acts in place
- `OutlinedButton` is the alternative or the non-writing action on the same surface
- `TextButton` explains, dismisses, or acts in a dialog
- `IconButton` is top bar only
- `FilterChip` filters or picks among existing data, and is never an action
- where two actions are both valid outcomes of the same question they take equal
  weight, and that surface has no filled primary at all

Then flip `AmbientScreen.kt:171` and `CareScreen.kt:151` to `Button`,
`PlantDetailScreen.kt:356` to `OutlinedButton` since it only navigates, and replace
`CareScreen.kt:257`, the app's only AssistChip, with an OutlinedButton carrying an
external-link glyph.

**Files.** `core/ui/.../Almanac.kt`, `SettingsScreen.kt:280-287`,
`AmbientScreen.kt:108`, `CareScreen.kt:118`, `RemindersHelpScreen.kt:195`,
`ScaleHelpScreen.kt:82`, `PlantEditScreen`, `LogEventScreen`,
`ExperimentDetailScreen`, `PostMortemScreen`, plus `docs/UI-SPEC.md`.

**Effort.** M for the headings, S for the vocabulary document and the three button
flips. Settings is the screen to convert first, because it is a top-level tab and
its private `SectionHeader` is the furthest from the house style.

**What it fixes.** The single most legible cause of "design language is all over the
place". A user walking Settings, then Care, then Stats currently sees the section
heading change from sentence-case titleMedium with no rule, to sentence-case
titleSmall with no rule, to letterspaced caps over a hairline. `Almanac.kt:156`
already documents this exact failure one level up, for screen titles, and fixed it
with `ScreenTitle`; the same fix was never carried down.

**What it does not fix.** The gaps above and below those headings, which take four
values and belong to workstream 4. Add a Konsist or lint rule in the same commit so a
raw heading or a raw divider cannot come back, or this returns.

**Anti-goal check.** Do not apply the "one filled primary per surface" rule to
`DashboardScreen.kt:803` and `:809`. A normal hierarchy pass would promote "Watered"
to filled and demote "Still wet", which silently scores watering above restraint.
The code is already right and the reason is written at `:801`.

### 3. One container grammar, and one watering answer

**What it is.** `Almanac.kt:30-42` and `Panel.kt:14-26` both argue in writing that a
card is the wrong container for a list of plants and that a rule says "same page,
further down". `PlantCard` honours that and closes each entry with `Rule()` at
`DashboardScreen.kt:604`. Eighty lines above it on the same screen, archived plants
render as bordered Cards at `:304`, and on the Due tab every plant is a bordered Card
at `DueScreen.kt:103`. Convert both to the ruled-entry form. Then extract one
`WateringAnswer` composable into `core/ui` with the icon pair and the labelled pair as
two sizes of the same component, and have the card, the quick sheet and the Due screen
all use it.

**Files.** `DueScreen.kt:103-146`, `DashboardScreen.kt:304`,
`DashboardScreen.kt:583-601` and `:801-812`, new file in `core/ui`.

**Effort.** M.

**What it fixes.** The grammar change a user meets by tapping between tab one and tab
two, which is the most likely thing the reviewer saw, since he would only have opened
the three bottom-nav tabs. It also makes the equal-weight anti-goal a property of one
component with one test instead of a convention three screens have to remember.
`MenuLabels.kt:6-11` already records four help pages going stale in an hour because a
label existed in two copies, and this answer pair exists in four.

**What it does not fix.** Do not convert the Cards that are genuinely boxed notes
rather than list items, such as `NotCalibratedCard` at `WeightScreen.kt:567` and the
repot warning at `PlantEditScreen.kt:280`. Panel's boxed note is a different and
correct use.

### 4. The spacing scale

**What it is.** Add `core/ui/.../Space.kt` with seven named steps and nothing else:
`Hair = 2.dp`, `Tight = 4.dp`, `Line = 8.dp`, `Entry = 12.dp`, `Block = 16.dp`,
`Section = 24.dp`, `Page = 32.dp`. Document `Hair` as the one deliberate sub-grid
value so it stops being a free choice. Map the strays mechanically: 6 and 10 become
`Line`, 14 becomes `Entry`, 18 and 20 become `Section`, 28 becomes `Page`. Then do
three things in order:

1. Give `Panel.kt`'s Card a `contentPadding: PaddingValues = PaddingValues(Space.Entry)`
   parameter and apply it inside, so the inset is a property of the component rather
   than something all 18 call sites invent. Today the same hairline box insets its text
   by 12dp on the Plants tab (`DashboardScreen.kt:306`) and 16dp on the Due tab
   (`DueScreen.kt:104`), which is two of the three top-level tabs.
2. Standardise on one spacing idiom. Eleven screens put `verticalArrangement` on the
   root Column and nothing on the children; ten put nothing on the root and a
   `padding(top =)` on every child; `CareScreen.kt:92` does both, so the real gap at
   `:108` is 4 plus 8 and at `:169` is 4 plus 20, which means the number written at a
   call site is not the gap the user sees. Convert CareScreen first, then WeightScreen
   and PlantDetailScreen, which hold 30 of the top-paddings between them.
3. Define exactly two flanking conventions for a rule and remove the per-call-site
   padding from the 18 `Rule` and `DoubleRule` sites, which currently pick from seven
   different values.

Drop the `top: Int = 16` parameter from `WayfindingScreen.kt:137` as part of the same
pass, because spacing being an argument of the content is the clearest symptom there is.

**Files.** New `core/ui/.../Space.kt`, `core/ui/.../Panel.kt`, then roughly 25 screens.

**Effort.** S for the scale file, L for the conversion. It is the one workstream that
genuinely needs more than a day, and it can be done screen by screen without a flag day.

**What it fixes.** The Law of Proximity only carries information if the distances are
quantised into clearly distinct tiers. With 6 and 8, and 10 and 12, and 14 and 16 all
in use for the same job, grouping stops signalling anything. On `PlantEditScreen.kt:84`
a single uniform 16dp means the distance from a label to its own control is identical to
the distance between two unrelated sections, so proximity is actively asserting that
nothing belongs together.

**What it does not fix.** Content dimensions, which legitimately vary, and which is why
the brief's count of 31 overstates the problem.

### 5. The Disclosure primitive, then Add plant

**What it is.** One composable in `core/ui/Almanac.kt` in the existing paper-and-hairline
voice: `Disclosure(label, summary, content)`, with a letterspaced-caps label over a Rule
matching `SectionHead`'s typography at `Almanac.kt:122-132`, the current value as a
summary on the right, and a chevron. Build this first. Then restructure the new-plant
path only, since the full form is correct for editing: first screen is Name, Species,
Location, "when did you last water it", Add plant, and everything else sits behind three
collapsed sections that show their current value in the header: "What it is", "Care
profile", "Weighing".

Wire the prefill while in there. `SpeciesCare` carries `light`, `water`, `medium` and
`depletionTrigger` (`SpeciesCare.kt:41-45`), `asTargetDryness()` at `:60` is documented
as the prefill and has zero callers, and `CareViewModel.applySuggestions`
(`CareViewModel.kt:108-130`) already copies all four onto a plant behind a four-step
post-hoc path. Call `findSpeciesCare` from `PlantEditViewModel.onSpecies`, fill those
four, and show one quiet line under the species field: "Filled light, dryness and the
depletion trigger from the Monstera notes. Change any of them below." Reuse the
`current.lightNeeds ?: care.light` precedence at `CareViewModel.kt:115-116` so a user
edit always wins. That makes the Care profile section open already answered and
attributed, so collapsing it costs nothing, and it makes the add-time AlmanacDialog
redundant on matched species, which removes an interruption rather than adding one.

Then collapse the three lifecycle actions at `PlantEditScreen.kt:352-371` behind one
"This plant is finished" row. Three same-shaped full-width targets where two are
irreversible is a geometry this codebase has already learned to avoid twice by its own
comments, at `PlantDetailScreen.kt:553-560` and `FertilizerScreen.kt:125-127`.

**Files.** `core/ui/.../Almanac.kt`, `PlantEditScreen.kt`, `PlantEditViewModel.kt`,
later `SettingsScreen.kt:68-101` and `FertilizerScreen.kt:96-154`.

**Effort.** M for the primitive, L for Add plant.

**What it fixes.** The one form complaint that is true, and it fixes it by making the
form stop contradicting its own opening line, which already says at `:91-96` that only
the name is needed and then presents nine more fields as if it did not.

**What it does not fix.** It does not touch LogEventScreen's real defect, which is the
14-chip picker in front of its disclosure rather than the disclosure itself.

**Anti-goal check.** No completeness meter and no "profile 3 of 7" on those headers. The
header states what the section is for and what it currently holds, never how much is
missing. When the SettingsScreen rationale moves behind a Disclosure, the
`contentDescription` merge at `SettingsScreen.kt:299-301` has to move with it, or the
screen reader loses the explanation, which is the exact regression the comment at
`:296-299` records fixing once already.

### 6. The first-run path

**What it is.** Four small changes that mostly are not styling. Add `.grain()` so the
intro is not the one screen rendering on flat colour: `MainActivity.kt:168` applies it to
the NavHost modifier only, and `IntroScreen` is composed at `:120` and returns past it at
`:121`, so the first impression of the app is the version `Grain.kt:17-21` calls a void.
Move `ensureNotificationPermission()` out of the `LaunchedEffect` at `MainActivity.kt:427`
and into the moment a reminder is actually created, which the docstring at `:432-434`
already names as the right trigger; today it fires the instant the user taps "Open the
diary", so an Android system dialog about reminders arrives before anything exists to
remind about. Give the empty dashboard one call to action rather than a filled FAB and a
tonal centre button both calling `onAddPlant`, and promote the survivor to the filled
`Button` every other primary action uses. Make `NOT_CALIBRATED` speak on the card instead
of returning null at `DashboardScreen.kt:659`, using the copy that already exists in
`SuppressionHelp.kt:32-40`, shortened to something like "Weigh it once after watering to
start".

Then extract one `EmptyState` composable into `core/ui` next to `Panel.kt` and convert the
seven near-identical copies at `DashboardScreen.kt:387`, `DueScreen.kt:78`,
`WeighingScreen.kt:82`, `LocationsScreen.kt:85`, `PropagationScreen.kt:79`,
`CareScreen.kt:58`, `WeightScreen.kt:140` and `PlantDetailScreen.kt:431`. Pick
titleMedium, since five of eight already use it. Decide once whether `BranchingMark`
appears on all of them or only on the dashboard, and write the reason in the docstring,
because the current state is that a mark with a careful 15-line rationale appears exactly
once, at `DashboardScreen.kt:393`.

**Files.** `MainActivity.kt`, `IntroScreen.kt`, `DashboardScreen.kt`, `DueScreen.kt`, new
file in `core/ui`, plus the five other empty states.

**Effort.** M.

**What it fixes.** The differentiator being absent from every screen in the first run
except the intro. A new plant has no anchors, so `Prediction.kt:93` returns
`NOT_CALIBRATED`, which maps to null, so the first card reads just "Never watered" with no
ring. Someone opening the app cold sees a competent ordinary plant list, which is exactly
what "app is nice, just looks a bit weird" sounds like.

**What it does not fix.** Nothing here helps an existing user with plants already in the app.

**Anti-goal check.** The `NOT_CALIBRATED` line is a statement of fact about the pot and it
disappears the moment there is one reading, so it cannot become a standing reproach. On the
Due tab, split the branch on whether any plants exist and say what the tab is for rather
than "you're up to date", but do not replace it with a count, because a counter that can
read zero is a score with one value.

### 7. Safety and gesture collisions

Four cheap fixes with little visual payoff and real correctness payoff.

| Fix | Evidence | Effort |
|---|---|---|
| Give `AmbientRow`'s delete the two-step confirm the other eight destructive actions use, with `colorScheme.error` on the second label | `AmbientScreen.kt:234` is `TextButton(onClick = onDelete)` firing `viewModel.delete(reading.id)` immediately, and it is the only one of nine not coloured as destructive. On these screens a TextButton otherwise means "History" or "Dismiss" | S |
| Pick one long press per plant card | `DashboardScreen.kt:437-442` puts `combinedClickable` on the whole row, and `:899` puts another on the 48dp droplet inside it, so holding the row and holding the droplet do different things with an invisible boundary. Keep the sheet, drop the droplet's, and reach the detailed form from the sheet's "More" at `:828` | S |
| Put 8dp between the two LogActions | `DashboardScreen.kt:444-447` is a Row with no `horizontalArrangement`, and `LogAction` at `:895-901` is exactly `size(48.dp)` with no margin, so two targets of opposite meaning share a boundary, and a miss between them opens the plant. `:148-150` already records being bitten by a neighbouring-target miss | S |
| Replace the two non-top-bar back arrows with stage-shaped affordances | `PropagationScreen.kt:209` and `:217` use `AppIcons.arrowBack` to move a cutting between stages, 20dp from the real back arrow at `:71`, and they are the only 2 of 32 IconButtons not in a top bar | S |

**Anti-goal check.** Do not enlarge one of the two log targets over the other. Making
"Watered" easier to hit than "Still wet" weights one answer above the other, which is the
anti-goal the symmetric pair exists to respect.

### 8. The navigation layer

**What it is.** The largest workstream and the one most likely to break something, so it
gets its own week rather than a slot in this pass. Four parts, in order:

1. One tab navigation pattern. `MainActivity.kt:141-158` gives Plants
   `popUpTo(Routes.DASHBOARD) { inclusive = true }` and the other two only
   `launchSingleTop = true`, and nothing uses `saveState` or `restoreState`. So Plants,
   Due, Settings, Due leaves four back-stack entries with Due in it twice, while going to
   Plants throws away the plant list's scroll position every time. Use
   `popUpTo(nav.graph.findStartDestination().id) { saveState = true }`,
   `launchSingleTop = true`, `restoreState = true` for all three. This is the cheapest
   single contributor to "just looks a bit weird" in the whole review, and it is S.
2. Stop treating the dashboard overflow as a menu. 32 routes are registered against 32
   constants, exactly 3 are visible as tabs, 14 entry points live in two overflow menus,
   and `SCAN`, `EXPERIMENTS`, `EXPERIMENT` and `STICKER` have no reachable entry point at
   all on a fresh install because `advancedFeatures` defaults to false
   (`Settings.kt:63`). Un-gate the advanced features or let them appear when they have
   content, and make the dashboard overflow a "More" destination listing the six jobs with
   one line each, which `WayfindingScreen.kt:70-81` already writes.
3. Collapse the four pure explainers into one Help screen of expandable sections, keeping
   as routes only the two that do something: `RemindersHelpScreen`, which acts on Android
   settings, and `WhyNoPredictionScreen`, which is reached from a refusal. That takes the
   documentation share of the destination graph from 8 of 32 down to 3, and it removes six
   full-width filled "Open" buttons that currently carry the same visual weight as "Log
   it". Add the backup help link to `BackupScreen`, which is where the question is asked.
4. Apply the rule the app already wrote. `WeightScreen.kt:157-163` states it: "Weighing is
   an act, not a view, so it moved into a sheet and the page became a page." Something you
   read and return from is a sheet, so `StickerScreen`, `ScaleHelpScreen` and
   `WhyNoPredictionScreen` lose their routes. Something that changes what the plant is
   stays a route, and those get an explicit cancel or a discard confirmation, because
   `PlantEditScreen.kt:69-75` currently wires its back arrow straight to `onDone` and a
   fifteen-control form discards silently.

**Effort.** L, and part 1 alone is S.

**What it fixes.** The destination grammar, which is the layer the reviewer's "all over
the place" is true in but that he could not have named. Also the reason "read this help
topic" ended up as a filled primary button: because each topic is a destination, each
needed a navigation control, and the heaviest one got used.

**What it does not fix.** It will not look different on any single screen, which is why it
is eighth despite being important.

**Anti-goal check.** Keep the Due tab free of counts and badges. A number on a tab is a
scoreboard.

### 9. Write-only fields

Four Log event controls collect data no screen in the app ever shows: `method`,
`fertilizerName`, `dilution` and `cause`, written at `LogEventViewModel.kt:173-179`,
collected at `LogEventScreen.kt:147-155, 179-206, 228-235`. The only two readers of a
stored event are `label()` at `PlantDetailScreen.kt:824-831`, which renders type plus the
still-wet suffix plus `amountMl`, and `EntryEditor` at `:644-700`, which edits the day and
the note. `fertilizerCadenceDays` is the same defect in the densest form: stored at
`PlantEditViewModel.kt:238` and read by nothing, because `ReminderKind` is `{ CHECK, TASK }`
and the only reminder anything creates is CHECK.

Decide the direction before touching the form. Either build the reader that
`UI-SPEC.md` section 4 already promises, which is expand-in-place on the timeline row, and
the fields earn their place behind a single "Details" disclosure inside the type branch; or
drop the controls and keep the columns for import compatibility. Do not do the third thing,
which is leave them inline and unexposed. Effort M either way.

Also fix the fork in the data while here: "It died" at `PlantEditScreen.kt:352-358` routes
to the post-mortem, which writes a DIED event and archives the plant
(`PostMortemViewModel.kt:105, :112`), while picking DIED from the chip bank at
`LogEventScreen.kt:107` writes the event and leaves the plant active and unarchived, feeding
the weight model. Remove DIED from the picker, or route it to the post-mortem. One path is
better than two that agree.

### 10. Docs refresh

`docs/NAVIGATION.md:8` and `:148` both say 28 routes; `Routes.kt` declares 32 and
`MainActivity.kt` registers 32. Section 3's two merges, Places absorbing Room conditions and
one Help screen, still read as done decisions and neither happened. `WayfindingScreen.kt:75-81`
advertises Scan and Experiments, which a default install does not have, and a map that is
wrong about the default build is worse than no map by that file's own standard. Move sections
3 and 5 into a "decided, not yet done" list, correct the count, and update `docs/HANDOVER.md`
in the same change, per this repo's convention that the live document changes with the work.
Effort S.

---

## 4. The first thing to do on Monday morning

**One task: the shape pass. Make every control in the app square, from the wrapper layer
only.**

Specifically, in one commit:

1. Add `TextButton`, `IconButton` and `FloatingActionButton` to
   `core/ui/.../Buttons.kt`, shadowing the Material names like the existing four, with
   `shape = MaterialTheme.shapes.small`, zero elevation, and `heightIn(min = 48.dp)` on the
   text variant.
2. Change the imports in the files that already import the other wrappers. No call site
   bodies change.
3. Delete `SheetBlock` at `DashboardScreen.kt:848` and raise `BlockHeight` to 56dp, so
   `Buttons.kt:31-33`'s claim that there is one height is true again.
4. Fix the four stray imports: `ExperimentsScreen.kt:11` to `ui.Card`,
   `ExperimentDetailScreen`'s `FilterChip` to the local one, `FertilizerScreen.kt:79` to the
   new FAB wrapper, and delete the unused `RoundedCornerShape` import at
   `PropagationScreen.kt:21`.
5. Give the `NavigationBar` at `MainActivity.kt:140` `containerColor = surface`,
   `tonalElevation = 0.dp`, a transparent active indicator and a 2dp `onSurface` rule above
   the selected item, with a `Rule()` along the bar's top edge.

Why this one. It is under a day, it is mechanical, it needs no design decisions, and it
removes the only fully rounded shape and the only unearned tonal surface from the bottom of
every screen the friend opened, plus the pill sitting under a square block inside the quick
sheet, which is the most likely literal cause of "looks a bit weird". It also closes the
mechanism rather than the instance: once the wrapper covers all seven affordances, a new
screen is correct by default, and the next `SheetBlock`-shaped call-site fix has nowhere to
hide. Verify it by screenshotting the three tabs and the quick-log sheet before and after,
not by reading the diff.

---

## 5. What to defer or refuse

### Refuse, on the evidence

| Thing | Why |
|---|---|
| Repick the colour palette or the type scale | Zero `Color(0x` in `app/`, 200 `colorScheme` call sites, one palette with semantic event colours, and `ThemeTest.kt` failing the build on contrast, ramp and tier collapse. 268 typography calls over 12 styles with 5 `fontSize` overrides. This is the strongest part of the codebase and "color schemes" is the one line of his review that does not survive reading it |
| A generic laws-of-UX conformance pass as a task | The principles are already encoded with citations in `Almanac.kt`, `BranchingMark.kt`, `Grain.kt` and `DepletionThumbnail`. The defect is adoption of the components those principles produced, which is workstreams 1 to 3. A generic pass would also import the standard onboarding checklist, which this project has already argued against |
| Split `PlantDetailScreen.kt` for its own sake | 929 lines, and most of them are the diary, which belongs there. Drive down the 400-line composable body instead, by moving the eight router entries out of the app bar and hoisting the photo set and the entry editor into shared components |

### Refuse, on the anti-goals

| Thing | Why it violates them |
|---|---|
| A setup checklist, a "profile 3 of 7" header, or any completeness meter on the new Disclosure sections | A progress indicator is a score. `DepletionThumbnail` at `DashboardScreen.kt:673-684` already rejects a bar filling to full because that is the grammar of task completion |
| A count or badge on the Due tab, or "0 due today" | A counter that can read zero is a score with one value |
| Promoting "Watered" to the filled primary in any hierarchy cleanup | It scores watering above restraint, which is the guilt mechanic the anti-goals exclude. `DashboardScreen.kt:801` and `:570-573` already say so next to the code |
| Enlarging one of the two 48dp log targets | Same violation expressed as geometry: an easier target is a weighted answer |
| A streak, a "you log mostly observations" summary, or any per-user statistic framed as a habit | Engagement mechanics. Reading the user's own event log to pick a sensible default is fine, because nothing is shown; showing them a figure about their own behaviour is not |
| A progress bar or spinner for the unloaded frame | If a skeleton is ever needed, use a static one made of rules and column heads. A bar filling toward complete is the grammar the design already refuses |

### Defer, with reasons

| Thing | Why later |
|---|---|
| The Places and Room conditions merge | `NAVIGATION.md` section 3 decided it and `AmbientScreen.kt` still exists at 237 lines with its own route, reachable only through `LocationsScreen.kt:77-79` inside `if (advanced)`. The full merge is M to L and competes with workstream 8. The cheap half now is to drop the `advanced` gate, because a feature already two levels inside an overflow does not also need a hidden switch |
| The closing line at the end of a weighing round | `WeighingViewModel.kt:180-188` calls `close()` on the last pot, so the most effortful thing the app asks for ends by the sheet vanishing, while one dashboard tap gets a haptic, a snackbar and an undo. The fix is one factual line about what the round bought, for example "six weighed, four curves gained a point, two can now predict", derived from the rows just saved. It is right and it needs careful copy, because every nearby phrasing is a score. Do it after workstream 3 |
| Grouping the 14-chip type picker and reordering it by what the user actually logs | Real Hick's Law cost on the step that gates the whole form, and it depends on removing DIED from the picker in workstream 9 first. Grouping under three labels is M. Reading the user's own log for the default is allowed because nothing is displayed |
| Reworking `PlantDetailScreen`'s overflow menu | It renders between 5 and 8 items depending on three conditions, so "Edit plant" lands in a different position per plant and muscle memory learns nothing. The right fix depends on workstream 8's decision about where the index lives |
| Location as chips rather than free text | `knownLocations` at `Locations.kt:57-69` exists and `AmbientScreen.kt:106-125` already renders exactly the needed pattern, so this is S, but it belongs in the Add plant restructure rather than as a separate change |
| `ScreenTitle`'s ellipsis | `Almanac.kt:158-167` sets `maxLines = 1, overflow = Ellipsis` with uppercase at 0.18em tracking, so a plant's only identifier on its own page is pre-committed to dropping characters. Allow two lines, or drop the tracking above about 1.3 font scale, and screenshot one long name at the largest system size to see which is needed. There is no `fontScale` handling anywhere in the project, so this is the start of a separate accessibility pass |
