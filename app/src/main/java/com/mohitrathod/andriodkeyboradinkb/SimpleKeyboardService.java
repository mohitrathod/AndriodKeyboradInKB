package com.mohitrathod.andriodkeyboradinkb;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleKeyboardService extends InputMethodService {
    private static final String TAG = "SimpleKeyboardService";
    private static final int MAX_SUGGESTIONS = 3;
    private static final int WORD_LOOKBACK = 30;

    private SimpleKeyboardView keyboardView;
    private WordDictionary wordDictionary;
    private UserDictionary userDictionary;
    private String lastCommittedWord = "";

    @Override
    public void onCreate() {
        super.onCreate();
        wordDictionary = new WordDictionary();
        userDictionary = UserDictionary.get(this);
        wordDictionary.loadAsync(this, () -> {
            userDictionary.ensureLoaded(() -> Log.d(TAG, "Dictionaries loaded"));
        });
    }

    @Override
    public View onCreateInputView() {
        keyboardView = new SimpleKeyboardView(this, isDarkTheme(), this::handleKey);
        keyboardView.setSuggestionListener(this::onSuggestionSelected);
        return keyboardView;
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
                connection.deleteSurroundingText(1, 0);
                break;
            case "ENTER":
                learnCurrentWord();
                connection.sendKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER));
                connection.sendKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_UP,
                        android.view.KeyEvent.KEYCODE_ENTER));
                lastCommittedWord = "";
                break;
            case "SPACE":
                learnCurrentWord();
                connection.commitText(" ", 1);
                lastCommittedWord = "";
                break;
            default:
                if (key.length() == 1) {
                    char c = key.charAt(0);
                    if (Character.isLetter(c) || c == '\'') {
                        lastCommittedWord += Character.toLowerCase(c);
                    } else {
                        lastCommittedWord = "";
                    }
                } else {
                    lastCommittedWord = "";
                }
                connection.commitText(key, 1);
        }
        updateSuggestions();
    }

    private void onSuggestionSelected(String word) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return;

        String currentWord = getCurrentWord(connection);
        if (!currentWord.isEmpty()) {
            connection.deleteSurroundingText(currentWord.length(), 0);
        }
        connection.commitText(word + " ", 1);
        lastCommittedWord = "";
        updateSuggestions();
    }

    private void learnCurrentWord() {
        if (!wordDictionary.isReady()) return;
        if (lastCommittedWord.length() >= 2 && lastCommittedWord.matches("[a-z']+")) {
            String word = lastCommittedWord.toLowerCase();
            if (!wordDictionary.isKnown(word) && !userDictionary.contains(word)) {
                userDictionary.recordUse(word);
            }
        }
    }

    private String getCurrentWord(InputConnection connection) {
        CharSequence textBefore = connection.getTextBeforeCursor(WORD_LOOKBACK, 0);
        if (textBefore == null) return "";
        String before = textBefore.toString();
        int end = before.length();
        while (end > 0) {
            char c = before.charAt(end - 1);
            if (Character.isLetter(c) || c == '\'') {
                end--;
            } else {
                break;
            }
        }
        return before.substring(end).toLowerCase();
    }

    private void updateSuggestions() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null || keyboardView == null) return;

        String currentWord = getCurrentWord(connection);
        if (currentWord.isEmpty()) {
            keyboardView.setSuggestions(Collections.emptyList());
            return;
        }

        List<String> suggestions = new ArrayList<>();

        if (userDictionary.isLoaded()) {
            suggestions.addAll(userDictionary.suggest(currentWord, MAX_SUGGESTIONS));
        }

        if (wordDictionary.isReady() && suggestions.size() < MAX_SUGGESTIONS) {
            int remaining = MAX_SUGGESTIONS - suggestions.size();
            List<String> dictSuggestions = wordDictionary.suggest(currentWord, remaining);
            for (String s : dictSuggestions) {
                if (!suggestions.contains(s)) {
                    suggestions.add(s);
                }
            }
        }

        keyboardView.setSuggestions(suggestions);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (keyboardView != null) {
            keyboardView.resetShift();
        }
        lastCommittedWord = "";
        updateSuggestions();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        learnCurrentWord();
        lastCommittedWord = "";
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        updateSuggestions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wordDictionary != null) wordDictionary.shutdown();
    }
}