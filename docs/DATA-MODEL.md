# ThirstTrap — Data Model

Room / SQLite. This is the most expensive thing to change later, so it is
specified in full.

---

## Cross-cutting rules

### Primary keys are TEXT UUIDs

Every table uses `id TEXT PRIMARY KEY` holding a UUID v4 string, not
`INTEGER AUTOINCREMENT`.

**Why:** offline-first plus possible future sync (requirements item 16) means
two devices could mint the same integer. UUIDs also make export/import
round-trips idempotent — re-importing a backup updates rows instead of
duplicating them. Cost is a slightly larger index, which is irrelevant at this
data volume.

### Timestamps are stored twice

```sql
timestamp          INTEGER NOT NULL,  -- epoch millis, UTC
tz_offset_minutes  INTEGER NOT NULL   -- local offset when recorded, e.g. 330 for IST
```

**Why both:** "days since last watered" must be counted in the *local calendar
the user actually lived*. If you water at 23:00 IST and store only UTC, the
event lands on the previous day and every interval calculation is off by one.
Storing the offset lets you reconstruct the local civil date exactly, even for
events logged in another timezone.

Never store local time alone. Never store a formatted string.

### Enums are TEXT, not ordinals

Store `"watered"`, not `3`. Ordinals break the moment you reorder the enum, and
they make the exported JSON unreadable. Use a Room `TypeConverter` per enum,
and make every converter tolerate an unknown value by mapping it to a
designated `UNKNOWN` member rather than throwing — a future version's event
type must not crash an older parser reading an export.

### Deletion

- Plants are **archived** (`archived = 1`), not deleted. Dead plants keep their
  history; requirements: *"post-mortems are half the value."*
- Hard delete exists but is a settings-screen action, requires a typed
  confirmation, and **offers an export first**.
- Hard delete cascades via `ON DELETE CASCADE`, and must also delete the
  plant's photo directory — SQLite cascade will not touch the filesystem. This
  is a known orphan-file source; there is a maintenance task for it below.

---

## Tables

### `plants`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | TEXT | no | UUID, PK |
| `name` | TEXT | no | User's nickname, e.g. "marbled pothos" |
| `species` | TEXT | yes | Free text |
| `species_source` | TEXT | yes | `manual` / `plantnet` / `offline_model` |
| `species_confidence` | REAL | yes | 0–1, only when from an ID API |
| `acquired_date` | INTEGER | yes | Epoch **day**, not millis — no clock time is known |
| `source` | TEXT | yes | `bought` / `cutting` / `gift` / `volunteer` |
| `medium` | TEXT | no | `soil` / `water` / `sphagnum` / `semi_hydro` — current value only |
| `location` | TEXT | yes | Free text or picklist value |
| `status` | TEXT | no | `active` / `dormant` / `dead` / `given_away` |
| `container_desc` | TEXT | yes | "6-inch terracotta", "jam jar" |
| `pot_diameter_cm` | REAL | yes | Used to sanity-check weight swing |
| `has_drainage` | INTEGER | yes | Boolean 0/1 |
| `target_dryness` | TEXT | yes | "top 2–3 cm dry", "nearly weightless" |
| `light_needs` | TEXT | yes | Free text; compared against measured lux (item 22) |
| `fertilizer_cadence_days` | INTEGER | yes | |
| `depletion_trigger` | REAL | no | Default `0.5`. Per-plant MAD fraction |
| `wet_anchor_g` | REAL | yes | Null until calibrated |
| `dry_anchor_g` | REAL | yes | Null until calibrated |
| `dry_anchor_provisional` | INTEGER | no | 1 = still the estimated anchor, default 1 |
| `slope_ewma_g_per_day` | REAL | yes | Cross-segment drying-rate prior; negative |
| `needs_recalibration` | INTEGER | no | Set on repot / medium change, default 0 |
| `cover_photo_id` | TEXT | yes | FK → `photos.id`, `ON DELETE SET NULL` |
| `archived` | INTEGER | no | Default 0 |
| `created_at` | INTEGER | no | |
| `updated_at` | INTEGER | no | |

Indices: `(status)`, `(location)`, `(archived)`.

Note `medium` is the *current* value; every change also writes a
`medium_changed` care event, so the history is reconstructable. Requirements
flag medium conversion as high-risk and worth surfacing in history.

### `care_events`

