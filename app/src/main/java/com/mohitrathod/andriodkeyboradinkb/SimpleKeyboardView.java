package com.mohitrathod.andriodkeyboradinkb;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Locale;

public class SimpleKeyboardView extends LinearLayout {
    interface KeyListener {
        void onKey(String key);
    }

    private final KeyListener keyListener;
    private final int backgroundColor;
    private final int textColor;
    private final int hintColor;
    private final int pressedColor;
    private boolean shifted;
    private boolean capsLocked;
    private boolean symbols;
    private final ArrayList<Button> letterButtons = new ArrayList<>();
    private final ArrayList<Button> questionButtons = new ArrayList<>();
    private final ArrayList<Button> allButtons = new ArrayList<>();
    private boolean highlightEnabled;
    private Button shiftButton;
    private LinearLayout lettersPage;
    private LinearLayout symbolsPage;
    private LinearLayout activePage;
    private final Handler deleteHandler = new Handler(Looper.getMainLooper());
    private boolean repeatingDelete;
    private Button pressedDeleteButton;
    private final Runnable repeatDelete = new Runnable() {
        @Override
        public void run() {
            if (repeatingDelete) {
                keyListener.onKey("BACKSPACE");
                if (repeatingDelete) {
                    deleteHandler.postDelayed(this, 75);
                }
            }
        }
    };

