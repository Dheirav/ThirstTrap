#!/usr/bin/env python3
"""Lint PROMPTS-hero.md before spending an hour of generation on it.

Everything here is a mistake that was actually made and only discovered by
looking at finished plates.

The expensive one: the room block listed its recurring objects as bare nouns,
"a flat kitchen scale, a phone lying face up beside it, a mug". A noun is not a
description, so the generator designed each object fresh every time it appeared.
The result was one phone with a black bezel and a lit screen in `scale-table`
and a different phone with a silver band and a dark screen in `phone-closeup`,
and three unrelated vessels across the scenes that mention a mug: a handle-less
faceted tumbler, a squat lidless cup, and a proper handled mug with coffee in
it. Nothing measured it, because none of it shows up in a palette statistic. A
viewer spots it immediately.

So: every prop a scene puts in frame must be described in the prop sheet, and
the two orientations of one beat must put the same props in frame.

The structural ones: gen.mjs sends exactly the style block and one scene body,
so anything in a third section is never sent, and a scene without a trailing
colon and an indented body is silently skipped.

  python3 tools/hero-gen/check-prompts.py
"""
import os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
PROMPTS = os.path.join(HERE, 'PROMPTS-hero.md')
# gen.mjs resolves attachments against the Axl tree, not this one
REFS_ROOT = '/home/dheirav/Code/Axl/mascot-source/generated'

text = open(PROMPTS).read()
lines = text.split('\n')
problems = []


def indented(i):
    out = []
    while i < len(lines) and (lines[i].startswith('    ') or not lines[i].strip()):
        out.append(lines[i][4:])
        i += 1
    return '\n'.join(out).strip()


style_at = next((i for i, l in enumerate(lines) if l.startswith('## Style block')), -1)
if style_at < 0:
    sys.exit('no "## Style block" heading: gen.mjs would send no style at all')
style = indented(next(i for i, l in enumerate(lines) if i > style_at and l.startswith('    ')))

scenes = []
for i, l in enumerate(lines):
    m = re.match(r'^\*\*([a-z0-9-]+)\.png\*\*, attach (.+):$', l.strip())
    if m:
        refs = re.findall(r'`([^`]+)`', m.group(2))
        scenes.append({'name': m.group(1), 'refs': refs, 'body': indented(i + 2), 'line': i + 1})

print(f'{len(scenes)} scenes parse, style block {len(style)} chars\n')

# --- the style block carries everything that is sent every time ----------
for need, why in (('THE ROOM', 'the shared room'),
                  ('THE PROPS', 'the prop sheet'),
                  ('only warm light', 'the one-lamp rule')):
    if need not in style:
        problems.append(f'style block is missing {why} ("{need}"). '
                        'gen.mjs sends only the style block and one scene body, '
                        'so anything outside it never reaches the model')

# --- the prop sheet, and who uses it -------------------------------------
# The style block arrives here already dedented by indented(), so the prop
# names sit at column 0, not at four spaces. Getting that wrong made the sheet
# parse as empty and every scene look like it referenced an undescribed prop.
sheet = {m.group(1).strip(): m.group(2).strip() for m in re.finditer(
    r'^([A-Z][A-Z ]{1,}[A-Z]):\s(.+?)(?=\n[A-Z][A-Z ]{1,}[A-Z]:|\n\n[A-Z][a-z]|\Z)',
    style, re.M | re.S)}
for k in ('THE ROOM', 'THE PROPS'):
    sheet.pop(k, None)

print('prop sheet defines: ' + ', '.join(sorted(sheet)) if sheet else 'prop sheet is EMPTY')

used = {}
for sc in scenes:
    m = re.search(r'In frame from the prop sheet: (.+?)\. Draw each one', sc['body'], re.S)
    if not m:
        problems.append(f'{sc["name"]}: no "In frame from the prop sheet" line, so nothing '
                        'tells it which objects must match the other scenes')
        continue
    # Match the sheet's own keys rather than guessing at capitalised words:
    # a generic all-caps regex both missed three-letter props and would pick up
    # shouted words in prose.
    blob = m.group(1)
    named = {k for k in sheet if re.search(r'\b' + re.escape(k) + r'\b', blob)}
    stray = {w for w in re.findall(r'\b([A-Z][A-Z ]{1,}[A-Z])\b', blob)
             if w.strip() not in sheet}
    for w in sorted(stray):
        problems.append(f'{sc["name"]}: names {w.strip()} in frame but the prop sheet does '
                        'not describe it, so it will be redesigned per scene')
    sc['props'] = named
    for n in named:
        used.setdefault(n, []).append(sc['name'])

for prop in sheet:
    if prop not in used:
        problems.append(f'prop sheet describes {prop} but no scene puts it in frame')

print('\nprops by how many scenes they appear in (2+ is where drift happens):')
for prop, who in sorted(used.items(), key=lambda kv: -len(kv[1])):
    print(f'  {prop:14s} {len(who):2d}  {", ".join(who)}')

# --- the two orientations of one beat must agree -------------------------
print('\norientation pairs:')
by = {sc['name']: sc for sc in scenes}
for sc in scenes:
    if sc['name'].endswith('-p'):
        continue
    other = by.get(sc['name'] + '-p')
    if not other:
        problems.append(f'{sc["name"]} has no portrait counterpart')
        continue
    a, b = sc.get('props', set()), other.get('props', set())
    same_lamp = ('lamp' in sc['body'].lower()) and ('lamp' in other['body'].lower())
    ok = a == b and same_lamp
    print(f'  {sc["name"]:16s} {"same props and both state the lamp" if ok else "MISMATCH"}')
    if a != b:
        problems.append(f'{sc["name"]}: landscape and portrait put different props in frame '
                        f'({sorted(a ^ b)}), which is how one beat ends up with two rooms')
    if not same_lamp:
        problems.append(f'{sc["name"]}: one orientation does not state the lamp')

# --- attachments have to exist where gen.mjs will look -------------------
for sc in scenes:
    for r in sc['refs']:
        if not os.path.exists(os.path.join(REFS_ROOT, r)):
            problems.append(f'{sc["name"]}: attachment not found: {r}')

print(f'\n{len(problems)} problem(s)' + (':' if problems else ''))
for p in problems:
    print(f'  {p}')
sys.exit(1 if problems else 0)
