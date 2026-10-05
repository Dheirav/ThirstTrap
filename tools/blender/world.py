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
import look, parts, scene as sc_mod
from mathutils import Vector

SILL_Z, TABLE_Z, WIN_Y = 0.95, 0.755, 1.06

# Objects that belong to the depth and roots shots only. See build().
CUT_POT = []
POTS_X = (-0.52, -0.21, 0.10, 0.41)


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
    soil = parts._soil_grid(loc[0], loc[1], loc[2] + h - 0.030, top_r - 0.010, n=96)
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


def leaf(m, base, aim_xy, length, width=0.052, rise=1.25, droop=0.60):
    ax, ay = aim_xy; n = math.hypot(ax, ay) or 1.0
    ax, ay = ax/n, ay/n; px, py = -ay, ax
    pts, faces, SEG = [], [], 5
    for i in range(SEG + 1):
        t = i / SEG
        z = base[2] + rise*length*t - droop*length*t*t
        cx, cy = base[0] + ax*length*t, base[1] + ay*length*t
        w = width * math.sin(math.pi * min(t*1.12, 1.0)) * 0.5
        pts += [(cx+px*w, cy+py*w, z-0.003*(1-t)), (cx, cy, z+0.005*math.sin(math.pi*t)),
                (cx-px*w, cy-py*w, z-0.003*(1-t))]
    for i in range(SEG):
        a = i*3; faces += [(a, a+1, a+4, a+3), (a+1, a+2, a+5, a+4)]
    me = bpy.data.meshes.new('leaf'); me.from_pydata(pts, [], faces); me.update()
    ob = bpy.data.objects.new('leaf', me); bpy.context.collection.objects.link(ob)
    ob.modifiers.new('s', 'SOLIDIFY').thickness = 0.0015
    return look.put(ob, m)


