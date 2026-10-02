import bpy, sys, os
sys.path.insert(0, os.path.join(os.getcwd(), 'tools/blender'))
from build import build_all, set_cam
which = sys.argv[-2]
sc, H = build_all(samples=96)
set_cam(sc, which)
sc.render.filepath = sys.argv[-1]
bpy.ops.render.render(write_still=True)
