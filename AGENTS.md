# ONE Fasting Tracker - AI Agent Guidelines

This document provides essential information for AI agents (Claude, Gemini, etc.) working on the ONE fasting tracker project. This is a Kotlin-based Android application with companion Wear OS support.

## Project Overview

ONE is a fasting tracker app with:
- **Android App**: Phone/tablet app with widgets and notifications
- **Wear OS App**: Smartwatch app with complications, tiles, and ongoing activities
- **Shared Module**: Common business logic and data synchronization
- **Architecture**: Clean architecture with reactive programming (Kotlin Flows)

## Development Environment

- **IDE**: Android Studio (all development happens here)
- **Language**: Kotlin
- **UI**: Jetpack Compose (phone) and Wear Compose (watch)
- **DI**: Koin
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: 31 (Android 12)
- **Target SDK**: 36

## Core Libraries & Technologies

### Dependency Injection
- **Koin** (v4.0.2): Lightweight DI framework
  - Modules: `AppModule`, `WearAppModule`, `SharedModule`
  - Use `koinViewModel()` for ViewModel injection in Compose
  - Example: `private val viewModel: TodayViewModel by koinViewModel()`

### UI Frameworks
- **Jetpack Compose**: Modern UI toolkit for Android app
  - Material 3 design system (v1.4.0-alpha17)
  - Material Icons Extended for rich iconography
- **Wear Compose**: Specialized compose for Wear OS
  - Material 3 for Wear (v1.0.0-alpha37)
  - Horologist tools for enhanced wear development
- **Glance**: Widget framework (v1.1.1)
  - Used for home screen widgets
  - Declarative API similar to Compose

### Data & State Management
- **Kotlin Coroutines & Flow** (v1.8.1): Async programming
  - StateFlow for UI state
  - SharedFlow for events
  - Repository methods return Flow for reactive updates
- **DataStore** (v1.1.6): Key-value storage
  - Replaces SharedPreferences
  - Type-safe with Preferences DataStore
  - Used for fasting state persistence
- **Room** (v2.7.1): SQLite database wrapper
  - Store fasting history records
  - Entity: `FastingRecord`
  - DAO: `FastingRecordDao`

### Wear OS Specific
- **Wearable Data Layer API**: Phone-watch communication
  - Part of Play Services Wearable (v19.0.0)
  - DataClient for sync operations
  - MessageClient for one-time messages
- **Tiles API** (v1.5.0-beta02): Quick settings tiles
- **Complications API**: Watch face data providers
- **Ongoing Activities**: Persistent notifications during fasts

### Background Processing
- **WorkManager** (v2.10.1): Deferrable background work
  - Schedule notification reminders
  - Periodic widget updates

### Analytics & Monitoring
- **Firebase Crashlytics**: Crash reporting
  - Auto-configured with google-services.json
  - Track non-fatal exceptions
  - Monitor stability metrics

## Critical Project Rules

### 1. Data Synchronization is Sacred
- **NEVER** break sync between phone and watch
- All fasting state changes MUST propagate via Wearable Data Layer
- Path: `/fasting_state` with keys: `is_fasting`, `start_time`, `fasting_goal`, `update_timestamp`
- Always include timestamp for conflict resolution

### 2. UI Component Updates
When data changes occur:
- **Widgets**: Update via `WidgetUpdateManager` in app module
- **Complications**: Force update via `ComplicationUpdateManager` in wear module  
- **Tiles**: Refresh via `TileService.requestUpdate()`
- **Ongoing Activities**: Managed by `OngoingActivityService`

### 3. Force Update Pattern
When receiving remote data updates:
```kotlin
// Example: In WatchFastingStateListenerService
override fun onDataChanged(dataEvents: DataEventBuffer) {
    // After updating local state...
    complicationUpdateManager.requestUpdateAll()
    tileService.requestUpdate()
}
```

## Module Structure

### `/shared` - Shared Android Library
- Core data models (`FastingDataItem`, `FastingRecord`)
- Repository interfaces and implementations
- `BaseFastingListenerService` - abstract service for sync
- Business logic (`FastingUseCase`)
- Constants and utilities
- Database setup (Room)
- DataStore configuration

### `/app` - Android Phone/Tablet App
- Main UI with Jetpack Compose
- Widget implementation (Glance)
- Notification scheduling (WorkManager)
- `FastingStateListenerService` extends `BaseFastingListenerService`
- Room database for history

### `/onewearos` - Wear OS App
- Wear-specific UI with Wear Compose
- Complications for watch faces
- Tiles for quick access
- Ongoing activities for persistent notifications
- `WatchFastingStateListenerService` extends `BaseFastingListenerService`

## Code Style Guidelines

### General Kotlin Conventions
- Use `data class` for models
- Prefer immutability (`val` over `var`)
- Use Kotlin coroutines and Flows for async operations
- Destructure imports when possible
- Follow standard Kotlin naming conventions

### Project-Specific Patterns
- Repository pattern for data access
- ViewModels for UI state management
- Dependency injection via Koin modules
- Reactive updates using StateFlow/SharedFlow

### Koin Dependency Injection
```kotlin
// Define modules
val appModule = module {
    viewModel { TodayViewModel(get()) }
    single { FastingDataRepositoryImpl(get(), get()) }
}

// In Application class
startKoin {
    androidContext(this@MainApplication)
    modules(appModule, sharedModule)
}

// In Composables
@Composable
fun TodayScreen() {
    val viewModel: TodayViewModel = koinViewModel()
    // ...
}
```

