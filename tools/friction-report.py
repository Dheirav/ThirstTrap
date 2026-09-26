#!/usr/bin/env python3
"""Friction report: what the diary says about how the app is actually used.

The D31 backfill feature was found by comparing two parallel data streams -
weight readings said waterings happened, the event log said they did not. This
script generalises that: every check here is a gap, an absence, or a workaround
that a single stream cannot show on its own. Run it monthly; it prints findings
or says there are none.

Usage:
    tools/friction-report.py <path-to-thirsttrap.db>
    tools/friction-report.py --pull          # fetch from a connected phone
                                             # (debug build, adb in PATH or
                                             #  ~/Android/Sdk/platform-tools)

Reads the database only. Safe on a live pull or an export.
"""

import argparse
import datetime
import os
import sqlite3
import subprocess
import sys
import tempfile

PKG = "dev.dheirav.thirsttrap"
DAY_MS = 86_400_000
POST_WATER_WINDOW_MS = 24 * 60 * 60 * 1000  # keep equal to POST_WATER_WINDOW_MILLIS
IST = datetime.timezone(datetime.timedelta(hours=5, minutes=30))


def ts(ms):
    return datetime.datetime.fromtimestamp(ms / 1000, IST).strftime("%d %b %H:%M")


def find_adb():
    for c in ("adb", os.path.expanduser("~/Android/Sdk/platform-tools/adb")):
        try:
            subprocess.run([c, "version"], capture_output=True, check=True)
            return c
        except (FileNotFoundError, subprocess.CalledProcessError):
            continue
    sys.exit("adb not found; pass a db path instead of --pull")


def pull_db(tmpdir):
    adb = find_adb()
    for f in ("thirsttrap.db", "thirsttrap.db-wal", "thirsttrap.db-shm"):
        out = subprocess.run(
            [adb, "exec-out", "run-as", PKG, "cat", f"databases/{f}"],
            capture_output=True,
        ).stdout
        with open(os.path.join(tmpdir, f), "wb") as fh:
            fh.write(out)
    path = os.path.join(tmpdir, "thirsttrap.db")
    if os.path.getsize(path) == 0:
        sys.exit("pull came back empty - is the phone connected and the debug build installed?")
    return path


