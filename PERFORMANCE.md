# Keyboard performance

## Implementation review

- Letter taps send text directly to the current `InputConnection`, with no disk,
  network, or surrounding-text reads in the key handler. Uppercase key values
  are prepared when buttons are created, rather than converted on every tap.
- Shift changes existing button labels rather than rebuilding the keyboard.
- Letters and symbols pages are created once per keyboard view and reused.
  The symbols page is created lazily on its first use; this retains a second
  small view tree after use in exchange for avoiding repeated button creation.
- Starting input in the default letter state does not rebuild the layout.
- Delete repetition is cancelled on touch release/cancel, page switches, input
  changes, keyboard dismissal, and view detachment.
- Press feedback uses an immediate solid pressed-state highlight with zero
  fade duration and no ripple or elevation animation. The setting disables it.
- Delete fires on touch-down and repeats every 75 ms after the system long-press
  delay. It sends Android Backspace events so the editor handles selections;
  release does not trigger an extra delete. A same-editor input restart preserves
  Shift and pending repeat, while starting a new input session resets them.

These changes reduce avoidable UI work; they are not measured latency results.
Android scheduling and the receiving editor also affect visible response time.

## Local validation

```bash
ANDROID_HOME=/root/dev/software/android-sdk \
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
```

Build and lint passed on 2026-09-22 (0 lint errors, 11 warnings). No device was
listed by `adb devices -l`, so runtime testing remains pending.

## Device checks

Use the same device, refresh rate, editor, and build type for before/after
comparisons. Test a native text field and a browser text field.

1. Type rapidly for at least a minute, including alternating fingers, spaces,
   punctuation, and Enter. Check for missing, duplicated, or reordered input.
2. Tap Shift once, then type `ii`: expect `Ii` and lowercase labels after the
   first character. Tap Shift twice before typing `ii`: expect `II` and a `CAPS`
   label. Tap Shift again to unlock. Repeat with Turkish as the device locale.
   One-shot Shift also clears after digits, punctuation, Space, or Enter;
   Backspace preserves it. Switching pages or fields clears Caps Lock as well.
3. Switch letter/symbol pages repeatedly. Measure the first switch separately
   from later cached switches; check labels and emitted characters agree.
4. Change text fields from shifted and symbol states. Confirm the keyboard
   returns to lowercase letters, including after rotation and reopening.
5. Hold Delete, release, and verify deletion stops. Repeat while dismissing the
   keyboard or changing fields: no delayed deletion should reach the new field.
   Verify a quick tap removes exactly one character, selected text is deleted,
   and holding Delete continues in native and browser editors. Dragging outside
   Delete must stop repetition. Check emoji and empty text fields too.
6. Check keyboard height, navigation-bar insets, and both themes after reopening
   and rotation.
7. Check that the press highlight appears immediately without a spreading ripple
   or fade, and that disabling it suppresses feedback on both keyboard pages.

Capture an Android Studio System Trace (or Perfetto trace) while doing these
checks. Inspect the IME main thread, input dispatch, frame deadlines, and GC.
After warm-up, Shift and page switching should not allocate new keyboard
buttons. Aim for UI work within one display frame: 16.7 ms at 60 Hz, 8.3 ms at
120 Hz. These are frame budgets, not a guarantee of end-to-end input latency.

Measure key-release-to-visible-character latency separately using an input /
rendering trace or high-frame-rate video; report median, p95, and worst case,
along with device model, Android version, refresh rate, and editor. This
keyboard commits normal taps on release through standard Android button clicks.
