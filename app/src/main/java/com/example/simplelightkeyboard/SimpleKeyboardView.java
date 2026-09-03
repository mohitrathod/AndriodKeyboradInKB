package com.example.simplelightkeyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.os.Handler;
import android.os.Looper;

public class SimpleKeyboardView extends LinearLayout {
    private static final int BACKGROUND = Color.rgb(232, 232, 240);
    private static final int TEXT = Color.rgb(24, 53, 105);
    private static final int HINT = Color.rgb(78, 82, 101);
    interface KeyListener {
        void onKey(String key);
    }

    private final KeyListener keyListener;
    private boolean shifted;
    private boolean symbols;

    public SimpleKeyboardView(Context context, KeyListener keyListener) {
        super(context);
        this.keyListener = keyListener;
        setOrientation(VERTICAL);
        setPadding(8, 2, 8, 32);
        setBackgroundColor(BACKGROUND);
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

    public void resetShift() {
        shifted = false;
        symbols = false;
        buildKeyboard();
    }

    private void buildKeyboard() {
        removeAllViews();
        if (symbols) {
            addRow(new String[]{"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"}, null);
            addRow(new String[]{"-", "_", "=", "+", "[", "]", "{", "}"}, null);
            addRow(new String[]{";", ":", "'", "\"", ",", ".", "?", "/"}, null);
            addRow(new String[]{"ABC", "\\", "|", "~", "`", "BACKSPACE"}, null);
            addBottomRow();
        } else {
            addRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}, null);
            addRow(new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"}, null);
            addRow(new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"}, null);
            addRow(new String[]{"SHIFT", "z", "x", "c", "v", "b", "n", "m", "BACKSPACE"}, null);
            addBottomRow();
        }
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
        addKey(row, ",", null, 0.8f);
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
            styled.setSpan(new ForegroundColorSpan(HINT), 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        button.setTextColor(TEXT);
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
}