The central log.

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | TEXT | no | UUID, PK |
| `plant_id` | TEXT | no | FK → `plants.id` `ON DELETE CASCADE` |
| `timestamp` | INTEGER | no | UTC millis |
| `tz_offset_minutes` | INTEGER | no | |
| `type` | TEXT | no | See enum below |
| `note` | TEXT | yes | Free text |
| `amount_ml` | REAL | yes | `watered` |
| `method` | TEXT | yes | `top` / `bottom_soak` — `watered` |
| `check_result` | TEXT | yes | `still_heavy` / `getting_light` / `dry_watered` — `checked` |
| `fertilizer_name` | TEXT | yes | `fertilized` |
| `dilution` | TEXT | yes | "1:5", "pinch/L" — `fertilized` |
| `from_medium` | TEXT | yes | `medium_changed` |
| `to_medium` | TEXT | yes | `medium_changed` |
| `milestone_kind` | TEXT | yes | `rooted` / `first_leaf` / `flowered` / `transplanted` |
| `cause` | TEXT | yes | `died` — post-mortem guess |
| `created_at` | INTEGER | no | |
| `updated_at` | INTEGER | no | |

Indices: **`(plant_id, timestamp DESC)`** — the timeline query, the hottest
read in the app. Also `(type, timestamp)` for stats, `(timestamp)` for a global
feed.

**Event types:**
`watered`, `checked`, `fertilized`, `water_changed`, `repotted`,
`medium_changed`, `pruned`, `treated`, `pest_or_disease`, `weeded`,
`observation`, `milestone`, `moved`, `died`, `unknown`

The nullable type-specific columns are a deliberate choice over a JSON blob or
a key-value side table: there are ~12 of them, they are queryable, and Room
maps them without ceremony. Revisit only if the count passes ~25.

### `weight_readings`

Separate from `care_events` on purpose. Weight readings are high-frequency
numeric data feeding a model; care events are narrative history. Mixing them
would make both the curve query and the timeline slower and uglier.

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | TEXT | no | UUID, PK |
| `plant_id` | TEXT | no | FK → `plants.id` `ON DELETE CASCADE` |
| `timestamp` | INTEGER | no | UTC millis |
| `tz_offset_minutes` | INTEGER | no | |
| `grams` | REAL | no | Total weight, pot included |
| `context` | TEXT | no | `routine` / `pre_water` / `post_water` / `calibration` |
| `care_event_id` | TEXT | yes | FK → `care_events.id` `ON DELETE SET NULL` |
| `segment_id` | TEXT | no | FK → `drying_segments.id` — denormalised for fast fits |
| `excluded` | INTEGER | no | User-flagged bad reading, default 0 |
| `created_at` | INTEGER | no | |

Indices: `(plant_id, timestamp)`, `(segment_id)`.

`context = post_water` re-anchors the wet anchor. `context = calibration` is
the initial anchor capture.

### `drying_segments`

One drying cycle: from a watering to the next. Persisted because segmentation
is stateful and the EWMA prior needs the history of *closed* segments.

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | TEXT | no | UUID, PK |
| `plant_id` | TEXT | no | FK → `plants.id` `ON DELETE CASCADE` |
| `started_at` | INTEGER | no | |
| `ended_at` | INTEGER | yes | Null = the open, current segment |
| `start_reason` | TEXT | no | `watering` / `repot` / `calibration` / `gap` |
| `final_slope_g_per_day` | REAL | yes | Written when the segment closes; feeds EWMA |
| `slope_method` | TEXT | yes | `theil_sen` / `two_point` / `insufficient` |
| `reading_count` | INTEGER | no | |

Index: `(plant_id, started_at DESC)`.

**Exactly one open segment per plant** (`ended_at IS NULL`). Enforce in the
repository; assert it in tests.

The *current* slope is computed live in `core:domain` on each read — it is a
median over ≤10 pairs and costs nothing. Only the **final** slope of a closed
segment is stored, because the EWMA depends on it and recomputing history on
every launch would be waste.

### `photos`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | TEXT | no | UUID, PK |
| `plant_id` | TEXT | no | FK → `plants.id` `ON DELETE CASCADE` |
| `care_event_id` | TEXT | yes | FK → `care_events.id` `ON DELETE SET NULL` |
| `relative_path` | TEXT | no | `photos/<plant-uuid>/<photo-uuid>.jpg` — **never absolute** |
| `taken_at` | INTEGER | no | UTC millis |
| `tz_offset_minutes` | INTEGER | no | |
| `width_px` | INTEGER | yes | |
| `height_px` | INTEGER | yes | |
| `bytes` | INTEGER | yes | For the storage-usage screen |
| `caption` | TEXT | yes | |
| `created_at` | INTEGER | no | |

