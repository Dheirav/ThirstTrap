# How AI is actually used in Blender work

Researched 2026-10-05, prompted by a day spent making the modelled room look
like the painted plates. Written against that, not as a general survey, because
the useful question is not "what exists" but "which of these would have helped
with what we were doing, and which would have made it worse".

Sources at the end. Where something is a vendor's claim rather than something I
could check, it says so.

## The four places it shows up

AI enters a Blender pipeline at four separate points, and they are not equally
mature. Treating them as one thing called "AI for Blender" is the main reason
people get disappointed.

### 1. Generating geometry from a prompt or an image

Text-to-3D and image-to-3D. Meshy, Rodin, Tripo and others take a sentence or a
reference picture and return a textured mesh, usually in under a minute, often
through a Blender plugin that imports it for you.

This is the part that demos well and disappoints in production, and the reason
is consistent across every source that is not selling one: **the topology is
wrong**. These models reconstruct a surface rather than build one, so what comes
back is dense irregular triangles with no edge flow, non-manifold edges, holes,
internal faces and self-intersections. Tripo's own write-up puts the cleanup at
two to four hours per asset before it can enter a real pipeline, which is a
vendor admitting the problem exists before selling you the fix.

It matters more than it sounds. Bad topology does not just mean an untidy mesh.
It means the thing cannot be deformed, so it cannot be rigged or animated, and
it means a subdivision or a bevel will pinch. For a prop that never moves and is
seen from one angle it is survivable. For anything else it is a rebuild.

The newer generators claim to produce clean quad topology as part of generation
rather than as a cleanup pass. That claim is recent and I have not tested it.

### 2. Generating textures and materials

This is the most settled of the four and the one with real open-source options.
Dream Textures runs Stable Diffusion inside Blender and can project generated
images onto a mesh. Others wrap hosted models: `blender-ai-textures` calls SDXL
and Flux through Replicate, `diffused-texture-addon` generates diffuse maps onto
a mesh directly, AI Material Factory produces PBR stacks.

It works because a texture is an image, and image generation is the thing these
models are genuinely good at. There is no topology to get wrong. The honest
limit is that a diffuse map is not a material: normal, roughness and metalness
either get derived from the colour by another model, which guesses, or you make
them yourself.

### 3. Driving Blender by script, through an agent

This is what today was. An LLM writes `bpy` Python and runs it, which is a
different activity from generating a mesh: the output is code, and the geometry
is whatever that code constructs.

The packaged form is BlenderMCP, a Blender addon plus a Model Context Protocol
server that exposes the Python API to any MCP-capable client. The agent gets
tools to create meshes, set materials, select objects, run arbitrary Python and
trigger renders. Several write-ups describe an officially maintained Blender
connector for Claude; I have not confirmed that against blender.org itself, so
treat it as reported rather than established.

Worth saying plainly, because it is the whole lesson of today: this mode has no
topology problem, because nothing is being reconstructed. A cone is a cone. What
it has instead is that **the agent cannot see**. Every real error today was
caught by rendering the frame and looking at it next to the plate, and several
came from me reasoning confidently about geometry I had not projected: the light
pass I assumed ran 0 to 1 and which actually ran to 31, the channel I thought
was missing when it had been in frame at 15% of the width the whole time, three
cameras nudged by eye that all put it off the top edge.

### 4. Inside the renderer

The quiet one. Cycles' denoiser is a trained model and has been shipping for
years, which is why 96 samples is enough for the frames in this project instead
of several thousand. Nobody calls it AI because it works.

Rigging is similar. Auto-Rig Pro detects a character's structure and builds a
rig with IK/FK and facial controls, and retargets mocap onto it. That is
learned-model work doing a job that used to be a week.

## What this project actually used, and what it did not

The plates were generated: ChatGPT, one scene per chat, through a driven
browser. The room is hand-scripted `bpy`, written by an agent reading
measurements. Those are categories 1 and 3, and the difference between them is
the whole story of the last few days.

The generated plates drifted. Nothing made two of them agree, and the fixes were
all about imposing agreement from outside: a room block, a prop sheet, a lint.
Even then the pots came back as different pots, because a sentence is not a
specification.

The scripted room cannot drift. The pot on the scale is the same function call
as the pot on the sill. That is not a quality claim, since the renders are still
behind the paintings, but it is a structural one, and it is why the answer to
"the phone looks different between plates" was eventually "model the room".

**What would plausibly have helped:** texture generation, for the one thing in
this room that is genuinely an image. The ledger sheet was drawn with PIL and a
chancery font and it is fine, but a generated paper texture would be better.
HDRI generation might have helped the window, although an enclosed room gets
almost nothing from a world light, which was measured here earlier.

