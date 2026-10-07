# Creating the app in Play Console: what to put, and what will stop you

Drafted 2026-10-08 against the app as it stands: `dev.dheirav.thirsttrap`,
versionCode 1, versionName 0.1.0-M0, minSdk 26, targetSdk 35.

## The create-app form

| Field | What to put | Why |
|---|---|---|
| App name | `ThirstTrap` | 50 characters, shown publicly. See the note on the name below |
| Default language | English (United Kingdom) | Every string in the app is British: fertiliser, colour, normalise. Picking en-US would make the listing disagree with the app |
| App or game | App | |
| Free or paid | **Free** | And understand that this one is one-way: a paid app can be made free later, a free app can never be made paid |
| Declarations | Both boxes | Developer Program Policies, and US export laws |

Nothing here is final except Free. The name and the listing can be edited.

## Three things that will stop you, in the order you will hit them

**1. Twelve testers for fourteen days.** A personal developer account opened now
cannot publish to production until it has run a closed test with at least twelve
testers who stayed opted in for fourteen continuous days. This is the big one.
It is not a queue you wait in, it is twelve real Google accounts you have to
find and keep enrolled for two weeks, so it is worth starting before the app is
finished rather than after.

**2. A privacy policy at a public URL.** Required, and you have no website. The
cheapest honest option is a `PRIVACY.md` in the ThirstTrap repository served
through GitHub Pages, which gives you a real URL on a domain you control for
nothing. The policy itself is short because the app barely collects anything,
and that is worth saying plainly rather than pasting a generic template that
claims to share data with partners you do not have.

**3. The data safety form, answered carefully.** It is tempting to tick "no data
collected" and move on. Mostly true here, but the manifest declares INTERNET and
ACCESS_NETWORK_STATE for the optional species lookup, which sends a species name
to an external service when the user turns it on. That is a data transmission
and it has to be declared even though it is off by default and carries no
identifier. CAMERA is for photographs that never leave the device. Declaring
honestly costs nothing; being caught understating it costs the account.

## The rest of the listing, once those are cleared

- Short description, 80 characters
- Full description, 4000
- App icon 512x512, feature graphic 1024x500, at least two phone screenshots
- Content rating questionnaire
- Target audience and content
- App access: nothing is behind a login, so say so
- Ads: none
- Signing: let Play manage the app signing key, and keep your upload key safe.
  Losing the upload key is recoverable; losing a self-managed app signing key
  is not

Bump the version before any upload. `versionCode 1` and `0.1.0-M0` are fine for
a sideload and read as unfinished on a store page.

## The name

`ThirstTrap` is a pun and it works, but "thirst trap" means a deliberately
provocative photo, and that is the first meaning most people and every automated
policy filter will reach for. It is probably fine, since the app is obviously
about houseplants the moment anyone looks. Two cheap things that make it
obviously fine:

- Make the first line of the store listing name a plant, not a mood. The short
  description is the thing a filter and a human both read first.
- Make the icon and the feature graphic unmistakably a pot and a scale.

Not a reason to rename it. A reason not to let the listing be coy.
