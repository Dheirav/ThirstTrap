"""Scrape the ASPCA toxic/non-toxic plant list into a JSON map.

Only the four listings we need (toxic/non-toxic x dogs/cats). The listing rows
carry both the common name and the scientific name, so the ~1000 detail pages
are never touched.
"""
import json, re, time, urllib.request, os, sys

BASE = "https://www.aspca.org/pet-care/aspca-poison-control/toxic-and-non-toxic-plants"
UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
SETS = [
    ("toxic_dog",     "field_toxicity_value%5B%5D=01",     27),
    ("toxic_cat",     "field_toxicity_value%5B%5D=02",     28),
    ("nontoxic_dog",  "field_non_toxicity_value%5B%5D=01", 37),
    ("nontoxic_cat",  "field_non_toxicity_value%5B%5D=02", 37),
]
TOTAL = sum(n + 1 for _, _, n in SETS)
ROW = re.compile(
    r'views-field-title">\s*<span class="field-content">\s*<a[^>]*>\s*'
    r'<div class="plant-title-name">(.*?)</div>.*?'
    r'views-field-title-scientific-name">\s*<span class="field-content">\s*<a[^>]*>\s*'
    r'<div class="plant-title-name">(.*?)</div>', re.S)

def clean(s):
    s = re.sub(r'<[^>]+>', '', s)
    return re.sub(r'\s+', ' ', s).replace('&amp;', '&').replace('&#039;', "'").strip()

def fetch(url):
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            return urllib.request.urlopen(req, timeout=40).read().decode("utf-8", "replace")
        except Exception as e:
            if attempt == 3:
                raise
            time.sleep(2 * (attempt + 1))

done = 0
out = {}
for key, q, last in SETS:
    for page in range(last + 1):
        url = f"{BASE}?{q}&page={page}"
        html = fetch(url)
        for m in ROW.finditer(html):
            common, sci = clean(m.group(1)), clean(m.group(2))
            if not sci:
                continue
            rec = out.setdefault(sci, {"scientific": sci, "common": set(), "flags": set()})
            if common:
                rec["common"].add(common)
            rec["flags"].add(key)
        done += 1
        with open("aspca_progress.txt", "w") as f:
            f.write(f"{done}/{TOTAL} pages  set={key} page={page}  species={len(out)}\n")
        time.sleep(0.6)

ser = {k: {"scientific": v["scientific"],
           "common": sorted(v["common"]),
           "flags": sorted(v["flags"])} for k, v in out.items()}
json.dump(ser, open("aspca.json", "w"), indent=1, ensure_ascii=False)
with open("aspca_progress.txt", "w") as f:
    f.write(f"DONE {done}/{TOTAL} pages, {len(ser)} species -> aspca.json\n")