Index: **`(plant_id, taken_at DESC)`** — drives both the photo grid and the
compare-view filmstrip.

A photo may exist without a care event (a plain snapshot) but never without a
plant. `taken_at` is the capture time, which is not necessarily the logging
time — a photo imported from the gallery keeps its original timestamp.

### `reminders`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | TEXT | no | UUID, PK |
| `plant_id` | TEXT | no | FK → `plants.id` `ON DELETE CASCADE` |
| `kind` | TEXT | no | `check` / `task` |
| `title` | TEXT | yes | `task` only, e.g. "remove humidity cover" |
| `interval_days` | INTEGER | yes | `check` only; null = derived from log |
| `next_due_at` | INTEGER | no | UTC millis |
| `enabled` | INTEGER | no | Default 1 |
| `snoozed_until` | INTEGER | yes | |
| `last_fired_at` | INTEGER | yes | |
| `created_at` | INTEGER | no | |

Index: `(next_due_at)` — the "what is due" query runs on every WorkManager tick.

**There is no `reminder_history` table.** Completing a check writes a `checked`
care event; that *is* the history. A separate table would be a second source of
truth for the same fact.

### `experiments` / `experiment_subjects` (phase 2)

`experiments`: `id`, `name`, `hypothesis`, `variable_tested`, `started_at`,
`ended_at`, `conclusion`, `created_at`.

`experiment_subjects`: `id`, `experiment_id` (FK CASCADE), `plant_id` (FK,
nullable), `label` (for subjects that are not full plants, e.g. "seed group B"),
`group_name`.

Deliberately thin. Requirements scope this as a phase-2 container for short
studies; do not over-build it before the MVP proves out.

---

## Settings — not in Room

DataStore (Preferences). Keys:

```
plantnet_api_key          String?    user-supplied, optional
kindwise_api_key          String?    bring-your-own, optional (item 25)
default_depletion_trigger Float      0.5
reminder_hour             Int        default 9 (local)
use_exact_alarms          Boolean    default false — see NOTIFICATIONS.md
theme                     enum       system / light / dark
photo_quality             enum       small / standard / high
last_export_at            Long?
```

API keys in DataStore are stored in app-private storage. That is adequate for
this threat model (a personal device, a free-tier key). Do not add
EncryptedSharedPreferences ceremony for a key the user can regenerate in a
click.

---

## Migrations

- `@Database(version = N, exportSchema = true)`, schemas written to
  `app/schemas/` and **committed to git**.
- Write an explicit `Migration(from, to)` for every version bump. Use Room
  `@AutoMigration` where the change is purely additive, but still commit the
  generated schema.
- **`fallbackToDestructiveMigration()` must never appear in a release build.**
  If you use it in debug, guard it behind `BuildConfig.DEBUG`. This app is a
  diary; wiping it on upgrade is the worst bug the project can ship.
- Every migration gets a `MigrationTestHelper` test that opens the old schema,
  inserts a representative row, migrates, and asserts the row survived intact.

---

## Export format (requirements item 10)

A zip, produced via SAF so the user picks the destination:

```
thirsttrap-export-2026-09-06.zip
├── thirsttrap.json        # all tables, one array per table
├── manifest.json          # schema version, app version, export timestamp, counts
└── photos/
    └── <plant-uuid>/<photo-uuid>.jpg
```

- `thirsttrap.json` is `kotlinx.serialization` output of the domain entities,
  not Room rows — the export must not be coupled to the storage schema.
- `manifest.json` carries `schema_version` so a future import knows what it is
  reading.
- Photo paths in the JSON are the same relative paths used in the DB, so
  import is a straight copy plus row insert.
- **Import is idempotent**: rows are upserted by UUID. Re-importing the same
  zip twice changes nothing. This is the payoff for decision D5.
- Export must stream (`ZipOutputStream` over the file list), never build the
  archive in memory — a 3-year photo history is close to 1 GB by the
  requirements' own storage budget.

---

## Maintenance tasks

Two known sources of drift, both handled by a periodic WorkManager job (weekly,
`requiresDeviceIdle`):

1. **Orphan photo files** — files on disk with no matching `photos` row, left
   by a hard delete or a crashed capture. Delete them.
2. **Orphan rows** — a `photos` row whose file is missing (restore gone wrong,
   user cleared data partially). Mark the row, show a broken-image placeholder,
   never crash the grid.

Also expose a settings screen showing storage used, photo count, and a "clean
up now" button that runs the same job on demand.
