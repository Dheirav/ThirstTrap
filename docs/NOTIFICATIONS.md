# ThirstTrap — Reminders & Notifications

The requirements doc calls this *"the app's hardest feature, not its easiest"*
and says to budget real time for it. That is correct, and it is why this
document exists separately.

---

## 1. The central decision: inexact by default

**This deviates from the requirements doc**, which specified
`exactAllowWhileIdle`. Handover decision D4.

### The problem with exact alarms

On Android 13+, `SCHEDULE_EXACT_ALARM` is **denied by default**. To use it you
must send the user to a system settings screen
(`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`) and ask them to grant it manually. That
is a terrible first-run experience and a permanent source of "the app stopped
reminding me" — because the OS can revoke it, and some OEM skins revoke it
aggressively.

There is a second permission, `USE_EXACT_ALARM`, which is granted
automatically — but the Play Store restricts it to apps whose *core function*
is alarms or calendars. A plant care app does not qualify. Declaring it is a
policy violation and a review rejection. **Do not use `USE_EXACT_ALARM`.**

### Why we do not need exactness

A watering *check* reminder is not time-critical to the minute. "Some time
around 9am, remind me to lift the pot" is the entire requirement. Nothing about
the product degrades if it arrives at 9:12.

### The design

**Default: a daily WorkManager job.** One periodic worker, scheduled for the
user's chosen hour (default 09:00 local). On each run it queries
`reminders WHERE enabled = 1 AND next_due_at <= now AND (snoozed_until IS NULL
OR snoozed_until <= now)` and posts one notification per plant that is due.

This buys three things for free:
- **No permission prompt at all** beyond `POST_NOTIFICATIONS`.
- **Reboot survival without a boot receiver** — WorkManager persists its queue
  in its own database and reschedules itself after restart.
- **Doze-friendly** — the OS batches it, so it costs almost no battery.

**Opt-in: exact alarms.** A settings toggle ("Remind me at a precise time"),
off by default. When switched on, check `AlarmManager.canScheduleExactAlarms()`
and, if false, show an in-app explainer *before* deep-linking to the settings
screen — never bounce the user to a system page with no context. Re-check the
capability on every app start, because it can be revoked; if it has been, fall
back to the WorkManager path silently and show a one-time notice.

The exact path uses `setExactAndAllowWhileIdle`, which is what the requirements
doc specified — it is simply no longer the default.

### Honest limitation

WorkManager's periodic work is *not* guaranteed to fire at a precise time; it
fires within a window when the device is awake, and Doze can defer it. For this
app that is acceptable. Document it in the in-app help so the user is not
surprised, rather than pretending it is precise.

---

## 2. Permissions

| Permission | When | Notes |
|---|---|---|
| `POST_NOTIFICATIONS` | API 33+, runtime | Request at a natural moment — **after the user creates their first reminder**, never on first launch |
| `SCHEDULE_EXACT_ALARM` | Only if exact mode enabled | Declared in manifest, granted via settings deep-link |
| `RECEIVE_BOOT_COMPLETED` | Only if exact mode enabled | WorkManager does not need it |

Nothing else. No storage permission (Photo Picker), no camera permission
(Google Code Scanner), no location.

If `POST_NOTIFICATIONS` is denied, the app must remain fully usable — reminders
degrade to an in-app due list on the dashboard. Never block the UI on it, never
re-prompt more than once.

---

## 3. Notification channels

Three, created at first launch. Separate channels let the user tune each
independently instead of muting everything.

| Channel | Importance | Contents |
|---|---|---|
| `watering_checks` | DEFAULT | "Time to check the marbled pothos" |
| `task_reminders` | DEFAULT | One-off tasks: "remove humidity cover" |
| `health_alerts` | LOW | Drying-rate diagnostics (WATERING-MODEL §7) |

Health alerts are LOW deliberately: they are informational, not urgent, and
should never buzz.

---

## 4. Reminder hygiene

The requirements list this as a top category complaint — Blossom ships with no
snooze, Planta piles up overdue nags. These are hard requirements, not polish.

### Every reminder is snoozable

Notification actions, so it is one tap **from the shade** without opening the
app:

- **"Watered"** → writes a `watered` care event, reschedules, dismisses.
- **"Still wet"** → writes a `checked` event with `check_result = still_heavy`,
  pushes `next_due_at` out, dismisses. This must feel as good to tap as
  "Watered" — see UI-SPEC §7 on tone.
- **"Snooze 1 day"** → sets `snoozed_until`.

Handle these in a `BroadcastReceiver` that delegates to a short-lived
`CoroutineWorker`. Do not do database writes on the receiver's main thread.

### Missed reminders collapse — they never stack

**One notification per plant, maximum, ever.** Use a stable notification ID
derived from the plant UUID, so a new reminder for the same plant *replaces*
the old one rather than adding to it.

