#!/usr/bin/env python3
"""Film grain, calibrated against the plates rather than chosen by eye.

The renders measure about half the within-region variation the plates carry and
have 12 to 28 points more completely featureless area. Some of that is missing
surface texture, which is a modelling job, and some of it is simply that the
plates were generated with grain over everything and a Cycles render is clean.

The amount here is derived: grain is added until the median local standard
deviation of 8x8 blocks matches the plate's. Guessing a percentage and looking
at it is how you end up with grain you can see, and the rule this project
already wrote down is that if a user notices it there is too much of it.
"""
import sys
import numpy as np
from PIL import Image


def local_sd(a):
    """Median standard deviation within 8x8 blocks: variation INSIDE a region."""
    g = a.mean(axis=2) if a.ndim == 3 else a
    h, w = g.shape
    H, W = h // 8 * 8, w // 8 * 8
    return float(np.median(g[:H, :W].reshape(H // 8, 8, W // 8, 8).std(axis=(1, 3))))


def add_grain(path, target_sd, max_sigma=6.0, seed=7):
    im = Image.open(path).convert('RGB')
    a = np.asarray(im, dtype=np.float32)
    have = local_sd(a)
    if have >= target_sd:
        return have, have, 0.0
    # Grain and signal variance add, so the sigma needed is the shortfall in
    # quadrature. Clamped, because past a point this stops being grain.
    sigma = min(max_sigma, float(np.sqrt(max(target_sd ** 2 - have ** 2, 0.0))))
    rng = np.random.default_rng(seed)
    # One monochrome field, not three: coloured grain reads as sensor noise,
    # and what the plates have is film.
    noise = rng.normal(0.0, sigma, a.shape[:2]).astype(np.float32)[:, :, None]
    out = np.clip(a + noise, 0, 255).astype(np.uint8)
    Image.fromarray(out).save(path)
    return have, local_sd(out.astype(np.float32)), sigma


if __name__ == '__main__':
    target = float(sys.argv[1])
    for p in sys.argv[2:]:
        before, after, sigma = add_grain(p, target)
        print(f'  {p.split("/")[-1]:24} local sd {before:.2f} -> {after:.2f}  (sigma {sigma:.2f})')
