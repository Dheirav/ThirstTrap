# ThirstTrap hero scene prompts

Date: 2026-10-03. Concept art for the landing page hero, generated the same way
the axl mascot in-betweens were: by driving the ChatGPT website in a signed-in
browser, one scene per fresh chat, with reference images attached.

These started as concepts to draw from and are now the shipping plates: the
page renders them directly, encoded by `encode.py`. The generated SVG opening
scene they replaced is gone. What that changes is that composition is no longer
advisory, because the joins between plates are solved against measured positions
in the images themselves. See section 7 of `docs/HERO-NARRATIVE.md`.

    cd /home/dheirav/Code/Axl/tools/chatgpt-gen
    ./browser.sh                                  # once, signed in, leave open
    node gen.mjs --prompts /home/dheirav/Code/ThirstTrap/tools/hero-gen/PROMPTS-hero.md \
                 --out /home/dheirav/Code/ThirstTrap/tools/hero-gen/out --all
    ./progress.sh --watch

The attached references are screenshots of observal.io, used for its
illustration language only. Palette and subject are ours and are specified in
the style block, which overrides anything the references suggest.

## Style block (paste first, every time)

    Flat vector illustration, in the manner of the attached reference
    screenshots: no outlines anywhere, form separated by steps of value
    alone, at most three flat values per object, hard-edged polygonal
    shadows rather than gradients, and a fine film grain over everything.
    Gradients are permitted only for emitted light such as a lamp glow.

    Use ONLY this palette and ignore the colours in the references. Ground
    and shadow: very dark warm brown, #1E1913 to #2E2820. Paper and text:
    #F5F0E2. Foliage: #2F5D3A and #43794F. Terracotta: a muted #8C4F43,
    deliberately LESS saturated than the accent. Night sky through a window:
    #22303C. A single warm accent, #C98A3A, which must cover well under one
    percent of the picture and must mark exactly one thing.

    A quiet domestic interior at night, lit by one warm lamp and by
    moonlight through a window. Calm, observational, unsentimental. No text,
    no labels, no logos, no user interface, no watermark. Nobody is smiling
    at the camera. Landscape, sixteen by nine.

## Scenes

**shelf-evening.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-room.png`, `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-hero.png`:

    Four small terracotta pots in a row on a windowsill, seen straight on at
    eye level, each with a low rosette of narrow green leaves. A metal
    watering can tipped over the leftmost pot, water falling in a thin
    stream into the soil. All four soil surfaces are dark and wet, because
    all four have just been watered. A night window behind the row with a
    moon. The room beyond the sill is almost black.

**finger-test.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`, `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-room.png`:

    Close on a single terracotta pot on a sill at night. A hand enters from
    the right and one index finger is pushed into the soil up to the first
    knuckle, leaving a small dished hole around it. The hand is cropped at
    the wrist by the edge of the frame. Only the finger, the soil surface
    and the pot rim are lit; everything else falls away into the dark.

**section.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-room.png`:

    A terracotta pot cut cleanly in half and seen from the side, like a
    diagram in a soil manual but drawn in the same flat illustrative style.
    The cut face of the pot wall is thick and visible. Inside, the top two
    centimetres of soil are pale and dry while everything below is dark and
    wet, with a network of fine pale roots in the lower two thirds. A single
    small depth mark in the warm accent colour sits beside the boundary
    between the dry band and the wet body. No text.

**scale-table.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A terracotta pot with a small plant standing on a flat kitchen scale on
    a wooden table, lit by one warm lamp from the upper right. The scale has
    a small dark readout panel on its front lip. A phone lies face up on the
    table beside it, its screen a pale rectangle of warm white. The rest of
    the room is dark. Seen from slightly above.

**ledger.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`, `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-hero.png`:

    A single sheet of cream paper pinned to a dark wall beside a window at
    night, lit warmly from one side. The sheet is ruled into seven rows with
    fine hairlines and a heavier rule under its heading. Each row is a short
    handwritten date on the left and the single handwritten word "Watered" repeated on the
    right, down the whole page, identical in every row. The repetition is the
    subject. Shown at an angle so the sheet reads as paper rather than a
    flat graphic.

**phone-closeup.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A close view of a phone lying flat on a wooden table at night, seen at a
    slight angle from above, filling a good third of the frame. Beside it and
    slightly behind, out of focus only in the sense of being less lit, the
    base of a kitchen scale with a terracotta pot standing on it. One warm
    lamp from the upper right, the rest of the room falling away dark. The
    phone's screen is switched OFF: a flat, very dark, slightly reflective
    rectangle with nothing on it at all, no icons, no glow, no text. The
    screen is the largest clean shape in the picture.

