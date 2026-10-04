"""Solve for the plate look's exposure and shadow floor instead of guessing.

  blender -b --python tools/blender/sweep_plate.py -- OUTDIR [shot]

Builds the room once and only re-wires the compositor between renders, which is
what makes a sweep affordable: the room takes longer to assemble than a small
frame takes to render.
"""
import bpy, sys, os, itertools
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look
look.FLAT = True
import world, plate

a = sys.argv[sys.argv.index('--') + 1:]
OUT = a[0]
SHOT = a[1] if len(a) > 1 else 'scale-table'
os.makedirs(OUT, exist_ok=True)

sc, M = world.build(res=(418, 235), samples=24)
plate.flat_materials()
plate.harden_shadows(0.25)
plate.no_dof()

loc, aim, lens, plens, fstop = world.SHOTS[SHOT]
sc.camera = world.look.camera(loc, aim, lens, None)

# The ranges come from probe_light.py, not from taste. The diffuse pass in this
# room runs to 31 with its median at 0.68 and its 95th at 4.72, so the exposure
# that lands the bright end on the top ramp stop is about 0.21. Two earlier
# sweeps searched 1.4 to 9.0 because I assumed the pass was 0..1 and never
# looked, which is a hundredfold error in the wrong direction.
#
# Gamma matters as much. The distribution is long-tailed, so a linear map leaves
# the median down in the bottom stop whatever the exposure; pulling it up is
# what spreads real surfaces across the steps.
GRID = list(itertools.product(
    [4],                            # steps
    [0.10, 0.18],                   # ambient floor
    [0.21, 0.35],                   # exposure: where the steps land
    [0.45, 0.65],                   # gamma
    [2.4, 3.4, 4.6],                # gain: brightness, restored after stepping
))
print(f'sweeping {len(GRID)} combinations on {SHOT}', flush=True)
for i, (steps, amb, ex, gm, gn) in enumerate(GRID, 1):
    plate.posterise(sc, steps=steps, lift=0.0, gamma=gm, ambient=amb,
                    exposure=ex, gain=gn)
    sc.render.filepath = os.path.join(OUT, f's{steps}_a{amb}_e{ex}_g{gm}_n{gn}.png')
    bpy.ops.render.render(write_still=True)
    print(f'  [{i}/{len(GRID)}] steps={steps} amb={amb} exp={ex} gam={gm} gain={gn}',
          flush=True)
