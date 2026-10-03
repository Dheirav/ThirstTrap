"""Render every beat from the one room, landscape and portrait.

  blender -b --python tools/blender/render_all.py -- OUTDIR [samples] [scale]

The room is built once and only the camera and the resolution change between
frames, which is the point: every shot is the same window, the same lamp and the
same pots, so the page can move between them instead of cutting.
"""
import bpy, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import world

a = sys.argv[sys.argv.index('--') + 1:]
OUT = a[0]
SAMPLES = int(a[1]) if len(a) > 1 else 96
SCALE = float(a[2]) if len(a) > 2 else 1.0
W, H = int(1672 * SCALE), int(941 * SCALE)

os.makedirs(OUT, exist_ok=True)
sc, M = world.build(res=(W, H), samples=SAMPLES)

ONLY = [x for x in a[3:]] if len(a) > 3 else None

# The hand belongs to two beats and cannot be in both pots at once, so it is
# placed per shot rather than built into the room.
HANDS = {
    'finger-test': dict(pot=(world.POTS_X[1], 0.930), yaw=-0.55, pitch=-0.58),
    'depth':       dict(pot=(-0.18, 0.035),           yaw=-0.35, pitch=-0.55),
}

for name in world.SHOTS:
    if ONLY and name not in ONLY:
        continue
    for portrait in (False, True):
        for c in [o for o in bpy.data.objects if o.type == 'CAMERA']:
            bpy.data.objects.remove(c, do_unlink=True)
        sc.render.resolution_x, sc.render.resolution_y = (H, W) if portrait else (W, H)
        h = HANDS.get(name)
        if h:
            z = world.soil_top(*h['pot']) or (world.TABLE_Z + 0.100)
            world.place_hand((h['pot'][0], h['pot'][1], z), yaw=h['yaw'], pitch=h['pitch'])
        else:
            world.place_hand((0, 0, -9))      # parked out of every frame
        world.set_shot(name, portrait)
        sc.render.filepath = os.path.join(OUT, f'{name}{"-p" if portrait else ""}.png')
        bpy.ops.render.render(write_still=True)
        print('SHOT', os.path.basename(sc.render.filepath))
print('ALL DONE')
