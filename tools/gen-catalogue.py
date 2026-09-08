"""Generate bundledSpeciesCatalogue.kt from the public-domain sources.

Sources
  - biologiste95/plant-dataset (Unlicense). 250 named indoor plants, each with
    ordinal codes for light, temperature, humidity, watering and soil mix.
  - ASPCA toxic/non-toxic plant list, scraped by aspca.py, for pet toxicity.

The dataset ships no legend for its codes, so the mapping below was calibrated
against plants whose care is not in dispute, and then cross-checked against the
48 hand-written entries, which were written months earlier and independently.
Every disagreement is printed at the end - if that list is not empty, the
mapping is wrong somewhere and the output should not be trusted.
"""
import json, re, sys, unicodedata
from collections import defaultdict, Counter
from xlsx import load

# ---------------------------------------------------------------- the legend

# brightness: 1 is the brightest. Calibrated on Aloe (1) vs Monstera obliqua (3)
# vs Epipremnum (2_4, i.e. bright down to low - which is exactly pothos).
LIGHT = {1: "direct sun", 2: "bright indirect light", 3: "medium light", 4: "low light"}

# watering: 1 wettest. Calibrated on Adiantum/Calathea/Fittonia/Spathiphyllum (1)
# vs Ficus/Monstera/Peperomia/Dracaena (2) vs Aloe/Crassula (3).
WATER = {
    1: ("Keep the mix lightly moist. Let the surface dry, but do not let the pot dry through.", 0.35),
    2: ("Let the top third to half of the pot dry out before watering.", 0.50),
    3: ("Let the pot dry out almost completely between waterings.", 0.75),
}

# humidity: 1 needs the most. Adiantum/Fittonia/Nephrolepis are 1, Aloe and
# Sansevieria are 3.
HUMIDITY = {
    1: "Wants humidity above ordinary room air. Suffers in dry winter heating.",
    2: "Ordinary room air is fine.",
    3: "Happy in dry air.",
}

# temperature: 2 is an ordinary heated room, so only 1 and 3 are worth saying.
# Calibrated on Hedera/Fatsia/Fuchsia (1, cool-growing) vs Aloe (3).
TEMPERATURE = {
    1: "Prefers it on the cool side; dislikes a hot dry room.",
    3: "Wants steady warmth. Keep it away from cold draughts and winter windows.",
}

# soil mix: only two codes are confidently identifiable. 5 is every succulent in
# the file (Aloe, Crassula, Sansevieria); 6 is the ferns (Adiantum, Nephrolepis).
# The rest are left unsaid rather than guessed.
SOIL = {5: "Wants a free-draining, gritty mix.", 6: "Wants a moisture-retentive, peaty mix."}

# ------------------------------------------------------------------- parsing

def codes(raw):
    """The set of levels in a cell.

    Cells are inconsistent - "2", "1_2", "2,3", "2 3" - and some were mangled by
    Excel: "1-3" was autocorrected into a date serial. Anything above 10 is one
    of those and is discarded rather than guessed at.
    """
    if not raw:
        return []
    nums = [int(n) for n in re.findall(r'\d+', str(raw))]
    return sorted({n for n in nums if 1 <= n <= 9})

def norm(s):
    s = unicodedata.normalize('NFKD', s or '')
    s = ''.join(c for c in s if not unicodedata.combining(c))
    s = s.lower().replace("'", '').replace('’', '')
    s = re.sub(r'[^a-z0-9 ]', ' ', s)
    return re.sub(r'\s+', ' ', s).strip()

def species_of(botanical):
    """Genus + species, with cultivars and quoted names stripped."""
    b = re.sub(r'[‘’\'"“”].*', '', botanical).strip()
    b = re.sub(r'\s+(spp?\.|var\.|x|×|hybrids?|hybridum)\b.*', '', b, flags=re.I).strip()
    parts = b.split()
    return ' '.join(parts[:2]) if len(parts) >= 2 else b

def kstr(s):
    return '"' + s.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$') + '"'

# ------------------------------------------------------------------ the data

rows = load('species-sources/Plants_indoor_dataset_iot_AI.xlsx')['xl/worksheets/sheet1.xml']
data = [r for r in rows[1:] if r.get('A', '').strip()]

