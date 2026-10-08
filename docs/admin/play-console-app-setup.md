# Creating the app in Play Console: what to put, and what will stop you

Drafted 2026-10-08 against the app as it stands: `dev.dheirav.thirsttrap`,
versionCode 1, versionName 0.1.0-M0, minSdk 26, targetSdk 35.

**Updated later the same day, twice.** The version scheme changed: versionCode is
the git commit count now and versionName is 0.2.0. And two things were found
that this draft did not know about, one of which cannot be undone and one of
which blocks the upload outright. Both are below, before the form, because they
matter more than the form does.

## Do not let Google generate the app signing key

This cannot be changed afterwards, and getting it wrong costs your testers their
photos.

Play App Signing means Google holds the key that signs what people actually
download. Android only accepts an update signed with the same certificate as the
installed app, and there is no override. So if Google generates a fresh key, the
Play build carries a different certificate from every APK sideloaded from
`thirsttrap-release.jks`, Android refuses the update, and the only route across
is to uninstall first, which wipes the install. A backup does carry the photos,
so it is survivable, but it means every tester has to export one first and
actually do it.

At app creation there is an option to provide your own app signing key instead.
Take it, and upload `~/thirsttrap-release.jks`. The Play build then carries the
same certificate as everything already handed out, and the store version lands
as an ordinary update. Google's own advice is to let them generate it, which is
better key hygiene in the abstract and wrong here, because it breaks continuity
with builds already on people's phones.

The option exists at creation time only. Certificate to expect, verified with
`apksigner` on the 2026-10-01 and 2026-10-08 builds, which match:
`60427626df3b4f5ac61e893b7f96c89845eec971743d297c2abf2fd02f598c1b`,
`CN=Dheirav Prakash`.

## targetSdk 35 will be rejected

Play requires new apps to target API 36, Android 16, and that came into force on
2026-08-31, which has passed. This app is `compileSdk 35` and `targetSdk 35`, so
an upload fails before any of the rest of this document matters.

It is not a one-line bump. SDK platform 36 is not installed, and AGP is 8.7.3
which is unlikely to accept `compileSdk 36`, so this probably pulls AGP forward,
and KSP is pinned to the Kotlin version (`2.0.21-1.0.28`) with Hilt at 2.52, so
the upgrade can cascade. Do it as its own piece of work with the tests as the
check, not as a step while filling in a form.

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
