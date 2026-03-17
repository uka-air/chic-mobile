# Chic Mobile Uploader

Android app module for uploading files in the background using WorkManager.

## Prerequisites

- **Android Studio** Koala (or newer)
- **Android SDK 34**
- **JDK 17**
- Internet access for first dependency sync

## Build in Android Studio (recommended)

1. Open Android Studio.
2. Select **Open** and choose this project folder.
3. Let Gradle sync finish.
4. Build the app from:
   - **Build > Make Project** (quick compile), or
   - **Build > Build Bundle(s) / APK(s) > Build APK(s)** (APK output).

## Build from command line

This repository currently does **not** include the Gradle Wrapper (`./gradlew`).
Use a local Gradle 8.7+ installation and run from the project root:

```bash
gradle :app:assembleDebug
```

For a release build:

```bash
gradle :app:assembleRelease
```

## Output artifacts

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Optional checks

Run lint:

```bash
gradle :app:lint
```

Run unit tests:

```bash
gradle :app:testDebugUnitTest
```

## API helper script

To request a presigned key and then call `raw_audios` with that key:

```bash
./scripts/request_upload_urls.sh
```

The script performs:
1. `POST /api/v1/presigns`
2. `POST /api/v1/raw_audios` with `{"key":"<key-from-presigns>"}`
