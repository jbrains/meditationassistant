package sh.ftp.rocketninelabs.meditationassistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.multidex.MultiDex;

import ca.jbrains.meditationassistant.LocalDateJunkDrawer;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

@AcraCore(buildConfigClass = BuildConfig.class, reportFormat = StringFormat.KEY_VALUE_LIST)
@AcraHttpSender(httpMethod = HttpSender.Method.POST, uri = "https://medinet.rocketnine.space/acra/acra.php")
public class MeditationAssistant extends Application {
    public static String URL_ROCKETNINELABS = "https://rocketnine.space";
    public static String URL_MEDINET = "https://medinet.rocketnine.space";
    public static String URL_SOURCE = "https://gitlab.com/tslocum/meditationassistant";

    public static String ACTION_PRESET = "sh.ftp.rocketninelabs.meditationassistant.PRESET";
    public static String ACTION_REMINDER = "sh.ftp.rocketninelabs.meditationassistant.DAILY_NOTIFICATION";
    public static String ACTION_UPDATED = "sh.ftp.rocketninelabs.meditationassistant.DAILY_NOTIFICATION_UPDATED";

    public static int CSV_COLUMN_COUNT = 5;

    public static int sessionNotificationID = 1990;
    public static int bellNotificationID = 1991;

    public boolean ispaused = false;
    public long pausestart = 0;
    public long pausetime = 0;
    public int previousRingerFilter = -1;
    public int previousRingerMode = -1;
    public String pendingNotificationAction = "";
    public Boolean asktorate = false;
    public Boolean asktodonate = false;
    public DatabaseHandler db = null;
    public PendingIntent reminderPendingIntent = null;
    public String theme = null;
    public String marketName = null;
    public Integer previous_volume = null;
    private String appVersion = null;
    private long timeToStopMeditate = 0;
    private long timeStartMeditate = 0;
    public boolean hideConnectedMsg;
    private MediNET medinet = null;
    private boolean screenoff = false;
    private boolean runnablestopped = true;
    private boolean sessrunnablestopped = true;
    private String medinetkey = null;
    private String medinetprovider = null;
    private boolean connectedonce = false;
    private boolean editingduration = false;
    private Boolean rememberduration = null;
    private Integer meditationstreak = null;
    private long meditationstreakexpires = 0;
    public ArrayList<Integer> streaktime = new ArrayList<>();
    public long streakbuffer = -1;
    private long sessrunnablestarttime = 0;
    private boolean sesswassignedout = false;
    private long sessionduration = 0;
    private Integer webview_scale = null;
    private String timerMode = null;
    private SharedPreferences prefs = null;
    private AlarmManager am;
    private WakeLocker wakeLocker = new WakeLocker();
    private Lock wakeLockerLock = new ReentrantLock();
    String pausedTimerHoursMinutes;
    String pausedTimerSeconds;
    private HashMap<String, MediaPlayer> mediaPlayers = new HashMap<String, MediaPlayer>();

