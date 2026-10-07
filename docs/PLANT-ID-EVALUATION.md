# Plant identification: the evaluation, and what it settled

Run 2026-09-30, against Google's AIY `plants_V1` TFLite classifier, the model
D29 named as the candidate for F25c. Tooling and scripts in `tools/plantid/`;
nothing there ships.

## The short version

**Do not bundle this model.** It got **0 of 48** identifiable photos right, and
the reason is not that it is a bad classifier. It is a field-guide classifier
for wild plants, and a houseplant on a desk is not the problem it was built for.

**It is not reckless about it, which matters.** Of 55 photos it declined 38 by
answering "background", and named something at 70% confidence or more and was
wrong exactly **once**. So the failure mode is uselessness, not lying, and a
confidence threshold would not rescue it because there is nothing correct
underneath to threshold.

## What was measured

The test set is this project's own diary: 55 photos across four plants, with
ground truth from the plant each photo is attached to. Small, unbalanced and
exactly the right *distribution*, which is phone photos of pots on a desk rather
than herbarium plates. A set that size can disqualify a classifier and cannot
validate one, and disqualifying is what happened.

| Plant | photos | genus in label set | top-1 correct | what it said instead |
|---|---|---|---|---|
| Creeping fig | 18 | **yes** (`Ficus`) | 0/18 | "background", all 18 |
| Peperomia | 22 | no | 0/22 | "background" 12, *Griselinia littoralis* 5 |
| Fittonia | 8 | no | 0/8 | *Goodyera pubescens* 6 |
| Flax seeds | 7 | not a plant yet | n/a | "background", all 7 |

`Ficus` never appeared in the top 5 for the one plant that is a Ficus.

## Why, and why it generalises

**Coverage.** The label set is 2101 classes over 1028 genera, and its largest are
*Quercus* (28), *Pinus* (20), *Asclepias* (20), *Viola* (18), *Acer* (14),
*Castilleja*, *Lupinus*, *Trifolium*. That is a North American field guide. Of 59
common indoor genera checked, **40 are absent**, including Philodendron,
Sansevieria, Dracaena, Epipremnum, Spathiphyllum, Calathea, Zamioculcas,
Anthurium, Begonia and Peperomia. Two of this diary's own four plants cannot be
named by it at any accuracy.

**Domain.** Even for `Ficus`, which it has, it abstained 18 times out of 18. A
seedling in a terracotta pot on a wooden desk beside a kitchen scale is out of
distribution for a model trained on plants growing in the ground outdoors. Our
own photo habit makes this worse: the app encourages photographing the pot on the
scale, which is the least identifiable possible framing.

**The generalisation.** Open pretrained plant classifiers come from citizen
science, and citizen science photographs wild plants, because that is what people
go outside and label. Houseplant identification is a smaller, separate, much less
open domain. So this is not "pick a better model": it is "train one on a
houseplant dataset", which is a different order of work and a different project.

## What this leaves

**All three options below were decided on 2026-09-30, and the answer was the
third: identification is closed entirely.** F25c by D41 and F25b by D42. This
section was left reading as though the decision were still open, which is how it
came back up on 2026-10-07 and got recommended a second time. `README.md` and
`docs/FEATURES.md` carry the settled wording.

**F25c as specified is closed.** Bundling an open pretrained classifier does not
work, and the 4.8 MB it would add to a 4.3 MB app buys nothing.

Three honest options remain, and the third is not a joke:

1. **Train a houseplant classifier.** A real project: assemble or find a labelled
   indoor dataset, fine-tune a MobileNet, quantise. Weeks, not days, and the
   output would still be genus-level at best.
2. **F25b, online with the user's own key.** Pl@ntNet's *API* handles ornamental
   flora far better than its open models, and Kindwise exists. Little work, no
   size cost. Costs: a photo leaves the device, and the user must go and get a
   key, which almost nobody will.
3. **Close identification entirely**, the way F16 was closed. Every plant app
   has it; ours would be a worse version; and the app's stated character is that
   it refuses to guess. It already resolves names you *type* against GBIF, which
   covers the case where you half-know what you have. "It does not guess at
   plants either" is coherent and costs nothing to hold.

## A note on being wrong about this

The first read of these numbers, mine, was "confidently wrong on the plant it
had a class for", because Creeping fig showed a median top-1 confidence of 0.87.
That 0.87 was its confidence in **"background"**, meaning "there is no plant here
I can name", which is the opposite of overconfidence. Worth recording because the
corrected finding is the more interesting one: the model behaves honestly and is
still unusable, and those are separate properties.
