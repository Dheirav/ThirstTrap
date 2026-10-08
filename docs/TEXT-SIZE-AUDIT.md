# Where the 12sp text is, and what it holds

Every use of `bodySmall` in the app, 96 of them across 21 files, with what each
one actually says. The question behind it is whether 12sp is the right size for
what is in it, and for most of these it is not.

## The sizes, for reference

`Type.kt` builds from Material 3's `Typography()` and changes only the family,
the letter spacing and the line height, so the sizes are the M3 defaults.

| Style | Size | Line height | What it is for here |
|---|---|---|---|
| `titleLarge` | 22sp | | screen titles |
| `titleMedium` | 16sp | | plant names, list row titles, serif |
| `bodyLarge` | 16sp | 26sp | help item names, one emphasis line |
| `bodyMedium` | 14sp | 22sp | help page prose |
| `bodySmall` | **12sp** | 18sp | **the 96 below** |
| `labelSmall` | 11sp | | section heads, 14 uses, all caps |

Android's own guidance puts body text at 14sp and reserves 12sp for captions and
annotations. So the test for each of these is simple: is it a caption, or is it
something somebody has to read?

## A. Prose, 59 of the 96

Full sentences that teach something or explain a control. This is the group that
should not be at 12sp, because none of it is an annotation. It is the app
explaining itself, and it is the writing you have spent this whole session
condensing.

The worst of it is where the explanation carries a decision. `PlantEditScreen`
has eight, including "How much of the pot's water range is used before watering.
50% suits most foliage plants", which is the sentence that tells somebody what
number to put in. `WeightScreen` has ten, including the diagnostic that says
water may be running down the sides of a shrunken root ball. `SettingsScreen`
has five, and every one of them is the reason to flip a switch.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/ambient/AmbientScreen.kt`**

- `:105` in `AmbientScreen` — Nothing here changes a prediction. It only explains one.
- `:165` in `AmbientScreen` — Either one on its own is useful. Both is better.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/backup/BackupScreen.kt`**

- `:93` in `BackupScreen` — Photos live inside the app rather than in your gallery, so  uninstalling ThirstTrap deletes them. Export before you  uninstall, change phone, or mo...
- `:109` in `BackupScreen` — Importing merges a backup into what is already here. Entries are matched  by their id, so importing the same file twice changes nothing.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/care/CareScreen.kt`**

- `:104` in `CareScreen` — Basic notes, from the bundled plant dataset.
- `:135` in `CareScreen` — Sets watering at ${(care.depletionTrigger * 100).roundToInt()}% dry,  along with the light and watering notes above.
- `:156` in `CareScreen` — General guidance for the species, not for your pot. Once you have  weighed this one a few times, its own drying curve is the better answer.
- `:202` in `LookupSection` — Looking the name up online is switched off. Settings has a toggle for it.
- `:251` in `LookupSection` — Still no care notes for this one. This says what the plant is, not  how to water it.
- `:268` in `LookupSection` — Online lookup is switched off in Settings. No network. Nothing else in the app needs one. No plant by that name in the GBIF backbone.  Check the sp...

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/dashboard/DashboardScreen.kt`**

- `:533` in `PlantCard` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/diagnose/DiagnoseScreen.kt`**

- `:96` in `DiagnoseScreen` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/experiments/ExperimentDetailScreen.kt`**

- `:108` in `ExperimentDetailScreen` — No subjects yet. Each subject is a plant, and its arm says what  it gets - \"banana water\", \"control\".
- `:160` in `ExperimentDetailScreen` — One way, on purpose: a conclusion that can be rewritten later is a  lab notebook in pencil. Getting it wrong is what the next  experiment is for.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/fertilizer/FertilizerScreen.kt`**

- `:156` in `FertilizerScreen` — Type the volume your can or bottle holds. Keep it and it becomes a button here. Tap a size to switch to it.
- `:232` in `FertilizerScreen` — That is ${d.concentrateMl.ml()} ml, too little to measure. Mix
- `:240` in `FertilizerScreen` — The dilution is not in a form this can work with. Try 1:200  or 5 ml/L.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/light/LightMeterScreen.kt`**

- `:201` in `LightMeterScreen` — Nothing lives here yet, which is usually why you are measuring it.
- `:212` in `LightMeterScreen` — Note what this plant wants in its details, and this screen will say  whether the spot suits it.
- `:233` in `LightMeterScreen` — Phone light sensors are not calibrated and differ between handsets, so  treat the band as the answer and the number as a hint.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/logevent/LogEventScreen.kt`**

- `:104` in `LogEventScreen` — Backdated entries are filed at midday, so they cannot sort ahead of  something you logged that morning.
- `:158` in `LogEventScreen` — The pot itself now weighs something different, so every reading  so far is measured against the wrong thing. Water it in and  weigh it once afterwa...
- `:242` in `LogEventScreen` — This clears the full and dry marks - the pot itself changed  weight, so every earlier reading now measures something else.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/more/MoreScreen.kt`**

