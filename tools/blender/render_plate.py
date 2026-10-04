"""Render the room in the flat-vector look of the plates.

  blender -b --python tools/blender/render_plate.py -- OUTDIR [samples] [shots...]

Knobs, all overridable with KEY=VALUE before the shot names:
  steps=4  value steps per surface   lift=0.06  the shadow floor
  gamma=0.85  where the steps land   lights=0.25  how hard the shadows are
  ambient=0.14  the shadow floor      exposure=0.21 where the steps land
  gain=4.6    overall brightness
"""
import bpy, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look
look.FLAT = True                      # before world.build, which makes the materials
import world, plate

a = sys.argv[sys.argv.index('--') + 1:]
OUT = a[0]
SAMPLES = int(a[1]) if len(a) > 1 else 64
rest = a[2:]
kw = {}
shots = []
for t in rest:
    if '=' in t:
        k, v = t.split('=', 1); kw[k] = float(v)
    else:
        shots.append(t)

os.makedirs(OUT, exist_ok=True)
W, H = int(kw.get('w', 1672)), int(kw.get('h', 941))
sc, M = world.build(res=(W, H), samples=SAMPLES)
plate.apply(sc, steps=int(kw.get('steps', 4)), lift=kw.get('lift', 0.06),
            gamma=kw.get('gamma', 0.85), light_scale=kw.get('lights', 0.25),
            ambient=kw.get('ambient', 0.14), exposure=kw.get('exposure', 0.21),
            gain=kw.get('gain', 4.6))

HANDS = {
    'finger-test': dict(pot=(world.POTS_X[1], 0.930), yaw=-0.55, pitch=-0.58),
    'depth':       dict(pot=(-0.18, 0.035),           yaw=-0.35, pitch=-0.55),
}

for name in (shots or world.SHOTS):
    loc, aim, lens, plens, fstop = world.SHOTS[name]
    for c in [o for o in bpy.data.objects if o.type == 'CAMERA']:
        bpy.data.objects.remove(c, do_unlink=True)
    h = HANDS.get(name)
    # place_hand takes the position as one tuple, not three arguments.
    if h:
        z = world.soil_top(*h['pot']) or (world.TABLE_Z + 0.100)
        world.place_hand((h['pot'][0], h['pot'][1], z), yaw=h['yaw'], pitch=h['pitch'])
    else:
        world.place_hand((0, 0, -9))      # parked out of every frame
    sc.render.resolution_x, sc.render.resolution_y = W, H
    sc.camera = world.look.camera(loc, aim, lens, None)   # no DoF in the plates
    sc.render.filepath = os.path.join(OUT, f'{name}.png')
    print(f'--- {name} ---', flush=True)
    bpy.ops.render.render(write_still=True)
