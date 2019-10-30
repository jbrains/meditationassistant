package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class PresetWidgetActivity extends Activity {
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText mAppWidgetText;
    private static final String PREF_PREFIX_KEY = "widgetpreset";

    public PresetWidgetActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.widget_preset_configure);
        setTitle(R.string.configureWidget);

        MeditationAssistant ma = (MeditationAssistant) getApplicationContext();

        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mAppWidgetText = (EditText) findViewById(R.id.presetWidgetLabel);
        final ListView listView = (ListView) findViewById(R.id.presetWidgetList);

        List<String> values = new ArrayList<String>();
        for (int i = 1; i < 4; i++) {
            String presetLabel = ma.getPrefs().getString("pref_preset_" + i + "_label", "");
            if (!presetLabel.isEmpty()) {
                values.add(presetLabel);
            } else {
                values.add("Preset " + i);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int preset = position + 1;

                String widgetText = mAppWidgetText.getText().toString();
                if (widgetText.isEmpty()) {
                    widgetText = ma.getPrefs().getString("pref_preset_" + preset + "_label", "");
                }

                createWidget(getApplicationContext(), preset, widgetText);
            }
        });

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d("MA", "configuring preset widget " + mAppWidgetId);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mAppWidgetText.setText(loadTitlePref(PresetWidgetActivity.this, mAppWidgetId));
    }

    private void createWidget(Context context, int preset, String widgetText) {
        savePresetPref(context, mAppWidgetId, preset);
        saveTitlePref(context, mAppWidgetId, widgetText);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        WidgetPresetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    static void saveTitlePref(Context context, int appWidgetId, String text) {
        MeditationAssistant ma = (MeditationAssistant) context.getApplicationContext();

        SharedPreferences.Editor prefs = ma.getPrefs().edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + "title", text);
        prefs.apply();
    }

    static String loadTitlePref(Context context, int appWidgetId) {
        MeditationAssistant ma = (MeditationAssistant) context.getApplicationContext();

        SharedPreferences prefs = ma.getPrefs();
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "title", null);
        if (titleValue != null) {
            return titleValue;
        }

        int preset = loadPresetPref(context, appWidgetId);
        if (preset >= 0) {
            return prefs.getString("pref_preset_" + preset + "_label", "");
        }

        return "";
    }

    static void savePresetPref(Context context, int appWidgetId, int preset) {
        MeditationAssistant ma = (MeditationAssistant) context.getApplicationContext();

        SharedPreferences.Editor prefs = ma.getPrefs().edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "preset", preset);
        prefs.apply();
    }

    static int loadPresetPref(Context context, int appWidgetId) {
        MeditationAssistant ma = (MeditationAssistant) context.getApplicationContext();

        SharedPreferences prefs = ma.getPrefs();
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "preset", -1);
    }

    static void deleteWidgetPrefs(Context context, int appWidgetId) {
        MeditationAssistant ma = (MeditationAssistant) context.getApplicationContext();

        SharedPreferences.Editor prefs = ma.getPrefs().edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "title");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "preset");
        prefs.apply();
    }
}
