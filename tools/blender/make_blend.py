"""Build the room and save it as a .blend you can open and poke at.

  blender -b --python tools/blender/make_blend.py -- [OUT.blend]

The room has no .blend of its own: world.py builds it from scratch on every
render, which is why there was nothing to open. This writes one out, with the
flat-look compositor already applied and a camera per shot, named for the shot,
so switching between them in the viewport shows what render_plate.py renders.

IMPORTANT, and it is the whole catch with this file: it is an OUTPUT, not a
source. Nothing you change here reaches world.py, and the next render rebuilds
the room from the script and discards it. Use it to find numbers by eye, which
is far quicker than guessing them at a terminal, then either tell me the values
or save the file and I will read the transforms back out of it.
"""
import bpy, sys, os

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import look
look.FLAT = True                      # before world.build, which makes materials
import world, plate

a = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
OUT = a[0] if a else os.path.join(HERE, 'out-plate', 'room.blend')
os.makedirs(os.path.dirname(OUT), exist_ok=True)

sc, M = world.build(res=(1672, 941), samples=80)
plate.apply(sc, steps=5, lift=0.0, gamma=0.65, light_scale=0.25,
            ambient=0.04, exposure=0.30, gain=3.2)

# The hand, placed for finger-test. The cut pot stays visible: depth and roots
# are the shots that use it, and hiding it here would just confuse the view.
world.HAND_LEN = 0.26
pot = (world.POTS_X[1], 0.978)
z = world.soil_top(*pot)
if z is not None:
    world.place_hand((pot[0], pot[1], z), yaw=0.75, pitch=0.0)
    world.dimple_soil(pot)

# One camera per shot, named for it. Blender's numpad-0 shows the ACTIVE one,
# so set the active camera from the outliner to switch shots.
for c in [o for o in bpy.data.objects if o.type == 'CAMERA']:
    bpy.data.objects.remove(c, do_unlink=True)
for name, (loc, aim, lens, plens, fstop) in world.SHOTS.items():
    cam = world.look.camera(loc, aim, lens, None)
    cam.name = 'cam_' + name
    cam.data.name = 'cam_' + name
sc.camera = bpy.data.objects.get('cam_ledger') or sc.camera

bpy.ops.wm.save_as_mainfile(filepath=OUT)
print('WROTE', OUT, '  cameras:', ', '.join(sorted(
    o.name for o in bpy.data.objects if o.type == 'CAMERA')))
