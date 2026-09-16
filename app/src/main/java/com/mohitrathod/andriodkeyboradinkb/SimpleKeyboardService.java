package com.mohitrathod.andriodkeyboradinkb;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class SimpleKeyboardService extends InputMethodService {
    private SimpleKeyboardView keyboardView;

    @Override
    public View onCreateInputView() {
        keyboardView = new SimpleKeyboardView(this, this::handleKey);
        return keyboardView;
    }

    private void handleKey(String key) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }

        switch (key) {
            case "BACKSPACE":
                connection.deleteSurroundingText(1, 0);
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
        if (keyboardView != null) {
            keyboardView.resetShift();
        }
    }
}
