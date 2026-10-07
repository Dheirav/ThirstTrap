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
# How much of the ledger texture's own brightness reaches Base Color. See the
# note at its use: in a posterised pipeline this, not the lighting, is what
# sets the page's rendered value.
PAPER_DIM = float(os.environ.get('TT_PAPER', 0.48))

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
    strata = []
    potroots = []
    import random as _r
    rnd_s = _r.Random(17)
    if cut:
        wet = look.mat('wet', look.srgb('#4A3422'), 0.96)
        # Two colours out of what used to be one. The grit on top keeps the pale
        # value, because gravel in lamplight is the brightest thing in a pot;
        # the dried layer under it drops to #74634C, a little lighter than the
        # wet column and no more. At the old #9A8264 that layer was a cream band
        # across the whole cut face, which is the slab this was meant to stop.
        dry = look.mat('dry', look.srgb('#74634C'), 0.98)
        grit_top = look.mat('grittop', look.srgb('#9A8264'), 0.96)
        bpy.ops.mesh.primitive_cylinder_add(vertices=sides, radius=top_r - 0.011,
                                            depth=h - 0.040,
                                            location=(loc[0], loc[1], loc[2] + (h - 0.040)/2 + 0.004))
        col = look.put(bpy.context.object, wet)
        # The top dressing is GRAINS, not a slab.
        #
        # It was a 30 mm cylinder of one flat colour, and in cross-section that
        # is a band of solid cream across the top of the cut: it read as a layer
        # of butter rather than as dry soil, and the eye went to it instead of to
        # the roots underneath. The plate's pot is topped with gravel, which is
        # speckled by construction, so the same thing here is a scatter of pale
        # grit sitting on the surface. A thin dark cylinder stays beneath it as
        # the dried layer itself, close enough in value to the wet soil that it
        # reads as the same material a little drier.
        # No dried-layer cylinder at all, in the end.
        #
        # It was 30 mm of one flat colour, then 12 mm of a darker one, and at
        # both it drew a horizontal stripe across the cut that the plate does not
        # have: a solid band reads as a seam between two materials, and soil has
        # no seams. Darkening it only made the stripe darker. The dryness it was
        # there to say is carried by the gravel on top, which is the thing you
        # would actually see, and the column below is left as one mass.
        _top = []
        for _ in range(240):
            a4 = rnd_s.uniform(0, 6.283)
            r4 = (top_r - 0.013) * math.sqrt(rnd_s.uniform(0, 1))
            _top.append(((loc[0] + math.cos(a4) * r4, loc[1] + math.sin(a4) * r4,
                          loc[2] + h - 0.030 + rnd_s.uniform(-0.003, 0.004)),
                         rnd_s.uniform(0.0016, 0.0038),
                         (rnd_s.uniform(0.8, 1.3), rnd_s.uniform(0.8, 1.3), rnd_s.uniform(0.5, 0.9)),
                         (rnd_s.uniform(0, 3), rnd_s.uniform(0, 3), rnd_s.uniform(0, 3))))
        strata.append(grains(_top, grit_top, subdiv=1, name='topdressing'))
        root_m = look.mat('root', look.srgb('#B8A684'), 0.85)
        import random
        rnd = random.Random(7)
        # A root BALL, not a fan of straws.
        #
        # The previous version ran 24 straight tubes radially outward from one
        # point near the crown, and that is what it rendered as: a starburst of
        # pale noodles filling the upper left of the face and leaving the rest of
        # the soil empty. Three things were wrong and all three are structural.
        #
        # Roots do not share an origin: they leave the stem at different heights
        # and from different points around it. They do not run straight: they
        # leave the crown sideways, turn down, and wander. And they do not stop
        # half way: a potted root system reaches the wall and the base, so the
        # soil volume fills rather than one quadrant of it.
        #
        # The path here eases outward early (f ** 0.6) and drops late
        # (f ** 1.4), which is that sideways-then-down turn, with a sine wobble
        # whose amplitude grows along the root so the fine end moves more than
        # the thick base does.
        # 110 of them, which sounds like a lot and is not. A cut face shows only
        # the roots that cross its plane: at 44 the face carried about twenty
        # stubs and read as scattered fragments rather than as a mass with a
        # section taken out of it. Density on the face is the whole effect, and
        # it costs nothing here because the shot is one frame.
        N = 9
        for i in range(110):
            a0 = rnd.uniform(0, 6.283)
            sr = rnd.uniform(0.0, top_r * 0.30)
            sx = loc[0] + math.cos(a0) * sr
            sy = loc[1] + math.sin(a0) * sr
            sz = loc[2] + h - 0.040 - rnd.uniform(0.0, 0.034)
            reach = rnd.uniform(0.50, 1.0)
            er = (top_r - 0.013) * reach
            ex = loc[0] + math.cos(a0) * er
            ey = loc[1] + math.sin(a0) * er
            ez = sz - rnd.uniform(0.55, 1.00) * (sz - (loc[2] + 0.008))
            wob = rnd.uniform(0.008, 0.020)
            ph = rnd.uniform(0, 6.283)
            spine = []
            for t in range(N):
                f = t / (N - 1)
                spine.append((sx + (ex - sx) * (f ** 0.6) + math.sin(ph + f * 5.2) * wob * f,
                              sy + (ey - sy) * (f ** 0.6) + math.cos(ph + f * 4.1) * wob * f,
                              sz + (ez - sz) * (f ** 1.4) + math.sin(ph * 1.7 + f * 6.4) * 0.0035))
            rad = rnd.uniform(0.0010, 0.0019)
            potroots.append(look.put(parts.tube(spine,
                                     [rad * (1.0 - 0.84 * (t / (N - 1))) for t in range(N)],
                                     name='root', seg=6), root_m))
            # Laterals, two to four per root and off the OUTER half of it, where
            # a root actually branches. One per root was too few to read.
            for _ in range(rnd.randint(2, 4)):
                k = rnd.randint(3, N - 2)
                b0 = spine[k]
                a2 = rnd.uniform(0, 6.283)
                ln2 = rnd.uniform(0.008, 0.020)
                lat = [b0]
                for t2 in range(1, 4):
                    f2 = t2 / 3
                    lat.append((b0[0] + math.cos(a2) * ln2 * f2,
                                b0[1] + math.sin(a2) * ln2 * f2 * 0.7,
                                b0[2] - ln2 * f2 * rnd.uniform(0.3, 0.9)))
                rr = rad * rnd.uniform(0.30, 0.46)
                potroots.append(look.put(parts.tube(lat, [rr, rr * 0.68, rr * 0.42, rr * 0.20],
                                         name='root', seg=5), root_m))

        # --- detail in the cross-section, which is the whole subject of the shot.
        #
        # First attempt put four strata cylinders and ninety pale crumbs in here
        # and both were wrong in the same way: too much contrast and too regular.
        # The bands rendered as a grill of hard horizontal bars, and the crumb,
        # spread over only 0.4 of the radius and half of it in the pale grit
        # colour, piled into a popcorn trail down the middle of the face.
        #
        # What the plate actually shows is a nearly uniform dark mass carrying
        # FINE speckle, with one pale gritty crust at the top. Soil is not
        # striped. So the bands go, the crumb gets small and dark and spreads
        # across the whole face, and the only strong value change in the column
        # stays where the plate puts it, at the surface.
        crumb_m = [look.mat('soilA', look.srgb('#4F3926'), 0.97),
                   look.mat('soilB', look.srgb('#3A2A1C'), 0.97)]
        grit_m = look.mat('grit', look.srgb('#5A5046'), 0.95)

        # Grit at the bottom, where a potted plant has its drainage. Small and
        # only a little paler than the mix, so it is a change of texture rather
        # than a stripe.
        _grit = []
        for _ in range(55):
            a0 = rnd_s.uniform(0, 6.283); rr = rnd_s.uniform(0, top_r - 0.016)
            _grit.append(((loc[0] + math.cos(a0) * rr, loc[1] + math.sin(a0) * rr,
                           loc[2] + rnd_s.uniform(0.006, 0.022)),
                          rnd_s.uniform(0.0022, 0.0042), (1.0, 1.0, 1.0),
                          (rnd_s.uniform(0, 3), rnd_s.uniform(0, 3), rnd_s.uniform(0, 3))))
        strata.append(grains(_grit, grit_m, subdiv=1, name='potgrit'))

        # Crumb sitting ON the cut plane, across its full width, and deliberately
        # NOT cut: sliced visually by the face while keeping their own
        # silhouettes, so it reads as broken earth instead of a surface a knife
        # went through. A perfectly flat cross-section is the tell that it is
        # geometry and not soil.
        _fa, _fb = [], []
        for _ in range(130):
            x0 = rnd_s.uniform(-(top_r - 0.013), top_r - 0.013)
            g2 = ((loc[0] + x0, loc[1] + rnd_s.uniform(-0.0035, 0.0035),
                   loc[2] + rnd_s.uniform(0.010, h - 0.048)),
                  rnd_s.uniform(0.0018, 0.0040),
                  (rnd_s.uniform(0.7, 1.3), rnd_s.uniform(0.5, 1.0), rnd_s.uniform(0.7, 1.3)),
                  (rnd_s.uniform(0, 3), rnd_s.uniform(0, 3), rnd_s.uniform(0, 3)))
            (_fa if rnd_s.random() < 0.55 else _fb).append(g2)
        grains(_fa, crumb_m[0], subdiv=1, name='facecrumb')
        grains(_fb, crumb_m[1], subdiv=1, name='facecrumb2')

    if cut:
        # TWO knives, because the pot and its contents want different cuts.
        #
        # The wall is opened all the way to the rim, which is what makes it read
        # as a pot with its front off. The SOIL keeps its top surface, which is
        # what gives the finger somewhere to press in plain view. One knife
        # doing both is what went wrong twice over: cut to full height it took
        # the soil's lid with the wall and the hand had nowhere to go but behind
        # the pot; stopped short of the lid it left a collar of terracotta
        # across the top that no plate has.
        knife = _cutter(loc, h, keep_lid=False)
        softknife = _cutter(loc, h, keep_lid=True)
        grit = _join(strata, 'potgrit')
        for ob in [body, rim]:
            b = ob.modifiers.new('cut', 'BOOLEAN'); b.operation = 'DIFFERENCE'
            b.object = knife
        for ob in [soil] + [o for o in (col, crust) if o] + ([grit] if grit else []):
            b = ob.modifiers.new('cut', 'BOOLEAN'); b.operation = 'DIFFERENCE'
            b.object = softknife
        # The roots get their OWN knife, 9 mm in front of the pot's. Cut with
        # the pot's they were sliced flush and showed only their cross-sections,
        # which rendered as faint grey scratches; left uncut entirely they hung
        # out of the face at full length and read as a bundle of pale sticks in
        # front of the soil rather than roots inside it. Nine millimetres proud
        # keeps their silhouette, which is what made the crumb read, while
        # leaving them bedded in the column.
        # Joined FIRST, then cut once. Each root used to carry its own boolean
        # against the knife, and at 110 trunks with laterals that is 446 boolean
        # modifiers evaluated on every render: the depth shot alone went to
        # nearly seven minutes. One mesh and one boolean is the same result.
        rknife = _cutter(loc, h)
        rknife.location.y -= 0.009
        joined = _join(potroots, 'potroots')
        if joined is not None:
            b = joined.modifiers.new('cut', 'BOOLEAN'); b.operation = 'DIFFERENCE'
            b.object = rknife
    return body, rim, soil


