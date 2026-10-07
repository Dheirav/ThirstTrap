# Edit plant: the captions, before and after

Proposed 2026-10-07. Nothing applied yet.

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

## What this does not touch

Not the boxes. The 16 same-weight rectangles, the chips that look like text
fields and the five empty full-width fields are a separate question and a
separate change. Cutting #7's chip subtitles happens to fix the staircase in
the source row, because the Cutting chip was double height and 634px wide
against Gift at 178, but that is a side effect rather than the aim.
