# Edit plant: the captions, before and after

Applied 2026-10-07. The edit form in 9c0f660, Settings in the commit after it.

The form carries about 400 words of permanent explanation, a paragraph under
nearly every control. That is what makes it read as cluttered, rather than the
boxes: the page is teaching while you are trying to fill it in.

The rule used below: **keep the fact, cut the reasoning.** A caption says what
the control does or what will happen. Why it works that way belongs in the help
screens, which already exist. One exception is kept and marked.

## The cuts

| # | Control | Now | Proposed |
|---|---|---|---|
| 1 | Form intro | Only the name is needed. Everything else can wait, or stay empty, because the app fills most of it in from what you log. (121) | Only the name is needed. The rest can stay empty. (48) |
| 2 | Species prefill | Filled {fields} from the {source} notes. Change any of them below. (89) | Filled {fields} from the {source} notes. (41) |
| 3 | Shares a pot | Another plant lives in the same pot or jar. (43) | *(cut: the label already says it)* (0) |
| 4 | Sharing effect | Watering, checking and feeding will be recorded on {plant} too, since it is the same pot. Everything else stays on this plant alone. (146) | Watering, checking and feeding also log on {plant}. (51) |
| 5 | Sharing weight | Weighing stays with {plant}: a pot weighs as one object, so only one plant in it can be the one on the scale. (123) | Weighing stays with {plant}. (28) |
| 6 | Pick a pot | Pick the plant it shares with. (30) | *(unchanged: an instruction, not an explanation)* (30) |
| 7 | Source chips | Cutting / *tracked on the propagation board*, Volunteer / *turned up on its own* | *(subtitles cut from the chips; the enum keeps them)* |
| 8 | Default water | Watering logs this by default, so you never retype it. (54) | Watering logs this by default. (30) |
| 9 | Weigh this pot | Turn this off for a pot where weight says nothing, like a closed terrarium that recycles its own water. It leaves the weighing round and stops being asked about. (160) | Off for pots where weight says nothing, like a closed terrarium. (64) |
| 10 | Depletion trigger | How much of the pot's water range is used up before this plant wants watering. Around 30% for moisture-lovers like ferns and fittonia, 50% for most foliage plants, 70% or more for succulents and other drought-lovers. (215) | How much of the pot's water range is used before watering. 50% suits most foliage plants. (89) |
| 11 | Scale step | A daily loss smaller than one step is rounding, not drying, so this decides when the app stays quiet rather than guessing. (123) | Losses smaller than one step are rounding, not drying. (54) |
| 12 | Weighing method | Measuring it a different way changes every reading by a constant, so the full and dry marks describe a measurement that no longer exists. Weigh it once after watering and they set themselves again. (195) | Changing this clears the full and dry marks. Weigh once after watering to set them again. (89) |
| 13 | Last watered | A plant you add today already has a history. This starts its first reminder from the right day. (96) | Starts the first reminder from the right day. (45) |
| 14 | Care notes dialog | There are care notes on file for this one: how much light it wants, how dry to let it get, and what usually goes wrong. (118) | Care notes are on file for this one. (36) |

**1513 characters down to 605, a cut of 60%.**

## The two judgement calls

**#12 keeps its consequence, and that is deliberate.** It is the only caption
here that warns about losing data: changing how you weigh a pot throws away the
full and dry marks. A caption that said only "changing this clears the marks"
would be a shorter warning and a worse one, so the recovery stays in.

**#10 keeps one number out of three.** The three worked percentages are genuinely
useful reference, and they are also the single longest caption on the page. One
anchor is enough to act on; the other two belong in the help screen with the
rest of the watering model.

## The boxes, half done 2026-10-07

Not the boxes, when this was written. One of the three is now fixed: an
unselected FilterChip was a transparent container inside a hairline outline,
which at this corner radius is the same drawing as an OutlinedTextField, so the
form showed nine identical rectangles with nothing to say which you tap to
choose and which you tap to type in. Chips are filled and borderless now, on
the surface ramp Theme.kt measures, and the two vocabularies separate. It is a
change to the wrapper in core/ui, so it holds everywhere rather than on this
screen.

