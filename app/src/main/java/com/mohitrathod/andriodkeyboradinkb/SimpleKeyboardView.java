package com.mohitrathod.andriodkeyboradinkb;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.os.Handler;
import android.os.Looper;
import java.util.Collections;
import java.util.List;

public class SimpleKeyboardView extends LinearLayout {
    interface KeyListener {
        void onKey(String key);
    }

    interface SuggestionListener {
        void onSuggestion(String word);
    }

    private final KeyListener keyListener;
    private final int backgroundColor;
    private final int textColor;
    private final int hintColor;
    private boolean shifted;
    private boolean symbols;
    private List<String> suggestions = Collections.emptyList();
    private SuggestionListener suggestionListener;
    private LinearLayout suggestionsRow;

    public SimpleKeyboardView(Context context, boolean dark, KeyListener keyListener) {
        super(context);
        this.keyListener = keyListener;
        if (dark) {
            this.backgroundColor = 0xFF1B1C23;
            this.textColor = 0xFFE8E8F0;
            this.hintColor = 0xFF8E92A8;
        } else {
            this.backgroundColor = 0xFFE8E8F0;
            this.textColor = 0xFF183569;
            this.hintColor = 0xFF4E5265;
        }
        setOrientation(VERTICAL);
        setPadding(8, 2, 8, 32);
        setBackgroundColor(backgroundColor);
        setOnApplyWindowInsetsListener((view, insets) -> {
            int bottomInset = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else {
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            setPadding(8, 2, 8, Math.max(32, bottomInset + 8));
            return insets;
        });
        buildKeyboard();
    }

    public void setSuggestionListener(SuggestionListener listener) {
        this.suggestionListener = listener;
    }

    public void setSuggestions(List<String> suggestions) {
        List<String> next = suggestions == null ? Collections.emptyList() : suggestions;
        if (next.equals(this.suggestions)) {
            return;
        }
        this.suggestions = next;
        renderSuggestions();
    }

    public void resetShift() {
        shifted = false;
        symbols = false;
        suggestions = Collections.emptyList();
        buildKeyboard();
    }

    private void buildKeyboard() {
        removeAllViews();
        suggestionsRow = null;
        if (!symbols) {
            suggestionsRow = new LinearLayout(getContext());
            suggestionsRow.setGravity(Gravity.CENTER);
            suggestionsRow.setPadding(0, 4, 0, 4);
            addView(suggestionsRow, new LayoutParams(-1, dpToPx(40), 0f));
            renderSuggestions();

            addRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}, null);
            addRow(new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"}, null);
            addRow(new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"}, null);
            addRow(new String[]{"SHIFT", "z", "x", "c", "v", "b", "n", "m", "BACKSPACE"}, null);
            addBottomRow();
        } else {
            addRow(new String[]{"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"}, null);
            addRow(new String[]{"-", "_", "=", "+", "[", "]", "{", "}"}, null);
            addRow(new String[]{";", ":", "'", "\"", ",", ".", "?", "/"}, null);
            addRow(new String[]{"ABC", "\\", "|", "~", "`", "BACKSPACE"}, null);
            addBottomRow();
        }
    }

    private void renderSuggestions() {
        if (suggestionsRow == null) return;
        suggestionsRow.removeAllViews();
        suggestionsRow.setVisibility(VISIBLE);
        int count = Math.min(suggestions.size(), 3);
        if (count == 0) {
            return;
        }
        float weight = 1f / count;
        for (int i = 0; i < count; i++) {
            addSuggestionKey(suggestionsRow, suggestions.get(i), weight);
        }
    }

    private void addSuggestionKey(LinearLayout row, String word, float weight) {
        Button button = new Button(getContext());
        button.setText(word);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(8, 0, 8, 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(v -> {
            if (suggestionListener != null) {
                suggestionListener.onSuggestion(word);
            }
        });
        LayoutParams params = new LayoutParams(0, -1, weight);
        params.setMargins(2, 0, 2, 0);
        row.addView(button, params);
    }

    private void addRow(String[] row, String[] hints) {
        LinearLayout container = new LinearLayout(getContext());
        container.setGravity(Gravity.CENTER);
        container.setPadding(0, 0, 0, 0);
        for (int i = 0; i < row.length; i++) {
            addKey(container, row[i], hints == null ? null : hints[i], 1f);
        }
        addView(container, new LayoutParams(-1, 0, 1f));
    }

    private void addBottomRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER);
        addKey(row, symbols ? "123" : "#+=", null, 1.2f);
        addKey(row, "?", null, 0.8f);
        addKey(row, "SPACE", null, 3.2f);
        addKey(row, ".", null, 0.8f);
        addKey(row, "ENTER", null, 1.2f);
        addView(row, new LayoutParams(-1, 0, 1f));
    }

    private void addKey(LinearLayout row, String key, String hint, float weight) {
        Button button = new Button(getContext());
        String label = key;
        if (key.equals("#+=") || key.equals("123")) {
            label = "?123";
        }
        if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
            label = shifted ? key.toUpperCase() : key.toLowerCase();
        } else if (key.equals("SHIFT")) {
            label = shifted ? "SHIFT ON" : "SHIFT";
        } else if (key.equals("BACKSPACE")) {
            label = "DEL";
        } else if (key.equals("ENTER")) {
            label = "ENTER";
        }
        if (hint != null && !hint.isEmpty()) {
            SpannableString styled = new SpannableString(hint + "\n" + label);
            styled.setSpan(new RelativeSizeSpan(0.52f), 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new ForegroundColorSpan(hintColor), 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new RelativeSizeSpan(1.15f), hint.length() + 1, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(styled);
        } else {
            button.setText(label);
        }
        float textSize = 18;
        if (key.equals("SPACE")) {
            textSize = 13;
        } else if (key.equals("SHIFT") || key.equals("BACKSPACE") || key.equals("ENTER") ||
                key.equals("#+=") || key.equals("123") || key.equals("ABC")) {
            textSize = 12;
        }
        button.setTextSize(textSize);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        if (key.equals("BACKSPACE")) {
            Handler deleteHandler = new Handler(Looper.getMainLooper());
            Runnable repeatDelete = new Runnable() {
                @Override
                public void run() {
                    keyListener.onKey("BACKSPACE");
                    deleteHandler.postDelayed(this, 75);
                }
            };
            button.setOnLongClickListener(v -> {
                keyListener.onKey("BACKSPACE");
                deleteHandler.postDelayed(repeatDelete, 75);
                return true;
            });
            button.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    deleteHandler.removeCallbacks(repeatDelete);
                }
                return false;
            });
        }
        button.setOnClickListener(v -> {
            if (key.equals("SHIFT")) {
                shifted = !shifted;
                buildKeyboard();
            } else if (key.equals("#+=") || key.equals("ABC") || key.equals("123")) {
                symbols = key.equals("#+=");
                shifted = false;
                suggestions = Collections.emptyList();
                buildKeyboard();
            } else {
                keyListener.onKey(key.equals("SPACE") ? "SPACE" :
                        (key.length() == 1 && Character.isLetter(key.charAt(0))
                                ? (shifted ? key.toUpperCase() : key.toLowerCase()) : key));
            }
        });
        LayoutParams params = new LayoutParams(0, -1, weight);
        params.setMargins(1, 0, 1, 0);
        row.addView(button, params);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}