def leafy_plant(at, leaf_m, stem_m, scale=1.0, seed=0):
    """The one plant this room grows, in a loose rosette of broad leaves.

    The old `plant` put six leaves in a flat fan, which read as a sparse twig
    at any distance, and the sill used a Poly Haven asset whose pot is a footed
    goblet. Neither matches the plates, where every pot holds the same broad
    rounded leaves on short upright stalks, and the pot is a plain taper.

    Two tiers rather than one ring: the outer leaves longer and dropping, the
    inner ones shorter and more upright. A single ring at one length reads as a
    paper fan, because real foliage overlaps itself and that overlap is most of
    what makes it look like a plant.
    """
    rnd = random.Random(seed)
    # Lifted clear of the soil. The leaves used to radiate from the soil surface
    # itself, so they closed over the pot mouth and no shot ever saw dirt, which
    # is a problem in a set of pictures about how wet the dirt is. A short rise
    # is also simply what the plant looks like.
    at = (at[0], at[1], at[2] + 0.022 * scale)
    outer, inner = 7, 5
    for i in range(outer):
        a = (i / outer) * math.tau + rnd.uniform(-0.18, 0.18)
        ln = (0.098 + rnd.uniform(-0.012, 0.014)) * scale
        leaf(leaf_m, at, (math.cos(a), math.sin(a)), ln,
             width=(0.062 + rnd.uniform(-0.006, 0.008)) * scale,
             rise=1.05 + rnd.uniform(-0.12, 0.12), droop=0.74 + rnd.uniform(-0.1, 0.1))
    for i in range(inner):
        a = (i / inner) * math.tau + 0.5 + rnd.uniform(-0.2, 0.2)
        ln = (0.066 + rnd.uniform(-0.010, 0.012)) * scale
        leaf(leaf_m, (at[0], at[1], at[2] + 0.012 * scale),
             (math.cos(a), math.sin(a)), ln,
             width=(0.048 + rnd.uniform(-0.005, 0.006)) * scale,
             rise=1.55 + rnd.uniform(-0.15, 0.15), droop=0.42 + rnd.uniform(-0.08, 0.08))
    for i in range(4):
        a = (i / 4) * math.tau + 0.3
        d = (math.cos(a), math.sin(a))
        bpy.ops.mesh.primitive_cone_add(
            vertices=6, radius1=0.0034 * scale, radius2=0.0016 * scale,
            depth=0.055 * scale,
            location=(at[0] + d[0] * 0.009, at[1] + d[1] * 0.009, at[2] + 0.024 * scale))
        look.put(bpy.context.object, stem_m)


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
    soil_m = look.mat('macrosoil', look.srgb('#32241A'), 0.98)
    # #CFC0A4 clipped to white at this gain. The plate's roots are pale against
    # dark crumb, which is a value relationship, not a bright material.
    root_m = look.mat('macroroot', look.srgb('#9C8A6E'), 0.88)

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
                                    location=(at[0], at[1], at[2] - 0.235))
    fl = look.put(bpy.context.object, soil_m, smooth=True)
    fd = fl.modifiers.new('crumb', 'DISPLACE')
    fd.texture = tex
    fd.strength = 0.07

    for side in (-1, 1):
        # 0.52 apart with a size of 1.6 means the two banks OVERLAP across the
        # middle, so there was no channel at all: the shot was a flat field of
        # dirt. They have to be further apart than half their own width.
        bpy.ops.mesh.primitive_grid_add(x_subdivisions=160, y_subdivisions=160, size=1.6,
                                        location=(at[0] + side * 0.95, at[1], at[2]))
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
    peb_m = look.mat('macrocrumb', look.srgb('#3E2D20'), 0.98)
    for _ in range(260):
        side = -1 if rnd.random() < 0.5 else 1
        # Clear of the channel lip. Projecting the gap showed it occupying 15%
        # of the frame all along, so it was never missing, it was being covered:
        # pebbles crowded its edges and forty roots crossed it.
        px = at[0] + side * rnd.uniform(0.22, 1.45)
        py = at[1] + rnd.uniform(-0.75, 0.75)
        pz = at[2] + 0.055 + rnd.uniform(-0.03, 0.05)
        r = rnd.uniform(0.012, 0.045)
        bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1, radius=r,
                                              location=(px, py, pz))
        o = bpy.context.object
        o.scale = (rnd.uniform(0.7, 1.3), rnd.uniform(0.7, 1.3), rnd.uniform(0.4, 0.8))
        o.rotation_euler = (rnd.uniform(0, 3), rnd.uniform(0, 3), rnd.uniform(0, 3))
        look.put(o, peb_m if rnd.random() < 0.6 else soil_m)

    # roots: long ones running across the banks, short torn ends at the channel
    for i in range(28):
        y0 = rnd.uniform(-0.62, 0.62)
        x0 = rnd.uniform(-1.2, -0.30) if i % 2 else rnd.uniform(0.30, 1.2)
        # Reach further so more of them bridge the channel: the torn ends over
        # the gap are what the shot is about.
        dx = rnd.uniform(0.34, 0.95) * (1 if x0 < 0 else -1)
        # A real bend, not jitter. Random offsets per point give a zigzag,
        # which is what the first version rendered: straight pale sticks with
        # kinks. One low-frequency curve along the whole length plus a little
        # noise is what reads as a root.
        spine = []
        bend = rnd.uniform(-0.16, 0.16)
        ph = rnd.uniform(0, math.tau)
        for t in range(9):
            f = t / 8
            spine.append((at[0] + x0 + dx * f + rnd.uniform(-0.012, 0.012),
                          at[1] + y0 + bend * math.sin(math.pi * f + ph * 0.2)
                          + rnd.uniform(-0.018, 0.018),
                          at[2] + 0.085 + 0.035 * math.sin(math.pi * f) * rnd.uniform(0.3, 1.0)
                          + rnd.uniform(-0.012, 0.012)))
        # Thinner. At 0.0045 to 0.0095 they rendered as ribbons laid over the
        # soil, and the plate's are fine pale threads THROUGH it.
        r = rnd.uniform(0.0022, 0.0052)
        # one radius per spine point: tube() indexes them together and six
        # points with five radii walks off the end
        taper = [r * v for v in (1.0, 0.95, 0.88, 0.80, 0.70, 0.58, 0.45, 0.33, 0.22)]
        tb = parts.tube(spine, taper, name='macroroot', seg=7)
        look.put(tb, root_m, smooth=True)
        # the torn end, pointing into the channel
        if rnd.random() < 0.55:
            tip = spine[-1]
            for _ in range(rnd.randint(2, 3)):
                a = rnd.uniform(-0.9, 0.9)
                sp = [tip, (tip[0] + 0.05 * math.cos(a), tip[1] + 0.05 * math.sin(a),
                            tip[2] + rnd.uniform(-0.01, 0.02))]
                look.put(parts.tube(sp, [r * 0.26, 0.0008], name='macroroot', seg=3),
                         root_m, smooth=True)


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


