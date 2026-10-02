"""Geometry helpers plus the pot and the hand.

Everything is in metres, so a 15 cm pot is 0.15 wide and a finger is 17 mm
thick. Keeping real scale means the depth of field and the light falloff
behave without fudge factors.
"""
import bpy, math
from mathutils import Vector, Matrix
from common import mat, put, SKIN, TERRACOTTA, SOIL, LEAF, LEAF_LIT


def tube(spine, radii, name='tube', seg=28, cap_tip=True, cap_end=True):
    """One continuous swept tube along a spine, with per-sample radius.

    A limb built as a stack of cones with spheres at the joints shows a ring at
    every seam once subsurf rounds it, which is why the first finger read as a
    caterpillar. Sweeping a single surface has no seams to show. Frames are
    parallel-transported along the spine so the rings do not twist.
    """
    P = [Vector(p) for p in spine]
    n = len(P)
    tangents = []
    for i in range(n):
        if i == 0:
            t = P[1] - P[0]
        elif i == n - 1:
            t = P[-1] - P[-2]
        else:
            t = P[i + 1] - P[i - 1]
        tangents.append(t.normalized())

    # a starting normal that is not parallel to the first tangent
    up = Vector((0, 0, 1))
    if abs(tangents[0].dot(up)) > 0.9:
        up = Vector((0, 1, 0))
    normals = [(up - tangents[0] * up.dot(tangents[0])).normalized()]
    for i in range(1, n):
        # parallel transport: rotate the previous normal by the turn in tangent
        prev_t, cur_t = tangents[i - 1], tangents[i]
        axis = prev_t.cross(cur_t)
        nrm = normals[-1]
        if axis.length > 1e-9:
            ang = math.atan2(axis.length, prev_t.dot(cur_t))
            nrm = nrm.copy()
            nrm.rotate(Matrix.Rotation(ang, 3, axis.normalized()))
        normals.append((nrm - cur_t * nrm.dot(cur_t)).normalized())

    verts, faces = [], []
    for i in range(n):
        t, nx = tangents[i], normals[i]
        ny = t.cross(nx).normalized()
        for j in range(seg):
            a = j * 2 * math.pi / seg
            verts.append(tuple(P[i] + (nx * math.cos(a) + ny * math.sin(a)) * radii[i]))
    for i in range(n - 1):
        for j in range(seg):
            a = i * seg + j
            b = i * seg + (j + 1) % seg
            faces.append((a, b, b + seg, a + seg))
    if cap_tip:
        c = len(verts); verts.append(tuple(P[0]))
        faces += [(c, (j + 1) % seg, j) for j in range(seg)]
    if cap_end:
        c = len(verts); verts.append(tuple(P[-1]))
        base = (n - 1) * seg
        faces += [(c, base + j, base + (j + 1) % seg) for j in range(seg)]

    me = bpy.data.meshes.new(name)
    me.from_pydata(verts, [], faces)
    me.validate()
    o = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(o)
    return o


def limb(p0, p1, r0, r1, name='limb', verts=24):
    """A tapered capsule from p0 to p1. The joints get spheres so that a chain
    of these reads as one limb instead of a stack of cones."""
    p0, p1 = Vector(p0), Vector(p1)
    d = p1 - p0
    bpy.ops.mesh.primitive_cone_add(vertices=verts, radius1=r0, radius2=r1,
                                    depth=d.length, location=(p0 + p1) / 2)
    o = bpy.context.object
    o.rotation_euler = d.to_track_quat('Z', 'Y').to_euler()
    o.name = name
    return o


def joint(p, r, name='joint'):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=16, radius=r, location=p)
    bpy.context.object.name = name
    return bpy.context.object


