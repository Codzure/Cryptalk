# Cryptalk

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Android CI](https://github.com/yourusername/Cryptalk/actions/workflows/android-ci.yml/badge.svg)](https://github.com/yourusername/Cryptalk/actions/workflows/android-ci.yml)

A secure messaging application built with modern Android development practices.

## Features

- End-to-end encrypted messaging
- Real-time message delivery
- Secure user authentication
- Clean and intuitive UI
- Built with Kotlin and Jetpack Compose

## Prerequisites

- Android Studio Flamingo (2022.2.1) or later
- Android SDK 33 or higher
- Kotlin 1.8.0 or higher
- Gradle 8.0 or higher

## Getting Started

1. Clone the repository:
   ```bash
   git clone [https://github.com/yourusername/Cryptalk.git](https://github.com/Codzure/Cryptalk.git)
   ```
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Run the app on an emulator or physical device

## Building

To build the debug version of the app:

```bash
./gradlew assembleDebug
```

## Testing

To run unit tests:

```bash
./gradlew test
```

To run instrumented tests on connected devices:

```bash
./gradlew connectedAndroidTest
```

## Code Style

This project uses ktlint for code style enforcement. To check the code style:

```bash
./gradlew ktlintCheck
```

To automatically fix code style issues:

```bash
./gradlew ktlintFormat
```

## Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) to get started.

## Security

Please review our [Security Policy](SECURITY.md) for reporting security vulnerabilities.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Maintainers

- [Leonard Mutugi]([https://github.com/yourusername](https://github.com/Codzure))

## Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Firebase](https://firebase.google.com/)
