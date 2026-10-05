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
# Solved, not swept. gain and ambient come from the pot's own albedo against
# the plate's lit and shaded pot values: lit/shade ratio 3.83 fixes the floor,
# and the floor fixes the gain. The old 6.5 and 0.05 were a grid search's
# answer to a question about the whole frame's histogram, and they ran the
# picture hot enough to clip 3.3% of phone-closeup, where no plate clips at all.
# 0.26 rather than a real 0.34: finger-test is a 42mm close-up and a real arm
# fills two thirds of it. See the note on HANDS above for why that shot is still
# not solved.
world.HAND_LEN = float(os.environ.get('TT_HAND', 0.26))
plate.apply(sc, steps=int(kw.get('steps', 5)), lift=kw.get('lift', 0.0),
            gamma=kw.get('gamma', 0.65), light_scale=kw.get('lights', 0.25),
            ambient=kw.get('ambient', 0.04), exposure=kw.get('exposure', 0.30),
            gain=kw.get('gain', 3.2))

HANDS = {
    # y was 0.930, which is the pot's FRONT RIM, not its soil. The sill pots
    # sit at y 0.985 with a top radius near 0.057, so the old number put the
    # fingertip exactly on the lip and the finger read as going through the
    # pot wall. Aim at the middle of the soil instead.
    # finger-test is NOT solved. Six angles were rendered and none reads: that
    # camera sits at (0.02, 0.50, 1.175) and looks at the pot from the front
    # right, the arm enters from frame right, so the extended index points back
    # toward the lens and disappears behind the hand's own mass. No yaw fixes
    # that, because the problem is which side of the hand the camera is on. The
    # shot was framed around build_finger's thin cone and wants reframing to see
    # the hand in profile, the way depth already does.
    #
    # yaw is the compass direction the FOREARM leaves along, in world terms, and
    # it has to be read off the camera rather than carried over from
    # build_finger, whose cone used the opposite convention. For finger-test the
    # camera sits at (0.02, 0.50, 1.175) looking at (-0.245, 0.975, 1.052), so
    # frame-right is about (0.87, 0.49, 0) and the arm wants yaw +0.5. At -0.55
    # it pointed at the lens: the arm foreshortened into a flat slab, which read
    # as a hand lying on its side rather than reaching down.
    'finger-test': dict(pot=(world.POTS_X[1], 0.978),
                        yaw=float(os.environ.get('TT_YAW', 0.75)),
                        pitch=float(os.environ.get('TT_PITCH', -0.75))),
    'depth':       dict(pot=(-0.18, 0.035),           yaw=0.70, pitch=-0.55),
}

# Which shots the cut pot is in. Everywhere else it is struck, like a set.
CUT_SHOTS = {'depth', 'roots'}

for name in (shots or world.SHOTS):
    for nm in world.CUT_POT:
        o = bpy.data.objects.get(nm)
        if o:
            o.hide_render = name not in CUT_SHOTS
    loc, aim, lens, plens, fstop = world.SHOTS[name]
    for c in [o for o in bpy.data.objects if o.type == 'CAMERA']:
        bpy.data.objects.remove(c, do_unlink=True)
    h = HANDS.get(name)
    # place_hand takes the position as one tuple, not three arguments.
    if h:
        z = world.soil_top(*h['pot'])
        if z is None:
            # Silent fallbacks are how the finger ended up under the sill for
            # several rounds. If the soil cannot be found, say so.
            raise SystemExit(f'soil_top found no soil near {h["pot"]} for {name}')
        world.place_hand((h['pot'][0], h['pot'][1], z), yaw=h['yaw'], pitch=h['pitch'])
        world.dimple_soil((h['pot'][0], h['pot'][1]))
    else:
        world.place_hand((0, 0, -9))      # parked out of every frame
        world._undimple()                 # and no dent in a pot nobody touched
    sc.render.resolution_x, sc.render.resolution_y = W, H
    sc.camera = world.look.camera(loc, aim, lens, None)   # no DoF in the plates
    sc.render.filepath = os.path.join(OUT, f'{name}.png')
    print(f'--- {name} ---', flush=True)
    bpy.ops.render.render(write_still=True)