- `:122` in `MoreScreen` — Two more, the pot sticker scanner and the experiment board, are  off until you turn on specialist tools in Settings. They need a  few plants to be ...
- `:140` in `Job` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/plantdetail/PlantDetailScreen.kt`**

- `:426` in `PlantDetailScreen` — No photos yet. The camera button above starts a record you can  compare against later.
- `:461` in `PlantDetailScreen` — One more photo and you can put them side by side, or play  them in order.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/plantedit/PlantEditScreen.kt`**

- `:113` in `PlantEditScreen` — Only the name is needed. The rest can stay empty.
- `:237` in `PlantEditScreen` — Pick the plant it shares with.
- `:351` in `PlantEditScreen` — Off for pots where weight says nothing, like a closed terrarium.
- `:379` in `PlantEditScreen` — How much of the pot's water range is used before watering.  50% suits most foliage plants.
- `:406` in `PlantEditScreen` — (built from data at runtime)
- `:421` in `PlantEditScreen` — Losses smaller than one step are rounding, not drying.
- `:440` in `PlantEditScreen` — Changing this clears the full and dry marks.  Weigh once after watering to set them again.
- `:454` in `PlantEditScreen` — Starts the first reminder from the right day.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/propagation/PropagationScreen.kt`**

- `:159` in `StageHead` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/settings/SettingsScreen.kt`**

- `:93` in `SettingsScreen` — Everything else - the catalogue, the predictions, the reminders, your whole  diary - works with no network at all, and always will.
- `:118` in `SettingsScreen` — Reminders are not timed to the minute, so a 9:00 one may arrive at  9:15. That keeps battery use tiny.
- `:136` in `SettingsScreen` — How dry a new plant should get before watering. Succulents more,  ferns less. Each plant can have its own.
- `:198` in `SettingsScreen` — Never exported. The diary lives only on this phone. Last export: today. Last export: yesterday. Last export: $days days ago. The diary lives only o...
- `:284` in `SettingRow` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/stats/StatsScreen.kt`**

- `:93` in `StatsScreen` — Counts, not scores. Nothing here goes up because you opened the app, and  a quiet month is a quiet month rather than a gap in a run.
- `:180` in `OutcomeTable` — Nothing has left your care yet, so there is no rate to give - and  an empty record is not a perfect one. Of the ${o.departed} that have left, ${o.g...
- `:272` in `PredictionTable` — (built from data at runtime)
- `:314` in `RootingTable` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/qr/StickerScreen.kt`**

- `:99` in `StickerScreen` — The code holds nothing but this plant's id - no data leaves the phone,  and it only means anything to this app.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/weighing/WeighingScreen.kt`**

- `:159` in `WeighingScreen` — (built from data at runtime)
- `:205` in `WeighingScreen` — Each reading moves that plant's prediction. Nothing else to do.
- `:247` in `WeighingScreen` — Watered and not weighed since, so this reading becomes the  new full mark.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/weight/WeightScreen.kt`**

- `:194` in `WeightScreen` — Grams over time. Once you weigh it just after watering, this becomes  a percentage and a prediction.
- `:225` in `WeightScreen` — One reading so far. Weigh it again in a day or two and the curve  starts here.
- `:246` in `WeightScreen` — The drying curve. Each drop is one cycle between waterings.
- `:314` in `WeightScreen` — Most recent ${shown.size} of ${s.readings.size}. Tap a row to correct  or remove it. Tap a row to correct a weight, mark it as not trustworthy, or ...
- `:359` in `WeightScreen` — (built from data at runtime)
- `:381` in `WeightScreen` — You watered this one ${wateredAgo(anchorMoment)}, so this reading  becomes the new full mark.
- `:425` in `WeightScreen` — Not set up yet: pick \"just watered\" on a weigh taken after  watering and draining, and that reading becomes this pot's full  mark. Anything else ...
- `:566` in `DiagnosticCard` — Worth checking whether water is running down the sides rather than  soaking in - a root ball that has shrunk away from the pot does that. The roots...
- `:817` in `AmbientWaiting` — (built from data at runtime)
- `:879` in `AmbientCard` — Drying about $pct% $pace than usual, and where it lives is $room  than it was. That is the pot behaving normally in a changed room. Drying about $p...

## B. The user's own words, 5

Notes that somebody typed into the app themselves, shown back to them at 12sp.
Harder to defend than anything else on this list: the app is rendering your
writing smaller than its own.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/ambient/AmbientScreen.kt`**

- `:241` in `AmbientRow` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/fertilizer/FertilizerScreen.kt`**

- `:250` in `FertilizerScreen` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/locations/LocationsScreen.kt`**

