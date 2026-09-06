# ThirstTrap — Devices & adb

Hard-won constraints of this WSL2 box and the test phones. Most of this was
paid for in the **Luna** project (`~/Code/Luna/docs/HANDOVER.md`, "Device —
read this before debugging anything"). It is restated here because a ThirstTrap
session will not have Luna's handover in context, and re-learning it costs
hours.

---

## 1. USB works — through the *Windows* adb, not WSL's

**Wired debugging works.** Verified 2026-09-06 with the Note 15 Pro attached
over USB and seen by the Windows adb server.

The distinction that matters: **WSL cannot see the USB device itself.** There
is no `/dev/bus/usb` and no usbip client, so WSL's own adb will never enumerate
a cable-attached phone without `usbipd-win` installed on the Windows side. What
works is letting the *Windows* adb server own the device and talking to it from
WSL.

```bash
WADB=/mnt/c/Users/dheir_ii8c/AppData/Local/Android/Sdk/platform-tools/adb.exe
"$WADB" start-server; sleep 6; "$WADB" devices -l
```

A USB device shows a plain hardware serial (`yx8pozg6dqtk75cq`); a wireless one
shows `<ip>:<port>`. That is how you tell which transport you are on.

The same binary also discovers phones over `_adb-tls-connect._tcp` by itself
when they are on wifi, so **no IP and no port are needed** for wireless either —
which sidesteps the port-changes-every-few-minutes problem that plagued Luna.
WSL's adb cannot do that: WSL2's NAT blocks mDNS multicast, so `adb mdns
services` returns nothing there.

If you must use WSL's adb for a wireless device, both IP and port have to be
read off the phone each time:

```bash
export PATH="$HOME/Android/Sdk/platform-tools:$PATH"
adb kill-server && adb connect <PHONE_IP>:<PORT>
```

*Historical note:* Luna recorded "USB does not reach WSL" alongside
`adbd: timed out while waiting for FUNCTIONFS_BIND` and a flapping transport on
the **Note 12 Pro**, and concluded USB was not worth pursuing. The first half is
still true literally; the conclusion was too broad. On the Note 15 Pro over the
Windows adb, USB is the most reliable channel available.

### ⚠ Only one adb server may run

`adbd` accepts a single connection. A WSL server and a Windows server steal the
device from each other, and the symptom is indistinguishable from a flaky
phone. In Luna this was misread as a hardware fault and cost about an hour.
**Kill one before using the other.** Pulls from the Windows adb must name a
Windows path (`C:\...`), readable from WSL under `/mnt/c/...`.

---

## 2. Target device — REDMI Note 15 Pro 5G (25080RABDG)

**This is the phone the app is for, and the primary development device.**
Verified over adb 2026-09-06.

| | |
|---|---|
| Model | `25080RABDG`, codename `lapis`, product `lapis_global` |
| OS | **Android 16 — API 36**, HyperOS **OS3.0** (`OS3.0.307.0.WPPMIXM`) |
| CPU / RAM | arm64-v8a · 7.6 GB |
| Screen | 1280×2772 @ 520 dpi |
| Transport | **USB**, via the Windows adb server (no TCP port set) |

### Everything works on this device

Measured, not assumed:

| Capability | Result |
|---|---|
| `adb shell input` (injection) | ✓ **works** — `KEYCODE_WAKEUP` and `KEYCODE_HOME` both returned rc=0 |
| `adb logcat` app logs | ✓ **unfiltered** — full output |
| `adb exec-out screencap -p` | ✓ **works** — 276 KB (this form returns 0 bytes on the Note 12 Pro) |
| `adb shell screencap` + `pull` | ✓ works |
| `adb shell uiautomator dump` | ✓ works |

**Consequence: UI flows can be driven programmatically here.** Claude can tap,
swipe, screenshot and read logs without a human in the loop. This is a large
practical improvement over the Note 12 Pro and it changes how M0 and M1 get
tested — automated interaction is available, and the user is needed only for
the judgement calls (does the 3-tap log *feel* fast, is the reminder tone
right) that were always theirs anyway.

### Android 16 / API 36 vs the specified compileSdk 35

`ARCHITECTURE.md` specifies `compileSdk 35` / `targetSdk 35`, and only
`platforms/android-35` is installed locally. The app runs correctly on Android
16 either way; targeting 35 simply does not opt into API 36's new enforcements.

**Decision: stay on 35 through M0 and M1**, revisit at M2 once the UI has
settled. Moving to 36 requires `sdkmanager "platforms;android-36"` and brings
forced edge-to-edge layout and predictive back — real UI consequences, not a
version-number change. Chasing an SDK bump before there are screens to lay out
is backwards.

### Installing: the prompt has a short timeout

`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` from `adb install`
almost always means **the on-device confirmation prompt was not accepted in
time** — not that anything is misconfigured. Xiaomi's *Install via USB* toggle
(Settings → Additional settings → Developer options) must be on, but once it
is, every install still raises a prompt on the phone that has to be tapped
within a few seconds.

**Be looking at the phone before running `adb install`.** Announce it, then run
it. Several failures on 2026-09-06 were logged here as OS refusals; the user
corrected that — the prompt was appearing, nobody was watching it.

Two earlier claims in this file were **wrong and have been removed**: that the
toggle turns itself back off, and that a fresh install is gated harder than an
update. Both were inferred from failures that had the simpler explanation
above. The "no dialog shown at all" observation came from a screenshot taken
three seconds in, which is not long enough to conclude a dialog never appeared.

**Fallback if the timing is awkward:** push the APK to `/sdcard/Download/` and
install it by tapping the file in the Files app.

```bash
"$WADB" push <apk> /sdcard/Download/thirsttrap-debug.apk
```

### HyperOS still kills background work

OS3.0 is newer than the Note 12 Pro's HyperOS, but it is the same lineage:
**Autostart is off by default** and the system will kill scheduled work
whichever scheduling approach the app uses. Reminders on a default-configured
device will not arrive.

**Autostart is NOT on the app's own info page.** Confirmed on the Note 15 Pro
(HyperOS OS3.0): `ACTION_APPLICATION_DETAILS_SETTINGS` opens MIUI's *App info*
screen, which has Storage, Power, permissions and "Pause app activity if
unused" — but **no Autostart toggle at all**. Sending a user there and telling
them to find it sends them somewhere it is not.

The Autostart list is a separate screen:

- Settings → Apps → Permissions → **Autostart**, or
- deep-link `com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity`,
  which resolves on this ROM and is what the in-app X7 button now uses.

Also turn **off** "Pause app activity if unused" on the App info page. Android
enables it by default and it stops notifications and revokes permissions for an
app left unopened — precisely wrong for a reminder app, and easy to miss because
it sits under "Permissions" rather than anything battery-related.

Then **Battery saver → No restrictions**, which *is* on the App info page.

This is why feature `X7` ("Reminders not arriving?" help screen) is an **M1**
deliverable, not later polish. Run the NOTIFICATIONS §7 matrix on this phone
with Autostart both **on and off** — the "off" run is what an ordinary user
gets. See handover D6.

---

## 3. Secondary device — Redmi Note 12 Pro (2209116AG)

Android 13 / API 33, arm64-v8a, 7.7 GB RAM, 1080×2400 @ 440 dpi, HyperOS
V816.0.33.0. Pairing with this box was done during the Luna project and
persists. Kept as a **compatibility check on an older Android**, not as a
development device — it is worse in every respect that matters.

### `adb shell input` is permanently unavailable here

Every attempt returns `SecurityException: INJECT_EVENTS`. On Xiaomi, event
injection is gated behind *USB debugging (Security settings)* — a separate
toggle from plain USB debugging — which requires a signed-in Mi account, which
requires a SIM. **There is no SIM in that phone.**

So no `tap`, no `swipe`, no `KEYCODE_WAKEUP` on this device, ever. Not
intermittent; earlier Luna notes calling it flaky were wrong and sent a session
hunting a fault that does not exist. Anything behind a tap there needs a human.

The one exception that does work:

```bash
adb shell monkey -p dev.dheirav.thirsttrap -c android.intent.category.LAUNCHER 1
```

Do not use `monkey` for anything else — its other modes send random events.

### Other constraints (Note 12 Pro only)

| Thing | Reality |
|---|---|
| Screenshots | Two-step only. `adb exec-out screencap -p > f.png` returns **0 bytes** there. |
| Sleeping screen | `screencap` returns a valid, entirely black ~15 KB PNG rather than an error. |
| UI state | `uiautomator dump` — replaces logcat. |
| Crash logs | `adb logcat -b crash` works. Clear it first (`-c`). |
| `sqlite3` on device | Not present. Pull the database instead. |
| Connection lifetime | Often dies **within a minute** of a launch or install. Batch commands into one invocation. |
| Install failures | An **empty** error from `adb install` means interrupted, not rejected — retry. `INSTALL_FAILED_USER_RESTRICTED` is Xiaomi's *Install via USB* toggle, which wants a Mi account. |
| Wireless port | Pairing persists; the **port changes several times an hour**. Read it off the device, or use the Windows adb which needs no port. |

### APK size — applies to both phones

Luna's plain 25 MB debug APK took 1–3 minutes over wireless and died
mid-transfer more often than it completed; the minified debug installed in
**45 s**. Adopt a minified-debug variant early. Over USB on the Note 15 this
matters less, but it is free to set up and pays off the moment you go wireless.

---

## 4. Toolchain — already configured, do not "fix" it

| | Location | Notes |
|---|---|---|
| SDK | `~/Android/Sdk` | platform-35, build-tools 34.0.0 + 35.0.0, platform-tools, licences accepted |
| JDK (Gradle daemon) | `/usr/lib/jvm/java-21-openjdk-amd64` | Pinned in **`~/.gradle/gradle.properties`** |
| System default JDK | 24, via SDKMAN | Deliberately left alone |
| Android Studio | Windows side only | No Linux install |

The JDK pin lives at **user level, outside any repo**, so repos stay portable
and Windows Studio keeps working with its bundled JDK. ThirstTrap inherits it
with no action. **Do not add `org.gradle.java.home` to this repo's
`gradle.properties`** — that would break the Windows path.

`ANDROID_HOME` is unset and adb is not on PATH. Neither is required: Gradle
locates the SDK through a machine-local, gitignored `local.properties`. Setting
them is convenience only.

Follow Luna's build config as the template — Gradle wrapper 8.11.1,
`jvmTarget 17`, `jvmToolchain(17)` in the pure-JVM module, version catalog,
compileSdk/targetSdk 35.

### Timezone — a stale Luna trap

Luna's handover warns that the WSL box was UTC+4 while the phone was IST, so
epoch-millis timestamps read 1h30m early host-side. **This box now reads IST**
(verified 2026-09-06), so the trap no longer applies. Kept here only so that
nobody re-derives the correction and double-counts it.

---

## 5. Quick reference

```bash
WADB=/mnt/c/Users/dheir_ii8c/AppData/Local/Android/Sdk/platform-tools/adb.exe

# Connect (Windows adb finds the phone itself — no IP, no port)
"$WADB" kill-server; "$WADB" start-server; sleep 8; "$WADB" devices

# Build & install — use the minified debug variant for anything going to a phone
./gradlew :core:domain:test
./gradlew :app:installDebug

# Launch. On the Note 15, plain `am start` works; `monkey` is the Note 12 Pro fallback.
adb shell am start -n dev.dheirav.thirsttrap/.MainActivity

# Drive the UI — works on the Note 15, NEVER on the Note 12 Pro
adb shell input tap <x> <y>
adb shell input swipe <x1> <y1> <x2> <y2>

# Read UI state (replaces logcat)
adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml

# Screenshot. One-step works on the Note 15; the two-step form is the Note 12 Pro fallback.
adb exec-out screencap -p > s.png
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png . && adb shell rm /sdcard/s.png

# Crashes
adb logcat -b crash -c    # clear first
adb logcat -b crash

# Pull the database
adb exec-out run-as dev.dheirav.thirsttrap cat databases/thirsttrap.db > thirsttrap.db
```
