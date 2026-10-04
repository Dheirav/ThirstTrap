#!/usr/bin/env python3
"""Grade the plates to each other, the way a film matches shots.

Fixing the lamp and the props was not enough, and measuring honestly said so:
after the regeneration, the mean distance between any two plates' colour and
tone distributions was 0.214, slightly WORSE than the 0.202 it started at. The
continuity that was fixed sits in the background of the frame, behind a 1.12x
push-in and a caption. What a viewer actually reads is the overall colour and
tone of each shot, and that was never touched.

Measured in Lab over the seven landscape plates:

    lightness   20.0 to 26.2, a spread of 6.2, already tight
    contrast    12.3 to 21.2, so shelf-evening is flat where ledger is punchy
    warmth (b)   6.0 to 16.5, so shelf-evening is cold and phone-closeup is hot

Lightness is fine and is left alone: these are different moments and some are
legitimately darker. Contrast and warmth are not, because one room at one hour
of one night does not change temperature between shots.

So each plate is pulled part of the way toward the set's median, in Lab, on
chroma and contrast only. Part of the way rather than all of it: forcing every
plate onto one number would flatten the differences that are the point, the
close dark soil against the wide moonlit sill. STRENGTH is how far.

  python3 tools/hero-gen/grade.py            report
  python3 tools/hero-gen/grade.py --write    write out/graded/
"""
import sys, os, glob
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
STRENGTH = 0.65
WHITE = np.array([0.95047, 1.0, 1.08883], np.float32)
M = np.array([[0.4124, 0.3576, 0.1805],
              [0.2126, 0.7152, 0.0722],
              [0.0193, 0.1192, 0.9505]], np.float32)


def to_lab(a):
    lin = np.where(a > 0.04045, ((a + 0.055) / 1.055) ** 2.4, a / 12.92)
    xyz = (lin @ M.T) / WHITE
    f = np.where(xyz > 0.008856, np.cbrt(np.maximum(xyz, 0)), 7.787 * xyz + 16 / 116)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]),
                     200 * (f[..., 1] - f[..., 2])], -1)


def to_rgb(lab):
    fy = (lab[..., 0] + 16) / 116
    fx, fz = fy + lab[..., 1] / 500, fy - lab[..., 2] / 200
    f = np.stack([fx, fy, fz], -1)
    xyz = np.where(f > 0.206893, f ** 3, (f - 16 / 116) / 7.787) * WHITE
    lin = xyz @ np.linalg.inv(M).T.astype(np.float32)
    lin = np.clip(lin, 0, 1)
    return np.where(lin > 0.0031308, 1.055 * lin ** (1 / 2.4) - 0.055, 12.92 * lin)


def load(p):
    return np.asarray(Image.open(p).convert('RGB'), np.float32) / 255


def plate_stats(lab):
    f = lab.reshape(-1, 3)
    return f.mean(0), f.std(0)


def main():
    write = '--write' in sys.argv
    files = {os.path.splitext(os.path.basename(f))[0]: f
             for f in sorted(glob.glob(os.path.join(HERE, 'out', '*.png')))}
    if not files:
        sys.exit('no plates in tools/hero-gen/out')

    labs = {n: to_lab(load(p)) for n, p in files.items()}
    st = {n: plate_stats(l) for n, l in labs.items()}
    mus = np.array([st[n][0] for n in files])
    sds = np.array([st[n][1] for n in files])
    tmu, tsd = np.median(mus, 0), np.median(sds, 0)

    print(f'set median: L {tmu[0]:.1f}  contrast {tsd[0]:.1f}  a {tmu[1]:.1f}  b {tmu[2]:.1f}')
    print(f'pulling {STRENGTH:.0%} of the way, on contrast and chroma only\n')
    print(f'{"plate":18s} {"contrast":>16s} {"warmth b":>16s}')

    dst = os.path.join(HERE, 'out', 'graded')
    if write:
        os.makedirs(dst, exist_ok=True)

    for n, p in files.items():
        lab = labs[n].copy()
        mu, sd = st[n]
        # Lightness is left alone on purpose: these are different moments and
        # some of them are meant to be darker than others.
        for c in (1, 2):                       # a and b: the colour of the light
            s = 1 + STRENGTH * (tsd[c] / max(sd[c], 1e-3) - 1)
            m = mu[c] + STRENGTH * (tmu[c] - mu[c])
            lab[..., c] = (lab[..., c] - mu[c]) * s + m
        s0 = 1 + STRENGTH * (tsd[0] / max(sd[0], 1e-3) - 1)
        lab[..., 0] = np.clip((lab[..., 0] - mu[0]) * s0 + mu[0], 0, 100)

        out = np.clip(to_rgb(lab), 0, 1)
        new = plate_stats(to_lab(out))
        print(f'{n:18s} {sd[0]:7.1f} -> {new[1][0]:5.1f} {mu[2]:7.1f} -> {new[0][2]:5.1f}')
        if write:
            Image.fromarray((out * 255).round().astype(np.uint8)).save(os.path.join(dst, n + '.png'))

    if write:
        print(f'\nwrote {dst}')
    else:
        print('\nreport only. pass --write to produce out/graded/')


if __name__ == '__main__':
    main()
