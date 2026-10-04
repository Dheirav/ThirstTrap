#!/usr/bin/env python3
"""Pull the real diary off the phone and write the fixture the model can read.

The claim this app rests on cannot be settled by unit tests, only by real pots.
This exports what the device holds into a plain TSV that `RealDiaryReport` reads
in core:domain, so the actual Kotlin scores the actual history. Reimplementing
the model in Python here would answer a different question: whether my
reimplementation agrees with my data, not whether the app is right.

  python3 tools/analysis/export-diary.py
  ./gradlew :core:domain:test --tests '*RealDiaryReport*' -i

The fixture is gitignored. It is the user's diary, not test data.

Note the two traps, both hit on the way to this working. `adb shell cat` on
Windows translates LF to CRLF and silently corrupts the database; `exec-out`
does not. And the WAL holds recent writes, so it has to be checkpointed or
pulled alongside, or the newest readings are simply missing.
"""
import os, sqlite3, subprocess, sys, tempfile

PKG = 'dev.dheirav.thirsttrap'
ADB = os.environ.get('ADB') or next(
    (p for p in ('/mnt/c/Users/dheir_ii8c/AppData/Local/Android/Sdk/platform-tools/adb.exe',
                 '/usr/bin/adb') if os.path.exists(p)), None)
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'core', 'domain', 'src', 'test', 'resources', 'real-diary.tsv')


def pull(tmp):
    if not ADB:
        sys.exit('no adb found; set ADB=/path/to/adb')
    subprocess.run([ADB, 'shell',
                    f"run-as {PKG} sqlite3 databases/thirsttrap.db 'PRAGMA wal_checkpoint(TRUNCATE);'"],
                   capture_output=True)
    for f in ('thirsttrap.db', 'thirsttrap.db-wal', 'thirsttrap.db-shm'):
        r = subprocess.run([ADB, 'exec-out', f'run-as {PKG} cat databases/{f}'], capture_output=True)
        if r.stdout:
            open(os.path.join(tmp, f), 'wb').write(r.stdout)
    db = os.path.join(tmp, 'thirsttrap.db')
    if not os.path.exists(db):
        sys.exit('could not read the database. Is the phone connected and the debug build installed?')
    return db


def main():
    with tempfile.TemporaryDirectory() as tmp:
        db = pull(tmp)
        c = sqlite3.connect(f'file:{db}?mode=ro', uri=True)
        c.row_factory = sqlite3.Row
        if c.execute('pragma integrity_check').fetchone()[0] != 'ok':
            sys.exit('database came off the phone corrupt (adb shell instead of exec-out?)')
        lines = []
        n = 0
        for p in c.execute('select id, name, depletion_trigger from plants'):
            rs = list(c.execute(
                'select timestamp t, grams, context, excluded from weight_readings '
                'where plant_id=? order by t', (p['id'],)))
            if len(rs) < 3:
                continue
            n += 1
            lines.append(f"PLANT\t{p['name']}\t{p['depletion_trigger'] or 0.5}")
            for r in rs:
                lines.append(f"R\t{r['t']}\t{r['grams']}\t{r['context'] or 'ROUTINE'}"
                             f"\t{1 if r['excluded'] else 0}")
            for e in c.execute('select timestamp t, type from care_events where plant_id=? order by t',
                               (p['id'],)):
                ty = str(e['type']).upper()
                kind = 'W' if 'WATER' in ty else ('P' if 'REPOT' in ty or 'MEDIUM' in ty else None)
                if kind:
                    lines.append(f'{kind}\t{e["t"]}')
        os.makedirs(os.path.dirname(OUT), exist_ok=True)
        open(OUT, 'w').write('\n'.join(lines) + '\n')
        print(f'{n} plants with enough readings -> {os.path.relpath(OUT, ROOT)}')
        print("now run:  ./gradlew :core:domain:test --tests '*RealDiaryReport*' -i")
        c.close()


if __name__ == '__main__':
    main()
