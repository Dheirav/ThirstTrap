"""One room, built once, with a camera for every beat.

The seven reference plates were generated independently, so they disagree: the
lamp is on the left in three and the right in two, the window moves, the moon
changes size. That is unavoidable when each picture is drawn from scratch and it
is the reason the page needed match cuts hidden in darkness.

A modelled room does not have that problem. The window is where the window is,
and every shot agrees about it because they are the same room. That is the whole
reason to rebuild rather than keep generating: not fidelity to any one plate,
but consistency across all of them, plus a camera that can move continuously
between shots instead of cutting.

Layout, in metres, floor at z=0:
  window wall at  y = +1.06, glazed from z 0.95 to 2.05
  sill          y  0.90..1.06, top at z 0.95, four pots along it
  table         centred (0.42, 0.06), top at z 0.755
  lamp          clamped at the right, x +0.95, throwing left across the table
  ledger        pinned to the pier at x -1.02, beside the window
"""
import bpy, math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look, parts, props, scene as sc_mod
from mathutils import Vector

SILL_Z, TABLE_Z, WIN_Y = 0.95, 0.755, 1.06

# Objects that belong to the depth and roots shots only. See build().
CUT_POT = []
# Was (-0.52, -0.21, 0.10, 0.41): exactly 0.31 apart, three times, which
# with identical plants on top read as four pinwheels on a metronome.
POTS_X = (-0.55, -0.23, 0.07, 0.42)


def _box(loc, scale, m, smooth=False):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc)
    o = bpy.context.object; o.scale = scale
    return look.put(o, m, smooth)


def facet_pot(loc, top_r=0.075, bot_r=0.058, h=0.130, sides=16, m=None, cut=False):
    """Faceted terracotta. `cut` removes the front half so the soil column shows,
    which is the depth shot: it is the same pot, not a different prop."""
    m = m or look.mat('terra', look.TERRA, 0.80)
    bpy.ops.mesh.primitive_cone_add(vertices=sides, radius1=bot_r, radius2=top_r, depth=h,
                                    end_fill_type='NOTHING', location=(loc[0], loc[1], loc[2] + h/2))
    body = look.put(bpy.context.object, m)
    s = body.modifiers.new('s', 'SOLIDIFY'); s.thickness = 0.006; s.offset = -1.0
    bpy.ops.mesh.primitive_circle_add(vertices=sides, radius=bot_r, fill_type='NGON',
                                      location=(loc[0], loc[1], loc[2] + 0.003))
    look.put(bpy.context.object, m)
    bpy.ops.mesh.primitive_cone_add(vertices=sides, radius1=top_r + 0.004, radius2=top_r + 0.006,
                                    depth=0.016, end_fill_type='NOTHING',
                                    location=(loc[0], loc[1], loc[2] + h - 0.004))
    rim = look.put(bpy.context.object, m)
    r2 = rim.modifiers.new('s', 'SOLIDIFY'); r2.thickness = 0.007; r2.offset = -1.0
    soil = parts._soil_grid(loc[0], loc[1], loc[2] + h - 0.030, top_r - 0.010, n=116)
    # parts.dress_soil has existed the whole time and was never called from this
    # file, so every pot in every shot had mirror-flat dirt. In a set of pictures
    # about how wet the soil is, and in two shots whose entire job is a finger
    # pressed into it, the surface had no grain and took no mark.
    parts.dress_soil(soil, bump=0.0021)
    look.put(soil, look.mat('soil', look.SOIL, 0.95), smooth=True)
    col = crust = None
    if cut:
        wet = look.mat('wet', look.srgb('#2A1D14'), 0.96)
        dry = look.mat('dry', look.srgb('#9A8straight'.replace('straight', '264')), 0.98)
        bpy.ops.mesh.primitive_cylinder_add(vertices=sides, radius=top_r - 0.011,
                                            depth=h - 0.040,
                                            location=(loc[0], loc[1], loc[2] + (h - 0.040)/2 + 0.004))
        col = look.put(bpy.context.object, wet)
        bpy.ops.mesh.primitive_cylinder_add(vertices=sides, radius=top_r - 0.0105, depth=0.030,
                                            location=(loc[0], loc[1], loc[2] + h - 0.045))
        crust = look.put(bpy.context.object, dry)
        root_m = look.mat('root', look.srgb('#B7A184'), 0.85)
        import random
        rnd = random.Random(7)
        for i in range(14):
            a0 = rnd.uniform(0, 6.283); r1 = rnd.uniform(0.020, top_r - 0.016)
            spine = [(loc[0], loc[1], loc[2] + h - 0.050)]
            for s in range(1, 5):
                f = s / 4
                spine.append((loc[0] + math.cos(a0) * r1 * f,
                              loc[1] + math.sin(a0) * r1 * f,
                              loc[2] + h - 0.050 - (h - 0.070) * f))
            tb = parts.tube(spine, [0.0022, 0.0017, 0.0012, 0.0008, 0.0005],
                            name='root', seg=6)
            look.put(tb, root_m)
    if cut:
        knife = _cutter(loc)
        for ob in [body, rim, soil] + [o for o in (col, crust) if o]:
            b = ob.modifiers.new('cut', 'BOOLEAN'); b.operation = 'DIFFERENCE'
            b.object = knife
        for o in bpy.data.objects:
            if o.name.startswith('root'):
                b = o.modifiers.new('cut', 'BOOLEAN'); b.operation = 'DIFFERENCE'
                b.object = knife
    return body, rim, soil


def _cutter(loc):
    bpy.ops.mesh.primitive_cube_add(size=1, location=(loc[0], loc[1] - 0.14, loc[2] + 0.07))
    c = bpy.context.object; c.scale = (0.4, 0.28, 0.4)
    c.hide_render = True; c.hide_viewport = True
    return c


def leaf(m, base, aim_xy, length, width=0.052, rise=1.25, droop=0.60,
         lobes=3, petiole=0.20, seed=0):
    """One leaf: a petiole, then a blade with its widest point past the middle.

    The old one was a five-segment ribbon whose width went as sin(pi*t), which
    peaks dead centre and falls to zero at both ends. That is a kite. It also
    had six cross-sections, so the outline was visibly polygonal at any size
    the hero shows, and it grew straight out of the soil with no stalk.

    Three changes, all of them things a leaf has and a kite does not. A bare
    petiole over the first fifth, so the blade is held away from the stem and
    you can see between leaves. A width profile u^1.1 * (1-u)^0.8, which peaks
    at 0.58 and carries a flatter shoulder, so the broad part is past the
    middle. And a shallow lobing on the edge, because an unbroken ellipse reads
    as a petal.
    """
    rnd = random.Random(seed)
    ax, ay = aim_xy
    n = math.hypot(ax, ay) or 1.0
    ax, ay = ax / n, ay / n
    px, py = -ay, ax
    SEG = 14
    pts, faces = [], []
    peak = 1.1 ** 1.1 * 0.8 ** 0.8          # normaliser for the profile below
    for i in range(SEG + 1):
        t = i / SEG
        z = base[2] + rise * length * t - droop * length * t * t
        cx, cy = base[0] + ax * length * t, base[1] + ay * length * t
        if t < petiole:
            w = width * 0.035
        else:
            u = (t - petiole) / (1.0 - petiole)
            prof = (u ** 1.1) * ((1.0 - u) ** 0.8)
            prof /= (1.1 ** 1.1 * 0.8 ** 0.8) / ((1.1 + 0.8) ** 1.9) or 1.0
            prof = min(prof, 1.0)
            # shallow lobes, so the outline is not one smooth ellipse
            prof *= 1.0 + 0.13 * math.cos(lobes * math.pi * u)
            w = width * prof * 0.5
        lift = 0.006 * math.sin(math.pi * t)
        pts += [(cx + px * w, cy + py * w, z - 0.002 * (1 - t)),
                (cx, cy, z + lift),
                (cx - px * w, cy - py * w, z - 0.002 * (1 - t))]
    for i in range(SEG):
        a0 = i * 3
        faces += [(a0, a0 + 1, a0 + 4, a0 + 3), (a0 + 1, a0 + 2, a0 + 5, a0 + 4)]
    me = bpy.data.meshes.new('leaf')
    me.from_pydata(pts, [], faces)
    me.update()
    ob = bpy.data.objects.new('leaf', me)
    bpy.context.collection.objects.link(ob)
    ob.modifiers.new('s', 'SOLIDIFY').thickness = 0.0012
    return look.put(ob, m)


