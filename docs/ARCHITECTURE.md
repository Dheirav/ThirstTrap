# ThirstTrap — Architecture

Android-native, Kotlin, Jetpack Compose, offline-first, single user.

---

## 1. Module layout

Four modules. This is deliberately **fewer** than the Now-in-Android style
20-module split — that layout exists to parallelise builds across a team, and
for a solo project it buys build-config overhead you pay for on every change.

```
thirsttrap/
├── app/                  Android application. UI, ViewModels, navigation, DI wiring.
├── core/domain/          PURE KOTLIN JVM. Entities, algorithms, repository interfaces.
├── core/data/            Android library. Room, DataStore, file storage, repo impls.
└── core/ui/              Android library. Theme, design tokens, shared composables.
```

Features live as **packages inside `app/`**, not as separate Gradle modules:

```
app/src/main/kotlin/dev/dheirav/thirsttrap/
├── ThirstTrapApplication.kt
├── MainActivity.kt
├── navigation/
├── feature/
│   ├── dashboard/
│   ├── plantdetail/
│   ├── quicklog/
│   ├── weight/
│   ├── photos/
│   ├── reminders/
│   ├── calibration/
│   ├── export/
│   └── settings/
└── di/
```

**When to split further:** if `app/` build times exceed ~45s incremental, or a
feature grows past ~15 files, promote it to `feature:<name>`. Not before.

### The one boundary that matters

`core/domain` is declared as a **pure JVM module**:

```kotlin
// core/domain/build.gradle.kts
plugins { kotlin("jvm") }
```

Not `com.android.library`. This means `android.*`, `androidx.*`, `Context`,
`Bitmap` and Room annotations are **not on its classpath** — the rule is
enforced by the compiler, not by code review. That is decision D2 in the
handover: the cheap KMP hedge.

Consequences you must accept:
- Domain entities are plain `data class`es. Room entities are *separate*
  classes in `core/data`, with mapping functions between them.
- That mapping is real boilerplate (~10 lines per entity). It is the price of
  the boundary. Pay it.
- `java.time` is fine in a JVM module. `kotlinx-datetime` is also fine and is
  the more KMP-portable choice — prefer it for anything in domain.

### Dependency direction

```
app  ──────────────►  core:ui
 │                        │
 │                        ▼
 └──────────────────►  core:data  ──────►  core:domain
                                             ▲
 app ────────────────────────────────────────┘
```

`core:domain` depends on **nothing**. `core:data` implements interfaces
declared in `core:domain`. `app` depends on all three. Nothing depends on
`app`. There are no cycles and Gradle will tell you if you create one.

---

## 2. Layering inside a feature

Standard unidirectional data flow.

```
Composable screen
   │  events (user intents)      ▲  UiState (immutable data class)
   ▼                             │
ViewModel  ── exposes StateFlow<UiState>
   │
   ▼
Repository interface        (declared in core:domain)
   │
   ▼
Repository implementation   (core:data)
   │
   ├─► Room DAO  (Flow<List<Entity>>)
   ├─► File storage (photos)
   └─► DataStore (settings)
```

Rules:

- **ViewModels never touch Room types.** They see domain entities only.
- **UI state is one immutable data class per screen**, exposed as
  `StateFlow<XUiState>` via `stateIn(viewModelScope, WhileSubscribed(5_000), Initial)`.
  The 5-second timeout keeps the flow alive across configuration changes
  without leaking past real backgrounding.
- **Composables take state + lambdas, never a ViewModel**, except the single
  route-level composable that hoists it. This keeps everything previewable.
- **No `LiveData`.** Flow throughout.
- Use cases are optional. Introduce a `UseCase` class only when logic spans
  more than one repository or is genuinely reusable — the drying-curve
  calculation is the clear case. Do not write pass-through use cases.

---

## 3. Technology choices

| Concern | Choice | Note |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Dynamic colour, dark theme mandatory |
| Navigation | Navigation Compose, type-safe routes | Serializable route objects (Nav 2.8+) |
| DI | **Hilt** | Handles ViewModel + WorkManager injection cleanly |
| Database | **Room** + KSP | `exportSchema = true`, schemas committed |
| Settings | **DataStore (Proto or Preferences)** | Not Room. Not SharedPreferences. |
| Async | Coroutines + Flow | |
| Background work | **WorkManager** | Survives reboot for free — see NOTIFICATIONS.md |
| Charts | **Vico**, or Compose `Canvas` | Canvas is ~100 lines for the drying curve and gives exact control. Try Canvas first. |
| Images | **Coil 3** | Compose-native, good disk/memory cache |
| Camera / gallery | `ActivityResultContracts.TakePicture` + `PickVisualMedia` | Photo Picker needs **no storage permission** |
| QR | **ML Kit `GmsBarcodeScanning`** | Google Code Scanner needs **no camera permission** |
| Export | `java.util.zip.ZipOutputStream` + SAF `CreateDocument` | Both stdlib/framework; no dependency |
| JSON | `kotlinx.serialization` | Export format, and Pl@ntNet responses |
| HTTP | **Ktor client** | KMP-portable; Retrofit is fine too if you prefer |
| Light sensor | `SensorManager` + `TYPE_LIGHT` | Direct, no wrapper |

