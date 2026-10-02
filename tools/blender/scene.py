"""The whole ThirstTrap hero set: one room, a night window with a sill, and a
table under a lamp.

Both shots live in the same room so the light is consistent between them. The
pot is built twice, once on the sill and once on the scale, because the story
cuts between them rather than carrying the pot across.
"""
import bpy, math, os
from mathutils import Vector
from common import (reset, mat, put, look_at, PAPER, INK, LEAF, TERRACOTTA,
                    LAMP, MOON, SKIN)
from parts import build_pot, build_plant, dress_soil, build_finger, tube

FONT_DIR = 'core/ui/src/main/res/font'
_fonts = {}


def font(name):
    """The app's own typefaces, so the screen in the render is the real thing."""
    if name not in _fonts:
        p = name if os.path.isabs(name) else os.path.join(FONT_DIR, name)
        _fonts[name] = bpy.data.fonts.load(p) if os.path.exists(p) else None
    return _fonts[name]


MONO = '/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf'


def text(body, loc, size, mat_, fnt='hanken_grotesk.ttf', align='LEFT'):
    """A flat text object in one of the app's typefaces.

    The font is assigned BEFORE the body. A new text object is born holding the
    word "Text" in Blender's built-in face, and its per-character formatting
    survives a later body change, so the first glyph of every string kept
    rendering in the wrong font and came out as a dark blob.
    """
    bpy.ops.object.text_add(location=loc)
    o = bpy.context.object
    f = font(fnt)
    if f:
        o.data.font = f
        o.data.font_bold = o.data.font_italic = o.data.font_bold_italic = f
    # KNOWN ARTIFACT: Blender tessellates a spurious wedge from the text
    # origin to the first glyph's contour. Ruled out z-fighting, denoising,
    # extrude, depth, per-character formatting and overlapping geometry; it
    # tracks the first DRAWN glyph, so a leading space does not absorb it.
    # Confined to the phone screen in the closing frames, which crossfade to
    # the real HTML list, so it is left alone rather than worked around badly.
    o.data.body = body
    o.data.size = size
    o.data.align_x = align
    o.data.materials.append(mat_)
    return o


# ---------------------------------------------------------------- room

def build_room():
    """Back wall with a window cut into it, a floor, and a side wall.

    The window is four planes rather than a boolean: a boolean on a wall this
    simple is a lot of risk (n-gons, flipped normals) for geometry that is four
    rectangles.
    """
    wall_m = mat('wall', (0.030, 0.027, 0.022, 1), 0.94)
    floor_m = mat('floor', (0.022, 0.019, 0.015, 1), 0.88)
    WY, WX0, WX1, WZ0, WZ1 = 1.20, -0.62, 0.72, 0.94, 1.96

    def panel(x0, x1, z0, z1, name):
        bpy.ops.mesh.primitive_plane_add(size=1, location=((x0 + x1) / 2, WY, (z0 + z1) / 2))
        o = bpy.context.object
        o.name = name
        o.rotation_euler = (math.pi / 2, 0, 0)
        o.scale = ((x1 - x0), (z1 - z0), 1)
        return put(o, wall_m, shade_smooth=False)

    panel(-2.6, WX0, 0.0, 2.7, 'wall_l')
    panel(WX1, 2.6, 0.0, 2.7, 'wall_r')
    panel(WX0, WX1, 0.0, WZ0, 'wall_b')
    panel(WX0, WX1, WZ1, 2.7, 'wall_t')

    bpy.ops.mesh.primitive_plane_add(size=1, location=(0, WY / 2 - 1.5, 0))
    fl = bpy.context.object
    fl.scale = (6.0, 3.0 + WY, 1)
    put(fl, floor_m, shade_smooth=False)

    # window frame and one mullion
    fr = mat('frame', (0.040, 0.035, 0.027, 1), 0.72)
    for (x0, x1, z0, z1) in [(WX0, WX1, WZ0, WZ0 + 0.035), (WX0, WX1, WZ1 - 0.035, WZ1),
                             (WX0, WX0 + 0.035, WZ0, WZ1), (WX1 - 0.035, WX1, WZ0, WZ1),
                             (0.03, 0.065, WZ0, WZ1)]:
        bpy.ops.mesh.primitive_cube_add(size=1,
                                        location=((x0 + x1) / 2, WY - 0.02, (z0 + z1) / 2))
        o = bpy.context.object
        o.scale = ((x1 - x0), 0.05, (z1 - z0))
        put(o, fr, shade_smooth=False)

    # night outside: a dim emissive backdrop and a moon
    sky = mat('sky', (0.012, 0.016, 0.030, 1), 1.0,
              emit=(0.030, 0.042, 0.080, 1), emit_strength=1.6)
    bpy.ops.mesh.primitive_plane_add(size=1, location=(0, 4.0, 1.4))
    o = bpy.context.object
    o.rotation_euler = (math.pi / 2, 0, 0)
    o.scale = (9, 6, 1)
    put(o, sky, shade_smooth=False)
    o.visible_diffuse = o.visible_glossy = False

    bpy.ops.mesh.primitive_uv_sphere_add(segments=48, ring_count=24, radius=0.165,
                                         location=(-0.30, 3.60, 1.78))
    put(bpy.context.object, mat('moon', (0.9, 0.93, 1.0, 1), 1.0,
                                emit=(0.86, 0.90, 1.0, 1), emit_strength=11.0))

    # the sill the pot stands on
    bpy.ops.mesh.primitive_cube_add(size=1, location=((WX0 + WX1) / 2, WY - 0.105, WZ0 - 0.012))
    o = bpy.context.object
    o.scale = ((WX1 - WX0) + 0.10, 0.23, 0.024)
    put(o, mat('sill', (0.042, 0.036, 0.027, 1), 0.78), shade_smooth=False)
    return dict(window=(WX0, WX1, WZ0, WZ1), wall_y=WY, sill_z=WZ0)