    private AlertDialog sessionDialog = null;
    // SMELL Is this truly nullable, or are we merely being paranoid?
    @Nullable
    private LocalDate sessionDialogStartedDate = null;
    private int sessionDialogStartedHour = -1;
    private int sessionDialogStartedMinute = -1;
    private int sessionDialogCompletedYear = -1;
    private int sessionDialogCompletedMonth = -1;
    private int sessionDialogCompletedDay = -1;
    private int sessionDialogCompletedHour = -1;
    private int sessionDialogCompletedMinute = -1;
    private boolean sessionDialogLengthSetManually = false;
    private int sessionDialogLengthHour = -1;
    private int sessionDialogLengthMinute = -1;
    // CONTRACT time in seconds (not milliseconds)
    private long sessionDialogUpdateSessionStarted = 0;
    private Activity sessionDialogActivity = null;
    private String sessionDialogCurrentOption = "";
    private Button sessionDialogStartedDateButton = null;
    private Button sessionDialogStartedTimeButton = null;
    private Button sessionDialogCompletedDateButton = null;
    private Button sessionDialogCompletedTimeButton = null;
    private Button sessionDialogLengthButton = null;
    private EditText sessionDialogMessage = null;
    private DatePickerDialog.OnDateSetListener sessionDialogDateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                // CONTRACT year, month, day values are compatible with java.util.Calendar
                @Override
                public void onDateSet(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                    // the onClick for the two DatePicker buttons sets this option
                    updateSessionIntervalDatesDependingOnWhichPartOfTheIntervalTheUserSelected(
                            LocalDateJunkDrawer.localDateFromJavaUtilCalendarComponentValues(year, monthOfYear, dayOfMonth)
                    );
                    // REFACTOR: eventually this becomes a SessionDialog class with an update method
                    updateSessionDialog();
                }
            };

    private void updateSessionIntervalDatesDependingOnWhichPartOfTheIntervalTheUserSelected(LocalDate selectedDate) {
        boolean isStartedModalDialog = sessionDialogCurrentOption.equals("started");

        // Does it make more sense to talk to the DatePicker View for some of this?
        // REFACTOR Replace with this.sessionStartedDate

        // REFACTOR Replace with this.sessionCompletedDate
        LocalDate maybeCompletedDate = interpretJavaUtilCalendarComponentValuesAsLocalDate(
                this.sessionDialogCompletedYear,
                this.sessionDialogCompletedMonth,
                this.sessionDialogCompletedDay
        );

        if (isStartedModalDialog) {
            // REFACTOR Move this behavior into a listener for the started button
            autocompleteSessionInterval(selectedDate, maybeCompletedDate);

        } else {
            // REFACTOR Move this behavior into a listener for the completed button
            normalizeSessionInterval(selectedDate);
        }
    }

    // normalize the interval, so that "start" is no later than "end"
    private void normalizeSessionInterval(LocalDate selectedDate) {
        this.sessionDialogStartedDate = (sessionDialogStartedDate != null)
                ? earliestOf(this.sessionDialogStartedDate, selectedDate)
                : selectedDate;
        
        writeSessionCompletedDate(selectedDate);
    }

    private void autocompleteSessionInterval(LocalDate selectedDate, LocalDate maybeCompletedDate) {
        // autofill the "completed date" to the selected "started date"
        this.sessionDialogStartedDate = selectedDate;
        writeSessionCompletedDate(maybeCompletedDate == null ? selectedDate : maybeCompletedDate);
    }

    private void writeSessionCompletedDate(LocalDate sessionCompletedDate) {
        if (sessionCompletedDate == null) {
            this.sessionDialogCompletedYear = -1;
            this.sessionDialogCompletedMonth = -1;
            this.sessionDialogCompletedDay = -1;
        } else {
            this.sessionDialogCompletedYear = sessionCompletedDate.getYear();
            this.sessionDialogCompletedMonth = sessionCompletedDate.getMonthValue() - 1;
            this.sessionDialogCompletedDay = sessionCompletedDate.getDayOfMonth();
        }
    }

    @Nullable
    private static LocalDate interpretJavaUtilCalendarComponentValuesAsLocalDate(int year, int month, int day) {
        return year == -1 || month == -1 || day == -1
                ? null
                : LocalDate.of(year, month + 1, day);
    }

    // REFACTOR Move into Happy Zone
    @NotNull
    private static LocalDate earliestOf(
            @NotNull LocalDate aDate,
            @NotNull LocalDate anotherDate
    ) {
        if (aDate.isAfter(anotherDate)) {
            return aDate;
        } else {
            return anotherDate;
        }
    }

    @NotNull
    private static List<Integer> localDateAsList(LocalDate date) {
        return Arrays.asList(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    private TimePickerDialog.OnTimeSetListener sessionDialogTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    if (sessionDialogCurrentOption.equals("started")) {
                        sessionDialogStartedHour = hourOfDay;
                        sessionDialogStartedMinute = minute;

                        if (sessionDialogCompletedHour == -1 && sessionDialogCompletedMinute == -1) {
                            sessionDialogCompletedHour = sessionDialogStartedHour;
                            sessionDialogCompletedMinute = sessionDialogStartedMinute;
                        }
                    } else if (sessionDialogCurrentOption.equals("length")) {
                        if (hourOfDay > 0 || minute > 0) {
                            sessionDialogLengthSetManually = true;
                            sessionDialogLengthHour = hourOfDay;
                            sessionDialogLengthMinute = minute;
                        } else {
                            sessionDialogLengthSetManually = false;
                            sessionDialogLengthHour = -1;
                            sessionDialogLengthMinute = -1;
                        }
                    } else {
                        sessionDialogCompletedHour = hourOfDay;
                        sessionDialogCompletedMinute = minute;
                    }

                    updateSessionDialog();
                }
            };

    SharedPreferences.OnSharedPreferenceChangeListener sharedPrefslistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences newprefs, String key) {
            if (key.equals("pref_meditationstreakbuffer")) {
                streakbuffer = -1;
                streaktime = new ArrayList<>();
            }
        }
    };

    public static void setAlphaCompat(View view, float alpha) {
        view.setAlpha(alpha);
    }

    public int dpToPixels(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    public int pixelsToDP(float px) {
        return (int) (px * getResources().getDisplayMetrics().density + 0.5f);
    }

    public String getMarketName() {
        if (marketName == null) {
            String osname = System.getProperty("os.name");
            if (osname != null && osname.equals("qnx")) {
                marketName = "bb"; // BlackBerry
            } else if (BuildConfig.FLAVOR.equals("opensource")) {
                marketName = "fdroid";
            } else { // To be uncommented based upon target market
                marketName = "google";
                //marketName = "amazon";
                //marketName = "getjar";
                //marketName = "slideme";
            }
        }

        return marketName;
    }

    public String capitalizeFirst(String string) {
        if (string == null) {
            return "";
        }

        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public int audioStream() {
        switch (getPrefs().getString("pref_audio_stream", "")) {
            case "media":
                return AudioManager.STREAM_MUSIC;
            case "ringtone":
                return AudioManager.STREAM_RING;
            case "notification":
                return AudioManager.STREAM_NOTIFICATION;
            default:
                return AudioManager.STREAM_ALARM;
        }
    }

    public int audioUsage() {
        switch (getPrefs().getString("pref_audio_stream", "")) {
            case "media":
                return AudioAttributes.USAGE_MEDIA;
            case "ringtone":
                return AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
            case "notification":
                return AudioAttributes.USAGE_NOTIFICATION;
            default:
                return AudioAttributes.USAGE_ALARM;
        }
    }

    public void restoreVolume() {
        if (previous_volume != null) {
            try {
                AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                mAudioManager.setStreamVolume(audioStream(), previous_volume, 0);
            } catch (java.lang.SecurityException e) {
                // Do nothing
            }
            previous_volume = null;
        }
    }

    public void connectOnce() {
        if (!connectedonce) {
            getMediNET().connect();
            connectedonce = true;
        }
    }

    public void rateApp() {
        if (getMarketName().equals("bb")) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("https://appworld.blackberry.com/webstore/content/" + (BuildConfig.FLAVOR.equals("free") ? "59939924" : "59939922") + "/")), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (getMarketName().equals("google")) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (getMarketName().equals("amazon")) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + getApplicationContext().getPackageName())), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public AlarmManager getAlarmManager() {
        if (am == null) {
            am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }

        return am;
    }

    public void setAlarm(boolean allowAlarmClock, long triggerAtMillis, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= 21 && allowAlarmClock) {
            getAlarmManager().setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, PendingIntent.getActivity(this, 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT)), pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            getAlarmManager().set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public void setNotificationControl() {
        previousRingerFilter = getRingerFilter();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        previousRingerMode = audioManager.getRingerMode();

        if ((getPrefs().getString("pref_notificationcontrol", "").equals("priority") || getPrefs().getString("pref_notificationcontrol", "").equals("alarms")) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!haveNotificationPermission()) {
                return;
            }

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.setInterruptionFilter(getPrefs().getString("pref_notificationcontrol", "").equals("priority") ? NotificationManager.INTERRUPTION_FILTER_PRIORITY : NotificationManager.INTERRUPTION_FILTER_ALARMS);
        } else if (getPrefs().getString("pref_notificationcontrol", "").equals("vibrate")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                int currentfilter = mNotificationManager.getCurrentInterruptionFilter();
                if (!haveNotificationPermission() && currentfilter != 0 && currentfilter != 1) {
                    return;
                }
            }

            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }
    }

    @SuppressLint("WrongConstant")
    public void unsetNotificationControl() {
        if (previousRingerFilter > 0 && (getPrefs().getString("pref_notificationcontrol", "").equals("priority") || getPrefs().getString("pref_notificationcontrol", "").equals("alarms")) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!haveNotificationPermission()) {
                return;
            }

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.setInterruptionFilter(previousRingerFilter);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int currentfilter = mNotificationManager.getCurrentInterruptionFilter();
            if (!haveNotificationPermission() && currentfilter != 0 && currentfilter != 1) {
                return;
            }
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (previousRingerMode >= 0 && getPrefs().getString("pref_notificationcontrol", "").equals("vibrate")) {
            audioManager.setRingerMode(previousRingerMode);
        }

        previousRingerFilter = -1;
        previousRingerMode = -1;
    }

    public int getRingerFilter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!haveNotificationPermission()) {
                return 0;
            }

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            return mNotificationManager.getCurrentInterruptionFilter();
        }

        return 0;
    }

    public boolean getEditingDuration() {
        return editingduration;
    }

    public void setEditingDuration(boolean bool) {
        editingduration = bool;
    }

    public int getMATextColor(Boolean enabled) {
        if (enabled) {
            if (!getMAThemeString().equals("dark") && !getMAThemeString().equals("buddhism")) {
                return android.R.color.primary_text_light;
            } else {
                return android.R.color.primary_text_dark;
            }
        } else {
            if (!getMAThemeString().equals("dark") && !getMAThemeString().equals("buddhism")) {
                return android.R.color.secondary_text_light;
            } else {
                return android.R.color.secondary_text_dark;
            }
        }
    }

    public int getMATheme() {
        return getMATheme(false);
    }

    public int getMATheme(Boolean dialogue) {
        if (theme == null) {
            theme = getPrefs().getString("pref_theme", "dark");
        }

        if (theme.equals("buddhism")) {
            return dialogue ? R.style.BuddhismDialogue : R.style.Buddhism;
        }

        if (dialogue) {
            if (!theme.equals("dark")) {
                return R.style.MeditationLightDialogTheme;
            } else {
                return R.style.MeditationDarkDialogTheme;
            }
        } else {
            if (theme.equals("light")) {
                return R.style.MeditationLightTheme;
            } else if (theme.equals("lightdark")) {
                return R.style.MeditationLightDarkTheme;
            } else {
                return R.style.MeditationDarkTheme;
            }
        }
    }

    public String getMAThemeString() {
        if (theme == null) {
            theme = getPrefs().getString("pref_theme", "dark");
        }

        return theme;
    }

    public MediNET getMediNET() {
        if (medinet == null) {
            //
        }
        return medinet;
    }

    public void setMediNET(MediNET medi) {
        medinet = medi;
        Log.d("MeditationAssistant", "Session: " + medinet);
    }

    public String getMediNETKey() {
        Log.d("MeditationAssistant", "getMediNETKey() - medinetkey: " + medinetkey + " prefs: " + getPrefs().getString("key", ""));
        if (medinetkey == null) {
            medinetkey = getPrefs().getString("key", "");
        }
        return medinetkey;
    }

    public String getMediNETProvider() {
        if (medinetprovider == null) {
            medinetprovider = getPrefs().getString("provider", "");
        }
        if (medinetprovider.trim().equals("")) {
            return "Unknown";
        }
        return medinetprovider;
    }

    public void cacheSound(int soundresource, String soundpath) {
        String cacheKey = soundpath;
        if (cacheKey.equals("")) {
            cacheKey = Integer.toString(soundresource);
        }
        if (mediaPlayers.containsKey(cacheKey)) {
            return;
        }

        MediaPlayer mp = new MediaPlayer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(audioUsage())
                    .build();
            mp.setAudioAttributes(audioAttributes);
        } else {
            mp.setAudioStreamType(audioStream());
        }
        try {
            if (!soundpath.equals("")) {
                mp.setDataSource(getApplicationContext(), Uri.parse(soundpath));
            } else {
                mp.setDataSource(getApplicationContext(), Uri.parse("android.resource://" + getPackageName() + "/" + soundresource));
            }

            mp.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();

            String soundLabel = soundpath;
            if (soundLabel.equals("")) {
                soundLabel = String.valueOf(soundresource);
            }
            Log.e("MeditationAssistant", "Failed to load sound: " + soundLabel);
            return;
        }

        mediaPlayers.put(cacheKey, mp);
    }

    public void cacheSessionSounds() {
        String label;
        for (int i = 0; i < 4; i++) {
            switch (i) {
                case 0:
                    label = "start";
                    break;
                case 1:
                    label = "interval";
                    break;
                case 2:
                    label = "finish";
                    break;
                case 3:
                    label = "bell";
                    break;
                default:
                    return;
            }

            SharedPreferences prefs = getPrefs();
            String soundPath = prefs.getString("pref_meditation_sound_" + label, "");
            if (!soundPath.equals("none")) {
                if (soundPath.equals("custom")) {
                    cacheSound(0, prefs.getString("pref_meditation_sound_" + label + "_custom", ""));
                } else {
                    cacheSound(MeditationSounds.getMeditationSound(soundPath), "");
                }
            }
        }
    }

    public void clearSoundCache() {
        for (MediaPlayer mp : mediaPlayers.values()) {
            try {
                mp.stop();
            } catch (Exception e) {
                // Do nothing
            }
            try {
                mp.release();
            } catch (Exception e) {
                // Do nothing
            }
        }

        mediaPlayers.clear();
    }

    public void playSound(int soundresource, String soundpath, boolean restoreVolume) {
        String wakeLockID = acquireWakeLock(false);

        String cacheKey = soundpath;
        if (cacheKey.equals("")) {
            cacheKey = Integer.toString(soundresource);
        }
        if (!mediaPlayers.containsKey(cacheKey)) {
            cacheSound(soundresource, soundpath);
        }
        if (!mediaPlayers.containsKey(cacheKey)) {
            Log.d("MeditationAssistant", "Failed to cache sound");
            return; // Failed to load sound
        }
        MediaPlayer mp = mediaPlayers.get(cacheKey);

        if (mp.isPlaying()) {
            Log.d("MeditationAssistant", "Failed to play sound: already playing");
            releaseWakeLock(wakeLockID);
            return;
        }

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (restoreVolume) {
                    MeditationAssistant.this.restoreVolume();
                }

                try {
                    mp.stop();
                } catch (Exception e) {
                    // Do nothing
                }
                try {
                    mp.prepareAsync();
                } catch (Exception e) {
                    // Do nothing
                }

                MeditationAssistant.this.releaseWakeLock(wakeLockID);
            }
        });

        try {
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();

            if (restoreVolume) {
                restoreVolume();
            }

            releaseWakeLock(wakeLockID);
        }
    }

    public void notifySession(int phase, boolean skipVibration, boolean restoreVolume) {
        String label;
        switch (phase) {
            case 0:
                label = "start";
                break;
            case 1:
                label = "interval";
                break;
            case 2:
                label = "finish";
                break;
            case 3:
                label = "bell";
                break;
            default:
                return;
        }
        SharedPreferences prefs = getPrefs();

        // Play sound
        String soundPath = prefs.getString("pref_meditation_sound_" + label, "");
        if (!soundPath.equals("none")) {
            if (soundPath.equals("custom")) {
                playSound(0, prefs.getString("pref_meditation_sound_" + label + "_custom", ""), restoreVolume);
            } else {
                playSound(MeditationSounds.getMeditationSound(soundPath), "", restoreVolume);
            }
        }

        // Vibrate device
        if (!skipVibration) {
            String vibration = prefs.getString("pref_meditation_vibrate_" + label, "");
            if (!vibration.isEmpty()) {
                if (vibration.equals("custom")) {
                    vibrateDevice(prefs.getString("pref_meditation_vibrate_" + label + "_custom", ""));
                } else {
                    vibrateDevice(vibration);
                }
            }
        }
    }

    public void startAuth(Context context, boolean showToast) {
        String trace = Arrays.toString(Thread.currentThread().getStackTrace());
        Log.d("MeditationAssistant", "startAuth called, current stack trace: " + trace);
        hideConnectedMsg = !showToast;

        if (showToast) {
            shortToast(getString(R.string.signInToMediNET));
        }

        AsyncTask.execute(() -> {
            AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/v2/auth") /* auth endpoint */,
                    Uri.parse("https://www.googleapis.com/oauth2/v4/token") /* token endpoint */
            );

            String clientId = BuildConfig.GOOGLEOAUTHKEY;
            Uri redirectUri = Uri.parse(BuildConfig.APPLICATION_ID + ":/oauth");
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    ResponseTypeValues.CODE,
                    redirectUri
            );
            builder.setScopes("profile");
            AuthorizationRequest request = builder.build();

            AuthorizationService authorizationService = new AuthorizationService(context);

            PendingIntent authIntent = PendingIntent.getActivity(MeditationAssistant.this, 0, new Intent(context, AuthResultActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_CANCEL_CURRENT);

            authorizationService.performAuthorizationRequest(
                    request,
                    authIntent,
                    authIntent);
        });
    }

    public ArrayList<Integer> getStreakBufferTime() {
        if (streaktime.isEmpty()) {
            String[] bufferSplit = getPrefs().getString("pref_meditationstreakbuffer", "4:00").split(":");
            streaktime.add(Integer.valueOf(bufferSplit[0]));
            streaktime.add(Integer.valueOf(bufferSplit[1]));
        }

        return streaktime;
    }

    public long getStreakBufferSeconds() {
        if (streakbuffer < 0) {
            ArrayList<Integer> streakbuffertime = getStreakBufferTime();
            streakbuffer = (streakbuffertime.get(0) * 3600) + (streakbuffertime.get(1) * 60);
        }

        return streakbuffer;
    }

    public void recalculateMeditationStreak(Activity activity) {
        Calendar dayCalendar = new GregorianCalendar();
        Integer daysback = 0;
        Integer recalculatedstreak = 0;
        Boolean sessionexiststoday = false;

        while (true) {
            Log.d("MeditationAssistant", "Checking (" + daysback + ") " + dayCalendar);
            if (db.numSessionsByDate(dayCalendar) > 0) {
                recalculatedstreak++;

                if (daysback == 0) {
                    sessionexiststoday = true;
                }
            } else if (daysback > 0) {
                break;
            }

            daysback++;
            dayCalendar.add(Calendar.DATE, -1);
        }

        Long currentStreak = getMeditationStreak().get(0);
        if (currentStreak < recalculatedstreak) {
            setMeditationStreak(recalculatedstreak, sessionexiststoday ? getStreakExpiresTwoDaysTimestamp() : getStreakExpiresOneDayTimestamp());
        } else if (currentStreak > recalculatedstreak) {
            showStreakDifferenceWarning(currentStreak.intValue(), recalculatedstreak, sessionexiststoday, activity);
        }
    }

    public ArrayList<Long> getMeditationStreak() {
        long timestamp = System.currentTimeMillis() / 1000;

        if (meditationstreak == null || meditationstreakexpires < 1) {
            meditationstreak = getPrefs().getInt("meditationstreak", 0);
            meditationstreakexpires = getPrefs().getLong("meditationstreakexpires", 0);
        }

        if (meditationstreakexpires > 0 && meditationstreakexpires < timestamp) {
            // Streak window has passed
            meditationstreak = 0;
            meditationstreakexpires = 0;

            getPrefs().edit().putInt("meditationstreak", meditationstreak)
                    .putLong("meditationstreakexpires", meditationstreakexpires)
                    .putBoolean("meditationstreakwarningshown", false).apply();
        }

        ArrayList<Long> streak = new ArrayList<>();
        streak.add((long) meditationstreak);
        streak.add(meditationstreakexpires);
        return streak;
    }

    public void addMeditationStreak() {
        addMeditationStreak(true);
    }

    public void addMeditationStreak(Boolean twodays) {
        ArrayList<Long> streak = getMeditationStreak();
        Long streakday = streak.get(0);
        Long streakexpires = streak.get(1);
        Log.d("MeditationAssistant", "addMeditationStreak() - Streak: " + streakday + " Expires: in " + (streakexpires - getTimestamp()) + " seconds");
        if (streakday == 0 || streakexpires - getTimestamp() < 86400) {
            streakday++;

            if (twodays) {
                setMeditationStreak(streakday.intValue(),
                        getStreakExpiresTwoDaysTimestamp());
            } else {
                setMeditationStreak(streakday.intValue(),
                        getStreakExpiresOneDayTimestamp());
            }
        }
    }

    public int getLongestMeditationStreak() {
        return getPrefs().getInt("longeststreak", 0);
    }

    public void setLongestMeditationStreak(int ms) {
        getPrefs().edit().putInt("longeststreak", ms).apply();
    }

    public long getStreakExpiresOneDayTimestamp() {
        Calendar c_midnight_oneday = new GregorianCalendar();
        c_midnight_oneday.setTime(new Date());
        c_midnight_oneday.set(Calendar.HOUR_OF_DAY, 0);
        c_midnight_oneday.set(Calendar.MINUTE, 0);
        c_midnight_oneday.set(Calendar.SECOND, 0);
        c_midnight_oneday.set(Calendar.MILLISECOND, 0);
        c_midnight_oneday.add(Calendar.DATE, 1); // One day

        return (c_midnight_oneday.getTimeInMillis() / 1000) + getStreakBufferSeconds();
    }

    public long getStreakExpiresTwoDaysTimestamp() {
        Calendar c_midnight_twodays = new GregorianCalendar();
        c_midnight_twodays.setTime(new Date());
        c_midnight_twodays.set(Calendar.HOUR_OF_DAY, 0);
        c_midnight_twodays.set(Calendar.MINUTE, 0);
        c_midnight_twodays.set(Calendar.SECOND, 0);
        c_midnight_twodays.set(Calendar.MILLISECOND, 0);
        c_midnight_twodays.add(Calendar.DATE, 2); // Two days

        return (c_midnight_twodays.getTimeInMillis() / 1000) + getStreakBufferSeconds();
    }

    public void notifySessionsUpdated() {
        Log.d("MeditationAssistant", "Sending session update notification");
        getPrefs().edit().putLong("sessionsupdate", getTimestamp()).apply();
    }

    public void notifyMediNETUpdated() {
        Log.d("MeditationAssistant", "Sending MediNET update notification");
        getPrefs().edit().putLong("medinetupdate", getTimestamp()).apply();
    }

    public Integer timePreferenceValueToSeconds(String timePreferenceValue, String defaultValue) {
        try {
            String[] timeValueSplit = ((timePreferenceValue != null && timePreferenceValue != "") ? timePreferenceValue : defaultValue).split(":");
            return (Integer.valueOf(timeValueSplit[0]) * 60) + Integer.valueOf(timeValueSplit[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public ArrayList<String> formatDuration(String duration) {
        if (!duration.equals("")) {
            String[] duration_separated = duration.split(":");
            String newHours = "-1";
            String newMinutes = "-1";

            if (duration_separated.length > 1) {
                if (!duration_separated[0].trim().equals("")) {
                    try {
                        newHours = String.valueOf(Integer
                                .valueOf(duration_separated[0]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    newHours = "0";
                }
                if (!duration_separated[1].trim().equals("")) {
                    try {
                        newMinutes = String.valueOf(Integer
                                .valueOf(duration_separated[1]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    newMinutes = "0";
                }
            } else {
                try {
                    newMinutes = String.valueOf(Integer.valueOf(duration));
                    newHours = "0";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (Integer.valueOf(newMinutes) >= 60) {
                newHours = String.format("%d",
                        (int) (Integer.valueOf(newHours) + Math.floor(Integer
                                .valueOf(newMinutes) / 60))
                );
                newMinutes = String.format("%02d",
                        Integer.valueOf(newMinutes) % 60);
            }

            if (!newHours.equals("-1") && !newMinutes.equals("-1")
                    && !(newHours.equals("0") && newMinutes.equals("0"))) {
                ArrayList<String> formatted_duration = new ArrayList<>();
                formatted_duration.add(newHours);
                formatted_duration.add(String.format("%02d",
                        Integer.valueOf(newMinutes)));

                return formatted_duration;
            }
        }

        return null;
    }

    public ArrayList<String> formatDurationEndAt(String duration) {
        if (!duration.equals("")) {
            String[] duration_separated = duration.split(":");
            String newHours = "-1";
            String newMinutes = "-1";
            Boolean shorthand = false;

            if (duration_separated.length > 1) {
                if (!duration_separated[0].trim().equals("")) {
                    try {
                        newHours = String.valueOf(Integer
                                .valueOf(duration_separated[0]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    newHours = "0";
                }
                if (!duration_separated[1].trim().equals("")) {
                    try {
                        newMinutes = String.valueOf(Integer
                                .valueOf(duration_separated[1]));
                        if (duration_separated[1].trim().length() < 2) {
                            shorthand = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    newMinutes = "0";
                }
            } else {
                try {
                    newHours = String.valueOf(Integer.valueOf(duration));
                    newMinutes = "0";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Log.d("MeditationAssistant", "Formatted end at duration newMinutes: " + newMinutes);
            if (shorthand && Integer.valueOf(newMinutes) > 0 && Integer.valueOf(newMinutes) <= 5) { // shorthand 3 -> 30, 4 -> 40...
                newMinutes = String.format("%02d",
                        Integer.valueOf(newMinutes) * 10);
            } else if (Integer.valueOf(newMinutes) >= 60) {
                newHours = String.format("%d",
                        (int) (Integer.valueOf(newHours) + Math.floor(Integer
                                .valueOf(newMinutes) / 60))
                );
                newMinutes = String.format("%02d",
                        Integer.valueOf(newMinutes) % 60);
            }

            if (Integer.valueOf(newHours) == 0 || Integer.valueOf(newHours) > 24) {
                return null; // Invalid hour for time
            } else if (Integer.valueOf(newHours) > 12) {
                newHours = String.valueOf(Integer.valueOf(newHours) - 12); // Subtract 12 hours if 24 hour time was provided
            }

            if (!newHours.equals("-1") && !newMinutes.equals("-1")
                    && !(newHours.equals("0") && newMinutes.equals("0"))) {
                ArrayList<String> formatted_duration = new ArrayList<>();
                formatted_duration.add(newHours);
                formatted_duration.add(String.format("%02d",
                        Integer.valueOf(newMinutes)));

                return formatted_duration;
            }
        }

        return null;
    }

    public SharedPreferences getPrefs() {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        }
        return prefs;
    }

    public boolean getRememberDuration() {
        if (rememberduration == null) {
            rememberduration = getPrefs().getBoolean("pref_rememberlasttimer", true);
        }
        return rememberduration;
    }

    public boolean getRunnableStopped() {
        return runnablestopped;
    }

    public void setRunnableStopped(boolean bool) {
        runnablestopped = bool;
    }

    public boolean getScreenOff() {
        return screenoff;
    }

    public void setScreenOff(boolean bool) {
        screenoff = bool;
    }

    public long getSessionDuration() {
        return sessionduration;
    }

    public void setSessionDuration(long duration) {
        sessionduration = duration;
    }

    public long getSessionsRunnableStartTime() {
        return sessrunnablestarttime;
    }

    public void setSessionsRunnableStartTime(long l) {
        sessrunnablestarttime = l;
    }

    public boolean getSessionsRunnableStopped() {
        return sessrunnablestopped;
    }

    public void setSessionsRunnableStopped(boolean bool) {
        sessrunnablestopped = bool;
    }

    public boolean getSessionsRunnableWasSignedOut() {
        return sesswassignedout;
    }

    public void setSessionsRunnableWasSignedOut(boolean bool) {
        sesswassignedout = bool;
    }

    public Long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    public long getTimeStartMeditate() {
        return timeStartMeditate;
    }

    public void setTimeStartMeditate(long l) {
        timeStartMeditate = l;
        pendingNotificationAction = "";
        // Log.d("MeditationAssistant", String.valueOf(timeToStopMeditate));
    }

    public long getTimeToStopMeditate() {
        return timeToStopMeditate;
    }

    public void setTimeToStopMeditate(long l) {
        timeToStopMeditate = l;
        getPrefs().edit().putLong("last_reminder", getTimestamp()).apply();
        //(new Exception()).printStackTrace();
        Log.d("MeditationAssistant", "Setting time to stop meditating: "
                + timeToStopMeditate);
    }

    public String getTimerMode() {
        if (timerMode == null) {
            timerMode = getPrefs().getString("pref_timer_mode", "timed");
        }
        return timerMode;
    }

    public void setTimerMode(String tm) {
        timerMode = tm;

        getPrefs().edit().putString("pref_timer_mode", timerMode).apply();
    }

    public Integer getWebViewScale() {
        if (webview_scale == null) {
            webview_scale = getPrefs().getInt("webviewscale", 100);
        }
        return webview_scale;
    }

    public void setWebViewScale(int scale) {
        webview_scale = scale;

        getPrefs().edit().putInt("webviewscale", webview_scale).apply();
    }

    public void hideBellNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(bellNotificationID);
    }

    public void hideSessionNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(sessionNotificationID);
    }

    public void longToast(String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        Integer applaunches = getPrefs().getInt("applaunches", 0) + 1;
        if (applaunches == 1) {
            getPrefs().edit().putBoolean("askedtodonate156", true).apply();
        } else if (!getPrefs().getBoolean("askedtodonate156", false)) {
            asktodonate = true;
            getPrefs().edit().putBoolean("askedtodonate156", true).apply();
        }
        getPrefs().edit().putInt("applaunches", applaunches).apply();

        Log.d("MeditationAssistant",
                "Meditation Assistant running (" + applaunches + " launches) on API level "
                        + Build.VERSION.SDK_INT
        );

        if (Build.VERSION.SDK_INT >= 23) {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(this, FilePickerActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }

        if (getPrefs().getBoolean("pref_vibrate", false)) {
            getPrefs()
                    .edit()
                    .putString("pref_meditation_vibrate_start", "medium")
                    .putString("pref_meditation_vibrate_interval", "medium")
                    .putString("pref_meditation_vibrate_finish", "medium")
                    .putBoolean("pref_vibrate", false)
                    .apply();
        }

        getPrefs().registerOnSharedPreferenceChangeListener(sharedPrefslistener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels();
        }

        // Reset timer to default values
        if (!getPrefs().getBoolean("pref_rememberlasttimer", true)) {
            SharedPreferences.Editor editor = getPrefs().edit();
            editor.putString("timerHours",
                    getPrefs().getString("timerDefaultHours", "15"));
            editor.putString("timerMinutes",
                    getPrefs().getString("timerDefaultMinutes", "15"));
            editor.apply();
        }

        // Upgrade to the new full screen preference
        try {
            boolean oldFullScreenPref = getPrefs().getBoolean("pref_full_screen", false);

            // We didn't encounter an exception, upgrade the preference
            String newFullScreenPref = "";
            if (oldFullScreenPref) {
                newFullScreenPref = "session";
            }

            getPrefs().edit().remove("pref_full_screen").apply();
            getPrefs().edit().putString("pref_full_screen", newFullScreenPref).apply();
        } catch (Exception e) {
            // Do nothing
        }

        db = DatabaseHandler.getInstance(getApplicationContext());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            /* Send the daily notification updated intent just in case the receiver hasn't been called yet */
            Log.d("MeditationAssistant", "Sending initial daily notification updated intent");
            Intent intent = new Intent();
            intent.setAction(MeditationAssistant.ACTION_UPDATED);
            sendBroadcast(intent);
        }
    }

    public String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public void setMediNETKey(String key, String provider) {
        Log.d("MeditationAssistant", "Setting medinet key: " + key + " - " + provider);
        medinetkey = key;
        medinetprovider = provider;
        long timestamp = System.currentTimeMillis() / 1000;

        SharedPreferences.Editor editor = getPrefs().edit();
        if (getPrefs().getBoolean("pref_rememberme", true)) {
            Log.d("MeditationAssistant", "Storing medinet key: " + key + " - " + provider);
            editor.putString("key", key);
            editor.putString("provider", provider);
        }
        if (!key.equals("")) {
            editor.putString("keyupdate", String.valueOf(timestamp));
        }
        editor.apply();
    }

    public void setMeditationStreak(Integer ms, long expires) {
        meditationstreak = ms;
        meditationstreakexpires = expires;

        Calendar date = new GregorianCalendar();
        date.setTimeZone(TimeZone.getDefault());
        long timestamp = date.getTimeInMillis() / 1000;
        Log.d("MeditationAssistant",
                "Streak: " + meditationstreak + ", expires: "
                        + expires + " (in "
                        + (expires - timestamp) + " seconds)"
        );

        getPrefs().edit().putInt("meditationstreak", meditationstreak).putLong("meditationstreakexpires", meditationstreakexpires).apply();

        if (meditationstreak == 1) {
            getPrefs().edit().putBoolean("meditationstreakwarningshown", false).apply();
        }
        if (meditationstreak > getLongestMeditationStreak()) {
            setLongestMeditationStreak(meditationstreak);
        }

        updateWidgets();

        /* Update all widgets */
        Intent update_widgets = new Intent(getApplicationContext(), WidgetStreakProvider.class);
        update_widgets.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        sendBroadcast(update_widgets);
    }

    public void pauseSession() {
        pausestart = getTimestamp();
        ispaused = true;
    }

    public long unPauseSession() {
        if (!ispaused) {
            return 0;
        }

        long thispausetime;
        if (getTimerMode().equals("endat")) {
            thispausetime = Math.min(getTimeToStopMeditate(), getTimestamp()) - pausestart;
        } else {
            thispausetime = Math.max(0, getTimestamp() - pausestart);
        }
        pausetime += thispausetime;

        Log.d("MeditationAssistant", "PAUSE: Un-paused.  Paused for " + thispausetime + " seconds (" + pausetime + " total)");

        ispaused = false;

        return thispausetime;
    }

    public void shortToast(String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        });
    }

    public AlertDialog showAnnouncementDialog(String title) {
        try {
            Looper.prepare();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (getMediNET().activity != null
                && !getMediNET().announcement.equals("")) {
            AlertDialog announceDialog = new AlertDialog.Builder(getMediNET().activity)
                    .setPositiveButton(R.string.dismiss,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {

                                }
                            }
                    )
                    .setTitle(title == null ? getString(R.string.announcement) : title)
                    .setMessage(medinet.announcement)
                    .setIcon(getMediNET().activity.getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{R.attr.actionIconGoToToday}).getResourceId(0, 0)))
                    .setCancelable(false)
                    .create();

            announceDialog.show();
            return announceDialog;
        }

        return null;
    }

    public void showDonationDialog(Activity activity) {
        AlertDialog donateDialog = new AlertDialog.Builder(activity)
                .setTitle(getString(R.string.donate))
                .setMessage(R.string.chooseDonationMethod)
                .setPositiveButton("Liberapay",
                        (dialog, id) -> startActivity(new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://liberapay.com/~968545")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                .setNegativeButton("PayPal",
                        (dialog, id) -> startActivity(new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(MeditationAssistant.URL_ROCKETNINELABS + "/donate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                .create();

        donateDialog.show();
    }

    public void showStreakDifferenceWarning(int oldstreak, int newstreak, boolean twodays, Activity activity) {
        try {
            Looper.prepare();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (activity == null) {
            return;
        }

        if (getPrefs().getBoolean("meditationstreakwarningshown", false)) {
            return;
        }
        getPrefs().edit().putBoolean("meditationstreakwarningshown", true).apply();

        AlertDialog streakDifferenceDialog = new AlertDialog.Builder(activity)
                .setPositiveButton(R.string.yes,
                        (dialog, id) -> {
                            setMeditationStreak(newstreak, twodays ? getStreakExpiresTwoDaysTimestamp() : getStreakExpiresOneDayTimestamp());
                        }
                )
                .setNegativeButton(R.string.no,
                        (dialog, id) -> {
                            // Do nothing
                        }
                )
                .setTitle(R.string.warning)
                .setMessage(String.format(getString(R.string.streakdifferencewarning), oldstreak, newstreak))
                .setIcon(activity.getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{R.attr.actionIconGoToToday}).getResourceId(0, 0)))
                .setCancelable(false)
                .create();

        streakDifferenceDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotificationChannels() {
        NotificationChannel sessionChannel = new NotificationChannel("session", getString(R.string.session), NotificationManager.IMPORTANCE_LOW);
        sessionChannel.enableLights(false);
        sessionChannel.enableVibration(false);

        NotificationChannel bellChannel = new NotificationChannel("bell", getString(R.string.session), NotificationManager.IMPORTANCE_LOW);
        bellChannel.enableLights(false);
        bellChannel.enableVibration(false);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(sessionChannel);
        notificationManager.createNotificationChannel(bellChannel);
    }

    public void showMindfulnessBellNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("notification");
        // intent.putExtra("notificationButton", "notification");
        // intent.putExtra("notificationButton", "");
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent3 = new Intent(this, MainActivity.class);
        // intent3.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // intent3.putExtra("notificationButton", "end");
        intent3.setAction("notificationEndBell");
        PendingIntent pIntentEnd = PendingIntent.getActivity(this, 0, intent3,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.mindfulnessBellActive))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pIntent)
                .addAction(R.drawable.ic_action_stop,
                        getString(R.string.deactivate), pIntentEnd);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId("bell");
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(bellNotificationID, notificationBuilder.build());
    }

    public void showSessionNotification() {
        if (!getPrefs().getBoolean("pref_notification", true)
                || getTimeStartMeditate() < 1) {
            hideSessionNotification();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("notification");
        // intent.putExtra("notificationButton", "notification");
        // intent.putExtra("notificationButton", "");
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent2 = new Intent(this, MainActivity.class);
        // intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent2.setAction("notificationPause");
        PendingIntent pIntentPause = PendingIntent.getActivity(this, 0, intent2,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent3 = new Intent(this, MainActivity.class);
        // intent3.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // intent3.putExtra("notificationButton", "end");
        intent3.setAction("notificationEnd");
        PendingIntent pIntentEnd = PendingIntent.getActivity(this, 0, intent3,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String streaktext = "";
        if (getMeditationStreak().get(0) > 1) {
            streaktext = String.valueOf(getMeditationStreak().get(0));
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(!ispaused ? R.string.sessionInProgress : R.string.sessionPaused))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentInfo(streaktext)
                .setContentIntent(pIntent)
                .addAction(R.drawable.ic_action_pause,
                        getString(!ispaused ? R.string.pause : R.string.resume), pIntentPause)
                .addAction(R.drawable.ic_action_stop,
                        getString(R.string.end), pIntentEnd);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId("session");
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(sessionNotificationID, notificationBuilder.build());
    }

    public void showSessionDialog(final SessionSQL session, Activity activity) {
        if (sessionDialog != null) {
            try {
                if (sessionDialog.isShowing()) {
                    sessionDialog.dismiss();
                }
            } catch (Exception e) {
                // Activity is not in the foreground
            }
        }

        if (getTimeStartMeditate() > 0) {
            shortToast(getString(session._id == 0 ? R.string.addSessionMeditating : R.string.editSessionMeditating));
            return;
        }

        sessionDialogUpdateSessionStarted = 0;
        sessionDialogActivity = activity;

        unsetStartedDate();
        sessionDialogStartedHour = -1;
        sessionDialogStartedMinute = -1;

        unsetCompletedDate();
        sessionDialogCompletedHour = -1;
        sessionDialogCompletedMinute = -1;

        sessionDialogLengthSetManually = false;
        sessionDialogLengthHour = -1;
        sessionDialogLengthMinute = -1;

        View sessionDialogView = LayoutInflater.from(sessionDialogActivity).inflate(R.layout.session_dialog, sessionDialogActivity.findViewById(R.id.sessionDialog));
        sessionDialogStartedDateButton = sessionDialogView.findViewById(R.id.sessionDialogSetDateStarted);
        sessionDialogStartedTimeButton = sessionDialogView.findViewById(R.id.sessionDialogSetTimeStarted);
        sessionDialogCompletedDateButton = sessionDialogView.findViewById(R.id.sessionDialogSetDateCompleted);
        sessionDialogCompletedTimeButton = sessionDialogView.findViewById(R.id.sessionDialogSetTimeCompleted);
        sessionDialogLengthButton = sessionDialogView.findViewById(R.id.sessionDialogSetLength);
        sessionDialogMessage = sessionDialogView.findViewById(R.id.sessionDialogSetMessage);

        if (session._id > 0) {
            sessionDialogUpdateSessionStarted = session._started;

            Calendar c_session_started = Calendar.getInstance();
            c_session_started.setTimeInMillis(session._started * 1000);
            this.sessionDialogStartedDate = LocalDateJunkDrawer.localDateFromTimeInSeconds(sessionDialogUpdateSessionStarted);
            sessionDialogStartedHour = c_session_started.get(Calendar.HOUR_OF_DAY);
            sessionDialogStartedMinute = c_session_started.get(Calendar.MINUTE);

            Calendar c_session_completed = Calendar.getInstance();
            c_session_completed.setTimeInMillis(session._completed * 1000);
            writeSessionCompletedDate(LocalDateJunkDrawer.localDateFromJavaUtilCalendarComponentValues(
                    c_session_completed.get(Calendar.YEAR),
                    c_session_completed.get(Calendar.MONTH),
                    c_session_completed.get(Calendar.DAY_OF_MONTH)
            ));
            sessionDialogCompletedHour = c_session_completed.get(Calendar.HOUR_OF_DAY);
            sessionDialogCompletedMinute = c_session_completed.get(Calendar.MINUTE);

            sessionDialogLengthSetManually = true;
            sessionDialogLengthHour = Long.valueOf(session._length / 3600).intValue();
            sessionDialogLengthMinute = Long.valueOf((session._length % 3600) / 60).intValue();
            if (sessionDialogLengthHour == 0 && sessionDialogLengthMinute == 0) {
                sessionDialogLengthMinute = 1;
            }

            sessionDialogMessage.setText(session._message);
        }

        sessionDialogStartedDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionDialogCurrentOption = "started";

                // REFACTOR Move the default value choice into showDatePickerDialog?
                showDatePickerDialog(
                        sessionDialogActivity,
                        sessionDialogDateSetListener,
                        isStartedDateUnset() ? LocalDate.now() : sessionDialogStartedDate
                );
            }
        });
        sessionDialogStartedTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionDialogCurrentOption = "started";
                TimePickerDialog timeDialog = null;

                if (sessionDialogStartedHour == -1 || sessionDialogStartedMinute == -1) {
                    Calendar c = Calendar.getInstance();
                    timeDialog = new TimePickerDialog(sessionDialogActivity,
                            sessionDialogTimeSetListener,
                            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
                } else {
                    timeDialog = new TimePickerDialog(sessionDialogActivity,
                            sessionDialogTimeSetListener,
                            sessionDialogStartedHour, sessionDialogStartedMinute, false);
                }

                timeDialog.show();
            }
        });
        sessionDialogCompletedDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionDialogCurrentOption = "completed";

                // REFACTOR Move the default value choice into showDatePickerDialog?
                LocalDate sessionCompletedDate = isCompletedDateUnset()
                        ? LocalDate.now()
                        : interpretJavaUtilCalendarComponentValuesAsLocalDate(sessionDialogCompletedYear, sessionDialogCompletedMonth, sessionDialogCompletedDay);

                showDatePickerDialog(
                        sessionDialogActivity,
                        sessionDialogDateSetListener,
                        sessionCompletedDate
                );
            }
        });
        sessionDialogCompletedTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionDialogCurrentOption = "completed";
                TimePickerDialog timeDialog = null;

                if (sessionDialogCompletedHour == -1 || sessionDialogCompletedMinute == -1) {
                    Calendar c = Calendar.getInstance();
                    timeDialog = new TimePickerDialog(sessionDialogActivity,
                            sessionDialogTimeSetListener,
                            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
                } else {
                    timeDialog = new TimePickerDialog(sessionDialogActivity,
                            sessionDialogTimeSetListener,
                            sessionDialogCompletedHour, sessionDialogCompletedMinute, false);
                }

                timeDialog.show();
            }
        });
        sessionDialogLengthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionDialogCurrentOption = "length";
                TimePickerDialog timeDialog = null;

                if (sessionDialogLengthHour == -1 || sessionDialogLengthMinute == -1) {
                    timeDialog = new TimePickerDialog(sessionDialogActivity,
                            sessionDialogTimeSetListener,
                            0, 0, true);
                } else {
                    timeDialog = new TimePickerDialog(sessionDialogActivity,
                            sessionDialogTimeSetListener,
                            sessionDialogLengthHour, sessionDialogLengthMinute, true);
                }

                timeDialog.show();
            }
        });

        sessionDialog = new AlertDialog.Builder(sessionDialogActivity)
                .setIcon(getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{session._id == 0 ? R.attr.actionIconNew : R.attr.actionIconGoToToday}).getResourceId(0, 0)))
                .setTitle(getString(session._id == 0 ? R.string.addSession : R.string.editSession))
                .setView(sessionDialogView)
                .setPositiveButton(getString(session._id == 0 ? R.string.add : R.string.edit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int which) {
                        // Overridden later
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int which) {
                        dialogInterface.dismiss();
                    }
                })
                .create();

        updateSessionDialog();
        sessionDialog.show();

        Button saveButton = sessionDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (
                        isStartedDateUnset()
                                || sessionDialogStartedHour == -1
                                || sessionDialogStartedMinute == -1
                                || isCompletedDateUnset()
                                || sessionDialogCompletedHour == -1
                                || sessionDialogCompletedMinute == -1
                ) {
                    shortToast(getString(R.string.invalidDateOrTime));
                } else {
                    // REFACTOR Don't bother replacing this until we're ready to introduce LocalDateTime for session started + completed.
                    Calendar c_started = Calendar.getInstance();
                    c_started.set(Calendar.YEAR, sessionDialogStartedDate.getYear());
                    c_started.set(Calendar.MONTH, sessionDialogStartedDate.getMonthValue() - 1);
                    c_started.set(Calendar.DAY_OF_MONTH, sessionDialogStartedDate.getDayOfMonth());
                    c_started.set(Calendar.HOUR_OF_DAY, sessionDialogStartedHour);
                    c_started.set(Calendar.MINUTE, sessionDialogStartedMinute);
                    c_started.set(Calendar.SECOND, 0);
                    c_started.set(Calendar.MILLISECOND, 0);

                    Calendar c_completed = Calendar.getInstance();
                    c_completed.set(Calendar.YEAR, sessionDialogCompletedYear);
                    c_completed.set(Calendar.MONTH, sessionDialogCompletedMonth);
                    c_completed.set(Calendar.DAY_OF_MONTH, sessionDialogCompletedDay);
                    c_completed.set(Calendar.HOUR_OF_DAY, sessionDialogCompletedHour);
                    c_completed.set(Calendar.MINUTE, sessionDialogCompletedMinute);
                    c_completed.set(Calendar.SECOND, 0);
                    c_completed.set(Calendar.MILLISECOND, 0);

                    // REFACTOR We should be able to use LocalDateTime directly to do all this arithmetic.
                    // REFACTOR Replace with Enum that describes the reasons that a session interval is invalid.
                    boolean invalidDateOrTimeCondition = c_started.getTimeInMillis() > Calendar.getInstance().getTimeInMillis() || c_completed.getTimeInMillis() > Calendar.getInstance().getTimeInMillis() || c_started.getTimeInMillis() >= c_completed.getTimeInMillis();
                    boolean invalidLengthCondition = ((sessionDialogLengthHour * 3600) + (sessionDialogLengthMinute * 60)) > ((c_completed.getTimeInMillis() - c_started.getTimeInMillis()) / 1000);

                    // Handle invalid session
                    if (invalidDateOrTimeCondition) {
                        shortToast(getString(R.string.invalidDateOrTime));
                        return;
                    }

                    if (invalidLengthCondition) {
                        shortToast(getString(R.string.invalidLength));
                        return;
                    }

                    // Try to store session
                    final SessionSQL existingSession = db.getSessionByStarted(c_started.getTimeInMillis() / 1000);
                    if (existingSession != null && existingSession._id > 0 && (session._id == 0 || !existingSession._id.equals(session._id))) {
                        shortToast(getString(R.string.sessionExists));
                        return;
                    }

                    // Update the view
                    AlertDialog postSessionDialog = new AlertDialog.Builder(activity)
                            .setIcon(getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{R.attr.actionIconInfo}).getResourceId(0, 0)))
                            .setTitle(getString(R.string.sessionPosted))
                            .setMessage(getString(R.string.postUpdatedSession))
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface,
                                                    int which) {
                                    dialogInterface.dismiss();
                                    completeSessionDialog(session, true);
                                }
                            })
                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface,
                                                    int which) {
                                    dialogInterface.dismiss();
                                    completeSessionDialog(session, false);
                                }
                            })
                            .setCancelable(true)
                            .create();
                    postSessionDialog.show();
                }
            }
        });
    }

    private void showDatePickerDialog(Context context, DatePickerDialog.OnDateSetListener dateSetListener, LocalDate date) {
        new DatePickerDialog(
                context,
                dateSetListener,
                date.getYear(),
                date.getMonthValue() - 1,
                date.getDayOfMonth()
        ).show();
    }

    private boolean isStartedDateUnset() {
        return null == sessionDialogStartedDate;
    }

    private boolean isCompletedDateUnset() {
        return isDateUnset(sessionDialogCompletedYear, sessionDialogCompletedMonth, sessionDialogCompletedDay);
    }

    // CONTRACT These component arguments are compatible with java.util.Calendar (January == 0)
    // REFACTOR Replace me eventually with `sessionStartedDate == null`.
    private static boolean isDateUnset(int year, int month, int day) {
        return null == interpretJavaUtilCalendarComponentValuesAsLocalDate(
                year,
                month,
                day
        );
    }

    private void unsetCompletedDate() {
        writeSessionCompletedDate(null);
    }

    private void unsetStartedDate() {
        this.sessionDialogStartedDate = null;
    }

    public void updateSessionDialog() {
        if (sessionDialogStartedDateButton == null || sessionDialogCompletedDateButton == null || sessionDialogStartedTimeButton == null || sessionDialogCompletedTimeButton == null || sessionDialogLengthButton == null) {
            return;
        }

        fillSessionDialogLength();

        SimpleDateFormat sdf_date = new SimpleDateFormat("MMMM d",
                Locale.getDefault());
        SimpleDateFormat sdf_time = new SimpleDateFormat("h:mm a",
                Locale.getDefault());

        sdf_date.setTimeZone(TimeZone.getDefault());
        sdf_time.setTimeZone(TimeZone.getDefault());

        // REFACTOR 11:29 Remove duplication and obsolete special case for checking unset.
        if (isStartedDateUnset()) {
            sessionDialogStartedDateButton.setText(getString(R.string.setDate));
        } else {
            String formattedTime = isStartedDateUnset()
                    ? "::this can never, ever happen::"
                    : sessionDialogStartedDate.format(DateTimeFormatter.ofPattern("MMMM d"));

            sessionDialogStartedDateButton.setText(formattedTime);
        }
        if (isCompletedDateUnset()) {
            sessionDialogCompletedDateButton.setText(getString(R.string.setDate));
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, sessionDialogCompletedYear);
            c.set(Calendar.MONTH, sessionDialogCompletedMonth);
            c.set(Calendar.DAY_OF_MONTH, sessionDialogCompletedDay);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            sessionDialogCompletedDateButton.setText(sdf_date.format(c.getTime()));
        }

        if (sessionDialogStartedHour == -1 || sessionDialogStartedMinute == -1) {
            sessionDialogStartedTimeButton.setText(getString(R.string.setTime));
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, sessionDialogStartedHour);
            c.set(Calendar.MINUTE, sessionDialogStartedMinute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            sessionDialogStartedTimeButton.setText(sdf_time.format(c.getTime()));
        }
        if (sessionDialogCompletedHour == -1 || sessionDialogCompletedMinute == -1) {
            sessionDialogCompletedTimeButton.setText(getString(R.string.setTime));
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, sessionDialogCompletedHour);
            c.set(Calendar.MINUTE, sessionDialogCompletedMinute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            sessionDialogCompletedTimeButton.setText(sdf_time.format(c.getTime()));
        }

        if (sessionDialogLengthHour == -1 || sessionDialogLengthMinute == -1) {
            sessionDialogLengthButton.setText(getString(R.string.setTime));
        } else {
            String summary = "";
            if (sessionDialogLengthHour > 0) {
                summary += sessionDialogLengthHour + " hour";
                if (sessionDialogLengthHour > 1) {
                    summary += "s";
                }
            }
            if (sessionDialogLengthMinute > 0) {
                summary += " " + sessionDialogLengthMinute + " minute";
                if (sessionDialogLengthMinute > 1) {
                    summary += "s";
                }
            }

            sessionDialogLengthButton.setText(summary.trim());
        }
    }

    public void fillSessionDialogLength() {
        if (sessionDialogLengthSetManually) {
            return;
        }

        if (
                isStartedDateUnset()
                        || sessionDialogStartedHour == -1
                        || sessionDialogStartedMinute == -1
                        || isCompletedDateUnset()
                        || sessionDialogCompletedHour == -1
                        || sessionDialogCompletedMinute == -1
        ) {
            return;
        }

        Calendar sc = Calendar.getInstance();
        sc.set(Calendar.YEAR, sessionDialogStartedDate.getYear());
        sc.set(Calendar.MONTH, sessionDialogStartedDate.getMonthValue() - 1);
        sc.set(Calendar.DAY_OF_MONTH, sessionDialogStartedDate.getDayOfMonth());
        sc.set(Calendar.HOUR_OF_DAY, sessionDialogStartedHour);
        sc.set(Calendar.MINUTE, sessionDialogStartedMinute);
        sc.set(Calendar.SECOND, 0);
        sc.set(Calendar.MILLISECOND, 0);

        Calendar cc = Calendar.getInstance();
        cc.set(Calendar.YEAR, sessionDialogCompletedYear);
        cc.set(Calendar.MONTH, sessionDialogCompletedMonth);
        cc.set(Calendar.DAY_OF_MONTH, sessionDialogCompletedDay);
        cc.set(Calendar.HOUR_OF_DAY, sessionDialogCompletedHour);
        cc.set(Calendar.MINUTE, sessionDialogCompletedMinute);
        cc.set(Calendar.SECOND, 0);
        cc.set(Calendar.MILLISECOND, 0);

        long length = (cc.getTimeInMillis() / 1000) - (sc.getTimeInMillis() / 1000);
        if (length <= 0) {
            return;
        }

        sessionDialogLengthHour = (int) length / 3600;
        sessionDialogLengthMinute = (int) (length % 3600) / 60;

        if (sessionDialogLengthHour == 0 && sessionDialogLengthMinute == 0) {
            sessionDialogLengthMinute = 1;
        }
    }

    // REFACTOR Replace return value with a... you know... value object.
    public ArrayList<Long> getSessionDialogValues() {
        Calendar c_started = Calendar.getInstance();
        c_started.set(Calendar.YEAR, sessionDialogStartedDate.getYear());
        c_started.set(Calendar.MONTH, sessionDialogStartedDate.getMonthValue() - 1);
        c_started.set(Calendar.DAY_OF_MONTH, sessionDialogStartedDate.getDayOfMonth());
        c_started.set(Calendar.HOUR_OF_DAY, sessionDialogStartedHour);
        c_started.set(Calendar.MINUTE, sessionDialogStartedMinute);
        c_started.set(Calendar.SECOND, 0);
        c_started.set(Calendar.MILLISECOND, 0);

        Calendar c_completed = Calendar.getInstance();
        c_completed.set(Calendar.YEAR, sessionDialogCompletedYear);
        c_completed.set(Calendar.MONTH, sessionDialogCompletedMonth);
        c_completed.set(Calendar.DAY_OF_MONTH, sessionDialogCompletedDay);
        c_completed.set(Calendar.HOUR_OF_DAY, sessionDialogCompletedHour);
        c_completed.set(Calendar.MINUTE, sessionDialogCompletedMinute);
        c_completed.set(Calendar.SECOND, 0);
        c_completed.set(Calendar.MILLISECOND, 0);

        ArrayList<Long> sc = new ArrayList<>();
        sc.add(c_started.getTimeInMillis() / 1000);
        sc.add(c_completed.getTimeInMillis() / 1000);
        sc.add((long) ((sessionDialogLengthHour * 3600) + (sessionDialogLengthMinute * 60)));
        return sc;
    }

    public void completeSessionDialog(SessionSQL session, boolean postSession) {
        ArrayList<Long> sc = getSessionDialogValues();
        Long started = sc.get(0);
        Long completed = sc.get(1);
        Long length = sc.get(2);

        if (postSession) {
            getMediNET().resetSession();
            getMediNET().session.started = started;
            getMediNET().session.length = length;
            getMediNET().session.completed = completed;
            getMediNET().session.message = sessionDialogMessage.getText().toString().trim();
            getMediNET().session.modified = getTimestamp();

            getMediNET().status = "";
            getMediNET().postSession(sessionDialogUpdateSessionStarted, sessionDialogActivity, () -> {
                if (getMediNET().status.equals("success")) {
                    notifySessionsUpdated();
                    sessionDialog.dismiss();
                }
            });
        } else {
            if (db.addSession(new SessionSQL((long) 0, started, completed, length, sessionDialogMessage.getText().toString().trim(), (long) 0, (long) 0, getTimestamp()), sessionDialogUpdateSessionStarted)) {
                if (session._id == 0) { // Add session
                    shortToast(getString(R.string.sessionAdded));
                } else {
                    shortToast(getString(R.string.sessionEdited));
                }

                notifySessionsUpdated();
                sessionDialog.dismiss();
            }
        }
    }

    public AlertDialog showStaleDataDialog() {
        Log.d("MeditationAssistant", "Showing stale data dialog");

        try {
            Looper.prepare();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (getMediNET().activity != null) {
            AlertDialog staleDataDialog = new AlertDialog.Builder(
                    getMediNET().activity)
                    .setPositiveButton(R.string.download,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    getMediNET().downloadSessions();
                                }
                            }
                    )
                    .setNegativeButton(R.string.dismiss,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    // Do nothing
                                }
                            }
                    )
                    .setTitle(R.string.downloadsessionstitle)
                    .setMessage(R.string.downloadsessionsmessage)
                    .setIcon(getMediNET().activity.getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{R.attr.actionIconDownCloud}).getResourceId(0, 0)))
                    .create();

            staleDataDialog.show();

            return staleDataDialog;
        } else {
            longToast(getString(R.string.downloadSessionsHint));
        }

        return null;
    }

    public void showFilePickerDialog(Activity activity, int requestCode, String action, String defaultName) {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent()
                    .setType("*/*");

            if (action.equals("openfile")) {
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            } else if (action.equals("newfile")) {
                intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
                intent.putExtra(Intent.EXTRA_TITLE, defaultName);
            }

            activity.startActivityForResult(Intent.createChooser(intent, "Select a file"), requestCode);
        } else {
            Intent i = new Intent(activity, FilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

            if (action.equals("openfile")) {
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                i.putExtra(FilePickerActivity.EXTRA_PATHS, FilePickerActivity.MODE_FILE);
            } else if (action.equals("newfile")) {
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_NEW_FILE);
                i.putExtra(FilePickerActivity.EXTRA_PATHS, FilePickerActivity.MODE_NEW_FILE);
            }

            activity.startActivityForResult(i, requestCode);
        }
    }

    public Uri filePickerResult(Intent intent) {
        if (Build.VERSION.SDK_INT >= 23) {
            return intent.getData();
        } else {
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);
            for (Uri uri : files) {
                return uri;
            }
        }
        return null;
    }

    public void askToDonate(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder
                .setIcon(getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{R.attr.actionIconInfo}).getResourceId(0, 0)))
                .setTitle(getString(R.string.announcement))
                .setMessage(getString(R.string.donate156))
                .setPositiveButton(getString(R.string.donate),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                showDonationDialog(activity);
                            }
                        })
                .setNegativeButton(getString(R.string.dismiss),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                .show();
    }

    public void showImportSessionsDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder
                .setIcon(getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{R.attr.actionIconForward}).getResourceId(0, 0)))
                .setTitle(getString(R.string.importsessions))
                .setMessage(getString(R.string.importsessions_utc_or_local))
                .setPositiveButton(getString(R.string.utc),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                showFilePickerDialog(activity, SettingsActivity.FILEPICKER_IMPORT_SESSIONS_UTC, "openfile", "");
                            }
                        })
                .setNegativeButton(getString(R.string.local),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                showFilePickerDialog(activity, SettingsActivity.FILEPICKER_IMPORT_SESSIONS_LOCAL, "openfile", "");
                            }
                        })
                .show();
    }

    public void importSessions(Activity activity, Uri uri, boolean useLocalTimeZone) {
        final Pattern lengthPattern = Pattern.compile("^[0-9]{1,2}:[0-9][0-9]$");

        InputStreamReader inputfile;
        try {
            inputfile = new InputStreamReader(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        CSVReader reader = new CSVReader(inputfile);

        long lengthHours, lengthMinutes;
        Date startedDate, completedDate;
        String[] lengthSplit;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        TimeZone tz;
        if (useLocalTimeZone) {
            Calendar cal = Calendar.getInstance();
            tz = cal.getTimeZone();
        } else {
            tz = TimeZone.getTimeZone("UTC");
        }
        df.setTimeZone(tz);

        ArrayList<SessionSQL> sessions = new ArrayList<>();
        int existingSessions = 0;

        try {
            List<String[]> data = reader.readAll();
            int i = 0;
            boolean foundHeader = false;
            for (String[] d : data) {
                i++;
                if (d.length != CSV_COLUMN_COUNT) {
                    longToast("Invalid row on line #" + i + ": expected " + CSV_COLUMN_COUNT + " columns, found " + d.length);
                    closeCSVReader(reader, inputfile);
                    return;
                } else if (d[0].trim().toLowerCase().equals("id")) {
                    if (foundHeader) {
                        longToast("Invalid row on line #" + i + ": header appears twice");
                        closeCSVReader(reader, inputfile);
                        return;
                    }

                    foundHeader = true;
                    continue;
                }

                SessionSQL s = new SessionSQL();

                try {
                    if (!lengthPattern.matcher(d[1].trim()).matches()) {
                        throw new Exception();
                    }

                    lengthSplit = d[1].trim().split(":");
                    if (lengthSplit.length != 2) {
                        throw new Exception();
                    }

                    lengthHours = Long.parseLong(lengthSplit[0]);
                    lengthMinutes = Long.parseLong(lengthSplit[1]);

                    if (lengthHours < 0 || lengthMinutes < 0 || lengthHours > 24 || lengthMinutes > 59) {
                        throw new Exception();
                    }

                    s._length = (lengthHours * 3600) + (lengthMinutes * 60);
                } catch (Exception e) {
                    longToast("Invalid row on line #" + i + ": invalid session length");
                    closeCSVReader(reader, inputfile);
                    return;
                }

                try {
                    if (d[2].trim().equals("")) {
                        throw new Exception();
                    }

                    startedDate = df.parse(d[2].trim());
                    s._started = startedDate.getTime() / 1000;
                } catch (Exception e) {
                    longToast("Invalid row on line #" + i + ": invalid session started date/time");
                    closeCSVReader(reader, inputfile);
                    return;
                }

                try {
                    if (d[3].trim().equals("")) {
                        throw new Exception();
                    }

                    completedDate = df.parse(d[3].trim());
                    s._completed = completedDate.getTime() / 1000;
                } catch (Exception e) {
                    longToast("Invalid row on line #" + i + ": invalid session completed date/time");
                    closeCSVReader(reader, inputfile);
                    return;
                }

                s._message = d[4].trim();

                boolean existing = false;
                for (int searchStarted = 0; searchStarted <= 59; searchStarted++) {
                    if (db.numSessionsByStarted(s._started + searchStarted) != 0) {
                        existing = true;
                        break;
                    }
                }

                if (existing) {
                    existingSessions++;
                } else {
                    sessions.add(s);
                }
            }

            closeCSVReader(reader, inputfile);
        } catch (Exception e) {
            e.printStackTrace();
            longToast("Failed to write CSV file: " + e.toString());
        }

        if (sessions.size() == 0) {
            longToast(getString(R.string.sessionsUpToDate));
            return;
        }

        AlertDialog sessionsImportedDialog = new AlertDialog.Builder(activity)
                .setIcon(getResources().getDrawable(getTheme().obtainStyledAttributes(getMATheme(true), new int[]{R.attr.actionIconForward}).getResourceId(0, 0)))
                .setTitle(getString(R.string.importsessions))
                .setMessage(String.format(getString(R.string.importsessions_complete), existingSessions, sessions.size()))
                .setPositiveButton(getString(R.string.wordimport), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int which) {
                        for (SessionSQL s : sessions) {
                            db.addSession(s, (long) 0);
                        }
                        recalculateMeditationStreak(activity);

                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int which) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        sessionsImportedDialog.show();
    }

    private void closeCSVReader(CSVReader reader, InputStreamReader file) {
        try {
            reader.close();
        } catch (IOException e) {
            // Do nothing
        }

        try {
            file.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    public void exportSessions(Activity activity, Uri uri) {
        try {
            FileOutputStream outputStream = (FileOutputStream) getContentResolver().openOutputStream(uri);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            CSVWriter writer = new CSVWriter(outputStreamWriter);
            List<String[]> data = new ArrayList<>();

            ArrayList<SessionSQL> sessions = db.getAllSessions();

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            data.add(new String[]{"ID", "Length", "Started", "Completed", "Message"});
            for (SessionSQL s : sessions) {
                data.add(new String[]{Long.toString(s._id), String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.SECONDS.toHours(s._length), TimeUnit.SECONDS.toMinutes(s._length) % TimeUnit.HOURS.toMinutes(1)), df.format(s._started * 1000), df.format(s._completed * 1000), s._message});
            }

            writer.writeAll(data);
            writer.close();
        } catch (IOException e) {
            longToast(getString(R.string.sessionExportFailed) + ": " + e.toString() + " - " + uri.toString());
            Log.e("MeditationAssistant", "Error exporting sessions to " + uri.toString(), e);
            return;
        }
        longToast(getString(R.string.sessionExportWasSuccessful));
    }

    public void updateWidgets() {
        Intent updateIntent = new Intent();
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        getApplicationContext().sendBroadcast(updateIntent);
    }

    public void vibrateDevice(String pattern) {
        ArrayList<Long> p = new ArrayList<Long>();
        if (pattern.equals("short")) {
            p.add((long) 110);
            p.add((long) 225);
            p.add((long) 110);
        } else if (pattern.equals("medium")) {
            p.add((long) 420);
            p.add((long) 375);
            p.add((long) 420);
        } else if (pattern.equals("long")) {
            p.add((long) 840);
            p.add((long) 550);
            p.add((long) 840);
        } else {
            String[] patternSplit = pattern.split(",");
            for (String pp : patternSplit) {
                pp = pp.trim();
                if (pp.isEmpty() || !StringUtils.isNumeric(pp)) {
                    continue;
                }

                long ppv = Long.parseLong(pp);
                if (ppv < 0L) {
                    ppv = 0L;
                } else if (ppv > 15000L) {
                    ppv = 15000L;
                }
                p.add(ppv);
            }
        }
        p.add(0, 0L);

        try {
            Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vi.vibrate(ArrayUtils.toPrimitive(p.toArray(new Long[p.size()])), -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean haveNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return mNotificationManager.isNotificationPolicyAccessGranted();
    }

    public void checkNotificationControl(Activity activity, String prefValue) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        String prefnotificationcontrol = prefValue;
        if (prefnotificationcontrol.equals("")) {
            prefnotificationcontrol = getPrefs().getString("pref_notificationcontrol", "");
        }
        if (!prefnotificationcontrol.equals("priority") && !prefnotificationcontrol.equals("alarms")) {
            return; // Notification filter will not be changed
        }

        if (haveNotificationPermission()) {
            return;
        }

        AlertDialog accountsAlertDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.permissionRequest)
                .setMessage(R.string.permissionRequestNotificationControl)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    @SuppressLint("InlinedApi") Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .create();
        accountsAlertDialog.show();
    }

    public ArrayList<Long> dateToSessionWindow(Calendar c) {
        ArrayList<Long> sessionWindow = new ArrayList<>();
        ArrayList<Integer> streakbuffertime = getStreakBufferTime();
        Calendar sessionWindowCalendar = (Calendar) c.clone();

        sessionWindowCalendar.set(Calendar.HOUR_OF_DAY, streakbuffertime.get(0));
        sessionWindowCalendar.set(Calendar.MINUTE, streakbuffertime.get(1));
        sessionWindow.add(sessionWindowCalendar.getTimeInMillis() / 1000);

        sessionWindowCalendar.add(Calendar.DATE, 1);
        sessionWindowCalendar.set(Calendar.HOUR_OF_DAY, streakbuffertime.get(0));
        sessionWindowCalendar.set(Calendar.MINUTE, streakbuffertime.get(1));
        sessionWindow.add(sessionWindowCalendar.getTimeInMillis() / 1000);

        return sessionWindow;
    }

    public String sessionToDate(SessionSQL session) {
        if (session._completed != null) {
            return timestampToDate(session._completed * 1000);
        } else {
            return timestampToDate((session._started + session._length) * 1000);
        }
    }

    private String timestampToDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy", Locale.US);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        Date api_date = cal.getTime();

        return sdf.format(api_date);
    }

    public void sendLogcat() {
        StringBuilder logcat = new StringBuilder();
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"logcat", "-d"};
        Process proc;
        try {
            proc = rt.exec(commands);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            String s;
            while ((s = stdError.readLine()) != null) {
                logcat.append("Logcat error: ").append(s).append("\n");
            }
            logcat.append("Logcat output:\n");
            while ((s = stdInput.readLine()) != null) {
                logcat.append(s).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            shortToast("Failed to retrieve logcat");
            return;
        }

        Intent e = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getString(R.string.myEmailAddress), null));
        String[] to = {getString(R.string.myEmailAddress)};
        e.putExtra(Intent.EXTRA_EMAIL, to);
        e.putExtra(Intent.EXTRA_SUBJECT, "Meditation Assistant Debug Log");
        e.putExtra(Intent.EXTRA_TEXT, logcat.toString());
        startActivity(Intent.createChooser(e, getString(R.string.sendEmail)));
    }

    public String getMAAppVersion() {
        if (appVersion == null) {
            appVersion = "";
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(
                        getPackageName(), 0);
                appVersion = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return appVersion;
    }

    public int getMAAppVersionNumber() {
        return BuildConfig.VERSION_CODE;
    }

    public String acquireWakeLock(Boolean fullWakeUp) {
        wakeLockerLock.lock();
        String wakelockID = wakeLocker.acquire(getApplicationContext(), fullWakeUp);
        wakeLockerLock.unlock();

        return wakelockID;
    }

    public void releaseWakeLock(String wakeLockID) {
        wakeLockerLock.lock();
        wakeLocker.release(wakeLockID);
        wakeLockerLock.unlock();
    }

    public void releaseAllWakeLocks() {
        wakeLockerLock.lock();
        wakeLocker.releaseAll();
        wakeLockerLock.unlock();
    }

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        ACRA.init(this);
    }
}