def leafy_plant(at, leaf_m, stem_m, scale=1.0, seed=0):
    """A branching shrub, which is what every plate actually shows.

    This used to build a rosette: seven leaves on one ring and five on another,
    all radiating from a single point at exact 2pi/n intervals. Four of those
    on a sill at exactly 0.31 m pitch read as four identical pinwheels on a
    metronome, and the species read as agave against plates full of broad
    lobed leaves on stalks.

    A stem with nodes up it fixes both at once. Leaves arrive at four or five
    different heights instead of one, which is what lets you see sky between
    them, and the angles come off a wandering phase rather than an exact
    division, so no two plants rhyme. Leaf count roughly triples, because the
    plates carry 25 to 40 and twelve was never going to read as foliage.
    """
    rnd = random.Random(seed)
    lean_a = rnd.uniform(0, math.tau)
    lean = rnd.uniform(0.06, 0.16)                 # nothing grows plumb
    H = 0.145 * scale

    def at_h(f):
        return (at[0] + math.cos(lean_a) * lean * H * f,
                at[1] + math.sin(lean_a) * lean * H * f,
                at[2] + H * f)

    spine = [at_h(i / 7) for i in range(8)]
    r0 = 0.0042 * scale
    look.put(parts.tube(spine, [r0 * (1 - 0.72 * (i / 7)) for i in range(8)],
                        name='stem', seg=7), stem_m, smooth=True)

    phase = rnd.uniform(0, math.tau)
    nodes = 5
    total = 0
    for i in range(nodes):
        f = 0.26 + 0.70 * (i / (nodes - 1))
        nb = at_h(f)
        # 137.5 degrees is the angle real phyllotaxis uses, and the point of it
        # here is only that it never repeats.
        for k in range(rnd.randint(2, 3)):
            a = phase + (total * 2.3999) + rnd.uniform(-0.25, 0.25)
            total += 1
            up = 0.9 - 0.5 * f
            ln = (0.052 + 0.030 * (1.0 - f) + rnd.uniform(-0.006, 0.008)) * scale
            br = [(nb[0], nb[1], nb[2]),
                  (nb[0] + math.cos(a) * ln * 0.30, nb[1] + math.sin(a) * ln * 0.30,
                   nb[2] + 0.016 * scale * up)]
            look.put(parts.tube(br, [r0 * 0.45, r0 * 0.30], name='stem', seg=5),
                     stem_m, smooth=True)
            leaf(leaf_m, br[1], (math.cos(a), math.sin(a)), ln,
                 width=(0.040 + 0.012 * (1 - f)) * scale,
                 rise=0.85 + up * 0.5 + rnd.uniform(-0.1, 0.1),
                 droop=0.78 + rnd.uniform(-0.12, 0.14),
                 lobes=rnd.choice((2, 3, 3, 4)), seed=seed * 91 + total)
    # a couple at the apex, shorter and more upright
    for k in range(2):
        a = phase + total * 2.3999 + rnd.uniform(-0.3, 0.3)
        total += 1
        leaf(leaf_m, at_h(1.0), (math.cos(a), math.sin(a)), 0.044 * scale,
             width=0.032 * scale, rise=1.5, droop=0.5,
             lobes=3, seed=seed * 91 + total)


def plant(at, leaf_m, stem_m, scale=1.0, n=6):
    for k, (ang, ln) in enumerate((2.7, .095), ) if False else enumerate(
            [(2.7, .095), (1.6, .080), (0.6, .103), (-0.5, .086), (-1.7, .075), (-2.7, .090)][:n]):
        leaf(leaf_m, at, (math.cos(ang), math.sin(ang)), ln*scale, width=0.052*scale)
    for ang in (2.7, 0.6, -1.7):
        d = (math.cos(ang), math.sin(ang))
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.0030*scale, radius2=0.0015*scale,
                                        depth=0.060*scale,
                                        location=(at[0]+d[0]*0.010, at[1]+d[1]*0.010, at[2]+0.026*scale))
        look.put(bpy.context.object, stem_m)