**What would have hurt:** text-to-3D for the props. A generated mug arrives with
triangle soup, its own UV layout and a baked PBR material, and every one of
those has to be stripped, because this look is flat colour with posterised
lighting and nothing photoreal survives the trip. The existing Poly Haven assets
already caused exactly this: a footed goblet instead of a plain pot, alpha-card
leaves that need a special material path, a root cluster that flat-shades into a
crumpled beige sheet. All three were eventually deleted and modelled by hand in
fewer lines than the code that was correcting them.

That is the general shape of it. **The more stylised the target, the worse
generated geometry performs**, because generation is trained toward plausible
reality and a flat-vector night interior is a deliberate departure from it.

## The licensing position, which matters if this ships

Blender's own stance is explicit and worth knowing before leaning on any of
this. The Foundation states there is no generative AI in Blender and none
planned, and positions the tool as made by humans for humans.

For contributions the policy splits. AI assistance is permitted for code, with
conditions, and generated code can be rejected on copyright risk if it is not
clearly an extension or refactor of what is already there. For assets it is a
flat no: contributors should not submit AI-generated images or 3D assets,
because copyright provenance is much harder to verify for an image than for a
patch.

Blender Artists, the community site, runs the same line for artwork: AI may
assist, majority-AI generation is not allowed.

None of that binds a private project. It does mean that if any of this were ever
contributed upstream or posted there, the plates would be a problem and the
scripted room would not. It is also a reasonable proxy for how a client or an
employer is likely to think about it.

## What I would take from this

**Generation is good at images and bad at geometry, and the gap is topology.**
That one sentence predicts most of the disappointment in this area.

**Scripting is the mode that suits this project**, because the thing we wanted
was consistency, and a function called twice is consistent in a way that two
prompts never are.

**Neither mode can see.** The plate comparison caught every error that mattered
today, in both modes: the drifting mug across six paintings, and the channel
that measurements insisted was fine. Whatever else gets automated, that step is
the one to keep doing by eye.

## Sources

- [11 Best Blender AI Tools & Plugins (2026 Compared)](https://www.myarchitectai.com/blog/blender-ai)
- [10 AI 3D Tools to Supercharge Your Blender & Unreal Workflow in 2026](https://www.3daistudio.com/3d-generator-ai-comparison-alternatives-guide/best-3d-generation-tools-2026/10-ai-3d-tools-supercharge-blender-unreal-workflow-2026)
- [Best AI Tools for Blender in 2026](https://www.3daistudio.com/blog/best-ai-tools-for-blender-2026)
- [blender-ai-textures (SDXL / Flux via Replicate)](https://github.com/temporarystudios/blender-ai-textures)
- [diffused-texture-addon](https://github.com/FrederikHasecke/diffused-texture-addon)
- [Cozy-Auto-Texture](https://github.com/torrinworx/Cozy-Auto-Texture)
- [AI Material Factory](https://superhivemarket.com/products/ai-material-factory/docs)
- [BlenderMCP (ahujasid)](https://mcpservers.org/servers/ahujasid/blender-mcp)
- [Blender MCP with Claude Code: a secure scripting setup](https://agentkit.best/blog/claude-code-guides/blender-mcp-with-claude-code-a-secure-ai-scripting-setup-guide)
- [Claude + Blender MCP: what it can and cannot do](https://www.mindstudio.ai/blog/claude-blender-mcp-real-world-performance)
- [Why AI 3D Models Have Bad Topology, And How to Fix It](https://www.tripo3d.ai/blog/why-ai-3d-models-have-bad-topology)
- [AI 3D Model Cleanup: a shrinkwrap workflow](https://www.tripo3d.ai/blog/explore/ai-3d-model-generator-and-shrinkwrap-cleanup-workflow)
- [Hunyuan3D Studio: end-to-end pipeline for game-ready assets (arXiv)](https://arxiv.org/pdf/2509.12815)
- [Auto-Rig Pro 2026 guide](https://www.blenderloop.com/2026/06/auto-rig-pro-2026-best-blender-auto.html)
- [Blender: upcoming Development Fund and AI policies](https://www.blender.org/news/upcoming-blender-development-fund-and-ai-policies/)
- [Blender devtalk: AI Contributions Policy](https://devtalk.blender.org/t/ai-contributions-policy/44202)
- [80.lv: Blender clarifies its AI policy](https://80.lv/articles/blender-clarifies-ai-policy-and-amends-anthropic-donation)
- [Blender Artists: clarification on AI generated work](https://blenderartists.org/t/clarification-on-ai-generated-work/1598841)
