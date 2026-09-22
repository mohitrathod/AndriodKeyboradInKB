package com.mohitrathod.andriodkeyboradinkb;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class SimpleKeyboardService extends InputMethodService {
    private SimpleKeyboardView keyboardView;

    @Override
    public View onCreateInputView() {
        if (keyboardView != null) {
            keyboardView.stopRepeatingDelete();
        }
        keyboardView = new SimpleKeyboardView(this, isDarkTheme(),
                isHighlightEnabled(), this::handleKey);
        return keyboardView;
    }

    private boolean isHighlightEnabled() {
        return getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("key_press_highlight", true);
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        if (keyboardView != null) {
            keyboardView.setHighlightEnabled(isHighlightEnabled());
        }
    }

    private boolean isDarkTheme() {
        int theme = getSharedPreferences("settings", MODE_PRIVATE).getInt("theme", 0);
        if (theme == 2) {
            return true;
        }
        if (theme == 1) {
            return false;
        }
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private void handleKey(String key) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }

        switch (key) {
            case "BACKSPACE":
                // Let the editor handle Backspace, including selected text.
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                break;
            case "ENTER":
                connection.sendKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER));
                connection.sendKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_UP,
                        android.view.KeyEvent.KEYCODE_ENTER));
                break;
            case "SPACE":
                connection.commitText(" ", 1);
                break;
            default:
                connection.commitText(key, 1);
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (keyboardView != null && !restarting) {
            keyboardView.resetShift();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (keyboardView != null) {
            keyboardView.stopRepeatingDelete();
        }
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onFinishInput() {
        if (keyboardView != null) {
            keyboardView.stopRepeatingDelete();
        }
        super.onFinishInput();
    }
}
