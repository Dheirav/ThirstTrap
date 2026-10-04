"""Where everything in the room lands in each shot's frame.

The plates were generated one at a time, so each one invented its own room: the
lamp sits left in five shots and right in two, and two of the portrait plates
light the same beat from the opposite side to their landscape twin. Measured in
tools/hero-gen, that is a 0.65 swing in the left/right luminance balance inside
one scroll.

The room already knows the answer, because the room is one room. This projects
the landmarks the viewer uses to locate themselves (lamp, window, moon, ledger,
table) into every shot's camera and prints the screen position, so the prompts
can state them as fact instead of saying "lit warmly from one side".

  blender -b --python tools/blender/continuity.py
"""
import bpy, sys, os, json, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import world
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector

sc, M = world.build(res=(1672, 941), samples=1)

# Picking "the brightest light" grabbed the moon outside the window, which sits
# at y 2.60, past the window wall. The lamp is the brightest light INSIDE the
# room, so bound it to the near side of the glass.
lights = [(o, getattr(o.data, 'energy', 0)) for o in bpy.data.objects if o.type == 'LIGHT']
print('\nlights:')
for o, e in sorted(lights, key=lambda t: -t[1]):
    t = o.matrix_world.translation
    print(f'  {o.name[:28]:28s} energy {e:7.1f}  ({t.x:+.2f}, {t.y:+.2f}, {t.z:+.2f})'
          + ('   <- outside' if t.y > world.WIN_Y else ''))
inside = [(o, e) for o, e in lights if o.matrix_world.translation.y <= world.WIN_Y]
k = max(inside, key=lambda t: t[1])[0] if inside else None
print(f'key lamp -> {k.name if k else "none"}')
LANDMARKS = {
    'lamp':    tuple(k.matrix_world.translation) if k else None,
    'window':  (-0.17, world.WIN_Y, 1.50),
    'ledger':  (-1.02, world.WIN_Y - 0.06, 1.44),
    'table':   (0.42, 0.06, world.TABLE_Z),
    'sill-L':  (world.POTS_X[0], 0.97, world.SILL_Z),
    'sill-R':  (world.POTS_X[3], 0.97, world.SILL_Z),
}
print('\nlandmarks (room metres, +x right as you face the window, +y toward it):')
for n, p in LANDMARKS.items():
    print(f'  {n:8s} {"(%.2f, %.2f, %.2f)" % p if p else "-"}')

def side(x):
    return 'off-frame left' if x < -0.02 else 'off-frame right' if x > 1.02 else \
           'left' if x < 0.36 else 'right' if x > 0.64 else 'centre'


def lamp_dir(cam):
    """Where the lamp is relative to THIS camera, as a painter would say it.

    The lamp's screen x is useless on its own: in four of the seven shots the
    lamp is behind the camera, and world_to_camera_view still returns an x for a
    point behind the lens, mirrored. Camera-local coordinates do not have that
    problem. Blender's camera looks down its own -z, with +x right and +y up, so
    the signs read straight off as left/right and above/below.
    """
    v = cam.matrix_world.inverted() @ Vector(LANDMARKS['lamp'])
    front = v.z < 0
    horiz = math.degrees(math.atan2(v.x, -v.z if front else v.z))
    lr = 'right' if v.x > 0.05 else 'left' if v.x < -0.05 else 'straight on'
    ud = 'above' if v.y > 0.05 else 'below' if v.y < -0.05 else 'level'
    return v, front, horiz, lr, ud


out = {}
for name, (loc, aim, lens, plens, fstop) in world.SHOTS.items():
    rows = {}
    for portrait, L in ((False, lens), (True, plens)):
        sc.render.resolution_x, sc.render.resolution_y = (941, 1672) if portrait else (1672, 941)
        for c in [o for o in bpy.data.objects if o.type == 'CAMERA']:
            bpy.data.objects.remove(c, do_unlink=True)
        cam = world.look.camera(loc, aim, L, fstop)
        sc.camera = cam
        bpy.context.view_layer.update()
        view = 'portrait' if portrait else 'landscape'
        for n, pt in LANDMARKS.items():
            if pt is None: continue
            v = world_to_camera_view(sc, cam, Vector(pt))
            rows.setdefault(n, {})[view] = (v.x, v.y, v.z)
        lv, front, horiz, lr, ud = lamp_dir(cam)
        rows.setdefault('_lamp_dir', {})[view] = dict(
            local=[lv.x, lv.y, lv.z], in_front=front, horiz_deg=horiz, lr=lr, ud=ud)
    out[name] = rows

print(f'\n{"shot":15s} {"view":10s} | {"the lamp is":26s} {"deg off axis":>12s} | '
      f'{"window":>8s} {"ledger":>8s} {"table":>7s}')
print('-' * 96)
for name, rows in out.items():
    for view in ('landscape', 'portrait'):
        d = rows['_lamp_dir'][view]
        g = lambda n: rows.get(n, {}).get(view, (float('nan'),))[0]
        where = f"{d['ud']} {d['lr']}" + ('' if d['in_front'] else ', behind camera')
        print(f'{name:15s} {view:10s} | {where:26s} {d["horiz_deg"]:11.0f}  | '
              f'{side(g("window"))[:8]:>8s} {side(g("ledger"))[:8]:>8s} {side(g("table"))[:7]:>7s}')

print('\nPAIRS — landscape and portrait of one beat must agree about the lamp:')
for name, rows in out.items():
    a, b = rows['_lamp_dir']['landscape'], rows['_lamp_dir']['portrait']
    ok = 'OK' if a['lr'] == b['lr'] and a['in_front'] == b['in_front'] else '*** DISAGREES ***'
    print(f'  {name:15s} landscape {a["lr"]:11s} portrait {b["lr"]:11s} {ok}')

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'continuity.json')
json.dump(out, open(p, 'w'), indent=1)
print('\nwrote', p)