If a plant was due three days ago and is still due, the user sees one
notification saying it has been 11 days — not three notifications. The
requirements phrase this as *"a missed reminder collapses silently into the
next one — no guilt stack."*

### Bulk clear

The in-app due list has a "clear all overdue" action that marks everything
checked-without-watering and reschedules. No confirmation dialog; it is
undoable via snackbar.

### Never guilt

No streak counters, no "you missed 4 days", no red badges counting failures.
The requirements are explicit: *plants aren't Duolingo*. A missed day must not
feel like failure.

---

## 5. Scheduling logic

```
next_due_at = last_relevant_event_time + interval_days
```

Where `interval_days` is, in priority order:
1. The user's explicit per-plant setting, if set.
2. The predicted ETA from the weight model, if a prediction is available
   (WATERING-MODEL §6) — this is the whole point of the app: the reminder is
   driven by the measured pot, not a calendar.
3. The computed average interval from the watering log (requirements item 8).
4. 7 days, as a last resort for a brand-new plant with no history.

When the weight model produces a prediction, **reminders reschedule
automatically** on each new weight reading. The reminder is a moving target
that tracks the plant.

`last_relevant_event_time` is the most recent `watered` **or** `checked` event
— a check that concluded "still wet" resets the clock just as a watering does,
because both mean "assessed recently".

---

## 6. OEM battery killers

Xiaomi/MIUI, Oppo/ColorOS, Vivo/FunTouch, OnePlus, Samsung and Huawei all ship
aggressive background-process killers that will silently stop scheduled work,
regardless of framework. This is not something the app can fix in code.

> **The target device is a REDMI Note 15 Pro 5G — Android 16, HyperOS OS3.0**
> (handover D6). This is the worst case, not an edge case. On HyperOS,
> **Autostart is off by default** and the system will kill scheduled work
> whichever scheduling approach the app uses. Reminders on a default-configured
> Redmi will simply not arrive until the user enables Autostart and exempts the
> app from battery optimisation. A newer HyperOS does not help: OS3.0 behaves
> the same way its predecessors did.
>
> Because of this, the help screen below is an **M1** deliverable (`X7`), not a
> later polish item. Test the notification matrix on this device with Autostart
> both **on and off** — the "off" run is what an ordinary user experiences.
>
> MIUI path: Settings → Apps → Manage apps → ThirstTrap → **Autostart** on, and
> **Battery saver → No restrictions**. Verify the exact path on the device; it
> moves between MIUI/HyperOS versions.

What the app does about it:

- A help screen: **"Reminders not arriving?"** — reachable from settings and
  linked from the reminder-creation screen.
- Detect `Build.MANUFACTURER` and show targeted instructions (the autostart /
  battery-optimisation path differs per skin). The `dontkillmyapp.com` guidance
  is the reference for per-OEM steps.
- Offer `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` as a **suggestion**, with
  an explanation of what it does. Never demand it, never gate the app on it.
- A **"send a test reminder in 10 seconds"** button on that screen so the user
  can verify the fix themselves rather than waiting a day.

---

## 7. Manual test matrix

Notification delivery cannot be meaningfully automated. Be honest about that
and test it deliberately instead of assuming it works.

Add a **debug menu** (debug builds only) with: fire a reminder now, fire in
10s, fast-forward a plant's `next_due_at`, dump the WorkManager queue.

| # | Scenario | Expected |
|---|---|---|
| 1 | Reminder due, app in foreground | In-app banner; no system notification |
| 2 | Reminder due, app backgrounded | System notification |
| 3 | Reminder due, app force-stopped | Notification still arrives (WorkManager) |
| 4 | Device rebooted before due time | Reminder survives, fires on schedule |
| 5 | Doze (`adb shell dumpsys deviceidle force-idle`) | Fires on next maintenance window |
| 6 | "Watered" tapped from shade, app closed | Event written, notification dismissed, rescheduled |
| 7 | "Still wet" tapped from shade | `checked` event written, due date pushed |
| 8 | "Snooze" tapped | `snoozed_until` set, no re-fire until then |
| 9 | Three plants overdue simultaneously | Three notifications, one per plant — never stacked per plant |
| 10 | Same plant overdue 3 days running | Exactly **one** notification, text reflects total elapsed |
| 11 | `POST_NOTIFICATIONS` denied | App fully usable; due list in-app; no crash, no nag loop |
| 12 | Exact mode on, permission then revoked in settings | Silent fallback to WorkManager + one-time notice |
| 13 | Timezone changed (travel) | Due times recomputed against the new local hour |
| 14 | DST-less IST, device clock manually changed | No duplicate or skipped fires |
| 15 | 50 plants, all due | Notifications posted without ANR; consider a summary group |

Run 1–12 on a real device before calling reminders done. An emulator does not
reproduce OEM behaviour, which is the thing most likely to break.
