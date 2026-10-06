"""Export the room's geometry as a glb, to try the route observal actually uses.

Their page is a <canvas id="stage3d"> and a 0.63 MB Draco-compressed glb from
the Blender glTF exporter: 58 meshes, and ZERO materials, textures, images,
cameras or animations. All the colour and the flat shading happens in their
shaders, and the camera move is JavaScript driven by scroll, with GSAP's
ScrollTrigger and Lenis, which are the same two libraries this page already
loads.

This exports with materials rather than without, because their node names are
hand-authored (lamp_shade, desk_top, chart_pin) and ours are whatever Blender
called them (Cube.041, leaf.060). Carrying the base colour through as
baseColorFactor lets the shader read the palette off the geometry instead of
needing a name table.

Lights and cameras are left out on purpose: the point of the exercise is that
both belong to the page, not to the file.

  blender -b --python tools/blender/export_glb.py -- OUT.glb
"""
import bpy, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look
look.FLAT = True
import world, plate

a = sys.argv[sys.argv.index('--') + 1:]
OUT = a[0] if a else '/tmp/scene.glb'

sc, M = world.build(res=(16, 16), samples=1)
plate.flat_materials()          # strip specular and texture, keep the flat albedo
# The hand, placed at the finger-test pot exactly as render_plate.py places it.
#
# It used to be parked at z -9 and then deleted by the cleanup below, which
# removes everything under the floor. That was written when the scripted hand
# was unusable and nobody wanted it in the move; the move now travels past this
# pot and the beat is a finger going into soil, so an empty pot is the one thing
# that frame cannot be. The flythrough showed no hands at all because of it.
world.HAND_LEN = 0.26
_pot = (world.POTS_X[1], 0.978)
_z = world.soil_top(*_pot)
if _z is not None:
    world.place_hand((_pot[0], _pot[1], _z), yaw=0.50, pitch=-0.35, into=0.004)
    world.dimple_soil(_pot)

# The soil macro is its own set five metres under the floor and is not part of
# the room the camera flies through, so it is not worth the bytes.
for o in list(bpy.data.objects):
    if o.type == 'MESH' and o.matrix_world.translation.z < -2.0:
        bpy.data.objects.remove(o, do_unlink=True)

for o in list(bpy.data.objects):
    if o.type in {'LIGHT', 'CAMERA'}:
        bpy.data.objects.remove(o, do_unlink=True)

meshes = [o for o in bpy.data.objects if o.type == 'MESH' and not o.hide_render]
print(f'exporting {len(meshes)} meshes')

bpy.ops.object.select_all(action='DESELECT')
for o in meshes:
    o.select_set(True)
bpy.context.view_layer.objects.active = meshes[0]

bpy.ops.export_scene.gltf(
    filepath=OUT,
    export_format='GLB',
    use_selection=True,
    export_apply=True,              # bake the modifiers: booleans, solidify, displace
    export_materials='EXPORT',
    # AUTO, not NONE. The ledger sheet is the only image in the room and it is
    # the entire subject of one beat: with images stripped, the page exported as
    # blank cream paper and the flythrough showed an empty sheet with a pin in
    # it. 'It can be refetched' was true and useless, because nothing refetched
    # it. It costs one small PNG.
    export_image_format='AUTO',
    export_cameras=False,
    export_lights=False,
    export_animations=False,
    export_draco_mesh_compression_enable=True,
    export_draco_mesh_compression_level=6,
    export_yup=True,
)
print('wrote', OUT, f'{os.path.getsize(OUT)/1048576:.2f} MB')
