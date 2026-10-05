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
