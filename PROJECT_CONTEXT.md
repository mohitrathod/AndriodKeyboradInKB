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
  - Setup screen with button opening Android input-method settings, theme selector, and custom word management.
- `app/src/main/java/com/mohitrathod/andriodkeyboradinkb/SimpleKeyboardService.java`
  - Android `InputMethodService`; commits text, handles enter, space, delete; loads dictionaries, learns words, provides autocomplete suggestions.
- `app/src/main/java/com/mohitrathod/andriodkeyboradinkb/SimpleKeyboardView.java`
  - Programmatic keyboard UI, key behavior, and suggestion strip.
- `app/src/main/java/com/mohitrathod/andriodkeyboradinkb/WordDictionary.java`
  - Loads bundled frequency word list from assets; prefix lookup with frequency ranking.
- `app/src/main/java/com/mohitrathod/andriodkeyboradinkb/UserDictionary.java`
  - Personal dictionary persisted to app-private storage; tracks usage counts, supports manual add/remove, and TSV import/export.
- `app/src/main/AndroidManifest.xml`
  - Declares the launcher activity and input-method service.
- `app/src/main/assets/words.txt`
  - Bundled word frequency list (MIT license, derived from hermitdave/FrequencyWords).

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
- **Suggestion strip** above keys when not on symbols page; appears only when the current word prefix has 4+ letters, showing up to 3 completions.
- **Autocomplete**: merges personal dictionary (active words) with bundled list; only words of 4+ letters are stored or suggested; tap to replace current word.
- **Learning**: words of 4+ letters typed twice are added to personal dictionary; manual add is immediate and also requires 4+ letters.
- **Import/Export**: from the app, export the personal dictionary to a TSV file (`word<TAB>count<TAB>auto|manual`) and import one back via the system file picker (Storage Access Framework, no storage permission). Import **merges**: existing words keep their count/manual flag, only new words are added.

## Build environment

The repository intentionally does not contain `local.properties`. In this environment,
the Android SDK is installed at:

`/usr/lib/android-sdk`

Build with the project Gradle wrapper (Gradle 8.7). The system `gradle` on PATH is too
old and must not be used.

Before building, create a temporary `local.properties` file:

```bash
printf 'sdk.dir=/usr/lib/android-sdk\n' > local.properties
```

Use Java 17:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

## Build debug APK

```bash
./gradlew --no-daemon assembleDebug
```

Output:

`app/build/outputs/apk/debug/app-debug.apk`

## Build release APK

```bash
./gradlew --no-daemon assembleRelease
```

The release output from Gradle is unsigned:

`app/build/outputs/apk/release/app-release-unsigned.apk`

An unsigned APK cannot be installed (Android reports "package invalid"). Align and
sign it before installing.

## Release signing

Release APKs are signed with a private release keystore kept outside the repository
(so the password is never committed):

- Keystore: `/root/dev/projects/AndriodKeyboradInKB-release-20260922.jks`
- Key alias: `andriod-kbrd`
- Store/key password: read from
  `/root/dev/projects/AndriodKeyboradInKB-release-20260922.pass`
  (single line, mode 600; not in this repository)
- Signer certificate SHA-256:
  `d872fbe45fdd6b9c5538224e70898e4f20b184298c5620570b8c5ed34578dc45`

```bash
SDK=/usr/lib/android-sdk
BT=$SDK/build-tools/34.0.0
KS=/root/dev/projects/AndriodKeyboradInKB-release-20260922.jks
PASSFILE=/root/dev/projects/AndriodKeyboradInKB-release-20260922.pass

$BT/zipalign -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  /tmp/opencode/sign/aligned.apk

$BT/apksigner sign \
  --ks "$KS" \
  --ks-key-alias andriod-kbrd \
  --ks-pass "file:$PASSFILE" \
  --out app/build/outputs/apk/release/andriod-keyboard-release.apk \
  /tmp/opencode/sign/aligned.apk

$BT/apksigner verify --print-certs \
  app/build/outputs/apk/release/andriod-keyboard-release.apk
```

Omit `--key-pass`: the one-line password file can only be consumed once. The debug
keystore (`~/.android/debug.keystore`) produces a signature mismatch ("package
conflict") and cannot update an existing release install, so use it only for
throwaway local tests.

Remove `local.properties` after building so the machine-specific SDK path is not
stored in the repository.

## Install/use

Install:

`app/build/outputs/apk/release/andriod-keyboard-release.apk`

Then open the app, choose **Open keyboard settings**, enable **Andriod Keyborad In KB**,
and select it from a text field. Manage custom words in the app under **Custom words**.