# ---------------------------------------------------------------- props

def build_scale(loc):
    """A kitchen scale with a lit readout. The number is the whole point of the
    second shot, so it is real text rather than a texture."""
    casing = mat('casing', (0.42, 0.40, 0.35, 1), 0.38)
    plate = mat('plate', (0.72, 0.70, 0.62, 1), 0.28, metallic=0.35)
    x, y, z = loc
    bpy.ops.mesh.primitive_cube_add(size=1, location=(x, y, z + 0.016))
    base = bpy.context.object
    base.scale = (0.175, 0.145, 0.032)
    b = base.modifiers.new('bev', 'BEVEL'); b.width, b.segments = 0.004, 3
    put(base, casing)

    bpy.ops.mesh.primitive_cube_add(size=1, location=(x, y - 0.004, z + 0.037))
    top = bpy.context.object
    top.scale = (0.168, 0.132, 0.008)
    t = top.modifiers.new('bev', 'BEVEL'); t.width, t.segments = 0.003, 3
    put(top, plate)

    # the display, recessed into the front lip
    bpy.ops.mesh.primitive_plane_add(size=1, location=(x, y - 0.0885, z + 0.018))
    d = bpy.context.object
    d.rotation_euler = (math.pi / 2, 0, 0)
    d.scale = (0.082, 0.022, 1)
    put(d, mat('lcd', (0.012, 0.014, 0.012, 1), 0.3), shade_smooth=False)

    readout = text('218 g', (x, y - 0.0895, z + 0.0115), 0.015,
                   mat('lcdtext', (0.1, 0.9, 0.4, 1), 0.5,
                       emit=(0.25, 1.0, 0.45, 1), emit_strength=5.0),
                   fnt=MONO, align='CENTER')
    readout.rotation_euler = (math.pi / 2, 0, 0)
    return readout


def build_phone(loc, rot_z=-0.42):
    """Phone, propped, showing the plant list in the app's own typefaces."""
    body_m = mat('phonebody', (0.030, 0.028, 0.024, 1), 0.22)
    paper_m = mat('paper', PAPER, 0.62, emit=PAPER, emit_strength=3.4)
    ink_m = mat('ink', INK, 0.8)
    mid_m = mat('midink', (0.28, 0.26, 0.21, 1), 0.8)
    terr_m = mat('terrtext', (0.42, 0.14, 0.09, 1), 0.8)
    rule_m = mat('rule', (0.72, 0.68, 0.58, 1), 0.9)

    W, H, TILT = 0.0715, 0.1480, 0.30
    bpy.ops.object.empty_add(location=loc)
    root = bpy.context.object
    root.name = 'phone'
    root.rotation_euler = (TILT, 0, rot_z)

    bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 0))
    shell = bpy.context.object
    shell.scale = (W + 0.006, 0.0085, H + 0.006)
    bv = shell.modifiers.new('bev', 'BEVEL'); bv.width, bv.segments = 0.004, 4
    put(shell, body_m)
    shell.parent = root

    def panel(w, h, px, pz, m, depth=-0.0050):
        bpy.ops.mesh.primitive_plane_add(size=1, location=(px, depth, pz))
        o = bpy.context.object
        o.rotation_euler = (math.pi / 2, 0, 0)
        o.scale = (w, h, 1)
        put(o, m, shade_smooth=False)
        o.parent = root
        return o

    panel(W, H, 0, 0, paper_m, depth=-0.0046)

    def label(body, px, pz, size, m, fnt='hanken_grotesk.ttf'):
        o = text(body, (px, -0.0078, pz), size, m, fnt=fnt)
        o.rotation_euler = (math.pi / 2, 0, 0)
        o.parent = root
        return o

    left = -W / 2 + 0.006
    label('PLANTS', left, H / 2 - 0.016, 0.0050, mid_m)
    panel(W - 0.012, 0.0004, 0, H / 2 - 0.022, rule_m)

    rows = [('Fittonia', 'Watered today', mid_m),
            ('Peperomia', 'Needs water now', terr_m),
            ('Creeping fig', 'Water in about 2 days', mid_m),
            ('Flax seeds', 'This one has gone', mid_m)]
    top = H / 2 - 0.030
    for i, (name, sub, subm) in enumerate(rows):
        z0 = top - i * 0.0225
        panel(0.0115, 0.0115, left + 0.0058, z0 - 0.0045, mat(f'thumb{i}', (0.80, 0.76, 0.66, 1), 0.85))
        label(name, left + 0.0155, z0 - 0.002, 0.0062, ink_m, fnt='newsreader.ttf')
        label(sub, left + 0.0155, z0 - 0.0092, 0.0044, subm)
        if i < len(rows) - 1:
            panel(W - 0.012, 0.0003, 0, z0 - 0.0135, rule_m)
    return root