- `:173` in `LocationsScreen` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/plantdetail/PlantDetailScreen.kt`**

- `:613` in `EventRow` — (built from data at runtime)
- `:856` in `LinkedNote` — (built from data at runtime)

## C. Values in table rows, 8

Readings, dates, NPK figures, dilution strings and light bands inside tabular
rows. A real argument exists for keeping these small, which is density: a table
of twenty readings is easier to scan when the rows are short. The one that gives
me pause is the weight itself on the weighing round, because that number is the
whole point of the app.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/fertilizer/FertilizerScreen.kt`**

- `:202` in `FertilizerScreen` — (built from data at runtime)
- `:209` in `FertilizerScreen` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/locations/LocationsScreen.kt`**

- `:139` in `LocationsScreen` — (built from data at runtime)
- `:150` in `LocationsScreen` — , measured ${measured.format(Date(it))}
- `:164` in `LocationsScreen` — , ${measured.format(Date(a.timestampMillis))}

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/weighing/WeighingScreen.kt`**

- `:139` in `WeighingScreen` — (built from data at runtime)

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/weight/WeightScreen.kt`**

- `:292` in `WeightScreen` — (built from data at runtime)
- `:948` in `ReadingEditor` — (built from data at runtime)

## D. Warnings, 1

Import warnings on the backup screen, at 12sp. A warning is the one thing on a
screen that should not be the smallest text on it.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/backup/BackupScreen.kt`**

- `:149` in `BackupScreen` — (built from data at runtime)

## E. Labels and captions, 23

These are what 12sp is for and I would leave them alone: the dot separators,
"archived - history kept", "excluded", "Concluded", "Nothing stray.", the
stage captions on the propagation board, the "Losing about N g a day" line.
Short, glanced at, not read.

Two in here are arguable rather than clear. `SettingsScreen:100`, "When to check
for plants that need a look", is a label for the reminder times but reads as an
instruction. `WeightScreen:978`, "It stays in the record, struck through", is a
one-line explanation of what excluding does, so by content it belongs in group A.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/ambient/AmbientScreen.kt`**

- `:235` in `AmbientRow` — ·

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/care/CareScreen.kt`**

- `:223` in `LookupSection` — Family $it
- `:231` in `LookupSection` — You typed $it, which is an older name for it.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/dashboard/DashboardScreen.kt`**

- `:295` in `DashboardScreen` — archived - history kept

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/experiments/ExperimentsScreen.kt`**

- `:165` in `ExperimentCard` — Concluded
- `:173` in `ExperimentCard` — else

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/light/LightMeterScreen.kt`**

- `:185` in `LightMeterScreen` — suits it too dark too bright not noted

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/plantdetail/PlantDetailScreen.kt`**

- `:367` in `PlantDetailScreen` — ·
- `:414` in `PlantDetailScreen` — else
- `:973` in `PlantHero` — ·

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/plantedit/PlantEditScreen.kt`**

- `:139` in `PlantEditScreen` — Filled ${joinNaturally(state.prefilled)} from the $from notes.
- `:225` in `PlantEditScreen` — Watering, checking and feeding also log on
- `:231` in `PlantEditScreen` — Weighing stays with ${shared.plantName}.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/postmortem/PostMortemScreen.kt`**

- `:129` in `PostMortemScreen` — - $it

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/propagation/PropagationScreen.kt`**

- `:117` in `PropagationScreen` — Nothing at this stage.
- `:195` in `CuttingCard` — just added moved here today 1 day here

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/settings/SettingsScreen.kt`**

- `:100` in `SettingsScreen` — When to check for plants that need a look
- `:225` in `SettingsScreen` — else  (${formatBytes(s.orphanFileBytes)})  and   else  whose file has gone
- `:231` in `SettingsScreen` — Nothing stray.

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/weighing/WeighingScreen.kt`**

- `:132` in `WeighingScreen` — just watered, this sets the full mark

**`app/src/main/kotlin/dev/dheirav/thirsttrap/feature/weight/WeightScreen.kt`**

- `:298` in `WeightScreen` — excluded
- `:489` in `PredictionHeadline` — Losing about ${-it.toInt()} g a day
- `:978` in `ReadingEditor` — It stays in the record, struck through.

## What I would do

Promote A, B and D, which is 65 of the 96, from `bodySmall` to `bodyMedium`.
That is one step, 12sp to 14sp, and it makes a sentence the same size everywhere
in the app including the help pages, which are at 14sp now.

Leave E alone, 23 sites, because that is what the size is for.

Decide C, 8 sites, separately. Density is a real argument in a table and I would
rather you looked at the weighing round before I change the number on it.

## What it costs

65 edits across 19 files, each one word. The risk is not the edit, it is the
reflow: some of these sit in rows with `heightIn(min = 48.dp)`, in table cells
with weights, or in cards sized around the current text, so a handful will want
looking at on the phone. The screens worth checking afterwards are the dense
ones: `WeightScreen`, `StatsScreen`, `FertilizerScreen` and `LocationsScreen`.
