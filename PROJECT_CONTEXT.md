# Project Context

## Project

- Name: Andriod Keyborad In KB
- Location: `/mnt/c/dev/projects/AndriodKeyboradInKB`
- Package/application ID: `com.mohitrathod.andriodkeyboradinkb`
- Language: Java
- Android minimum SDK: 23
- Android target/compile SDK: 35
- Gradle plugin: Android Gradle Plugin `8.6.1`
- Gradle used in this environment: `8.7`
- Java used for builds: `/usr/lib/jvm/java-17-openjdk-amd64`

## Important source files

- `app/src/main/java/com/mohitrathod/andriodkeyboradinkb/MainActivity.java`
  - Setup screen with a button opening Android input-method settings.
- `app/src/main/java/com/mohitrathod/andriodkeyboradinkb/SimpleKeyboardService.java`
  - Android `InputMethodService`; commits text, handles enter, space, and delete.
- `app/src/main/java/com/mohitrathod/andriodkeyboradinkb/SimpleKeyboardView.java`
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

`/root/dev/software/android-sdk`

Gradle is installed at:

`/root/dev/software/gradle-8.7/bin/gradle`

Before building, create a temporary `local.properties` file:

```bash
printf 'sdk.dir=/root/dev/software/android-sdk\n' > local.properties
```

Use Java 17:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

## Build debug APK

```bash
ANDROID_HOME=/root/dev/software/android-sdk \
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
/root/dev/software/gradle-8.7/bin/gradle --no-daemon :app:assembleDebug
```

Output:

`app/build/outputs/apk/debug/app-debug.apk`

## Build release APK

```bash
ANDROID_HOME=/root/dev/software/android-sdk \
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
/root/dev/software/gradle-8.7/bin/gradle --no-daemon :app:assembleRelease
```

The release output from Gradle is unsigned:

`app/build/outputs/apk/release/app-release-unsigned.apk`

The release APK currently delivered to the user is manually aligned and signed as:

`app/build/outputs/apk/release/andriod-keyboard-release.apk`

## Release signing

The release keystore is kept outside the repository. Do not commit the keystore or its
password. If the keystore is available, align and sign the release APK with Android
Build Tools:

```bash
SDK=/root/dev/software/android-sdk
KEYSTORE=<path-to-keystore>

$SDK/build-tools/35.0.0/zipalign -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  /tmp/andriod-app-release-aligned.apk

$SDK/build-tools/35.0.0/apksigner sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:<keystore-password> \
  --key-pass pass:<key-password> \
  --out app/build/outputs/apk/release/andriod-keyboard-release.apk \
  /tmp/andriod-app-release-aligned.apk

$SDK/build-tools/35.0.0/apksigner verify \
  app/build/outputs/apk/release/andriod-keyboard-release.apk
```

Remove `local.properties` after building so the machine-specific SDK path is not
stored in the repository.

## Install/use

Install:

`app/build/outputs/apk/debug/app-debug.apk`

Then open the app, choose **Open keyboard settings**, enable **Andriod Keyborad In KB**,
and select it from a text field.
