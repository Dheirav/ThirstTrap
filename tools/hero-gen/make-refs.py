#!/usr/bin/env python3
"""Build the attachments that make the generated plates agree with the room.

Two kinds, both regenerated from source rather than hand-made:

  layout-<shot>.png   the Blender room from that shot's camera. This is the
                      only thing that actually fixes continuity, because the
                      room cannot disagree with itself. Lifted toward a common
                      brightness: the renders are a night room and three of
                      them sit under mean 40, which is right for the look and
                      useless as a guide, since you cannot place a pot you
                      cannot see. A fixed gamma was wrong the other way, it
                      washed the already-lit shots to near white, so each one
                      is solved for the exponent that lands it on the target.

  ref-palette.png     the palette as a picture. The hex codes in the style
                      block were ignored on all fourteen plates: foliage came
                      back at hue 60 to 102 against a spec of 134, olive rather
                      than green. A swatch is a reference it will look at.

  python3 tools/hero-gen/make-refs.py
"""
import os, glob
from PIL import Image, ImageDraw, ImageFont, ImageEnhance

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
SRC, DST = os.path.join(ROOT, 'tools', 'blender', 'out'), os.path.join(HERE, 'refs')
TARGET = 96.0

def font(sz):
    for p in ('/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf',
              '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'):
        try:
            return ImageFont.truetype(p, sz)
        except OSError:
            pass
    return ImageFont.load_default()

def meanL(im):
    return sum(im.convert('L').resize((64, 64)).getdata()) / 4096

def lift(im, target=TARGET):
    """Solve for the gamma that puts this image's mean at `target`."""
    lo, hi = 0.20, 1.00
    for _ in range(18):
        g = (lo + hi) / 2
        lut = [min(255, int(255 * ((i / 255) ** g))) for i in range(256)]
        m = meanL(im.point(lut * 3))
        if m < target:
            hi = g
        else:
            lo = g
    lut = [min(255, int(255 * ((i / 255) ** g))) for i in range(256)]
    return im.point(lut * 3), g

os.makedirs(DST, exist_ok=True)
print(f'{"layout ref":28s} {"mean in":>8s} {"gamma":>6s} {"mean out":>9s}')
for f in sorted(glob.glob(os.path.join(SRC, '*.png'))):
    name = os.path.splitext(os.path.basename(f))[0]
    im = Image.open(f).convert('RGB')
    before = meanL(im)
    out, g = lift(im)
    out = ImageEnhance.Color(out).enhance(0.55)      # it is a layout, not a palette
    out.thumbnail((1200, 1200))
    out.save(os.path.join(DST, f'layout-{name}.png'))
    print(f'{"layout-" + name + ".png":28s} {before:8.0f} {g:6.2f} {meanL(out):9.0f}')

P = [('ground',       '#1E1913', 'darkest ground and shadow'),
     ('ground light', '#2E2820', 'the lighter ground step'),
     ('paper',        '#F5F0E2', 'paper, text, the lit edge'),
     ('foliage',      '#2F5D3A', 'leaves in shadow.  GREEN, not olive'),
     ('foliage lit',  '#43794F', 'leaves in light.   GREEN, not olive'),
     ('terracotta',   '#8C4F43', 'pots. MUTED, less saturated than the accent'),
     ('night sky',    '#22303C', 'sky through the window'),
     ('accent',       '#C98A3A', 'ONE thing only, under 1% of the picture')]
W, SW, ROW, PAD = 1360, 300, 112, 44
card = Image.new('RGB', (W, ROW * len(P) + 112), (24, 21, 17))
d = ImageDraw.Draw(card)
d.text((PAD, 30), 'THIRSTTRAP HERO PALETTE', fill='#F5F0E2', font=font(34))
d.text((PAD, 74), 'use these exact colours and no others', fill='#C98A3A', font=font(22))
for i, (n, hexc, note) in enumerate(P):
    y = 112 + i * ROW
    d.rectangle([PAD, y, PAD + SW, y + ROW - 16], fill=hexc, outline='#4A4036')
    d.text((PAD + SW + 32, y + 16), f'{hexc}   {n}', fill='#F5F0E2', font=font(26))
    d.text((PAD + SW + 32, y + 56), note, fill='#B9AE9B', font=font(20))
card.save(os.path.join(DST, 'ref-palette.png'))
print(f'\nref-palette.png {card.size}')