merged = {}
for r in data:
    bot = r['A'].strip()
    sp = species_of(bot)
    if not sp or len(sp) < 4:
        continue
    w = codes(r.get('N'))
    if not w or w[0] not in WATER:
        continue                      # no watering code, no usable entry
    e = merged.setdefault(sp, {'species': sp, 'common': [], 'w': [], 'b': [], 'h': [], 't': [], 's': []})
    e['w'].append(w[0])
    e['b'] += codes(r.get('K'))
    e['h'] += codes(r.get('M'))
    e['t'] += codes(r.get('L'))
    e['s'] += codes(r.get('O'))
    for c in re.split(r'[,;/]', r.get('C', '')):
        c = c.strip()
        if c and norm(c) and norm(c) not in [norm(x) for x in e['common']]:
            e['common'].append(c)

# ------------------------------------------------- accepted names, from GBIF

# The dataset's names are decades old in places. Resolving them matters twice:
# a modern name has to find the plant, and the shadow check below has to see
# through a rename - "Aloe barbadensis" is Aloe vera, which the curated tier
# already covers properly.
gbif = json.load(open('species-sources/gbif.json'))

def accepted_of(sp):
    """The accepted binomial, or None when GBIF offered nothing better.

    Guarded because GBIF happily answers a junk query with a genus: the row
    "Begonia President" resolves to "Bigonia", which is not a plant name at all.
    Only a confident, two-word, genuinely different answer is taken.
    """
    g = gbif.get(sp)
    if not g or g.get('status') != 'SYNONYM':
        return None
    a = (g.get('accepted') or '').strip()
    if len(a.split()) != 2 or (g.get('confidence') or 0) < 90:
        return None
    return a if a.lower() != sp.lower() else None

# ------------------------------------------------------------------- toxicity

try:
    aspca = json.load(open('species-sources/aspca.json'))
except FileNotFoundError:
    print('aspca.json missing - run aspca.py first', file=sys.stderr)
    sys.exit(1)

tox_by_species, tox_by_genus = {}, defaultdict(list)
for sci, rec in aspca.items():
    key = norm(species_of(sci))
    if key:
        tox_by_species.setdefault(key, rec)
        tox_by_genus[key.split()[0]].append(rec)

def toxicity_for(sp):
    n = norm(sp)
    rec = tox_by_species.get(n) or tox_by_species.get(norm(accepted_of(sp) or ''))
    scope = 'this species'
    if rec is None:
        g = tox_by_genus.get(n.split()[0])
        if not g:
            return None
        flags = set().union(*(set(x['flags']) for x in g))
        # A genus where some species are toxic and others are not cannot be
        # summarised safely, so it is not summarised.
        if {'toxic_cat', 'toxic_dog'} & flags and {'nontoxic_cat', 'nontoxic_dog'} & flags:
            return "Toxicity varies within this genus - check the exact species with the ASPCA before trusting it around a pet."
        rec, scope = {'flags': sorted(flags)}, 'this genus'
    f = set(rec['flags'])
    toxic = [a for a, k in (('cats', 'toxic_cat'), ('dogs', 'toxic_dog')) if k in f]
    safe = [a for a, k in (('cats', 'nontoxic_cat'), ('dogs', 'nontoxic_dog')) if k in f]
    if toxic and safe:
        return f"Toxic to {' and '.join(toxic)}; not toxic to {' and '.join(safe)}. (ASPCA, {scope}.)"
    if toxic:
        return f"Toxic to {' and '.join(toxic)}. (ASPCA, {scope}.)"
    if safe:
        return f"Not toxic to {' and '.join(safe)}. (ASPCA, {scope}.)"
    return None

# --------------------------------------------------------------- build entries

def mode(xs, default=None):
    return Counter(xs).most_common(1)[0][0] if xs else default

entries = []
for sp, e in sorted(merged.items()):
    w = mode(e['w'])
    water, trigger = WATER[w]
    b = sorted(set(e['b'])) or None
    if b:
        lo, hi = b[0], b[-1]
        lo, hi = max(1, min(4, lo)), max(1, min(4, hi))
        light = (f"Prefers {LIGHT[lo]}." if lo == hi
                 else f"Prefers {LIGHT[lo]}, and tolerates down to {LIGHT[hi]}.")
        light = light[0].upper() + light[1:]
    else:
        light = "No light level recorded for this one - treat bright indirect as the safe default."
    hcodes = sorted(set(e['h']))
    humidity = HUMIDITY.get(hcodes[0]) if hcodes and hcodes[0] in HUMIDITY else None
    notes = [n for n in (TEMPERATURE.get(mode(e['t'])), SOIL.get(mode(e['s']))) if n]
    entries.append({
        'species': sp, 'accepted': accepted_of(sp),
        'common': e['common'], 'light': light, 'water': water,
        'trigger': trigger, 'humidity': humidity, 'toxicity': toxicity_for(sp),
        'note': ' '.join(notes) or None,
    })