    public SimpleKeyboardView(Context context, boolean dark, boolean highlightEnabled,
            KeyListener keyListener) {
        super(context);
        this.keyListener = keyListener;
        this.highlightEnabled = highlightEnabled;
        if (dark) {
            this.backgroundColor = 0xFF1B1C23;
            this.textColor = 0xFFE8E8F0;
            this.hintColor = 0xFF8E92A8;
            this.pressedColor = 0x33FFFFFF;
        } else {
            this.backgroundColor = 0xFFE8E8F0;
            this.textColor = 0xFF183569;
            this.hintColor = 0xFF4E5265;
            this.pressedColor = 0x26000000;
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

    public void setHighlightEnabled(boolean enabled) {
        if (highlightEnabled == enabled) {
            return;
        }
        highlightEnabled = enabled;
        for (Button button : allButtons) {
            applyKeyBackground(button);
        }
    }

    private void applyKeyBackground(Button button) {
        if (highlightEnabled) {
            // Instant pressed state: no ripple, fade, timer, or per-tap allocation.
            StateListDrawable background = new StateListDrawable();
            background.addState(new int[]{android.R.attr.state_pressed},
                    new ColorDrawable(pressedColor));
            background.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
            background.setEnterFadeDuration(0);
            background.setExitFadeDuration(0);
            button.setBackground(background);
        } else {
            button.setBackground(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public void resetShift() {
        stopRepeatingDelete();
        boolean wasShifted = shifted;
        shifted = false;
        capsLocked = false;
        if (wasShifted) {
            updateShiftLabels();
        }
        if (!symbols) {
            return;
        }
        symbols = false;
        buildKeyboard();
    }

    public void stopRepeatingDelete() {
        repeatingDelete = false;
        deleteHandler.removeCallbacks(repeatDelete);
        if (pressedDeleteButton != null) {
            pressedDeleteButton.setPressed(false);
            pressedDeleteButton = null;
        }
    }

    private final class DeleteButton extends Button {
        DeleteButton(Context context) {
            super(context);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    stopRepeatingDelete();
                    pressedDeleteButton = this;
                    repeatingDelete = true;
                    setPressed(true);
                    // Delete immediately, then repeat after the usual hold delay.
                    performClick();
                    if (repeatingDelete) {
                        deleteHandler.postDelayed(repeatDelete,
                                ViewConfiguration.getLongPressTimeout());
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getX() < 0 || event.getX() >= getWidth()
                            || event.getY() < 0 || event.getY() >= getHeight()) {
                        stopRepeatingDelete();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopRepeatingDelete();
                    // Already clicked on DOWN; do not delete twice on a tap.
                    return true;
                default:
                    return true;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopRepeatingDelete();
        super.onDetachedFromWindow();
    }

    private void updateShiftLabels() {
        for (Button button : letterButtons) {
            String key = (String) button.getTag();
            button.setText(shifted ? key.toUpperCase(Locale.ROOT) : key);
        }
        for (Button button : questionButtons) {
            button.setText(shifted ? "." : "?");
        }
        if (shiftButton != null) {
            shiftButton.setText(getShiftLabel());
        }
    }

    private String getShiftLabel() {
        return capsLocked ? "CAPS" : (shifted ? "SHIFT ON" : "SHIFT");
    }

    private void buildKeyboard() {
        stopRepeatingDelete();
        removeAllViews();
        activePage = symbols ? symbolsPage : lettersPage;
        if (activePage != null) {
            addView(activePage, new LayoutParams(-1, -1));
            return;
        }
        // Build each page once; subsequent switches reuse its buttons and listeners.
        activePage = new LinearLayout(getContext());
        activePage.setOrientation(VERTICAL);
        if (symbols) {
            symbolsPage = activePage;
        } else {
            lettersPage = activePage;
        }
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
        addView(activePage, new LayoutParams(-1, -1));
    }

    private void addRow(String[] row, String[] hints) {
        LinearLayout container = new LinearLayout(getContext());
        container.setGravity(Gravity.CENTER);
        container.setPadding(0, 0, 0, 0);
        for (int i = 0; i < row.length; i++) {
            addKey(container, row[i], hints == null ? null : hints[i], 1f);
        }
        activePage.addView(container, new LayoutParams(-1, 0, 1f));
    }

    private void addBottomRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER);
        addKey(row, symbols ? "123" : "#+=", null, 1.2f);
        addKey(row, ",", null, 0.8f);
        addKey(row, "SPACE", null, 3.2f);
        addKey(row, "?", null, 0.8f);
        addKey(row, "ENTER", null, 1.2f);
        activePage.addView(row, new LayoutParams(-1, 0, 1f));
    }

    private void addKey(LinearLayout row, String key, String hint, float weight) {
        Button button = key.equals("BACKSPACE")
                ? new DeleteButton(getContext()) : new Button(getContext());
        button.setStateListAnimator(null);
        final boolean letter = key.length() == 1 && Character.isLetter(key.charAt(0));
        final String uppercase = letter ? key.toUpperCase(Locale.ROOT) : key;
        String label = key;
        if (key.equals("#+=") || key.equals("123")) {
            label = "?123";
        }
        if (letter) {
            label = shifted ? uppercase : key;
            button.setTag(key);
            letterButtons.add(button);
        } else if (key.equals("SHIFT")) {
            shiftButton = button;
            label = getShiftLabel();
        } else if (key.equals("?")) {
            questionButtons.add(button);
            label = shifted ? "." : "?";
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
        allButtons.add(button);
        applyKeyBackground(button);
        button.setOnClickListener(v -> {
            if (key.equals("SHIFT")) {
                // First tap: one character. Second tap: lock. Third tap: off.
                if (capsLocked) {
                    capsLocked = false;
                    shifted = false;
                } else if (shifted) {
                    capsLocked = true;
                } else {
                    shifted = true;
                }
                updateShiftLabels();
            } else if (key.equals("#+=") || key.equals("ABC") || key.equals("123")) {
                symbols = key.equals("#+=");
                shifted = false;
                capsLocked = false;
                updateShiftLabels();
                buildKeyboard();
            } else {
                String output = key;
                if (shifted) {
                    if (letter) {
                        output = uppercase;
                    } else if (key.equals("?")) {
                        output = ".";
                    }
                }
                keyListener.onKey(output);
                if (shifted && !capsLocked && !key.equals("BACKSPACE")) {
                    shifted = false;
                    updateShiftLabels();
                }
            }
        });
        LayoutParams params = new LayoutParams(0, -1, weight);
        params.setMargins(1, 0, 1, 0);
        row.addView(button, params);
    }
}
