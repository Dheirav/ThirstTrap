# A moving hero, with or without a character

Written after watching the observal recording frame by frame and measuring it
against our plates and renders. The question was whether their approach needs a
character, and if so how we would build one.

## What their hero actually is

Seventeen seconds of continuous 3D camera movement through an illustrated
space, then a cut to a real product walkthrough with cursor moves and
handwritten callouts. The second half is the structure this page already has,
so the only part in question is the first half.

It is genuine 3D, not parallaxed layers: the laptop rotates in perspective
between six and eleven seconds, which flat planes cannot do.

Measured against ours, the look is the same family and we are not losing on it:

    5-bit colours   median V   clipping
    observal hero          844      0.49       0.01%
    our plate              653      0.27       0.00%
    our render             470      0.28       0.02%

So the difference is not the renderer, the palette or the flatness. It is four
other things, and only one of them is the character.

## The four differences

1. **A person is in it.** Hands on a keyboard, a figure at a desk, framings over
   a shoulder.
2. **An enormous scale range.** It opens on an aerial view of a whole circular
   room and ends on the surface of a laptop screen. Our entire flythrough happens
   within about one table's width.
3. **Something is in the near foreground almost constantly.** Plants, lamps,
   furniture edges, the back of a chair, passing through and occluding.
4. **The space is built for the move.** The floor is a circle, the walls curve;
   the geometry exists so those framings work. Our room is a plausible flat that
   shots were then hunted for inside, which is backwards.

## Can it be done without a character? Yes, and it suits this product better

List what the character does for them, then ask what else could do each job.

| What the person provides | What replaces it here |
|---|---|
| Scale, so you know how big the room is | Objects of known size: a mug, a phone, a pot. We have all three. |
| A subject for the camera to approach and orbit | The pot on the scale, the cut pot, the sheet. |
| Over-the-shoulder framings, which are inherently dynamic | Over-object framings, with the near object out of focus and cropped. |
| Human presence | The hand. We already have one and it has never been used. |
| A point of view for the story | The ledger. Seven rows in one handwriting is a person without a face. |

And there is a stronger reason than feasibility. The style block for the plates
already says **"Nobody is smiling at the camera"**, the plates contain no person
except a hand, and the product's anti-goals are specifically about not making
the user perform. A character would make the page about the person keeping the
plants. The app is about the pot.

What makes their hero work is not the person. It is continuous motion, a huge
scale range, and constant foreground occlusion. All three are available to us
with no character at all.

### What to build instead, in order

1. **Use the hand that exists.** `parts.build_hand` is a modelled right hand:
   index with three phalanges and joints, a bevelled palm, the other fingers
   curled to hide knuckles it does not model. The fingertip is at the origin so
   placing it is one move. Every shipping shot uses `build_finger` plus a cone
   instead, and the hand is called only by a test script. This is the cheapest
   large improvement available anywhere in the project.
2. **Widen the scale range.** The move currently travels about a metre. It
   should open wide enough to see the room and end close enough to read the
   number on the scale. That is a camera problem, not a modelling one.
3. **Put something in the near foreground of every framing.** The one note all
   five review lenses reached independently. A pot rim, a book edge, a leaf, the
   lamp shade: cropped, dark, out of focus.
4. **Design the set for the path.** Decide the move first, then build only what
   it passes. Everything we have was modelled as a room and then searched for
   shots.

## If we did want a character, here is how

Their figure is mostly **silhouette**. Seen from behind or in profile, the hair
is a single blob, the face is barely rendered or absent, and the body is flat
dark shapes. That is what makes it affordable, and it is the only version worth
attempting.

Three tiers, cheapest first:

**Tier 1, hands and forearms.** Days of work. Use `build_hand`, add a forearm
with a wrist that necks in before it widens, and keep the crop at the elbow.
This gets human presence into the frame with no face, no body and no rig. It is
also already half done.

**Tier 2, a seated silhouette from behind.** A week or two. A torso, a head, a
hair mass, two arms, all low-poly and read almost entirely as a dark shape
against the lamp. Rules that make it survive flat shading: never show the face,
never light it from the front, keep it below 20% of the frame, and let furniture
cross it. Rigging can be avoided by modelling it in one pose, since the camera
moves and the figure does not have to.

**Tier 3, a full articulated character.** Not worth it. Flat shading removes
every cue that makes a modelled human read, and a bad one is far worse than
none. If this is ever wanted, it is a job for a bought or CC0 character, and the
risk is that it brings a style the room does not share.

## Recommendation

No character. The hand is the character, it is already written, and using it is
one afternoon. Then widen the scale range and put something in the foreground of
every shot, which is what the review found and what the observal recording
confirms from a completely different direction.