def place_hand(at, yaw=0.0, pitch=-0.62, into=0.012):
    """One finger pushed into the soil at `at`, with a forearm leaving frame.

    A whole scripted hand reads as a lay figure, which is why parts.build_finger
    crops to one finger: the rest of the hand never has to survive being built
    out of primitives. The arm exists only so the finger is attached to someone.
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
        wall=look.textured('wall', look.srgb('#2B2219'), 'rough_plaster_brick_diff_1k.jpg',
                           strength=0.70, scale=0.6),
        pier=look.mat('pier', look.srgb('#33281D'), 0.92),
        sill=look.textured('sill', look.srgb('#2C2118'), 'wood_table_001_diff_1k.jpg',
                           strength=0.80, scale=2.2),
        wood=look.textured('wood', look.srgb('#3B2919'), 'wood_table_001_diff_1k.jpg',
                           strength=0.85, scale=1.4),
        frame=look.mat('frame', look.srgb('#1E1812'), 0.85),
        terra=look.textured('terra', look.TERRA, 'terracotta_floor_tiles_diff_1k.jpg',
                            strength=0.55, scale=3.0, rough=0.80),
        leaf=look.mat('leaf', look.LEAF, 0.66),
        stem=look.mat('stem', look.STEM, 0.60),
        metal=look.mat('metal', look.srgb('#6E7276'), 0.42, ),
        paper=look.mat('paper', look.srgb('#CDBE9E'), 0.92),
        plastic=look.mat('plastic', look.srgb('#8C867A'), 0.60),
        glassblack=look.mat('screen', look.srgb('#0A0B0D'), 0.18),
        mug=look.mat('mug', look.srgb('#796F60'), 0.76),
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
    sky = look.mat('sky', look.SKY, 0.9, emit=look.srgb('#1E2A38'), strength=0.42)
    g = _box((-0.17, WIN_Y + 2.60, 1.50), (9.0, 0.02, 5.0), sky)
    for mx in (-0.17,):
        _box((mx, WIN_Y - 0.02, 1.50), (0.022, 0.03, 1.08), M['frame'])
    _box((-0.17, WIN_Y - 0.02, 1.50), (1.92, 0.03, 0.022), M['frame'])
    city = look.mat('city', look.srgb('#141A22'), 0.95)
    for x, h in ((-1.30, 0.62), (-0.78, 0.95), (-0.34, 0.45), (0.14, 0.78), (0.66, 0.52),
                 (1.15, 0.70)):
        _box((x, WIN_Y + 1.90, 0.95 + h/2), (0.42, 0.06, h), city)
    # The plate's window is a dark field with small warm lights scattered in it,
    # and that scatter is most of what makes it read as a city rather than a
    # painted panel. Six windows was too few to read as anything.
    litw = look.mat('litwin', (0, 0, 0, 1), 1.0, emit=look.srgb('#C98A3A'), strength=4.0)
    litc = look.mat('litwin2', (0, 0, 0, 1), 1.0, emit=look.srgb('#9FB4C6'), strength=2.2)
    WINDOWS = [(-1.38, 1.18), (-1.22, 1.42), (-1.30, 1.30), (-0.92, 1.24),
               (-0.80, 1.30), (-0.74, 1.52), (-0.86, 1.68), (-0.60, 1.14),
               (-0.42, 1.36), (-0.28, 1.22), (-0.36, 1.08), (0.02, 1.46),
               (0.18, 1.38), (0.10, 1.60), (0.26, 1.20), (0.52, 1.30),
               (0.70, 1.18), (0.62, 1.44), (0.84, 1.26), (1.06, 1.38),
               (1.20, 1.16), (1.12, 1.56)]
    for i, (x, z) in enumerate(WINDOWS):
        _box((x, WIN_Y + 1.86, z), (0.026, 0.01, 0.038), litw if i % 3 else litc)
    bpy.ops.mesh.primitive_circle_add(vertices=28, radius=0.135, fill_type='NGON',
                                      location=(0.60, WIN_Y + 2.45, 1.70))
    mn = bpy.context.object; mn.rotation_euler = (math.radians(90), 0, 0)
    look.put(mn, look.mat('moon', (1, 1, 1, 1), 1.0, emit=(0.95, 0.96, 1.0, 1), strength=5.0))

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
    can = _box((POTS_X[0] - 0.19, 0.95, SILL_Z + 0.20), (0.115, 0.095, 0.105), M['metal'])
    can.rotation_euler = (0, math.radians(-26), 0)
    sp = _box((POTS_X[0] - 0.085, 0.95, SILL_Z + 0.175), (0.115, 0.022, 0.022), M['metal'])
    sp.rotation_euler = (0, math.radians(-26), 0)
    _box((POTS_X[0] - 0.012, 0.975, SILL_Z + 0.075), (0.006, 0.006, 0.105),
         look.mat('water', look.srgb('#8FA6B4'), 0.2))

    # --- the table and what is on it
    _box((0.42, 0.06, TABLE_Z - 0.013), (1.34, 0.78, 0.026), M['wood'])
    for sx in (-0.60, 0.60):
        for sy in (-0.33, 0.33):
            _box((0.42 + sx, 0.06 + sy, (TABLE_Z - 0.03)/2), (0.05, 0.05, TABLE_Z - 0.03), M['wood'])
    # the scale, with the same pot standing on it
    _box((0.30, 0.12, TABLE_Z + 0.022), (0.215, 0.185, 0.044), M['plastic'])
    _box((0.30, 0.12, TABLE_Z + 0.048), (0.195, 0.165, 0.010), M['plastic'])
    _box((0.30, 0.035, TABLE_Z + 0.030), (0.072, 0.012, 0.024), M['glassblack'])
    facet_pot((0.30, 0.13, TABLE_Z + 0.053), top_r=0.078, bot_r=0.059, h=0.132,
              m=M['terra'])
    leafy_plant((0.30, 0.13, TABLE_Z + 0.172), M['leaf'], M['stem'], scale=1.45, seed=9)
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
    append('book_encyclopedia_set_01', (-0.52, -0.14, TABLE_Z), scale=0.075, rot_z=0.5,
           mats=[('_paper', look.mat('pages', look.srgb('#9B8D74'), 0.95))],
           default=look.mat('book', look.srgb('#43342६'.replace('६','6')), 0.88))
    # one mug beside the phone, not a whole service: the set's origin is at the
    # middle of a 0.9 m spread, so appending it whole scatters crockery.
    # Left of the pot, not right of it. Both mug and phone sat out at x 0.76 and
    # 0.86, so the scale-table camera framed the pot with empty table beside it
    # while the plate has the mug on one side and the phone on the other. The
    # room is the authority on where things are, but when it disagrees with
    # every plate about composition it is the room that is wrong.
    # 0.32 rather than 0.02. Sweeping mug positions against BOTH table cameras
    # found this one visible in each with the widest margin; at 0.02 it suited
    # scale-table and fell outside phone-closeup entirely.
    bpy.ops.mesh.primitive_cylinder_add(vertices=22, radius=0.044, depth=0.098,
                                        location=(0.32, -0.06, TABLE_Z + 0.049))
    look.put(bpy.context.object, M['mug'])
    bpy.ops.mesh.primitive_torus_add(major_radius=0.036, minor_radius=0.0065,
                                     major_segments=20, minor_segments=8,
                                     location=(0.268, -0.06, TABLE_Z + 0.052),
                                     rotation=(math.radians(90), 0, 0))
    look.put(bpy.context.object, M['mug'])
    # and something in it. An empty cream cylinder reads as a paper cup, and
    # the dark disc is most of what makes the plate's mug a mug.
    bpy.ops.mesh.primitive_circle_add(vertices=22, radius=0.038, fill_type='NGON',
                                      location=(0.32, -0.06, TABLE_Z + 0.086))
    look.put(bpy.context.object, look.mat('coffee', look.srgb('#241509'), 0.55))
    append('trowel_01', (0.74, 0.96, SILL_Z + 0.006), scale=0.55, rot_z=1.25,
           default=look.mat('tool', look.srgb('#3A342C'), 0.62))
    # phone, face up, screen off
    ph = _box((0.63, -0.14, TABLE_Z + 0.006), (0.078, 0.158, 0.009), M['frame'])
    ph.rotation_euler = (0, 0, math.radians(-14))
    scr = _box((0.63, -0.14, TABLE_Z + 0.0112), (0.070, 0.148, 0.001), M['glassblack'])
    scr.rotation_euler = (0, 0, math.radians(-14))

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
    led = _box((-1.06, 1.002, 1.46), (0.215, 0.012, 0.285), M['paper'])
    led.rotation_euler = (0, 0, math.radians(-4))
    sheet_png = os.path.join(ASSETS, 'ledger-sheet.png')
    if os.path.exists(sheet_png):
        bpy.ops.mesh.primitive_plane_add(size=1.0, location=(-1.06, 0.9955, 1.46))
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
                                         location=(-1.058, 0.990, 1.5885))
    look.put(bpy.context.object, look.mat('pin', look.srgb('#B98B3C'), 0.42),
             smooth=True)

    soil_macro()
    # The macro set gets its own key, warm and from the right like the room's,
    # because the one thing it does have to share is the light.
    bpy.ops.object.light_add(type='AREA', location=(1.1, -0.9, -4.1))
    ml = bpy.context.object
    ml.data.energy, ml.data.size, ml.data.color = 34.0, 0.45, look.LAMP
    look.aim(ml, (0.0, 0.0, -5.0))

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
 'ledger':        ((-0.46, 0.26, 1.515), (-1.02, 0.99, 1.468), 40, 33, 4.0),
 'shelf-evening': ((-0.30, -0.28, 1.21), (-0.10, 0.98, 1.03), 35, 28, None),
 'finger-test':   ((0.02, 0.50, 1.175), (-0.245, 0.975, 1.052), 42, 35, 2.8),
 'depth':         ((-0.18, -0.60, 0.95), (-0.18, 0.10, 0.845), 55, 45, 3.5),
 'roots':         ((0.0, -0.56, -4.44), (0.0, 0.0, -5.06), 40, 34, 2.2),
 'scale-table':   ((0.26, -0.82, 1.12), (0.33, 0.16, 0.91), 33, 27, None),
 'phone-closeup': ((0.74, -0.52, 0.845), (0.47, 0.10, 0.818), 34, 28, 3.2),
}


def set_shot(name, portrait=False):
    loc, aim, lens, plens, fstop = SHOTS[name]
    cam = look.camera(loc, aim, lens=plens if portrait else lens, fstop=fstop)
    cam.data.sensor_fit = 'HORIZONTAL'
    return cam