def _cutter(loc, h=0.130, keep_lid=True):
    """The knife that opens the pot, stopping BELOW the soil's top surface.

    It used to span the pot's whole height and more, so it took the top surface
    away with the front wall. That left no soil anywhere a finger could reach
    from the front: the hand had to go behind the pot to find any, where the
    camera cannot see it, and when it stayed in front it hung over the hole.
    Both complaints about this shot came from one cause.

    The plate does the obvious thing instead, which took far too long to notice:
    it cuts the WALL and keeps the LID. The soil surface runs the full width of
    the pot, the finger presses it at the front in plain view, and the cross
    section below shows the column. Stopping 12 mm under the surface leaves a
    crust thick enough to read as ground rather than as a skin.
    """
    top = (loc[2] + h - 0.042) if keep_lid else (loc[2] + h + 0.08)
    bot = loc[2] - 0.12
    bpy.ops.mesh.primitive_cube_add(size=1, location=(loc[0], loc[1] - 0.14, (top + bot) / 2))
    c = bpy.context.object; c.scale = (0.4, 0.28, top - bot)
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

    What it still got wrong until 2026-10-08 was the proportion. Length against
    width ran 1.3:1 to 1.5:1, and the apex leaves were 1.4:1, so every blade was
    barely longer than it was wide and read as a spade. The plates carry roughly
    3.5:1, long and narrow with a visible midrib. That one ratio is most of why
    the foliage looked like a different species.
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
            # Shallower than it was. 0.13 was tuned on a blade half as long
            # again as it was wide, where it read as lobing; on a 3.2:1 blade
            # the same amplitude reads as a ragged edge.
            prof *= 1.0 + 0.06 * math.cos(lobes * math.pi * u)
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
            ln = (0.072 + 0.034 * (1.0 - f) + rnd.uniform(-0.007, 0.010)) * scale
            br = [(nb[0], nb[1], nb[2]),
                  (nb[0] + math.cos(a) * ln * 0.30, nb[1] + math.sin(a) * ln * 0.30,
                   nb[2] + 0.016 * scale * up)]
            look.put(parts.tube(br, [r0 * 0.45, r0 * 0.30], name='stem', seg=5),
                     stem_m, smooth=True)
            leaf(leaf_m, br[1], (math.cos(a), math.sin(a)), ln,
                 width=(0.023 + 0.007 * (1 - f)) * scale,
                 rise=0.85 + up * 0.5 + rnd.uniform(-0.1, 0.1),
                 droop=0.78 + rnd.uniform(-0.12, 0.14),
                 lobes=rnd.choice((2, 3, 3, 4)), seed=seed * 91 + total)
    # a couple at the apex, shorter and more upright
    for k in range(2):
        a = phase + total * 2.3999 + rnd.uniform(-0.3, 0.3)
        total += 1
        leaf(leaf_m, at_h(1.0), (math.cos(a), math.sin(a)), 0.060 * scale,
             width=0.019 * scale, rise=1.5, droop=0.5,
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


def _grain_template(subdiv):
    """Verts and faces of one icosphere, made once and reused.

    bpy.ops.mesh.primitive_ico_sphere_add is an OPERATOR: it runs context
    handling, undo push and depsgraph work on every call. The crumb called it
    1685 times and the scene took 364 seconds to BUILD, which is where the
    render time had actually gone; the render itself was never slow.
    """
    key = ('_grain', subdiv)
    cache = _grain_template.__dict__.setdefault('cache', {})
    if key not in cache:
        bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=subdiv, radius=1.0,
                                              location=(0, 0, 0))
        o = bpy.context.object
        verts = [tuple(v.co) for v in o.data.vertices]
        faces = [tuple(pp.vertices) for pp in o.data.polygons]
        bpy.data.objects.remove(o, do_unlink=True)
        cache[key] = (verts, faces)
    return cache[key]


