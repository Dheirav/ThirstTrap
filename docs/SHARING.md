# Sharing ThirstTrap with other people

Written 2026-09-30. Decisions taken in this document: distribution goes GitHub
first and Play later, and both a landing page and an in-app walkthrough get
built. The rest is a plan, not a record.

Where the app is: 96 of 101 features, schema v13, 214 JVM tests and 14
instrumented ones passing, running daily on one phone against four real plants.
Nobody but its author has ever opened it.

---

## 1. What actually blocks handing it to someone

Three things, in order of how hard they bite.

**The release build is unsigned.** `app/build.gradle.kts` has a `release` block
with minify and resource shrinking on, and no `signingConfig`. An unsigned APK
will not install. This is the whole of stage one: generate a keystore, keep it
out of the repo and backed up somewhere that is not this laptop, and wire a
`signingConfig` that reads its password from `local.properties` or an
environment variable. Losing that keystore means never being able to ship an
update that upgrades an existing install, so it matters more than it looks.

**The version says `0.1.0-M0`.** That is an internal milestone tag. Anyone who
sees it reads "not finished", and it also gives you nothing to talk about when
someone says which version they are on. `versionCode` is still 1 and has to
increment on every build you hand out, or Android will refuse the upgrade.

**The permission list reads worse than the app behaves.** The manifest asks for
CAMERA, INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS and VIBRATE. A
privacy-minded person checking that list sees INTERNET on an app whose Settings
screen promises that nothing leaves the phone. The promise is true, because the
only network path is the opt-in name lookup, off by default. But the burden of
explaining that is on you, and it belongs in the listing and the landing page
rather than being discovered and mistrusted.

Nothing else is a blocker. Debug tools are already gated behind
`BuildConfig.DEBUG`, so they disappear from a release build without any work.

---

## 2. Big pots: the gap, and why it is smaller than it looks

The premise is that you put the pot on a kitchen scale, which rules out exactly
the plants people care most about: the floor-standing Monstera, the ficus, the
big repotted thing in the corner. Too heavy to lift, too wide for the platform,
over the 5 kg capacity.

**The model does not need the pot's weight. It needs a number proportional to
it.** Depletion is `(wet - now) / (wet - dry)` and the ETA is remaining over
slope. Multiply every reading by the same constant and both come out identical,
because the constant cancels in a ratio. So any measurement that is a consistent
fraction of the true weight carries the same information.

That is now pinned by `PartialWeightInvarianceTest`, which runs the real
assembly at 1.0, 0.6, 0.38, 0.25 and 0.1 of the pot and gets the same predicted
day to within a microsecond. It is not an argument, it is a test.

Which makes three methods available:

- **Tip it onto one edge.** Put the scale under one side, leave the other on the
  floor, read whatever it says. Roughly a third of the pot's weight, and the
  exact fraction does not matter as long as you tip it the same way each time.
  Nothing is lifted. This is the good answer for a heavy pot.
- **A bathroom scale.** 150 kg capacity, a platform wide enough for most pots.
  The catch is resolution: typically 100 g steps. A 20 kg pot holding 2 kg of
  available water crosses its trigger over 1 kg, which is ten steps, and ten
  steps is enough for a slope.
- **A luggage scale and a sling** for anything hanging, which is the one case
  where the whole weight is easy to get and a platform scale is useless.

**The one real risk, and the change it implies.** Consistency is everything: the
anchors describe a measurement, so changing how you measure invalidates them
exactly as a repot does. Today the app has no idea how a reading was taken, so
it cannot warn you, and it cannot adjust.

It also has one constant that assumes a kitchen scale.
`Anchors.minMeaningfulSlope` is `max(1 g, 0.5% of the wet-dry range)`. The
relative half scales correctly. The 1 g floor is a statement about the
instrument: a kitchen scale reads to about a gram, so a sub-gram daily slope
cannot be told from its own noise. For a bathroom scale in 100 g steps that
floor is roughly a hundred times too low, which means the app would happily fit
a drying curve to what is actually rounding.