def build_hand(skin=None):
    """A right hand, palm down, index extended and angled down to meet soil.

    Built with the index FINGERTIP AT THE ORIGIN, so placing the hand is just
    moving its parent empty to wherever the finger should touch. The other
    fingers are curled, which is what a hand doing this actually does and also
    hides the knuckles we are not modelling well.
    """
    skin = skin or mat('skin', SKIN, 0.62)
    parts = []

    # index: three phalanges, running back and up from the tip at the origin
    tip, pip, mcp = (0, 0, 0), (0.028, 0.019, 0.012), (0.052, 0.040, 0.027)
    knuck = (0.070, 0.060, 0.040)
    parts += [limb(tip, pip, 0.0072, 0.0085, 'idx1'),
              limb(pip, mcp, 0.0085, 0.0095, 'idx2'),
              limb(mcp, knuck, 0.0095, 0.0105, 'idx3'),
              joint(pip, 0.0085), joint(mcp, 0.0095)]

    # palm: a flattened box from the knuckles back to the wrist
    bpy.ops.mesh.primitive_cube_add(size=1, location=(0.100, 0.082, 0.052))
    palm = bpy.context.object
    palm.scale = (0.052, 0.042, 0.018)
    palm.rotation_euler = (0, -0.52, 0.62)
    mod = palm.modifiers.new('bev', 'BEVEL')
    mod.width, mod.segments = 0.012, 4
    parts.append(palm)

    # the curled fingers, tucked under and behind the index
    for i, (dx, dy, dz, r) in enumerate([
            (0.086, 0.040, 0.030, 0.0088),
            (0.098, 0.028, 0.026, 0.0090),
            (0.112, 0.020, 0.023, 0.0082)]):
        a = (dx, dy, dz)
        b = (dx + 0.014, dy - 0.026, dz - 0.012)
        c = (dx + 0.004, dy - 0.044, dz + 0.004)
        parts += [limb(a, b, r, r * 0.95, f'cur{i}a'),
                  limb(b, c, r * 0.95, r * 0.9, f'cur{i}b'),
                  joint(b, r * 0.95)]

    # thumb, lying along the near side
    parts += [limb((0.088, 0.108, 0.044), (0.052, 0.104, 0.026), 0.0105, 0.0092, 'th1'),
              limb((0.052, 0.104, 0.026), (0.028, 0.092, 0.018), 0.0092, 0.0080, 'th2'),
              joint((0.052, 0.104, 0.026), 0.0092)]

    # wrist and forearm, running away from camera-right
    parts += [joint((0.132, 0.108, 0.070), 0.026, 'wrist'),
              limb((0.132, 0.108, 0.070), (0.330, 0.210, 0.138), 0.027, 0.042, 'forearm')]

    for p in parts:
        put(p, skin, subsurf=1 if p.name.startswith(('idx', 'cur', 'th', 'fore')) else 0)

    bpy.ops.object.empty_add(location=(0, 0, 0))
    root = bpy.context.object
    root.name = 'hand'
    for p in parts:
        p.parent = root
    return root


def build_pot(loc=(0, 0, 0), top_r=0.075, bot_r=0.056, h=0.130):
    """Terracotta pot with a rim, and a soil surface that is a real mesh so a
    dimple and a finger hole can be pushed into it.

    The body is an OPEN cone plus solidify. A filled cone is a solid lump of
    terracotta, and the soil then sits inside it where nothing can see it.
    """
    pot_m = mat('terracotta', TERRACOTTA, 0.68)
    soil_m = mat('soil', SOIL, 0.94)

    bpy.ops.mesh.primitive_cone_add(vertices=72, radius1=bot_r, radius2=top_r, depth=h,
                                    end_fill_type='NOTHING',
                                    location=(loc[0], loc[1], loc[2] + h / 2))
    body = put(bpy.context.object, pot_m)
    sol = body.modifiers.new('sol', 'SOLIDIFY')
    sol.thickness = 0.005
    sol.offset = -1.0

    # base, so the pot is not open underneath
    bpy.ops.mesh.primitive_circle_add(vertices=72, radius=bot_r, fill_type='NGON',
                                      location=(loc[0], loc[1], loc[2] + 0.004))
    put(bpy.context.object, pot_m, shade_smooth=False)

    bpy.ops.mesh.primitive_torus_add(major_radius=top_r + 0.001, minor_radius=0.0055,
                                     major_segments=72, minor_segments=12,
                                     location=(loc[0], loc[1], loc[2] + h))
    rim = put(bpy.context.object, pot_m)
    for ob in (body, rim):
        bpy.context.view_layer.objects.active = ob
        bpy.ops.object.shade_smooth_by_angle(angle=math.radians(40))

    # soil: a UNIFORM grid clipped to a disc. A triangle fan puts every vertex
    # on a spoke through the centre, so displacing it produces a starburst
    # rather than grain. The disc is cut slightly wider than the pot's inner
    # wall, so the ragged edge of the clip is buried in the terracotta.
    soil = _soil_grid(loc[0], loc[1], loc[2] + h - 0.022, top_r - 0.009, n=116)
    put(soil, soil_m, shade_smooth=True)
    return body, rim, soil


