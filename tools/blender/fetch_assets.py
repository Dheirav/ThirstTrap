#!/usr/bin/env python3
"""Pull the CC0 props the room needs from Poly Haven.

Everything on Poly Haven is CC0, so these can be used and redistributed with no
attribution condition. Provenance is recorded in assets/CREDITS.md anyway,
because knowing where a mesh came from is worth more than the licence requires.

Only the .blend and the maps that change the silhouette are fetched. The look is
flat-shaded colour from look.py, so diffuse, roughness, AO and displacement are
all dead weight: the one map that matters is alpha, because without it a leaf
card renders as a solid rectangle.
"""
import json, os, sys, urllib.request, pathlib

# The API rejects urllib's default User-Agent with a 403, so say who we are.
_OPEN = urllib.request.build_opener()
_OPEN.addheaders = [('User-Agent', 'ThirstTrap-hero/1.0 (+https://github.com/Dheirav/ThirstTrap)')]
urllib.request.install_opener(_OPEN)

ASSETS = pathlib.Path(__file__).parent / 'assets'
RES = '1k'
KEEP = ('alpha',)          # maps that affect geometry as seen, not shading
WANT = [
    'potted_plant_01', 'potted_plant_02', 'potted_plant_04',
    'calathea_orbifolia_01', 'pachira_aquatica_01',
    'desk_lamp_arm_01', 'root_cluster_01', 'book_encyclopedia_set_01',
    'tea_set_01', 'wooden_spoon', 'planter_pot_clay', 'trowel_01',
]

def get(url, dest):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        return dest.stat().st_size, True
    with urllib.request.urlopen(url, timeout=120) as r, open(dest, 'wb') as f:
        f.write(r.read())
    return dest.stat().st_size, False

def main():
    ASSETS.mkdir(exist_ok=True)
    total, lines = 0, []
    for a in WANT:
        try:
            with urllib.request.urlopen(f'https://api.polyhaven.com/files/{a}', timeout=60) as r:
                d = json.load(r)
        except Exception as e:
            print(f'{a:28} FAILED {e}'); continue
        b = d.get('blend', {}).get(RES, {}).get('blend')
        if not b:
            print(f'{a:28} no {RES} blend'); continue
        n, cached = get(b['url'], ASSETS / a / f'{a}.blend')
        got = n
        for name, sub in (b.get('include') or {}).items():
            if not any(k in name.lower() for k in KEEP):
                continue
            n2, _ = get(sub['url'], ASSETS / a / pathlib.Path(name).name)
            got += n2
        total += got
        lines.append(f'- **{a}** — Poly Haven, CC0 — https://polyhaven.com/a/{a}')
        print(f'{a:28} {got/1048576:6.2f} MB{"  (cached)" if cached else ""}')
    (ASSETS / 'CREDITS.md').write_text(
        '# Third-party assets\n\n'
        'All from [Poly Haven](https://polyhaven.com), all **CC0**: usable and\n'
        'redistributable with no attribution condition. Listed anyway.\n\n'
        'Fetched at 1k with `fetch_assets.py`. Materials are replaced on append\n'
        'with the flat palette in `look.py`, so only alpha maps are kept.\n\n'
        + '\n'.join(lines) + '\n')
    print(f'\ntotal {total/1048576:.1f} MB in {ASSETS}')

if __name__ == '__main__':
    main()