# ---------------------------- read the hand-written tier back out of Kotlin

curated_aliases = set()
curated = []          # (name, [aliases], trigger)
kt = open('../core/domain/src/main/kotlin/dev/dheirav/thirsttrap/domain/SpeciesCatalogue.kt').read()
for block in re.finditer(r'SpeciesCare\((.*?)\n    \),', kt, re.S):
    body = block.group(1)
    nm = re.search(r'name = "([^"]*)"', body)
    al = re.search(r'aliases = listOf\(([^)]*)\)', body, re.S)
    tg = re.search(r'depletionTrigger = ([\d.]+)', body)
    if not (nm and al and tg):
        continue
    names = [norm(a) for a in re.findall(r'"([^"]*)"', al.group(1))]
    curated.append((nm.group(1), names, float(tg.group(1))))
    curated_aliases.update(names)


# ------------------------------- drop what the curated tier already answers

def curated_answer(text):
    """What findSpeciesCare would return from the curated tier alone."""
    q = norm(text)
    best, best_len = None, -1
    for name, aliases, trig in curated:
        for a in aliases:
            if a == q or f' {q} '.find(f' {a} ') >= 0 or f' {a} '.find(f' {q} ') >= 0:
                if len(a) > best_len:
                    best, best_len = (name, trig), len(a)
    return best

# A generated entry the curated tier already covers is not just redundant, it is
# a liability: two tiers answering the same query with different numbers. The
# curated tier wins in findSpeciesCare anyway, so dropping these loses nothing
# and removes every possible contradiction by construction.
shadowed, contradictions = [], []
shadowed_aliases = defaultdict(list)
kept = []
for e in entries:
    hit = None
    for a in [e['species'], e['accepted']] + e['common']:
        if not a:
            continue
        hit = curated_answer(a)
        if hit:
            break
    if hit:
        shadowed.append((e['species'], hit[0]))
        # The curated entry is the right answer for these names, but it has
        # never heard of them - "Scindapsus aureus" is pothos and contains no
        # word the pothos entry knows. Dropping the generated entry without
        # handing over its names would make an old label unsearchable.
        for a in [e['species'], e['accepted']] + e['common']:
            if a and len(norm(a)) >= 3:
                shadowed_aliases[hit[0]].append(norm(a))
        if abs(hit[1] - e['trigger']) > 0.2:
            contradictions.append(f"{e['species']}: dataset {e['trigger']} vs {hit[0]} {hit[1]}")
    else:
        kept.append(e)
donated = shadowed_aliases
entries = kept

# ------------------------------------------- alias assignment and collision check

genus_species = defaultdict(set)
for e in entries:
    genus_species[norm(e['species']).split()[0]].add(norm(e['species']))

claimed = dict()
# Donated names are claimed first: they belong to a curated entry, and a
# generated entry must not take a name off one.
for curated_name, names in donated.items():
    for a in names:
        if a and len(a) >= 3 and a not in curated_aliases and a not in claimed:
            claimed[a] = curated_name
        elif a in curated_aliases or claimed.get(a) != curated_name:
            names[:] = [n for n in names if n != a] if a in curated_aliases else names
donated = {k: [a for a in v if claimed.get(a) == k] for k, v in donated.items()}
donated = {k: v for k, v in donated.items() if v}

for e in entries:
    cands = [norm(e['species'])]
    if e['accepted']:
        cands.append(norm(e['accepted']))
    cands += [norm(c) for c in e['common']]
    g = norm(e['accepted'] or e['species']).split()[0]
    # A genus alias is only safe when this file holds exactly one species of it:
    # otherwise "crassula" would answer for six plants with different needs.
    if len(genus_species[g]) == 1:
        cands.append(g)
    keep = []
    for a in cands:
        if not a or len(a) < 3 or a in curated_aliases or a in claimed:
            continue
        claimed[a] = e['species']
        keep.append(a)
    e['aliases'] = keep

entries = [e for e in entries if e['aliases']]

# ---------------------------------------------------------------- emit Kotlin