def _soil_grid(cx, cy, z, r, n=116):
    verts, faces, idx = [], [], {}
    step = (2 * r) / n
    for i in range(n + 1):
        for j in range(n + 1):
            x, y = -r + i * step, -r + j * step
            idx[(i, j)] = len(verts)
            verts.append((cx + x, cy + y, z))
    for i in range(n):
        for j in range(n):
            mx = -r + (i + 0.5) * step
            my = -r + (j + 0.5) * step
            if math.hypot(mx, my) <= r:
                faces.append((idx[(i, j)], idx[(i + 1, j)], idx[(i + 1, j + 1)], idx[(i, j + 1)]))
    me = bpy.data.meshes.new('soil')
    me.from_pydata(verts, [], faces)
    me.validate()
    o = bpy.data.objects.new('soil', me)
    bpy.context.collection.objects.link(o)
    return o


def build_leaf(length=0.16, width=0.052, curl=0.30, droop=0.22, segs=(18, 7)):
    """A real leaf silhouette built vertex by vertex.

    A squashed sphere reads as a plastic blob, because a leaf's shape is in its
    outline: wide in the middle, pointed at the tip, and folded along the
    midrib. That is three lines of maths and it is the difference between a
    plant and a toy.
    """
    nu, nv = segs
    verts, faces = [], []
    for i in range(nu + 1):
        u = i / nu
        half = width * math.sin(math.pi * max(u, 1e-4)) ** 0.62
        # the blade droops along its length and folds up either side of the rib
        z0 = -droop * u * u
        for j in range(nv + 1):
            v = (j / nv) * 2 - 1
            verts.append((u * length, v * half, z0 + curl * (v * v) * half))
    for i in range(nu):
        for j in range(nv):
            a = i * (nv + 1) + j
            faces.append((a, a + 1, a + nv + 2, a + nv + 1))
    me = bpy.data.meshes.new('leaf')
    me.from_pydata(verts, [], faces)
    me.validate()
    o = bpy.data.objects.new('leaf', me)
    bpy.context.collection.objects.link(o)
    sol = o.modifiers.new('sol', 'SOLIDIFY')
    sol.thickness = 0.0012
    return o


def build_plant(leaf_mat, n=9, base=(0, 0, 0.112), stem_mat=None,
                gap_dir=None, gap_width=1.15,
                leaf_len=0.135, leaf_var=0.030, tilt_base=0.85, width=0.048):
    """Leaves fanning out of the soil, each on a short stem.

    gap_dir opens a sector facing the camera. Without it a plant with leaves
    all the way round always puts one leaf tip a few centimetres from the lens,
    and that leaf becomes the entire frame.
    """
    out = []
    for i in range(n):
        a = i * (math.pi * 2 / n) + 0.35
        tilt = tilt_base + 0.30 * (i % 3) / 2.0
        L = leaf_len + leaf_var * ((i * 7) % 5) / 4.0
        if gap_dir is not None:
            off = abs((a - gap_dir + math.pi) % (2 * math.pi) - math.pi)
            if off < gap_width:
                # inside the gap: shorten hard and lift, so the lens looks over
                # the plant rather than through it
                L *= 0.34 + 0.50 * (off / gap_width)
                tilt += 0.45
        leaf = build_leaf(length=L, width=width + 0.010 * (i % 2))
        leaf.location = (base[0] + math.cos(a) * 0.014,
                         base[1] + math.sin(a) * 0.014,
                         base[2] + 0.030 + 0.016 * (i % 3))
        leaf.rotation_euler = (0, -tilt, a)
        put(leaf, leaf_mat)
        out.append(leaf)
        if stem_mat:
            s = limb((base[0] + math.cos(a) * 0.008, base[1] + math.sin(a) * 0.008, base[2]),
                     leaf.location, 0.0035, 0.0022, f'stem{i}', verts=12)
            put(s, stem_mat)
            out.append(s)
    return out


