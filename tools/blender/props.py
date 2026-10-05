"""The props that are actually in frame, modelled instead of generated.

The room exports 596 meshes of which 60 percent are called Cube.013 or
leaf.060. Theirs exports 58, of which 3 percent are auto-named, and the rest
are batten, box_trim, chart_pin, drawer_knob, keyboard_well, lamp_shade. That
is the whole difference and it is not detail density: we have ten times their
mesh count and less to look at, because a leaf generated forty times is one
idea repeated while a drawer knob is a decision somebody made.

So this models the four objects the flythrough actually passes: the can, the
scale, the mug and the phone. Each was previously two or three un-bevelled
boxes, and each is in most of the frames.

Two builders carry all of it. A lathe, because everything in a kitchen that
holds liquid is rotationally symmetric and a revolved profile costs the same as
a cylinder while reading as a made object. And a slab with a real bevel,
because the single clearest tell of a primitive is an edge with no width: a
0.3 mm chamfer catches one highlight and that is the whole difference between a
box and a thing.
"""
import os
import bmesh
import bpy
import math
import mathutils

import look


def pivot(objs, at):
    """Move each object's origin to `at`, so a later rotation turns it in place.

    Both builders here put their vertices at WORLD coordinates and leave the
    object's transform at the identity: lathe() adds loc to every vertex, and
    slab() bakes its position into the mesh when it applies the scale the bevel
    needs. Either way the origin sits at the world origin, so o.rotation_euler
    swings the prop around the room on a radius equal to its distance from the
    centre, instead of turning it where it stands.

    The phone showed it plainly. Placed at (0.62, -0.17) with rot_z -0.24 it
    rendered centred on y -0.313, which is exactly where rotating (0.62, -0.17)
    about the world origin lands: 140 mm out, leaving 67 mm of the phone over
    the near edge of a table that ends at -0.33. It was never floating in the
    air; its underside sat on the table top the whole time. It was over the edge.

    Every prop that takes rot_z had this, the mug and the watering can included,
    and the error grows with distance from the room's centre, which is why it
    showed up on the phone first.
    """
    at = mathutils.Vector(at)
    for o in objs:
        o.data.transform(mathutils.Matrix.Translation(-at))
        o.location = at
    return objs


def lathe(profile, loc, m, segs=24, smooth=True, name='lathe'):
    """Revolve a list of (radius, z) around Z at `loc`.

    A profile is how you draw a mug: the wall goes up, turns over at the rim
    and comes back down inside it. A cylinder cannot do that, which is why the
    old mug needed a boolean to get an inside at all.
    """
    verts, faces = [], []
    n = len(profile)
    for s in range(segs):
        a = (s / segs) * math.tau
        ca, sa = math.cos(a), math.sin(a)
        for r, z in profile:
            verts.append((loc[0] + r * ca, loc[1] + r * sa, loc[2] + z))
    for s in range(segs):
        s2 = (s + 1) % segs
        for i in range(n - 1):
            a0, b0 = s * n + i, s * n + i + 1
            a1, b1 = s2 * n + i, s2 * n + i + 1
            faces.append((a0, b0, b1, a1))
    me = bpy.data.meshes.new(name)
    me.from_pydata(verts, [], faces)
    me.update()
    ob = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(ob)
    return look.put(ob, m, smooth=smooth)


def slab(loc, size, m, bevel=0.0035, segments=2, name='slab'):
    """A box with an edge that has width.

    The clearest tell of a primitive is a perfectly sharp edge, because nothing
    manufactured has one: a chamfer catches a single highlight and that one
    line is most of what separates a scale from a cuboid.
    """
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc)
    o = bpy.context.object
    o.name = name
    o.scale = size
    bpy.ops.object.transform_apply(scale=True)
    b = o.modifiers.new('bev', 'BEVEL')
    b.width, b.segments, b.limit_method = bevel, segments, 'ANGLE'
    b.harden_normals = True
    return look.put(o, m)


