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
import bpy, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look, parts, scene as sc_mod
from mathutils import Vector

SILL_Z, TABLE_Z, WIN_Y = 0.95, 0.755, 1.06
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
        # Each plant asset names its soil differently: potted_plant_04 says
        # _dirt, potted_plant_01 says _pebbles. Matching only one of them meant
        # swapping the plant silently dropped the hand to table height, below
        # the sill and out of frame.
        if o.type != 'MESH' or not any(
                k in o.name.lower() for k in ('_dirt', '_pebbles', '_soil', '_ground')):
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
    skin = look.mat('skin', look.SKIN, 0.72)
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
    sky = look.mat('sky', look.SKY, 0.9, emit=look.srgb('#38495F'), strength=1.05)
    g = _box((-0.17, WIN_Y + 2.60, 1.50), (9.0, 0.02, 5.0), sky)
    for mx in (-0.17,):
        _box((mx, WIN_Y - 0.02, 1.50), (0.022, 0.03, 1.08), M['frame'])
    _box((-0.17, WIN_Y - 0.02, 1.50), (1.92, 0.03, 0.022), M['frame'])
    city = look.mat('city', look.srgb('#141A22'), 0.95)
    for x, h in ((-1.30, 0.62), (-0.78, 0.95), (-0.34, 0.45), (0.14, 0.78), (0.66, 0.52),
                 (1.15, 0.70)):
        _box((x, WIN_Y + 1.90, 0.95 + h/2), (0.42, 0.06, h), city)
    for x, z in ((-0.80, 1.30), (-0.74, 1.52), (0.18, 1.38), (0.70, 1.18)):
        _box((x, WIN_Y + 1.86, z), (0.030, 0.01, 0.045),
             look.mat('litwin', (1, 1, 1, 1), 1.0, emit=look.srgb('#C98A3A'), strength=3.0))
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
    for i, x in enumerate(POTS_X):
        append('potted_plant_01', (x, 0.985, SILL_Z), scale=0.185 + 0.012 * (i % 3),
               rot_z=1.1 * i, mats=P1, default=M['terra'])
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
    append('potted_plant_02', (0.30, 0.13, TABLE_Z + 0.053), scale=0.30,
           rot_z=2.1, mats=P2, default=M['terra'])
    # the cut pot, the depth shot, on the same table
    facet_pot((-0.18, 0.10, TABLE_Z), m=M['terra'], cut=True)
    plant((-0.18, 0.10, TABLE_Z + 0.104), M['leaf'], M['stem'], scale=0.9)
    # the roots beat is a macro into real root geometry rather than my 14 tubes
    rootm = look.mat('rootm', look.srgb('#A8906F'), 0.88)
    rc = append('root_cluster_01', (-0.18, 0.118, TABLE_Z + 0.048), scale=0.026,
                rot_z=0.4, default=rootm)
    scatter(rc, [((-0.205, 0.108, TABLE_Z + 0.030), 0.021, (0.5, 0.2, 1.9)),
                 ((-0.152, 0.122, TABLE_Z + 0.034), 0.019, (-0.4, 0.3, 3.4)),
                 ((-0.180, 0.100, TABLE_Z + 0.018), 0.024, (0.2, -0.3, 0.8)),
                 ((-0.196, 0.130, TABLE_Z + 0.062), 0.016, (1.1, 0.1, 5.0)),
                 ((-0.165, 0.112, TABLE_Z + 0.056), 0.017, (-0.7, 0.4, 2.6))])
    # the lamp the plates actually show, with the key light at its head
    lamp_root = append('desk_lamp_arm_01', (0.90, 0.33, TABLE_Z), scale=0.40, rot_z=-2.3,
           mats=[('lamp-head', look.mat('shadeglow', (1, 1, 1, 1), 1.0,
                                       emit=look.LAMP + (1,), strength=2.0))],
           default=look.mat('lampbody', look.srgb('#2A2420'), 0.55))
    append('book_encyclopedia_set_01', (-0.52, -0.14, TABLE_Z), scale=0.075, rot_z=0.5,
           mats=[('_paper', look.mat('pages', look.srgb('#9B8D74'), 0.95))],
           default=look.mat('book', look.srgb('#43342६'.replace('६','6')), 0.88))
    # one mug beside the phone, not a whole service: the set's origin is at the
    # middle of a 0.9 m spread, so appending it whole scatters crockery.
    bpy.ops.mesh.primitive_cylinder_add(vertices=22, radius=0.041, depth=0.094,
                                        location=(0.86, -0.17, TABLE_Z + 0.047))
    look.put(bpy.context.object, M['mug'])
    bpy.ops.mesh.primitive_torus_add(major_radius=0.034, minor_radius=0.006,
                                     major_segments=20, minor_segments=8,
                                     location=(0.906, -0.17, TABLE_Z + 0.050),
                                     rotation=(math.radians(90), 0, 0))
    look.put(bpy.context.object, M['mug'])
    append('trowel_01', (0.74, 0.96, SILL_Z + 0.006), scale=0.55, rot_z=1.25,
           default=look.mat('tool', look.srgb('#3A342C'), 0.62))
    # phone, face up, screen off
    ph = _box((0.76, -0.10, TABLE_Z + 0.006), (0.078, 0.158, 0.009), M['frame'])
    ph.rotation_euler = (0, 0, math.radians(-14))
    scr = _box((0.76, -0.10, TABLE_Z + 0.0112), (0.070, 0.148, 0.001), M['glassblack'])
    scr.rotation_euler = (0, 0, math.radians(-14))

    # --- the ledger, pinned to the left pier
    _box((-0.86, 1.00, 1.30), (0.42, 0.20, 0.022), M['wood'])          # shelf by the ledger
    append('potted_plant_04', (-0.78, 1.00, 1.311), scale=0.78, rot_z=1.3,
           mats=PM, default=M['terra'])
    append('potted_plant_04', (-0.96, 0.99, 1.311), scale=0.62, rot_z=-0.6,
           mats=PM, default=M['terra'])
    led = _box((-1.06, 1.00, 1.46), (0.012, 0.215, 0.285), M['paper'])
    led.rotation_euler = (0, 0, math.radians(4))
    rule = look.mat('rule', look.srgb('#4A4134'), 0.95)
    for i in range(7):
        z = 1.46 + 0.098 - i * 0.030
        r = _box((-1.0535, 1.00, z), (0.001, 0.185, 0.0014), rule)
        r.rotation_euler = (0, 0, math.radians(4))
        w = _box((-1.0535, 1.012 - 0.052, z + 0.009), (0.001, 0.052, 0.009), rule)
        w.rotation_euler = (0, 0, math.radians(4))
    hd = _box((-1.0535, 1.00, 1.46 + 0.126), (0.001, 0.185, 0.0030), rule)
    hd.rotation_euler = (0, 0, math.radians(4))
    _box((-1.0535, 1.00 + 0.018, 1.46 + 0.02), (0.001, 0.0016, 0.252), rule)

    # --- light. One lamp, clamped right, plus the night outside. 58% of the
    # plates sit below V 0.18 and only 6% above 0.60: this is one source.
    # Put the key inside the lamp's own head so the shade restricts it. A bare
    # area light floating over the room lights every surface equally, which is
    # why the two close shots blew out: the plates show a POOL of light with a
    # dark surround, and the thing that makes a pool is the fixture.
    bpy.context.view_layer.update()
    # The fixture was swallowing its own light: the key sits at the shade and the
    # shade is solid, so almost nothing escaped. Let the lamp be lit and visible
    # but stop it casting shadows, which is the usual way to put a practical
    # light inside a modelled fixture without modelling the aperture.
    for o in (lamp_root.children_recursive if hasattr(lamp_root, 'children_recursive')
              else [c for c in bpy.data.objects if c.parent == lamp_root]):
        if o.type == 'MESH':
            o.visible_shadow = False
    # An articulated lamp reaches 0.73 m from its base, so seating the BASE put
    # the head at x=1.63, straight through the right wall at 1.28: the key light
    # spent four renders outside the room. Place it by the head, which is the
    # part whose position actually matters, and let the base land where it may.
    head = next((o for o in bpy.data.objects if 'lamp-head' in o.name.lower()), None)
    WANT_HEAD = Vector((0.60, 0.24, 1.30))
    if head:
        # Iterate rather than assume one pass: matrix_world for a child of the
        # root can still be stale right after append re-seats it, so a single
        # correction is computed against the wrong starting point and leaves the
        # lamp a long way from where it was asked for.
        # Correct on the shade's GEOMETRY, not its object origin. This asset's
        # head mesh sits 0.9 m from its own origin, so aligning the origin put
        # the light correctly over the table while leaving the visible shade
        # back by the sill: one lamp, lighting one place and appearing in
        # another. The bounding-box centre is where the shade actually is.
        import mathutils
        for _ in range(4):
            pts = [head.matrix_world @ mathutils.Vector(c) for c in head.bound_box]
            ctr = sum(pts, mathutils.Vector()) / len(pts)
            delta = WANT_HEAD - ctr
            if delta.length < 1e-4:
                break
            lamp_root.location = lamp_root.location + delta
            bpy.context.view_layer.update()
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
 'ledger':        ((-0.30, 0.02, 1.52), (-1.00, 0.99, 1.44), 38, 30, 4.0),
 'shelf-evening': ((-0.30, -0.28, 1.21), (-0.10, 0.98, 1.03), 35, 28, None),
 'finger-test':   ((0.17, 0.47, 1.20), (-0.21, 0.975, 1.095), 55, 45, 2.8),
 'depth':         ((-0.18, -0.60, 0.95), (-0.18, 0.10, 0.845), 55, 45, 3.5),
 'roots':         ((-0.18, -0.28, 0.845), (-0.18, 0.10, 0.830), 85, 72, 2.2),
 'scale-table':   ((0.28, -0.86, 1.08), (0.36, 0.12, 0.845), 42, 34, None),
 'phone-closeup': ((0.80, -0.60, 0.95), (0.70, -0.04, 0.775), 42, 34, 3.2),
}


def set_shot(name, portrait=False):
    loc, aim, lens, plens, fstop = SHOTS[name]
    cam = look.camera(loc, aim, lens=plens if portrait else lens, fstop=fstop)
    cam.data.sensor_fit = 'HORIZONTAL'
    return cam