So the fix is not a new maths problem, it is one field:

1. Record **how this pot is weighed** per plant: whole pot, tipped on one edge,
   hanging, with the instrument's step size.
2. Derive the noise floor from that step size instead of the 1 g constant, so a
   coarse instrument raises the bar rather than sneaking under it.
3. Treat a change of method like a repot: say so at the moment it changes,
   because the anchors no longer describe the same measurement.
4. Say the tipping trick out loud in the weighing help, because nobody will
   invent it themselves, and it is the difference between the feature working on
   half someone's plants and all of them.

That is a day's work and it roughly doubles the number of plants the
differentiator applies to. I would do it before sharing, not after.

---

## 3. What a regular user does not need

The instinct is to cut features. I think that is wrong. Almost nothing here is
useless, but **28 routes reach the user through three tabs and a Settings screen
carrying eight feature links**, which is the actual problem. Places, Room
conditions, Figures, Experiments, Feeding, Diagnose and two help links all live
under Settings. Nobody looks in Settings for a feature.

**Hide outright:**

- **Debug tools.** Already gated on `BuildConfig.DEBUG`. No work.
- **Pot stickers and the QR scanner.** This is a genuinely good idea for someone
  with thirty plants and a printer, and noise for someone with four. Keep the
  code, put it behind a single "Advanced" toggle rather than on the plant menu
  where it currently competes with Timelapse and Care notes.
- **Experiments.** The most specialised thing in the app. It assumes you want to
  run a controlled test on a houseplant, which is a real thing a small number of
  people want. Same Advanced toggle.
- **Room conditions.** Asks the user to log temperature and humidity by hand.
  Its payoff is one explanatory sentence on the weight screen. Worth keeping for
  whoever wants it and not worth showing to everyone.

**Keep but move out of Settings:**

- **Feeding**, which is a thing you do to plants, not a preference.
- **Figures**, which belongs near the plants it counts.
- **Places**, same.
- **Diagnose** and **Reminders not arriving?**, which are help and should live in
  one Help screen rather than two loose links between the reminder hour and the
  room section.

**Leave exactly as is:** the plants list, Due, the quick-log sheet, plant detail
with its timeline and photos, weighing and the weighing round, timelapse,
propagation, care notes, backup. That is the app.

---

## 4. UI

Four things, roughly in order of what a new person would trip on.

**Navigation is the big one.** Three tabs and a junk drawer. The honest shape is
either a fourth tab or an overflow that is organised: things you do to plants,
things about the house, help, settings. A new user currently has no way to
discover that Feeding or Figures exist.

**First launch shows "No plants yet. Add the first one and start logging."** It
is accurate and it teaches nothing. This is the single highest-leverage screen in
the app, because it is the only one every user sees before they understand
anything, and right now it spends that moment on a sentence.

**The weighing feature is invisible until you go looking.** It is behind a plant's
overflow menu, under "Weight and prediction", which is the name of a screen
rather than an invitation. The thing that makes this app different from every
other plant app is three taps deep and named after its implementation.

**Synthetic taps kept missing the plant-row menu**, because a plain tap opens a
menu whose only item deletes, and the menu renders above the row it belongs to.
That was already recorded as a mis-delete risk. It is also just confusing.

---

## 5. The tutorial, both halves

They are different jobs and need different content. The page persuades someone
to install it. The walkthrough teaches someone who already has.

**In-app walkthrough.** Not a carousel of screenshots, which everybody skips.
Three cards on first launch, each answerable in one sentence:

1. This is a diary, not a schedule. Nothing here counts streaks or tells you off.
2. Weigh a pot now and then and it will tell you when it actually needs water,
   because a pot loses weight as it dries. A kitchen scale is the whole
   apparatus, and a heavy pot can be tipped onto one edge instead.
3. Everything stays on this phone. One button exports the lot.