def watering_can(at, body_m, spout_m, rot_z=0.0):
    """Was three un-bevelled boxes that read as a folded paper plane."""
    g = []
    g.append(lathe([(0.000, 0.000), (0.052, 0.000), (0.058, 0.018), (0.060, 0.090),
                    (0.057, 0.120), (0.052, 0.126), (0.050, 0.124), (0.054, 0.119),
                    (0.057, 0.090), (0.055, 0.020), (0.000, 0.014)],
                   at, body_m, segs=22, name='can_body'))
    # the spout: a swept tube that leaves the body low and rises, which is what
    # makes it pour from the bottom of the water rather than the top
    import parts
    sp = [(at[0] + 0.050, at[1], at[2] + 0.022),
          (at[0] + 0.105, at[1], at[2] + 0.030),
          (at[0] + 0.150, at[1], at[2] + 0.056),
          (at[0] + 0.178, at[1], at[2] + 0.086)]
    g.append(look.put(parts.tube(sp, [0.014, 0.011, 0.0092, 0.0105], name='can_spout', seg=12),
                      spout_m, smooth=True))
    g.append(lathe([(0.0000, 0.000), (0.0135, 0.000), (0.0135, 0.006), (0.0000, 0.006)],
                   (at[0] + 0.178, at[1], at[2] + 0.086), spout_m, segs=14, name='can_rose'))
    # the handle, over the top, where a hand would actually lift it
    hp = [(at[0] - 0.040, at[1], at[2] + 0.120),
          (at[0] - 0.030, at[1], at[2] + 0.170),
          (at[0] + 0.012, at[1], at[2] + 0.182),
          (at[0] + 0.046, at[1], at[2] + 0.150),
          (at[0] + 0.052, at[1], at[2] + 0.112)]
    g.append(look.put(parts.tube(hp, [0.0062] * 5, name='can_handle', seg=10),
                      spout_m, smooth=True))
    pivot(g, at)
    for o in g:
        o.rotation_euler = (0, 0, rot_z)
    return g


def kitchen_scale(at, body_m, glass_m, lit_m=None):
    """Was two slabs. A scale is a platform, a base it stands proud of, a
    recessed window and four feet, and the recess is the part that says it
    reads a number back to you."""
    g = [slab((at[0], at[1], at[2] + 0.0115), (0.212, 0.182, 0.023), body_m,
              bevel=0.005, segments=3, name='scale_base'),
         slab((at[0], at[1], at[2] + 0.0285), (0.196, 0.166, 0.012), body_m,
              bevel=0.004, segments=3, name='scale_platform')]
    # the readout, set INTO the front lip rather than stuck on it
    g.append(slab((at[0], at[1] - 0.0885, at[2] + 0.0135), (0.082, 0.004, 0.019),
                  glass_m, bevel=0.0012, name='scale_window'))
    if lit_m:
        g.append(slab((at[0], at[1] - 0.0905, at[2] + 0.0135), (0.060, 0.0012, 0.009),
                      lit_m, bevel=0.0006, name='scale_readout'))
    for dx in (-0.088, 0.088):
        for dy in (-0.072, 0.072):
            g.append(lathe([(0.0000, 0.000), (0.0090, 0.000), (0.0090, 0.004), (0.0000, 0.004)],
                           (at[0] + dx, at[1] + dy, at[2]), body_m, segs=10, name='scale_foot'))
    return g


