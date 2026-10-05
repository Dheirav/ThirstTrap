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
world.place_hand((0, 0, -9))    # park the hand out of the room

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
    export_image_format='NONE',     # the ledger texture is the only image and it can be refetched
    export_cameras=False,
    export_lights=False,
    export_animations=False,
    export_draco_mesh_compression_enable=True,
    export_draco_mesh_compression_level=6,
    export_yup=True,
)
print('wrote', OUT, f'{os.path.getsize(OUT)/1048576:.2f} MB')
