"""Make Cycles look like the painted plates.

The room was modelled so the shots could not disagree with each other, and then
rendered photoreal, which is a different picture from the one the page shows.
The plates are flat vector: no outlines, form separated by steps of value alone,
at most three flat values per object, hard-edged shadows rather than gradients,
and gradients allowed only for emitted light. A denoised Cycles render with
textures, soft area shadows and depth of field is the opposite of every one of
those.

The usual way to do this in Blender is Shader to RGB with a constant ColorRamp,
which is EEVEE only. EEVEE cannot open a GPU context under WSL: it renders a
flat grey field, measured at mean 54.9 and standard deviation 2.9 on a test
scene with a lit sphere in it, which is nothing at all.

So the stepping happens after the render instead, which Cycles can do because it
will hand over its lighting separately from its colour:

    light     = diffuse direct + diffuse indirect
    stepped   = posterise(value of light), hue and saturation untouched
    result    = diffuse colour x stepped + emission

Posterising the value channel rather than the three colour channels is the part
worth stating. Running a constant ramp over R, G and B independently moves them
across their steps at different moments, so a warm shadow turns green or magenta
on the boundary. Value alone keeps the hue the lamp gave it, which matters here
because every warm surface in the room is lit by one warm lamp.

Emission is added back unstepped on purpose: the style block allows gradients
for emitted light, and the lamp's own glow is the one soft thing in the plates.
"""
import bpy


def flat_materials():
    """Pure diffuse, no texture, no specular. Value steps carry all the form."""
    for m in bpy.data.materials:
        if not m.use_nodes:
            continue
        b = m.node_tree.nodes.get('Principled BSDF')
        if not b:
            continue
        # An emissive surface must not also have an albedo, or the ambient floor
        # lifts it. The night sky, the moon and the lit city windows are all
        # emission over a base colour, and the floor multiplies every albedo in
        # the scene before the gain does, so the sky was arriving at roughly
        # half of full lighting on top of its own glow. That is what made the
        # window a flat bright slab and the whole set look washed: the thing
        # that is supposed to be the darkest surface in the room was being lit
        # like the brightest.
        if b.inputs['Emission Strength'].default_value > 0.0:
            b.inputs['Base Color'].default_value = (0, 0, 0, 1)
        # A specular highlight is a gradient on an unlit surface, which the
        # plates never have. Roughness 1 removes the sheen the renders had on
        # every pot and the table.
        b.inputs['Roughness'].default_value = 1.0
        if 'Specular IOR Level' in b.inputs:
            b.inputs['Specular IOR Level'].default_value = 0.0
        if 'Sheen Weight' in b.inputs:
            b.inputs['Sheen Weight'].default_value = 0.0


def harden_shadows(scale=0.25):
    """Shrink every light. A big area light draws a soft gradient at every
    shadow edge; the plates' shadows are hard polygons, so the sources that
    cast them have to be small."""
    n = 0
    for o in bpy.data.objects:
        if o.type != 'LIGHT':
            continue
        d = o.data
        if hasattr(d, 'size'):
            d.size = max(0.012, d.size * scale)
            if hasattr(d, 'size_y'):
                d.size_y = max(0.012, d.size_y * scale)
        if hasattr(d, 'shadow_soft_size'):
            d.shadow_soft_size = max(0.012, d.shadow_soft_size * scale)
        n += 1
    return n


def no_dof():
    for c in bpy.data.cameras:
        c.dof.use_dof = False


