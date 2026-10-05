#!/usr/bin/env python3
"""Draw the ledger sheet as an image, because text is the one thing here that is
cheaper as a texture than as geometry.

The sheet is the subject of its own shot and it was blank. The rows existed as
thin boxes and the words as solid bars, which at render scale read as a ruled
but empty page: the beat is "the same word, every day, seven times", and an
empty page does not say it.

Z003 is URW Chancery, the only script face on this machine, and the per row
jitter is there because seven identical lines of type read as a printed form.
The point of the picture is that a person wrote the same thing out by hand
until it stopped meaning anything.

  python3 tools/blender/make_ledger.py
"""
import os, random
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'assets', 'ledger-sheet.png')
W, H = 540, 716
PAPER, INK, RULE = (217, 205, 178), (62, 52, 38), (138, 126, 104)
SCRIPT = '/usr/share/fonts/opentype/urw-base35/Z003-MediumItalic.otf'
# Dates, and what was written beside each one.
#
# It used to read "Watered" on all seven rows, and that was the wrong beat for
# this app to put on its own hero. Watering every day is the calendar habit the
# product exists to replace; a pot weighed every day is watered when it is light
# and left alone when it is not. So the page now says what the app would
# actually have you write, which is the same two answers its buttons offer, and
# the gaps between the waterings are the point rather than the repetition.
ROWS = [('Sep 29', 'Watered'),
        ('Sep 30', 'Still wet'),
        ('Oct 1',  'Still wet'),
        ('Oct 2',  'Watered'),
        ('Oct 3',  'Still wet'),
        ('Oct 4',  'Still wet'),
        ('Oct 5',  'Watered')]


def main():
    rnd = random.Random(7)
    im = Image.new('RGB', (W, H), PAPER)
    d = ImageDraw.Draw(im)
    try:
        f = ImageFont.truetype(SCRIPT, 44)
        fs = ImageFont.truetype(SCRIPT, 40)
    except OSError:
        f = fs = ImageFont.load_default()

    top, gap = 150, 74
    d.line([(52, top - 34), (W - 52, top - 34)], fill=INK, width=4)   # the heavy rule
    for i, (date, word) in enumerate(ROWS):
        y = top + i * gap
        d.line([(52, y + 44), (W - 52, y + 44)], fill=RULE, width=2)  # hairline
        # Jitter per row. Seven lines set identically read as a printed form,
        # and the whole point is that a person wrote this out by hand.
        d.text((72 + rnd.randint(-3, 3), y + rnd.randint(-4, 4)), date, font=fs, fill=INK)
        d.text((272 + rnd.randint(-4, 4), y + rnd.randint(-4, 4)), word, font=f, fill=INK)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    im.save(OUT)
    print('wrote', OUT, im.size)


if __name__ == '__main__':
    main()