Still open: the five empty full-width fields that take about a third of the
screen while holding nothing, and the fact that a field with a value notches
its label into the border while an empty one puts the label inside the box, so
the same control draws two ways depending on data.

## The original note

Not the boxes. The 16 same-weight rectangles, the chips that look like text
fields and the five empty full-width fields are a separate question and a
separate change. Cutting #7's chip subtitles happens to fix the staircase in
the source row, because the Cutting chip was double height and 634px wide
against Gift at 178, but that is a side effect rather than the aim.

---

# The rest of the app, surveyed 2026-10-07

27,484 characters of user-visible prose across 25 screens. Most of it should
stay, and the reason is worth writing down: the edit form's problem was a
specific one, not "too many words".

## Where the same pattern recurs

**Settings, and only Settings.** `SettingRow(title, subtitle, control)` is
structurally the same thing as the edit form's "Weigh this pot" plus paragraph
plus switch, and it has three rows whose subtitles average **279 characters**,
five or six lines each under a toggle.

| Row | Now | Proposed |
|---|---|---|
| Offer care notes for a new plant | Just after you add a plant, if there are notes on file for its species, the app offers them. On by default: the moment you have typed the species name is the moment they are worth reading, and nobody goes looking in a menu for something they do not know is there. It only ever asks once per plant. (303) | Offers the notes on file for a species just after you add a plant. Once per plant. (81) |
| Look up unknown plant names online | Off by default, and the only thing in the app that can send anything anywhere. On, the care screen can resolve a name it does not recognise and link the Wikipedia article - it sends the name you typed and nothing else. It never fetches care advice, and never runs on its own. (275) | The only thing here that sends anything out: the species name, nothing else, to link its Wikipedia article. Never runs on its own. (131) |
| Show the specialist tools | Pot stickers and the scanner, experiments, and logging room temperature by hand. None of them is useless and none of them is for everybody: stickers pay off at thirty pots and a printer, and experiments assume you want to run a controlled test on a houseplant. (260) | Pot stickers and the scanner, experiments, and logging room temperature by hand. (79) |

**838 characters down to 298, applied.** The middle row is cut least on purpose: it is a
privacy disclosure, the only switch in the app that lets anything leave the
phone, and every fact in it is kept. What goes is the reassurance that it never
fetches care advice, which is already implied by saying it sends the name and
nothing else.

## Where it looks similar and is not

**WeightScreen has the most long prose in the app, 16 passages, and almost none
of it is caption.** "Drying about 20% faster than usual, and where it lives is
warmer than it was. That is the pot behaving normally in a changed room" is the
app's answer. It is the product. Cutting it would be cutting the feature.

**Empty states are required, not clutter.** `UI-SPEC.md` section 9 sets them out
as a table and names the wording: "No plants yet. Illustration plus 'Add your
first plant'. Not a blank screen." Half of what the survey flags on
FertilizerScreen, LocationsScreen, StatsScreen and PlantDetailScreen is exactly
that, and shortening it would be working against a decision already made.

**Destructive-action warnings keep their recovery**, by the same rule applied to
the weighing-method caption on the edit form.

**The help screens are prose by definition.** WayfindingScreen, BackupHelpScreen,
RemindersHelpScreen, ScaleHelpScreen and HelpScreen hold 8,384 characters
between them and that is the whole point of them. If anything they should be
taking text from elsewhere, which is where the cut captions went.

## CareScreen, a partial case

Eight long passages, of which the empty and error states stay. The candidates
are the editorial asides, such as "Your own log will outgrow generic advice
anyway" and "General guidance for the species, not for your pot", which say the
same thing twice on one screen. Worth a pass, smaller than Settings, and better
done when somebody is next in that file.
