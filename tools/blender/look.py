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
import bpy, math, os
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
    nt = w.node_tree
    bg = nt.nodes['Background']
    # 54% of the plate sits in a narrow band at V 0.10 to 0.20 and only 9.5%
    # falls below 0.1, so its shadows have a floor. That floor is ambient, not a
    # fill light: a second lamp would put highlights where the plate has none.
    #
    # It is an HDRI rather than a flat colour, and the reason is measurable.
    # Comparing the median local variation of 8x8 blocks at three scales, the
    # plates climb as you zoom out, 0.90 to 2.13 and 1.54 to 3.49, while the
    # renders sat flat at 0.50 at every scale. The plates' surfaces shade across
    # their own width; the renders' did not, because a single flat world colour
    # lights every facet of a wall identically no matter which way it faces.
    #
    # Dim on purpose. This is ambient with direction, not a second light: the
    # lamp still does the lighting.
    env = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       'assets', 'hdri', 'small_empty_room_1_1k.hdr')
    if os.path.exists(env):
        tex = nt.nodes.new('ShaderNodeTexEnvironment')
        tex.image = bpy.data.images.load(env)
        nt.links.new(tex.outputs['Color'], bg.inputs['Color'])
        bg.inputs[1].default_value = 0.22
    else:
        bg.inputs[0].default_value = srgb('#39414E')
        bg.inputs[1].default_value = 0.16
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


def leaf_mat(name, base, alpha_png, rough=0.66):
    """Flat leaf colour that keeps the asset's alpha cutout.

    The broad-leaf assets build each leaf as a card with an alpha map doing the
    shape. Replacing their material wholesale, which is what every other prop
    here gets, turns every leaf into a solid rectangle. So this one keeps the
    one texture that is geometry in disguise and throws away the rest.
    """
    import os
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    b = nt.nodes['Principled BSDF']
    b.inputs['Base Color'].default_value = base
    b.inputs['Roughness'].default_value = rough
    b.inputs['Specular IOR Level'].default_value = 0.15
    if alpha_png and os.path.exists(alpha_png):
        img = nt.nodes.new('ShaderNodeTexImage')
        img.image = bpy.data.images.load(alpha_png)
        img.image.colorspace_settings.name = 'Non-Color'
        img.interpolation = 'Closest'
        nt.links.new(img.outputs['Color'], b.inputs['Alpha'])
        m.blend_method = 'CLIP' if hasattr(m, 'blend_method') else m.blend_method
    return m


TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets', 'tex')


def textured(name, base, tex_file, strength=0.35, scale=2.0, rough=0.85):
    """A flat colour broken up by a texture, not replaced by one.

    Measuring the plates against the renders at three zoom levels showed the
    plates' within-region variation CLIMBING as you zoom out, 0.90 to 2.13,
    while the renders stayed flat at 0.50 at every scale. Their surfaces shade
    across their own width and ours did not.

    Lighting cannot fix that here. An HDRI world was tried first and changed the
    numbers not at all, because this is an enclosed room and the world only
    reaches through the window. A physically lit flat-albedo wall genuinely is
    uniform; the plates' modulation is painted, not lit.

    So the variation has to live in the albedo. The texture is mixed only
    [strength] of the way toward the flat colour, because the look is flat
    illustration and a photoreal plank would be a different app's render. It is
    there to stop a surface being one number, not to be noticed.
    """
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    b = nt.nodes['Principled BSDF']
    b.inputs['Roughness'].default_value = rough
    b.inputs['Specular IOR Level'].default_value = 0.15
    path = os.path.join(TEX, tex_file)
    if not os.path.exists(path):
        b.inputs['Base Color'].default_value = base
        return m
    img = nt.nodes.new('ShaderNodeTexImage')
    img.image = bpy.data.images.load(path)
    mapping = nt.nodes.new('ShaderNodeMapping')
    mapping.inputs['Scale'].default_value = (scale, scale, scale)
    coord = nt.nodes.new('ShaderNodeTexCoord')
    nt.links.new(coord.outputs['Object'], mapping.inputs['Vector'])
    nt.links.new(mapping.outputs['Vector'], img.inputs['Vector'])
    # Modulate AROUND the base, do not multiply it down.
    #
    # The first attempt multiplied the texture into the albedo, and nothing
    # showed. These colours are nearly black: #3B2919 is about 0.04 in linear,
    # so base * texture varies between 0 and 0.04, which is a couple of 8-bit
    # levels. The texture has to drive a mix between a darker and a lighter
    # version of the colour instead, which gives real variation at the value the
    # surface actually sits at.
    bw = nt.nodes.new('ShaderNodeRGBToBW')
    nt.links.new(img.outputs['Color'], bw.inputs['Color'])
    lo = tuple(c * (1.0 - strength) for c in base[:3]) + (1.0,)
    hi = tuple(min(1.0, c * (1.0 + strength * 2.2)) for c in base[:3]) + (1.0,)
    mix = nt.nodes.new('ShaderNodeMixRGB')
    mix.blend_type = 'MIX'
    mix.inputs['Color1'].default_value = lo
    mix.inputs['Color2'].default_value = hi
    nt.links.new(bw.outputs['Val'], mix.inputs['Fac'])
    nt.links.new(mix.outputs['Color'], b.inputs['Base Color'])
    return m