Then contextual hints rather than a tour: the first time a plant has one weight
reading, the weight screen already says "weigh it again in a day or two and the
curve starts here", and that pattern is the right one. It teaches at the moment
of use and costs nothing to skip. The app is already good at this in places and
does not do it at all on the plants list.

**Landing page.** The Observal launch page's shape fits this well: scroll drives
a guided demo, a scripted cursor walks the UI chapter by chapter. The chapters
are obvious once you have the app: log a watering in one tap, weigh a pot and
watch the curve appear, get a reminder that names a measured day rather than a
calendar interval, and the refusals, where the app says it does not know instead
of guessing. That last chapter is the one that would actually sell it to the kind
of person who distrusts plant apps, and no competitor can copy it because their
whole model is a schedule.

There is a `scroll-story` skill for exactly this page shape, which is what I
would use rather than building the scroll machinery from scratch.

---

## 6. Order of work

**Stage one, GitHub and a signed APK.** Everything below is done except the
last two, which need the user rather than the code.

1. ~~Weighing method per plant, and the noise floor derived from it.~~ Done,
   D36. Also proved the model is invariant under a proportional measurement, so
   a heavy pot can be tipped rather than lifted.
2. ~~Navigation: get the seven features out of Settings.~~ Done, D37. Three tabs
   kept; the Plants overflow is ordered jobs-then-record. Places absorbed room
   conditions and the three help doors became one.
3. ~~First-run page.~~ Done, D37. One page, three points, one button, and
   re-readable from Help because it carries the tipping trick.
4. ~~Make weighing discoverable.~~ Done, D39. "Weigh it" is in the quick-log
   sheet, one tap from the list, and the menu entry is "When it needs water"
   rather than "Weight and prediction".
5. ~~Advanced toggle for stickers, QR, experiments and room conditions.~~ Done,
   D39. Off by default.
6. ~~Keystore, `signingConfig`, and a real `versionName`.~~ Done. The keystore
   lives at `~/thirsttrap-release.jks` with its four properties in
   `local.properties`, and the backup is off the machine. `versionCode` is the
   git commit count rather than a typed integer, because the typed one stayed at
   1 through two different signed releases, so neither could update the other.
   `versionName` is 0.2.0 and Settings shows the code beside it, which is what a
   tester quotes back.
7. **Tag a release and attach the APK.** Install notes are written, at
   `docs/INSTALL.md`, covering the unknown-sources dialog, the Play Protect
   warning, MIUI's extra scan, the version line to quote in a bug report, and
   why uninstalling loses the photos when a backup does not. Still to do is the
   tag itself with the APK attached, and Obtainium for anyone who wants updates
   without a store.

Found along the way rather than planned, each written up in the decisions log:
`ImportIdempotenceTest` had not compiled since the F11/F12 commit so the whole
instrumented suite was unrunnable; `Routes.AMBIENT` became unreachable when the
Settings links were stripped; `knownLocations` ignored places that only had a
room reading; the first-run page flashed on every launch because the settings
flow started from defaults; and "Watered today" meant "within 24 hours" rather
than today, which was wrong for most of every day on the most-read line in the
app.

**Stage two, Play:** a privacy policy page, a store listing with screenshots, a
content rating questionnaire, and the versioning discipline that comes with an
install base you cannot ask to reinstall. Worth doing only once stage one has
found the things that only real users find, which it will.

The landing page can be built at any point and does not block either stage.

---

## 7. What I would not do

**Do not add accounts or cloud sync to make sharing easier.** F16 is undecided
for good reasons and none of them changed. Every other feature was built on
"nothing leaves this phone", and the app says so in those words.

**Do not cut the refusals to look more confident.** The app declining to predict,
naming which of three reasons it has, and refusing a fertiliser dose too small to
pour, is the most distinctive thing about it. It will read as weakness in a
screenshot and as trustworthiness in use, and the second one is what keeps people.