def soil_macro(at=(0.0, 0.0, -5.0), seed=3):
    """The roots beat: inside the soil, not looking at a pot.

    Every other shot is the room seen from somewhere in it. This one is a macro
    into dirt, and the room has no geometry at that scale, which is why the
    camera kept framing the whole cut pot instead. It is built here as its own
    small set and parked five metres under the floor, because nothing in the
    sequence can see this and the room at the same time, so it does not have to
    agree with the room about anything except the light.

    The channel is a gap between two displaced banks rather than a groove cut
    into one. A boolean trench in a noisy surface fights the noise at its edges
    and reads as a seam; two banks leave a clean dark gap and the torn roots
    bridging it are the subject anyway.
    """
    rnd = random.Random(seed)
    # Darker dirt, not more light. The roots already measure p95 0.702 against the
    # plate's 0.710; it was the soil at p50 0.259 against 0.216 that flattened the
    # shot, so contrast ran 5.1x where the plate runs 6.5x.
    soil_m = look.mat('macrosoil', look.srgb('#2A1E16'), 0.98)
    # #CFC0A4 clipped to white at this gain. The plate's roots are pale against
    # dark crumb, which is a value relationship, not a bright material.
        # The roots read pale in the plate, which is a RENDERED value, not an
    # albedo. #BCAC8A is 0.42 linear and at gain 3.2 that lands at 1.40, so the
    # roots were the thing clipping 3.6% of this frame. They want to arrive
    # near white, not start there.
    root_m = look.mat('macroroot', look.srgb('#938872'), 0.88)

    tex = bpy.data.textures.new('crumb', type='CLOUDS')
    tex.noise_scale = 0.09
    tex.noise_depth = 4

    # A floor under the channel, DEEP and in its own shadow. Without one the gap
    # renders as a black void, which is a hole in the picture rather than a hole
    # in the soil. At 0.17 down it was the opposite problem: lit as brightly as
    # the banks, so the trench vanished into continuous dirt at two depths and
    # the shot read as flat ground. The plate's channel has crumb at the bottom
    # of it, visible but clearly further from the light.
    bpy.ops.mesh.primitive_grid_add(x_subdivisions=90, y_subdivisions=90, size=1.2,
                                    location=(at[0], at[1], at[2] - 0.175))
    fl = look.put(bpy.context.object, soil_m, smooth=True)
    fd = fl.modifiers.new('crumb', 'DISPLACE')
    fd.texture = tex
    fd.strength = 0.07

    for side in (-1, 1):
        # 0.52 apart with a size of 1.6 means the two banks OVERLAP across the
        # middle, so there was no channel at all: the shot was a flat field of
        # dirt. They have to be further apart than half their own width.
        bpy.ops.mesh.primitive_grid_add(x_subdivisions=160, y_subdivisions=160, size=1.6,
                                        location=(at[0] + side * 0.88, at[1], at[2]))
        g = look.put(bpy.context.object, soil_m, smooth=True)
        d = g.modifiers.new('crumb', 'DISPLACE')
        d.texture = tex
        d.strength = 0.11
        # Tip the banks toward each other so the gap reads as a channel with
        # depth rather than a slot cut in a flat floor.
        # 13 degrees rotated each bank about its own centre, which lifted the
        # inner edge across the gap and closed the channel again as fast as
        # widening it opened one. 6 is enough to say the walls lean in.
        g.rotation_euler = (0, math.radians(-side * 6), 0)

    # Crumb, as geometry. A displaced grid gives a mottled PATTERN, which is
    # what the first four attempts rendered: the soil read as camouflage paint
    # rather than as lumps, because flat shading has no gradient to tell you a
    # surface is round. Actual pebbles have silhouettes and cast shadows on each
    # other, and that is the whole difference.
    peb_m = look.mat('macrocrumb', look.srgb('#342618'), 0.98)
    for _ in range(260):
        side = -1 if rnd.random() < 0.5 else 1
        # Clear of the channel lip. Projecting the gap showed it occupying 15%
        # of the frame all along, so it was never missing, it was being covered:
        # pebbles crowded its edges and forty roots crossed it.
        px = at[0] + side * rnd.uniform(0.15, 1.45)
        py = at[1] + rnd.uniform(-0.75, 0.75)
        pz = at[2] + 0.055 + rnd.uniform(-0.03, 0.05)
        r = rnd.uniform(0.012, 0.045)
        bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1, radius=r,
                                              location=(px, py, pz))
        o = bpy.context.object
        o.scale = (rnd.uniform(0.7, 1.3), rnd.uniform(0.7, 1.3), rnd.uniform(0.4, 0.8))
        o.rotation_euler = (rnd.uniform(0, 3), rnd.uniform(0, 3), rnd.uniform(0, 3))
        look.put(o, peb_m if rnd.random() < 0.6 else soil_m)

    # Roots that END at the channel, which is the entire beat.
    #
    # "Each push tears the fine roots it is measuring." Every previous version
    # ran roots clean across the gap and then added more of them, on the theory
    # that the shot was under-populated. It was not: a denser net is still an
    # intact one, and no scatter density produces a severed end. A root that
    # crosses the channel says a root was there. A root that stops at the lip
    # with pale fibre splayed out of the break says something took it off.
    #
    # So each one runs from its bank INWARD and terminates short of the gap,
    # and the break is modelled: three to six fine threads fanning forward and
    # down, thinner than the root and lighter, because torn xylem is paler than
    # the root skin around it.
    torn_m = look.mat('macrotorn', look.srgb('#A69B82'), 0.90)
    for i in range(26):
        side = -1 if i % 2 else 1
        x0 = at[0] + side * rnd.uniform(0.60, 1.45)
        y0 = at[1] + rnd.uniform(-0.62, 0.62)
        # stop just short of the lip, with a little scatter so the breaks do not
        # line up into a seam of their own
        xe = at[0] + side * rnd.uniform(0.085, 0.145)
        bend = rnd.uniform(-0.14, 0.14)
        spine = []
        for t in range(9):
            f = t / 8
            spine.append((x0 + (xe - x0) * f + rnd.uniform(-0.010, 0.010),
                          y0 + bend * math.sin(math.pi * f) + rnd.uniform(-0.016, 0.016),
                          at[2] + 0.085 + 0.030 * math.sin(math.pi * f) * rnd.uniform(0.3, 1.0)
                          + rnd.uniform(-0.010, 0.010)))
        r = rnd.uniform(0.0050, 0.0105)
        look.put(parts.tube(spine, [r * v for v in
                                    (1.0, 0.96, 0.90, 0.83, 0.74, 0.63, 0.50, 0.36, 0.20)],
                            name='macroroot', seg=7), root_m, smooth=True)
        # the break
        tip = spine[-1]
        inward = -side
        for _ in range(rnd.randint(3, 6)):
            a2 = rnd.uniform(-0.75, 0.75)
            ln = rnd.uniform(0.018, 0.042)
            end = (tip[0] + inward * ln * math.cos(a2),
                   tip[1] + ln * math.sin(a2) * 0.8,
                   tip[2] + rnd.uniform(-0.016, 0.008))
            mid = ((tip[0] + end[0]) / 2, (tip[1] + end[1]) / 2,
                   (tip[2] + end[2]) / 2 + rnd.uniform(0.0, 0.006))
            look.put(parts.tube([tip, mid, end],
                                [r * 0.26, r * 0.15, 0.0005],
                                name='macroroot', seg=4), torn_m, smooth=True)

    # a handful of short stubs left in the banks, roots whose other half is gone
    for _ in range(9):
        side = -1 if rnd.random() < 0.5 else 1
        x0 = at[0] + side * rnd.uniform(0.30, 0.95)
        y0 = at[1] + rnd.uniform(-0.6, 0.6)
        xe = x0 - side * rnd.uniform(0.10, 0.20)
        r = rnd.uniform(0.0020, 0.0038)
        sp = [(x0, y0, at[2] + 0.082), ((x0 + xe) / 2, y0 + rnd.uniform(-0.01, 0.01),
                                        at[2] + 0.094), (xe, y0, at[2] + 0.086)]
        look.put(parts.tube(sp, [r, r * 0.7, r * 0.3], name='macroroot', seg=5),
                 root_m, smooth=True)
        for _ in range(rnd.randint(2, 4)):
            a2 = rnd.uniform(-1.0, 1.0)
            ln = rnd.uniform(0.012, 0.030)
            look.put(parts.tube([(xe, y0, at[2] + 0.086),
                                 (xe - side * ln * math.cos(a2), y0 + ln * math.sin(a2) * 0.8,
                                  at[2] + 0.086 + rnd.uniform(-0.01, 0.006))],
                                [r * 0.22, 0.0005], name='macroroot', seg=3),
                     torn_m, smooth=True)


ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets')


