# ThirstTrap privacy policy

Last updated 9 October 2026.

ThirstTrap is a plant-care diary for Android, written and published by Dheirav
Prakash as an individual rather than a company. This page says what the app does
with your information. It is short because the app does very little with it.

## The short version

There is no account, no server of mine, and no analytics. Your plants, your
diary entries, your weights, your reminders and your photos stay on your phone.
I cannot see any of it, and neither can anyone else, because none of it is sent
anywhere.

One feature is an exception and it is off until you turn it on. That is the whole
policy. The rest of this page is the detail behind it.

## What stays on your phone

Everything you put into the app:

- The plants you add, with their names, species, pot sizes, locations and notes
- Every diary entry: waterings, checks, feeds, repottings, observations
- Every weight reading and everything derived from it, including the predictions
- Your reminder times
- Your photos

All of it lives in the app's own private storage on the device. Photos in
particular are kept inside the app rather than in your gallery, which is
deliberate: a plant diary should not scatter dozens of near-identical pot photos
through the camera roll you share with other people.

I have no copy of any of it. There is nothing for me to have a copy of.

## The one thing that leaves the phone

The app can look up a plant you have named, to fill in care notes it does not
already hold. **This is switched off by default.** You turn it on in Settings,
under Network, and you can turn it off again at any time. Nothing happens
automatically: a lookup only runs when you ask for one on a specific plant.

When you do ask, two services are contacted, in this order:

1. **GBIF**, the Global Biodiversity Information Facility, at `api.gbif.org`, to
   turn the name you typed into a recognised plant species. The name you typed is
   sent, along with a note that it should be matched against plants rather than
   animals or fungi.
2. **Wikipedia**, at `en.wikipedia.org`, to fetch a short summary for whichever
   species GBIF matched. The matched scientific name is sent.

What is sent is the plant name and nothing else. No identifier, no device
information, nothing from your diary, no photos, no weights, no location. The app
identifies itself to both services as "ThirstTrap/1.0 (offline plant diary)".

As with any request your phone makes to any website, those two services can see
the IP address the request came from, which usually indicates roughly where in
the world you are. That is a property of how the internet works rather than
something the app chooses to send, but you should know it happens. Each service
handles what it receives under its own privacy policy, and neither of them is
mine.

Everything else in the app, the predictions, the reminders, the bundled plant
catalogue, your whole diary, works with no network connection at all and always
will.

## What the permissions are for

| Permission | Why |
|---|---|
| Camera | Taking a photo of a plant. The photo is saved in the app and nowhere else |
| Internet | Only the species lookup described above, and only when you ask for one |
| Network state | Checking whether you are online before attempting a lookup, so it can say "offline" instead of hanging |
| Notifications | Showing your reminders. Composed and shown on the device |
| Vibrate | A short buzz when you log something, so you know it registered |

## Analytics, advertising and tracking

None. There is no analytics library in the app, no crash reporting, no
advertising, and no third-party tracking of any kind. Nothing measures how you
use it, because nothing in the app sends anything about how you use it.

## Backups, and what they mean for your privacy

You can export everything to a single zip file from Settings. That file is yours,
it goes wherever you tell your phone to put it, and the app does not upload it or
keep a second copy. Once it leaves the app it is as private as the place you put
it, so if you store it in a cloud drive, that drive's privacy terms apply to it
and not this policy.

Worth knowing for a different reason: photos are not recoverable without a
backup. They live in the app's private storage, so uninstalling the app deletes
them permanently and no backup you took earlier will bring back ones taken since.

## Children

The app is not directed at children and collects nothing from anybody, so there
is nothing specific to say here beyond that.

## Deleting your data

Uninstall the app. Because nothing is stored anywhere else, uninstalling removes
all of it, and there is no request to make of me and no account to close. If you
want to keep any of it first, export a backup before you uninstall.

## Changes to this policy

If the app ever does something new with your information, this page changes first
and the date at the top changes with it. If a change ever meant information
leaving your phone that does not leave it today, it would be off by default and
ask you, the same way the species lookup does.

## Contact

Questions about this policy, or about the app: dheirav2005@gmail.com