def grains(spec, m, subdiv=1, name='grains'):
    """Many pebbles as ONE mesh, built in Python rather than by operator.

    `spec` is (location, radius, (sx, sy, sz), (rx, ry, rz)) per grain. The
    result is identical geometry to adding each one separately and joining
    them, at a fraction of the cost, because nothing here touches an operator
    or creates an object per grain.
    """
    tv, tf = _grain_template(subdiv)
    verts, faces = [], []
    for loc, r, sc3, rot in spec:
        base = len(verts)
        cx, sx_ = math.cos(rot[0]), math.sin(rot[0])
        cy, sy_ = math.cos(rot[1]), math.sin(rot[1])
        cz, sz_ = math.cos(rot[2]), math.sin(rot[2])
        for vx, vy, vz in tv:
            x, y, z = vx * r * sc3[0], vy * r * sc3[1], vz * r * sc3[2]
            y, z = y * cx - z * sx_, y * sx_ + z * cx          # about X
            x, z = x * cy + z * sy_, -x * sy_ + z * cy         # about Y
            x, y = x * cz - y * sz_, x * sz_ + y * cz          # about Z
            verts.append((loc[0] + x, loc[1] + y, loc[2] + z))
        faces.extend(tuple(i + base for i in f) for f in tf)
    me = bpy.data.meshes.new(name)
    me.from_pydata(verts, [], faces)
    me.update()
    ob = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(ob)
    return look.put(ob, m)