def append(name, loc, scale=1.0, rot_z=0.0, mats=None, default=None, drop=('_ground',)):
    """Append a CC0 prop from Poly Haven and reshade it flat.

    The props are modelled photoreal with PBR maps; the page is flat-shaded
    colour. So only the geometry is kept and every material slot is replaced
    from look.py, which is also why only alpha maps were downloaded: nothing
    else survives the trip. Assets whose leaves are alpha cards are avoided for
    that reason, which is why every plant in the room is potted_plant_04.
    """
    path = os.path.join(ASSETS, name, name + '.blend')
    before = set(bpy.data.objects)
    with bpy.data.libraries.load(path, link=False) as (src, dst):
        dst.objects = list(src.objects)
    loaded = [o for o in bpy.data.objects if o not in before]
    new = [o for o in loaded if o.type == 'MESH']
    # Poly Haven ships a display base with several assets, which renders as a
    # large pale dome sitting in the middle of the room. Names are taken before
    # the removals, because removing an object invalidates every Python
    # reference to it and reading .name afterwards raises.
    keep = [o.name for o in new if not any(d in o.name.lower() for d in drop)]
    keep_all = [o.name for o in loaded if not any(d in o.name.lower() for d in drop)]
    for o in [x for x in new if any(d in x.name.lower() for d in drop)]:
        bpy.data.objects.remove(o, do_unlink=True)
    new = [bpy.data.objects[n] for n in keep if n in bpy.data.objects]
    loaded = [bpy.data.objects[n] for n in keep_all if n in bpy.data.objects]
    root = bpy.data.objects.new(name + '_root', None)
    bpy.context.collection.objects.link(root)
    root.location, root.scale = loc, (scale, scale, scale)
    root.rotation_euler = (0, 0, rot_z)
    bpy.context.view_layer.update()

    # Re-parent EVERY mesh to the root, keeping its world transform, rather than
    # only the ones that had no parent. desk_lamp_arm_01 is rigged: its meshes
    # are parented to an armature, so leaving that alone meant the root moved
    # nothing and the IK re-solved on each nudge. That is why correcting the
    # lamp's position diverged instead of converging. Nothing here is animated,
    # so the pose is baked and the asset becomes rigid geometry under the root.
    # Link and re-parent the WHOLE asset, not just its meshes. desk_lamp_arm_01
    # is rigged: its meshes carry Armature modifiers, so they follow the armature
    # no matter what their parent says. Leaving the armature unlinked meant the
    # root moved nothing and every correction diverged. Parent the top level of
    # the hierarchy, whatever type it is, and the rig travels with it.
    for o in loaded:
        if o.name not in bpy.context.collection.objects:
            bpy.context.collection.objects.link(o)
    for o in loaded:
        if o.parent is None:
            # No matrix_parent_inverse here: letting the child keep its original
            # world transform cancels the root's scale and rotation, which put a
            # full-size lamp back in a room asking for a 0.40 one.
            o.parent = root
    bpy.context.view_layer.update()
    for o in new:
        # potted_plant_04 gives dirt, ground, plant and pot ONE shared material,
        # so a material-name match cannot tell them apart. The object name can,
        # and the order matters: "potted_plant_04_pot" contains both "pot" and
        # "plant", so the specific hint has to be tested first. Hence a list
        # rather than a dict.
        # Match the OBJECT name first and only fall back to material names.
        # Every part of the lamp carries both 'desk_lamp_arm_01' and
        # 'desk_lamp_arm_01_light', so testing a joined haystack gave the whole
        # fixture the emissive shade material and it rendered as a solid white
        # blob. Same shape of bug as the pots: the hint matched more than meant.
        oname = o.name.lower()
        slots = [(m.name.lower() if m else '') for m in o.data.materials]
        o.data.materials.clear()
        pick = None
        for hint, m in (mats or []):
            if hint in oname:
                pick = m; break
        if pick is None:
            for hint, m in (mats or []):
                if any(hint in s for s in slots):
                    pick = m; break
        if pick is None:
            pick = default
        if pick:
            o.data.materials.append(pick)
        for poly in o.data.polygons:
            poly.use_smooth = False
    bpy.context.view_layer.update()
    # An appended asset's objects carry their own offsets from the file's
    # origin, so parenting them to an empty at `loc` places the ORIGIN there,
    # not the asset: the desk lamp landed a metre and a half from where it was
    # asked for, which is why its light pool was in the wrong half of the room.
    # Re-seat the root so the asset's base centre is actually at `loc`.
    bpy.context.view_layer.update()
    import mathutils
    pts = [o.matrix_world @ mathutils.Vector(c) for o in new for c in o.bound_box]
    if pts:
        cx = (min(q.x for q in pts) + max(q.x for q in pts)) / 2
        cy = (min(q.y for q in pts) + max(q.y for q in pts)) / 2
        bz = min(q.z for q in pts)
        root.location = (root.location.x + (loc[0] - cx),
                         root.location.y + (loc[1] - cy),
                         root.location.z + (loc[2] - bz))
        bpy.context.view_layer.update()
    return root


def soil_top(x, y, r=0.12):
    """Top of the dirt surface nearest (x, y). The pot is an appended asset, so
    where its soil sits is a fact about the mesh, not a number I get to pick."""
    import mathutils
    best = None
    for o in bpy.data.objects:
        # Each plant asset named its soil differently: potted_plant_04 said
        # _dirt, potted_plant_01 said _pebbles. Matching only one of them
        # dropped the hand to table height, below the sill and out of frame.
        #
        # It happened again when the appended pots were replaced by modelled
        # ones, whose soil object is simply called "soil", because none of the
        # names here start with an underscore for it. Same failure, same
        # silence: the function returns None, the caller falls back to the
        # table, and the finger ends up 12 cm under the sill. Matched on the
        # bare word too now, and the caller is no longer allowed to guess.
        nm = o.name.lower()
        if o.type != 'MESH' or not (
                nm.startswith('soil') or
                any(k in nm for k in ('_dirt', '_pebbles', '_soil', '_ground'))):
            continue
        pts = [o.matrix_world @ mathutils.Vector(c) for c in o.bound_box]
        cx = sum(p.x for p in pts) / 8; cy = sum(p.y for p in pts) / 8
        if math.hypot(cx - x, cy - y) > r:
            continue
        top = max(p.z for p in pts)
        if best is None or top > best:
            best = top
    return best


# Soil vertices lifted by a dimple, so the next shot can put them back. The
# soil meshes persist across shots in one build, so a dimple pressed for
# finger-test would otherwise still be there in scale-table, which is a dent in
# a pot nobody has touched.
_DIMPLED = []


def _undimple():
    for mesh_name, saved in _DIMPLED:
        me = bpy.data.meshes.get(mesh_name)
        if me:
            for i, z in saved:
                me.vertices[i].co.z = z
            me.update()
    _DIMPLED.clear()


def dimple_soil(at, r=0.020, depth=0.0075):
    """Press a hole into whichever soil surface is under `at`.

    Real displaced geometry rather than a dark texture, because the hole has to
    catch the light from one side and cast into itself. That is the product's
    whole premise rendered as one shape, and it was missing.
    """
    _undimple()
    x, y = at[0], at[1]
    best, bestd = None, 1e9
    for o in bpy.data.objects:
        if o.type != 'MESH' or not o.name.lower().startswith('soil'):
            continue
        c = sum((o.matrix_world @ Vector(v) for v in o.bound_box), Vector()) / 8
        d = math.hypot(c.x - x, c.y - y)
        if d < bestd:
            best, bestd = o, d
    if best is None or bestd > 0.14:
        return None
    me = best.data
    saved = []
    for i, v in enumerate(me.vertices):
        dd = math.hypot(v.co.x - x, v.co.y - y)
        if dd < r:
            saved.append((i, v.co.z))
            t = 1.0 - (dd / r)
            v.co.z -= depth * (t ** 1.6)
    me.update()
    if saved:
        _DIMPLED.append((me.name, saved))
    return best


