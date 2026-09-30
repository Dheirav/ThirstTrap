# Navigation: the flows, then the shape

Written 2026-09-30, before implementing anything. The point of writing it first
is that information architecture is cheap to decide and expensive to undo.

## 1. What is actually there

28 routes reach the user through three tabs. Sorted by what they are, not by
where they currently live:

| Kind | Routes |
|---|---|
| Top level | Plants, Due, Settings |
| About one plant | detail, edit, log, weight, compare, timelapse, care notes, light, sticker, post-mortem |
| A job across many plants | weighing round, feeding, propagation, scan a sticker |
| About the house | places, place light, room conditions |
| Looking back | figures, experiments, one experiment |
| Help | reminders not arriving, scale help, diagnose |
| Data | backup, debug |

**The per-plant group is already right.** You reach those from a plant, which is
where you are when you want them. Nothing in this document changes that.

**Everything else is wrong in the same way.** Feeding, Places, Room conditions,
Figures, Experiments, Diagnose and the reminders help are all reached from
Settings. Seven features behind a gear icon. Nobody looks in Settings for a
feature, so for a new user those seven do not exist.

## 2. The flows, by how often they happen

This is the ordering that matters, because frequency should decide depth.

**Every day, or nearly.** Open the app, see what wants attention, log a watering
or a check. Due and Plants, with the quick-log sheet. Two taps today, and this
works.

**Every week or two.** The scale comes out and every pot goes on it. One screen,
the weighing round, reached from an icon on Plants. The flow is right; the icon
is a weight symbol nobody will recognise cold.

**Every few weeks.** Mixing feed and needing the dose for the can in your hand.
This is a watering job and it is currently four taps into Settings.

**Every few days, but only if you are propagating.** The propagation board. A
view of plants, so it belongs near Plants.

**Monthly at most.** Figures, Places, Room conditions, Experiments. Real value,
rare use, and no reason to be adjacent to the daily loop.

**When something is wrong.** Diagnose, reminders help, scale help. Three separate
entry points for one intention.

**Rarely and deliberately.** Backup, storage, preferences. This is Settings.

## 3. Two merges that remove screens rather than moving them

**Places absorbs Room conditions.** Both answer questions about a location:
Places holds the note and the last light reading, Room conditions holds
temperature and humidity by location. They are the same subject split across two
screens, and the split is why neither is obviously useful. One Places screen
where a place shows its light, its recent temperature and humidity, and what
lives there, is a screen worth opening.

**One Help screen.** Diagnose, "Reminders not arriving?" and the scale help are
three doors to "something is not working". They become one Help entry with three
sections. Settings currently has two of them loose between the reminder hour and
the room section, which is how you get a settings screen nobody can scan.

Those two merges take seven orphans down to four: Feeding, Places, Figures,
Experiments.

## 4. Where the four go

The per-plant group proves the principle the app already follows: a thing lives
where you are when you want it. So the question is only what these four are
about, and the answer is the collection rather than any one plant.

**Decided 2026-09-30: three tabs, a richer Plants bar.** Written up below as
proposed and then decided against, because the reasoning for the alternative is
worth keeping. The mitigation for the discoverability objection is that the
overflow is *ordered and divided* rather than a flat list of seven, and the
first-run walkthrough points at it once.

**Considered: a fourth tab.** Plants, Due, Almanac, Settings. The Almanac tab
carries Feeding, Places, Figures and Experiments, each a section rather than a
loose link. Settings goes back to being settings, plus data and one Help entry.

Reasons for a tab over an overflow menu on Plants:

- The problem being solved is discoverability, and a tab is discoverable in a way
  an overflow is not. Moving seven items from one hidden menu to another hidden
  menu is not progress.
- The Plants top bar already carries three icons and a floating button. It is the
  screen the app opens on and the one that should stay quiet.
- The app is an almanac by design, with mastheads, rules and column heads. A
  named section is the idiom it is already written in.

The cost is a tab for things used monthly, and a word a new user has to learn
once. The walkthrough can spend one line on it.

**Chosen: three tabs, richer Plants bar.** Weighing and Feeding as icons on
Plants, because those are the two recurring jobs. Everything else in one
overflow on the same screen, ordered so the jobs come first and the record
second, with a rule between them. Scan moves off the bar into the overflow: it
is rarer than either job and the bar should not carry four icons beside a title.

## 5. The resulting shape

```
Plants     list, add
           bar icons:  weighing round, feeding
           overflow:   propagation board, scan a sticker
                       ----
                       places, figures, experiments
           a plant:    detail, log, weight, photos, timelapse, care, light, sticker

Due        what wants attention, and logging it

Settings   Appearance, Network, Reminders, Watering default
           Your data: backup, storage
           Help: diagnose, reminders not arriving, using a scale
           Debug tools, debug builds only
```

## 6. What this does not fix

**Weighing is still named after its implementation.** "Weight and prediction" in
a plant's overflow menu is a screen title, not an invitation, and it is the
feature the whole app exists for. That is a separate item on the sharing plan and
it needs a name, not a new home.

**A plain tap on a diary row opens a menu whose only item deletes.** Recorded
twice already as a mis-delete risk. Unrelated to tabs, still worth fixing.
