#!/usr/bin/env python3
"""Pull the foliage in every plate onto the same green.

The plates were generated one at a time and the palette drifted. Measured over
the fourteen of them, the leaves came back anywhere from hue 60 to hue 102
against a specified 134: olive in some plates and almost brown in `roots`, with
a 42 degree spread between plates. Two plants in one room do not do that, and
it is a thing the eye reads as "different picture" without being able to name.

So each plate's leaves are rotated onto the target by its own measured error.
That fixes two things at once, the drift from spec and, more importantly, the
spread between plates, because afterwards they are all on the same number.

The pots are deliberately left alone, although they are off-spec too, at hue 23
to 31 against a specified 12. Their band overlaps the warm accent at hue 35, the
lamp glow and the wood, so rotating it would turn the lamp and the table red.
And the pots are consistently wrong, all fourteen in the same direction with an
8 degree spread, so they still read as the same clay in the same room. Being
off-spec and being inconsistent are different problems, and only the second one
is what this is for.

  python3 tools/hero-gen/harmonise.py            report what it would do
  python3 tools/hero-gen/harmonise.py --write    write out/harmonised/
"""
import sys, os, glob
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TARGET_HUE = 134.0          # #2F5D3A / #43794F
BAND = (55.0, 175.0)        # the only greens in this palette are leaves
FEATHER = 18.0              # degrees of soft edge, so no pixel steps at the band wall
S_MIN, S_SOFT = 0.12, 0.10
V_MIN, V_SOFT = 0.06, 0.14
MAX_SHIFT = 60.0


def rgb_to_hsv(a):
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    mx, mn = a.max(-1), a.min(-1)
    d = mx - mn
    h = np.zeros_like(mx)
    m = d > 1e-6
    rm, gm, bm = (mx == r) & m, (mx == g) & m, (mx == b) & m
    h[rm] = ((g - b)[rm] / d[rm]) % 6
    h[gm] = ((b - r)[gm] / d[gm]) + 2
    h[bm] = ((r - g)[bm] / d[bm]) + 4
    return h * 60.0, np.where(mx > 1e-6, d / np.maximum(mx, 1e-6), 0.0), mx


def hsv_to_rgb(h, s, v):
    h = np.mod(h, 360.0) / 60.0
    i = np.floor(h).astype(int)
    f = h - i
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    i = i % 6
    out = np.zeros(h.shape + (3,), dtype=np.float32)
    for k, (R, G, B) in enumerate([(v, t, p), (q, v, p), (p, v, t),
                                   (p, q, v), (t, p, v), (v, p, q)]):
        m = i == k
        out[m] = np.stack([R, G, B], -1)[m]
    return out


def smooth(x):
    x = np.clip(x, 0, 1)
    return x * x * (3 - 2 * x)


def weights(h, s, v):
    """1 well inside the leaf band, ramping to 0 at its edges and in low saturation."""
    lo, hi = BAND
    w = smooth((h - lo) / FEATHER) * smooth((hi - h) / FEATHER)
    w *= smooth((s - S_MIN) / S_SOFT)
    # And by value. Hue is barely encoded in 8-bit RGB once a pixel is nearly
    # black: asking a v=0.08 pixel to move 50 degrees moves it two levels, so
    # two plates whose leaves sit mostly in shadow refused to reach the target
    # and dragged the spread back to 18 degrees. Those pixels are not visible as
    # a colour anyway, so weight them out of both the measurement and the shift.
    w *= smooth((v - V_MIN) / V_SOFT)
    return w


def harmonise(im):
    a = np.asarray(im.convert('RGB'), dtype=np.float32) / 255.0
    h, s, v = rgb_to_hsv(a)
    w = weights(h, s, v)
    if w.sum() < 50:
        return im, None, None, 0.0
    before = float((h * w).sum() / w.sum())          # weighted, so edge pixels count less
    # Applying the plain error undershoots, because a feathered pixel moves by
    # w*shift rather than shift, so the mean lands short of the target: the first
    # pass left a 17 degree spread between plates instead of closing it. Solve
    # for the shift that actually lands the measured mean on the target instead.
    shift = TARGET_HUE - before
    for _ in range(6):
        landed = float(((h + w * shift) * w).sum() / w.sum())
        err = TARGET_HUE - landed
        if abs(err) < 0.15:
            break
        shift += err
    shift = float(np.clip(shift, -MAX_SHIFT, MAX_SHIFT))
    h2 = h + w * shift
    out = hsv_to_rgb(h2, s, v)
    res = Image.fromarray((np.clip(out, 0, 1) * 255).round().astype(np.uint8))
    h3, _, _ = rgb_to_hsv(np.asarray(res, dtype=np.float32) / 255.0)
    after = float((h3 * w).sum() / w.sum())
    return res, before, after, 100.0 * float((w > 0.5).sum()) / w.size


def main():
    write = '--write' in sys.argv
    dst = os.path.join(HERE, 'out', 'harmonised')
    if write:
        os.makedirs(dst, exist_ok=True)
    print(f'{"plate":18s} {"leaf hue":>9s} {"shift":>7s} {"after":>7s} {"leaf area":>10s}')
    befores, afters = [], []
    for f in sorted(glob.glob(os.path.join(HERE, 'out', '*.png'))):
        name = os.path.splitext(os.path.basename(f))[0]
        im = Image.open(f)
        res, before, after, area = harmonise(im)
        if before is None:
            print(f'{name:18s} {"no leaves":>9s}')
            if write:
                im.convert('RGB').save(os.path.join(dst, name + '.png'))
            continue
        befores.append(before); afters.append(after)
        print(f'{name:18s} {before:9.1f} {after - before:+7.1f} {after:7.1f} {area:9.1f}%')
        if write:
            res.save(os.path.join(dst, name + '.png'))
    if befores:
        print(f'\nspread between plates: {max(befores) - min(befores):.0f} degrees '
              f'-> {max(afters) - min(afters):.0f} degrees   (target {TARGET_HUE:.0f})')
    if write:
        print(f'wrote {dst}')
    else:
        print('\nreport only. pass --write to produce out/harmonised/')


if __name__ == '__main__':
    main()
