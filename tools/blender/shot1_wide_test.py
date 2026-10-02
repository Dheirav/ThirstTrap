import bpy, sys, os, math
sys.path.insert(0, os.path.join(os.getcwd(), 'tools/blender'))
from common import reset, mat, put, look_at, LAMP, MOON, LEAF, TERRACOTTA
from parts import build_hand, build_pot, build_plant, dress_soil

sc = reset(res=(1280, 720), samples=96)
body, rim, soil = build_pot()
# the finger has been pushed in at this spot, so the soil is already dished
dress_soil(soil, dimple_at=(0.014, 0.004), dimple_r=0.020, dimple_d=0.011)

build_plant(mat('leaf', LEAF, 0.42), n=7, stem_mat=mat('stem', (0.04, 0.10, 0.05, 1), 0.5),
            gap_dir=math.atan2(-0.78, 0.34), gap_width=0.95,
            leaf_len=0.078, leaf_var=0.022, tilt_base=1.08, width=0.026)

hand = build_hand()
hand.location = (0.014, 0.004, 0.106)
hand.rotation_euler = (0.22, 0.0, -0.55)

bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0.06, -0.012))
put(bpy.context.object, mat('sill', (0.035, 0.030, 0.022, 1), 0.8)).scale = (0.55, 0.16, 0.012)

# Key light now BEHIND and right, so the hand is rim-lit rather than modelled
# by the light: a silhouette hides geometry a procedural hand cannot earn.
bpy.ops.object.light_add(type='AREA', location=(0.42, 0.34, 0.40))
k = bpy.context.object; k.data.energy = 30; k.data.size = 0.28
k.data.color = LAMP; k.rotation_euler = (1.15, 0.0, 2.35)
# a low bounce so the soil is readable at all
bpy.ops.object.light_add(type='AREA', location=(-0.30, -0.34, 0.34))
f = bpy.context.object; f.data.energy = 5.5; f.data.size = 0.7
f.data.color = MOON; f.rotation_euler = (0.95, 0.0, -0.55)

w = bpy.data.worlds.new('w'); sc.world = w; w.use_nodes = True
w.node_tree.nodes['Background'].inputs[0].default_value = (0.012, 0.012, 0.015, 1)

# higher and closer: looking DOWN at the soil, which is what the shot is about,
# and tight enough that the hand is cropped to a finger and a knuckle
CAM = (0.34, -0.78, 0.46)
bpy.ops.object.camera_add(location=CAM)
sc.camera = look_at(bpy.context.object, (0.006, 0.0, 0.110), lens=50)
sc.camera.data.dof.use_dof = True
sc.camera.data.dof.focus_distance = 0.92
sc.camera.data.dof.aperture_fstop = 4.0

sc.render.filepath = sys.argv[-1]
bpy.ops.render.render(write_still=True)