out = ['''package dev.dheirav.thirsttrap.domain

// GENERATED FILE - do not edit by hand. Regenerate with tools/gen-catalogue.py.
//
// Sources, both public domain, both vendored under tools/ so a regeneration
// does not depend on either still being online:
//
//   biologiste95/plant-dataset (Unlicense) - 250 indoor plants with ordinal
//   codes for light, temperature, humidity, watering and soil mix.
//
//   ASPCA toxic and non-toxic plant list - pet toxicity.
//
// The dataset publishes no legend for its codes. The mapping was calibrated
// against plants whose care is not in dispute (Aloe and Crassula at the dry
// end, Adiantum and Fittonia at the wet end) and then cross-checked against the
// hand-written tier, which was written earlier and independently. See
// docs/SPECIES-CATALOGUE.md.
//
// These entries are deliberately thinner than the curated ones. They can say
// how bright, how wet and how humid, and nothing about what actually kills the
// plant - so they are marked CareDetail.BUNDLED and the UI says so.

/** The generated long tail. See [curatedSpeciesCatalogue] for the good stuff. */
val bundledSpeciesCatalogue: List<SpeciesCare> = listOf(''']

def display_name(raw):
    """Tidy a common name from the dataset for use as a heading.

    The cells are hand-typed and inconsistent: curly apostrophes, trailing
    botanical names glued on with "or", stray capitals.
    """
    n = re.sub(r'\s+or\s+.*$', '', raw.strip(), flags=re.I)
    n = n.replace('\u2019', "'").replace('\u2018', "'")
    n = re.sub(r'\s+', ' ', n).strip(' .,;')
    return (n[0].upper() + n[1:]) if n else n

for e in entries:
    name = display_name(e['common'][0]) if e['common'] else ''
    if not name:
        name = e['accepted'] or e['species']
    lines = ['    SpeciesCare(']
    lines.append(f'        name = {kstr(name)},')
    lines.append(f'        botanical = {kstr(e["accepted"] or e["species"])},')
    lines.append('        aliases = listOf(' + ', '.join(kstr(a) for a in e['aliases']) + '),')
    lines.append(f'        light = {kstr(e["light"])},')
    lines.append(f'        water = {kstr(e["water"])},')
    lines.append('        medium = Medium.SOIL,')
    lines.append(f'        depletionTrigger = {e["trigger"]},')
    if e['humidity']:
        lines.append(f'        humidity = {kstr(e["humidity"])},')
    if e['toxicity']:
        lines.append(f'        toxicity = {kstr(e["toxicity"])},')
    note = e['note']
    if e['accepted']:
        # Worth saying: it is usually why a search for the modern name found
        # nothing on the label, or the old name found nothing in a modern book.
        syn = f'Older name: {e["species"]}. Still what most labels say.'
        note = f'{note} {syn}' if note else syn
    if note:
        lines.append(f'        note = {kstr(note)},')
    lines.append('        detail = CareDetail.BUNDLED,')
    lines.append('        source = "plant-dataset (Unlicense); ASPCA",')
    lines.append('    ),')
    out.append('\n'.join(lines))

out.append(')\n')

out.append("""/**
 * Names the generated tier resolved that the hand-written entries had never
 * heard of.
 *
 * Every one of these is a plant a curated entry already covers properly, under
 * a name that entry does not recognise: an old botanical synonym, or a common
 * name from somewhere else. Without them, dropping the duplicate generated
 * entry would have made the old name unsearchable - which is exactly what a
 * plant label bought five years ago still says.
 *
 * Keyed by [SpeciesCare.name] in [curatedSpeciesCatalogue].
 */
val bundledAliasesForCurated: Map<String, List<String>> = mapOf(
""")
for name in sorted(donated):
    names = sorted(set(donated[name]))
    out.append('    ' + kstr(name) + ' to listOf(\n' +
               ''.join(f'        {kstr(a)},\n' for a in names) + '    ),')
out.append(')\n')
open('../core/domain/src/main/kotlin/dev/dheirav/thirsttrap/domain/SpeciesCatalogueBundled.kt', 'w').write('\n\n'.join(out))

print(f'rows in dataset          : {len(data)}')
print(f'merged to species        : {len(merged)}')
print(f'emitted (aliases survived): {len(entries)}')
print(f'with toxicity            : {sum(1 for e in entries if e["toxicity"])}')
print(f'with humidity            : {sum(1 for e in entries if e["humidity"])}')
print(f'aliases claimed          : {len(claimed)}')
print(f'shadowed by curated      : {len(shadowed)}')
print(f'aliases donated to curated: {sum(len(v) for v in donated.values())}')
print()
print('Of those, entries the dataset would have contradicted outright:')
print('\n'.join('  ' + c for c in contradictions) if contradictions else '  none')