**depth.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A terracotta pot cut cleanly in half from the side so the full depth of
    soil is visible, standing on a table at night. The top band of soil, about
    a sixth of the pot's depth, is pale and crumbly and dry. Everything below
    it is dark, dense and visibly damp, with a faint sheen. A single finger
    enters from the right and reaches only into that pale top band, stopping
    well short of the wet soil. The composition is the comparison: the shallow
    reach against the depth it cannot read.

**roots.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A close view inside dark damp potting soil, filling the frame, with a
    network of fine pale roots spreading through it. Through the middle of the
    picture runs a fresh channel where something has been pushed in and
    withdrawn, and the fine roots along its edges are visibly torn and broken,
    pale ends hanging loose. Lit warmly from one side so the broken ends catch
    the light. No hand, no pot rim, no room. Just soil and roots and the damage.

**ledger-p.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-hero.png`:

    A single sheet of cream paper pinned to a dark wall beside a window at
    night, lit warmly from one side. The sheet is ruled into seven rows with
    fine hairlines. Each row is a short handwritten date on the left and the
    single handwritten word "Watered" on the right, identical in every row.
    The repetition is the subject.

    Portrait orientation, nine by sixteen, which overrides the landscape
    instruction in the style block. Compose for a tall frame: stack the
    subject vertically, keep it central, and let the top and bottom of the
    picture carry quiet dark areas rather than cropping a wide scene.

**shelf-evening-p.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-hero.png`:

    A row of four small terracotta pots of leafy plants on a windowsill at
    night, seen along the sill so they recede up the frame rather than across
    it, with all four clearly countable. A metal watering can tips over the
    nearest one and pours a thin stream into it. Night city beyond the glass.

    Portrait orientation, nine by sixteen, which overrides the landscape
    instruction in the style block. Compose for a tall frame: stack the
    subject vertically, keep it central, and let the top and bottom of the
    picture carry quiet dark areas rather than cropping a wide scene.

**finger-test-p.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    One terracotta pot of a leafy plant on a windowsill at night, filling the
    middle of the frame. A hand enters from the side, cropped at the wrist, and
    one finger is pushed into the soil up to the first knuckle. Lit warmly from
    one side so the hand reads as a silhouette with a lit edge.

    Portrait orientation, nine by sixteen, which overrides the landscape
    instruction in the style block. Compose for a tall frame: stack the
    subject vertically, keep it central, and let the top and bottom of the
    picture carry quiet dark areas rather than cropping a wide scene.

**depth-p.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A terracotta pot cut cleanly in half from the side so the full depth of
    soil is visible. The top band of soil is pale, crumbly and dry; everything
    below is dark, dense and visibly damp. A single finger enters from the side
    and reaches only into that pale top band, stopping well short of the wet
    soil. The composition is the comparison.

    Portrait orientation, nine by sixteen, which overrides the landscape
    instruction in the style block. Compose for a tall frame: stack the
    subject vertically, keep it central, and let the top and bottom of the
    picture carry quiet dark areas rather than cropping a wide scene.

**roots-p.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A close view inside dark damp potting soil filling the frame, with fine
    pale roots spreading through it, and a fresh channel running down the
    middle where something has been pushed in and withdrawn. The fine roots
    along its edges are torn, pale broken ends catching a warm side light.

    Portrait orientation, nine by sixteen, which overrides the landscape
    instruction in the style block. Compose for a tall frame: stack the
    subject vertically, keep it central, and let the top and bottom of the
    picture carry quiet dark areas rather than cropping a wide scene.

**scale-table-p.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A terracotta pot of a leafy plant standing on a small kitchen scale on a
    wooden table at night, with a phone lying flat on the table below it in the
    same frame. One warm lamp above. The pot and the phone stack vertically so
    both are clearly readable.

    Portrait orientation, nine by sixteen, which overrides the landscape
    instruction in the style block. Compose for a tall frame: stack the
    subject vertically, keep it central, and let the top and bottom of the
    picture carry quiet dark areas rather than cropping a wide scene.

**phone-closeup-p.png**, attach `../../../ThirstTrap/tools/hero-gen/refs/ref-observal-desk.png`:

    A phone lying flat on a wooden table at night, seen at a slight angle from
    above and filling much of the frame, with the base of a kitchen scale and a
    terracotta pot behind it. One warm lamp. The phone's screen is switched
    OFF: a flat, very dark, slightly reflective rectangle with nothing on it at
    all, no icons, no glow, no text.

    Portrait orientation, nine by sixteen, which overrides the landscape
    instruction in the style block. Compose for a tall frame: stack the
    subject vertically, keep it central, and let the top and bottom of the
    picture carry quiet dark areas rather than cropping a wide scene.
