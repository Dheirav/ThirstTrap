"""Pull the hand out of the Meshy scene, and nothing else.

  blender -b --python tools/blender/extract_hand.py -- OUT.glb [preview_dir]

Meshy returns one fused mesh holding a pot, a plant, soil and a hand, so the
hand cannot be separated by object or by loose part. It is separated
geometrically instead: the pot and its soil are a solid of revolution about a
vertical axis, so every vertex within the pot's radius AND below the soil line
belongs to the pot, and what is left is the hand and forearm.

Only the hand is taken. The pot, the plant and the soil are built procedurally
in world.py and are identical in all seven shots, and the complaint that began
this work was that the pots and plants differed from plate to plate; dropping in
a one-off pot would reintroduce exactly that. The soil here is also heavily
displaced, which is wrong for a look whose shading depends on large even areas.
"""
import bpy, sys, os, math, mathutils
import numpy as np

a = sys.argv[sys.argv.index('--') + 1:]
OUT = a[0]
PREVIEW = a[1] if len(a) > 1 else None
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets', 'meshy-hand-pot.glb')

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=SRC)
o = [x for x in bpy.data.objects if x.type == 'MESH'][0]
bpy.context.view_layer.objects.active = o
o.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.select_all(action='SELECT')
bpy.ops.mesh.separate(type='LOOSE')
bpy.ops.object.mode_set(mode='OBJECT')
parts = sorted([x for x in bpy.data.objects if x.type == 'MESH'],
               key=lambda q: -len(q.data.vertices))
big = parts[0]
for q in parts[1:]:
    bpy.data.objects.remove(q, do_unlink=True)
# separate() leaves no active object once the others are gone, and mode_set
# silently has no poll target without one.
bpy.ops.object.select_all(action='DESELECT')
bpy.context.view_layer.objects.active = big
big.select_set(True)

co = np.array([(big.matrix_world @ v.co)[:] for v in big.data.vertices])
# The axis from the pot's base: only the lowest slice is certainly pot, because
# the hand never goes below the pot's foot.
base = co[co[:, 2] < co[:, 2].min() + 0.12]
ax, ay = base[:, 0].mean(), base[:, 1].mean()
r = np.hypot(co[:, 0] - ax, co[:, 1] - ay)
th = np.arctan2(co[:, 1] - ay, co[:, 0] - ax)

# Where the pot ends, found by ANGULAR COVERAGE rather than by height. A pot is
# a solid of revolution, so every horizontal slice of it occupies all 24 sectors
# around the axis; a hand, an arm or a leaf occupies a few. Walking up from the
# floor and stopping at the first slice that is no longer a full ring finds the
# soil line exactly.
#
# The first version guessed it as a percentile of height among vertices inside
# the pot radius, which put it 0.24 too high because the hand's own vertices sit
# inside that radius and dragged the percentile up with them. The cut then ran
# through the rim and the fingers: it left a sawtooth ring of leftover rim and
# tore the hand open, which a render showed at once and no vertex count would
# have.
SLICE = 0.04
pot_top, pot_r = co[:, 2].min(), 0.0
z0 = co[:, 2].min()
while z0 < co[:, 2].max():
    m = (co[:, 2] >= z0) & (co[:, 2] < z0 + SLICE)
    if m.sum() >= 10:
        sectors = len(set(((th[m] + np.pi) / (2 * np.pi) * 24).astype(int)))
        if sectors < 22:
            break
        pot_top, pot_r = z0 + SLICE, max(pot_r, r[m].max())
    z0 += SLICE
print(f'AXIS ({ax:+.3f}, {ay:+.3f})  pot top z {pot_top:+.3f}  pot r {pot_r:.3f}')

is_pot = (r < pot_r * 1.15) & (co[:, 2] < pot_top)
print(f'POT verts {is_pot.sum()}   KEPT verts {(~is_pot).sum()}')

bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.select_all(action='DESELECT')
bpy.ops.object.mode_set(mode='OBJECT')
for i, v in enumerate(big.data.vertices):
    v.select = bool(is_pot[i])
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.delete(type='VERT')
bpy.ops.object.mode_set(mode='OBJECT')
print(f'AFTER CUT verts {len(big.data.vertices)}')

# Anything left that is not connected to the biggest island is pot crumbs.
bpy.context.view_layer.objects.active = big
big.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.select_all(action='SELECT')
bpy.ops.mesh.separate(type='LOOSE')
bpy.ops.object.mode_set(mode='OBJECT')
isl = sorted([x for x in bpy.data.objects if x.type == 'MESH'],
             key=lambda q: -len(q.data.vertices))


def axis_offset(q):
    """How far the island's centroid sits from the pot's axis of revolution.

    This is what separates the hand from the plant, and shape is not. Both are
    thick by bounding box (0.44 against 0.42), so a thickness test passes both
    and falls through to vertex count, which picks the plant because a spray of
    leaves carries more geometry than an arm. But the plant GROWS from the pot,
    so it sits on the axis, while the arm reaches in from outside it. Offset
    separates them cleanly where no property of the island alone does.
    """
    c = sum((q.matrix_world @ v.co for v in q.data.vertices),
            mathutils.Vector()) / len(q.data.vertices)
    return math.hypot(c.x - ax, c.y - ay)


print('ISLANDS', [(len(i.data.vertices), round(axis_offset(i), 3)) for i in isl[:8]])
hand = max(isl, key=axis_offset)
# everything that is not the hand, which is no longer isl[0]
for q in [x for x in isl if x is not hand]:
    bpy.data.objects.remove(q, do_unlink=True)
bpy.ops.object.select_all(action='DESELECT')
bpy.context.view_layer.objects.active = hand
hand.select_set(True)

# The pot's rim lip survives the cut and stays welded to the fingertips, so the
# hand comes out trailing a curved ribbon. It cannot be removed by height or by
# radius, because the fingers occupy the same band of both. It CAN be removed by
# angle: the rim is a ring that spans 18 of 24 sectors around the pot axis while
# the arm reaches in from one direction and spans 3. Keeping the arm's own wedge
# drops the ring and leaves the hand whole.
hco = np.array([(hand.matrix_world @ v.co)[:] for v in hand.data.vertices])
hth = np.arctan2(hco[:, 1] - ay, hco[:, 0] - ax)
arm = hth[hco[:, 2] > pot_top + 0.18]
amid = np.arctan2(np.sin(arm).mean(), np.cos(arm).mean())
adev = np.abs(np.arctan2(np.sin(arm - amid), np.cos(arm - amid))).max()
off = np.abs(np.arctan2(np.sin(hth - amid), np.cos(hth - amid)))
drop = off > adev + 0.5          # generous, because fingers splay wider than a wrist
print(f'WEDGE mid {np.degrees(amid):+.1f} deg  half {np.degrees(adev + 0.5):.1f} deg  '
      f'dropping {drop.sum()} of {len(hth)}')
bpy.context.view_layer.objects.active = hand
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.select_all(action='DESELECT')
bpy.ops.object.mode_set(mode='OBJECT')
for i, v in enumerate(hand.data.vertices):
    v.select = bool(drop[i])
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.delete(type='VERT')
bpy.ops.object.mode_set(mode='OBJECT')

bb = [hand.matrix_world @ mathutils.Vector(c) for c in hand.bound_box]
xs = [v.x for v in bb]; ys = [v.y for v in bb]; zs = [v.z for v in bb]
print(f'HAND verts {len(hand.data.vertices)}  size '
      f'{max(xs)-min(xs):.3f} x {max(ys)-min(ys):.3f} x {max(zs)-min(zs):.3f}')

