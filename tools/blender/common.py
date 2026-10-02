"""Shared setup for the ThirstTrap hero renders.

The palette is lifted from the app's Theme.kt so the page, the app and the
render agree. Everything is built from primitives in script rather than from a
.blend, because a .blend is a binary nobody can review and this scene is small
enough that the code IS the asset.
"""
import bpy, math
from mathutils import Vector

PAPER      = (0.93, 0.89, 0.80, 1)
INK        = (0.045, 0.040, 0.030, 1)
LEAF       = (0.055, 0.145, 0.072, 1)
LEAF_LIT   = (0.105, 0.245, 0.128, 1)
TERRACOTTA = (0.300, 0.075, 0.045, 1)
SOIL       = (0.030, 0.021, 0.013, 1)
SKIN       = (0.68, 0.46, 0.31, 1)
LAMP       = (1.0, 0.68, 0.38)
MOON       = (0.62, 0.70, 0.86)


FLAT = False


def reset(res=(1280, 720), samples=128, flat=False):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'
    sc.cycles.device = 'GPU'
    prefs = bpy.context.preferences.addons['cycles'].preferences
    prefs.compute_device_type = 'CUDA'
    for d in prefs.get_devices_for_type('CUDA'):
        d.use = (d.type == 'CUDA')
    sc.cycles.samples = samples
    sc.cycles.use_denoising = True
    # Clamping the indirect bounces keeps fireflies out of a scene that is
    # mostly dark with one small bright source, which is exactly the setup
    # that produces them.
    sc.cycles.sample_clamp_indirect = 4.0
    sc.render.resolution_x, sc.render.resolution_y = res
    sc.render.image_settings.file_format = 'PNG'
    global FLAT
    FLAT = flat
    if flat:
        # Flat illustration has no tone curve and no bokeh: colours are the
        # colours, and a blurred foreground reads as a mistake rather than a
        # lens. EEVEE's toon ramps are unavailable here (no GPU context under
        # WSL), so the two-tone comes from Cycles' own Toon BSDF instead.
        sc.view_settings.view_transform = 'Standard'
        sc.view_settings.look = 'None'
    else:
        sc.view_settings.view_transform = 'AgX'
        sc.view_settings.look = 'AgX - Base Contrast'
    return sc


def mat(name, base, rough=0.6, emit=None, emit_strength=2.5, metallic=0.0):
    if FLAT:
        return _flat_mat(name, base, emit, emit_strength)
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    b = m.node_tree.nodes['Principled BSDF']
    b.inputs['Base Color'].default_value = base
    b.inputs['Roughness'].default_value = rough
    b.inputs['Metallic'].default_value = metallic
    if emit:
        b.inputs['Emission Color'].default_value = emit
        b.inputs['Emission Strength'].default_value = emit_strength
    return m


def put(obj, mat_, shade_smooth=True, subsurf=0):
    obj.data.materials.append(mat_)
    if shade_smooth:
        for p in obj.data.polygons:
            p.use_smooth = True
    if subsurf:
        m = obj.modifiers.new('sub', 'SUBSURF')
        m.levels = min(subsurf, 2)
        m.render_levels = subsurf
    return obj


def aim(obj, target):
    """Point any object's -Z at a target. Lights and cameras both face -Z."""
    d = Vector(target) - obj.location
    obj.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()
    return obj


def look_at(cam, target, lens=50):
    aim(cam, target)
    cam.data.lens = lens
    return cam


def flat_lift(c):
    """Pull a colour into illustration range.

    The palette was picked for a photoreal render through AgX, which lifts its
    own shadows. Flat shading runs on the Standard transform, which does not,
    so the same values come out crushed to near black. This is the one curve
    that maps one intent onto the other instead of keeping two palettes in
    sync by hand.
    """
    lifted = [0.06 + 0.75 * (max(v, 0.0) ** 0.45) for v in c[:3]]
    # Lifting every channel also drags them together, which is why terracotta
    # came out as dusty pink. Push the chroma back out around the new value.
    g = sum(lifted) / 3.0
    out = [min(1.0, max(0.0, g + (v - g) * 1.55)) for v in lifted]
    return tuple(out + list(c[3:]))


def _flat_mat(name, base, emit=None, emit_strength=2.5, size=0.58):
    """Two-tone cel shading: one lit value, one shadow value, a hard edge.

    Toon BSDF with Smooth at zero is the only way to get a hard terminator in
    Cycles, since Shader to RGB is an EEVEE node and EEVEE cannot open a GPU
    context in this environment.
    """
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    # look the output node up by TYPE: its socket names are not stable enough
    # to index by string across versions and locales
    for x in list(nt.nodes):
        if x.type != 'OUTPUT_MATERIAL':
            nt.nodes.remove(x)
    # re-fetch AFTER the removals: mutating the collection invalidates any
    # node pointer taken before it, and using a stale one segfaults Blender
    out = next(x for x in nt.nodes if x.type == 'OUTPUT_MATERIAL')
    if emit:
        e = nt.nodes.new('ShaderNodeEmission')
        e.inputs[0].default_value = emit
        e.inputs[1].default_value = emit_strength
        nt.links.new(e.outputs[0], out.inputs[0])
    else:
        t = nt.nodes.new('ShaderNodeBsdfToon')
        t.component = 'DIFFUSE'
        t.inputs[0].default_value = flat_lift(base)
        t.inputs[1].default_value = size
        t.inputs[2].default_value = 0.0
        nt.links.new(t.outputs[0], out.inputs[0])
    return m