def mug(at, body_m, coffee_m, rot_z=0.0):
    """Was a cylinder with a torus floating beside it, hollowed by a boolean.
    A lathed profile turns over at the rim and comes back down inside, so the
    wall has thickness and the inside is part of the same surface."""
    import parts
    g = [lathe([(0.0000, 0.000), (0.0370, 0.000), (0.0400, 0.012), (0.0432, 0.062),
                (0.0440, 0.094), (0.0405, 0.098), (0.0392, 0.094), (0.0386, 0.020),
                (0.0000, 0.012)],
               at, body_m, segs=26, name='mug_body')]
    g.append(lathe([(0.0000, 0.000), (0.0380, 0.000)],
                   (at[0], at[1], at[2] + 0.064), coffee_m, segs=26, name='mug_coffee'))
    # a handle that joins the wall at both ends instead of hovering near it
    hp = [(at[0] + 0.040, at[1], at[2] + 0.078),
          (at[0] + 0.069, at[1], at[2] + 0.072),
          (at[0] + 0.077, at[1], at[2] + 0.050),
          (at[0] + 0.066, at[1], at[2] + 0.029),
          (at[0] + 0.040, at[1], at[2] + 0.024)]
    g.append(look.put(parts.tube(hp, [0.0070, 0.0062, 0.0058, 0.0062, 0.0070],
                                 name='mug_handle', seg=10), body_m, smooth=True))
    pivot(g, at)
    for o in g:
        o.rotation_euler = (0, 0, rot_z)
    return g


def phone(at, body_m, glass_m, rot_z=0.0):
    """Was two boxes. A phone is a bevelled slab with the glass inset inside a
    border, and the border is what stops it reading as a black rectangle."""
    g = [slab((at[0], at[1], at[2] + 0.0045), (0.076, 0.156, 0.009), body_m,
              bevel=0.0028, segments=3, name='phone_body'),
         slab((at[0], at[1], at[2] + 0.0092), (0.068, 0.148, 0.0008), glass_m,
              bevel=0.0006, name='phone_glass'),
         slab((at[0] + 0.024, at[1] + 0.059, at[2] + 0.0098), (0.020, 0.020, 0.0016),
              body_m, bevel=0.0008, name='phone_camera')]
    pivot(g, at)
    for o in g:
        o.rotation_euler = (0, 0, rot_z)
    return g


# How dark each band is as a fraction of the SKY's own EMISSION, near to far.
# Expressed against the sky rather than as three fixed hexes because the sky is
# what they are seen against: hardcoding both let the first set drift to a 4:1
# contrast, where the buildings were black holes and the roofline vanished.
# Multiplying the sky colour also keeps the hue, so a band can only ever be a
# dimmer version of the sky behind it, and the far band approaching 1.0 IS
# atmospheric perspective rather than an imitation of it.
BAND_K = tuple(float(v) for v in
               os.environ.get('TT_BAND_K', '0.45,0.62,0.78').split(','))

# Heights as a fraction of the old ones. The first set ran 0.55 to 2.70 above a
# base at sill-0.35, which put 27 buildings' worth of roofline above the window
# head: the glass was wall-to-wall city and the sky plane behind it was never
# visible in a single pixel. A skyline you cannot see sky past is not a skyline.
# 0.55 chosen on the measurement that matters, which is not the mean. At 0.45
# the window mean lands nearer the plate (0.209 against 0.210) but its local
# spread falls to 0.105, and spread is what makes a roofline legible; 0.55 gives
# 0.203 and 0.114 against the plate's 0.126, so it trades 0.006 of mean for
# 0.009 of the thing you actually see. It leaves a fifth of the glass as sky.
BAND_H = float(os.environ.get('TT_BAND_H', '0.55'))

# What fraction of a facade's windows are lit. See the note at its use.
WIN_LIT = float(os.environ.get('TT_WIN_P', '0.10'))


