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
import bmesh
import bpy
import math

import look


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
    for o in g:
        o.rotation_euler = (0, 0, rot_z)
    return g
