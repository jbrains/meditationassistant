package sh.ftp.rocketninelabs.meditationassistant;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
    private static final boolean FORCE_TABLET_VIEW = false; // Useful when debugging
    static int FILEPICKER_SELECT_SOUND_START = 101;
    static int FILEPICKER_SELECT_SOUND_INTERVAL = 102;
    static int FILEPICKER_SELECT_SOUND_FINISH = 103;
    static int FILEPICKER_IMPORT_SESSIONS_UTC = 104;
    static int FILEPICKER_IMPORT_SESSIONS_LOCAL = 105;
    static int FILEPICKER_EXPORT_SESSIONS = 106;
    private static final int PERMISSION_REQUEST_SOUND_READ_EXTERNAL_STORAGE = 3002;
    private static final int PERMISSION_REQUEST_IMPORT_READ_EXTERNAL_STORAGE = 3003;
    private static final int PERMISSION_REQUEST_EXPORT_WRITE_EXTERNAL_STORAGE = 3004;
    public Boolean initialTimePickerChange = true;
    public Boolean initialMainButtonsChange = true;
    public Boolean initialSoundChangeStart = true;
    public Boolean initialSoundChangeInterval = true;
    public Boolean initialSoundChangeFinish = true;
    public SessionPreferenceFragment sessionPreferenceFragment = null;
    public ReminderPreferenceFragment reminderPreferenceFragment = null;
    public MeditationPreferenceFragment meditationPreferenceFragment = null;
    public ProgressPreferenceFragment progressPreferenceFragment = null;
    public MediNETPreferenceFragment medinetPreferenceFragment = null;
    public MiscellaneousPreferenceFragment miscellaneousPreferenceFragment = null;
    private int selectingPrefsound = 0;
    private MeditationAssistant ma = null;
    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof CheckBoxPreference) {
                if (preference.getKey().equals("pref_daily_reminder")) {
                    Intent intent = new Intent();
                    intent.setAction(MeditationAssistant.ACTION_UPDATED);
                    sendBroadcast(intent);
                } else if (preference.getKey().equals("pref_usetimepicker")) {
                    if (!initialTimePickerChange) {
                        Toast.makeText(SettingsActivity.this, getString(R.string.restartApp), Toast.LENGTH_SHORT).show();
                    }
                    initialTimePickerChange = false;
                }

                return true;
            } else if (preference instanceof ColorPickerPreference) {
                AppWidgetManager wm = AppWidgetManager.getInstance(getApplicationContext());
                int[] ids = wm.getAppWidgetIds(new ComponentName(getApplicationContext(), WidgetPresetProvider.class));
                ids = mergeInts(ids, wm.getAppWidgetIds(new ComponentName(getApplicationContext(), WidgetPresetProvider1.class)));
                ids = mergeInts(ids, wm.getAppWidgetIds(new ComponentName(getApplicationContext(), WidgetPresetProvider2.class)));
                ids = mergeInts(ids, wm.getAppWidgetIds(new ComponentName(getApplicationContext(), WidgetPresetProvider3.class)));

                for (int widgetId : ids) {
                    WidgetPresetProvider.updateAppWidget(getApplicationContext(), wm, widgetId);
                }

                Intent intent2 = new Intent(getApplicationContext(), WidgetStreakProvider2.class);
                intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids2 = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), WidgetStreakProvider2.class));
                intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids2);
                sendBroadcast(intent2);

                Intent intent3 = new Intent(getApplicationContext(), WidgetStreakProvider3.class);
                intent3.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids3 = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), WidgetStreakProvider3.class));
                intent3.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids3);
                sendBroadcast(intent3);

                return true;
            }

            String stringValue = "";
            if (!preference.getKey().equals("pref_presetsettings")) {
                stringValue = value.toString();
            }

            if (preference instanceof MultiSelectListPreference) {
                if (preference.getKey().equals("pref_mainbuttons")) {
                    if (!initialMainButtonsChange) {
                        Toast.makeText(SettingsActivity.this, getString(R.string.restartApp), Toast.LENGTH_SHORT).show();
                    }
                    initialMainButtonsChange = false;
                }

                HashSet<String> presetSettings = (HashSet<String>) value;
                if (presetSettings.size() == 0) {
                    preference.setSummary(getString(preference.getKey().equals("pref_presetsettings") ? R.string.disabled : R.string.none));
                } else {
                    List<String> presetsettings = Arrays.asList(getResources().getStringArray(preference.getKey().equals("pref_presetsettings") ? R.array.presetsettings : R.array.mainbuttons));
                    List<String> presetsettings_values = Arrays.asList(getResources().getStringArray(preference.getKey().equals("pref_presetsettings") ? R.array.presetsettings_values : R.array.mainbuttons_values));

                    String presetsummary = "";

                    for (Iterator<String> preseti = presetsettings_values.iterator(); preseti.hasNext(); ) {
                        String preset = preseti.next();
                        if (presetSettings.contains(preset)) {
                            if (presetsummary != "") {
                                presetsummary += ", ";
                            }
                            presetsummary += presetsettings.get(presetsettings_values.indexOf(preset));
                        }
                    }

                    preference.setSummary(presetsummary);
                }
            } else if (preference instanceof ListPreference || preference instanceof ListPreferenceSound) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : "Gong"
                ); // TODO: Don't hardcode sound names

                if (listPreference.getKey().equals("pref_theme")) {
                    if (!getMeditationAssistant().getPrefs().getString(listPreference.getKey(), "dark").equals(stringValue)) {
                        Toast.makeText(SettingsActivity.this, getString(R.string.restartApp), Toast.LENGTH_SHORT).show();
                    }
                } else if (listPreference.getKey().equals("pref_notificationcontrol")) {
                    getMeditationAssistant().checkNotificationControl(SettingsActivity.this, stringValue);
                } else if (listPreference.getKey().equals("pref_meditation_sound_start")) {
                    if (stringValue.equals("custom")) {
                        if (!initialSoundChangeStart) {
                            selectCustomSound(FILEPICKER_SELECT_SOUND_START);
                        }

                        preference.setSummary(customSoundSummary(getMeditationAssistant().getPrefs().getString("pref_meditation_sound_start_custom", "")));
                    }
                    initialSoundChangeStart = false;
                } else if (listPreference.getKey().equals("pref_meditation_sound_interval")) {
                    if (stringValue.equals("custom")) {
                        if (!initialSoundChangeInterval) {
                            selectCustomSound(FILEPICKER_SELECT_SOUND_INTERVAL);
                        }

                        preference.setSummary(customSoundSummary(getMeditationAssistant().getPrefs().getString("pref_meditation_sound_interval_custom", "")));
                    }
                    initialSoundChangeInterval = false;
                } else if (listPreference.getKey().equals("pref_meditation_sound_finish")) {
                    if (stringValue.equals("custom")) {
                        if (!initialSoundChangeFinish) {
                            selectCustomSound(FILEPICKER_SELECT_SOUND_FINISH);
                        }

                        preference.setSummary(customSoundSummary(getMeditationAssistant().getPrefs().getString("pref_meditation_sound_finish_custom", "")));
                    }
                    initialSoundChangeFinish = false;
                }
            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    //preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }
            } else if (preference instanceof TimePreference) {
                if (preference.getKey().equals("pref_daily_reminder_time") || preference.getKey().equals("pref_meditationstreakbuffer")) {
                    String timeValue = "";
                    try {
                        String[] timeValueSplit = ((stringValue != null && stringValue != "") ? stringValue : (preference.getKey().equals("pref_daily_reminder_time") ? "19:00" : "4:00")).split(":");

                        String ampm = "AM";
                        if (Integer.valueOf(timeValueSplit[0]) >= 12) {
                            timeValueSplit[0] = String.valueOf(Integer.valueOf(timeValueSplit[0]) - 12);
                            ampm = "PM";
                        }
                        if (Integer.valueOf(timeValueSplit[0]) == 0) {
                            timeValueSplit[0] = "12";
                        }

                        timeValue = String.valueOf(Integer.valueOf(timeValueSplit[0])) + ":"
                                + String.format("%02d", Integer.valueOf(timeValueSplit[1])) + " " + ampm;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    preference.setSummary(timeValue);

                    if (preference.getKey().equals("pref_daily_reminder_time")) {
                        Intent intent = new Intent();
                        intent.setAction(MeditationAssistant.ACTION_UPDATED);
                        sendBroadcast(intent);
                    }
                } else { // pref_session_delay and pref_session_interval
                    Log.d("MeditationAssistant", preference.getKey() + " value: " + String.valueOf(stringValue));

                    String timeValue = "";
                    Boolean isDisabled = false;
                    try {
                        String[] timeValueSplit = ((stringValue != null && stringValue != "") ? stringValue : (preference.getKey().equals("pref_session_delay") ? "00:15" : "00:00")).split(":");
                        timeValue = String.valueOf((int) Math.floor(Integer.valueOf(timeValueSplit[0]) / 60)) + ":"
                                + String.format("%02d", Integer.valueOf(timeValueSplit[0]) % 60) + ":"
                                + String.format("%02d", Integer.valueOf(timeValueSplit[1]));
                        isDisabled = (Integer.valueOf(timeValueSplit[0]) == 0 && Integer.valueOf(timeValueSplit[1]) == 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    preference.setSummary(isDisabled ? getString(R.string.disabled) : timeValue);

                    if (preference.getKey().equals("pref_session_interval")) {
                        ListPreferenceSound prefIntervalSound = (ListPreferenceSound) (sessionPreferenceFragment == null ? findPreference("pref_meditation_sound_interval") : sessionPreferenceFragment.findPreference("pref_meditation_sound_interval"));
                        prefIntervalSound.setEnabled(!isDisabled);

                        EditTextPreference prefIntervalCount = (EditTextPreference) (sessionPreferenceFragment == null ? findPreference("pref_interval_count") : sessionPreferenceFragment.findPreference("pref_interval_count"));
                        prefIntervalCount.setEnabled(!isDisabled);
                    }
                }
            } else if (preference instanceof EditTextPreference) {
                String reminderText = getString(preference.getKey().equals("pref_daily_reminder_text") ? R.string.reminderText : R.string.ignore_introphrase);
                if (stringValue != null && (preference.getKey().equals("pref_sessionintro") || !String.valueOf(stringValue).trim().equals(""))) {
                    reminderText = String.valueOf(stringValue).trim();
                }
                preference.setSummary(reminderText);
            } else if (preference instanceof SeekBarPreference) {
                if (stringValue == null || stringValue.equals("")) {
                    stringValue = "50";
                }
                preference.setSummary(String.valueOf(((Integer.valueOf(stringValue) + 4) / 5 * 5)) + "%");
            } else if (preference.getKey().equals("pref_interval_count")) {
                if (stringValue == null || String.valueOf(stringValue).trim().equals("")) {
                    stringValue = "0";
                }
                if (Integer.valueOf(stringValue) <= 0) {
                    preference.setSummary(getString(R.string.unlimited));
                } else {
                    preference.setSummary(getResources().getQuantityString(
                            R.plurals.numtimes, Integer.valueOf(stringValue),
                            String.valueOf(Integer.valueOf(stringValue))
                    ));
                }
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            return true;
        }
    };

    private void selectCustomSound(int requestCode) {
        selectingPrefsound = requestCode;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_SOUND_READ_EXTERNAL_STORAGE);
        } else {
            getMeditationAssistant().showFilePickerDialog(SettingsActivity.this, requestCode, FilePickerActivity.MODE_FILE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_SOUND_READ_EXTERNAL_STORAGE: {
                if ((grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getMeditationAssistant().showFilePickerDialog(SettingsActivity.this, selectingPrefsound, FilePickerActivity.MODE_FILE);
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setIcon(
                            getResources()
                                    .getDrawable(
                                            getTheme()
                                                    .obtainStyledAttributes(
                                                            getMeditationAssistant()
                                                                    .getMATheme(true),
                                                            new int[]{R.attr.actionIconSettings}
                                                    )
                                                    .getResourceId(0, 0)
                                    )
                    )
                            .setTitle(getString(R.string.permissionRequest))
                            .setMessage(
                                    getString(R.string.permissionRequired))
                            .setPositiveButton(getString(R.string.tryAgain),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            selectCustomSound(selectingPrefsound);
                                        }
                                    })
                            .setNegativeButton(getString(R.string.deny),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    }).show();
                    break;
                }
            }
            case PERMISSION_REQUEST_IMPORT_READ_EXTERNAL_STORAGE: {
                if ((grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getMeditationAssistant().showImportSessionsDialog(SettingsActivity.this);
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setIcon(
                            getResources()
                                    .getDrawable(
                                            getTheme()
                                                    .obtainStyledAttributes(
                                                            getMeditationAssistant()
                                                                    .getMATheme(true),
                                                            new int[]{R.attr.actionIconSettings}
                                                    )
                                                    .getResourceId(0, 0)
                                    )
                    )
                            .setTitle(getString(R.string.permissionRequest))
                            .setMessage(
                                    getString(R.string.permissionRequired))
                            .setPositiveButton(getString(R.string.tryAgain),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            ActivityCompat.requestPermissions(SettingsActivity.this,
                                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                    PERMISSION_REQUEST_IMPORT_READ_EXTERNAL_STORAGE);
                                        }
                                    })
                            .setNegativeButton(getString(R.string.deny),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    }).show();
                    break;
                }
            }
            case PERMISSION_REQUEST_EXPORT_WRITE_EXTERNAL_STORAGE: {
                if ((grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getMeditationAssistant().showFilePickerDialog(SettingsActivity.this, FILEPICKER_EXPORT_SESSIONS, FilePickerActivity.MODE_NEW_FILE);
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setIcon(
                            getResources()
                                    .getDrawable(
                                            getTheme()
                                                    .obtainStyledAttributes(
                                                            getMeditationAssistant()
                                                                    .getMATheme(true),
                                                            new int[]{R.attr.actionIconSettings}
                                                    )
                                                    .getResourceId(0, 0)
                                    )
                    )
                            .setTitle(getString(R.string.permissionRequest))
                            .setMessage(
                                    getString(R.string.permissionRequired))
                            .setPositiveButton(getString(R.string.tryAgain),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            ActivityCompat.requestPermissions(SettingsActivity.this,
                                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    PERMISSION_REQUEST_EXPORT_WRITE_EXTERNAL_STORAGE);
                                        }
                                    })
                            .setNegativeButton(getString(R.string.deny),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    }).show();
                    break;
                }
            }
        }
    }

    private Long uploadsessions_lastlick = (long) 0;
    private Long downloadsessions_lastlick = (long) 0;

    private static boolean isXLargeTablet(Context context) {
        return FORCE_TABLET_VIEW || ((context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isSimplePreferences(Context context) {
        return !isXLargeTablet(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(getMeditationAssistant().getMATheme());
        getMeditationAssistant().utility.initializeTracker(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        getMeditationAssistant().utility.trackingStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getMeditationAssistant().utility.trackingStop(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == FILEPICKER_IMPORT_SESSIONS_UTC || requestCode == FILEPICKER_IMPORT_SESSIONS_LOCAL) {
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);
            for (Uri uri : files) {
                getMeditationAssistant().importSessions(SettingsActivity.this, uri, requestCode == FILEPICKER_IMPORT_SESSIONS_LOCAL);
            }

            return;
        } else if (requestCode == FILEPICKER_EXPORT_SESSIONS) {
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);
            for (Uri uri : files) {
                getMeditationAssistant().exportSessions(SettingsActivity.this, uri);
            }
            return;
        }

        String pref;
        String pref_key;
        if (requestCode == FILEPICKER_SELECT_SOUND_START) {
            pref_key = "pref_meditation_sound_start";
            pref = "pref_meditation_sound_start_custom";
        } else if (requestCode == FILEPICKER_SELECT_SOUND_INTERVAL) {
            pref_key = "pref_meditation_sound_interval";
            pref = "pref_meditation_sound_interval_custom";
        } else if (requestCode == FILEPICKER_SELECT_SOUND_FINISH) {
            pref_key = "pref_meditation_sound_finish";
            pref = "pref_meditation_sound_finish_custom";
        } else {
            return;
        }

        List<Uri> files = Utils.getSelectedFilesFromResult(intent);
        for (Uri uri : files) {
            getMeditationAssistant().getPrefs().edit().putString(pref, uri.toString()).apply();
        }

        if (requestCode == FILEPICKER_SELECT_SOUND_START) {
            initialSoundChangeStart = true;
        } else if (requestCode == FILEPICKER_SELECT_SOUND_INTERVAL) {
            initialSoundChangeInterval = true;
        } else if (requestCode == FILEPICKER_SELECT_SOUND_FINISH) {
            initialSoundChangeFinish = true;
        }

        ListPreferenceSound prefMeditationSound = (ListPreferenceSound) (sessionPreferenceFragment == null ? findPreference(pref_key) : sessionPreferenceFragment.findPreference(pref_key));
        prefMeditationSound.getOnPreferenceChangeListener().onPreferenceChange(prefMeditationSound, getMeditationAssistant().getPrefs().getString(pref_key, "gong"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        int i = item.getItemId();
        if (i == R.id.action_about) {
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (i == R.id.action_accountsettings) {
            Intent openActivity = new Intent(this,
                    MediNETActivity.class);
            openActivity.putExtra("page", "account");
            startActivity(openActivity);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    void setupSoundPreferences(PreferenceFragment preferenceFragment) {
        String[] meditation_sounds = getResources().getStringArray(R.array.meditation_sounds);
        String[] meditation_sounds_values = getResources().getStringArray(R.array.meditation_sounds_values);

        ListPreferenceSound prefMeditationSoundStart = (ListPreferenceSound) (preferenceFragment == null ? findPreference("pref_meditation_sound_start") : preferenceFragment.findPreference("pref_meditation_sound_start"));
        prefMeditationSoundStart.setEntries(meditation_sounds);
        prefMeditationSoundStart.setEntryValues(meditation_sounds_values);

        ListPreferenceSound prefMeditationSoundInterval = (ListPreferenceSound) (preferenceFragment == null ? findPreference("pref_meditation_sound_interval") : preferenceFragment.findPreference("pref_meditation_sound_interval"));
        prefMeditationSoundInterval.setEntries(meditation_sounds);
        prefMeditationSoundInterval.setEntryValues(meditation_sounds_values);

        ListPreferenceSound prefMeditationSoundFinish = (ListPreferenceSound) (preferenceFragment == null ? findPreference("pref_meditation_sound_finish") : preferenceFragment.findPreference("pref_meditation_sound_finish"));
        prefMeditationSoundFinish.setEntries(meditation_sounds);
        prefMeditationSoundFinish.setEntryValues(meditation_sounds_values);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // Remove priority/alarms entries
            ListPreference pref_notificationcontrol = (ListPreference) (preferenceFragment == null ? findPreference("pref_notificationcontrol") : preferenceFragment.findPreference("pref_notificationcontrol"));
            pref_notificationcontrol.setEntries(R.array.notificationcontrol_premarshmallow);
            pref_notificationcontrol.setEntryValues(R.array.notificationcontrol_values_premarshmallow);
        }
    }

    void setupPreferences(String pref_type, PreferenceFragment preferenceFragment) {
        if (pref_type.equals("all") || pref_type.equals("session")) {
            if (preferenceFragment != null) {
                sessionPreferenceFragment = (SessionPreferenceFragment) preferenceFragment;
            }

            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_sessionintro") : preferenceFragment.findPreference("pref_sessionintro"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_session_delay") : preferenceFragment.findPreference("pref_session_delay"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_meditation_sound_start") : preferenceFragment.findPreference("pref_meditation_sound_start"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_meditation_sound_interval") : preferenceFragment.findPreference("pref_meditation_sound_interval"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_session_interval") : preferenceFragment.findPreference("pref_session_interval"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_interval_count") : preferenceFragment.findPreference("pref_interval_count"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_meditation_sound_finish") : preferenceFragment.findPreference("pref_meditation_sound_finish"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_notificationcontrol") : preferenceFragment.findPreference("pref_notificationcontrol"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_sessionvolume") : preferenceFragment.findPreference("pref_sessionvolume"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_presetsettings") : preferenceFragment.findPreference("pref_presetsettings"));
        }
        if (pref_type.equals("all") || pref_type.equals("reminder")) {
            if (preferenceFragment != null) {
                reminderPreferenceFragment = (ReminderPreferenceFragment) preferenceFragment;
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_daily_reminder_text") : preferenceFragment.findPreference("pref_daily_reminder_text"));
                bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_daily_reminder_time") : preferenceFragment.findPreference("pref_daily_reminder_time"));
                bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_daily_reminder") : preferenceFragment.findPreference("pref_daily_reminder"));
            }
        }
        if (pref_type.equals("all") || pref_type.equals("meditation")) {
            if (preferenceFragment != null) {
                meditationPreferenceFragment = (MeditationPreferenceFragment) preferenceFragment;
            }

            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_usetimepicker") : preferenceFragment.findPreference("pref_usetimepicker"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_screencontrol") : preferenceFragment.findPreference("pref_screencontrol"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_full_screen") : preferenceFragment.findPreference("pref_full_screen"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_text_size") : preferenceFragment.findPreference("pref_text_size"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_autosave") : preferenceFragment.findPreference("pref_autosave"));
        }
        if (pref_type.equals("all") || pref_type.equals("progress")) {
            if (preferenceFragment != null) {
                progressPreferenceFragment = (ProgressPreferenceFragment) preferenceFragment;
            }

            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_meditationstreakbuffer") : preferenceFragment.findPreference("pref_meditationstreakbuffer"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_progresstab") : preferenceFragment.findPreference("pref_progresstab"));

            Preference importSessions = (preferenceFragment == null ? findPreference("importsessions") : preferenceFragment.findPreference("importsessions"));
            importSessions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (ContextCompat.checkSelfPermission(SettingsActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(SettingsActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST_IMPORT_READ_EXTERNAL_STORAGE);
                    } else {
                        getMeditationAssistant().showImportSessionsDialog(SettingsActivity.this);
                    }
                    return false;
                }
            });

            Preference exportSessions = (preferenceFragment == null ? findPreference("exportsessions") : preferenceFragment.findPreference("exportsessions"));
            exportSessions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (ContextCompat.checkSelfPermission(SettingsActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(SettingsActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST_EXPORT_WRITE_EXTERNAL_STORAGE);
                    } else {
                        getMeditationAssistant().showFilePickerDialog(SettingsActivity.this, FILEPICKER_EXPORT_SESSIONS, FilePickerActivity.MODE_NEW_FILE);
                    }
                    return false;
                }
            });

        }
        if (pref_type.equals("all") || pref_type.equals("medinet")) {
            if (preferenceFragment != null) {
                medinetPreferenceFragment = (MediNETPreferenceFragment) preferenceFragment;
            }

            Preference uploadSessions = (preferenceFragment == null ? findPreference("uploadsessions") : preferenceFragment.findPreference("uploadsessions"));
            uploadSessions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {

                    if (getMeditationAssistant().getMediNETKey().equals("")) {
                        getMeditationAssistant().startAuth(SettingsActivity.this, true);
                    } else {
                        if (getMeditationAssistant().db.getNumSessions() == 0) {
                            getMeditationAssistant().longToast(
                                    getMeditationAssistant().getString(R.string.sessionsUpToDate));

                            return false;
                        }

                        if (getMeditationAssistant().getTimestamp()
                                - uploadsessions_lastlick > 5) {
                            uploadsessions_lastlick = getMeditationAssistant()
                                    .getTimestamp();
                            getMeditationAssistant().getMediNET()
                                    .uploadSessions();
                        }
                    }

                    return false;
                }
            });

            Preference downloadSessions = (preferenceFragment == null ? findPreference("downloadsessions") : preferenceFragment.findPreference("downloadsessions"));
            downloadSessions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (getMeditationAssistant().getMediNETKey().equals("")) {
                        getMeditationAssistant().startAuth(SettingsActivity.this, true);
                    } else {
                        if (getMeditationAssistant().getTimestamp()
                                - downloadsessions_lastlick > 5) {
                            downloadsessions_lastlick = getMeditationAssistant()
                                    .getTimestamp();
                            getMeditationAssistant().getMediNET()
                                    .downloadSessions();
                        }
                    }

                    return false;
                }
            });
        }
        if (pref_type.equals("all") || pref_type.equals("miscellaneous")) {
            if (preferenceFragment != null) {
                miscellaneousPreferenceFragment = (MiscellaneousPreferenceFragment) preferenceFragment;
            }

            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_theme") : preferenceFragment.findPreference("pref_theme"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_widgetcolor") : preferenceFragment.findPreference("pref_widgetcolor"));
            bindPreferenceSummaryToValue(preferenceFragment == null ? findPreference("pref_mainbuttons") : preferenceFragment.findPreference("pref_mainbuttons"));

            if (BuildConfig.FLAVOR.equals("opensource")) { // Hide usage statistics preference, as tracking is completely disabled
                CheckBoxPreference pref_sendusage = (CheckBoxPreference) (preferenceFragment == null ? findPreference("pref_sendusage") : preferenceFragment.findPreference("pref_sendusage"));
                (preferenceFragment == null ? getPreferenceScreen() : preferenceFragment.getPreferenceScreen()).removePreference(pref_sendusage);
            }
        }
    }

    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'Session' preferences.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        addPreferencesFromResource(R.xml.pref_session);

        // Add 'Daily Reminder' preferences
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            fakeHeader = new PreferenceCategory(this);
            fakeHeader.setTitle(R.string.pref_daily_reminder);
            getPreferenceScreen().addPreference(fakeHeader);
            addPreferencesFromResource(R.xml.pref_reminder);
        }

        // Add 'Meditation' preferences
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.meditation);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_meditation);

        // Add 'Progress' preferences
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.progress);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_progress);

        // Add 'MediNET' preferences
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.mediNET);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_medinet);

        // Add 'Miscellaneous' preferences
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.miscellaneous);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_miscellaneous);

        setupSoundPreferences(null);
        setupPreferences("all", null);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                loadHeadersFromResource(R.xml.pref_headers_pre26, target);
            }
            loadHeadersFromResource(R.xml.pref_headers_footer, target);
        }
    }

    private String customSoundSummary(String meditation_sound) {
        if (meditation_sound == null || meditation_sound.equals("")) {
            return getString(R.string.noSound);
        }

        try {
            return java.net.URLDecoder.decode(meditation_sound.substring(meditation_sound.lastIndexOf("/") + 1), "UTF-8");
        } catch (Exception e) {
            return meditation_sound;
        }
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        if (preference.getKey().equals("pref_widgetcolor")) {
            return;
        }

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference.getKey().equals("pref_daily_reminder") || preference.getKey().equals("pref_usetimepicker")) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    getMeditationAssistant().getPrefs()
                            .getBoolean(preference.getKey(), false)
            );
        } else if (preference.getKey().equals("pref_presetsettings")) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, getMeditationAssistant().getPrefs().getStringSet("pref_presetsettings", new HashSet<String>(Arrays.asList(getResources().getStringArray(R.array.presetsettings_default))))
            );
        } else if (preference.getKey().equals("pref_mainbuttons")) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, getMeditationAssistant().getPrefs().getStringSet("pref_mainbuttons", new HashSet<String>()));
        } else if (preference.getKey().equals("pref_sessionvolume")) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, getMeditationAssistant().getPrefs().getInt("pref_sessionvolume", 50));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    getMeditationAssistant().getPrefs()
                            .getString(preference.getKey(), "")
            );
        }
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SessionPreferenceFragment.class.getName().equals(fragmentName) ||
                ReminderPreferenceFragment.class.getName().equals(fragmentName) ||
                MeditationPreferenceFragment.class.getName().equals(fragmentName) ||
                ProgressPreferenceFragment.class.getName().equals(fragmentName) ||
                MediNETPreferenceFragment.class.getName().equals(fragmentName) ||
                MiscellaneousPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SessionPreferenceFragment extends PreferenceFragment {
        public SessionPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_session);
            PreferenceCategory prefCatSession = (PreferenceCategory) findPreference("pref_cat_session");
            getPreferenceScreen().removePreference(prefCatSession);

            SettingsActivity settingsactivity = (SettingsActivity) getActivity();
            settingsactivity.setupSoundPreferences(this);
            settingsactivity.setupPreferences("session", this);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ReminderPreferenceFragment extends PreferenceFragment {
        public ReminderPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_reminder);

            SettingsActivity settingsactivity = (SettingsActivity) getActivity();
            settingsactivity.setupPreferences("reminder", this);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MeditationPreferenceFragment extends PreferenceFragment {
        public MeditationPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_meditation);

            SettingsActivity settingsactivity = (SettingsActivity) getActivity();
            settingsactivity.setupPreferences("meditation", this);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ProgressPreferenceFragment extends PreferenceFragment {
        public ProgressPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_progress);

            SettingsActivity settingsactivity = (SettingsActivity) getActivity();
            settingsactivity.setupPreferences("progress", this);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MediNETPreferenceFragment extends PreferenceFragment {
        public MediNETPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_medinet);

            SettingsActivity settingsactivity = (SettingsActivity) getActivity();
            settingsactivity.setupPreferences("medinet", this);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MiscellaneousPreferenceFragment extends PreferenceFragment {
        public MiscellaneousPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_miscellaneous);

            SettingsActivity settingsactivity = (SettingsActivity) getActivity();
            settingsactivity.setupPreferences("miscellaneous", this);
        }
    }

    private int[] mergeInts(int[] arg1, int[] arg2) {
        int[] result = new int[arg1.length + arg2.length];
        System.arraycopy(arg1, 0, result, 0, arg1.length);
        System.arraycopy(arg2, 0, result, arg1.length, arg2.length);
        return result;
    }
}