def skyline(win_y, sill_z, wall_m_maker, lit_warm, lit_cool, rnd, sky_hex='#323A44'):
    """A city in three depth bands instead of six boxes on one plane.

    This matters more for a moving camera than for any still. Everything beyond
    the glass sat at a single y, so it was a painted backdrop: it slid with the
    window frame and never moved against itself. Three bands at different
    distances parallax at different rates as the camera travels, which is free
    motion that a still frame cannot show and that no amount of detail on one
    plane can fake.

    The bands also get lighter and bluer with distance. Atmospheric perspective
    is the one depth cue that survives flat shading intact, because it is a
    change of colour rather than of shading, and in a picture with no outlines
    and three values per object it is most of what says far away.
    """
    made = []
    # The first set ran #0E131A to #212A35 against a sky of #323A44, which is
    # roughly 4:1, and at that contrast the buildings were black holes with lit
    # windows floating in them and no readable roofline at all. Low contrast is
    # what lets you read a skyline; high contrast erases it.
    # The last column is the share of lit windows that are WARM, not a label.
    # It was 'warm' / 'cool' / None, which meant the mid band was always cool,
    # and since the mid band carries the most buildings nearly every light in
    # the glass came out pale blue. The plate's city is almost entirely amber:
    # lit windows are lamps in other people's rooms, and rooms are warm. The
    # cool ones are the minority that reads as a screen or a stairwell.
    BANDS = [
        # y offset from the window, how many, height range, share warm
        (1.55, BAND_K[0], 7, (0.55, 1.30), 0.86),
        (2.60, BAND_K[1], 9, (0.75, 1.95), 0.64),
        (4.00, BAND_K[2], 11, (1.10, 2.70), None),
    ]
    for depth, k, count, (h0, h1), lit in BANDS:
        h0, h1 = h0 * BAND_H, h1 * BAND_H
        # Emissive, not lit, and this is the whole reason the city read as black
        # holes. There is no light source outside the glass, so a building with
        # an albedo and no emission can only ever arrive at the ambient floor,
        # which measured 0.04 against a sky of 0.21. Flat art does not light its
        # backdrop: it states a value. Emission states it, so a band sits exactly
        # where BAND_K puts it relative to the sky and nothing in the room's
        # lighting can drag it off.
        m = wall_m_maker(f'city{depth}', sky_hex, k)
        span = 2.0 + depth * 0.85            # wider bands further out, to fill the view
        for i in range(count):
            x = -span / 2 + span * (i + rnd.uniform(0.15, 0.85)) / count
            h = rnd.uniform(h0, h1)
            w = rnd.uniform(0.22, 0.52) * (1 + depth * 0.18)
            yield_box = (x, win_y + depth, sill_z - 0.35 + h / 2), (w, 0.08, h)
            made.append(('box', yield_box, m))
            if not lit:
                continue
            # Windows on a GRID, because that is what a building is. Scattered
            # at random they read as confetti floating in front of the sky: a
            # facade's windows line up in columns, and it is that alignment the
            # eye uses to decide the dark shape behind them is a building at all.
            # Random placement was undoing the silhouette the bands exist for.
            #
            # The size no longer grows with depth either. It did, to keep far
            # windows resolvable, but compensating for perspective cancels the
            # perspective, so the far band's windows arrived the same size on
            # screen as the near band's and flattened the three depths back into
            # one plane.
            ww, wh = 0.028, 0.032
            px, pz = ww * 2.2, wh * 2.5          # pitch: glass is a minority of a wall
            cols = max(1, int((w - 0.05) / px))
            rows = max(1, int((h - 0.16) / pz))
            for cx in range(cols):
                for cz in range(rows):
                    # Most windows are dark at night. Lighting them all is what
                    # makes a CG city look like a circuit board. 0.10 is not a
                    # taste call: the plate shows 22 lit windows through this
                    # glass and a grid at 0.34 showed 77, so the rate is scaled
                    # by 22/77. Counted with a blob count, because the mean value
                    # of the window cannot tell 22 lights from 77 dimmer ones.
                    if rnd.random() > WIN_LIT:
                        continue
                    wx = x - (cols - 1) * px / 2 + cx * px
                    wz = sill_z - 0.35 + 0.10 + cz * pz
                    made.append(('win', ((wx, win_y + depth - 0.05, wz), (ww, 0.01, wh)),
                                 lit_warm if rnd.random() < lit else lit_cool))
    return made