# Normalise, so that placing the hand in the room is one move instead of a
# rotation puzzle. The mesh is rewritten with the FINGERTIP at the origin, the
# forearm along +X, and world up kept as +Z.
#
# That last constraint is the whole point. A rotation that only has to carry one
# axis onto another leaves the roll about that axis free, and four solves for
# the scripted hand each picked a different free roll and each came out wrong.
# Building an orthonormal frame from the arm direction AND up removes the
# freedom: there is exactly one such frame, so there is nothing left to guess.
hco = np.array([(hand.matrix_world @ v.co)[:] for v in hand.data.vertices])
tip = mathutils.Vector(hco[hco[:, 2].argmin()])
cuff = mathutils.Vector(hco[hco[:, 2] > np.percentile(hco[:, 2], 92)].mean(axis=0))
# Rotate about Z ONLY, and keep the sculpt's own tilt.
#
# The first version built a full orthonormal frame carrying the arm onto +X
# with world up preserved, which is correct for an arbitrary direction and
# wrong for this mesh. The hand was sculpted already reaching DOWN into a pot,
# and re-orienting it threw that away: measured in place, the fingertip sat at
# z 1.014 while the lowest point of the hand was at 0.905, so the palm hung
# 109 mm below the finger and the hand landed on the pot's rim instead of its
# soil. The pose was right in the file and the normalisation broke it.
#
# Turning about the vertical axis alone puts the forearm along +X in plan while
# leaving the downward reach exactly as modelled, so the fingertip stays the
# lowest thing on the hand and placing it in the soil puts it in the soil.
az = math.atan2(cuff.y - tip.y, cuff.x - tip.x)
reach = (cuff - tip).length
print(f'NORMALISE tip ({tip.x:+.3f},{tip.y:+.3f},{tip.z:+.3f})  reach {reach:.3f}  '
      f'azimuth {math.degrees(az):+.1f} deg')

bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
M = (mathutils.Matrix.Scale(1.0 / reach, 4)
     @ mathutils.Matrix.Rotation(-az, 4, 'Z')
     @ mathutils.Matrix.Translation(-tip))
hand.data.transform(M)
hand.matrix_world = mathutils.Matrix.Identity(4)
# Flat shading: the room's look is faceted geometry, and a smooth-shaded organic
# sculpt is the one thing in it that would carry a gradient across a surface.
for poly in hand.data.polygons:
    poly.use_smooth = False

if PREVIEW:
    os.makedirs(PREVIEW, exist_ok=True)
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'; sc.cycles.device = 'GPU'; sc.cycles.samples = 24
    sc.render.resolution_x = 520; sc.render.resolution_y = 460
    w = bpy.data.worlds.new('w'); sc.world = w; w.use_nodes = True
    w.node_tree.nodes['Background'].inputs[0].default_value = (0.18, 0.19, 0.22, 1)
    bpy.ops.object.light_add(type='SUN', location=(2, -3, 4))
    bpy.context.object.data.energy = 4
    ctr = sum((hand.matrix_world @ v.co for v in hand.data.vertices),
              mathutils.Vector()) / len(hand.data.vertices)
    rad = max(max(xs)-min(xs), max(ys)-min(ys), max(zs)-min(zs))
    for i, ang in enumerate((0, 60, 130, 210, 300)):
        d = mathutils.Vector((math.cos(math.radians(ang)), math.sin(math.radians(ang)), 0.45))
        bpy.ops.object.camera_add(location=ctr + d * rad * 2.0)
        cam = bpy.context.object
        cam.rotation_euler = (ctr - cam.location).to_track_quat('-Z', 'Y').to_euler()
        cam.data.lens = 60
        sc.camera = cam
        sc.render.filepath = os.path.join(PREVIEW, f'hand-{ang:03d}.png')
        bpy.ops.render.render(write_still=True)

bpy.ops.object.select_all(action='DESELECT')
hand.select_set(True)
bpy.ops.export_scene.gltf(filepath=OUT, use_selection=True, export_format='GLB')
print('WROTE', OUT)
