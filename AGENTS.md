# ONE Fasting Tracker

Kotlin Android + Wear OS fasting tracker. Jetpack Compose UI, Koin DI, Room DB, DataStore, Kotlin Flows. Min SDK 31.

## Before You Start

1. Read `ARCHITECTURE.md` for the full project structure
2. Create a feature branch from `main`
3. Ask: **does this change need to sync between phone and watch?**

## Commands

```bash
./gradlew app:assembleDebug              # Build phone app
./gradlew onewearos:assembleDebug        # Build wear app
./gradlew app:installDebug               # Install phone app on device
./gradlew onewearos:installDebug         # Install wear app on device
./gradlew clean                          # Clean build
```

## Do Not

- **Break sync** between phone and watch — all fasting state changes MUST propagate via Wearable Data Layer
- **Forget UI component updates** after data changes — update ALL of: widgets (`WidgetUpdateManager`), complications (`ComplicationUpdateManager`), tiles (`TileService.requestUpdate()`), ongoing activities (`OngoingActivityService`)
- **Omit timestamps** in sync updates — required for conflict resolution
- **Create local-only state** that should be synced
- **Use blocking calls** — use coroutines and Flows everywhere
- **Add dependencies** without going through `gradle/libs.versions.toml` first
- **Skip Koin registration** for new ViewModels or repositories
- **Filter incorrectly** in listener services — always filter local events to prevent sync loops

## Architecture Rules

### Dependency flow

```
app ──→ features ──→ shared
onewearos ──────────→ shared
```

### Modules

| Module       | Purpose                                                                  |
| ------------ | ------------------------------------------------------------------------ |
| `:app`       | Phone/tablet app — Compose UI, Glance widgets, WorkManager notifications |
| `:onewearos` | Wear OS app — Wear Compose, complications, tiles, ongoing activities     |
| `:features`  | Feature modules shared by phone app                                      |
| `:shared`    | Core models, repositories, sync services, Room DB, DataStore, constants  |

### Key patterns

- Repository pattern for data access; repositories return `Flow`
- ViewModels manage UI state with `StateFlow`/`SharedFlow`
- Koin modules: `AppModule`, `WearAppModule`, `SharedModule` — use `koinViewModel()` in Compose
- Shared models live in `shared/core/models`; repository interfaces in `shared/core/data/repositories`

### Data sync

- Path: `/fasting_state` — keys: `is_fasting`, `start_time`, `fasting_goal`, `update_timestamp`
- Path: `/smart_reminder` — keys: `suggested_time`, `reasoning`, `timestamp`
- `BaseFastingListenerService` (shared) is extended by `FastingStateListenerService` (app) and `WatchFastingStateListenerService` (wear)

### Domain facts

- Fasting goals: `circadian` (13h), `16:8` (16h), `18:6` (18h), `20:4` (20h), `36hour` (36h)
- Minimum completed fast: 13 hours
- Spanish localization (`values-es`) in all three source modules

## Pull Requests

- PR title: use conventional commits format (`feat`, `fix`, `chore`, etc.) with optional scope, e.g. `feat(sync): add conflict resolution`
- PR body: brief description of what changed and why
- Keep the title under 70 characters
- Use the description for details, not the title
- Do not add "Co-Authored-By" lines
- End the PR body with hashtags: `#vibe-coded`, the AI tool used (e.g. `#claude-code`, `#gemini-cli`, `#codex`), and a tag for the feature (e.g. `#fasting`, `#sync`, `#widgets`)

## Changelogs

- `CHANGELOG.md` — Android phone/tablet app
- `CHANGELOG_WEAROS.md` — Wear OS app
- Add customer-facing changes to the `[Unreleased]` section using categories: Added, Changed, Fixed, Removed
- **Include**: features, bug fixes, UX changes, performance improvements
- **Exclude**: refactoring, dependency bumps, architecture changes, code style

## Project Docs

- `ARCHITECTURE.md` — full project structure and layer breakdown
- `CHANGELOG.md` / `CHANGELOG_WEAROS.md` — release history
- `gradle/libs.versions.toml` — all dependency versions
