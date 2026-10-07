# Play Console developer account: what I put in the form

Drafted 2026-10-08. Edit before pasting; the numbers are true as of that date.

## "Tell us about your previous experience"

I have not used Play Console before. This would be my first developer account,
and the app below is the first thing I have built that is worth publishing.

I have been writing an Android app called ThirstTrap since September 2026. It is
a plant care diary whose point of difference is that it works out when to water
from the weight of the pot rather than from a calendar. You put the pot on a
kitchen scale every few days, the app fits the drying curve, and it tells you
when the plant will next be thirsty. A schedule cannot know your room, your soil
or how big the plant has got, which is why most watering reminders are wrong.

It is written in Kotlin with Jetpack Compose and Material 3, across four Gradle
modules: the app itself and separate data, domain and UI libraries. Storage is
Room, currently at fifteen schema versions, every one an automatic migration
with its schema exported, committed and covered by an instrumented test, because
a diary has to survive upgrades. Reminders run through WorkManager, dependency
injection is Hilt, settings live in DataStore, and navigation is Navigation
Compose. There are 324 JVM tests and 24 instrumented tests.

It is about 32,000 lines across 213 Kotlin files, with 219 commits. The source
is at https://github.com/Dheirav/ThirstTrap.

It is offline only, deliberately. Nothing leaves the phone: no account, no sync,
no server. The single network call in the app is an optional species name
lookup that is off by default and sends only the name you typed. Backups are a
zip file you export and keep yourself.

I have been running it daily on my own phone against real plants for the past
month, and most of the defects I have fixed were found that way rather than at
the desk.

Before that I built Luna, also Kotlin and Compose: an offline menstrual cycle
tracker, about 16,500 lines across 82 files. It takes the same position harder.
There is no INTERNET permission in the manifest at all, so the app physically
cannot send anything anywhere, and it refuses to state a number it has not
earned: it predicts a window that narrows as real cycles accumulate rather than
a single confident date, and it never conflates an observed day with an
estimated one. It is a personal project, not published, and not a medical
device.

Before Luna I wrote a native attendance tracker, which is where I learnt most of
this. It is for students keeping track of university attendance: you log each
class present, absent or late with a swipe, and it keeps a live percentage per
subject, works out how many classes you can still afford to miss, and forecasts
where you will end up if you carry on as you are. Jetpack Compose and Material 3
again, MVVM over a repository layer with Room, and StateFlow throughout.
https://github.com/Dheirav/Attendance_Tracker_Mobile

I have also built a private two-person app on Kotlin and Compose with Firebase
behind it, which is the only one of the four that syncs anything. I mention it
because the other three are deliberately offline and I would rather say plainly
that I have built against a backend than have it look like I only know how to
avoid one.

## "Have you used any other Google accounts to access Play Console?"

Read the question carefully: it asks whether you have **used another account to
access Play Console** in the last six months, not whether you own other Google
accounts. Owning a second address is not the trigger.

Answer **No** if this is your first Play Console account and nobody has ever
added you to theirs as a team member. Answer **Yes** if either of those happened
on a different address, including being added to a company or a friend's
account. Getting this wrong is the kind of thing that stalls a review later,
and there is no advantage to answering No.

## Website

You do not have one hosted. Two honest options:

- **Put the GitHub repository**: `https://github.com/Dheirav/ThirstTrap`. It is
  a real URL, it is not shown publicly, and for a brand new account it gives the
  reviewer something to look at: a month of commits with the reasoning written
  down. This is what I would do.
- **Tick "I don't have a website"**, which is allowed and costs nothing.

Do not put a placeholder or a URL you do not control.
