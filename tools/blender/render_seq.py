"""Render the hero sequence to a PNG sequence.

  blender -b --factory-startup --python tools/blender/render_seq.py -- \
      OUT_DIR START END WIDTH HEIGHT SAMPLES
"""
import bpy, sys, os
sys.path.insert(0, os.path.join(os.getcwd(), 'tools/blender'))
from build import build_all
from animate import (animate_finger, animate_dimple, animate_camera,
                     animate_readout, F_END)

a = sys.argv[sys.argv.index('--') + 1:]
out_dir = a[0]
start, end = int(a[1]), int(a[2])
W, H_, samples = int(a[3]), int(a[4]), int(a[5])
# Portrait renders put the 36 mm sensor on the vertical, so the same lens sees
# far less horizontally. Scaling every focal length keeps the subject framed.
lens_scale = float(a[6]) if len(a) > 6 else 1.0
flat = (len(a) > 7 and a[7] == 'flat')

sc, H = build_all(samples=samples, res=(W, H_), flat=flat)
animate_finger(H['finger'])
animate_dimple(H['sill_soil'])
cam = animate_camera(sc, phone=H['phone'])
if lens_scale != 1.0 and cam.data.animation_data and cam.data.animation_data.action:
    for fc in cam.data.animation_data.action.fcurves:
        if fc.data_path == 'lens':
            for k in fc.keyframe_points:
                k.co.y *= lens_scale
                k.handle_left.y *= lens_scale
                k.handle_right.y *= lens_scale
animate_readout(H['readout'])

sc.frame_start, sc.frame_end = start, end
sc.render.filepath = os.path.join(out_dir, 'f_')
sc.render.image_settings.file_format = 'PNG'
sc.render.image_settings.color_mode = 'RGB'
sc.render.use_overwrite = True
print(f'[seq] frames {start}..{end} of {F_END} at {W}x{H_}, {samples} samples')
bpy.ops.render.render(animation=True)