def dress_soil(soil, dimple_at=None, dimple_r=0.016, dimple_d=0.009, bump=0.0019):
    """Give the soil surface a grain, and optionally push a hole into it.

    The hole is the point of the whole sequence, so it is real displaced
    geometry rather than a dark texture: it has to catch the light from the
    side and cast into itself.
    """
    me = soil.data
    # Coherent noise rather than per-vertex random: soil is clumps a few
    # millimetres across, and white noise renders as television static.
    # Real value noise, not a sum of sines: sines at fixed frequencies beat
    # against each other and against the grid, which is what produced the
    # basketweave. Frequencies are kept below the grid's Nyquist limit so the
    # relief is something the mesh can actually represent.
    def h2(i, j, seed):
        v = (i * 374761393 + j * 668265263 + seed * 144665) & 0xFFFFFFFF
        v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((v ^ (v >> 16)) & 0xFFFF) / 32767.5 - 1.0

    def vnoise(x, y, freq, seed):
        fx, fy = x * freq, y * freq
        i, j = math.floor(fx), math.floor(fy)
        tx, ty = fx - i, fy - j
        sx = tx * tx * (3 - 2 * tx)
        sy = ty * ty * (3 - 2 * ty)
        a, b = h2(i, j, seed), h2(i + 1, j, seed)
        c, d = h2(i, j + 1, seed), h2(i + 1, j + 1, seed)
        return (a * (1 - sx) + b * sx) * (1 - sy) + (c * (1 - sx) + d * sx) * sy

    def grain(x, y):
        return (vnoise(x, y, 170.0, 1) * 0.55
                + vnoise(x, y, 355.0, 2) * 0.30
                + vnoise(x, y, 640.0, 3) * 0.15)

    for v in me.vertices:
        v.co.z += bump * grain(v.co.x, v.co.y)
    if dimple_at:
        cx, cy = dimple_at
        for v in me.vertices:
            d = math.hypot(v.co.x - cx, v.co.y - cy)
            if d < dimple_r:
                t = 1.0 - (d / dimple_r)
                v.co.z -= dimple_d * (t ** 1.6)
    me.update()
    return soil


def build_finger(skin=None, nail=True, bend=1.0):
    """One index finger, cropped at the knuckle, as a single swept surface.

    A whole scripted hand reads as a wooden lay figure, because a hand is all
    knuckle creases and tendon shapes and none of that survives being built out
    of primitives. A single finger does survive it, so every close shot frames
    the finger and lets the rest of the hand leave frame.

    Tip at the origin, running back along +X, so placing it is a matter of
    moving the parent to the point of contact.
    """
    skin = skin or mat('skin', SKIN, 0.55)

    spine, radii = [], []
    N = 26
    for i in range(N + 1):
        s_ = i / N
        # curve the finger slightly, and swell it a touch at each knuckle so it
        # reads as jointed without any seam
        x = 0.082 * s_
        y = 0.021 * s_ ** 1.5
        z = 0.028 * (s_ ** 1.25) * bend
        r = 0.0061 + 0.0029 * s_
        r += 0.00055 * math.exp(-((s_ - 0.33) / 0.09) ** 2)
        r += 0.00065 * math.exp(-((s_ - 0.66) / 0.10) ** 2)
        spine.append((x, y, z)); radii.append(r)

    # rounded pad in front of the tip, so the end is not a flat disc
    pad = []
    for k in range(5, 0, -1):
        d = (k / 5.0) * 0.0056
        pad.append(((-d * 0.75, -0.0004 * k, -0.0010 * k),
                    max(0.0010, 0.0061 * math.sqrt(max(0.0, 1 - (d / 0.0061) ** 2)))))
    spine = [p for p, _ in pad] + spine
    radii = [r for _, r in pad] + radii

    # a stub of the hand for the frame to cut
    spine += [(0.102, 0.027, 0.036 * bend), (0.130, 0.034, 0.045 * bend)]
    radii += [0.0128, 0.0190]

    finger = put(tube(spine, radii, 'finger_mesh', seg=36), skin)
    parts = [finger]

    if nail:
        bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=16, radius=0.0054,
                                             location=(0.0165, 0.0016, 0.0094))
        n = bpy.context.object
        n.scale = (1.55, 1.00, 0.26)
        n.rotation_euler = (0, -0.34, 0.16)
        put(n, mat('nail', (0.76, 0.58, 0.49, 1), 0.22))
        parts.append(n)

    bpy.ops.object.empty_add(location=(0, 0, 0))
    root = bpy.context.object
    root.name = 'finger'
    for p in parts:
        p.parent = root
    return root


def add_dimple_key(soil, at, r=0.024, depth=0.010, name='Dimple'):
    """A shape key holding the hole, so it can be driven over the shot.

    The dimple cannot be baked into the mesh if it has to appear as the finger
    goes in, and a shape key is the cheapest thing that animates geometry
    without a rig.
    """
    if not soil.data.shape_keys:
        soil.shape_key_add(name='Basis', from_mix=False)
    kb = soil.shape_key_add(name=name, from_mix=False)
    cx, cy = at
    for i, p in enumerate(kb.data):
        d = math.hypot(p.co.x - cx, p.co.y - cy)
        if d < r:
            t = 1.0 - (d / r)
            p.co.z -= depth * (t ** 1.6)
    kb.value = 0.0
    return kb