### File Organization
- Group related files in packages (e.g., `today/`, `notifications/`, `widgets/`)
- Keep platform-specific code in respective modules
- Share common logic in `/shared` module

## Common Tasks

### Adding a New Feature
1. Update shared models if needed in `/shared/core/models`
2. Modify repository interfaces in `/shared/core/data/repositories`
3. Add Koin definitions if new dependencies needed
4. Implement UI in respective module (`/app` or `/onewearos`)
5. Ensure sync works by testing on both devices
6. Update relevant UI components (widgets/complications/tiles)

### Modifying Fasting Logic
1. Update `FastingUseCase` in shared module
2. Ensure `FastingEventManager` callbacks are triggered
3. Test sync between devices
4. Verify widgets/complications update correctly

### Working with Widgets/Complications/Tiles
- Widgets: Use Glance API in `/app/widgets`
- Complications: Update `MainComplicationService` in `/onewearos`
- Tiles: Modify `MainTileService` in `/onewearos`
- Always trigger manual updates after data changes

### Adding New Dependencies
1. Add to `gradle/libs.versions.toml`
2. Reference in module's `build.gradle.kts`
3. Update Koin modules if needed
4. Document usage in relevant code

## Code Quality Standards (Recommended)

### Linting
Consider adding to `app/build.gradle.kts`:
```kotlin
// ktlint
id("org.jlleitschuh.gradle.ktlint") version "12.1.0"

// detekt  
id("io.gitlab.arturbosch.detekt") version "1.23.6"
```

### Formatting
- Use Android Studio's default Kotlin style
- Format code with Ctrl+Alt+L (Cmd+Option+L on Mac)
- Optimize imports with Ctrl+Alt+O (Cmd+Option+O on Mac)

## Building and Running

### Via Android Studio
1. Open project in Android Studio
2. Select run configuration (app or onewearos)
3. Choose device/emulator
4. Click Run (Shift+F10)

### Gradle Commands (if needed)
```bash
# Build debug APK
./gradlew app:assembleDebug
./gradlew onewearos:assembleDebug

# Install on device
./gradlew app:installDebug
./gradlew onewearos:installDebug

# Clean build
./gradlew clean
```

## Common Pitfalls to Avoid

1. **Forgetting to update UI components** after data changes
2. **Not including timestamp** in sync updates (causes conflicts)
3. **Modifying sync logic** without testing both directions
4. **Creating local-only state** that should be synced
5. **Not filtering local events** in listener services (causes loops)
6. **Missing Koin injection setup** for new ViewModels or repositories
7. **Using blocking calls** instead of coroutines/flows

## Testing Synchronization

1. Install app on both phone and watch
2. Start fasting on phone → verify watch updates
3. Stop fasting on watch → verify phone updates
4. Check widgets/complications reflect current state
5. Force close apps and verify state persists

## Firebase/Crashlytics

- Configuration files already in place (`google-services.json`)
- Crashlytics enabled for error tracking
- No additional setup needed

## Debugging Tips

### For Sync Issues
1. Check logcat for both devices
2. Verify DataLayer path and keys match
3. Ensure timestamps are properly updated
4. Check node ID filtering in listener services

### For UI Update Issues  
1. Verify update managers are called
2. Check if UI components are registered properly
3. Look for lifecycle issues
4. Ensure Glance/Compose states are updated

### For Dependency Injection Issues
1. Check Koin module definitions
2. Verify scope (single vs factory)
3. Ensure modules are loaded in Application class
4. Look for circular dependencies

## Project-Specific Knowledge

- Fasting goals are predefined: 13h, 16:8, 18:6, 20:4, 36h
- Weekly progress tracking is available
- Notifications scheduled during active fasts
- Spanish localization available (`values-es`)
- Material 3 with dynamic theming
- Minimum Android 12 required (API 31)

## When Making Changes

1. **Make a plan first** - understand the requirements, identify affected components, and create a step-by-step approach
2. **Think sync first** - will this change need to sync?
3. **Update all UI components** - widgets, complications, tiles, ongoing activities
4. **Test on both platforms** - phone and watch
5. **Maintain clean architecture** - keep layer separation
6. **Follow existing patterns** - consistency is key
7. **Use existing libraries** - don't reinvent the wheel
8. **Keep dependencies updated** - check `libs.versions.toml`
9. **Update changelogs** - document major changes in `CHANGELOG.md` (Android app) and `CHANGELOG_WEAROS.md` (Wear OS app)

## Changelog Management

- **CHANGELOG.md**: Track changes for the Android phone/tablet app
- **CHANGELOG_WEAROS.md**: Track changes for the Wear OS companion app
- Add **customer-facing** changes, bug fixes, and new features to the `[Unreleased]` section
- When releasing, move items from `[Unreleased]` to a versioned section
- Use standard categories: Added, Changed, Fixed, Deprecated, Removed, Security

### What to Include in Changelog:
- New features users can see/use
- Bug fixes that affect user experience
- UI/UX changes users will notice
- Performance improvements users can feel
- Breaking changes to user workflows

### What NOT to Include:
- Internal code refactoring
- Component extractions/reorganization
- Dependency updates (unless they affect users)
- Architecture improvements
- Code style changes
- Developer tooling updates

Remember: The core challenge of this project is maintaining perfect synchronization between devices while keeping all UI components updated. When in doubt, force update the UI components!