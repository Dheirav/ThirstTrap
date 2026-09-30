"""Run AIY plants_V1 over the diary's own photos.

The test set is small and unbalanced: four species, all of them well-lit
close-ups on a desk. That can disqualify a classifier and cannot validate one.
The question being asked here is narrower and more useful than accuracy: when
the model meets a plant it has no class for, does it decline or does it answer
confidently and wrongly? A model that knows it does not know could still be
honest about the third of houseplant genera it covers.
"""
import csv, json, sys, zipfile, io, collections
import numpy as np
from PIL import Image
from ai_edge_litert.interpreter import Interpreter

BACKUP = sys.argv[1]

labels = {int(r['id']): r['name'] for r in csv.DictReader(open('model/labelmap.csv'))}

z = zipfile.ZipFile(BACKUP)
data = json.loads(z.read('thirsttrap.json'))
plants = {p['id']: p for p in data['plants']}

# Ground truth at genus level, which is the most the app would ever trust.
TRUTH = {
    'Peperomia': 'Peperomia',
    'Creeping fig': 'Ficus',
    'Fittonia': 'Fittonia',
    'Flax seeds': None,        # sprouting seed, not an identifiable plant
}

it = Interpreter(model_path='model/plants.tflite')
it.allocate_tensors()
inp, out = it.get_input_details()[0], it.get_output_details()[0]
scale, zero = out['quantization']

def classify(img_bytes):
    im = Image.open(io.BytesIO(img_bytes)).convert('RGB').resize((224, 224))
    x = np.expand_dims(np.asarray(im, dtype=np.uint8), 0)
    it.set_tensor(inp['index'], x)
    it.invoke()
    probs = (it.get_tensor(out['index'])[0].astype(np.float32) - zero) * scale
    order = probs.argsort()[::-1]
    return [(labels.get(int(i), '?'), float(probs[i])) for i in order[:5]]

rows, by_plant = [], collections.defaultdict(list)
for ph in data['photos']:
    path = ph['relativePath']
    if path not in z.namelist():
        continue
    name = plants[ph['plantId']]['name']
    top5 = classify(z.read(path))
    rows.append((name, top5))
    by_plant[name].append(top5)

print(f"{len(rows)} photos over {len(by_plant)} plants\n")
print(f"{'plant':<14} {'n':>3}  {'in label set':<12} {'top-1 correct':<14} {'median top-1 conf':>18}")
print("-" * 68)
for name, results in sorted(by_plant.items()):
    truth = TRUTH[name]
    known = 'no' if truth is None else ('yes' if any(
        v.split()[0] == truth for v in labels.values()) else 'ABSENT')
    correct = sum(1 for r in results if truth and r[0][0].split()[0] == truth)
    confs = sorted(r[0][1] for r in results)
    med = confs[len(confs) // 2]
    shown = f"{correct}/{len(results)}" if truth else "n/a"
    print(f"{name:<14} {len(results):>3}  {known:<12} {shown:<14} {med:>17.2f}")

print("\nWhat it says instead, for the plants it has no class for:")
for name in sorted(by_plant):
    if TRUTH[name] is not None and any(
            v.split()[0] == TRUTH[name] for v in labels.values()):
        continue
    guesses = collections.Counter(r[0][0] for r in by_plant[name])
    top = ", ".join(f"{g} ({c})" for g, c in guesses.most_common(3))
    conf = max(r[0][1] for r in by_plant[name])
    print(f"  {name:<14} -> {top}   highest confidence seen: {conf:.2f}")

print("\nCreeping fig is a Ficus and Ficus IS a class. What it says instead:")
g = collections.Counter(r[0][0] for r in by_plant['Creeping fig'])
for name, c in g.most_common(5):
    print(f"  {c:>2}x  {name}")
ficus_rank = []
for r in by_plant['Creeping fig']:
    ranks = [i for i, (n, _) in enumerate(r) if n.split()[0] == 'Ficus']
    ficus_rank.append(ranks[0] if ranks else None)
inside5 = sum(1 for x in ficus_rank if x is not None)
print(f"\n  Ficus anywhere in the top 5: {inside5}/{len(ficus_rank)}")

abstain = sum(1 for _, t in rows if t[0][0] == 'background')
confident_wrong = sum(1 for _, t in rows if t[0][0] != 'background' and t[0][1] >= 0.7)
print(f"\nOver all {len(rows)} photos:")
print(f"  said 'background' (abstained): {abstain}")
print(f"  named something at >=0.70 confidence and was wrong: {confident_wrong}")
