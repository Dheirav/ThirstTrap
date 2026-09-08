# The species catalogue

How the app answers "what is this plant and how do I look after it", and — more
important — where it refuses to answer.

Written 2026-09-08, when the catalogue went from 48 hand-written entries to
49 + 115 generated ones.

---

## 1. Two tiers, marked as such

| | Entries | Where it comes from | What it can say |
|---|---|---|---|
| `CareDetail.CURATED` | 49 | Hand-written | Light, water, humidity, toxicity, **what usually kills this plant**, and a trigger chosen deliberately |
| `CareDetail.BUNDLED` | 115 | Generated from open data | Light, water, humidity, toxicity, a trigger derived from an ordinal code |

The tiers are visibly different in the UI. A bundled entry carries the line
*"Basic notes, from the bundled plant dataset. Enough to set a starting point,
not enough to tell you what usually goes wrong with this one"* and a `Source:`
footer. A thin entry must not wear the authority of a thick one.

`findSpeciesCare` ranks curated above bundled unconditionally. It is not a
tie-break on match length: a curated entry matching loosely still beats a
bundled entry matching exactly, because only the curated tier knows failure
modes.

## 2. Sources

**biologiste95/plant-dataset** — Unlicense, i.e. public domain. Vendored at
`tools/species-sources/`. 250 named indoor plants.

What it actually contains, as opposed to what its README advertises:

| Column | Filled | Usable |
|---|---|---|
| botanical name | 250/250 | yes |
| common names | 233/250 | yes |
| light / temperature / humidity / watering / soil | ~195/250 | yes, as **undocumented ordinal codes** |
| toxicity | **10/250** | no |
| general care prose | **3/250** | no |
| problems | **2/250** | no |
| description | **6/250** | no |
| foliage / flower | **1/250** | no |
| commercial light levels | **0/250** | no |

The prose columns the README names are essentially empty. What the dataset
really provides is a botanical name, common names, and four ordinal codes. That
is still worth having — the watering code maps onto the one number a user
cannot guess — but it is not a care encyclopaedia and the generated entries are
written to sound like what they are.

**ASPCA toxic and non-toxic plant list** — free, authoritative, and the only
toxicity source, since the dataset's column is 96% empty. Scraped by
`tools/aspca.py` into `tools/species-sources/aspca.json`: 573 species with
cat/dog flags. Only the four listing views are fetched, never the ~1000 detail
pages.

**GBIF backbone taxonomy** — free, keyless. Used at *build time* to resolve each
dataset name to its accepted name. See §4.

All three are vendored, so regenerating the catalogue needs no network and
nothing breaks if a service disappears.

## 3. The code legend, and how it was calibrated

The dataset ships no legend. The mapping in `tools/gen-catalogue.py` was
derived from plants whose care is not in dispute, then checked against the
hand-written tier, which was written earlier and independently.

**Watering** — the important one, because it becomes `depletionTrigger`.

| Code | Calibrated on | Meaning | Trigger |
|---|---|---|---|
| 1 | Adiantum, Calathea, Fittonia, Spathiphyllum | keep lightly moist | 0.35 |
| 2 | Ficus, Monstera, Peperomia, Dracaena, Hedera | let the top third dry | 0.50 |
| 3 | Aloe, Crassula | dry out almost completely | 0.75 |

**Light** — 1 is brightest, not dimmest. Calibrated on Aloe (1), Monstera
obliqua (3) and Epipremnum (2_4, i.e. bright down to low, which is exactly
pothos). **Humidity** — 1 needs the most: Adiantum and Fittonia are 1, Aloe and
Sansevieria are 3. **Temperature** — 2 is an ordinary heated room; only 1 (cool)
and 3 (warm) are worth saying. **Soil** — only 5 (every succulent in the file)
and 6 (the ferns) are confidently identifiable; 1/2/3/4/7 are left unsaid rather
than guessed.

Cells are inconsistent — `2`, `1_2`, `2,3`, `2 3` — and some were mangled by
Excel before we ever saw them: `1-3` was autocorrected into the date serial
`44621`. Anything above 9 is discarded rather than interpreted.