def _join(objs, name):
    """Merge a list of meshes into one object.

    Blender's cost here is per OBJECT, not per triangle. The macro set reached
    4080 meshes for 437k triangles, and a seven-shot render went from five
    minutes to over an hour: 1458 root tubes and 1500 crumb grains, each its own
    object with its own transform, evaluation and draw. Joined, the geometry and
    the picture are identical and the scene is a tenth of the objects.

    Only safe for meshes carrying no modifiers, because join() applies the
    ACTIVE object's modifier stack to everything it swallows. Everything passed
    here is a plain tube or a plain sphere.
    """
    objs = [o for o in objs if o and o.name in bpy.data.objects]
    if len(objs) < 2:
        return objs[0] if objs else None
    bpy.ops.object.select_all(action='DESELECT')
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    bpy.ops.object.join()
    out = bpy.context.view_layer.objects.active
    out.name = name
    return out


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
    # Down again from #938872: this frame still clipped 2.8% where no plate
    # clips at all, and warming the key made it worse. 0.254 linear at gain
    # 3.2 lands at 0.81, which is pale against the crumb without touching
    # the ceiling.
    root_m = look.mat('macroroot', look.srgb('#8A7E66'), 0.88)

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
        # 0.845, not 0.88. The plate's channel is a CRACK: an irregular gap a
        # root's length across, with crumb spilling into it. At 0.88 the two
        # banks left a 160 mm corridor that rendered as a wide black band down
        # the middle of the frame, which is a hole in the picture and not a hole
        # in the soil.
        #
        # 0.52 apart with a size of 1.6 means the two banks OVERLAP across the
        # middle, so there was no channel at all: the shot was a flat field of
        # dirt. They have to be further apart than half their own width.
        bpy.ops.mesh.primitive_grid_add(x_subdivisions=160, y_subdivisions=160, size=1.6,
                                        location=(at[0] + side * 0.845, at[1], at[2]))
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
    # Twice as many, at half the size, and one subdivision rounder. 260 grains
    # at subdivision 1 is a 20-face icosahedron up to 45 mm across, so each one
    # presented four or five flat planes as large as a root and the soil read as
    # a pile of shards rather than as crumb. The plate's dirt is finer than its
    # roots are thick, and that relationship is what makes it read as soil.
    # Darker than #342618. Scattering the crumb to the frame put six times as
    # many lit pebbles on camera, and each one is a surface catching the key,
    # so the dirt's median value climbed from 0.220 to 0.267 against the
    # plate's 0.216: the shot gained its texture and lost its contrast in the
    # same change. The pebbles have to be darker now that there are more.
    peb_m = look.mat('macrocrumb', look.srgb('#281C11'), 0.98)
    _crumb = []
    _spec_peb, _spec_soil = [], []
    # Scattered to the FRAME, for the same reason the roots are. 1500 grains
    # spread over |x| 0.15 to 1.45 and |y| out to 0.75 put about sixty of them
    # on camera, because this shot sees 0.710 across and 0.400 down: the banks
    # rendered as two nearly bare slopes and the dirt read as a painted surface
    # rather than as crumb, which is the one thing the geometry was added for.
    def _grain(px, py, pz, r):
        g = ((px, py, pz), r,
             (rnd.uniform(0.7, 1.3), rnd.uniform(0.7, 1.3), rnd.uniform(0.4, 0.8)),
             (rnd.uniform(0, 3), rnd.uniform(0, 3), rnd.uniform(0, 3)))
        (_spec_peb if rnd.random() < 0.6 else _spec_soil).append(g)

    # Enough to pack, rather than enough to scatter. At 700 the grains covered
    # about half the area they were spread over, so the flat displaced grid
    # showed through between them everywhere and the soil read as beans on a
    # board. Broken earth has no gaps: clods touch, overlap and bury each other,
    # and the dark between them is a crevice rather than a backdrop.
    #
    # The size range widens at the same time. One narrow band of sizes is what
    # made them read as a product rather than as earth; real crumb runs from
    # dust to lumps, and the big ones are what give the small ones scale.
    for _ in range(2000):
        side = -1 if rnd.random() < 0.5 else 1
        _grain(at[0] + side * rnd.uniform(0.12, 0.42),
               at[1] + rnd.uniform(-0.26, 0.26),
               # Lowered with the size increase, not independently of it. A
               # clod now reaches 0.019 above its own centre, so the field that
               # used to sit under the roots at 0.085 was burying them: the
               # bottom third of the frame came out as pure earth with no roots
               # in it at all. Soil in front of a root is what makes it look
               # buried; soil over every root is just soil.
               at[2] + 0.040 + rnd.uniform(-0.035, 0.040),
               rnd.uniform(0.0018, 0.019))

    # Crumb IN the crack, spilling down its walls. Every grain was held clear of
    # the lip, so the channel rendered as a bare dark polygon with two clean
    # edges, and a crack with smooth sides is a cut rather than a break. The
    # plate has crumb all the way down its crevice.
    for _ in range(560):
        side = -1 if rnd.random() < 0.5 else 1
        _grain(at[0] + side * rnd.uniform(0.02, 0.15),
               at[1] + rnd.uniform(-0.26, 0.26),
               at[2] + rnd.uniform(-0.15, 0.05),
               rnd.uniform(0.0018, 0.015))

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
    _roots = []
    for i in range(20):
        side = -1 if i % 2 else 1
        # Built to the FRAME, not to the set, which is the whole reason this
        # shot kept reading as straws no matter how the roots were tuned. The
        # camera sees 0.710 across and 0.400 down at the aim plane. These used
        # to start 0.60 to 1.45 out and stop 0.1 short of the middle, so a root
        # was about a metre long and a quarter of it was on screen, and y0 was
        # spread over four times the frame height so two thirds of them missed
        # the frame entirely. What rendered was the last tapering stretch of a
        # smooth arc plus its tip fork, every time: no thick end, no kinks, no
        # branch points, because all of that was off camera. Start at the frame
        # edge instead and the whole root is in shot.
        x0 = at[0] + side * rnd.uniform(0.30, 0.46)
        y0 = at[1] + rnd.uniform(-0.30, 0.30)
        # One in four arches over the channel rather than ending at it. The gap
        # is still the subject, but with every root stopping at the same two
        # lips the middle third of the frame was bare dirt, and the plate has
        # roots crossing its crevice as well as torn ends hanging into it.
        # Each root at its own depth, rather than all of them on one plane.
        # Every root sat at 0.085, which is near the top of a frame 0.40 deep,
        # so the lower 40% of the picture carried 2.3% root against the plate's
        # 12.2%: a mat of roots lying on soil instead of roots running through
        # it. Spread them and the soil buries some, half-buries others, and the
        # ones in front read as in front BECAUSE the others are behind.
        z0 = 0.085 + rnd.uniform(-0.085, 0.020)
        crosses = (i % 4 == 3)
        if crosses:
            xe = at[0] - side * rnd.uniform(0.14, 0.30)
        else:
            # stop just short of the lip, with a little scatter so the breaks
            # do not line up into a seam of their own
            xe = at[0] + side * rnd.uniform(0.085, 0.145)
        # Kinks, not an arc. The old spine was one sine bend plus 10 mm of
        # jitter on a 1.0 run, which at this focal length is a straight rod.
        # The plate's roots change heading visibly at every segment, and that
        # polygonal wander is most of what makes them read as grown rather than
        # extruded. Walk the spine with the heading kicked each step and pulled
        # back toward the target, so it wanders but still arrives at the lip.
        # A root runs at an angle, not along the x axis. Every one of these
        # used to go flat left to right, because x0 and xe differed only in x
        # and the y walk wandered 30 mm at most, so the frame filled with
        # parallel horizontal bars and at three of them it read as scaffolding.
        # Give each root an end offset in y as well and they cross at angles,
        # which is what the plate does.
        # Slope proportional to the run, and never near zero. Drawing the end
        # offset from a fixed range let a root that happened to land ye near y0
        # render as a horizontal bar, and it hit the long crossing roots
        # hardest because their run is twice as far: two of those parallel
        # across the frame is the scaffolding look again.
        _run = abs(xe - x0)
        ye = y0 + _run * rnd.uniform(0.25, 0.65) * (1 if rnd.random() < 0.5 else -1)
        # Thirteen points rather than nine: the kink matters, but a kink every
        # eighth of a short root is a bend in a pipe. More, smaller corners.
        N = 13
        spine = [(x0, y0, at[2] + z0 + rnd.uniform(-0.008, 0.008))]
        for t in range(1, N):
            f = t / (N - 1)
            py = spine[-1][1]
            ty = y0 + (ye - y0) * f
            spine.append((x0 + (xe - x0) * f + rnd.uniform(-0.007, 0.007),
                          py + (ty - py) * 0.55 + rnd.uniform(-0.010, 0.010),
                          at[2] + z0 + 0.030 * math.sin(math.pi * f) * rnd.uniform(0.3, 1.0)
                          + rnd.uniform(-0.010, 0.010)))
        # The old 0.0072 to 0.0132 was tuned when only the tapered tip was in
        # shot, so it described a tip and not a root. With the base in frame it
        # renders at full width, and the plate's thickest root is about 1.2% of
        # the frame across, which is 0.0042 of world radius here.
        r = rnd.uniform(0.0032, 0.0056)
        inward_sign = -side
        _roots.append(look.put(parts.tube(spine, [r * (1.0 - 0.60 * (t / (N - 1)) ** 1.8)
                                                  for t in range(N)],
                                          name='macroroot', seg=7), root_m, smooth=True))
        # Tuned against a MEASURED target, not by eye: the plate's roots cover
        # 13.4% of its frame above value 0.45. Before branching this shot ran
        # 5.9% and read as a few sticks on bare dirt; three generations of thick
        # branches ran 24.4% and read as a mat of straw with no soil left to see.
        # Two generations at this thickness sit in between, which is the point:
        # the subject is roots IN soil, and that needs both of them visible.
        #
        # Branches that BRANCH. This is the structural difference from the
        # plate and the reason a run of tapering tubes could never match it:
        # the plate's roots are a tree. A trunk splits into two or three, each
        # of those splits again, and the fine ends are three or four
        # generations out. What was here before was one straight stub per
        # lateral, which drew whiskers on a cable.
        #
        # Children leave from a point ALONG the parent as well as from its tip,
        # which is what fills the space between roots rather than only
        # extending them.
        def _branch(p0, d, length, rad, level):
            M = 6
            ba = rnd.uniform(0, math.tau)
            amp = 0.11 * length
            sp = []
            for t2 in range(M):
                f2 = t2 / (M - 1)
                # no kick on the first point, or the branch detaches from
                # the parent it grows out of
                j = 0.0 if t2 == 0 else length * 0.055
                sp.append((p0[0] + d[0] * length * f2 + math.cos(ba) * amp * f2 * f2
                           + rnd.uniform(-j, j),
                           p0[1] + d[1] * length * f2 + math.sin(ba) * amp * f2 * f2
                           + rnd.uniform(-j, j),
                           p0[2] + d[2] * length * f2 + math.sin(f2 * 2.6 + ba) * amp * 0.30
                           + rnd.uniform(-j, j)))
            _roots.append(look.put(parts.tube(sp, [rad * (1.0 - 0.45 * (t2 / (M - 1)))
                                                   for t2 in range(M)],
                                             name='macroroot', seg=7), root_m, smooth=True))
            if level <= 0:
                return
            for c in range(rnd.randint(2, 3)):
                k2 = M - 1 if c == 0 else rnd.randint(2, M - 2)
                base2 = sp[k2]
                dev = rnd.uniform(0.40, 1.05) * (1 if rnd.random() < 0.5 else -1)
                nd = (d[0] * math.cos(dev) - d[1] * math.sin(dev),
                      d[0] * math.sin(dev) + d[1] * math.cos(dev),
                      d[2] + rnd.uniform(-0.30, 0.30))
                nn = math.sqrt(nd[0] ** 2 + nd[1] ** 2 + nd[2] ** 2) or 1.0
                _branch(base2, (nd[0] / nn, nd[1] / nn, nd[2] / nn),
                        length * rnd.uniform(0.44, 0.70),
                        rad * rnd.uniform(0.44, 0.62), level - 1)

        for _ in range(rnd.randint(2, 4)):
            k = rnd.randint(1, N - 2)
            base = spine[k]
            a3 = rnd.uniform(-1.4, 1.4)
            d0 = (inward_sign * math.cos(a3) * 0.6, math.sin(a3), rnd.uniform(-0.35, 0.10))
            n0 = math.sqrt(d0[0] ** 2 + d0[1] ** 2 + d0[2] ** 2) or 1.0
            _branch(base, (d0[0] / n0, d0[1] / n0, d0[2] / n0),
                    rnd.uniform(0.09, 0.17), r * 0.50, 2)

        # the break
        tip = spine[-1]
        inward = -side
        for _ in range(0 if crosses else rnd.randint(3, 6)):
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
        _roots.append(look.put(parts.tube(sp, [r, r * 0.7, r * 0.3], name='macroroot', seg=5),
                 root_m, smooth=True))
        for _ in range(rnd.randint(2, 4)):
            a2 = rnd.uniform(-1.0, 1.0)
            ln = rnd.uniform(0.012, 0.030)
            _roots.append(look.put(parts.tube([(xe, y0, at[2] + 0.086),
                                 (xe - side * ln * math.cos(a2), y0 + ln * math.sin(a2) * 0.8,
                                  at[2] + 0.086 + rnd.uniform(-0.01, 0.006))],
                                [r * 0.22, 0.0005], name='macroroot', seg=3),
                     torn_m, smooth=True))


    # Two joins, and they are the whole reason this set renders in minutes
    # rather than an hour. See _join: the cost is per object.
    _join(_roots, 'macroroots')
    # subdiv 1, not 2. A 320-face icosphere is a ball, and a field of balls is
    # a bowl of lentils. The plate's clods are faceted: flat planes meeting at
    # edges, each catching the light differently, which is what broken earth
    # looks like. 80 faces gives that without returning to the shards this was
    # at subdiv 0, because the grains are a third the size they were then.
    #
    # It is also cheaper. 2560 grains at 80 faces is 205k, against 960 at 320
    # faces for 307k, so there is more soil and less geometry.
    grains(_spec_peb, peb_m, subdiv=1, name='macrocrumb')
    grains(_spec_soil, soil_m, subdiv=1, name='macrocrumb_dark')


ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets')