def posterise(sc, steps=4, lift=0.0, gamma=1.0, ambient=0.10,
              exposure=1.0, gain=1.0):
    """Wire the compositor to step the lighting and keep the colour flat."""
    vl = sc.view_layers[0]
    vl.use_pass_diffuse_color = True
    vl.use_pass_diffuse_direct = True
    vl.use_pass_diffuse_indirect = True
    vl.use_pass_emit = True
    vl.use_pass_normal = True        # for the denoiser below

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

    # Denoise the lighting BEFORE it is stepped, and this is the whole reason
    # the shadow edges were speckle rather than polygon.
    #
    # Cycles' own denoising is on, but it denoises the Combined pass. This graph
    # never looks at Combined: it consumes DiffDir and DiffInd, which are raw.
    # Running a constant ramp over a noisy signal does not average the noise, it
    # promotes it: a pixel half a sample either side of a step boundary lands in
    # a different band from its neighbour, so sampling noise becomes step
    # assignment and every edge dithers. Hardening the lights made it worse,
    # because a smaller source is a noisier one.
    #
    # Albedo and Normal are fed in because a denoiser given only colour cannot
    # tell a soft shadow from a noisy one, and will flatten both.
    den = nt.nodes.new('CompositorNodeDenoise')
    nt.links.new(add.outputs[0], den.inputs['Image'])
    if 'Albedo' in den.inputs:
        nt.links.new(rl.outputs['DiffCol'], den.inputs['Albedo'])
    if 'Normal' in den.inputs and 'Normal' in rl.outputs:
        nt.links.new(rl.outputs['Normal'], den.inputs['Normal'])

    sep = nt.nodes.new('CompositorNodeSeparateColor'); sep.mode = 'HSV'
    nt.links.new(den.outputs[0], sep.inputs[0])

    # Exposure goes in BEFORE the steps, and that ordering is the whole thing.
    #
    # The first version multiplied after the ramp, which looks equivalent and is
    # not. The ramp's stops are positions on 0..1, but the raw diffuse pass in a
    # room lit by one lamp peaks around 0.3, so every pixel landed in the bottom
    # one or two stops and the top of the ramp was never reached. The picture
    # came out with two usable value levels instead of four, and brightening
    # afterwards just scaled those two. Scaling first spreads the real range
    # across the stops, which is what asking for four value steps meant.
    pre = nt.nodes.new('CompositorNodeMath'); pre.operation = 'MULTIPLY'
    pre.inputs[1].default_value = exposure
    nt.links.new(sep.outputs[2], pre.inputs[0])

    shape = nt.nodes.new('CompositorNodeMath'); shape.operation = 'POWER'
    shape.inputs[1].default_value = gamma
    nt.links.new(pre.outputs[0], shape.inputs[0])

    ramp = nt.nodes.new('CompositorNodeValToRGB')
    ramp.color_ramp.interpolation = 'CONSTANT'
    e = ramp.color_ramp.elements
    while len(e) > 1:
        e.remove(e[-1])
    e[0].position, e[0].color = 0.0, (lift, lift, lift, 1)
    for i in range(1, steps):
        p = i / steps
        v = lift + (1.0 - lift) * (i / (steps - 1))
        el = e.new(p)
        el.color = (v, v, v, 1)
    nt.links.new(shape.outputs[0], ramp.inputs[0])

    tov = nt.nodes.new('CompositorNodeSeparateColor'); tov.mode = 'HSV'
    nt.links.new(ramp.outputs[0], tov.inputs[0])

    comb = nt.nodes.new('CompositorNodeCombineColor'); comb.mode = 'HSV'
    nt.links.new(sep.outputs[0], comb.inputs[0])     # hue from the real light
    nt.links.new(sep.outputs[1], comb.inputs[1])     # saturation too
    nt.links.new(tov.outputs[2], comb.inputs[2])     # value from the steps

    # A floor under the shadows, and a gain over the whole thing.
    #
    # Both are measured, not taste. The plate's darkest tenth sits at 0.099 and
    # the first flat render bottomed out at 0.012: Cycles is right that an
    # unlit surface in a dark room receives almost nothing, and the plate is
    # painted, so its shadows keep enough of their own colour to read as wood
    # and clay rather than as holes. The floor puts that back. The gain exists
    # because the same render came out 1.9x dark against the plate overall.
    floor = nt.nodes.new('CompositorNodeMixRGB'); floor.blend_type = 'ADD'
    floor.inputs[0].default_value = 1.0
    nt.links.new(comb.outputs[0], floor.inputs[1])
    floor.inputs[2].default_value = (ambient, ambient, ambient, 1)

    mul = nt.nodes.new('CompositorNodeMixRGB'); mul.blend_type = 'MULTIPLY'
    mul.inputs[0].default_value = 1.0
    nt.links.new(rl.outputs['DiffCol'], mul.inputs[1])
    nt.links.new(floor.outputs[0], mul.inputs[2])

    # Brightness has to be its own control, separate from exposure, and this is
    # the correction for two sweeps that went the wrong way.
    #
    # Posterising normalises the lighting to 0..1 by construction. The pass in
    # this room reaches 31, so everything above the top stop is flattened onto
    # it and the picture loses the brightness it had before stepping. Exposure
    # decides WHERE the steps fall and necessarily destroys magnitude doing it;
    # the gain puts the magnitude back afterwards. Trying to get both from one
    # number is why raising exposure made the picture darker and flatter at the
    # same time, which should have been the clue.
    post = nt.nodes.new('CompositorNodeMixRGB'); post.blend_type = 'MULTIPLY'
    post.inputs[0].default_value = 1.0
    nt.links.new(mul.outputs[0], post.inputs[1])
    post.inputs[2].default_value = (gain, gain, gain, 1)

    glow = nt.nodes.new('CompositorNodeMixRGB'); glow.blend_type = 'ADD'
    glow.inputs[0].default_value = 1.0
    nt.links.new(post.outputs[0], glow.inputs[1])
    nt.links.new(rl.outputs['Emit'], glow.inputs[2])

    nt.links.new(glow.outputs[0], out.inputs[0])
    return nt


def apply(sc, steps=4, lift=0.0, gamma=0.55, light_scale=0.25,
          ambient=0.14, exposure=0.28, gain=3.4):
    flat_materials()
    n = harden_shadows(light_scale)
    no_dof()
    posterise(sc, steps=steps, lift=lift, gamma=gamma,
              ambient=ambient, exposure=exposure, gain=gain)
    print(f'plate look: {steps} steps, exposure {exposure} (where the steps land), '
          f'gain {gain} (brightness), ambient {ambient}, gamma {gamma}, '
          f'{n} lights hardened')
