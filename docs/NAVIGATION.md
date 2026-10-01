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

## 6. What this did not fix

Both items here are now done, and they are left in rather than deleted because
what they say about this document is useful: the two things it set aside as "not
navigation" turned out to be the two the user noticed first.

**Weighing was named after its implementation.** "Weight and prediction" in a
plant's overflow menu was a screen title, not an invitation, for the feature the
whole app exists for. It read "When it needs water" for a day after that, which
promised a date the screen frequently declines to give: there are seven ways it
can refuse. It is **"How thirsty it is"** now, which is true in every state and
matches the voice of "How dry before watering" elsewhere in the app.

**A plain tap on a diary row opened a menu whose only item deleted.** Recorded
twice as a mis-delete risk before this document, and it cost a third real entry
afterwards. A tap opens the entry now, D48.

## 7. What it actually became

Added 2026-10-01, after building it. Sections 1 to 6 are the plan as written
before any code; this is the shape that exists, and the two differ in ways worth
recording.

**28 routes, down from 28.** No net change, but not the same 28. `compare` and
`timelapse` are gone, folded into the photo viewer (section 3's merges, carried
further than planned), and `intro` and `help` were added. The viewer itself has
no route at all: it is an overlay in the activity's own window, because a Dialog
gets clipped inside the system bars and a screen would need a back-stack entry
for something that is really a mode of the page you are already on.

**The three tabs landed as planned.** Plants, Due, Settings. The seven features
behind the gear icon came out to the dashboard's overflow: Feeding, Propagation
board, Scan a pot sticker, Places, Figures, Experiments.

**What the plan did not anticipate is that gestures needed deciding too.** The
tab shape was the easy half. The harder half was that this app had accumulated
four different meanings for a tap, and two of them opened destructive menus.
The rule now, applied everywhere:

> A tap opens the thing you tapped. A long press is a shortcut, and only ever
> to something that is also reachable by tapping.

That resolved every case bar two, and those two are deliberate:

| Where | Tap | Long press |
|---|---|---|
| A plant row | opens the plant | the quick-log sheet |
| The watering droplet on a row | logs the usual amount | amount and method |
| A photo | opens it full screen | nothing |
| A diary entry | opens the entry | nothing |
| A filmstrip thumbnail | picks it | nothing |

The two long presses survive because nothing is *only* behind them. The sheet's
contents are all on the plant page the tap now opens, which is why "Log
something" had to be added there first; the droplet's long press duplicates the
log screen. That is the test: a long press may be a shortcut, never a sole
route.

**The two long presses are the only things in the app a new user cannot
discover by trying**, which is why they are written down in the "Finding your
way around" help page rather than left to be found.

## 8. Help was the same mistake again

Added 2026-10-01, within the hour, because the user spotted it immediately.

Section 1 of this document says: nobody looks in Settings for a feature, so for
a new user the seven features behind the gear icon do not exist. Seven were
moved out. Then Help was put in, and one of Help's four entries was a feature.

"A plant does not look right" is the diagnosis tree. Somebody reaches for it at
the moment they are worried about a plant, and it was four taps deep behind
Settings, then behind a word meaning "I am confused", then in a list next to
three pieces of documentation. It is now **"Something looks wrong"**, second in
the plant's own menu under "Log something", because it is about that plant and
that is where you are when you want it.

Two things fell out of the move. The route is plant-scoped now, so the tree's
"Log what you found" button at every leaf finally has somewhere to go: it had
been written, shipped and never once rendered, because nothing passed
`onLogEvent` and nothing could while the only route in came from a screen that
does not know which plant you mean. And Help now has a rule it can be held to:

> If an entry does something rather than explaining something, it does not
> belong in Help.

Which leaves Help as four pieces of documentation, and the open question the
user raised alongside this one: whether documentation behind Settings is itself
too hard to find. Not decided.

