package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements OnShowcaseEventListener {
    public static String BROADCAST_ACTION_ALARM = "sh.ftp.rocketninelabs.meditationassistant.ALARM";
    public static int ID_DELAY = 77702;
    public static int ID_INTERVAL = 77701;
    public static int ID_END = 77703;

    public MeditationAssistant ma = null;
    SharedPreferences.OnSharedPreferenceChangeListener sharedPrefslistener = (newprefs, key) -> {
        Object newValue = getMeditationAssistant().getPrefs().getAll().get(key);
        Log.d("MeditationAssistant", key + " changed to " + newValue);

        new Handler(Looper.getMainLooper()).post(() -> {
            if ((key.equals("timerHours") || key.equals("timerMinutes")) && getMeditationAssistant().getTimeToStopMeditate() < 1) {
                TextView txtTimer = (TextView) findViewById(R.id.txtTimer);
                txtTimer.setText(getMeditationAssistant().getPrefs().getString("timerHours", "0")
                        + ":"
                        + String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs().getString(
                        "timerMinutes", "15"))));
            } else if (key.equals("medinetupdate")
                    || key.equals("pref_meditation_sound_finish")
                    || key.equals("pref_meditation_sound_finish_custom")
                    || key.equals("pref_meditation_sound_start")
                    || key.equals("pref_meditation_sound_start_custom")
                    || key.equals("meditationstreak")) {
                updateTexts();
            } else if (key.equals("pref_text_size")) {
                updateTextSize();
            } else if (key.equals("keyupdate")) {
                if (!getMeditationAssistant().getMediNETKey().equals("")) {
                    getMeditationAssistant().getMediNET().connect();
                }
            }
        });
    };
    private Handler handler;
    private Runnable meditateRunnable = null;
    private Runnable screenDimRunnable = null;
    private Runnable screenOffRunnable = null;
    private AlarmManager am = null;
    private PendingIntent pendingintent = null;
    private PendingIntent pendingintent_delay = null;
    private PendingIntent pendingintent_interval = null;
    private Runnable intervalRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("MeditationAssistant", "Interval running");
            handler.removeCallbacks(this);
            if (getMeditationAssistant().getTimeStartMeditate() > 0) {
                if (getMeditationAssistant().getTimeToStopMeditate() != 0
                        && getMeditationAssistant().getTimeToStopMeditate()
                        - (System.currentTimeMillis() / 1000) < 30) {
                    return; // No interval sounds during the final 30 seconds
                }

                String intervalSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_interval", "");

                if (!intervalSoundPath.equals("none") || getMeditationAssistant().vibrationEnabled()) {
                    if (!intervalSoundPath.equals("none")) {
                        if (intervalSoundPath.equals("custom")) {
                            intervalSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_interval_custom", "");
                            getMeditationAssistant().playSound(0, intervalSoundPath, false);
                        } else {
                            getMeditationAssistant().playSound(MeditationSounds.getMeditationSound(intervalSoundPath), "", false);
                        }
                    }

                    getMeditationAssistant().vibrateDevice();

                    long interval = Math.max(
                            getMeditationAssistant().timePreferenceValueToSeconds(getMeditationAssistant().getPrefs().getString("pref_session_interval", "00:00"), "00:00"), 0);
                    Log.d("MeditationAssistant", "Interval is set to " + String.valueOf(interval) + " seconds");

                    if (interval > 0 && (getMeditationAssistant().getTimeToStopMeditate() == -1
                            || getMeditationAssistant().getTimeToStopMeditate()
                            - (System.currentTimeMillis() / 1000) > (interval + 30) || getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false))) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.SECOND, (int) interval);

                        Log.d("MeditationAssistant", "Setting INTERVAL WAKEUP alarm for "
                                + String.valueOf(cal.getTimeInMillis()) + " (Now: "
                                + System.currentTimeMillis() + ", in: " + String.valueOf((cal.getTimeInMillis() - System.currentTimeMillis()) / 1000) + ")");

                        Intent intent_interval = new Intent(
                                getApplicationContext(), MainActivity.class);
                        intent_interval.putExtra("wakeup", true);
                        intent_interval.putExtra("wakeupinterval", true);
                        intent_interval.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent_interval.setAction(BROADCAST_ACTION_ALARM);
                        pendingintent_interval = PendingIntent.getActivity(
                                getApplicationContext(), ID_INTERVAL,
                                intent_interval, PendingIntent.FLAG_CANCEL_CURRENT);
                        getMeditationAssistant().setAlarm(true, cal.getTimeInMillis(), pendingintent_interval);

                        handler.postDelayed(this, interval * 1000);
                    }
                }
            }
        }
    };
    private String lastKey = "";
    private boolean skipDelay;
    private boolean playedStartSound;
    private String previous_timermode = "timed";
    private Boolean usetimepicker = true;
    private ShowcaseView sv = null;
    private String next_tutorial = "";
    private int intervals = 0;
    private Runnable clearWakeLock = new Runnable() {
        @Override
        public void run() {
            getMeditationAssistant().releaseWakeLock(wakeLockID);
            wakeLockID = null;
        }
    };
    private Boolean finishedTutorial = null;
    private String wakeLockID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(getMeditationAssistant().getMATheme());
        setContentView(R.layout.activity_main);

        getMeditationAssistant().utility.initializeTracker(this);

        handler = new Handler();

        am = getMeditationAssistant().getAlarmManager();

        lastKey = getMeditationAssistant().getPrefs().getString("key", "");

        if (getMeditationAssistant().getMediNET() == null) {
            getMeditationAssistant().setMediNET(new MediNET(this));
            Log.d("MeditationAssistant", "New instance of MediNET created");
        } else {
            getMeditationAssistant().getMediNET().activity = this;
        }

        Set<String> hideMainButtons = getMeditationAssistant().getPrefs().getStringSet("pref_mainbuttons", null);
        if (hideMainButtons != null) {
            Boolean hidProgress = false;
            Boolean hidCommunity = false;

            for (Iterator<String> hidebtni = hideMainButtons.iterator(); hidebtni.hasNext(); ) {
                String hideButton = hidebtni.next();
                if (hideButton.equals("medinet")) {
                    View divStreakUpper = findViewById(R.id.divstreakUpper);
                    Button btnMeditationStreak = (Button) findViewById(R.id.btnMeditationStreak);
                    divStreakUpper.setVisibility(View.GONE);
                    btnMeditationStreak.setVisibility(View.GONE);
                } else if (hideButton.equals("progress")) {
                    Button btnProgress = (Button) findViewById(R.id.btnProgress);
                    btnProgress.setVisibility(View.GONE);

                    hidProgress = true;
                } else if (hideButton.equals("community")) {
                    Button btnCommunity = (Button) findViewById(R.id.btnCommunity);
                    btnCommunity.setVisibility(View.GONE);

                    hidCommunity = true;
                }
            }

            if (hidProgress && hidCommunity) {
                View divMainControls = findViewById(R.id.divMainControls);
                LinearLayout layMainControls = (LinearLayout) findViewById(R.id.layMainControls);
                divMainControls.setVisibility(View.GONE);
                layMainControls.setVisibility(View.GONE);
            } else if (hidProgress || hidCommunity) {
                View divMainControlsInner = findViewById(R.id.divMainControlsInner);
                divMainControlsInner.setVisibility(View.GONE);
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        findViewById(R.id.btnMeditate).setLongClickable(true);
        final View btnMeditate = findViewById(R.id.btnMeditate);
        btnMeditate.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return longPressMeditate(v);
            }
        });

        usetimepicker = getMeditationAssistant().getPrefs().getBoolean("pref_usetimepicker", false);

        final EditText editDuration = (EditText) findViewById(R.id.editDuration);
        final TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);
        timepickerDuration.setIs24HourView(true);
        if (usetimepicker) {
            editDuration.setVisibility(View.GONE);
            timepickerDuration.setVisibility(View.VISIBLE);
        } else {
            timepickerDuration.setVisibility(View.GONE);
            editDuration.setVisibility(View.VISIBLE);
        }

        editDuration
                .setOnEditorActionListener(new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(android.widget.TextView v,
                                                  int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            RelativeLayout layEditDuration = (RelativeLayout) findViewById(R.id.layEditDuration);
                            if (layEditDuration.getVisibility() == View.VISIBLE) {
                                setDuration(null);
                            }

                            return true;
                        }

                        return false;
                    }
                });
        editDuration.setOnKeyListener(new EditText.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    RelativeLayout layEditDuration = (RelativeLayout) findViewById(R.id.layEditDuration);
                    if (layEditDuration.getVisibility() == View.VISIBLE) {
                        setDuration(null);
                    }

                    return true;
                }

                return false;
            }
        });

        Button btnPreset1 = (Button) findViewById(R.id.btnPreset1);
        Button btnPreset2 = (Button) findViewById(R.id.btnPreset2);
        Button btnPreset3 = (Button) findViewById(R.id.btnPreset3);

        View.OnLongClickListener presetListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {
                if (getMeditationAssistant().getTimerMode().equals("endat")) {
                    ArrayList<String> duration_formatted = new ArrayList<String>();
                    if (usetimepicker) {
                        duration_formatted.add(String.valueOf(timepickerDuration.getCurrentHour()));
                        duration_formatted.add(String.format("%02d",
                                timepickerDuration.getCurrentMinute()));
                    } else {
                        duration_formatted = getMeditationAssistant().formatDurationEndAt(editDuration.getText().toString().trim());
                    }

                    if (duration_formatted == null || duration_formatted.size() != 2) {
                        getMeditationAssistant().shortToast(getString(R.string.setPresetHintBlank));
                        return true;
                    }
                } else if (!getMeditationAssistant().getTimerMode().equals("untimed")) {
                    ArrayList<String> duration_formatted = new ArrayList<String>();
                    if (usetimepicker) {
                        duration_formatted.add(String.valueOf(timepickerDuration.getCurrentHour()));
                        duration_formatted.add(String.format("%02d",
                                timepickerDuration.getCurrentMinute()));
                    } else {
                        duration_formatted = getMeditationAssistant().formatDuration(editDuration.getText().toString().trim());
                    }

                    if (duration_formatted == null || duration_formatted.size() != 2) {
                        getMeditationAssistant().shortToast(getString(R.string.setPresetHintBlank));
                        return true;
                    }
                }

                final String preset_key;
                if (view.getId() == R.id.btnPreset1) {
                    preset_key = "pref_preset_1";
                } else if (view.getId() == R.id.btnPreset2) {
                    preset_key = "pref_preset_2";
                } else if (view.getId() == R.id.btnPreset3) {
                    preset_key = "pref_preset_3";
                } else {
                    return false;
                }

                LayoutInflater presetInflater = getLayoutInflater();
                View presetLayout = presetInflater.inflate(R.layout.set_preset, null);
                final EditText editPresetTitle = (EditText) presetLayout.findViewById(R.id.editPresetTitle);
                editPresetTitle.setText(getPresetDefaultLabel());
                editPresetTitle.setSelection(editPresetTitle.getText().length());
                editPresetTitle.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setIcon(
                        getResources()
                                .getDrawable(
                                        getTheme()
                                                .obtainStyledAttributes(
                                                        getMeditationAssistant()
                                                                .getMATheme(),
                                                        new int[]{R.attr.actionIconForward}
                                                )
                                                .getResourceId(0, 0)
                                )
                )
                        .setTitle(getString(R.string.setPreset))
                        .setView(presetLayout)
                        .setPositiveButton(getString(R.string.set),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        getMeditationAssistant().getPrefs().edit().putString(preset_key + "_label", editPresetTitle.getText().toString().trim()).apply();
                                        savePreset(preset_key);
                                        updatePresets();
                                    }
                                })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do nothing
                            }
                        }).show();

                return true;
            }
        };

        btnPreset1.setOnLongClickListener(presetListener);
        btnPreset2.setOnLongClickListener(presetListener);
        btnPreset3.setOnLongClickListener(presetListener);

        updateVisibleViews(false);

        if (getMeditationAssistant().getEditingDuration()) {
            changeDuration(null);
        }

        if (getMeditationAssistant().getPrefs().getBoolean("pref_autosignin", false) && !getMeditationAssistant().getMediNETKey().equals("")) {
            getMeditationAssistant().connectOnce();
        } else {
            getMeditationAssistant().getMediNET().updated();
        }

        onNewIntent(getIntent());

        Object language = Locale.getDefault().getLanguage();
        if (language != null && !language.equals("en") && getMeditationAssistant().getPrefs().getInt("applaunches", 0) >= 5 && !getMeditationAssistant().getPrefs().getBoolean("askedtotranslate", false)) {
            getMeditationAssistant().getPrefs().edit().putBoolean("askedtotranslate", true).apply();

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            startActivity(new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(MeditationAssistant.URL_MEDINET + "/translate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(
                    getResources()
                            .getDrawable(
                                    getTheme()
                                            .obtainStyledAttributes(
                                                    getMeditationAssistant()
                                                            .getMATheme(),
                                                    new int[]{R.attr.actionIconNotImportant}
                                            )
                                            .getResourceId(0, 0)
                            )
            )
                    .setTitle(getString(R.string.translate))
                    .setMessage(
                            getString(R.string.translateMeditationAssistantText))
                    .setPositiveButton(getString(R.string.yes),
                            dialogClickListener)
                    .setNegativeButton(getString(R.string.no),
                            dialogClickListener).show();
        }

        showNextTutorial();
        getMeditationAssistant().recalculateMeditationStreak(MainActivity.this);

        long pref_delay = Integer.valueOf(getMeditationAssistant().getPrefs().getString("pref_delay", "-1"));
        long pref_interval = Integer.valueOf(getMeditationAssistant().getPrefs().getString("pref_interval", "-1"));
        if (pref_delay >= 0 || pref_interval >= 0) {
            getMeditationAssistant().getPrefs().edit().putString("pref_delay", "-1").putString("pref_interval", "-1").apply();

            getMeditationAssistant().getMediNET().announcement = getString(R.string.helpUpgradeDelayInterval);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getMeditationAssistant()
                            .showAnnouncementDialog(getString(R.string.pref_notification));
                }
            });
        }

        getMeditationAssistant().checkNotificationControl(MainActivity.this, "");

        if (getMeditationAssistant().asktodonate) {
            getMeditationAssistant().asktodonate = false;
            getMeditationAssistant().askToDonate(MainActivity.this);
        }

        // TODO: Implement
        //getMeditationAssistant().setupGoogleClient(MainActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (sv != null && sv.isShown()) {
            try {
                sv.hide();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshAll() {
        updateTextSize();
        updateMeditate(false, false);
        updateTexts();
        updatePresets();
        getMeditationAssistant().setRunnableStopped(true);
        startRunnable();
    }

    public void cancelSetDuration(View view) {
        TextView txtTimer = (TextView) findViewById(R.id.txtTimer);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(txtTimer.getWindowToken(), 0);
        getMeditationAssistant().setEditingDuration(false);
        getMeditationAssistant().setTimerMode(previous_timermode);
        changeDuration(null);
        updateVisibleViews(false);
    }

    public void updateEditDuration() {
        if (usetimepicker) {
            TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);

            if (getMeditationAssistant().getTimerMode().equals("timed")) {
                timepickerDuration.setEnabled(true);
                timepickerDuration.setCurrentHour(Integer.valueOf(getMeditationAssistant().getPrefs().getString("timerHours", "0")));
                timepickerDuration.setCurrentMinute(Integer.valueOf(String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs().getString("timerMinutes", "15")))));
            } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
                timepickerDuration.setEnabled(true);
                timepickerDuration.setCurrentHour(Integer.valueOf(getMeditationAssistant().getPrefs().getString("timerHoursEndAt", "0")));
                timepickerDuration.setCurrentMinute(Integer.valueOf(String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs().getString("timerMinutesEndAt", "0")))));
            } else {
                timepickerDuration.setEnabled(false);
            }
        } else {
            EditText editDuration = (EditText) findViewById(R.id.editDuration);

            if (getMeditationAssistant().getTimerMode().equals("timed")) {
                editDuration.setText(getMeditationAssistant().getPrefs().getString("timerHours", "0")
                        + ":"
                        + String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs().getString(
                        "timerMinutes", "15"))));
            } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
                editDuration.setText(getMeditationAssistant().getPrefs().getString("timerHoursEndAt", "0")
                        + ":"
                        + String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs().getString(
                        "timerMinutesEndAt", "0"))));
            } else {
                editDuration.setText(getString(R.string.ignore_om));
            }
        }

        updateMeditate(false, false);
    }

    public void setDuration(View view) {
        if (usetimepicker) {
            TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);

            if (getMeditationAssistant().getTimerMode().equals("timed")) {
                getMeditationAssistant().getPrefs().edit().putString("timerHours", String.valueOf(timepickerDuration.getCurrentHour())).putString("timerMinutes", String.valueOf(timepickerDuration.getCurrentMinute())).apply();
            } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
                getMeditationAssistant().getPrefs().edit().putString("timerHoursEndAt", String.valueOf(timepickerDuration.getCurrentHour())).putString("timerMinutesEndAt", String.valueOf(timepickerDuration.getCurrentMinute())).apply();
            }
        } else {
            EditText editDuration = (EditText) findViewById(R.id.editDuration);

            if (getMeditationAssistant().getTimerMode().equals("timed")) {
                ArrayList<String> duration_formatted = getMeditationAssistant().formatDuration(editDuration.getText().toString().trim());
                if (duration_formatted != null && duration_formatted.size() == 2) {
                    SharedPreferences.Editor editor = getMeditationAssistant().getPrefs().edit();
                    editor.putString("timerHours", duration_formatted.get(0));
                    editor.putString("timerMinutes", duration_formatted.get(1));
                    editor.apply();
                } else {
                    getMeditationAssistant().shortToast(getString(R.string.setTimerDurationHint));
                    return;
                }
            } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
                ArrayList<String> duration_formatted = getMeditationAssistant().formatDurationEndAt(editDuration.getText().toString().trim());
                if (duration_formatted != null && duration_formatted.size() == 2) {
                    SharedPreferences.Editor editor = getMeditationAssistant().getPrefs().edit();
                    editor.putString("timerHoursEndAt", duration_formatted.get(0));
                    editor.putString("timerMinutesEndAt", duration_formatted.get(1));
                    editor.apply();
                } else {
                    getMeditationAssistant().shortToast(getString(R.string.setTimerEndAtHint));
                    return;
                }
            }

            TextView txtTimer = (TextView) findViewById(R.id.txtTimer);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(txtTimer.getWindowToken(), 0);
        }

        getMeditationAssistant().setEditingDuration(false);
        changeDuration(null);
    }

    /* Called when the duration is clicked */
    public void changeDuration(View view) {
        RelativeLayout layEditDuration = (RelativeLayout) findViewById(R.id.layEditDuration);
        TextView txtTimer = (TextView) findViewById(R.id.txtTimer);
        EditText editDuration = (EditText) findViewById(R.id.editDuration);

        if (getMeditationAssistant().getTimeStartMeditate() > 0) {
            return; // Don't switch during a meditation session
        }

        if (getMeditationAssistant().getEditingDuration()) {
            return; // Don't switch back while editing, must use buttons
        }

        if (sv != null && sv.isShown()) {
            try {
                sv.hide();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Boolean wasEditing = false;
        if (txtTimer.getVisibility() == View.GONE) {
            layEditDuration.setVisibility(View.GONE);
            txtTimer.setVisibility(View.VISIBLE);
            getMeditationAssistant().setEditingDuration(false);
            wasEditing = true;
        } else {
            txtTimer.setVisibility(View.GONE);
            layEditDuration.setVisibility(View.VISIBLE);

            if (view != null) {
                getMeditationAssistant().setEditingDuration(true);
                previous_timermode = getMeditationAssistant().getTimerMode();
            }

            updateEditDuration();

            if (getMeditationAssistant().getTimerMode().equals("timed") || getMeditationAssistant().getTimerMode().equals("endat")) {
                editDuration.requestFocus();
                InputMethodManager imm = (InputMethodManager) this
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editDuration,
                        InputMethodManager.SHOW_IMPLICIT);
                editDuration.setSelection(0, editDuration.getText().length());
            }
        }

        updateMeditate(false, false);
        updateVisibleViews(false);

        if (wasEditing) {
            showNextTutorial();
        }
    }

    private void showNextTutorial() {
        if (finishedTutorial == null) {
            finishedTutorial = getMeditationAssistant().getPrefs().getBoolean("finishedTutorial", false);

            if (!finishedTutorial && getMeditationAssistant().db.getNumSessions() > 0) { // Already recorded a session
                getMeditationAssistant().getPrefs().edit().putBoolean("finishedTutorial", true).apply();
                finishedTutorial = true;
            }
        }

        if (!finishedTutorial) {
            Log.d("MeditationAssistant", "Showing next tutorial: " + next_tutorial);
            if (!getMeditationAssistant().getPrefs().getBoolean("finishedTutorial", false)) {
                getMeditationAssistant().getPrefs().edit().putBoolean("finishedTutorial", true).apply(); // Commit early because of crashes
            }

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (sv != null) {
                Log.d("MeditationAssistant", "Tutorial still visible");
                return; // Tutorial still visible
            }

            if (next_tutorial.equals("")) {
                if (!getMeditationAssistant().getEditingDuration()) {
                    View txtTimer = findViewById(R.id.txtTimer);
                    if (txtTimer == null) {
                        return;
                    }
                    next_tutorial = "settings";

                    ViewTarget target = new ViewTarget(R.id.txtTimer, this);
                    try {
                        sv = new ShowcaseView.Builder(this)
                                .withNewStyleShowcase()
                                .setTarget(target)
                                .setContentTitle(R.string.timer)
                                .setContentText(R.string.timerHelp)
                                .setShowcaseEventListener(this)
                                .setStyle(R.style.MeditationShowcaseTheme)
                                .build();
                        sv.hideButton();
                        sv.setHideOnTouchOutside(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (next_tutorial.equals("settings")) {
                if (!getMeditationAssistant().getEditingDuration()) {
                    View actionSettings = findViewById(R.id.action_settings);
                    if (actionSettings == null) {
                        return;
                    }
                    next_tutorial = "medinet";

                    try {
                        sv = new ShowcaseView.Builder(this)
                                .withNewStyleShowcase()
                                .setContentTitle(R.string.settings)
                                .setContentText(R.string.settingsHelp)
                                .setShowcaseEventListener(this)
                                .setStyle(R.style.MeditationShowcaseTheme)
                                .build();
                        sv.hideButton();
                        sv.setHideOnTouchOutside(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (next_tutorial.equals("medinet")) {
                if (!getMeditationAssistant().getEditingDuration()) {
                    View btnMeditationStreak = findViewById(R.id.btnMeditationStreak);
                    if (btnMeditationStreak == null) {
                        return;
                    }
                    next_tutorial = "none";
                    finishedTutorial = true;

                    ViewTarget target = new ViewTarget(R.id.btnMeditationStreak, this);
                    try {
                        sv = new ShowcaseView.Builder(this)
                                .withNewStyleShowcase()
                                .setTarget(target)
                                .setContentTitle(R.string.mediNET)
                                .setShowcaseEventListener(this)
                                .setStyle(R.style.MeditationShowcaseTheme)
                                .build();
                        sv.setContentText(getString(R.string.medinetHelp) + "\n\nनमस्ते");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    public void showViewMA(View view, boolean fadeView) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() != View.VISIBLE && fadeView) {
            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.fadeinma);
            view.startAnimation(animation);
        }
        view.setVisibility(View.VISIBLE);
    }

    public void hideViewMA(View view, boolean fadeView) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() == View.VISIBLE && fadeView) {
            Log.d("MeditationAssistant", "Visible, fading out...");
            FadeOutAnimationListener listener = new FadeOutAnimationListener();
            listener.setView(view);
            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.fadeoutma);
            animation.setAnimationListener(listener);
            view.startAnimation(animation);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    public void updateVisibleViews(boolean fadeViews) {
        RadioGroup radgMainTimerMode = (RadioGroup) findViewById(R.id.radgMainTimerMode);

        RelativeLayout layLowerViews = (RelativeLayout) findViewById(R.id.layLowerViews);
        LinearLayout layLowerViewsEditing = (LinearLayout) findViewById(R.id.layLowerViewsEditing);

        if (getMeditationAssistant().getEditingDuration()) {
            hideViewMA(layLowerViews, false);
            showViewMA(layLowerViewsEditing, false);
        } else {
            showViewMA(layLowerViews, false);
            hideViewMA(layLowerViewsEditing, false);
        }

        EditText editDuration = (EditText) findViewById(R.id.editDuration);

        if (getMeditationAssistant().getEditingDuration()) {
            if (getMeditationAssistant().getTimerMode().equals("timed") || getMeditationAssistant().getTimerMode().equals("endat")) {
                InputMethodManager imm = (InputMethodManager) this
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editDuration,
                        InputMethodManager.SHOW_IMPLICIT);

                editDuration.setSelection(0, editDuration.getText().length());
                editDuration.setEnabled(true);
            } else {
                editDuration.setSelection(0, 0);
                editDuration.setEnabled(false);
            }
        } else {
            editDuration.setSelection(0, 0);
            editDuration.setEnabled(false);
        }

        /* Update radio buttons */
        if (getMeditationAssistant().getTimerMode().equals("untimed")) {
            radgMainTimerMode.check(R.id.radMainUntimed);
        } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
            radgMainTimerMode.check(R.id.radMainEndAt);
        } else {
            radgMainTimerMode.check(R.id.radMainTimed);
        }
    }

    public void updateTextSize() {
        TextView txtTimer = (TextView) findViewById(R.id.txtTimer);
        EditText editDuration = (EditText) findViewById(R.id.editDuration);
        String text_size = getMeditationAssistant().getPrefs().getString("pref_text_size", "normal");
        if (text_size.equals("tiny")) {
            txtTimer.setTextSize(85);
            editDuration.setTextSize(85);
        } else if (text_size.equals("small")) {
            txtTimer.setTextSize(115);
            editDuration.setTextSize(115);
        } else if (text_size.equals("large")) {
            txtTimer.setTextSize(175);
            editDuration.setTextSize(175);
        } else if (text_size.equals("extralarge")) {
            txtTimer.setTextSize(200);
            editDuration.setTextSize(200);
        } else { // Normal
            txtTimer.setTextSize(153);
            editDuration.setTextSize(153);
        }
    }

    public void updatePresets() {
        Button btnPreset1 = (Button) findViewById(R.id.btnPreset1);
        Button btnPreset2 = (Button) findViewById(R.id.btnPreset2);
        Button btnPreset3 = (Button) findViewById(R.id.btnPreset3);

        btnPreset1.setText(getMeditationAssistant().getPrefs().getString("pref_preset_1_label", getString(R.string.setPreset)));
        btnPreset2.setText(getMeditationAssistant().getPrefs().getString("pref_preset_2_label", getString(R.string.setPreset)));
        btnPreset3.setText(getMeditationAssistant().getPrefs().getString("pref_preset_3_label", getString(R.string.setPreset)));
    }

    public String getPresetDefaultLabel() {
        EditText editDuration = (EditText) findViewById(R.id.editDuration);
        TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);

        if (getMeditationAssistant().getTimerMode().equals("untimed")) {
            return "Untimed";
        } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
            ArrayList<String> duration_formatted = new ArrayList<String>();
            if (usetimepicker) {
                duration_formatted.add(String.valueOf(timepickerDuration.getCurrentHour()));
                duration_formatted.add(String.format("%02d",
                        timepickerDuration.getCurrentMinute()));
            } else {
                duration_formatted = getMeditationAssistant().formatDurationEndAt(editDuration.getText().toString().trim());
            }

            if (duration_formatted != null && duration_formatted.size() == 2) {
                return String.format(getString(R.string.presetLabelEndAt), duration_formatted.get(0) + ":" + duration_formatted.get(1));
            }
        } else { // timed
            ArrayList<String> duration_formatted = new ArrayList<String>();
            if (usetimepicker) {
                duration_formatted.add(String.valueOf(timepickerDuration.getCurrentHour()));
                duration_formatted.add(String.format("%02d",
                        timepickerDuration.getCurrentMinute()));
            } else {
                duration_formatted = getMeditationAssistant().formatDuration(editDuration.getText().toString().trim());
            }

            if (duration_formatted != null && duration_formatted.size() == 2) {
                return duration_formatted.get(0) + ":" + duration_formatted.get(1);
            }
        }

        return "";
    }

    public void pressPreset(View view) {
        final EditText editDuration = (EditText) findViewById(R.id.editDuration);

        String preset_key;
        if (view.getId() == R.id.btnPreset1) {
            preset_key = "pref_preset_1";
        } else if (view.getId() == R.id.btnPreset2) {
            preset_key = "pref_preset_2";
        } else if (view.getId() == R.id.btnPreset3) {
            preset_key = "pref_preset_3";
        } else {
            return;
        }

        String preset_value = getMeditationAssistant().getPrefs().getString(preset_key, "");
        Boolean successfulRestore = false;

        if (!preset_value.equals("")) {
            try {
                Set<String> presetSettings = getMeditationAssistant().getPrefs().getStringSet("pref_presetsettings", new HashSet<String>(Arrays.asList(getResources().getStringArray(R.array.presetsettings_default))));
                JSONObject preset = new JSONObject(preset_value);
                Log.d("MeditationAssistant", "Restore preset settings: " + presetSettings.toString() + " - Values: " + preset_value);

                // Mode and duration
                if (presetSettings.contains("modeandduration")) {
                    if (preset.getString("modeandduration").equals("untimed")) {
                        getMeditationAssistant().setTimerMode("untimed");

                        setDuration(null);
                    } else if (preset.getString("modeandduration").startsWith("e") && preset.getString("modeandduration").length() > 1) {
                        getMeditationAssistant().setTimerMode("endat");

                        if (usetimepicker) {
                            String[] preset_split = preset.getString("modeandduration").substring(1).split(":");
                            if (preset_split.length == 2) {
                                TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);
                                timepickerDuration.setCurrentHour(Integer.valueOf(preset_split[0]));
                                timepickerDuration.setCurrentMinute(Integer.valueOf(preset_split[1]));
                            }
                        } else {
                            editDuration.setText(preset.getString("modeandduration").substring(1));
                        }
                        setDuration(null);
                    } else if (!preset.getString("modeandduration").equals("")) {
                        getMeditationAssistant().setTimerMode("timed");

                        if (usetimepicker) {
                            String[] preset_split = preset.getString("modeandduration").split(":");
                            if (preset_split.length == 2) {
                                TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);
                                timepickerDuration.setCurrentHour(Integer.valueOf(preset_split[0]));
                                timepickerDuration.setCurrentMinute(Integer.valueOf(preset_split[1]));
                            }
                        } else {
                            editDuration.setText(preset.getString("modeandduration"));
                        }
                        setDuration(null);
                    }
                }

                // Introduction
                if (presetSettings.contains("introduction") && preset.has("introduction")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_sessionintro", preset.getString("introduction")).apply();
                }

                // Delay
                if (presetSettings.contains("delay")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_session_delay", preset.getString("delay")).apply();
                }

                // Start sound
                if (presetSettings.contains("startsound")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_meditation_sound_start", preset.getString("startsound")).apply();
                    if (preset.getString("startsound").equals("custom")) {
                        getMeditationAssistant().getPrefs().edit().putString("pref_meditation_sound_start_custom", preset.getString("startsoundcustom")).apply();
                    }
                }

                // Interval duration
                if (presetSettings.contains("intervalduration")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_session_interval", preset.getString("intervalduration")).apply();
                }

                // Interval sound
                if (presetSettings.contains("intervalsound")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_meditation_sound_interval", preset.getString("intervalsound")).apply();
                    if (preset.getString("intervalsound").equals("custom")) {
                        getMeditationAssistant().getPrefs().edit().putString("pref_meditation_sound_interval_custom", preset.getString("intervalsoundcustom")).apply();
                    }
                }

                // Interval count
                if (presetSettings.contains("intervalcount")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_interval_count", preset.getString("intervalcount")).apply();
                }

                // Complete sound
                if (presetSettings.contains("completesound")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_meditation_sound_finish", preset.getString("completesound")).apply();
                    if (preset.getString("completesound").equals("custom")) {
                        getMeditationAssistant().getPrefs().edit().putString("pref_meditation_sound_finish_custom", preset.getString("completesoundcustom")).apply();
                    }
                }

                // Ringtone and notifications
                if (presetSettings.contains("ringtone")) {
                    getMeditationAssistant().getPrefs().edit().putString("pref_notificationcontrol", preset.getString("ringtone")).apply();
                }

                // Session volume
                if (presetSettings.contains("volume") && preset.has("volume")) {
                    getMeditationAssistant().getPrefs().edit().putInt("pref_sessionvolume", preset.getInt("volume")).apply();
                }

                // Endless
                if (presetSettings.contains("endless")) {
                    getMeditationAssistant().getPrefs().edit().putBoolean("pref_softfinish", preset.getBoolean("endless")).apply();
                }

                // Vibrate
                if (presetSettings.contains("vibrate")) {
                    getMeditationAssistant().getPrefs().edit().putBoolean("pref_vibrate", preset.getBoolean("vibrate")).apply();
                }

                successfulRestore = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (successfulRestore) {
            Intent presetIntent = new Intent();
            presetIntent.setAction(MeditationAssistant.ACTION_PRESET);
            sendBroadcast(presetIntent);
        } else {
            getMeditationAssistant().shortToast(getString(R.string.setPresetHint));
        }
    }

    public void openProgress(View view) {
        if (sv != null && sv.isShown()) {
            try {
                sv.hide();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Intent intent = new Intent(this, ProgressActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void savePreset(String preset_key) {
        EditText editDuration = (EditText) findViewById(R.id.editDuration);
        TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);

        Preset preset = new Preset();
        if (getMeditationAssistant().getTimerMode().equals("untimed")) {
            preset.modeandduration = "untimed";
        } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
            ArrayList<String> duration_formatted = new ArrayList<String>();
            if (usetimepicker) {
                duration_formatted.add(String.valueOf(timepickerDuration.getCurrentHour()));
                duration_formatted.add(String.format("%02d",
                        timepickerDuration.getCurrentMinute()));
            } else {
                duration_formatted = getMeditationAssistant().formatDurationEndAt(editDuration.getText().toString().trim());
            }

            if (duration_formatted != null && duration_formatted.size() == 2) {
                preset.modeandduration = "e" + duration_formatted.get(0) + ":" + duration_formatted.get(1);
            }
        } else { // timed
            ArrayList<String> duration_formatted = new ArrayList<String>();
            if (usetimepicker) {
                duration_formatted.add(String.valueOf(timepickerDuration.getCurrentHour()));
                duration_formatted.add(String.format("%02d",
                        timepickerDuration.getCurrentMinute()));
            } else {
                duration_formatted = getMeditationAssistant().formatDuration(editDuration.getText().toString().trim());
            }

            if (duration_formatted != null && duration_formatted.size() == 2) {
                preset.modeandduration = duration_formatted.get(0) + ":" + duration_formatted.get(1);
            }
        }

        preset.introduction = getMeditationAssistant().getPrefs().getString("pref_sessionintro", getString(R.string.ignore_introphrase));
        preset.delay = getMeditationAssistant().getPrefs().getString("pref_session_delay", "00:15");
        preset.startsound = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_start", "");
        preset.startsoundcustom = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_start_custom", "");
        preset.intervalduration = getMeditationAssistant().getPrefs().getString("pref_session_interval", "00:00");
        preset.intervalsound = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_interval", "");
        preset.intervalsoundcustom = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_interval_custom", "");
        preset.intervalcount = getMeditationAssistant().getPrefs().getString("pref_interval_count", "");
        preset.completesound = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_finish", "");
        preset.completesoundcustom = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_finish_custom", "");
        preset.ringtone = getMeditationAssistant().getPrefs().getString("pref_notificationcontrol", "");
        preset.volume = getMeditationAssistant().getPrefs().getInt("pref_sessionvolume", 50);
        preset.endless = getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false);
        preset.vibrate = getMeditationAssistant().getPrefs().getBoolean("pref_vibrate", false);

        String exported = preset.export().toString();
        Log.d("MeditationAssistant", "Setting preset: " + exported);
        getMeditationAssistant().getPrefs().edit().putString(preset_key, exported).apply();
    }

    public void pressMeditate(View view) {
        if (sv != null && sv.isShown()) {
            try {
                sv.hide();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long timestamp = getMeditationAssistant().getTimestamp();

        if (getMeditationAssistant().getTimeStartMeditate() > 0) { // session in progress
            if (getMeditationAssistant().getTimeStartMeditate() > timestamp) { // currently in delay phase
                Log.d("MeditationAssistant",
                        "PREVIOUS Timestamp: "
                                + String.valueOf(timestamp)
                                + " Stop: "
                                + String.valueOf(getMeditationAssistant()
                                .getTimeToStopMeditate())
                                + " Start: "
                                + String.valueOf(getMeditationAssistant()
                                .getTimeStartMeditate())
                );
                getMeditationAssistant().setTimeStartMeditate(timestamp);

                if (getMeditationAssistant().getTimeToStopMeditate() != -1 && getMeditationAssistant().getTimerMode().equals("timed")) {  // update end time for timed session
                    getMeditationAssistant().setTimeToStopMeditate(
                            timestamp
                                    + getMeditationAssistant()
                                    .getSessionDuration()
                    );
                }

                Log.d("MeditationAssistant",
                        "NEW Timestamp: "
                                + String.valueOf(timestamp)
                                + " Stop: "
                                + String.valueOf(getMeditationAssistant()
                                .getTimeToStopMeditate())
                                + " Start: "
                                + String.valueOf(getMeditationAssistant()
                                .getTimeStartMeditate())
                );
                handler.removeCallbacks(meditateRunnable);
                handler.removeCallbacks(intervalRunnable);
                skipDelay = true;
                if (pendingintent_delay != null) {
                    am.cancel(pendingintent_delay);
                }
                handler.postDelayed(meditateRunnable, 50);
            } else { // Currently in meditation phase
                if (!getMeditationAssistant().ispaused) { // In progress, pause the session
                    if (pendingintent != null) {
                        am.cancel(pendingintent);
                    }
                    if (pendingintent_interval != null) {
                        am.cancel(pendingintent_interval);
                    }
                    Log.d("MeditationAssistant", "CANCELLED MAIN WAKEUP AND INTERVAL ALARMS");

                    TextView txtTimer = (TextView) findViewById(R.id.txtTimer);
                    TextView txtDurationSeconds = (TextView) findViewById(R.id.txtDurationSeconds);
                    getMeditationAssistant().pausedTimerHoursMinutes = txtTimer.getText().toString();
                    getMeditationAssistant().pausedTimerSeconds = txtDurationSeconds.getText().toString();
                    getMeditationAssistant().pauseSession();

                    handler.removeCallbacks(screenDimRunnable);
                    handler.removeCallbacks(screenOffRunnable);
                    WindowManager.LayoutParams params = getWindow().getAttributes();
                    params.screenBrightness = -1;
                    getWindow().setAttributes(params);

                    if (view != null) {
                        getMeditationAssistant().longToast(getString(R.string.pausedNotification));
                    }
                } else { // Paused, un-pause
                    long pausetime = getMeditationAssistant().unPauseSession();

                    if (getMeditationAssistant().getTimerMode().equals("timed")) {
                        getMeditationAssistant().setTimeToStopMeditate(getMeditationAssistant().getTimeToStopMeditate() + pausetime);
                    } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
                        if (getMeditationAssistant().getTimeToStopMeditate() - pausetime <= 0) {
                            Intent openAlarmReceiverActivity = new Intent(getApplicationContext(), CompleteActivity.class);
                            openAlarmReceiverActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            openAlarmReceiverActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(openAlarmReceiverActivity);

                            return;
                        }
                    }

                    setIntervalAlarm();

                    if (getMeditationAssistant().getTimeToStopMeditate() != -1 && timestamp < getMeditationAssistant().getTimeToStopMeditate()) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.SECOND, (int) (getMeditationAssistant()
                                .getTimeToStopMeditate() - getMeditationAssistant().getTimestamp()));

                        Intent intent = new Intent(getApplicationContext(),
                                MainActivity.class);
                        intent.putExtra("wakeup", true);
                        intent.putExtra("fullwakeup", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.setAction(BROADCAST_ACTION_ALARM);
                        pendingintent = PendingIntent.getActivity(
                                getApplicationContext(), ID_END, intent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                        Log.d("MeditationAssistant", "Setting MAIN WAKEUP alarm for "
                                + String.valueOf(cal.getTimeInMillis()) + " (Now: "
                                + System.currentTimeMillis() + ", in: " + String.valueOf((cal.getTimeInMillis() - System.currentTimeMillis()) / 1000) + ")");
                        getMeditationAssistant().setAlarm(true, cal.getTimeInMillis(), pendingintent);
                    }

                    getMeditationAssistant().ispaused = false;
                    screenDimOrOff();
                }
            }

            return;
        }

        getMeditationAssistant().releaseAllWakeLocks();
        getMeditationAssistant().ispaused = false;
        getMeditationAssistant().pausetime = 0;
        playedStartSound = false;
        skipDelay = false;
        intervals = 0;

        Log.d("MeditationAssistant", "Timestamp: " + String.valueOf(timestamp));
        Integer secondsTillFinished = 0;
        if (getMeditationAssistant().getTimerMode().equals("timed")) {
            secondsTillFinished = Integer.parseInt(getMeditationAssistant().getPrefs().getString(
                    "timerHours", "0"))
                    * 3600
                    + (Integer.parseInt(getMeditationAssistant().getPrefs().getString("timerMinutes", "15")) * 60);
        } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
            Date now = new Date();

            Calendar c_now = Calendar.getInstance();
            c_now.setTime(now);

            int end_at_hour = Integer.parseInt(getMeditationAssistant().getPrefs().getString("timerHoursEndAt", "0"));
            int end_at_minute = Integer.parseInt(getMeditationAssistant().getPrefs().getString("timerMinutesEndAt", "0"));

            if (end_at_hour > 11) {
                end_at_hour -= 12; // convert 24 to 12 hour time
            }

            Calendar c_endat = Calendar.getInstance();
            c_endat.setTime(now);

            if (c_now.get(Calendar.HOUR_OF_DAY) >= 12) { // noon or later
                Log.d("MeditationAssistant", "End at debug: NOON OR LATER");
                if ((end_at_hour + 12) >= c_now.get(Calendar.HOUR_OF_DAY)) { // later today
                    Log.d("MeditationAssistant", "End at debug: A LATER TODAY");
                    if ((end_at_hour + 12) == c_now.get(Calendar.HOUR_OF_DAY) && end_at_minute <= c_now.get(Calendar.MINUTE)) { // End at is now or earlier
                        getMeditationAssistant().shortToast(getString(R.string.invalidEndAt));
                        return;
                    }

                    c_endat.set(Calendar.HOUR_OF_DAY, end_at_hour + 12);
                } else { // tomorrow
                    Log.d("MeditationAssistant", "End at debug: A TOMORROW");
                    c_endat.add(Calendar.DATE, 1);
                    c_endat.set(Calendar.HOUR_OF_DAY, end_at_hour);
                }
            } else {
                Log.d("MeditationAssistant", "End at debug: BEFORE NOON");
                if (end_at_hour >= c_now.get(Calendar.HOUR_OF_DAY)) { // later today (before noon)
                    Log.d("MeditationAssistant", "End at debug: B LATER TODAY BEFORE NOON");
                    if (end_at_hour == c_now.get(Calendar.HOUR_OF_DAY) && end_at_minute <= c_now.get(Calendar.MINUTE)) { // End at is now or earlier
                        getMeditationAssistant().shortToast(getString(R.string.invalidEndAt));
                        return;
                    }

                    c_endat.set(Calendar.HOUR_OF_DAY, end_at_hour);
                } else { // later today (after noon)
                    Log.d("MeditationAssistant", "End at debug: B LATER TODAY AFTER NOON");
                    c_endat.set(Calendar.HOUR_OF_DAY, end_at_hour + 12);
                }
            }
            c_endat.set(Calendar.MINUTE, end_at_minute);
            c_endat.set(Calendar.SECOND, 0);

            Log.d("MeditationAssistant", "NOW HOUROFDAY: " + String.valueOf(c_now.get(Calendar.HOUR_OF_DAY)) + " MINUTE: " + String.valueOf(c_now.get(Calendar.MINUTE)));
            Log.d("MeditationAssistant", "END HOUROFDAY: " + String.valueOf(c_endat.get(Calendar.HOUR_OF_DAY)) + " MINUTE: " + String.valueOf(c_endat.get(Calendar.MINUTE)));
            Log.d("MeditationAssistant", "-- END AT: " + String.valueOf(c_endat.getTimeInMillis() / 1000) + " NOW: " + String.valueOf(c_now.getTimeInMillis() / 1000));

            // Add two seconds to account for partial seconds between now and the end at time for pretty durations
            secondsTillFinished = (int) ((c_endat.getTimeInMillis() - c_now.getTimeInMillis()) / 1000) + 2;

            if (secondsTillFinished < 60) {
                getMeditationAssistant().shortToast(getString(R.string.invalidEndAt));
                return;
            }
        }

        getMeditationAssistant().setSessionDuration(secondsTillFinished);

        Log.d("MeditationAssistant", "Current delay value: " + getMeditationAssistant().getPrefs().getString("pref_session_delay", "00:15"));

        long delay = Math.max(
                getMeditationAssistant().timePreferenceValueToSeconds(getMeditationAssistant().getPrefs().getString("pref_session_delay", "00:15"), "00:15"), 0);

        getMeditationAssistant().setTimeStartMeditate(timestamp + delay);

        switch (getMeditationAssistant().getTimerMode()) {
            case "timed":
                getMeditationAssistant().setTimeToStopMeditate(timestamp + secondsTillFinished + delay);
                break;
            case "endat":
                if (secondsTillFinished <= delay) {
                    delay = 0;
                }
                getMeditationAssistant().setTimeToStopMeditate(timestamp + secondsTillFinished);
                break;
            default:
                getMeditationAssistant().setTimeToStopMeditate(-1);
                break;
        }

        updateMeditate(true, false);
        Log.d("MeditationAssistant", "Starting runnable from startMeditate");

        String sessionIntro = getMeditationAssistant().getPrefs().getString("pref_sessionintro", getString(R.string.ignore_introphrase));
        if (!sessionIntro.equals("")) {
            getMeditationAssistant().shortToast(sessionIntro);
        }

        meditateRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("MeditationAssistant", "Execute meditateRunnable");

                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
                if (getMeditationAssistant().getTimeStartMeditate() == 0) {
                    return;
                }

                if (!playedStartSound) {
                    playedStartSound = true;

                    if (getMeditationAssistant().getTimeToStopMeditate() != -1) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.SECOND, (int) (getMeditationAssistant()
                                .getTimeToStopMeditate() - getMeditationAssistant()
                                .getTimeStartMeditate()));

                        Intent intent = new Intent(getApplicationContext(),
                                MainActivity.class);
                        intent.putExtra("wakeup", true);
                        intent.putExtra("fullwakeup", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.setAction(BROADCAST_ACTION_ALARM);
                        pendingintent = PendingIntent.getActivity(
                                getApplicationContext(), ID_END, intent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                        Log.d("MeditationAssistant", "Setting MAIN WAKEUP alarm for "
                                + String.valueOf(cal.getTimeInMillis()) + " (Now: "
                                + System.currentTimeMillis() + ", in: " + String.valueOf((cal.getTimeInMillis() - System.currentTimeMillis()) / 1000) + ")");
                        getMeditationAssistant().setAlarm(true, cal.getTimeInMillis(), pendingintent);
                    }

                    if (!skipDelay) {
                        getMeditationAssistant().vibrateDevice();
                    }

                    String startSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_start", "");
                    if (!startSoundPath.equals("none")) {
                        if (startSoundPath.equals("custom")) {
                            startSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_start_custom", "");
                            getMeditationAssistant().playSound(0, startSoundPath, false);
                        } else {
                            getMeditationAssistant().playSound(MeditationSounds.getMeditationSound(startSoundPath), "", false);
                        }
                    }

                    setIntervalAlarm();
                }

                screenDimOrOff();
            }
        };

        startRunnable();

        getMeditationAssistant().setNotificationControl();

        WindowManager.LayoutParams params = getWindow().getAttributes();

        if (getMeditationAssistant().getPrefs().getString("pref_screencontrol", "dim").equals("on")) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        String pref_full_screen = getMeditationAssistant().getPrefs().getString("pref_full_screen", "");
        if (pref_full_screen.equals("session")) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        getWindow().setAttributes(params);

        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (getMeditationAssistant().previous_volume == null) {
            getMeditationAssistant().previous_volume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, (int) ((mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) * getMeditationAssistant().getPrefs().getInt("pref_sessionvolume", 50) * 0.1) / 10), 0);

        if (delay > 0) {
            setVolumeControlStream(AudioManager.STREAM_ALARM);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, (int) delay);

            Log.d("MeditationAssistant", "Setting DELAY WAKEUP alarm for "
                    + String.valueOf(cal.getTimeInMillis()) + " (Now: "
                    + System.currentTimeMillis() + ", in: " + String.valueOf((cal.getTimeInMillis() - System.currentTimeMillis()) / 1000) + ")");

            Intent intent_delay = new Intent(getApplicationContext(),
                    MainActivity.class);
            intent_delay.putExtra("wakeup", true);
            intent_delay.putExtra("wakeupstart", true);
            intent_delay.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent_delay.setAction(BROADCAST_ACTION_ALARM);
            pendingintent_delay = PendingIntent.getActivity(
                    getApplicationContext(), ID_DELAY, intent_delay,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            getMeditationAssistant().setAlarm(true, cal.getTimeInMillis(), pendingintent_delay);
        } else {
            handler.postDelayed(meditateRunnable, 50);
        }
    }

    private void setIntervalAlarm() {
        long interval = Math.max(
                getMeditationAssistant().timePreferenceValueToSeconds(getMeditationAssistant().getPrefs().getString("pref_session_interval", "00:00"), "00:00"), 0);
        Log.d("MeditationAssistant", "Interval is set to " + String.valueOf(interval) + " seconds");

        if (interval > 0) {
            Log.d("MeditationAssistant", "Reached postDelayed for interval runnable");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, (int) interval);

            Log.d("MeditationAssistant", "Setting INITIAL INTERVAL WAKEUP alarm for "
                    + String.valueOf(cal.getTimeInMillis()) + " (Now: "
                    + System.currentTimeMillis() + ", in: " + String.valueOf((cal.getTimeInMillis() - System.currentTimeMillis()) / 1000) + ")");

            Intent intent_interval = new Intent(
                    getApplicationContext(), MainActivity.class);
            intent_interval.putExtra("wakeup", true);
            intent_interval.putExtra("wakeupinterval", true);
            intent_interval.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent_interval.setAction(BROADCAST_ACTION_ALARM);
            pendingintent_interval = PendingIntent.getActivity(
                    getApplicationContext(), ID_INTERVAL,
                    intent_interval, PendingIntent.FLAG_CANCEL_CURRENT);
            getMeditationAssistant().setAlarm(true, cal.getTimeInMillis(), pendingintent_interval);
        }
    }

    private void screenDimOrOff() {
        String pref_screencontrol = getMeditationAssistant().getPrefs().getString("pref_screencontrol", "dim");
        if (!pref_screencontrol.equals("ondim") && !pref_screencontrol.equals("dim") && !pref_screencontrol.equals("off")) {
            return;
        }

        screenDimRunnable = () -> {
            if (getMeditationAssistant().getTimeStartMeditate() == 0) {
                Log.d("MeditationAssistant", "Exiting runnable for dimming screen");
                return;
            }

            WindowManager.LayoutParams windowParams = getWindow().getAttributes();
            windowParams.screenBrightness = 0.01f;
            getWindow().setAttributes(windowParams);

            if (pref_screencontrol.equals("ondim")) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        };

        handler.postDelayed(screenDimRunnable, 250);
    }

    public boolean longPressMeditate(View view) {
        long timestamp = System.currentTimeMillis() / 1000;
        Log.d("MeditationAssistant", "stopMedidate");
        getMeditationAssistant().unPauseSession();

        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        if (pendingintent != null) {
            am.cancel(pendingintent);
            Log.d("MeditationAssistant", "Cancelled main wake alarm");
        }
        if (pendingintent_delay != null) {
            am.cancel(pendingintent_delay);
            Log.d("MeditationAssistant", "Cancelled delay alarm");
        }
        if (pendingintent_interval != null) {
            am.cancel(pendingintent_interval);
            Log.d("MeditationAssistant", "Cancelled interval alarm");
        }

        getMeditationAssistant().releaseAllWakeLocks();
        getMeditationAssistant().restoreVolume();

        if (getMeditationAssistant().getTimeStartMeditate() != 0) {
            if (view != null
                    && timestamp
                    - getMeditationAssistant().getTimeStartMeditate() > 0) {
                Intent openAlarmReceiverActivity = new Intent(
                        getApplicationContext(), CompleteActivity.class);
                openAlarmReceiverActivity.putExtra("manual", true);
                openAlarmReceiverActivity
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(openAlarmReceiverActivity);
            } else {
                // Reset timestamps for current session
                getMeditationAssistant().setTimeStartMeditate(0);
                getMeditationAssistant().setTimeToStopMeditate(0);
                updateMeditate(false, false);
            }

            handler.removeCallbacks(meditateRunnable);
            handler.removeCallbacks(intervalRunnable);
            handler.removeCallbacks(screenDimRunnable);
            handler.removeCallbacks(screenOffRunnable);
            getMeditationAssistant().setRunnableStopped(true);
            return true;
        }

        return false;
    }

    public void onTimerModeSelected(View view) {
        EditText editDuration = (EditText) findViewById(R.id.editDuration);
        TimePicker timepickerDuration = (TimePicker) findViewById(R.id.timepickerDuration);

        String newTimerMode = "timed";
        if (view.getId() == R.id.radMainEndAt || view.getId() == R.id.layMainEndAt) {
            newTimerMode = "endat";
        } else if (view.getId() == R.id.radMainUntimed || view.getId() == R.id.layMainUntimed) {
            newTimerMode = "untimed";
        }

        getMeditationAssistant().setTimerMode(newTimerMode);
        if (newTimerMode.equals("untimed")) {
            if (usetimepicker) {
                timepickerDuration.setEnabled(false);
            } else {
                editDuration.setText(getString(R.string.ignore_om));
            }
        } else {
            if (usetimepicker) {
                timepickerDuration.setEnabled(true);
            } else {

                if (editDuration.getText().toString().equals(getString(R.string.ignore_om))) {
                    if (newTimerMode.equals("endat")) { // Don't leave om character in edit text
                        editDuration.setText(getMeditationAssistant().getPrefs().getString("timerHoursEndAt", "0")
                                + ":"
                                + String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs()
                                .getString("timerMinutesEndAt", "0"))));
                    } else { // timed
                        editDuration.setText(getMeditationAssistant().getPrefs().getString("timerHours", "0")
                                + ":"
                                + String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs()
                                .getString("timerMinutes", "15"))));
                    }
                    editDuration.requestFocus();
                    InputMethodManager imm = (InputMethodManager) this
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editDuration,
                            InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }

        updateVisibleViews(false);
    }

    @Override
    protected void onPause() {
        if (getMeditationAssistant().getTimeStartMeditate() > 0
                && getMeditationAssistant().getTimeToStopMeditate() != 0) {
            getMeditationAssistant().showNotification();
        }

        getMeditationAssistant().setScreenOff(true);
        getMeditationAssistant().getPrefs().unregisterOnSharedPreferenceChangeListener(sharedPrefslistener);

        super.onPause();
    }

    @Override
    protected void onResume() {
        getMeditationAssistant().hideNotification();

        if (getIntent().getStringExtra("action") != null) {
            Log.d("MeditationAssistant", "Intent for MainActivity: "
                    + getIntent().getStringExtra("action"));
        }
        getMeditationAssistant().setScreenOff(false);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = -1;
        getWindow().setAttributes(params);

        getMeditationAssistant().getPrefs().registerOnSharedPreferenceChangeListener(sharedPrefslistener);
        if (lastKey != null && !lastKey.equals("")
                && !getMeditationAssistant().getPrefs().getString("key", "").equals(lastKey)) {
            Log.d("MeditationAssistant", "onResume detected key change");
        }

        usetimepicker = getMeditationAssistant().getPrefs().getBoolean("pref_usetimepicker", false);

        refreshAll();

        if (getMeditationAssistant().asktorate) {
            getMeditationAssistant().asktorate = false;

            if (!getMeditationAssistant().getPrefs().getBoolean("askedtorate", false) && (getMeditationAssistant().getMarketName().equals("bb") || getMeditationAssistant().getMarketName().equals("google") || getMeditationAssistant().getMarketName().equals("amazon"))) {
                getMeditationAssistant().getPrefs().edit().putBoolean("askedtorate", true).apply();

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                getMeditationAssistant().rateApp();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(
                        getResources()
                                .getDrawable(
                                        getTheme()
                                                .obtainStyledAttributes(
                                                        getMeditationAssistant()
                                                                .getMATheme(),
                                                        new int[]{R.attr.actionIconNotImportant}
                                                )
                                                .getResourceId(0, 0)
                                )
                )
                        .setTitle(getString(R.string.rateMeditationAssistant))
                        .setMessage(
                                getString(R.string.rateMeditationAssistantText))
                        .setPositiveButton(getString(R.string.yes),
                                dialogClickListener)
                        .setNegativeButton(getString(R.string.no),
                                dialogClickListener).show();
            }
        }

        showNextTutorial();

        super.onResume();
    }

    @Override
    public void onStart() {
        getMeditationAssistant().utility.trackingStart(this);
        getMeditationAssistant().utility.connectGoogleClient();

        super.onStart();
    }

    @Override
    protected void onStop() {
        getMeditationAssistant().utility.trackingStop(this);

        getMeditationAssistant().utility.disconnectGoogleClient();

        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (!getMeditationAssistant().getPrefs().getBoolean("pref_autosignin", false) && !getMeditationAssistant().getPrefs().getString("key", "").equals("")) {
            getMeditationAssistant().getPrefs().edit().putString("key", "").apply();
        }

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && getMeditationAssistant().getEditingDuration()) {
            cancelSetDuration(null);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void updateTexts() {
        String pref_full_screen = getMeditationAssistant().getPrefs().getString("pref_full_screen", "");

        TextView txtMainStatus = (TextView) findViewById(R.id.txtMainStatus);
        Button btnMeditationStreak = (Button) findViewById(R.id.btnMeditationStreak);
        Resources res = getResources();

        if (getMeditationAssistant().getTimeToStopMeditate() < 1) {
            resetScreenBrightness();
            getMeditationAssistant().unsetNotificationControl();

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (!pref_full_screen.equals("always")) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } else if (!pref_full_screen.equals("always") && !pref_full_screen.equals("session")) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (!getMeditationAssistant().getPrefs().getBoolean("pref_showstreak", true)
                || getMeditationAssistant().getMeditationStreak().get(0) < 1) {
            if (getMeditationAssistant().getMediNET().status.equals("success")) {
                btnMeditationStreak
                        .setText(getString(R.string.signOutOfMediNET));
            } else {

                btnMeditationStreak.setText(getString(R.string.signInToMediNET));
            }
        } else {
            btnMeditationStreak.setText(res.getQuantityString(
                    R.plurals.daysOfMeditation, getMeditationAssistant()
                            .getMeditationStreak().get(0).intValue(),
                    getMeditationAssistant().getMeditationStreak().get(0).intValue()
            ));
        }

        if (getMeditationAssistant().getMediNET().status != null) {
            switch (getMeditationAssistant().getMediNET().status) {
                case "connecting":
                    txtMainStatus.setText(getString(R.string.mediNETConnecting));
                    break;
                case "success":
                    txtMainStatus.setText(getString(R.string.mediNETConnected));
                    break;
                default:
                    txtMainStatus.setText("");
                    break;
            }
        }

        btnMeditationStreak.postInvalidate();
        txtMainStatus.postInvalidate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        int widgetId =  intent.getIntExtra("widgetid", -1);

        super.onNewIntent(intent);
        setIntent(intent);

        if (getIntent() != null) {
            Log.d("MeditationAssistant", "Intent for MainActivity: "
                    + getIntent().toString());
            if (getIntent().getAction() != null) {
                if (getIntent().getAction().equals("widgetclick")) {
                    if (widgetId >= 0) {
                        int preset = PresetWidgetActivity.loadPresetPref(getApplicationContext(), widgetId);
                        if (preset >= 0) {
                            String preset_value = getMeditationAssistant().getPrefs().getString("pref_preset_" + preset, "");
                            if (!preset_value.isEmpty()) {
                                View presetView;
                                if (preset == 1) {
                                    presetView = findViewById(R.id.btnPreset1);
                                } else if (preset == 2) {
                                    presetView = findViewById(R.id.btnPreset2);
                                } else {
                                    presetView = findViewById(R.id.btnPreset3);
                                }

                                if (!getMeditationAssistant().getEditingDuration()) {
                                    changeDuration(null);
                                }

                                pressPreset(presetView);
                            }
                        }
                        return;
                    }

                    if (!getMeditationAssistant().getMediNET().status.equals("success") && getMeditationAssistant().getPrefs().getBoolean("pref_autosignin", false)) {
                        getMeditationAssistant().getMediNET().connect();
                    }
                } else if (getIntent().getAction().equals("notificationPause")) {
                    if (getMeditationAssistant().getTimeStartMeditate() > 0) {
                        updateMeditate(false, false);
                        pressMeditate(new View(getApplicationContext()));
                    }
                } else if (getIntent().getAction().equals("notificationEnd")) {
                    if (getMeditationAssistant().getTimeStartMeditate() > 0) {
                        longPressMeditate(new View(getApplicationContext()));
                    }
                }
            }

            if (intent.getBooleanExtra("wakeup", false)) {
                Boolean fullWakeUp = intent.getBooleanExtra("fullwakeup", false);
                Boolean wakeUpStart = intent.getBooleanExtra("wakeupstart", false);
                Boolean wakeUpInterval = intent.getBooleanExtra("wakeupinterval", false);

                Log.d("MeditationAssistant", "ALARM RECEIVER INTEGRATED: Received broadcast - Full: " + (fullWakeUp ? "Full" : "Partial") + " - Start/interval: " + (wakeUpStart ? "Start" : (wakeUpInterval ? "Interval" : "Neither")));

                if (wakeLockID != null) {
                    getMeditationAssistant().releaseWakeLock(wakeLockID);
                }
                wakeLockID = getMeditationAssistant().acquireWakeLock(fullWakeUp);

                handler.removeCallbacks(clearWakeLock);
                handler.postDelayed(clearWakeLock, 7000);

                if (wakeUpStart) {
                    handler.postDelayed(meditateRunnable, 50);
                } else if (wakeUpInterval) {
                    if (getMeditationAssistant().getTimeStartMeditate() > 0) {
                        if (getMeditationAssistant().getTimeToStopMeditate() != 0
                                && getMeditationAssistant().getTimeToStopMeditate()
                                - (System.currentTimeMillis() / 1000) < 30 && !getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false)) {
                            Log.d("MeditationAssistant", "Interval - final 30 seconds, not firing");
                            return; // No interval sounds during the final 30 seconds
                        }

                        String interval_limit = getMeditationAssistant().getPrefs().getString("pref_interval_count", "");
                        if (interval_limit.equals("")) {
                            interval_limit = "0";
                        }
                        if (Integer.valueOf(interval_limit) > 0 && intervals >= Integer.valueOf(interval_limit)) {
                            Log.d("MeditationAssistant", "Interval - reached interval limit, not firing A");
                            return; // No further intervals
                        }

                        String intervalSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_interval", "");

                        if (!intervalSoundPath.equals("none") || getMeditationAssistant().vibrationEnabled()) {
                            if (getMeditationAssistant().getTimeToStopMeditate() == -1
                                    || ((System.currentTimeMillis() / 1000) > getMeditationAssistant().getTimeToStopMeditate() && (System.currentTimeMillis() / 1000) - getMeditationAssistant().getTimeToStopMeditate() >= 5) || getMeditationAssistant().getTimeToStopMeditate()
                                    - (System.currentTimeMillis() / 1000) >= 5) { // Not within last 5 seconds
                                if (!intervalSoundPath.equals("none")) {
                                    if (intervalSoundPath.equals("custom")) {
                                        intervalSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_interval_custom", "");
                                        getMeditationAssistant().playSound(0, intervalSoundPath, false);
                                    } else {
                                        getMeditationAssistant().playSound(MeditationSounds.getMeditationSound(intervalSoundPath), "", false);
                                    }
                                }

                                getMeditationAssistant().vibrateDevice();
                            }

                            long interval = Math.max(
                                    getMeditationAssistant().timePreferenceValueToSeconds(getMeditationAssistant().getPrefs().getString("pref_session_interval", "00:00"), "00:00"), 0);
                            Log.d("MeditationAssistant", "Interval is set to " + String.valueOf(interval) + " seconds");

                            if (interval > 0 && (getMeditationAssistant().getTimeToStopMeditate() == -1
                                    || ((getMeditationAssistant().getTimeToStopMeditate()
                                    - (System.currentTimeMillis() / 1000)) > (interval + 30)) || getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false))) {
                                intervals++;

                                if (Integer.valueOf(interval_limit) > 0 && intervals >= Integer.valueOf(interval_limit)) {
                                    Log.d("MeditationAssistant", "Interval - reached interval limit, not firing B");
                                    return; // No further intervals
                                }

                                Calendar cal = Calendar.getInstance();
                                cal.add(Calendar.SECOND, (int) interval);

                                Log.d("MeditationAssistant", "Setting INTERVAL WAKEUP alarm for "
                                        + String.valueOf(cal.getTimeInMillis()) + " (Now: "
                                        + System.currentTimeMillis() + ", in: " + String.valueOf((cal.getTimeInMillis() - System.currentTimeMillis()) / 1000) + ") - TOTAL TIME LEFT: " + String.valueOf(getMeditationAssistant().getTimeToStopMeditate()
                                        - (System.currentTimeMillis() / 1000)));

                                Intent intent_interval = new Intent(
                                        getApplicationContext(), MainActivity.class);
                                intent_interval.putExtra("wakeup", true);
                                intent_interval.putExtra("wakeupinterval", true);
                                intent_interval.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                intent_interval.setAction(BROADCAST_ACTION_ALARM);
                                pendingintent_interval = PendingIntent.getActivity(
                                        getApplicationContext(), ID_INTERVAL,
                                        intent_interval, PendingIntent.FLAG_CANCEL_CURRENT);
                                getMeditationAssistant().setAlarm(true, cal.getTimeInMillis(), pendingintent_interval);
                            } else {
                                Log.d("MeditationAssistant", "Skipping INTERVAL WAKEUP alarm");
                            }
                        }
                    }
                }

                if (fullWakeUp) {
                    if (getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false)) {
                        String finishSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_finish", "");
                        if (!finishSoundPath.equals("none")) {
                            if (finishSoundPath.equals("custom")) {
                                finishSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_finish_custom", "");
                                getMeditationAssistant().playSound(0, finishSoundPath, true);
                            } else {
                                getMeditationAssistant().playSound(MeditationSounds.getMeditationSound(finishSoundPath), "", true);
                            }
                        }

                        getMeditationAssistant().vibrateDevice();
                    } else {
                        Intent openAlarmReceiverActivity = new Intent(getApplicationContext(), CompleteActivity.class);
                        openAlarmReceiverActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        openAlarmReceiverActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(openAlarmReceiverActivity);
                    }
                }
            }
        }
    }

    public void updateMeditate(boolean setDisabled, boolean complete) {
        Button btnMeditate = (Button) findViewById(R.id.btnMeditate);
        TextView txtTimer = (TextView) findViewById(R.id.txtTimer);
        TextView txtDurationSeconds = (TextView) findViewById(R.id.txtDurationSeconds);

        long duration = 0;
        Long timestamp = System.currentTimeMillis() / 1000;

        if (getMeditationAssistant().getTimeToStopMeditate() == -1
                || getMeditationAssistant().getTimeToStopMeditate() > 0) {
            if (getMeditationAssistant().getTimeToStopMeditate() != -1) {
                if (getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false) && timestamp > getMeditationAssistant().getTimeToStopMeditate()) {
                    duration = timestamp - getMeditationAssistant()
                            .getTimeStartMeditate();
                } else {
                    duration = Math.min(getMeditationAssistant()
                                    .getTimeToStopMeditate() - timestamp,
                            getMeditationAssistant().getTimeToStopMeditate()
                                    - getMeditationAssistant()
                                    .getTimeStartMeditate()
                    );
                }
            } else {
                duration = timestamp
                        - getMeditationAssistant().getTimeStartMeditate();
            }

            txtTimer.setClickable(false);
        } else {
            resetScreenBrightness();
            getMeditationAssistant().unsetNotificationControl();

            txtTimer.setClickable(true);
        }

        if (complete) {
            Log.d("MeditationAssistant", "Session complete");
        } else if (getMeditationAssistant().ispaused) {
            txtTimer.setText(getMeditationAssistant().pausedTimerHoursMinutes);
        } else if (getMeditationAssistant().getTimeToStopMeditate() == -1 || (getMeditationAssistant().getTimeToStopMeditate() > 0 && timestamp > getMeditationAssistant().getTimeToStopMeditate() && getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false))) {
            duration -= getMeditationAssistant().pausetime;

            if (timestamp >= getMeditationAssistant().getTimeStartMeditate()) {
                int hoursSince = (int) duration / 3600;
                int minutesSince = ((int) duration % 3600) / 60;
                txtTimer.setText(String.valueOf(hoursSince) + ":"
                        + String.format("%02d", minutesSince));
            } else {
                txtTimer.setText(getString(R.string.ignore_om));
            }
        } else if (duration > 0) {
            int hoursLeft = (int) duration / 3600;
            int minutesLeft = ((int) duration % 3600) / 60;
            if (minutesLeft == 60) {
                hoursLeft += 1;
                minutesLeft = 0;
            }

            txtTimer.setText(String.valueOf(hoursLeft) + ":"
                    + String.format("%02d", minutesLeft));
        }

        if (getMeditationAssistant().getTimeToStopMeditate() == -1 || duration > 0 || (getMeditationAssistant().getTimeToStopMeditate() > 0 && getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false))) {
            long delayRemaining = getMeditationAssistant().getTimeStartMeditate() - timestamp;

            findViewById(R.id.btnMeditate).setEnabled(true);

            if (getMeditationAssistant().ispaused) {
                btnMeditate.setText(getString(R.string.resumeOrEnd));
                getMeditationAssistant().setAlphaCompat(txtDurationSeconds, 1f);
            } else if (delayRemaining > 0) {
                btnMeditate.setText(getString(R.string.tapToSkip));
                getMeditationAssistant().setAlphaCompat(txtDurationSeconds, 0.75f);
            } else {
                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

                btnMeditate.setText(getString(R.string.pauseOrEnd));
                getMeditationAssistant().setAlphaCompat(txtDurationSeconds, 1f);
            }

            if (getMeditationAssistant().getPrefs().getBoolean("pref_display_seconds", true)) {
                if (getMeditationAssistant().ispaused) {
                    txtDurationSeconds.setText(getMeditationAssistant().pausedTimerSeconds);
                } else if (getMeditationAssistant().getTimeStartMeditate() == timestamp
                        || getMeditationAssistant().getTimeToStopMeditate() == timestamp) {
                    txtDurationSeconds.setText(R.string.ignore_omkara);
                } else if (getMeditationAssistant().getTimeStartMeditate() < timestamp) {
                    txtDurationSeconds.setText(String.valueOf(duration % 60));
                } else {
                    if (delayRemaining >= 60) {
                        txtDurationSeconds.setText(String.format("%d",
                                (int) delayRemaining / 60)
                                + ":"
                                + String.format("%02d", delayRemaining % 60));
                    } else {
                        txtDurationSeconds.setText(String
                                .valueOf(delayRemaining % 60));
                    }
                }
            }
        } else {
            txtDurationSeconds.setText("");
            findViewById(R.id.btnMeditate).setEnabled(true);
            btnMeditate.setText(getString(R.string.meditate));
            getMeditationAssistant().setAlphaCompat(txtDurationSeconds, 1.0f);

            if (getMeditationAssistant().getTimerMode().equals("timed")) {
                txtTimer.setText(getMeditationAssistant().getPrefs().getString("timerHours", "0")
                        + ":"
                        + String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs().getString("timerMinutes", "15"))));
            } else if (getMeditationAssistant().getTimerMode().equals("endat")) {
                txtTimer.setText(getMeditationAssistant().getPrefs().getString("timerHoursEndAt", "0")
                        + ":"
                        + String.format("%02d", Integer.valueOf(getMeditationAssistant().getPrefs().getString("timerMinutesEndAt", "0"))));
                txtDurationSeconds.setText(getString(R.string.endAt).toLowerCase());
            } else {
                txtTimer.setText(getString(R.string.ignore_om));
            }
        }

        if (getMeditationAssistant().getEditingDuration()) {
            getMeditationAssistant().setAlphaCompat(txtDurationSeconds, 0f);
        } else {
            getMeditationAssistant().setAlphaCompat(txtDurationSeconds, 1f);
        }

        if (setDisabled) {
            btnMeditate.setText(getString(R.string.meditate));
            findViewById(R.id.btnMeditate).setEnabled(false);
        }

        updateTexts();
    }

    public void startRunnable() {
        if (getMeditationAssistant().getRunnableStopped()) {
            Log.d("MeditationAssistant", "Starting runnable");
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    handler.removeCallbacks(this);
                    boolean sessionEnding = false;

                    if (getMeditationAssistant().getScreenOff()) {
                        getMeditationAssistant().setRunnableStopped(true);
                        Log.d("MeditationAssistant",
                                "Screen off, stopping runnable...");
                    }

                    if (getMeditationAssistant().pendingNotificationAction
                            .equals("exit")) {
                        getMeditationAssistant().pendingNotificationAction = "";

                        if (getMeditationAssistant().getTimeStartMeditate() > 0) {
                            longPressMeditate(null);
                        }
                        finish();
                        return;
                    } else if (getMeditationAssistant().pendingNotificationAction
                            .equals("end")) {
                        getMeditationAssistant().pendingNotificationAction = "";
                        sessionEnding = true;

                        if (getMeditationAssistant().getTimeStartMeditate() > 0) {
                            longPressMeditate(new View(getApplicationContext()));
                        }
                    }

                    if (sessionEnding) {
                        getMeditationAssistant().setRunnableStopped(true);
                    } else if (getMeditationAssistant().getTimeToStopMeditate() == -1
                            || getMeditationAssistant().getTimeToStopMeditate() > (System
                            .currentTimeMillis() / 1000) || (getMeditationAssistant().getTimeToStopMeditate() != 0 && getMeditationAssistant().getPrefs().getBoolean("pref_softfinish", false))) {
                        updateMeditate(false, false);
                        if (!getMeditationAssistant().getScreenOff()) {
                            handler.postDelayed(this, 250);
                        }
                    } else {
                        Log.d("MeditationAssistant",
                                "Stopping - start:"
                                        + String.valueOf(getMeditationAssistant()
                                        .getTimeStartMeditate())
                                        + " stop:"
                                        + String.valueOf(getMeditationAssistant()
                                        .getTimeToStopMeditate())
                        );

                        getMeditationAssistant().setRunnableStopped(true);
                        if (getMeditationAssistant().getTimeToStopMeditate() != 0) {
                            getMeditationAssistant().setTimeToStopMeditate(0); // Don't trigger the last_reminder change unless necessary
                        }
                        updateMeditate(false, (getMeditationAssistant()
                                .getTimeStartMeditate() > 0));
                    }
                }
            };
            getMeditationAssistant().setRunnableStopped(false);
            handler.postDelayed(runnable, 100);
        } else {
            Log.d("MeditationAssistant",
                    "Not starting runnable.  Stopped flag is not set.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MeditationAssistant.REQUEST_FIT) {
            getMeditationAssistant().utility.googleAPIAuthInProgress = false;
            if (resultCode == RESULT_OK) {
                getMeditationAssistant().utility.onGoogleClientResult();
            }
        }
    }

    public void pressCommunity(View view) {
        if (sv != null && sv.isShown()) {
            try {
                sv.hide();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getMeditationAssistant().getMediNET().browseTo(this, "community");
    }

    public void pressMediNET(View view) {
        Log.d("MeditationAssistant", "Open medinet: " + getMeditationAssistant().getMediNETKey());

        if (sv != null && sv.isShown()) {
            try {
                sv.hide();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!getMeditationAssistant().getMediNETKey().equals("")) {
            if (!getMeditationAssistant().getMediNET().status.equals("success")) {
                getMeditationAssistant().getMediNET().connect();
            } else {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                getMeditationAssistant().getMediNET().signOut();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(
                        getResources().getDrawable(
                                getTheme().obtainStyledAttributes(
                                        getMeditationAssistant().getMATheme(),
                                        new int[]{R.attr.actionIconSignOut})
                                        .getResourceId(0, 0)
                        )
                )
                        .setTitle(
                                getString(R.string.signOut))
                        .setMessage(getString(R.string.signOutOfMediNETConfirmTitle))
                        .setPositiveButton(getString(R.string.signOut),
                                dialogClickListener)
                        .setNegativeButton(getString(R.string.cancel),
                                dialogClickListener).show();
            }
        } else {
            askToSignIn();
        }
    }

    public void askToSignIn() {
        getMeditationAssistant().startAuth(MainActivity.this, false);
    }

    public void resetScreenBrightness() {
        WindowManager.LayoutParams windowParams = getWindow()
                .getAttributes();
        windowParams.screenBrightness = -1;
        getWindow().setAttributes(windowParams);
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (sv != null && sv.isShown()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {
        if (!finishedTutorial) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        sv = null;
    }

    @Override
    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
        sv = null;
    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {

    }

    @Override
    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

    }
}