class Report:
    def __init__(self):
        self.findings = 0

    def section(self, title):
        print(f"\n== {title}")

    def finding(self, line):
        self.findings += 1
        print(f"  ! {line}")

    def ok(self, line):
        print(f"  - {line}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("db", nargs="?", help="path to thirsttrap.db")
    ap.add_argument("--pull", action="store_true", help="pull from a connected phone")
    args = ap.parse_args()

    tmpdir = None
    if args.pull:
        tmpdir = tempfile.mkdtemp(prefix="tt-friction-")
        db_path = pull_db(tmpdir)
    elif args.db:
        db_path = args.db
    else:
        ap.error("give a db path or --pull")

    con = sqlite3.connect(db_path)
    con.row_factory = sqlite3.Row
    con.execute("PRAGMA wal_checkpoint(FULL)")

    all_plants = {r["id"]: dict(r) for r in con.execute("SELECT * FROM plants")}
    plants = {pid: p for pid, p in all_plants.items() if not p["archived"]}
    name = lambda pid: all_plants.get(pid, {}).get("name", "?")  # noqa: E731
    events = [dict(r) for r in con.execute("SELECT * FROM care_events ORDER BY timestamp")]
    readings = [
        dict(r)
        for r in con.execute("SELECT * FROM weight_readings ORDER BY timestamp")
    ]
    now = int(datetime.datetime.now().timestamp() * 1000)
    rep = Report()

    watered = {}
    for e in events:
        if e["type"] == "WATERED":
            watered.setdefault(e["plant_id"], []).append(e["timestamp"])

    # 1. Unlogged waterings: POST_WATER reading with no WATERED event in the
    #    prior window. Zero is the expectation since D31 backfills these; a hit
    #    here is a regression, or an import from before the fix.
    rep.section("Unlogged waterings (weights say watered, log does not)")
    hits = 0
    for r in readings:
        if r["context"] != "POST_WATER" or r["excluded"]:
            continue
        ws = watered.get(r["plant_id"], [])
        if not any(0 <= r["timestamp"] - w <= POST_WATER_WINDOW_MS for w in ws):
            rep.finding(f"{name(r['plant_id'])}: POST_WATER {r['grams']:.0f} g at {ts(r['timestamp'])} with no watering logged")
            hits += 1
    if not hits:
        rep.ok("none - every post-water weigh has its watering")

    # 2. The reverse gap: waterings with no POST_WATER weigh after them. Each
    #    one is a missed wet-anchor refresh, which is how the anchor goes stale.
    rep.section("Waterings without a post-water weigh (wet anchor starving)")
    hits = 0
    post_by_plant = {}
    for r in readings:
        if r["context"] == "POST_WATER" and not r["excluded"]:
            post_by_plant.setdefault(r["plant_id"], []).append(r["timestamp"])
    for pid, ws in watered.items():
        if not plants.get(pid, {}).get("weight_tracked", 0):
            continue
        for w in ws:
            posts = post_by_plant.get(pid, [])
            if not any(0 <= p - w <= POST_WATER_WINDOW_MS for p in posts):
                rep.finding(f"{name(pid)}: watered {ts(w)}, never weighed after")
                hits += 1
    if not hits:
        rep.ok("none - the wet anchor is being fed")

    # 3. Dry-anchor coverage: waterings with no PRE_WATER weigh shortly before.
    #    Not each one matters, but a plant with *no* pre-water reading ever is
    #    running on a guessed dry anchor forever.
    rep.section("Dry anchor coverage")
    for pid, p in plants.items():
        if not p.get("weight_tracked", 0) or pid not in watered:
            continue
        pres = [
            r for r in readings
            if r["plant_id"] == pid and r["context"] == "PRE_WATER" and not r["excluded"]
        ]
        if not pres:
            rep.finding(f"{name(pid)}: no PRE_WATER reading ever - dry anchor is still the guess")
        else:
            rep.ok(f"{name(pid)}: dry anchor measured ({len(pres)} pre-water weigh{'s' if len(pres) != 1 else ''})")

    # 4. Mis-weighs: excluded readings are the record of scale friction.
    rep.section("Excluded readings (mis-weighs)")
    exc = [r for r in readings if r["excluded"]]
    if exc:
        for r in exc:
            rep.finding(f"{name(r['plant_id'])}: {r['grams']:.0f} g at {ts(r['timestamp'])} excluded")
        rep.ok(f"{len(exc)} of {len(readings)} readings - above ~5% suggests a scale or placement problem")
    else:
        rep.ok(f"none of {len(readings)} readings excluded")

    # 5. Same-episode double waterings: two waterings within half a day are one
    #    episode logged twice, or a correction workaround.
    rep.section("Same-day double waterings")
    hits = 0
    for pid, ws in watered.items():
        ws = sorted(ws)
        for a, b in zip(ws, ws[1:]):
            if b - a < DAY_MS / 2:
                rep.finding(f"{name(pid)}: watered twice within {(b - a) / 3_600_000:.1f} h ({ts(a)} and {ts(b)})")
                hits += 1
    if not hits:
        rep.ok("none")

    # 6. Empty observations: an OBSERVATION with no note and no photo is a tap
    #    that recorded nothing - either a misfire or a flow that lost the user.
    rep.section("Empty observations (logged, but nothing in them)")
    photo_events = {
        r["care_event_id"]
        for r in con.execute("SELECT care_event_id FROM photos WHERE care_event_id IS NOT NULL")
    }
    empties = [
        e for e in events
        if e["type"] == "OBSERVATION" and not (e["note"] or "").strip() and e["id"] not in photo_events
    ]
    if empties:
        for e in empties:
            rep.finding(f"{name(e['plant_id'])}: empty observation at {ts(e['timestamp'])}")
    else:
        rep.ok("none - every observation carries a note or a photo")

    # 7. Reminder responsiveness: for enabled reminders, how the current due
    #    date relates to now. Persistent overdue is reminder fatigue starting.
    rep.section("Reminders")
    for r in con.execute("SELECT * FROM reminders WHERE enabled = 1"):
        pid = r["plant_id"]
        if all_plants.get(pid, {}).get("archived"):
            # An archived plant with a live reminder will nag about a pot that
            # is no longer on the shelf. Archiving should have disabled it.
            rep.finding(f"{name(pid)}: ARCHIVED but its reminder is still enabled")
            continue
        overdue_days = (now - r["next_due_at"]) / DAY_MS
        if overdue_days > 1:
            rep.finding(f"{name(pid)}: check overdue by {overdue_days:.1f} days")
        else:
            due = "due " + ts(r["next_due_at"])
            rep.ok(f"{name(pid)}: {due}")

    # 8. Feature staleness: when each part of the app was last actually used.
    #    A feature untouched for a month either finished its job or never had
    #    one - both are design information.
    rep.section("Feature staleness (days since last use)")
    def days_since(t):
        return (now - t) / DAY_MS if t else None

    last_by_type = {}
    for e in events:
        last_by_type[e["type"]] = e["timestamp"]
    for t, when in sorted(last_by_type.items(), key=lambda kv: kv[1]):
        d = days_since(when)
        line = f"{t.lower():15} last used {d:5.1f} days ago"
        (rep.finding if d > 30 else rep.ok)(line)
    last_photo = con.execute("SELECT MAX(taken_at) FROM photos").fetchone()[0]
    if last_photo:
        d = days_since(last_photo)
        (rep.finding if d > 30 else rep.ok)(f"{'photos':15} last used {d:5.1f} days ago")
    last_weight = readings[-1]["timestamp"] if readings else None
    if last_weight:
        d = days_since(last_weight)
        (rep.finding if d > 7 else rep.ok)(f"{'weighing':15} last used {d:5.1f} days ago")

    # 9. Weighing cadence per plant: the streak view. A tracked plant not
    #    weighed in 3+ days is the habit slipping - worth knowing early.
    rep.section("Weighing cadence")
    for pid, p in plants.items():
        if not p.get("weight_tracked", 0):
            continue
        mine = [r["timestamp"] for r in readings if r["plant_id"] == pid]
        if not mine:
            rep.finding(f"{name(pid)}: weight-tracked but never weighed")
            continue
        gap = (now - max(mine)) / DAY_MS
        n_days = len({t // DAY_MS for t in mine})
        span = max(1, (max(mine) - min(mine)) // DAY_MS + 1)
        line = f"{name(pid)}: {len(mine)} readings over {span} days ({n_days} distinct days), last {gap:.1f} days ago"
        (rep.finding if gap > 3 else rep.ok)(line)

    # 10. Usage instrumentation, when the table exists (schema v12+).
    #     Abandonments and overrides are the signals the diary cannot show.
    has_usage = con.execute(
        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='usage_events'"
    ).fetchone()[0]
    if has_usage:
        rep.section("Flow friction (usage instrumentation)")
        rows = [dict(r) for r in con.execute("SELECT * FROM usage_events ORDER BY timestamp")]
        by_flow = {}
        for u in rows:
            by_flow.setdefault(u["flow"], []).append(u)
        any_line = False
        for flow, us in sorted(by_flow.items()):
            opened = sum(1 for u in us if u["kind"] == "FLOW_OPENED")
            done = sum(1 for u in us if u["kind"] == "FLOW_COMPLETED")
            gone = sum(1 for u in us if u["kind"] == "FLOW_ABANDONED")
            overrides = [u for u in us if u["kind"] == "SUGGESTION_OVERRIDDEN"]
            if opened:
                line = f"{flow}: {opened} opened, {done} completed, {gone} abandoned"
                (rep.finding if opened and gone / max(opened, 1) > 0.25 else rep.ok)(line)
                any_line = True
            if overrides:
                from collections import Counter
                top = Counter(u["detail"] for u in overrides).most_common(3)
                detail = ", ".join(f"{d} x{n}" for d, n in top)
                (rep.finding if len(overrides) >= 5 else rep.ok)(
                    f"{flow}: {len(overrides)} suggestion overrides ({detail})"
                )
                any_line = True
        if not any_line:
            rep.ok("instrumented, nothing recorded yet")

    print(f"\n{'=' * 50}")
    if rep.findings:
        print(f"{rep.findings} finding(s) - each one is a candidate improvement or a habit slipping.")
    else:
        print("No findings. The diary and the workflows agree with each other.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
