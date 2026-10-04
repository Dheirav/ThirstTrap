"""What range does the diffuse light pass actually cover?

The sweep went in circles because the ramp's stops are positions on 0..1 and
nobody had measured where the lighting in this room actually lies. Too little
gain and every pixel sits in the bottom stop; too much and they all pile on the
top one, which is why `steps` stopped changing anything. One render answers it.
"""
import bpy, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import look
look.FLAT = True
import world, plate

a = sys.argv[sys.argv.index('--') + 1:]
OUT, SHOT = a[0], (a[1] if len(a) > 1 else 'scale-table')
os.makedirs(OUT, exist_ok=True)
sc, M = world.build(res=(418, 235), samples=32)
plate.flat_materials(); plate.harden_shadows(0.25); plate.no_dof()
loc, aim, lens, plens, fstop = world.SHOTS[SHOT]
sc.camera = world.look.camera(loc, aim, lens, None)

vl = sc.view_layers[0]
for p in ('diffuse_color', 'diffuse_direct', 'diffuse_indirect', 'emit'):
    setattr(vl, f'use_pass_{p}', True)
sc.use_nodes = True
nt = sc.node_tree
for n in list(nt.nodes):
    nt.nodes.remove(n)
rl = nt.nodes.new('CompositorNodeRLayers')
out = nt.nodes.new('CompositorNodeComposite')
add = nt.nodes.new('CompositorNodeMixRGB'); add.blend_type = 'ADD'
add.inputs[0].default_value = 1.0
nt.links.new(rl.outputs['DiffDir'], add.inputs[1])
nt.links.new(rl.outputs['DiffInd'], add.inputs[2])
nt.links.new(add.outputs[0], out.inputs[0])
# Read the result inside Blender. There is no EXR reader in this environment's
# python, and rendering to PNG would clip and gamma the very numbers being
# measured.
sc.view_settings.view_transform = 'Raw'
sc.render.image_settings.file_format = 'OPEN_EXR'
sc.render.image_settings.color_depth = '32'
bpy.ops.render.render()
# Render Result keeps no accessible pixel buffer in background mode, so it is
# written out and read back as an ordinary image.
tmp = os.path.join(OUT, 'light.exr')
bpy.data.images['Render Result'].save_render(filepath=tmp)
img = bpy.data.images.load(tmp)
buf = list(img.pixels)
import statistics
L = sorted(0.2126 * buf[i] + 0.7152 * buf[i + 1] + 0.0722 * buf[i + 2]
           for i in range(0, len(buf), 4))
def q(p):
    return L[min(len(L) - 1, int(p * len(L)))]
print('LIGHTPASS ' + ' '.join(f'p{int(p*100)}={q(p):.4f}'
                              for p in (.1, .25, .5, .75, .9, .95, .99)))
print(f'LIGHTPASS max={L[-1]:.4f}')
print(f'EXPOSURE to put p95 on the top stop: {1.0 / max(q(.95), 1e-6):.2f}')
