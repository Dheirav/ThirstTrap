import bpy, sys, os, math
sys.path.insert(0, os.path.join(os.getcwd(), 'tools/blender'))
from common import reset, mat, put, look_at, LAMP, MOON, LEAF, SOIL
from parts import build_pot, build_plant, dress_soil, build_finger

sc = reset(res=(1280, 720), samples=112)
CAM = (0.105, -0.215, 0.285)
AIM = (0.008, 0.000, 0.109)

body, rim, soil = build_pot()
dress_soil(soil, dimple_at=(0.015, 0.003), dimple_r=0.026, dimple_d=0.010)
build_plant(mat('leaf', LEAF, 0.42), n=6, stem_mat=mat('stem', (0.04, 0.10, 0.05, 1), 0.5),
            gap_dir=math.atan2(CAM[1], CAM[0]), gap_width=1.35,
            leaf_len=0.080, leaf_var=0.022, tilt_base=1.14, width=0.026)

f = build_finger()
f.location = (0.014, 0.002, 0.1045)
f.rotation_euler = (0.0, -0.72, -0.60)

bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0.10, -0.012))
put(bpy.context.object, mat('sill', (0.030, 0.026, 0.019, 1), 0.85)).scale = (0.90, 0.22, 0.012)

bpy.ops.object.light_add(type='AREA', location=(0.40, 0.26, 0.38))
k = bpy.context.object; k.data.energy = 15; k.data.size = 0.26
k.data.color = LAMP; k.rotation_euler = (1.10, 0.0, 2.30)
bpy.ops.object.light_add(type='AREA', location=(-0.26, -0.30, 0.30))
fl = bpy.context.object; fl.data.energy = 4.0; fl.data.size = 0.6
fl.data.color = MOON; fl.rotation_euler = (0.95, 0.0, -0.60)

w = bpy.data.worlds.new('w'); sc.world = w; w.use_nodes = True
w.node_tree.nodes['Background'].inputs[0].default_value = (0.012, 0.012, 0.015, 1)

bpy.ops.object.camera_add(location=CAM)
sc.camera = look_at(bpy.context.object, AIM, lens=60)
sc.camera.data.dof.use_dof = True
sc.camera.data.dof.focus_distance = math.dist(CAM, AIM)
sc.camera.data.dof.aperture_fstop = 5.0

sc.render.filepath = sys.argv[-1]
bpy.ops.render.render(write_still=True)