**Rows with no watering code are dropped entirely.** An entry that cannot
suggest a trigger has nothing this app needs.

## 4. GBIF, at build time

The dataset's names are decades old in places. Resolving all 237 through the
GBIF backbone rewrote **62** of them:

    Scindapsus aureus       -> Epipremnum aureum      (pothos)
    Aloe barbadensis        -> Aloe vera
    Calathea makoyana       -> Goeppertia makoyana
    Brassaia actinophylla   -> Heptapleurum actinophyllum
    Beloperone guttata      -> Justicia brandegeeana

Without this, `Scindapsus aureus` shipped as a **second pothos entry** with a
different depletion trigger from the curated one, under a name no user would
recognise as the same plant.

Guarded, because GBIF answers a junk query with a genus: the row
`Begonia President` resolves to `Bigonia`. Only a confident (≥90), two-word,
genuinely different answer is taken.

## 5. Two rules that keep the tiers from contradicting each other

**Shadowing.** A generated entry the curated tier already answers is dropped —
checked against *both* the typed name and the GBIF-accepted name, because
`Calathea makoyana` becomes `Goeppertia makoyana` and would otherwise stop
matching the curated Calathea entry. 102 of 217 were dropped this way.

This removes every possible contradiction by construction. The generator prints
what it dropped; at the last run the dataset would otherwise have disagreed
outright on ten plants, always in the same direction — wetter than the curated
tier for succulents, epiphytes and cacti. The dataset's watering code appears to
describe nursery frequency rather than how dry a pot should get in a flat.

**Alias donation.** Dropping an entry must not make its names unsearchable —
`Scindapsus aureus` contains no word the pothos entry knows. So a shadowed
entry hands its names to the curated entry that shadowed it: 186 aliases,
emitted as `bundledAliasesForCurated` and merged in `speciesCatalogue`. An old
plant label still finds the right plant.

### The cross-check found a bug in the hand-written tier

The generated tier put Christmas cactus at 0.5 against the curated `Cactus`
entry's 0.85, and the generated tier was right: *Schlumbergera* is a Brazilian
forest epiphyte, not a desert plant. The curated entry even said so in its own
light field — *"Christmas cactus is the exception"* — and then applied the
desert trigger to it anyway.

Christmas cactus is now its own curated entry at 0.5. Treating it as a desert
cactus is the usual way they get killed.

## 6. The online fallback: names, never care

Off by default. Settings → Network → *Look up unknown plant names online*.

When the catalogue has nothing and the toggle is on, the care screen offers one
button. It resolves the typed name through GBIF and links the Wikipedia
article. It **cannot return care advice** — `SpeciesLookupService` has no field
for it — so the worst it can do is link the wrong article, never suggest the
wrong watering schedule. Nothing feeds `SpeciesCare`, `depletionTrigger` or any
prediction.

The failure states are distinct on purpose: *not enabled*, *offline*, *no match*
and *service error* say different things, because "we didn't ask" and "it doesn't
exist" are different facts.

What leaves the phone is the species name and nothing else — no plant id, no
photo, no log, no device identifier. `GbifSpeciesLookupService` checks consent
itself rather than trusting call sites, and uses `HttpURLConnection`: two calls
to two keyless APIs do not justify pulling OkHttp into an app whose argument is
that it does not need a network.

Neither service dying breaks anything. The catalogue, the predictions, the
reminders and the whole diary work with no network at all.

## 7. Regenerating

    cd tools
    python3 aspca.py          # optional, refreshes toxicity; ~2 min
    python3 gbif-resolve.py   # optional, refreshes accepted names; cached
    python3 gen-catalogue.py  # writes SpeciesCatalogueBundled.kt

`gen-catalogue.py` needs no network — it reads the vendored sources. It reparses
`SpeciesCatalogue.kt` to learn the curated aliases and triggers, so **editing a
curated entry changes what gets generated**. Regenerate after touching one.

The output is checked in. `SpeciesCareTest` asserts the invariants that matter:
no two entries claim the same alias, curated always wins, the two tiers never
both answer the same query, every generated entry names its source, and the
catalogue still refuses to answer for "flax seeds".