def place_hand(at, yaw=0.0, pitch=-0.62, into=0.012):
    """A hand pushed into the soil at `at`, with a forearm leaving frame.

    It used to be one finger and a cone, on the argument that a whole scripted
    hand reads as a lay figure. That argument was made before parts.build_hand
    was written, and build_hand has sat unused ever since: a right hand with
    three phalanges and joints in the index, a bevelled palm, and the other
    fingers curled under, which is both what a hand doing this actually does and
    a way of hiding knuckles it does not model. Its fingertip is at its own
    origin, so placing it is the same one move the finger needed.

    A cone with a finger on it was the single most obviously wrong object in the
    set, and the fix had been written and never called.

    Called per shot rather than built into the room, because the same hand is in
    two different pots in two different beats.
    """
    # Clean up by hierarchy, not by name. build_finger parents a sphere to the
    # finger empty without naming it 'finger*', so deleting by prefix orphaned
    # it: the leftovers from the previous shot stayed in the room and showed up
    # as pale fragments floating over the sill.
    stale = []
    for o in bpy.data.objects:
        if o.name.startswith(('finger', 'arm', 'limb', 'joint')):
            stale.append(o)
            stale.extend(o.children_recursive)
    for n in {o.name for o in stale}:
        if n in bpy.data.objects:
            bpy.data.objects.remove(bpy.data.objects[n], do_unlink=True)
    skin = look.mat('skin', look.srgb('#8A6248'), 0.86)
    # Back to build_finger, and the reason is worth keeping.
    #
    # parts.build_hand exists and had never been called, which looked like a
    # free upgrade: a right hand with three phalanges, joints, a bevelled palm
    # and the other fingers curled. Four rotation solves later, each one wrong
    # in a different way, I rendered it on its own against a ground plane and
    # the problem was not the rotation at all. It does not read as a hand. The
    # palm is a blob, the curled fingers are detached sausages hanging beside
    # it, and the forearm is a tapered cone. It is not better than one finger,
    # it is worse, because a single finger is an abstraction a viewer completes
    # and a bad hand is one they do not.
    #
    # The original comment on this function was right and I should have tested
    # its claim before overturning it: a whole scripted hand reads as a lay
    # figure. A real hand here is modelling work, not a function call.
    f = parts.build_finger(skin=skin)
    f.name = 'finger'
    f.location = (at[0], at[1], at[2] - into)
    f.rotation_euler = (0.0, pitch, yaw)
    bpy.context.view_layer.update()
    wrist = f.matrix_world @ Vector((0.128, 0.033, 0.044))
    a = sc_mod.build_arm(wrist, (at[0] + 0.62 * math.cos(yaw),
                                 at[1] + 0.62 * math.sin(yaw), at[2] + 0.34), skin=skin)
    a.name = 'arm'
    return f


def scatter(root, placements):
    """Linked duplicates of an appended asset.

    root_cluster_01 is 225k faces. Appending it five times to make a root mass
    would load five copies of that; linked duplicates share the mesh data, so
    the cost is five object headers and the renderer instances the rest.
    """
    src = [o for o in root.children_recursive if o.type == 'MESH']
    made = []
    for loc, scale, rot in placements:
        holder = bpy.data.objects.new(root.name + '_dup', None)
        bpy.context.collection.objects.link(holder)
        holder.location, holder.scale = loc, (scale, scale, scale)
        holder.rotation_euler = rot
        for o in src:
            c = o.copy()                      # object copy, mesh data shared
            bpy.context.collection.objects.link(c)
            c.parent = holder
            c.matrix_parent_inverse = o.matrix_parent_inverse.copy()
            c.matrix_basis = o.matrix_basis.copy()
        made.append(holder)
    bpy.context.view_layer.update()
    return made


