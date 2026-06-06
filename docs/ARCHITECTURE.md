# ONE Fasting Tracker Architecture

## Overview

ONE is a multi-module Android and Wear OS fasting tracker. The app uses Jetpack
Compose, Wear Compose, Koin, Room, DataStore, Kotlin Flows, and the Wearable Data
Layer.

The architecture is split by responsibility:

- Pure models and domain contracts live in JVM modules.
- Android infrastructure lives in `:core:data`.
- Shared non-string design assets live in design-system modules.
- Platform shells own app startup, navigation, and platform-specific services.
- Feature modules own user-facing UI and ViewModels.

## Modules

| Module | Purpose |
| --- | --- |
| `:app` | Phone/tablet shell, Navigation 3, Koin startup, widgets, phone sync service |
| `:wear` | Wear OS shell, Wear navigation, Koin startup, watch sync service and ongoing activity hooks |
| `:widget` | Glance widgets |
| `:complications` | Wear complication data sources |
| `:tiles` | Wear tiles |
| `:features:dashboard:app` | Phone dashboard and today screen |
| `:features:dashboard:wear` | Wear dashboard |
| `:features:profile:app` | Phone fasting history/profile UI |
| `:features:settings:app` | Phone settings UI |
| `:core:model` | Pure Kotlin models and value types |
| `:core:domain` | Pure Kotlin repository interfaces, use cases, and domain logic |
| `:core:data` | Android data layer: Room, DataStore, repositories, sync, notifications, DI |
| `:core:strings` | Shared user-facing strings and translations |
| `:core:designsystem:common` | Shared non-string resources and common Compose UI |
| `:core:designsystem:app` | Phone-only shared Compose components |
| `:core` | Legacy Android compatibility module for remaining shared Android code and notification resources |

`:core` is intentionally smaller than before. New pure logic should go to
`:core:model` or `:core:domain`; new data/platform infrastructure should go to
`:core:data`; new strings should go to `:core:strings`; new shared UI should go
to the appropriate design-system module.

## Dependency Flow

```
:core:model
    ↑
:core:domain
    ↑
:core:data

:core                       → :core:domain, :core:model
:core:data                  → :core, :core:domain, :core:model
:core:strings
:core:designsystem:common
:core:designsystem:app     → :core, :core:strings, :core:designsystem:common

:features:*:app            → :core, :core:strings, :core:designsystem:app,
                              :core:designsystem:common
:features:dashboard:wear   → :core, :core:strings, :core:designsystem:common

:app                       → :core, :core:data, :core:strings,
                              :core:designsystem:common, :features:*:app, :widget
:wear                      → :core, :core:data, :core:strings,
                              :core:designsystem:common, :features:dashboard:wear,
                              :complications, :tiles
:widget                    → :core, :core:strings
:complications             → :core, :core:strings
:tiles                     → :core, :core:strings
```

Feature modules should not depend on each other. Phone feature modules may use
`:core:designsystem:app`; Wear feature modules must not depend on phone-only
design-system code.

## Core Layer Responsibilities

### `:core:model`

Pure Kotlin data structures used across modules. This module must not depend on
Android, Compose, Room, DataStore, WorkManager, or Koin.

Examples:

- fasting state and history models
- pure goal data models
- pure fasting rules and goal catalogs

### `:core:domain`

Pure Kotlin business logic. Repository interfaces and use cases live here so UI
and platform code can depend on contracts instead of concrete data
implementations.

Examples:

- `FastingDataRepository`
- `SettingsRepository`
- `CustomGoalRepository`
- fasting use cases
- `FastingEventProcessor`
- notification scheduler abstraction

### `:core:data`

Android infrastructure and concrete implementations. This is where Android SDK,
Room, DataStore, WorkManager, Play Services Wearable, and Koin wiring belong.

Examples:

- Room database and DAOs
- repository implementations
- `BaseFastingListenerService`
- Wearable Data Layer helpers
- notification scheduling
- `SharedModule`
- `HistoryDatabaseModule`

### `:core:strings`

Shared user-facing strings used by phone, Wear, widgets, tiles, and
complications. Translations live next to the default strings in locale-specific
resource directories.

Examples:

- navigation labels
- feature copy and action labels
- widget, tile, and complication text
- notification text

### `:core:designsystem:common`

Shared non-string resources and UI that can be used by phone, Wear, widgets, and
other surfaces.

Examples:

- vector drawables
- fonts
- shared typography helpers
- `FastingProgressBar`

### `:core:designsystem:app`

Shared phone-only Compose components. These components use phone Material 3 and
must not be consumed by Wear modules.

Examples:

- `TimePickerDialog`
- `DateTimeWheelPicker`
- `WheelPicker`
- `GoalSelectionCard`
- `FastingMonthCalendar`

## Feature Modules

A feature is a complete user journey or business capability, not just a single
screen. Features live under `features/<name>/<platform>`.

Current features:

- `:features:dashboard:app`
- `:features:dashboard:wear`
- `:features:profile:app`
- `:features:settings:app`

Feature modules own their Compose screens, ViewModels, and feature-scoped UI
components. They use Koin for ViewModel injection and expose screens for the
platform shells to call.

## Platform Shells

### Phone/tablet app (`:app`)

The phone shell owns:

- Application startup and Koin module loading
- Navigation 3 setup
- phone-specific services
- Glance widget integration
- app-level resources such as launcher icons and manifest values

### Wear app (`:wear`)

The Wear shell owns:

- Application startup and Koin module loading
- Wear navigation setup
- watch sync service hooks
- ongoing activity updates
- watch-specific resources and manifest values

Wear should reflect data owned by the app/data layer. Importing a module does not
automatically install its DI bindings; platform shells explicitly load the Koin
modules they need.

## Phone and Watch Sync

Fasting state sync uses the Wearable Data Layer. Repository implementations in
`:core:data` write local state and publish remote updates. `:app` and `:wear`
extend the shared listener service from `:core:data` with platform-specific UI
surface updates.

See `docs/DATA_SYNC.md` for paths, keys, conflict resolution, and key files.

## Rules

- Keep `:core:model` and `:core:domain` Android-free.
- Put repository interfaces and use cases in `:core:domain`.
- Put repository implementations, Room, DataStore, WorkManager, sync, and Koin
  data bindings in `:core:data`.
- Put shared user-facing strings in `:core:strings`.
- Put non-string shared resources in `:core:designsystem:common`.
- Put phone-only shared Compose components in `:core:designsystem:app`.
- Keep Wear UI on Wear Compose and do not depend on `:core:designsystem:app`.
- Do not add dependencies directly to feature modules without checking module
  boundaries.
- All fasting state changes that should appear on both devices must sync through
  the Wearable Data Layer with timestamps.

## Adding New Code

Use this placement guide:

| New code | Module |
| --- | --- |
| Pure model/value object | `:core:model` |
| Repository interface or use case | `:core:domain` |
| Room/DataStore/WorkManager/Play Services implementation | `:core:data` |
| Shared user-facing string | `:core:strings` |
| Shared vector/font/non-string resource | `:core:designsystem:common` |
| Phone-only reusable Compose component | `:core:designsystem:app` |
| Wear-only reusable Compose component | Wear feature or future Wear design-system module |
| Feature screen/ViewModel | Matching `:features:<name>:<platform>` module |
| App startup/navigation/platform service | `:app` or `:wear` |
