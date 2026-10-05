"""One continuous camera move through the room, which is the only thing this
room can do that a painted plate cannot.

world.py's own header has said this from the beginning: "a camera that can move
continuously between shots instead of cutting". Seven paintings can only ever be
seven paintings with dissolves between them, and every match cut the page tried
had to be hidden in darkness because the plates disagree about where everything
is. A modelled room does not need a cut, because the camera can simply travel
from one beat to the next through a space that is actually there.

  blender -b --python tools/blender/flythrough.py -- OUTDIR [frames] [samples] [w]

Progress: tools/blender/fly-progress.sh

The path visits the room beats in scroll order. `roots` is deliberately left
out: it is a macro inside soil, five metres under the floor and not reachable
from the room by any move a camera could make, which is the one place the plates
cut too.
"""
import bpy, sys, os, math, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look
look.FLAT = True
import world, plate
from mathutils import Vector

a = sys.argv[sys.argv.index('--') + 1:]
OUT = a[0]
FRAMES = int(a[1]) if len(a) > 1 else 120
SAMPLES = int(a[2]) if len(a) > 2 else 48
W = int(a[3]) if len(a) > 3 else 836
H = int(W * 941 / 1672)
os.makedirs(OUT, exist_ok=True)

# The beats the move passes through, in scroll order, with how long it dwells.
# Dwell is what makes it read as shots rather than one long swoop: the camera
# settles, the caption lands, and then it leaves.
BEATS = ['ledger', 'shelf-evening', 'finger-test', 'depth', 'scale-table', 'phone-closeup']
DWELL = 0.42            # fraction of each leg spent nearly stationary on the beat

sc, M = world.build(res=(W, H), samples=SAMPLES)
plate.apply(sc)

# The hand belongs to two beats and was in every frame of the first pass,
# including shelf-evening and phone-closeup, because it was placed once before
# the loop and never hidden. A continuous move does not get to ignore which
# props belong to which beat; it has to strike them while the camera travels,
# the same as a cut would.
hand_at = (world.POTS_X[1], 0.978)
z = world.soil_top(*hand_at)
_before = set(bpy.data.objects)
world.place_hand((hand_at[0], hand_at[1], z), yaw=-0.55, pitch=-0.62)
HAND = [o.name for o in bpy.data.objects if o not in _before and not o.hide_render]
world.dimple_soil(hand_at)

# legs are 0:ledger>shelf 1:shelf>finger 2:finger>depth 3:depth>scale 4:scale>phone.
# The hand comes in while the camera is travelling toward finger-test and
# leaves while it travels away from depth, so it is never seen arriving.
HAND_IN, HAND_OUT = 1.55, 3.45

cam_data = bpy.data.cameras.new('fly')
cam = bpy.data.objects.new('fly', cam_data)
bpy.context.collection.objects.link(cam)
sc.camera = cam
sc.render.resolution_x, sc.render.resolution_y = W, H


def smoothstep(t):
    return t * t * (3 - 2 * t)


def ease(t):
    """Dwell at each end of a leg, travel in the middle.

    A constant-speed move between two framings reads as a drone shot. Real
    coverage holds, moves, holds, and the hold is where the sentence is read.
    """
    if t < DWELL:
        return 0.0
    if t > 1 - DWELL:
        return 1.0
    return smoothstep((t - DWELL) / (1 - 2 * DWELL))


def catmull(p0, p1, p2, p3, t):
    """Through the control points, not near them: the camera has to actually
    arrive at each beat's framing, which a Bezier would only approximate."""
    t2, t3 = t * t, t * t * t
    return tuple(0.5 * ((2 * b) + (-a + c) * t
                        + (2 * a - 5 * b + 4 * c - d) * t2
                        + (-a + 3 * b - 3 * c + d) * t3)
                 for a, b, c, d in zip(p0, p1, p2, p3))


LOC = [world.SHOTS[b][0] for b in BEATS]
AIM = [world.SHOTS[b][1] for b in BEATS]
LENS = [world.SHOTS[b][2] for b in BEATS]
# duplicate the ends so the spline starts and finishes on a real beat
LOC = [LOC[0]] + LOC + [LOC[-1]]
AIM = [AIM[0]] + AIM + [AIM[-1]]
LENS = [LENS[0]] + LENS + [LENS[-1]]

legs = len(BEATS) - 1
status = os.path.join(OUT, 'status.json')
print(f'flythrough: {FRAMES} frames over {legs} legs at {W}x{H}, {SAMPLES} samples', flush=True)

for f in range(FRAMES):
    u = f / (FRAMES - 1) * legs
    i = min(int(u), legs - 1)
    t = ease(u - i)
    loc = catmull(LOC[i], LOC[i + 1], LOC[i + 2], LOC[i + 3], t)
    aim = catmull(AIM[i], AIM[i + 1], AIM[i + 2], AIM[i + 3], t)
    lens = LENS[i + 1] + (LENS[i + 2] - LENS[i + 1]) * t
    for nm in HAND:
        o = bpy.data.objects.get(nm)
        if o:
            o.hide_render = not (HAND_IN <= u <= HAND_OUT)
    cam.location = loc
    cam_data.lens = lens
    look.aim(cam, aim)
    sc.render.filepath = os.path.join(OUT, f'f{f:04d}.png')
    bpy.ops.render.render(write_still=True)
    json.dump({'done': f + 1, 'total': FRAMES}, open(status, 'w'))
    print(f'  [{f + 1}/{FRAMES}] leg {i + 1} t={t:.2f} lens {lens:.0f}', flush=True)
