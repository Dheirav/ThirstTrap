"""Resolve every dataset name to its GBIF-accepted name, once, at build time.

The dataset uses names that are decades out of date - Scindapsus aureus for
Epipremnum aureum, Fittonia verschaffeltii for F. albivenis, Zygocactus for
Schlumbergera. Without this the generated tier ships a second entry for a plant
the curated tier already covers, under a name nobody would recognise as the
same plant.

Output is vendored so a regeneration needs no network.
"""
import json, os, re, sys, time, urllib.parse, urllib.request
from xlsx import load

OUT = 'species-sources/gbif.json'
cache = json.load(open(OUT)) if os.path.exists(OUT) else {}

rows = load('species-sources/Plants_indoor_dataset_iot_AI.xlsx')['xl/worksheets/sheet1.xml']

def species_of(botanical):
    b = re.sub(r'[‘’\'"“”].*', '', botanical).strip()
    b = re.sub(r'\s+(spp?\.|var\.|x|×|hybrids?|hybridum)\b.*', '', b, flags=re.I).strip()
    parts = b.split()
    return ' '.join(parts[:2]) if len(parts) >= 2 else b

names = sorted({species_of(r['A'].strip()) for r in rows[1:] if r.get('A', '').strip()})
names = [n for n in names if len(n) >= 4]
todo = [n for n in names if n not in cache]
print(f'{len(names)} names, {len(todo)} to fetch', flush=True)

for i, n in enumerate(todo, 1):
    url = 'https://api.gbif.org/v1/species/match?' + urllib.parse.urlencode(
        {'name': n, 'kingdom': 'Plantae', 'strict': 'false'})
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'ThirstTrap catalogue build'})
        d = json.load(urllib.request.urlopen(req, timeout=30))
    except Exception as e:
        d = {'matchType': 'ERROR', 'error': str(e)}
    cache[n] = {
        'matchType': d.get('matchType'),
        'status': d.get('status'),
        'confidence': d.get('confidence'),
        # `species` is the ACCEPTED species when the query was a synonym.
        'accepted': d.get('species') or d.get('canonicalName'),
        'canonical': d.get('canonicalName'),
        'family': d.get('family'),
        'genus': d.get('genus'),
        'usageKey': d.get('usageKey'),
    }
    with open('gbif_progress.txt', 'w') as f:
        f.write(f'{i}/{len(todo)} resolved  last={n}\n')
    time.sleep(0.25)

json.dump(cache, open(OUT, 'w'), indent=1, ensure_ascii=False, sort_keys=True)
syn = sum(1 for v in cache.values() if v.get('status') == 'SYNONYM')
none = sum(1 for v in cache.values() if v.get('matchType') in (None, 'NONE', 'ERROR'))
with open('gbif_progress.txt', 'w') as f:
    f.write(f'DONE {len(cache)} names, {syn} synonyms rewritten, {none} unmatched\n')
print(f'DONE {len(cache)} names, {syn} synonyms rewritten, {none} unmatched')