def append(name, loc, scale=1.0, rot_z=0.0, mats=None, default=None,
           drop=('_ground', 'stash')):
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
    # large pale dome sitting in the middle of the room.
    #
    # 'stash' joins it. book_encyclopedia_set_01 carries a Sphere_stash_0, a
    # 153 mm sphere that has been sitting in this room since the books were
    # added and shows on the sill in shelf-evening as a brown dome behind the
    # first pot. It only became obvious when the table stack moved into
    # phone-closeup's frame and it filled the right of it. Nothing referenced
    # it, nothing placed it, and it was never meant to render: an appended
    # asset's file is not a prop, it is a bag containing one. Names are taken before
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


# Fingertip to mid-forearm on a real arm, in metres. The asset is normalised to
# length 1, so this is the only number that sets its size.
HAND_LEN = 0.34


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
    # A sculpted hand, from assets/hand.glb. See tools/blender/extract_hand.py
    # for where it comes from and how it is cut out.
    #
    # The two scripted attempts are worth keeping straight, because the lesson
    # is not "the code was buggy". build_finger draws one finger on a cone: a
    # single finger is an abstraction a viewer completes, so it reads, but it was
    # the most obviously wrong object in the set. parts.build_hand drew a real
    # hand and read worse, because a bad hand is one the viewer does NOT
    # complete; I spent four rotation solves on it before rendering it alone
    # against a ground plane showed the rotation was never the problem. A hand
    # is modelling work, and the right fix was to get a modelled one.
    #
    # The asset arrives normalised: fingertip at its origin, forearm along +X,
    # up at +Z, length 1. So placing it is scale, yaw, pitch, translate, and
    # there is no free roll left to guess at.
    import os
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets', 'hand.glb')
    before = set(bpy.data.objects)
    bpy.ops.import_scene.gltf(filepath=path)
    h = [o for o in bpy.data.objects if o not in before and o.type == 'MESH'][0]
    for o in [o for o in bpy.data.objects if o not in before and o is not h]:
        bpy.data.objects.remove(o, do_unlink=True)
    h.name = 'finger'                     # the name the rest of the file cleans up by
    h.data.materials.clear()
    look.put(h, skin)
    h.scale = (HAND_LEN, HAND_LEN, HAND_LEN)
    # yaw turns the arm about the pot; pitch is the arm's ELEVATION, negative,
    # because the forearm leaves frame above the hand.
    #
    # The signs are not free. With XYZ order, +X carries to
    # (cos ry cos rz, cos ry sin rz, -sin ry), so the arm's rise is -sin(ry):
    # ry must be the negative of the elevation. Writing -pitch here tipped the
    # arm down into the sill instead of up out of frame.
    # glTF imports arrive in QUATERNION rotation mode, and in that mode Blender
    # ignores rotation_euler completely: it reads rotation_quaternion instead,
    # which the importer leaves at identity. So every yaw and pitch set here has
    # been silently discarded since the asset was introduced, and the hand has
    # been sitting in whatever orientation extract_hand.py baked into it. The
    # apparent direction changes between renders came from editing that script,
    # not from any angle set in this file.
    h.rotation_mode = 'XYZ'
    h.rotation_euler = (0.0, pitch, yaw)
    h.location = (at[0], at[1], at[2] - into)
    bpy.context.view_layer.update()
    return h


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
    # Left wall. There was never one, and nothing in five shots looked past the
    # sill far enough to notice. ledger does: it was rewidened from 40mm to 31mm
    # in 1f9a399 and its left third became the outside of the room, which renders
    # as absolute black. Measured 0.057 against the plate's 0.164 there, and no
    # amount of light fixes a surface that does not exist.
    _box((-1.45, 0.2, 1.5), (0.10, 5, 3.4), M['wall'])                  # left wall
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

    # Warmer, and smaller. Measured against the plate: its moon renders at
    # (247, 221, 181), a red-minus-blue of +65, and this one rendered at
    # (239, 236, 229), +9. That is a white disc, and a white disc in a warm
    # evening frame reads as a hole in the glass rather than as the moon. The
    # emission now carries the plate's own ratios, g/r 0.89 and b/r 0.73.
    #
    # The radius comes down because the disc covered 8119 pixels against the
    # plate's 6877, and the brightest object in the frame being a fifth too big
    # pulls the eye off the sill, which is the subject.
    bpy.ops.mesh.primitive_circle_add(vertices=28, radius=0.124, fill_type='NGON',
                                      location=(0.60, WIN_Y + 2.45, 1.70))
    mn = bpy.context.object; mn.rotation_euler = (math.radians(90), 0, 0)
    look.put(mn, look.mat('moon', (1, 1, 1, 1), 1.0, emit=(0.98, 0.76, 0.45, 1), strength=0.92))

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
        # The second pot is the hero of finger-test and is deliberately the
        # biggest on the sill. It is not decoration: the hand is 190 mm across
        # and a 120 mm mouth cannot receive it, so the fingers passed THROUGH the
        # rim no matter where the press point went. The plate gets away with a
        # small pot because its hand has one finger extended well ahead of the
        # others; this one's fingers are bunched, so the pot has to open wider
        # than the hand. A sill of pots at one size was the odder thing anyway.
        sc_i = 1.62 if i == 1 else 0.92 + 0.10 * ((i * 7) % 3) / 2.0
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
    # 24 sides, not the default 16. A 16-sided cone reads as round while it is
    # whole, because the facets run round the back out of sight; cut in half it
    # shows four flat faces across the front and reads as a box, which is what
    # the depth render has been showing all along.
    facet_pot((-0.18, 0.10, TABLE_Z), sides=24, m=M['terra'], cut=True)
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
    #
    # Its strength is only meaningful if a camera can SEE it. scale-table sat at
    # z 1.26 and the shade's rim is at 1.2595, so that camera was level with the
    # mouth and looking at this disc edge-on: TT_GLOW at 1.6, 3.0 and 5.5 gave
    # byte-identical frames. The camera dropped to 1.08 to look up into the shade
    # the way every plate does. The lamp did not move, because it is also the key
    # light for the table and raising it would dim three shots to fix one.
    # Smaller and dimmer than the shade's mouth. At full width and strength 2.6
    # it rendered as a flat white ellipse with the dark shade invisible behind
    # it, which is the one gradient the plates allow turned into a sticker.
    bpy.ops.mesh.primitive_circle_add(vertices=28, radius=0.120, fill_type='NGON',
                                      location=(LAMP_AT[0], LAMP_AT[1], LAMP_AT[2] + 0.018))
    look.put(bpy.context.object, look.mat('shadeglow', (0, 0, 0, 1), 1.0,
                                          emit=look.LAMP + (1,),
                                          strength=float(os.environ.get('TT_GLOW', 3.0))))
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

    # A second can, on the table rather than the sill. Not a duplicate for its
    # own sake: the plate's scale-table has a can standing at the right of frame
    # and the room only had one, out on the sill where that camera cannot see it,
    # so the right third of the shot was bare table. A watering can on the table
    # of someone who waters by weight is also just where it would be.
    props.watering_can((0.95, 0.21, TABLE_Z), M['metal'], M['metal'], rot_z=2.15)

    append('trowel_01', (0.74, 0.96, SILL_Z + 0.006), scale=0.55, rot_z=1.25,
           default=look.mat('tool', look.srgb('#3A342C'), 0.62))
    # phone, face up, screen off.
    #
    # Pulled in from y -0.26. The body is 156 mm long, so at -0.26 it spanned
    # -0.338 to -0.182 while the table's near edge is at -0.33: eight
    # millimetres of it hung off the end, and the yaw put a corner further out
    # still. A phone resting on nothing is the one thing in a still that reads
    # instantly as wrong.
    props.phone((0.47, -0.04, TABLE_Z), M['frame'], M['glassblack'], rot_z=-0.24)

    # --- foreground dressing.
    #
    # Size is set against the CAMERA, not against the table. Scaled to 0.105 and
    # sat 0.26 m from the phone-closeup lens, this stack filled the right 40% of
    # that frame as a near-black mass and took its dark fraction to 48.6% against
    # the plate's 19.0%. A foreground object that crops an edge has to crop it,
    # not fill it.
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
    append('book_encyclopedia_set_01', (0.80, -0.22, TABLE_Z), scale=0.055, rot_z=-0.6,
           mats=[('_paper', look.mat('pages2', look.srgb('#8B7E68'), 0.95))],
           default=look.mat('book2', look.srgb('#3B2F26'), 0.9))

    # --- the ledger
    #
    # On a wall RETURN, not on the window pier, and this is why the shot could
    # not be framed. The pier sits in the window plane at y 1.0, the same depth
    # as the sill, so a camera close enough to read the page was close enough
    # that the sill fell 41 degrees below the frame, and one far enough to hold
    # both put the page at 11% of frame width with its date column unreadable.
    # Four framings traded one against the other and none could have worked.
    #
    # The plate solves it with geometry rather than with a lens: its sheet hangs
    # on a short wall that juts into the room, so the page is near the camera
    # and the sill runs away behind it. The room now has that wall.
    _box((-1.12, 0.60, 1.5), (0.12, 0.94, 3.4), M['pier'])
    # There was a 0.42 x 0.20 plank here at z 1.30, 350 mm above the real sill
    # and resting on nothing, carrying two pots. It read as a duplicate window
    # sill floating in mid air, which is exactly what it was. The room has one
    # sill, at SILL_Z, and the ledger shot should show that one.
    # Moved right and shrunk. At x -0.96 the second pot stood directly under
    # the sheet and its leaves covered the last four rows, which is the half of
    # the page that makes the point.
    facet_pot((-0.80, 0.985, SILL_Z), top_r=0.052, bot_r=0.040, h=0.090, m=M['terra'])
    leafy_plant((-0.80, 0.985, SILL_Z + 0.078), M['leaf'], M['stem'], scale=0.60, seed=11)
    facet_pot((-0.95, 0.985, SILL_Z), top_r=0.046, bot_r=0.035, h=0.080, m=M['terra'])
    leafy_plant((-0.95, 0.985, SILL_Z + 0.070), M['leaf'], M['stem'], scale=0.52, seed=12)
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
    led = _box((-1.052, 0.60, 1.400), (0.008, 0.215, 0.285), M['paper'])
    sheet_png = os.path.join(ASSETS, 'ledger-sheet.png')
    if os.path.exists(sheet_png):
        bpy.ops.mesh.primitive_plane_add(size=1.0, location=(-1.0445, 0.60, 1.400))
        face = bpy.context.object
        # One quarter turn about X is all it needs: that sends the plane's
        # normal to -y, which is the way the pier faces, and its own up to world
        # z. The plane's X is then the page's width and its Y the page's height,
        # which is what the image expects. The small yaw is the tilt the paper
        # already has.
        # Square to the slab, and 3.5 mm proud of it. The page carried a 3
        # degree tilt that the slab behind it does not, and a 215 mm wide plane
        # turned 3 degrees swings its edge 5.6 mm: more than it stood proud, so
        # one half of it sank behind the slab and rendered as blank paper. That
        # is where the date column went. The slab's front face is
        # at x -1.048 and this plane went in at -1.0485, half a millimetre
        # behind it, so every pixel of the page was the slab's plain paper
        # material and the texture never rendered. That is why the dates did not
        # appear, and why dimming the texture and sweeping both lights that
        # reach it changed the sheet's value by almost nothing: none of those
        # knobs were attached to what the camera was actually seeing.
        #
        # A further quarter turn about Z carries that normal from -y round to
        # +x, which is the way the return faces.
        face.rotation_euler = (math.radians(90), 0, math.radians(90))
        face.scale = (0.215, 0.285, 1.0)
        pm = look.mat('ledgerpaper', look.srgb('#D9CDB2'), 0.95)
        nt = pm.node_tree
        img = nt.nodes.new('ShaderNodeTexImage')
        img.image = bpy.data.images.load(sheet_png)
        img.interpolation = 'Cubic'
        # Scaled down before it reaches Base Color, and the reason is the
        # pipeline rather than the paper. plate.py POSTERISES the lighting, which
        # normalises it to a handful of steps and destroys its magnitude, so a
        # surface's final value comes from its ALBEDO times the gain and barely
        # from how much light falls on it. Sweeping the two lights that reach
        # this sheet across a five-fold range moved it from 0.945 to 0.936.
        #
        # The page's paper is (217, 205, 178), so 0.85, and at gain 3.2 that
        # clips on almost any light at all: when the sheet moved onto the wall
        # return it blew out over 12% of the frame. The texture keeps its own
        # colours for every other use; this multiply is what sets the rendered
        # value, and it is the only knob here that does anything.
        dim = nt.nodes.new('ShaderNodeMixRGB')
        dim.blend_type = 'MULTIPLY'
        dim.inputs[0].default_value = 1.0
        dim.inputs[2].default_value = (PAPER_DIM, PAPER_DIM, PAPER_DIM, 1)
        nt.links.new(img.outputs['Color'], dim.inputs[1])
        nt.links.new(dim.outputs[0],
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
                                         location=(-1.0435, 0.603, 1.5835))
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
    bpy.ops.object.light_add(type='AREA', location=(-0.58, 0.56, 1.56))
    lb = bpy.context.object
    # Moved with the sheet. Its energy is solved for a half-metre throw, and
    # when the page went onto the wall return this light stayed put and ended up
    # 0.10 m from it: the sheet clipped 12.1% of the frame. The light now sits
    # 0.49 m off the page's new face, so the solve below still holds.
    #
    # 2.6 W half a metre away put the sheet at 0.866 and clipped a tenth of the
    # frame. Solved rather than nudged: the target is the plate's 0.562, so in
    # linear terms 0.562^2.2 over 0.866^2.2 is 0.39, and 2.6 x 0.39 is 1.0.
    lb.data.energy, lb.data.size, lb.data.color = float(os.environ.get('TT_LB', 1.0)), 0.22, look.LAMP
    lb.data.use_shadow = False
    look.aim(lb, (-1.05, 0.60, 1.400))
    # Wash for the new left wall. Deliberately separate from the ledger bounce
    # above, which is solved to put the SHEET at the plate's 0.562 and must not
    # be disturbed. Large and shadowless, because it stands in for bounce off a
    # wall nobody modelled, and bounce has no edge.
    #
    # 11 W at size 1.60 put the wall at 0.262 against the plate's 0.164 and
    # printed a visible disc on it: too bright AND too small, which is the same
    # mistake the ledger bounce above already recorded making with a spot. Both
    # halves are fixed by measurement rather than by taste. Energy scales by
    # (0.164/0.262) ** 2.2 = 0.357, so 11.0 becomes 3.9; the source doubles in
    # size and moves back so its falloff across the wall is gentle enough to
    # have no edge.
    bpy.ops.object.light_add(type='AREA', location=(-0.30, -1.15, 1.95))
    lw = bpy.context.object
    lw.data.energy, lw.data.size, lw.data.color = float(os.environ.get('TT_LW', 3.9)), 3.20, look.LAMP
    lw.data.use_shadow = False
    look.aim(lw, (-1.45, 0.35, 1.35))


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
# Solved, not nudged, after four framings that each traded the sheet against
# the sill. The constraint is geometric: the sheet spans z 1.403 to 1.687 and
# the sill sits at 0.95, so from 0.68 m away they are 41 degrees apart against a
# 31 mm lens's 36.5 degree vertical field and CANNOT both be in frame. Scanning
# camera position, aim and lens for a setting that holds the whole sheet plus a
# sill pot gives this one: the page lands at screen x 0.24 filling 37% of the
# frame's height, which is where the plate puts it.
#
# The ledger aim points to the RIGHT of the sheet, not at it. Aiming straight
# at the subject centres it, and this shot wants it off to one side with the
# sill running away behind, which is how the plate composes it. Aiming at the
# sheet put it dead centre with blank wall filling the left third.
#
# The ledger aim was at the sheet's own height, 1.548, because the shot was
# framed around a plank that floated 350 mm above the sill and has since been
# deleted. With that gone the real sill fell right out of frame, measured at
# screen y -0.35. Tilted down to 1.255, which is the midpoint of the 31 degrees
# between them, so the sheet sits high in frame and the sill runs along the
# bottom the way the plate has it.
SHOTS = {
 # Pulled back and aimed lower so the sill comes into shot. It framed z 1.177
 # to 1.593 and the pots sit at 0.95 to 1.15, so the plants the diary is ABOUT
 # were a quarter of a metre below the bottom of the frame: a page of
 # handwriting against a bare wall. The plate puts the sheet on the left and the
 # sill on the right, which is what makes it a picture of a habit rather than a
 # photograph of a list.
 'ledger':        ((-0.33, -0.18, 1.47), (-0.86, 0.86, 1.20), 28, 26, 4.0),
 'shelf-evening': ((-0.30, -0.28, 1.21), (-0.10, 0.98, 1.03), 35, 28, None),
 'finger-test':   ((0.05, 0.47, 1.325), (-0.245, 0.975, 1.030), 42, 35, 2.8),
 'depth':         ((-0.60, -0.62, 0.86), (-0.12, 0.12, 0.825), 45, 38, 3.5),
 'roots':         ((0.0, -0.56, -4.44), (0.0, 0.0, -5.06), 40, 34, 2.2),
 'scale-table':   ((0.20, -1.00, 1.08), (0.34, 0.15, 1.00), 31, 26, None),
 'phone-closeup': ((0.60, -0.40, 0.88), (0.42, 0.18, 0.84), 33, 27, 2.2),
}


def set_shot(name, portrait=False):
    loc, aim, lens, plens, fstop = SHOTS[name]
    cam = look.camera(loc, aim, lens=plens if portrait else lens, fstop=fstop)
    cam.data.sensor_fit = 'HORIZONTAL'
    return cam
