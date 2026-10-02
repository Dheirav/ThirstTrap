"""The shot: camera moves, the finger going in and coming out, the hole it
leaves, and the scale settling on 218 g.

Scroll scrubs these frames, so the timeline IS the storyboard. Frame numbers
are the single source of truth for both the render and the page.
"""
import bpy, math
from mathutils import Vector, Euler
from common import look_at
from build import CAMS, SILL

# ---- the storyboard, in frames -------------------------------------------
F_END        = 140
F_DESCEND    = (25, 52)    # finger comes down and pushes in
F_HOLD       = (53, 74)    # it stays in while the camera is tight
F_WITHDRAW   = (75, 92)    # it comes out, leaving the hole
F_CUT        = 93          # hard cut to the table
F_PULLBACK   = (93, 120)   # reveal the pot standing on the scale
F_PHONE      = (121, 140)  # move across to the phone

# unit vector the fingertip travels along, derived from the finger's own
# orientation so the push is along the finger rather than straight down
_ROT = (0.0, -0.72, -0.63)


def _axis():
    """The direction the fingertip travels: along the finger, not straight down."""
    v = Vector((1, 0, 0))
    v.rotate(Euler(_ROT, 'XYZ'))
    return -v.normalized()


def animate_finger(finger, deep=0.0, out=0.045):
    """Push the finger in over F_DESCEND, hold, then pull it out."""
    ax = _axis()
    base = Vector(finger.location)

    def at(frame, d):
        finger.location = base + ax * d
        finger.keyframe_insert('location', frame=frame)

    at(1, -out)
    at(F_DESCEND[0], -out)
    at(F_DESCEND[1], deep)
    at(F_HOLD[1], deep)
    at(F_WITHDRAW[1], -out)
    at(F_END, -out)


def animate_dimple(soil, key_name='Dimple'):
    """The hole deepens as the finger goes in, and stays once it leaves."""
    kb = soil.data.shape_keys.key_blocks.get(key_name)
    if not kb:
        return
    for frame, v in [(1, 0.0), (F_DESCEND[0], 0.0), (F_DESCEND[1], 1.0), (F_END, 1.0)]:
        kb.value = v
        kb.keyframe_insert('value', frame=frame)


def screen_view(phone, dist=0.30):
    """Camera straight down the phone screen's own normal.

    The handoff crossfades into a flat HTML page, so the last frame has to show
    the screen square-on. Any off-axis angle leaves the rendered screen a
    trapezoid and the cut shows.
    """
    m = phone.matrix_world
    n = (m.to_3x3() @ Vector((0, -1, 0))).normalized()
    centre = m @ Vector((0, -0.0046, 0))
    return tuple(centre + n * dist), tuple(centre)


def animate_camera(sc, phone=None):
    """One camera, keyframed through both shots, with a hard cut at F_CUT.

    A cut is a cut: the keyframe before it is set to CONSTANT so the camera
    does not fly across the room between the sill and the table.
    """
    bpy.ops.object.camera_add(location=CAMS['sill_wide'][0])
    cam = bpy.context.object
    sc.camera = cam
    cam.data.dof.use_dof = not sc.get('flat', False)
    cam.data.dof.aperture_fstop = 4.0

    def key(frame, preset, custom=None):
        loc, aim, lens, fstop = custom if custom else CAMS[preset]
        cam.location = loc
        look_at(cam, aim, lens=lens)
        cam.data.dof.focus_distance = math.dist(loc, aim)
        cam.data.dof.aperture_fstop = fstop
        cam.keyframe_insert('location', frame=frame)
        cam.keyframe_insert('rotation_euler', frame=frame)
        cam.data.keyframe_insert('lens', frame=frame)
        cam.data.dof.keyframe_insert('focus_distance', frame=frame)
        cam.data.dof.keyframe_insert('aperture_fstop', frame=frame)

    key(1, 'sill_wide')
    key(24, 'sill_wide')
    key(F_DESCEND[1], 'sill_mid')
    key(F_HOLD[0], 'sill_tight')
    key(F_WITHDRAW[1], 'sill_tight')
    key(F_CUT - 1, 'sill_tight')
    key(F_CUT, 'table_scale')
    key(F_PULLBACK[1], 'table_wide')
    key(F_PHONE[0], 'table_wide')
    if phone is not None:
        loc, aim = screen_view(phone, dist=0.30)
        # 138 mm at 0.30 m makes the 71.5 mm screen fill the frame width, which
        # is the same cover framing the HTML stage uses when it fades in
        key(F_END, None, custom=(loc, aim, 138.0, 14.0))
    else:
        key(F_END, 'phone')

    # freeze everything through the cut so nothing interpolates across it
    for data in (cam.animation_data, cam.data.animation_data,
                 cam.data.dof.id_data.animation_data):
        if not data or not data.action:
            continue
        for fc in data.action.fcurves:
            for k in fc.keyframe_points:
                if k.co.x <= F_CUT - 1:
                    k.interpolation = 'BEZIER'
                if abs(k.co.x - (F_CUT - 1)) < 0.5:
                    k.interpolation = 'CONSTANT'
    return cam


def animate_readout(readout, final=218):
    """The scale settles rather than snapping: it overshoots once, the way a
    real one does when you set a pot down on it."""
    seq = {}
    a, b = F_PULLBACK
    for f in range(a, b + 1):
        t = (f - a) / max(1, (b - a))
        if t < 0.18:
            g = final * (t / 0.18) * 1.06
        elif t < 0.34:
            g = final * (1.06 - 0.09 * ((t - 0.18) / 0.16))
        else:
            g = final * (0.97 + 0.03 * min(1.0, (t - 0.34) / 0.22))
        seq[f] = f'{int(round(g))} g'
    for f in range(b + 1, F_END + 1):
        seq[f] = f'{final} g'

    def on_frame(scene, _depsgraph=None):
        readout.data.body = seq.get(scene.frame_current, f'{final} g')

    bpy.app.handlers.frame_change_pre.append(on_frame)
    return on_frame
