"""The look, measured off the generated plate rather than chosen.

tools/hero-gen/out/finger-test.png is the target. What the measurements say
about it, and what each one forces here:

  58.2% of the picture is below V 0.18 and only 6.4% is above V 0.60.
  So there is one light that matters and almost no fill. Shadow is the
  default state of the frame, not an accident at the edges.

  49,421 distinct colours, with smooth falloff inside hard-edged shapes.
  So this is NOT a posterised cel look, and the toon path in common.py is the
  wrong tool: the flatness is in the GEOMETRY, which is faceted and low-poly,
  not in the shader. Everything here is flat-shaded on purpose.

  Terracotta reads H20 S0.50 V0.30, a dark brown, where common.py carries a
  saturated H8 S0.85 red that was picked for a photoreal render through AgX.
  The lit leaf is V 0.19 and the shadow leaf V 0.09, both far darker than a
  lifted palette would give.

Colours below are written in sRGB hex because that is what the measurements
are in, and converted once on the way to Blender, which wants linear.
"""
import bpy, math
from mathutils import Vector

def srgb(h, a=1.0):
    """sRGB hex to the linear float tuple Blender expects."""
    v = [int(h[i:i+2], 16) / 255 for i in (1, 3, 5)]
    f = lambda c: c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return (f(v[0]), f(v[1]), f(v[2]), a)

# measured off the plate
TERRA      = srgb('#6F4836')   # pot body, reads #AD723B lit and #3D271E in shade
TERRA_DARK = srgb('#6B3F28')   # the shaded half of the pot, a separate facet colour
SOIL       = srgb('#3A2A20')
SOIL_LIT   = srgb('#6B5038')
LEAF       = srgb('#2E3A26')   # dark enough that lit it lands near the measured #273022
STEM       = srgb('#35422B')
SKIN       = srgb('#C08A5E')
WALL       = srgb('#2A2420')
SILL       = srgb('#4A3A2C')
FRAME      = srgb('#241C16')
SKY        = srgb('#223040')   # night through the glass
LAMP       = (1.0, 0.72, 0.45)
MOON       = (0.55, 0.66, 0.88)


def reset(res=(836, 470), samples=96):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'
    sc.cycles.device = 'GPU'
    p = bpy.context.preferences.addons['cycles'].preferences
    p.compute_device_type = 'CUDA'
    for d in p.get_devices_for_type('CUDA'):
        d.use = (d.type == 'CUDA')
    sc.cycles.samples = samples
    sc.cycles.use_denoising = True
    # one small bright source in a dark room is the exact setup that fireflies
    sc.cycles.sample_clamp_indirect = 3.0
    sc.render.resolution_x, sc.render.resolution_y = res
    sc.render.image_settings.file_format = 'PNG'
    # AgX lifts and desaturates its own shadows, which is wrong here: the plate
    # has a hard floor at V 0.08 and 58% of the frame sitting on it. Standard
    # keeps the values where the lighting puts them.
    sc.view_settings.view_transform = 'Standard'
    sc.view_settings.look = 'None'
    w = bpy.data.worlds.new('w'); sc.world = w; w.use_nodes = True
    # 54% of the plate sits in a narrow band at V 0.10 to 0.20 and only 9.5%
    # falls below 0.1, so its shadows have a floor. That floor is ambient, not a
    # fill light: a second lamp would put highlights where the plate has none.
    w.node_tree.nodes['Background'].inputs[0].default_value = srgb('#39414E')
    w.node_tree.nodes['Background'].inputs[1].default_value = 0.16
    return sc


def mat(name, base, rough=0.75, emit=None, strength=1.0):
    m = bpy.data.materials.new(name); m.use_nodes = True
    b = m.node_tree.nodes['Principled BSDF']
    b.inputs['Base Color'].default_value = base
    b.inputs['Roughness'].default_value = rough
    b.inputs['Specular IOR Level'].default_value = 0.18
    if emit:
        b.inputs['Emission Color'].default_value = emit
        b.inputs['Emission Strength'].default_value = strength
    return m


def put(obj, m, smooth=False):
    """Flat by default. The plate's flatness is faceted geometry, so smoothing
    a 16-sided pot would erase the single thing that makes it read as drawn."""
    obj.data.materials.append(m)
    for poly in obj.data.polygons:
        poly.use_smooth = smooth
    return obj


def aim(obj, target):
    d = Vector(target) - obj.location
    obj.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()
    return obj


def camera(loc, at, lens=50, fstop=None):
    bpy.ops.object.camera_add(location=loc)
    cam = aim(bpy.context.object, at)
    cam.data.lens = lens
    bpy.context.scene.camera = cam
    if fstop:
        cam.data.dof.use_dof = True
        cam.data.dof.focus_distance = (Vector(at) - Vector(loc)).length
        cam.data.dof.aperture_fstop = fstop
    return cam


def frame_at(cam, depth_y):
    """Width and height the frame covers on the plane y = depth_y."""
    sc = bpy.context.scene
    d = (depth_y - cam.location.y) / (cam.matrix_world.to_quaternion() @ Vector((0, 0, -1))).y
    sw = cam.data.sensor_width
    w = d * sw / cam.data.lens
    return w, w * sc.render.resolution_y / sc.render.resolution_x, d


def place_at_frame(cam, fx, fy, depth_y):
    """World position of a point that lands at frame fraction (fx, fy) on the
    plane y = depth_y, with (0,0) top-left. Set dressing in a tight shot is the
    one thing that cannot be placed by eye: this frame is 35 cm wide, so an
    object put where it 'looks about right' in metres lands outside it."""
    fwd = cam.matrix_world.to_quaternion() @ Vector((0, 0, -1))
    right = cam.matrix_world.to_quaternion() @ Vector((1, 0, 0))
    up = cam.matrix_world.to_quaternion() @ Vector((0, 1, 0))
    w, h, d = frame_at(cam, depth_y)
    centre = cam.location + fwd * d
    return centre + right * ((fx - 0.5) * w) + up * ((0.5 - fy) * h)
