"""Rebuild tools/hero-gen/out/finger-test.png as geometry.

Run:
  ~/opt/blender-4.5.14-linux-x64/blender -b --python tools/blender/match_ref.py -- OUT.png [samples]

Composition is taken from the plate: pot centred and about 43% of the frame
wide, rim a little under half way up, window and moon behind to the left, the
lamp hard to the right at x=0.95 of frame, hand entering from the right with
one finger in the soil.
"""
import bpy, sys, math, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look, parts, scene as sc_mod
from mathutils import Vector

# parts.py was written against the photoreal palette; point it at the measured one
parts.SOIL, parts.SKIN = look.SOIL, look.SKIN
parts.TERRACOTTA, parts.LEAF, parts.LEAF_LIT = look.TERRA, look.LEAF, look.LEAF
sc_mod.mat, sc_mod.put = look.mat, (lambda o, m, shade_smooth=True, subsurf=0: look.put(o, m, smooth=False))
parts.mat, parts.put = look.mat, (lambda o, m, shade_smooth=True, subsurf=0: look.put(o, m, smooth=False))


def facet_pot(loc=(0, 0, 0), top_r=0.075, bot_r=0.058, h=0.130, sides=16):
    """A 16-sided pot, flat shaded. The plate's pot is visibly faceted, and that
    faceting is what makes it read as drawn rather than rendered: smoothing it
    would be the single change that breaks the look."""
    body_m = look.mat('terra', look.TERRA, 0.80)
    bpy.ops.mesh.primitive_cone_add(vertices=sides, radius1=bot_r, radius2=top_r, depth=h,
                                    end_fill_type='NOTHING', location=(loc[0], loc[1], loc[2] + h / 2))
    body = look.put(bpy.context.object, body_m)
    s = body.modifiers.new('s', 'SOLIDIFY'); s.thickness = 0.006; s.offset = -1.0
    bpy.ops.mesh.primitive_circle_add(vertices=sides, radius=bot_r, fill_type='NGON',
                                      location=(loc[0], loc[1], loc[2] + 0.003))
    look.put(bpy.context.object, body_m)
    # the rim is a short wider band, not a torus: a torus reads as a smooth
    # highlight ring and the plate's rim is a flat chamfered lip
    bpy.ops.mesh.primitive_cone_add(vertices=sides, radius1=top_r + 0.004, radius2=top_r + 0.006,
                                    depth=0.016, end_fill_type='NOTHING',
                                    location=(loc[0], loc[1], loc[2] + h - 0.004))
    rim = look.put(bpy.context.object, body_m)
    r2 = rim.modifiers.new('s', 'SOLIDIFY'); r2.thickness = 0.007; r2.offset = -1.0
    soil = parts._soil_grid(loc[0], loc[1], loc[2] + h - 0.030, top_r - 0.010, n=96)
    look.put(soil, look.mat('soil', look.SOIL, 0.95), smooth=True)
    return body, rim, soil


def broad_leaf(mat_, base, aim_xy, length=0.17, width=0.060, rise=0.9, droop=0.35):
    """Few, broad, folded planes. The plate's leaves are flat polygons with one
    crease down the middle, not subdivided surfaces."""
    ax, ay = aim_xy
    n = math.hypot(ax, ay) or 1.0
    ax, ay = ax / n, ay / n
    px, py = -ay, ax
    pts, faces = [], []
    SEG = 4
    for i in range(SEG + 1):
        t = i / SEG
        # one crease: the centre line rides higher than the two edges
        z = base[2] + rise * length * t - droop * length * t * t
        cx, cy = base[0] + ax * length * t, base[1] + ay * length * t
        w = width * math.sin(math.pi * min(t * 1.15, 1.0)) * 0.5
        pts += [(cx + px * w, cy + py * w, z - 0.004 * (1 - t)),
                (cx, cy, z + 0.006 * math.sin(math.pi * t)),
                (cx - px * w, cy - py * w, z - 0.004 * (1 - t))]
    for i in range(SEG):
        a = i * 3
        faces += [(a, a + 1, a + 4, a + 3), (a + 1, a + 2, a + 5, a + 4)]
    me = bpy.data.meshes.new('leaf'); me.from_pydata(pts, [], faces); me.update()
    ob = bpy.data.objects.new('leaf', me); bpy.context.collection.objects.link(ob)
    sol = ob.modifiers.new('s', 'SOLIDIFY'); sol.thickness = 0.0016
    return look.put(ob, mat_)