**On Hilt:** it costs annotation-processing build time. Worth it here because
WorkManager + ViewModel injection is otherwise fiddly boilerplate. If build
times annoy you, Koin is a legitimate swap — but decide once, early.

---

## 4. Build configuration

```
minSdk    = 26   (Android 8.0)
targetSdk = 35   (Android 15)
compileSdk= 35
```

**Why `compileSdk 35` when the target phone is API 36:** the Note 15 Pro runs
Android 16, but only `platforms/android-35` is installed and targeting 36
forces edge-to-edge layout and predictive back. The app runs correctly on
Android 16 either way. Stay on 35 through M1; revisit at M2 once the UI has
settled — see `docs/DEVICE.md` §2.

**Why `minSdk 26` and not lower:** API 26 gives `java.time` without
desugaring, notification channels as a first-class concept, and
`setExactAndAllowWhileIdle` semantics that are consistent. Below 26 you inherit
compatibility work for a user base of roughly nobody. This is a personal app on
a modern phone; there is no argument for 21.

Other settings:

- Kotlin JVM target 17, Java 17 toolchain.
- Version catalog (`gradle/libs.versions.toml`) for all dependencies. Not
  hardcoded strings in build files.
- R8 / minify enabled for release. Keep rules for Room entities and
  kotlinx-serialization.
- `.gitignore`: `/build`, `.gradle`, `local.properties`, `*.keystore`,
  `.idea/` (except `codeStyles`), `app/release/`.
- **Commit `app/schemas/`.** Room's exported schema JSON is how migrations are
  verified. Losing it means losing the ability to test migrations.

---

## 5. File storage

Photos never go in the database.

```
<app-private files dir>/
└── photos/
    └── <plant-uuid>/
        └── <photo-uuid>.jpg
```

- **Store relative paths in the DB** (`photos/<plant>/<photo>.jpg`), never
  absolute. Absolute paths break across reinstall and restore — this is the
  single most common way a photo-heavy app loses its images.
- App-private internal storage: no permission needed, excluded from other
  apps, wiped on uninstall (which is why export matters).
- Compress on capture to ~200–500 KB (requirements NFR): resize longest edge to
  ~1600 px, JPEG quality ~80. Do this **once, on write**, not on read.
- Keep an EXIF-stripped copy — location metadata in a plant photo is a privacy
  leak into any exported zip.
- `FileProvider` for any outbound share; never expose a raw `file://` URI.

---

## 6. Testing strategy

The value here is lopsided, so put the effort where it pays.

**Heavily tested — `core:domain` (pure JVM, fast, no Android):**
- The drying-curve algorithm. This is the app's differentiator and it is pure
  maths over a list of readings. Test it hard:
  - Synthetic linear decay + Gaussian noise → asserted ETA within tolerance.
  - A single wild outlier → Theil–Sen must absorb it (this is *why* it was
    chosen over least squares).
  - Segmentation: assert a fit is **never** computed across a watering event.
  - Anchor adaptation, EWMA update, confidence tiering, all the suppression
    rules ("need another reading").
  - Property tests: for any monotonically decreasing series, ETA ≥ 0.
- Attention-sort ordering for the dashboard.
- Export/import round trip: serialize → deserialize → assert deep equality.

**Moderately tested — `core:data`:**
- Room DAO tests with an in-memory database.
- **Migration tests** using `MigrationTestHelper` against the committed
  schema JSON. Every migration, every version. Losing user data is the
  unforgivable failure in a diary app.

**Lightly tested — UI:**
- A few Compose UI tests for the quick-log flow (the 3-tap requirement) and
  the photo-compare picker. Not exhaustive screenshot testing; the churn cost
  exceeds the benefit on a solo project.

**Manually tested — notifications.** Be honest: this cannot be meaningfully
automated. `docs/NOTIFICATIONS.md` carries an explicit manual test matrix and
specifies a debug menu for firing reminders on demand.

Run with:
```bash
./gradlew :core:domain:test          # fast, run constantly
./gradlew testDebugUnitTest          # everything JVM
./gradlew connectedDebugAndroidTest  # needs a device/emulator
```

---

## 7. Things deliberately not done

- **No multi-module feature split.** Revisit only if build times justify it.
- **No repository-per-entity ceremony.** Group by aggregate: `PlantRepository`
  covers plants, events, and photos, because they are always read together.
  `WeightRepository` is separate because it feeds the model, not the timeline.
- **No `Result<T>` wrapper on every call.** Local database reads do not fail in
  interesting ways. Use exceptions for genuine errors; reserve explicit result
  types for network (Pl@ntNet) and export.
- **No abstraction over Room.** It is already the abstraction.