def build_figure(loc=(0.92, 0.46, 0.0), facing=-1.9, reach_to=None):
    """Dheirav, as a silhouette. He is backlit by the lamp and never in focus,
    so he is built from a handful of swept tubes. A detailed figure here would
    be work nobody can see."""
    dark = mat('figure', (0.018, 0.016, 0.013, 1), 0.92)
    x, y, z = loc
    parts = [
        tube([(x, y, z + 0.02), (x, y, z + 0.46), (x - 0.01, y, z + 0.92)],
             [0.075, 0.085, 0.080], 'legs', seg=20),
        tube([(x - 0.01, y, z + 0.90), (x - 0.02, y - 0.02, z + 1.18), (x - 0.03, y - 0.03, z + 1.40)],
             [0.105, 0.125, 0.112], 'torso', seg=24),
        tube([(x - 0.03, y - 0.03, z + 1.42), (x - 0.03, y - 0.03, z + 1.58)],
             [0.048, 0.050, ], 'neck', seg=16),
    ]
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=16, radius=0.098,
                                         location=(x - 0.035, y - 0.035, z + 1.66))
    parts.append(bpy.context.object)
    for p in parts:
        put(p, dark)
    if reach_to is not None:
        # shoulder -> elbow -> wrist, aimed at wherever the finger ended up.
        # The elbow is placed outward and low so the arm reads as reaching
        # down into the pot rather than pointing at it.
        sh = Vector((x - 0.03, y - 0.03, z + 1.34))
        wr = Vector(reach_to)
        mid = (sh + wr) * 0.5
        out = Vector((wr.y - sh.y, sh.x - wr.x, 0))
        out = out.normalized() * 0.11 if out.length > 1e-6 else Vector((0.11, 0, 0))
        elbow = mid + out + Vector((0, 0, -0.10))
        arm = tube([tuple(sh), tuple(sh + (elbow - sh) * 0.55), tuple(elbow),
                    tuple(elbow + (wr - elbow) * 0.55), tuple(wr)],
                   [0.062, 0.050, 0.044, 0.038, 0.034], 'arm', seg=22)
        put(arm, dark)
        parts.append(arm)

    bpy.ops.object.empty_add(location=(x, y, z))
    root = bpy.context.object
    root.name = 'figure'
    for p in parts:
        p.parent = root
    if reach_to is None:
        root.rotation_euler = (0, 0, facing)
    return root


def build_arm(wrist, from_point, skin=None):
    """A forearm entering frame, with no body attached to it.

    The figure this used to belong to read as a shop mannequin, for the same
    reason the scripted hand did: a human body is not a thing you get out of
    swept tubes. An arm coming in from outside the frame implies the person
    without asking the geometry to depict one, which is also exactly how the
    drawn version of this shot worked.
    """
    skin = skin or mat('armskin', SKIN, 0.58)
    w, f = Vector(wrist), Vector(from_point)
    d = f - w
    # a slight bow in the forearm, so it is not a straight pipe
    side = Vector((-d.y, d.x, 0))
    side = side.normalized() * 0.022 if side.length > 1e-6 else Vector((0, 0.022, 0))
    spine = [tuple(w),
             tuple(w + d * 0.22 + side * 0.5 + Vector((0, 0, 0.004))),
             tuple(w + d * 0.50 + side),
             tuple(w + d * 0.78 + side * 0.6),
             tuple(f)]
    radii = [0.0200, 0.0245, 0.0310, 0.0385, 0.0460]
    return put(tube(spine, radii, 'forearm', seg=28), skin)
