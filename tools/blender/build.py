"""Assemble the full set and expose the camera positions the shots use."""
import bpy, math
from mathutils import Vector
from common import reset, mat, put, look_at, aim, LAMP, MOON, LEAF
from parts import build_pot, build_plant, dress_soil, build_finger, add_dimple_key
from scene import build_room, build_scale, build_phone, build_arm

SILL = (0.050, 1.095, 0.952)      # pot on the windowsill
TABLE_Z = 0.760
TPOT = (0.620, 0.150, TABLE_Z + 0.046)   # pot standing on the scale plate


def build_table():
    top_m = mat('tabletop', (0.042, 0.034, 0.024, 1), 0.66)
    leg_m = mat('leg', (0.040, 0.034, 0.026, 1), 0.7)
    bpy.ops.mesh.primitive_cube_add(size=1, location=(0.72, 0.12, TABLE_Z - 0.014))
    t = bpy.context.object
    t.scale = (1.30, 0.70, 0.028)
    b = t.modifiers.new('bev', 'BEVEL'); b.width, b.segments = 0.004, 2
    put(t, top_m)
    for sx in (-0.58, 0.58):
        for sy in (-0.30, 0.30):
            bpy.ops.mesh.primitive_cube_add(size=1,
                                            location=(0.72 + sx, 0.12 + sy, (TABLE_Z - 0.03) / 2))
            l = bpy.context.object
            l.scale = (0.045, 0.045, TABLE_Z - 0.03)
            put(l, leg_m, shade_smooth=False)
    return t


def plant_on(pot_loc, gap_dir, leaf_mat, stem_mat, n=6):
    return build_plant(leaf_mat, n=n, stem_mat=stem_mat,
                       base=(pot_loc[0], pot_loc[1], pot_loc[2] + 0.112),
                       gap_dir=gap_dir, gap_width=1.25,
                       leaf_len=0.082, leaf_var=0.024, tilt_base=1.10, width=0.027)


def build_all(samples=128, res=(1280, 720), flat=False):
    sc = reset(res=res, samples=samples, flat=flat)
    sc['flat'] = flat
    leaf_m = mat('leafm', LEAF, 0.42)
    stem_m = mat('stemm', (0.040, 0.100, 0.050, 1), 0.5)

    build_room()
    build_table()

    # --- the sill: the pot being poked
    p1 = build_pot(loc=SILL)
    dress_soil(p1[2])
    add_dimple_key(p1[2], (SILL[0] + 0.016, SILL[1] - 0.004), r=0.026, depth=0.010)
    plant_on(SILL, gap_dir=-2.20, leaf_mat=leaf_m, stem_mat=stem_m)
    finger = build_finger()
    finger.location = (SILL[0] + 0.015, SILL[1] - 0.003, SILL[2] + 0.1045)
    finger.rotation_euler = (0.0, -0.72, -0.63)
    bpy.context.view_layer.update()
    # the arm has to end exactly where the finger's cropped stub ends, or the
    # two read as separate objects in the wide shot
    wrist = finger.matrix_world @ Vector((0.128, 0.033, 0.044))
    arm = build_arm(wrist, (1.05, 0.05, 1.60))
    # the arm rides with the finger, so pushing the finger in moves the whole
    # limb. Its far end leaves the frame, so nobody sees that end move too.
    arm.parent = finger
    arm.matrix_parent_inverse = finger.matrix_world.inverted()

    # --- the table: the same pot, on the scale
    readout = build_scale((TPOT[0], TPOT[1], TABLE_Z))
    p2 = build_pot(loc=TPOT)
    dress_soil(p2[2], dimple_at=(TPOT[0] + 0.016, TPOT[1] - 0.004),
               dimple_r=0.026, dimple_d=0.010)
    plant_on(TPOT, gap_dir=-1.55, leaf_mat=leaf_m, stem_mat=stem_m)
    phone = build_phone((1.02, 0.02, TABLE_Z + 0.082), rot_z=-0.55)

    # --- light. One warm lamp in the room and the moon outside, nothing else:
    # the phone screen and the scale readout are their own sources.
    if flat:
        bpy.ops.object.light_add(type='SUN', location=(1.62, -0.25, 1.90))
        k = bpy.context.object
        k.data.energy, k.data.angle, k.data.color = 3.1, 0.03, LAMP
        aim(k, (0.70, 0.35, 0.85))
    else:
        bpy.ops.object.light_add(type='AREA', location=(1.62, -0.25, 1.62))
        k = bpy.context.object
        k.data.energy, k.data.size, k.data.color = 55.0, 0.42, LAMP
        aim(k, (0.78, 0.15, 0.80))

    if flat:
        bpy.ops.object.light_add(type='SUN', location=(0.10, 2.30, 2.10))
        m = bpy.context.object
        m.data.energy, m.data.angle, m.data.color = 1.1, 0.05, MOON
        aim(m, (0.05, 1.05, 1.05))
    else:
        bpy.ops.object.light_add(type='AREA', location=(0.30, 2.30, 2.10))
        m = bpy.context.object
        m.data.energy, m.data.size, m.data.color = 22.0, 1.60, MOON
        aim(m, (0.05, 1.05, 1.05))

    if not flat:
        bpy.ops.object.light_add(type='AREA', location=(0.86, 0.52, 1.46))
        s2 = bpy.context.object
        s2.data.energy, s2.data.size, s2.data.color = 16.0, 0.30, LAMP
        aim(s2, (0.06, 1.08, 1.03))

    w = bpy.data.worlds.new('w'); sc.world = w; w.use_nodes = True
    # In flat mode the world IS the shadow colour, so it is chosen rather than
    # left near black: a cel shadow has to be a value you can read.
    w.node_tree.nodes['Background'].inputs[0].default_value = (
        (0.125, 0.145, 0.185, 1) if flat else (0.006, 0.006, 0.009, 1))
    return sc, dict(finger=finger, arm=arm, phone=phone, readout=readout,
                    sill_soil=p1[2], table_soil=p2[2])


# (location, aim, lens, f-stop). The f-stop is per shot because depth of
# field at 0.28 m is a few millimetres deep: the aperture that gives the wide
# shot its bokeh turns the close shot into a smear.
CAMS = {
    'sill_wide':   ((-0.62, -0.52, 1.30), (0.16, 1.00, 1.06), 32, 3.5),
    'sill_mid':    ((-0.190, 0.560, 1.320), (0.050, 1.070, 1.050), 45, 9.0),
    'sill_tight':  ((-0.103, 0.884, 1.230), (0.055, 1.085, 1.056), 60, 22.0),
    'table_wide':  ((0.16, -1.12, 1.16), (0.78, 0.12, 0.88), 40, 4.5),
    'table_scale': ((0.33, -0.56, 1.02), (0.62, 0.15, 0.845), 55, 11.0),
    'phone':       ((0.72, -0.52, 0.99), (1.00, 0.02, 0.845), 55, 11.0),
}


def set_cam(sc, name, fstop=None):
    loc, aim, lens, f = CAMS[name]
    fstop = fstop or f
    bpy.ops.object.camera_add(location=loc)
    cam = look_at(bpy.context.object, aim, lens=lens)
    sc.camera = cam
    cam.data.dof.use_dof = not sc.get('flat', False)
    cam.data.dof.focus_distance = math.dist(loc, aim)
    cam.data.dof.aperture_fstop = fstop
    return cam
