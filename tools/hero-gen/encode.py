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
import harmonise as _h

SRC = pathlib.Path(__file__).parent / 'out'
DST = pathlib.Path(__file__).parents[2] / 'site' / 'hero'

# Qualities chosen from the sweep in tools/hero-gen/README, not by eye. Raising
# them buys RMSE that is already under the perceptual floor for flat art.
AVIF_Q, WEBP_Q = 58, 78

# Plates whose lighting has to be flipped to agree with the shot before them.
# Baked into the asset rather than applied as a CSS scaleX(-1), because the
# parallax engine composes transforms and a negative scale silently reverses
# the pan direction of every keyframe on that layer.
#
# finger-test is the one that needs this, and it is worth writing down why,
# because the obvious reading is wrong. The room in tools/blender puts the lamp
# to the right of all seven cameras, and the unflipped plate agrees with it:
# highlights at 0.71. Flipping moves them to 0.29, which disagrees. So the flip
# looks like a mistake.
#
# It is not, yet. Five of the seven landscape plates are lit from the left, so
# the plate that agrees with the room is the odd one out in the sequence the
# viewer actually scrolls through. Unflipped, the lamp changes sides three times
# down the page; flipped, once. Until the set is regenerated from the rewritten
# prompts, agreeing with its neighbours beats agreeing with the room, because
# what reads as "a different room" is the light moving, not the light being on
# a side no viewer can check.
#
# Three portrait plates get the same treatment, for the same reason. Portrait
# changed sides six times down the scroll, which is worse than landscape ever
# was. finger-test-p also had the hand entering from the right while the shipped
# landscape has it entering from the left, so flipping it fixes the lamp and the
# hand together. roots-p and phone-closeup-p carry no text and no hand, so a
# mirror costs nothing. ledger-p is the one that cannot be flipped at any price,
# because the page is covered in handwriting, which is why portrait still starts
# on the wrong side and lands at one change rather than none.
#
# Drop all four after a regeneration, and confirm with check-plates.py rather
# than by eye: both rows should read R R R R R R R with 0 changes.
FLIP = set(a for a in sys.argv[1:] if not a.startswith('-'))

# Pull every plate's leaves onto one green. See harmonise.py: the generated
# plates range from hue 60 to 102 against a specified 134, which reads as a
# different picture rather than a different time of day. Off by default so the
# raw encode stays reproducible.
HARMONISE = '--harmonise' in sys.argv


def main():
    DST.mkdir(parents=True, exist_ok=True)
    total_src = total_avif = 0
    for png in sorted(SRC.glob('*.png')):
        # section.png was dropped when the hero went to seven plates and the page
        # has never referenced it. Encoding it shipped two files nothing loads.
        if 'reject' in png.stem or png.stem == 'section':
            continue
        im = Image.open(png).convert('RGB')
        if HARMONISE:
            im = _h.harmonise(im)[0]
        if png.stem in FLIP:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        for fmt, q in (('AVIF', AVIF_Q), ('WEBP', WEBP_Q)):
            out = DST / f'{png.stem}.{fmt.lower()}'
            im.save(out, fmt, quality=q)
            if fmt == 'AVIF':
                total_avif += out.stat().st_size
        total_src += png.stat().st_size
        flag = ('  (flipped)' if png.stem in FLIP else '') + \
               ('  (harmonised)' if HARMONISE else '')
        print(f'{png.stem:18} {png.stat().st_size/1048576:5.2f} MB -> '
              f'{(DST / (png.stem + ".avif")).stat().st_size/1024:6.1f} KB{flag}')
    print(f'\n{"TOTAL":18} {total_src/1048576:5.2f} MB -> {total_avif/1024:6.1f} KB '
          f'({100*total_avif/total_src:.1f}%)')


if __name__ == '__main__':
    main()
