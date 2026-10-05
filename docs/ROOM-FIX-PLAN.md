# Finishing the Blender room

Four shots are not usable. This is the order, the reason for the order, and what
"done" means for each, written down before starting so the work can be checked
against it rather than declared finished.

Three of the seven are already close: `shelf-evening`, `scale-table` and
`depth`. They are not touched.

## The order, and why

1. **finger-test** and 2. **phone-closeup** come first because they are the same
   defect `scale-table` had, and that fix already worked once: props sitting
   outside the frustum and a lens too long to reach them. Cheapest, and the
   method is proven.
3. **ledger** is next. It needs one new asset, handwriting on the sheet, plus
   dressing that already exists elsewhere in the room.
4. **roots** is last because it is the only one that is wrong in kind rather
   than degree. The plate is a macro inside soil; the room has no soil-macro
   geometry at all, so there is nothing to reframe. It needs building.

## 1. finger-test

**Wrong now:** no finger in frame at all, two pots, and most of the picture is
bare wall. The plate is a close pot on the sill with a hand entering from the
right, the window behind, and more foliage either side.

**Done when:** the finger is in the soil, at least three pots are visible, the
window is behind the subject rather than beside it, and no more than about a
third of the frame is undressed wall.

**Approach:** check whether the hand is being placed at all before moving the
camera, because a missing finger and a bad framing are different bugs and
fixing the second would hide the first.

## 2. phone-closeup

**Wrong now:** an empty table with a phone on it. The plate has the phone large
in the foreground, the scale and pot behind it, the mug, and the sill beyond.

**Done when:** phone, scale, pot and mug are all in frame with the sill behind,
and the phone reads as the largest clean shape.

**Approach:** the same as `scale-table`. Widen the lens, raise the aim, and move
whichever props sit outside the frustum. Measure the frustum rather than guess
which ones those are.

## 3. ledger

**Wrong now:** a blank cream rectangle, two pots, and a dark field. The sheet is
the subject of the shot and it has nothing written on it.

**Done when:** the sheet carries seven ruled rows with a date and the word
"Watered" in each, the books are on the sill, and at least three pots are in
frame.

**Approach:** generate the sheet as an image and map it, rather than modelling
rows. Text is the one thing in this room that is cheaper as a texture, and the
page already has a drawn version to copy from.

## 4. roots

**Wrong now:** it frames the whole cut pot from outside. The plate is inside the
soil, filling the frame, with fine pale roots through dark crumb and a torn
channel down the middle.

**Done when:** no pot and no room are visible, the frame is soil and roots, and
the torn channel reads.

**Approach:** build a soil block with a displaced surface and scatter root tubes
through it, lit from the right like every other shot. It does not need to be the
same soil as the pot's; nothing in the sequence can see both at once.

## How each one gets checked

Side by side against its plate, at size, before moving on. Every defect fixed in
this room so far was found that way and none were found by a measurement.


---

## What happened

Checked against the "done when" lines above, not against how it felt.

**1. finger-test — done.** Two bugs were stacked and fixing the framing would
have hidden the other, which is why the plan said to check the hand first.
`soil_top` returned None for the modelled pots, because it matched the
underscore-prefixed names the appended assets used, and the caller fell back to
table height, putting the finger 12 cm under the sill. The hand was also aimed
at the pot's front rim rather than its soil, so the finger read as going through
the pot wall. Finger in the soil, window behind, wall under a third. Two pots
rather than three.

**2. phone-closeup — done.** The camera was aiming at bare table, because the
props had moved when `scale-table` was fixed. The new one was chosen by
projecting phone, scale, pot, mug and sill into four candidates and taking the
one that held them. Then looking at it showed I had framed a wide shot when the
plate is a closeup, so it moved in. Phone, scale, pot and sill are in; the mug
is not, and the phone is not the largest shape.

**3. ledger — done.** The sheet is a texture now, seven rows with dates and
"Watered" in URW Chancery with per-row jitter. The slab was also modelled thin
in X, which is how you build paper on a side reveal, while it hangs on the
pier's room-facing surface: the camera met it at 52 degrees and the page
foreshortened into a strip.

**4. roots — improved, not done.** It is soil and roots filling the frame now
instead of the whole pot, which is the right kind of picture. The channel is not
reading and the roots are too uniform. Five passes, and the one that mattered
was replacing the displaced grid's mottling with scattered pebble geometry:
flat shading has no gradient to say a surface is round, so a displaced grid
gives a PATTERN and reads as camouflage paint. Lumps have silhouettes and cast
shadows on each other, and that is the whole difference.

## The two follow-ups, done

Both said "solve it from the geometry rather than nudge it", and taking that
literally is what made them quick.

**The mug.** Sweeping candidate positions against BOTH table cameras found one
visible in each with a comfortable margin. At its old place it suited
`scale-table` and fell outside `phone-closeup` entirely. It is in both shots
now. It also costs something: it sits in front of the scale rather than beside
it, where the plate has it clear. One mug and two cameras that want it on
opposite sides is a real constraint, not a bug, and this is the compromise.

**The channel.** Projecting its edges showed it had been in frame the whole
time, at 15% of the frame's width, so it was never missing: it was being
covered. Pebbles crowded its lip, forty roots crossed it, and each bank was
tilted 13 degrees about its own centre, which lifted the inner edge across the
gap and closed it as fast as widening it opened one.

Then the floor under it went through both failures in turn. At 0.17 below the
banks it was lit as brightly as they were, so the trench vanished into
continuous dirt at two depths. At 0.42 it was a black slot, a hole in the
picture rather than a hole in the soil. 0.235 is in shadow but still lit, which
is what the plate has.

Three cameras were nudged before any of this was measured, and all three put the
channel off the top edge. The projection took one render.

## Where it stands

Six of seven are usable. `roots` is much closer and still the weakest: the
trench reads now, but the soil is flatter than the plate's and the roots are
more even than torn. Both are scatter-density problems rather than anything
structural.