def build(res=(1672, 941), samples=96):
    sc = look.reset(res=res, samples=samples)
    M = dict(
        wall=look.textured('wall', look.srgb('#352E27'), 'rough_plaster_brick_diff_1k.jpg',
                           strength=0.70, scale=0.6),
        pier=look.mat('pier', look.srgb('#403729'), 0.92),
        sill=look.textured('sill', look.srgb('#4A3E33'), 'wood_table_001_diff_1k.jpg',
                           strength=0.80, scale=2.2),
        wood=look.textured('wood', look.srgb('#5C4A3B'), 'wood_table_001_diff_1k.jpg',
                           strength=0.85, scale=1.4),
        frame=look.mat('frame', look.srgb('#1E1812'), 0.85),
        terra=look.textured('terra', look.TERRA, 'terracotta_floor_tiles_diff_1k.jpg',
                            strength=0.55, scale=3.0, rough=0.80),
        leaf=look.mat('leaf', look.LEAF, 0.66),
        stem=look.mat('stem', look.STEM, 0.60),
        metal=look.mat('metal', look.srgb('#6E7276'), 0.42, ),
        paper=look.mat('paper', look.srgb('#CDBE9E'), 0.92),
        plastic=look.mat('plastic', look.srgb('#6E6A61'), 0.60),
        glassblack=look.mat('screen', look.srgb('#0A0B0D'), 0.18),
        mug=look.mat('mug', look.srgb('#7A7366'), 0.76),
    )
    # --- shell
    _box((0, 0.2, -0.02), (6, 5, 0.04), M['wood'])                      # floor
    _box((0, WIN_Y + 0.06, 0.475), (6, 0.10, 0.95), M['wall'])          # under the sill
    _box((0, WIN_Y + 0.06, 2.75), (6, 0.10, 1.40), M['wall'])           # over the head
    _box((-2.20, WIN_Y + 0.06, 1.5), (2.1, 0.10, 3.4), M['wall'])       # left of the opening
    _box((2.00, WIN_Y + 0.06, 1.5), (2.1, 0.10, 3.4), M['wall'])        # right of the opening
    _box((1.28, 0.2, 1.5), (0.10, 5, 3.4), M['wall'])                   # right wall
    for x, w in ((-1.14, 0.30), (0.80, 0.30)):                          # piers either side
        _box((x, WIN_Y, 1.5), (w, 0.11, 1.12), M['pier'])
    _box((-0.17, WIN_Y, 2.10), (2.2, 0.11, 0.18), M['pier'])            # head
    _box((-0.17, 0.985, SILL_Z - 0.028), (2.5, 0.185, 0.055), M['sill'])
    # glazing: dim emission, because night read as paint when it was a flat colour
    # Dimmer than it was. Measured on the plate the glass sits at value 0.24,
    # and it has to stay below the lit wood or the window pulls the eye out of
    # the room, which is the opposite of what these shots are about.
    # The glass measured 0.24 on the plate and the window region 0.21, against
    # 0.09 here, so the night outside was reading as a wall rather than as a
    # city. Sweepable because the posterise ramp makes the relationship between
    # emission and final value nonlinear, so it has to be measured, not solved.
    # 0.75 puts the glass at 0.240, which is the value the glass measures on the
    # plate. Solved rather than swept: the bands are emissive now, so the glass
    # is its emission alone, and (0.0578 * s) ** (1/2.2) = 0.24 gives s = 0.75.
    # The old 0.56 was fitted when the city was lit geometry hiding the sky
    # entirely, so it was fitted against a window the viewer never saw.
    SKY_HEX = os.environ.get('TT_SKY_HEX', '#323A44')
    sky = look.mat('sky', look.SKY, 0.9, emit=look.srgb(SKY_HEX),
                   strength=float(os.environ.get('TT_SKY', 0.75)))
    # pushed back behind the far band, which now reaches WIN_Y + 4.0
    g = _box((-0.17, WIN_Y + 6.40, 1.50), (26.0, 0.02, 14.0), sky)
    for mx in (-0.17,):
        _box((mx, WIN_Y - 0.02, 1.50), (0.022, 0.03, 1.08), M['frame'])
    _box((-0.17, WIN_Y - 0.02, 1.50), (1.92, 0.03, 0.022), M['frame'])
    # The city, in three depth bands. See props.skyline for why that is worth
    # more to a moving camera than to any still.
    _rnd = random.Random(11)
    # Solved against the glass, not chosen. A lit window at strength 1.4 on a
    # colour whose linear value is 0.588 arrives at 0.92, which is a hair off
    # clipping and roughly four times the sky: that is why they read as holes
    # punched in the picture rather than as lights across the way. 0.62 and 0.42
    # put warm at 0.63 and cool at 0.52, which sits them above the 0.24 glass
    # without letting the brightest thing in the frame be something outside it.
    litw = look.mat('litwin', (0, 0, 0, 1), 1.0, emit=look.srgb('#C98A3A'), strength=0.62)
    litc = look.mat('litwin2', (0, 0, 0, 1), 1.0, emit=look.srgb('#9FB4C6'), strength=0.42)
    for kind, (loc, scale), m in props.skyline(
            WIN_Y, SILL_Z,
            lambda n, h, k: look.mat(n, (0, 0, 0, 1), 1.0, emit=look.srgb(h),
                                     strength=float(os.environ.get('TT_SKY', 0.75)) * k),
            litw, litc, _rnd, SKY_HEX):
        o = _box(loc, scale, m)
        # The city casts no shadow, and that is not laziness. The sky plane is
        # the room's only light through the glass, and the far band stands
        # between it and the window: with shadows on, adding the skyline darkened
        # the whole interior, which is how a backdrop should never behave.
        o.visible_shadow = False

    bpy.ops.mesh.primitive_circle_add(vertices=28, radius=0.135, fill_type='NGON',
                                      location=(0.60, WIN_Y + 2.45, 1.70))
    mn = bpy.context.object; mn.rotation_euler = (math.radians(90), 0, 0)
    look.put(mn, look.mat('moon', (1, 1, 1, 1), 1.0, emit=(0.94, 0.92, 0.86, 1), strength=0.92))

    # --- the four pots on the sill, and the can pouring into the first
    soil_m = look.textured('potsoil', look.SOIL, 'farm_soil_diff_1k.jpg',
                           strength=0.75, scale=8.0, rough=0.96)
    A = lambda n, f: os.path.join(ASSETS, n, f)
    # The spiky succulent read as agave against plates full of broad leaves.
    # These assets build each leaf as an alpha card, so the leaf material has to
    # keep that one texture or every leaf becomes a solid rectangle.
    leaf1 = look.leaf_mat('leaf1', look.LEAF, A('potted_plant_01', 'potted_plant_01_leaves_alpha_1k.png'))
    leaf2 = look.leaf_mat('leaf2', look.LEAF, A('potted_plant_02', 'potted_plant_02_leaves_alpha_1k.png'))
    P1 = [('_leaves', leaf1), ('_stem', M['stem']), ('_pebbles', soil_m), ('_pot', M['terra'])]
    P2 = [('_leaves', leaf2), ('_dirt', soil_m), ('_pot', M['terra'])]
    PM = [('_pot', M['terra']), ('_dirt', soil_m), ('_ground', soil_m),
          ('_plant', M['leaf']), ('leaves', M['leaf']), ('leaf', M['leaf'])]
    # Modelled here rather than appended. potted_plant_01 is a footed goblet and
    # its leaves are alpha cards, so against the plates' plain tapered pots and
    # broad foliage it read as a different prop in every shot that showed it.
    for i, x in enumerate(POTS_X):
        sc_i = 0.92 + 0.10 * ((i * 7) % 3) / 2.0
        facet_pot((x, 0.985, SILL_Z), top_r=0.062 * sc_i, bot_r=0.047 * sc_i,
                  h=0.108 * sc_i, m=M['terra'])
        leafy_plant((x, 0.985, SILL_Z + 0.094 * sc_i), M['leaf'], M['stem'],
                    scale=0.80 * sc_i, seed=i)
    props.watering_can((POTS_X[0] - 0.20, 0.95, SILL_Z + 0.055),
                       M['metal'], M['metal'], rot_z=-0.28)
    _box((POTS_X[0] - 0.012, 0.975, SILL_Z + 0.075), (0.006, 0.006, 0.105),
         look.mat('water', look.srgb('#8FA6B4'), 0.2))

    # --- the table and what is on it
    _box((0.42, 0.06, TABLE_Z - 0.013), (1.34, 0.78, 0.026), M['wood'])
    for sx in (-0.60, 0.60):
        for sy in (-0.33, 0.33):
            _box((0.42 + sx, 0.06 + sy, (TABLE_Z - 0.03)/2), (0.05, 0.05, TABLE_Z - 0.03), M['wood'])
    # the scale, with the same pot standing on it
    props.kitchen_scale((0.30, 0.12, TABLE_Z), M['plastic'], M['glassblack'],
                        lit_m=look.mat('readout', (0, 0, 0, 1), 1.0,
                                       emit=look.srgb('#9FD8C4'), strength=0.75))
    facet_pot((0.30, 0.13, TABLE_Z + 0.0345), top_r=0.078, bot_r=0.059, h=0.132,
              m=M['terra'])
    leafy_plant((0.30, 0.13, TABLE_Z + 0.1535), M['leaf'], M['stem'], scale=1.45, seed=9)
    # the cut pot, the depth shot, on the same table
    # The cut pot belongs to two shots and was standing in every other one: a
    # pot sliced open in the middle of `scale-table` is not a continuity detail,
    # it is a prop nobody struck between setups. Recorded here so the renderer
    # can hide it where it does not belong.
    _before = set(bpy.data.objects)
    facet_pot((-0.18, 0.10, TABLE_Z), m=M['terra'], cut=True)
    leafy_plant((-0.18, 0.10, TABLE_Z + 0.104), M['leaf'], M['stem'], scale=0.9, seed=4)
    # root_cluster_01 is dropped. It was appended to give the roots beat real
    # geometry instead of the fourteen generated tubes, and flat-shaded at this
    # scale it renders as a crumpled beige sheet that covers the soil column the
    # depth and roots shots exist to show. The tubes are cruder and read as
    # roots, which is the only thing being asked of them.
    # Only the objects that are actually rendered. The boolean knife lives in
    # here too and is hidden on purpose, so a blanket unhide for the depth and
    # roots shots parked a 0.8 m cube between the camera and the pot and
    # rendered both frames solid white.
    CUT_POT.clear()
    CUT_POT.extend(o.name for o in bpy.data.objects
                   if o not in _before and not o.hide_render)
    # A pendant over the table, modelled, not an appended desk lamp.
    #
    # desk_lamp_arm_01 was fought for a long time: it is rigged, its shade sits
    # 0.9 m from its own origin, and four separate bugs came out of trying to
    # place it. All of that was effort spent on the wrong prop. Every plate
    # shows a wide shade hanging low over the table, which is one cone, one
    # disc and a flex, and it is the single biggest object in three of the
    # seven shots. It also puts the glow exactly where the key light already
    # is, so nothing has to be reconciled.
    LAMP_AT = (0.60, 0.24, 1.26)
    # Not near-black. The shade is the biggest object in three shots and at
    # #241E1A it read as a hole punched in the window behind it.
    shade_m = look.mat('shade', look.srgb('#4A3B2F'), 0.9)
    bpy.ops.mesh.primitive_cone_add(vertices=28, radius1=0.175, radius2=0.052,
                                    depth=0.105, end_fill_type='NOTHING',
                                    location=(LAMP_AT[0], LAMP_AT[1], LAMP_AT[2] + 0.052))
    shade = look.put(bpy.context.object, shade_m)
    sm = shade.modifiers.new('s', 'SOLIDIFY'); sm.thickness = 0.005; sm.offset = 1.0
    # the lit underside, which is the warm ellipse the plates all show
    # Smaller and dimmer than the shade's mouth. At full width and strength 2.6
    # it rendered as a flat white ellipse with the dark shade invisible behind
    # it, which is the one gradient the plates allow turned into a sticker.
    bpy.ops.mesh.primitive_circle_add(vertices=28, radius=0.120, fill_type='NGON',
                                      location=(LAMP_AT[0], LAMP_AT[1], LAMP_AT[2] + 0.018))
    look.put(bpy.context.object, look.mat('shadeglow', (0, 0, 0, 1), 1.0,
                                          emit=look.LAMP + (1,), strength=0.9))
    # the bulb, a little below the rim so it reads as a source, not a panel
    bpy.ops.mesh.primitive_uv_sphere_add(segments=18, ring_count=10, radius=0.042,
                                         location=(LAMP_AT[0], LAMP_AT[1], LAMP_AT[2] + 0.034))
    look.put(bpy.context.object, look.mat('bulb', (0, 0, 0, 1), 1.0,
                                          emit=(1.0, 0.86, 0.66, 1), strength=7.0),
             smooth=True)
    _box((LAMP_AT[0], LAMP_AT[1], LAMP_AT[2] + 0.105 + 0.42),
         (0.008, 0.008, 0.84), shade_m)                       # flex to the ceiling
    lamp_root = None
    # On the sill, which is where the plate has them: a stack to the right of
    # the sheet, reading as the near edge of the windowsill. They were on the
    # table, out of every frame; then briefly on the ledger shelf, where they
    # sat against the sheet as an unreadable blob.
    append('book_encyclopedia_set_01', (-0.74, 0.985, 0.962), scale=0.075, rot_z=0.35,
           mats=[('_paper', look.mat('pages', look.srgb('#9B8D74'), 0.95))],
           default=look.mat('book', look.srgb('#43342६'.replace('६','6')), 0.88))
    # one mug beside the phone, not a whole service: the set's origin is at the
    # middle of a 0.9 m spread, so appending it whole scatters crockery.
    # Left of the pot, not right of it. Both mug and phone sat out at x 0.76 and
    # 0.86, so the scale-table camera framed the pot with empty table beside it
    # while the plate has the mug on one side and the phone on the other. The
    # room is the authority on where things are, but when it disagrees with
    # every plate about composition it is the room that is wrong.
    # Back to the left of the scale, and the council was right the first time.
    #
    # It was moved to 0.32 so one position could be visible from both table
    # cameras, and the composition lens said that was exactly what welded those
    # two shots into one framing. It is now also fatal: sitting between the
    # camera and the scale, it blocks the move's closing shot completely, from
    # every angle tried. A continuous move forces one mug position, so the
    # position should serve the move rather than split the difference between
    # two stills that no longer exist on their own.
    props.mug((0.035, -0.115, TABLE_Z), M['mug'],
              look.mat('coffee', look.srgb('#241509'), 0.55), rot_z=2.4)

    append('trowel_01', (0.74, 0.96, SILL_Z + 0.006), scale=0.55, rot_z=1.25,
           default=look.mat('tool', look.srgb('#3A342C'), 0.62))
    # phone, face up, screen off
    props.phone((0.66, -0.26, TABLE_Z), M['frame'], M['glassblack'], rot_z=-0.24)

    # --- foreground dressing.
    #
    # All five review lenses reached this independently and the observal
    # recording confirms it from a third direction: every plate breaks a frame
    # edge with a near, dark object and six of our seven renders break none. It
    # is what makes an empty half of frame read as deliberate rather than
    # unfinished, and it is the cheapest thing on the list.
    #
    # Placed against the camera PATH rather than against any one shot, at y
    # -0.4 to -0.6, which is in front of the table cameras and between them and
    # the sill. Nothing here is a new kind of object: the room already contains
    # pots, plants and books, and a prop that only exists to crop an edge would
    # be the kind of thing a viewer notices.
    facet_pot((-0.17, -0.52, TABLE_Z), top_r=0.066, bot_r=0.050, h=0.114, m=M['terra'])
    leafy_plant((-0.17, -0.52, TABLE_Z + 0.108), M['leaf'], M['stem'], scale=1.15, seed=21)
    append('book_encyclopedia_set_01', (0.86, -0.42, TABLE_Z), scale=0.082, rot_z=-0.6,
           mats=[('_paper', look.mat('pages2', look.srgb('#8B7E68'), 0.95))],
           default=look.mat('book2', look.srgb('#3B2F26'), 0.9))

    # --- the ledger, pinned to the left pier
    _box((-0.86, 1.00, 1.30), (0.42, 0.20, 0.022), M['wood'])          # shelf by the ledger
    # Moved right and shrunk. At x -0.96 the second pot stood directly under
    # the sheet and its leaves covered the last four rows, which is the half of
    # the page that makes the point.
    facet_pot((-0.80, 1.00, 1.311), top_r=0.052, bot_r=0.040, h=0.090, m=M['terra'])
    leafy_plant((-0.80, 1.00, 1.389), M['leaf'], M['stem'], scale=0.60, seed=11)
    facet_pot((-0.665, 0.99, 1.311), top_r=0.046, bot_r=0.035, h=0.080, m=M['terra'])
    leafy_plant((-0.665, 0.99, 1.380), M['leaf'], M['stem'], scale=0.52, seed=12)
    # The sheet is a textured plane now, not a slab with thin boxes stacked on
    # it for rows and solid bars for words. At render scale those read as a
    # ruled but empty page, and the beat this shot carries is the same word
    # written seven times. tools/blender/make_ledger.py draws it.
    # Thin in Y, not in X. The slab was 0.012 thick along x, which is how you
    # model paper stuck to a side reveal, but it hangs on the pier's
    # room-facing surface. The ledger camera therefore met it at 52 degrees off
    # square and the page foreshortened into a vertical strip, which read as a
    # bookmark rather than a sheet of paper.
    # Floating, and the numbers say exactly why: the sheet's bottom edge was at
    # z 1.3175 and the shelf top at 1.311, six millimetres apart, so it read as
    # a card propped on a shelf rather than paper pinned to a wall. Its back
    # face was also only 3 mm into the pier. Lifted clear of the shelf and
    # pressed flat against the wall.
    led = _box((-1.06, 1.0075, 1.545), (0.215, 0.008, 0.285), M['paper'])
    led.rotation_euler = (0, 0, math.radians(-4))
    sheet_png = os.path.join(ASSETS, 'ledger-sheet.png')
    if os.path.exists(sheet_png):
        bpy.ops.mesh.primitive_plane_add(size=1.0, location=(-1.06, 1.0025, 1.545))
        face = bpy.context.object
        # One quarter turn about X is all it needs: that sends the plane's
        # normal to -y, which is the way the pier faces, and its own up to world
        # z. The plane's X is then the page's width and its Y the page's height,
        # which is what the image expects. The small yaw is the tilt the paper
        # already has.
        face.rotation_euler = (math.radians(90), 0, math.radians(-4))
        face.scale = (0.215, 0.285, 1.0)
        pm = look.mat('ledgerpaper', look.srgb('#D9CDB2'), 0.95)
        nt = pm.node_tree
        img = nt.nodes.new('ShaderNodeTexImage')
        img.image = bpy.data.images.load(sheet_png)
        img.interpolation = 'Cubic'
        nt.links.new(img.outputs['Color'],
                     nt.nodes['Principled BSDF'].inputs['Base Color'])
        look.put(face, pm)
    # The divider between the date column and the word column is in the texture
    # now, along with the rows; it was the last piece of the sheet still
    # modelled and it rendered as a hairline floating off the paper.
    #
    # The pin is not, because it is the one part of this that has thickness: the
    # plate shows a brass head catching the lamp and throwing a small shadow
    # down the sheet, which a painted dot cannot do.
    bpy.ops.mesh.primitive_uv_sphere_add(segments=14, ring_count=8, radius=0.0075,
                                         location=(-1.058, 0.9985, 1.6735))
    look.put(bpy.context.object, look.mat('pin', look.srgb('#B98B3C'), 0.42),
             smooth=True)

    soil_macro()
    # The macro set gets its own key, warm and from the right like the room's,
    # because the one thing it does have to share is the light.
    # Grazing, not overhead. The plate's soil runs 6.5x from its dark quarter to
    # its bright one and the render ran 5.1x, which is a lump-shadow problem:
    # a steep key lights the tops of the crumb and the sides equally, and only a
    # shallow one makes each lump cast across its neighbour.
    bpy.ops.object.light_add(type='AREA', location=(1.32, -1.00, -4.46))
    ml = bpy.context.object
    ml.data.energy, ml.data.size, ml.data.color = 128.0, 0.40, look.LAMP
    look.aim(ml, (0.0, 0.0, -5.0))
    # and a dim fill down the channel itself. The banks shadow their own floor
    # so completely that the trench rendered as a black slot, which is a hole in
    # the picture rather than a hole in the soil.
    bpy.ops.object.light_add(type='AREA', location=(0.0, -0.30, -4.62))
    tf = bpy.context.object
    # 9 W lit the whole set and took the frame from 37% dark to 0.7%, clipping
    # 5.6% of it. This is a fill for one slot, not a second key.
    tf.data.energy, tf.data.size, tf.data.color = 3.2, 0.14, look.LAMP
    look.aim(tf, (0.0, 0.0, -5.18))

    # --- light. One lamp, clamped right, plus the night outside. 58% of the
    # plates sit below V 0.18 and only 6% above 0.60: this is one source.
    # Put the key inside the lamp's own head so the shade restricts it. A bare
    # area light floating over the room lights every surface equally, which is
    # why the two close shots blew out: the plates show a POOL of light with a
    # dark surround, and the thing that makes a pool is the fixture.
    bpy.context.view_layer.update()
    # The shade is solid and would swallow its own key, so the fixture is lit
    # and visible but casts no shadow. That is the ordinary way to put a
    # practical inside a modelled lamp without modelling its aperture.
    #
    # What used to be here was forty lines of correcting an appended, rigged
    # desk lamp whose head sat 0.9 m from its own origin and kept landing
    # through the right-hand wall. The pendant is modelled at the key's own
    # position now, so there is nothing left to reconcile.
    for o in bpy.data.objects:
        if o.type == 'MESH' and o.name.lower().startswith(('cone', 'circle', 'sphere')):
            pass
    for nm in ('shade', 'shadeglow', 'bulb'):
        for o in bpy.data.objects:
            if o.type == 'MESH' and o.data.materials and o.data.materials[0] \
                    and o.data.materials[0].name.startswith(nm):
                o.visible_shadow = False
    WANT_HEAD = Vector((0.60, 0.24, 1.26))
    hp = WANT_HEAD
    bpy.ops.object.light_add(type='AREA', location=(hp.x, hp.y, hp.z - 0.035))
    k = bpy.context.object
    k.data.energy, k.data.size, k.data.color = 20.0, 0.10, look.LAMP
    look.aim(k, (0.26, 0.20, TABLE_Z + 0.01))
    bpy.ops.object.light_add(type='AREA', location=(0.86, 0.94, 1.14))
    k2 = bpy.context.object
    k2.data.energy, k2.data.size, k2.data.color = 11.0, 0.10, look.LAMP
    look.aim(k2, (-0.90, 0.99, SILL_Z + 0.05))
    # A bounce for the ledger wall, and the reason it is allowed.
    #
    # Measured, the ledger frame is already right everywhere except its subject:
    # the room around the sheet sits at 0.232 against the plate's 0.216, and the
    # sheet itself at 0.231 against 0.562. It is 1.9 m from the only lamp, so
    # inverse square puts it in the bottom lighting step and no global exposure
    # change can lift it without blowing out the table.
    #
    # This is not a second lamp. It is dim, it is warm, it casts no shadow, and
    # it stands where light actually would bounce off the window reveal and the
    # sill back onto that pier. One visible fixture is the rule; one bounce is
    # how a room works.
    # A SPOT, not an area. An area light at 7.5 W lit the whole room and clipped
    # 10.7% of the frame; turning it down to 0.55 W then left the sheet at 0.377
    # against the plate's 0.562, because an area light that is dim enough not to
    # spill is too dim to do the job. A narrow cone puts the light on the sheet
    # and nowhere else, which is what the measurement actually asked for.
    # In FRONT of the sheet, not beside it. At x -0.70 against the sheet's
    # -1.06 the cone arrived at a graze, so it blew out the sheet's right edge
    # to 255 while the face it was meant to light stayed at 0.37, and it threw
    # a visible theatrical pool on the pier behind. Nearly head on, wider, and
    # softer at the edge: a bounce has no rim.
    # An area light after all, but close and in front rather than far and to
    # the side. The spot solved the graze and introduced its own tell: a cone
    # edge, printing a circular pool on the pier behind the sheet that no
    # bounced light has. A small source half a metre away falls off by inverse
    # square instead, which is the same gradient without the rim.
    bpy.ops.object.light_add(type='AREA', location=(-1.00, 0.52, 1.47))
    lb = bpy.context.object
    # 2.6 W half a metre away put the sheet at 0.866 and clipped a tenth of the
    # frame. Solved rather than nudged: the target is the plate's 0.562, so in
    # linear terms 0.562^2.2 over 0.866^2.2 is 0.39, and 2.6 x 0.39 is 1.0.
    lb.data.energy, lb.data.size, lb.data.color = 1.0, 0.22, look.LAMP
    lb.data.use_shadow = False
    look.aim(lb, (-1.05, 1.00, 1.46))

    bpy.ops.object.light_add(type='AREA', location=(-0.18, -0.52, 0.95))
    cf = bpy.context.object
    cf.data.energy, cf.data.size, cf.data.color = 4.5, 0.12, look.LAMP
    look.aim(cf, (-0.18, 0.06, TABLE_Z + 0.055))
    bpy.ops.object.light_add(type='AREA', location=(-0.10, 2.60, 2.10))
    f = bpy.context.object
    f.data.energy, f.data.size, f.data.color = 34.0, 2.2, look.MOON
    look.aim(f, (-0.10, 1.00, 1.30))
    return sc, M


