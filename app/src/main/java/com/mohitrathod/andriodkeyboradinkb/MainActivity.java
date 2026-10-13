package com.mohitrathod.andriodkeyboradinkb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "settings";
    private static final String KEY_THEME = "theme";
    private static final int THEME_SYSTEM = 0;
    private static final int THEME_LIGHT = 1;
    private static final int THEME_DARK = 2;

    private UserDictionary userDictionary;
    private TextView dictCountView;
    private EditText dictInput;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(applyTheme(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userDictionary = UserDictionary.get(this);
        userDictionary.ensureLoaded(() -> runOnUiThread(this::updateDictCount));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);
        root.setBackgroundColor(getColor(R.color.main_background));

        TextView title = new TextView(this);
        title.setText(R.string.setup_title);
        title.setTextColor(getColor(R.color.title_text));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText(R.string.setup_message);
        message.setTextColor(getColor(R.color.message_text));
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 20, 0, 28);

        Button enable = new Button(this);
        enable.setText(R.string.enable_keyboard);
        enable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        root.addView(message, new LinearLayout.LayoutParams(-1, -2));
        root.addView(enable, new LinearLayout.LayoutParams(-1, -2));

        TextView themeLabel = new TextView(this);
        themeLabel.setText(R.string.theme_label);
        themeLabel.setTextColor(getColor(R.color.title_text));
        themeLabel.setTextSize(18);
        themeLabel.setPadding(0, 28, 0, 8);

        RadioGroup themeGroup = new RadioGroup(this);
        themeGroup.setOrientation(RadioGroup.HORIZONTAL);
        int savedTheme = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_THEME, THEME_SYSTEM);
        addThemeOption(themeGroup, getString(R.string.theme_system), THEME_SYSTEM, savedTheme);
        addThemeOption(themeGroup, getString(R.string.theme_light), THEME_LIGHT, savedTheme);
        addThemeOption(themeGroup, getString(R.string.theme_dark), THEME_DARK, savedTheme);
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt(KEY_THEME, checkedId).apply();
            recreate();
        });

        root.addView(themeLabel, new LinearLayout.LayoutParams(-1, -2));
        root.addView(themeGroup, new LinearLayout.LayoutParams(-1, -2));

        addDictionarySection(root);

        Button contactDeveloper = new Button(this);
        contactDeveloper.setText(R.string.contact_developer);
        contactDeveloper.setOnClickListener(v -> startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.contact_developer_url)))));
        LinearLayout.LayoutParams contactParams = new LinearLayout.LayoutParams(-1, -2);
        contactParams.topMargin = 28;
        root.addView(contactDeveloper, contactParams);

        setContentView(root);
    }

    private void addDictionarySection(LinearLayout root) {
        TextView dictLabel = new TextView(this);
        dictLabel.setText(R.string.dictionary_label);
        dictLabel.setTextColor(getColor(R.color.title_text));
        dictLabel.setTextSize(18);
        dictLabel.setPadding(0, 28, 0, 8);

        dictCountView = new TextView(this);
        dictCountView.setTextColor(getColor(R.color.message_text));
        dictCountView.setTextSize(14);
        dictCountView.setPadding(0, 0, 0, 8);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        dictInput = new EditText(this);
        dictInput.setHint(R.string.dictionary_hint);
        dictInput.setTextColor(getColor(R.color.title_text));
        dictInput.setHintTextColor(getColor(R.color.hintColor));
        dictInput.setTextSize(16);
        dictInput.setBackgroundResource(android.R.drawable.edit_text);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1f);
        inputParams.setMargins(0, 0, 8, 0);
        inputRow.addView(dictInput, inputParams);

        Button addBtn = new Button(this);
        addBtn.setText(R.string.dictionary_add);
        addBtn.setOnClickListener(v -> addCustomWord());
        inputRow.addView(addBtn, new LinearLayout.LayoutParams(-2, -2));

        Button clearBtn = new Button(this);
        clearBtn.setText(R.string.dictionary_clear);
        clearBtn.setOnClickListener(v -> clearCustomWords());
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, -2);
        clearParams.topMargin = 8;
        clearParams.bottomMargin = 8;

        root.addView(dictLabel, new LinearLayout.LayoutParams(-1, -2));
        root.addView(dictCountView, new LinearLayout.LayoutParams(-1, -2));
        root.addView(inputRow, new LinearLayout.LayoutParams(-1, -2));
        root.addView(clearBtn, clearParams);
    }

    private void addCustomWord() {
        String word = dictInput.getText().toString().trim().toLowerCase();
        if (word.isEmpty() || word.length() < 2 || !word.matches("[a-z']+")) {
            Toast.makeText(this, R.string.dictionary_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        userDictionary.addManual(word);
        dictInput.setText("");
        updateDictCount();
        Toast.makeText(this, R.string.dictionary_added, Toast.LENGTH_SHORT).show();
    }

    private void clearCustomWords() {
        userDictionary.clear();
        updateDictCount();
        Toast.makeText(this, R.string.dictionary_cleared, Toast.LENGTH_SHORT).show();
    }

    private void updateDictCount() {
        if (dictCountView != null && userDictionary.isLoaded()) {
            dictCountView.setText(getString(R.string.dictionary_count, userDictionary.size()));
        }
    }

    private Context applyTheme(Context base) {
        int theme = base.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_THEME, THEME_SYSTEM);
        if (theme == THEME_SYSTEM) {
            return base;
        }
        Configuration config = new Configuration(base.getResources().getConfiguration());
        int night = theme == THEME_DARK
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        return base.createConfigurationContext(config);
    }

    private void addThemeOption(RadioGroup group, String label, int id, int saved) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(label);
        button.setTextColor(getColor(R.color.title_text));
        button.setChecked(id == saved);
        button.setPadding(16, 8, 16, 8);
        group.addView(button, new RadioGroup.LayoutParams(-2, -2));
    }
}