# Play Console listing: everything to copy in

Written 2026-10-09 against 0.2.1, `versionCode` 236, `targetSdk` 36. Everything
here is ready to paste except the two image jobs, which are noted at the end.

## A number worth getting right

The catalogue is **164 species**, not 115. 49 hand-written entries in
`SpeciesCatalogue.kt` and 115 generated ones in `SpeciesCatalogueBundled.kt`,
counted by name with no overlap between the two files. An earlier draft of this
document said 115, which is only the generated half.

## Short description

Play allows 80 characters and counts them strictly.

    A plant diary that weighs the pot, so it knows when watering is actually due.

That is 76. The reason it leads with weighing rather than with "plant care" is
that weighing is the only thing here no other plant app does, and the listing has
about four seconds to say so.

Two alternatives if that reads too blunt:

    Weigh the pot, not the calendar. A plant diary that learns each pot's thirst.

    A plant diary for people who would rather measure than guess.

## Full description

Play allows 4000 characters. This is about 1700, deliberately, because a listing
nobody finishes is longer than it needed to be.

---

ThirstTrap is a plant diary built around one idea: a pot tells you when it needs
water, and a calendar does not.

A pot loses weight as it dries, steadily enough to fit a line to. Put one on a
kitchen scale now and then, and the app works out how fast that particular pot
loses water in your room, with your soil, at the size your plant has grown to.
Then it tells you the day it will actually be thirsty. A calendar cannot do that,
because a calendar knows none of those things.

Too heavy to lift? Tip it onto one edge with the scale under that side. Only the
change matters, so tip it the same way each time and the number works just as
well.

WHAT IT IS NOT

No streaks. No counts of what you missed. No tone of voice about it. A reminder
asks you to look at a plant; it never tells you off, and "still wet, left it
alone" is a real answer that gets logged as one. Watering is not a score to run
up, and a plant you checked and sensibly left dry is a plant you looked after.

IT STAYS ON YOUR PHONE

No account. No server. No analytics. Your plants, your diary, your weights and
your photos never leave the device. One feature can look a plant name up online
to fill in care notes, and it is off until you switch it on, sends the name and
nothing else, and never runs on its own. Everything else, including the
predictions and the reminders, works with no network at all.

Backup writes one file you keep, wherever you keep things.

WHAT ELSE IS IN IT

- A diary per plant: waterings with amounts, checks, feeds, repottings, notes
- Photos kept with the plant and the date, so you can compare a leaf against
  itself six weeks ago, or play them in order
- Care notes for 164 species, bundled, offline, with 49 written by hand
- Two plants in one pot, handled properly: water the pot and both get the entry
- Fertiliser dilution worked out for the can you are actually holding
- Light readings per room, using the phone's sensor
- A propagation board for cuttings, from the day you take one to the day it is
  a plant
- QR stickers for pots, printable as one sheet, so scanning a pot opens it
- Reminders that are deliberately loose to the quarter hour, because exact
  timing costs battery and nobody needs a plant notification to the second

Free, no advertising, no purchases.

---

## Data safety form

Answer it as a transmission rather than as "no data collected". Everything below
is checkable in the source.

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** | The optional lookup sends a species name to two third parties |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | Both endpoints are HTTPS |
| Do you provide a way for users to request that their data is deleted? | **No**, and explain: nothing is held remotely to delete | Uninstalling removes everything, as there is no server |

Data type to declare: under **App info**, or **Other** if the category list has
no better fit, the species name the user types.

- Collected: no. Shared: **yes**
- Purpose: **App functionality**
- Is it optional? **Yes**, users can choose whether to send it
- Processed ephemerally: yes, it is sent to look something up and not stored
- Linked to identity: **no**. No account, no identifier of any kind is sent

Do **not** declare photos, diary entries, weights or location as collected or
shared. They never leave the device, and Play's definition of collection is
"transmitted off the device".

The camera permission needs no data declaration, because the photographs it takes
are written to the app's private storage and never transmitted.

## Content rating questionnaire

Category: **Utility, productivity, communication or other**. Then the honest
answers are no to everything: no violence, no sexual content, no profanity, no
drugs, no gambling, no user-to-user communication, no sharing of location, no
purchases. Expect "Rated for 3+" or the local equivalent.

## Store settings

| Field | Value |
|---|---|
| App category | Lifestyle, or House & Home. Lifestyle is the closer fit |
| Tags | Plants, gardening, journal, home |
| Contact email | dheirav2005@gmail.com |
| Website | optional, and only once there is one |
| Privacy policy | the hosted URL of `docs/privacy.html` |

## The two image jobs, which are not done

**Phone screenshots, at least two, up to eight.** 16:9 or 9:16, minimum 320px on
the short side. The screenshots taken on the device on 2026-10-08 are the right
content: the plant list, a weight screen with its drying curve, and Figures. They
are raw captures with a status bar and a debug clock, so they want cropping at
least, and most listings frame them with a caption.

**Feature graphic, 1024x500, required.** Nothing in the repo is this shape. The
Blender hero renders are the obvious source material, since a photograph of a pot
on a kitchen scale is exactly the one idea the listing is selling.

Neither is hard, and neither can be done by writing text, which is why they are
at the bottom.
