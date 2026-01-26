# ONE - Fasting Tracker

A beautifully designed intermittent fasting tracker for Android and Wear OS that helps you monitor your fasting windows and achieve your health goals.

## Features

### Android App
- **Multiple Fasting Plans**: Support for 13h, 16:8, 18:6, 20:4, and 36h fasting windows
- **Real-time Tracking**: Live progress tracking with visual countdown
- **Weekly Progress**: Track your fasting consistency over time
- **Home Screen Widgets**: Quick access to fasting status and controls
- **Smart Notifications**: Reminders when your fasting goal is achieved
- **Material You Design**: Dynamic theming that adapts to your device
- **Offline First**: All data stored locally for privacy

### Wear OS Companion
- **Full Standalone App**: Start, stop, and track fasts directly from your watch
- **Watch Face Complications**: See fasting status at a glance
- **Tiles**: Quick access from the watch tiles menu
- **Ongoing Activities**: Persistent notification during active fasts
- **Perfect Sync**: Seamless synchronization between phone and watch

## Screenshots

*Coming soon*

## Requirements

- Android 12 (API 31) or higher
- Wear OS 3.0 or higher (for watch app)

## Installation

### From Source

1. Clone the repository
```bash
git clone https://github.com/charliesbot/one.git
cd one
```

2. Open the project in Android Studio

3. Build and run on your device or emulator

<details>
<summary><strong>Advanced Setup (Optional)</strong></summary>

#### Firebase Setup
To enable crash reporting and analytics:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add two Android apps (one for phone, one for wear)
3. Download `google-services.json` for each
4. Place them in `app/` and `onewearos/` directories

#### Release Builds
For release builds, create `keystore.properties` in the root:
```properties
storeFile=/path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Then build:
```bash
./gradlew app:installRelease
./gradlew onewearos:installRelease
```
</details>

## Architecture

The project follows clean architecture principles with three main modules:

- **`/shared`**: Common business logic, data models, and synchronization
- **`/app`**: Android phone/tablet application
- **`/onewearos`**: Wear OS application

### Key Technologies

- **UI**: Jetpack Compose & Wear Compose
- **DI**: Koin
- **Async**: Kotlin Coroutines & Flow
- **Storage**: DataStore & Room
- **Sync**: Wearable Data Layer API

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Development Tips

- See [CLAUDE.md](CLAUDE.md) for detailed development guidelines
- See [ARCHITECTURE.md](ARCHITECTURE.md) for system design details
- Ensure phone-watch sync works before submitting PRs
- Test all UI components (widgets, complications, tiles)
- Follow existing code style and patterns

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with Jetpack Compose and Material 3
- Icons from Material Icons Extended
- Fasting windows based on popular intermittent fasting protocols

## Author

Made with love by [@charliesbot](https://twitter.com/charliesbot)

## Contributors

Thanks to these awesome people for their contributions:

- [@nikunjgoel95](https://github.com/nikunjgoel95)