def build():
    sc = look.reset(res=(1672, 941), samples=int(sys.argv[sys.argv.index('--') + 2])
                    if len(sys.argv) > sys.argv.index('--') + 2 else 96)

    # The camera is built first: this frame is 35 cm wide, and every piece of
    # set dressing below is placed by where it lands IN it rather than by a
    # guess in metres. The first attempt put the moon 0.9 m up, which is four
    # frame-heights above the top of the picture.
    POT_D, POT_FRAC, LENS, TILT = 0.162, 0.40, 50.0, math.radians(35)
    d = (POT_D / POT_FRAC) * LENS / 36.0
    cam = look.camera((0.075, -d * math.cos(TILT) * 0.995, 0.104 + d * math.sin(TILT)),
                      (0.0, 0.0, 0.104), lens=LENS)
    at = lambda fx, fy, y: look.place_at_frame(cam, fx, fy, y)

    wall_m = look.mat('wall', look.WALL, 0.92)
    sill_m = look.mat('sill', look.SILL, 0.85)
    frame_m = look.mat('frame', look.FRAME, 0.8)

    # --- the sill the pot stands on
    bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0.015, -0.026))
    s = bpy.context.object; s.scale = (2.2, 0.150, 0.052); look.put(s, look.mat('sill', look.srgb('#2E241B'), 0.9))
    # --- the wall the lamp is on, to camera right, and the one behind
    bpy.ops.mesh.primitive_cube_add(size=1, location=(0.265, 0.12, 0.30))
    w = bpy.context.object; w.scale = (0.05, 0.62, 1.1); look.put(w, wall_m)
    # --- the window: a dim emitting plane so the night reads as light, not paint
    sky = look.mat('sky', look.SKY, 0.9, emit=look.srgb('#2A3645'), strength=0.30)
    gc = at(0.5, 0.5, 0.215)
    bpy.ops.mesh.primitive_plane_add(size=1, location=(gc.x, 0.215, gc.z))
    g = bpy.context.object; g.rotation_euler = (math.radians(90), 0, 0)
    g.scale = (1.3, 1.0, 0.95); look.put(g, sky)
    for fx, fy, wfr in ((0.03, 0.56, 0.11), (0.12, 0.49, 0.09), (0.22, 0.58, 0.08),
                        (0.31, 0.52, 0.10), (0.40, 0.60, 0.07)):
        c = at(fx, fy, 0.212); fw, _, _ = look.frame_at(cam, 0.212)
        bpy.ops.mesh.primitive_cube_add(size=1, location=(c.x, 0.212, c.z))
        b = bpy.context.object; b.scale = (fw * wfr, 0.01, abs(c.z) * 0.9 + 0.12)
        look.put(b, frame_m)
    mc = at(0.145, 0.105, 0.210)
    bpy.ops.mesh.primitive_circle_add(vertices=28, radius=0.015, fill_type='NGON',
                                      location=(mc.x, 0.210, mc.z))
    m = bpy.context.object; m.rotation_euler = (math.radians(90), 0, 0)
    look.put(m, look.mat('moon', (1, 1, 1, 1), 1.0, emit=(0.95, 0.96, 1.0, 1), strength=3.2))
    for fx in (0.46, -0.02):
        c = at(fx, 0.5, 0.211)
        bpy.ops.mesh.primitive_cube_add(size=1, location=(c.x, 0.211, c.z))
        b = bpy.context.object; b.scale = (0.012, 0.02, 0.8); look.put(b, frame_m)

    # --- the pot
    body, rim, soil = facet_pot()
    parts.dress_soil(soil, dimple_at=(0.016, -0.004), dimple_r=0.022, dimple_d=0.009)
    leaf_m = look.mat('leaf', look.LEAF, 0.65)
    stem_m = look.mat('stem', look.STEM, 0.6)
    for ang, ln in ((2.7, 0.086), (1.8, 0.071), (0.9, 0.094), (-0.2, 0.078),
                    (-1.5, 0.068), (-2.4, 0.082)):
        d = (math.cos(ang), math.sin(ang))
        broad_leaf(leaf_m, (0.0, 0.0, 0.104), d, length=ln,
                   width=0.058, rise=1.30, droop=0.72)
    for ang in (2.7, 0.9, -1.5):
        dd = (math.cos(ang), math.sin(ang))
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.003, radius2=0.0015, depth=0.055,
                                        location=(dd[0] * 0.010, dd[1] * 0.010, 0.128))
        look.put(bpy.context.object, stem_m)

    # --- the hand, entering from camera right with one finger in the soil
    if os.environ.get('HAND', '1') == '1':
      skin_m = look.mat('skin', look.SKIN, 0.72)
      finger = parts.build_finger(skin=skin_m)
      finger.location = (0.016, -0.004, 0.103)
      finger.rotation_euler = (0.0, -0.62, 0.22)
      bpy.context.view_layer.update()
      wrist = finger.matrix_world @ Vector((0.128, 0.033, 0.044))
      sc_mod.build_arm(wrist, (0.72, 0.02, 0.26), skin=skin_m)

    # --- light. 6.4% of the plate is above V 0.60 and 58% below 0.18, so this
    # is one lamp and almost nothing else.
    bpy.ops.object.light_add(type='AREA', location=(0.205, -0.195, 0.275))
    k = bpy.context.object
    k.data.energy, k.data.size, k.data.color = 3.2, 0.09, look.LAMP
    look.aim(k, (0.05, 0.00, 0.10))
    bpy.ops.object.light_add(type='AREA', location=(-0.45, 1.10, 0.80))
    f = bpy.context.object
    f.data.energy, f.data.size, f.data.color = 1.6, 1.20, look.MOON
    look.aim(f, (0.0, 0.0, 0.12))

    return sc


if __name__ == '__main__':
    sc = build()
    out = sys.argv[sys.argv.index('--') + 1]
    sc.render.filepath = out
    bpy.ops.render.render(write_still=True)
    print('WROTE', out)
