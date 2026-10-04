#!/usr/bin/env python3
"""Does each plate agree with the room, and with its own other orientation?

The plates were generated one scene per chat, so nothing made them agree. The
room in tools/blender does agree with itself by construction, so it is the
authority: tools/blender/continuity.py projects the lamp into every shot's
camera and writes continuity.json, and this scores the paintings against it.

Two things are measured.

  Light side. Not the whole-frame left/right brightness balance, which mostly
  reports where the dark subject happens to sit, but the horizontal centroid of
  the brightest tenth of the picture. Highlights are where the light lands, so
  that centroid IS the lamp's side, and it survives a composition that puts a
  black pot on one half.

  Palette. The style block names six colours. Measured in hue, because value
  drifts legitimately between a lit shot and a dark one while hue should not.

  python3 tools/hero-gen/check-plates.py [--src]     --src scores out/ instead
"""
import sys, os, json, colorsys, glob
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
CONT = os.path.join(ROOT, 'tools', 'blender', 'continuity.json')

SPEC = {           # hue, and how far it may drift before it reads as a different room
    'terracotta': (11.5, 10, (5, 45), 0.28, 0.12, 0.80),
    'foliage':    (134.0, 22, (60, 175), 0.18, 0.06, 1.00),
    'sky':        (207.0, 20, (180, 250), 0.10, 0.04, 0.60),
}

def hsv(c):
    h, s, v = colorsys.rgb_to_hsv(*[x / 255 for x in c])
    return h * 360, s, v

def band(im, lo, hi, smin, vlo, vhi):
    return [p for p in im.getdata()
            if lo <= hsv(p)[0] <= hi and hsv(p)[1] > smin and vlo < hsv(p)[2] < vhi]

def mean(px):
    return None if not px else tuple(sum(c[i] for c in px) // len(px) for i in range(3))

def highlight_x(im, top_frac=0.10):
    """Horizontal centroid of the brightest `top_frac` of pixels, 0 left .. 1 right."""
    g = im.convert('L').resize((240, int(240 * im.height / im.width)))
    w, h = g.size
    d = list(g.getdata())
    cut = sorted(d)[int(len(d) * (1 - top_frac))]
    num = den = 0
    for i, v in enumerate(d):
        if v >= cut:
            num += (i % w) * v
            den += v
    return (num / den) / (w - 1) if den else 0.5

def verdict(x, want):
    """want is the room's answer: 'right', 'left' or 'straight on'."""
    if want == 'straight on':
        return abs(x - 0.5) <= 0.10, f'{x:.2f} (want 0.40-0.60)'
    if want == 'right':
        return x >= 0.54, f'{x:.2f} (want >=0.54)'
    return x <= 0.46, f'{x:.2f} (want <=0.46)'

src = '--src' in sys.argv
files = sorted(glob.glob(os.path.join(HERE, 'out', '*.png'))) if src else \
        sorted(glob.glob(os.path.join(ROOT, 'site', 'hero', '*.avif')))
if not os.path.exists(CONT):
    sys.exit('no continuity.json — run:\n  blender -b --python tools/blender/continuity.py')
cont = json.load(open(CONT))

print(f'scoring {"tools/hero-gen/out" if src else "site/hero"} against the room\n')
print(f'{"plate":18s} {"room says":12s} {"highlights":>22s}  {"":4s} | '
      f'{"terracotta":>12s} {"foliage":>12s} {"sky":>12s}')
print('-' * 100)

fails, rows = [], {}
for f in files:
    name = os.path.splitext(os.path.basename(f))[0]
    shot, view = (name[:-2], 'portrait') if name.endswith('-p') else (name, 'landscape')
    if shot not in cont:
        continue
    im = Image.open(f).convert('RGB')
    small = im.resize((300, int(300 * im.height / im.width)))
    want = cont[shot]['_lamp_dir'][view]['lr']
    ok, detail = verdict(highlight_x(im), want)
    rows[name] = (highlight_x(im), want, ok)

    cells = []
    for key, (want_h, tol, (lo, hi), smin, vlo, vhi) in SPEC.items():
        px = band(small, lo, hi, smin, vlo, vhi)
        # A hue averaged over a handful of pixels is noise, not a colour. The
        # re-rolled ledger has six foliage pixels in the whole frame and was
        # reported at hue 170 and then 77, neither of which meant anything:
        # there is no plant in that plate to be the wrong green.
        if len(px) < 0.002 * small.width * small.height:
            cells.append(f'{"none":>12s}')
            continue
        m = mean(px)
        if m is None:
            cells.append(f'{"-":>12s}')
            continue
        h = hsv(m)[0]
        d = abs(h - want_h)
        cells.append(f'{h:7.0f}{"  ok" if d <= tol else f" +{d:.0f}":>5s}')
        if d > tol:
            fails.append((name, f'{key} hue {h:.0f}, spec {want_h:.0f} (+{d:.0f})'))
    if not ok:
        fails.append((name, f'lamp on the wrong side: highlights at {detail}, room says {want}'))
    print(f'{name:18s} {want:12s} {detail:>22s}  {"ok" if ok else "FAIL":4s} | ' + ' '.join(cells))

print('\npairs — the two orientations of one beat must light it from the same side:')
for shot in cont:
    a, b = rows.get(shot), rows.get(shot + '-p')
    if not a or not b:
        continue
    same = (a[0] > 0.5) == (b[0] > 0.5) or (abs(a[0] - .5) < .06 and abs(b[0] - .5) < .06)
    print(f'  {shot:15s} landscape {a[0]:.2f}  portrait {b[0]:.2f}   {"ok" if same else "*** FLIPS ***"}')
    if not same:
        fails.append((shot, f'the two orientations light it from opposite sides'))

# What the viewer actually experiences is not "does this plate match the room"
# but "does the light stop moving while I scroll". Count the side changes down
# the scroll, separately per orientation, because nobody sees both in one pass.
ORDER = ['ledger', 'shelf-evening', 'finger-test', 'depth', 'roots',
         'scale-table', 'phone-closeup']
print('\nside changes down the scroll (the lamp jumping is what reads as a different room):')
for suffix, label in (('', 'landscape'), ('-p', 'portrait')):
    seq = [(n, rows[n + suffix][0]) for n in ORDER if n + suffix in rows]
    sides = ['R' if x > 0.5 else 'L' for _, x in seq]
    jumps = sum(1 for a, b in zip(sides, sides[1:]) if a != b)
    print(f'  {label:10s} {" ".join(sides)}   {jumps} change(s)')

print(f'\n{len(fails)} problem(s)' + (':' if fails else ''))
for n, why in fails:
    print(f'  {n:18s} {why}')
sys.exit(1 if fails else 0)