# (name, camera loc, aim, lens, portrait lens, f-stop)
#
# The f-stop is per shot and most of them have one now. Depth of field was off
# everywhere, because look.camera only enables it when passed a stop and nothing
# passed one, so a 40 cm close-up of soil was rendered with the same infinite
# sharpness as the back wall. That is most of what separates a photograph of a
# room from a diagram of one.
#
# Wider stop on the close shots, where a few centimetres of focus is the whole
# effect, and none at all on the two wide room shots, where everything in frame
# is genuinely meant to be legible.
SHOTS = {
 'ledger':        ((-0.34, 0.10, 1.60), (-1.03, 0.99, 1.548), 31, 26, 4.0),
 'shelf-evening': ((-0.30, -0.28, 1.21), (-0.10, 0.98, 1.03), 35, 28, None),
 'finger-test':   ((0.02, 0.50, 1.175), (-0.245, 0.975, 1.052), 42, 35, 2.8),
 'depth':         ((-0.60, -0.62, 0.86), (-0.12, 0.12, 0.825), 45, 38, 3.5),
 'roots':         ((0.0, -0.56, -4.44), (0.0, 0.0, -5.06), 40, 34, 2.2),
 'scale-table':   ((0.20, -1.00, 1.26), (0.34, 0.15, 0.88), 31, 26, None),
 'phone-closeup': ((0.70, -0.52, 0.800), (0.62, -0.17, 0.762), 33, 27, 2.2),
}


def set_shot(name, portrait=False):
    loc, aim, lens, plens, fstop = SHOTS[name]
    cam = look.camera(loc, aim, lens=plens if portrait else lens, fstop=fstop)
    cam.data.sensor_fit = 'HORIZONTAL'
    return cam
