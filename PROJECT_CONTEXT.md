# Project Context

## Project

- Name: Simple Light Keyboard
- Location: `/mnt/c/dev/projects/AndriodKeyboradInKB`
- Package/application ID: `com.example.simplelightkeyboard`
- Language: Java
- Android minimum SDK: 23
- Android target/compile SDK: 35
- Gradle plugin: Android Gradle Plugin `8.6.1`
- Gradle used in this environment: `8.7`
- Java used for builds: `/usr/lib/jvm/java-17-openjdk-amd64`

## Important source files

- `app/src/main/java/com/example/simplelightkeyboard/MainActivity.java`
  - Setup screen with a button opening Android input-method settings.
- `app/src/main/java/com/example/simplelightkeyboard/SimpleKeyboardService.java`
  - Android `InputMethodService`; commits text, handles enter, space, and delete.
- `app/src/main/java/com/example/simplelightkeyboard/SimpleKeyboardView.java`
  - Programmatic keyboard UI and key behavior.
- `app/src/main/AndroidManifest.xml`
  - Declares the launcher activity and input-method service.

## Current keyboard behavior

- Light lavender background with transparent key areas.
- Dark navy character labels.
- Dedicated `1-0` number row at the top.
- QWERTY letter rows.
- Special-character page opened with `?123` / `#+=`.
- Bottom row contains `?123`, question mark, space, period, and enter.
- Bottom navigation-bar inset is handled dynamically, with a minimum bottom padding.
- `SHIFT` persists after typing and converts letters to uppercase.
- `DEL` deletes one character on tap and repeats while held.
- Control labels use smaller text so `SHIFT`, `DEL`, and `ENTER` fit.

## Build environment

The repository intentionally does not contain `local.properties`. In this environment,
the Android SDK is installed at:

`/tmp/simple-light-build/android-sdk`

Gradle is installed at:

`/tmp/simple-light-build/gradle-8.7/bin/gradle`

Before building, create a temporary `local.properties` file:

```bash
printf 'sdk.dir=/tmp/simple-light-build/android-sdk\n' > local.properties
```

Use Java 17:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

## Build debug APK

```bash
/tmp/simple-light-build/gradle-8.7/bin/gradle --no-daemon assembleDebug
```

Output:

`app/build/outputs/apk/debug/app-debug.apk`

## Build release APK

```bash
/tmp/simple-light-build/gradle-8.7/bin/gradle --no-daemon assembleRelease
```

The release output from Gradle is unsigned:

`app/build/outputs/apk/release/app-release-unsigned.apk`

The release APK currently delivered to the user is manually aligned and signed as:

`app/build/outputs/apk/release/simple-light-keyboard-release.apk`

## Release signing

The local development keystore is outside the repository:

`/tmp/simple-light-build/simple-light-release.keystore`

Do not commit the keystore or its password. If the keystore is available, align and
sign the release APK with Android Build Tools:

```bash
SDK=/tmp/simple-light-build/android-sdk
KEYSTORE=/tmp/simple-light-build/simple-light-release.keystore

$SDK/build-tools/35.0.0/zipalign -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  /tmp/simple-light-build/app-release-aligned.apk

$SDK/build-tools/35.0.0/apksigner sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:<local-keystore-password> \
  --key-pass pass:<local-keystore-password> \
  --out app/build/outputs/apk/release/simple-light-keyboard-release.apk \
  /tmp/simple-light-build/app-release-aligned.apk

$SDK/build-tools/35.0.0/apksigner verify \
  app/build/outputs/apk/release/simple-light-keyboard-release.apk
```

Remove `local.properties` after building so the machine-specific SDK path is not
stored in the repository.

## Install/use

Install:

`app/build/outputs/apk/release/simple-light-keyboard-release.apk`

Then open the app, choose **Open keyboard settings**, enable **Simple Light Keyboard**,
and select it from a text field.
