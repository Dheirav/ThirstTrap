#!/usr/bin/env python3
"""Encode the generated hero plates for the web.

The plates come out of the generator as ~1.7 MB PNGs, which is 8 MB for the set
against a 120 KB site. They are painted, with no fine detail and no text small
enough to alias, so they take lossy compression far harder than a photograph
would: AVIF at q58 measured 2.7% of the PNG at an RMSE of 2.9/255, which is
below what the eye resolves on this material.

WebP is emitted alongside purely as the <picture> fallback. At these sizes it
costs nothing to ship both.
"""
import sys, pathlib
from PIL import Image

SRC = pathlib.Path(__file__).parent / 'out'
DST = pathlib.Path(__file__).parents[2] / 'site' / 'hero'

# Qualities chosen from the sweep in tools/hero-gen/README, not by eye. Raising
# them buys RMSE that is already under the perceptual floor for flat art.
AVIF_Q, WEBP_Q = 58, 78

# Plates whose lighting has to be flipped to agree with the shot before them.
# Baked into the asset rather than applied as a CSS scaleX(-1), because the
# parallax engine composes transforms and a negative scale silently reverses
# the pan direction of every keyframe on that layer.
FLIP = set(a for a in sys.argv[1:] if not a.startswith('-'))


def main():
    DST.mkdir(parents=True, exist_ok=True)
    total_src = total_avif = 0
    for png in sorted(SRC.glob('*.png')):
        if 'reject' in png.stem:
            continue
        im = Image.open(png).convert('RGB')
        if png.stem in FLIP:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        for fmt, q in (('AVIF', AVIF_Q), ('WEBP', WEBP_Q)):
            out = DST / f'{png.stem}.{fmt.lower()}'
            im.save(out, fmt, quality=q)
            if fmt == 'AVIF':
                total_avif += out.stat().st_size
        total_src += png.stat().st_size
        flag = '  (flipped)' if png.stem in FLIP else ''
        print(f'{png.stem:18} {png.stat().st_size/1048576:5.2f} MB -> '
              f'{(DST / (png.stem + ".avif")).stat().st_size/1024:6.1f} KB{flag}')
    print(f'\n{"TOTAL":18} {total_src/1048576:5.2f} MB -> {total_avif/1024:6.1f} KB '
          f'({100*total_avif/total_src:.1f}%)')


if __name__ == '__main__':
    main()
