package sh.ftp.rocketninelabs.meditationassistant;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@ReportsCrashes(
        formUri = "http://medinet.ftp.sh/acra/acra.php"
)
public class MeditationAssistant extends Application {

    private static final String AUTH_PENDING = "auth_state_pending";
    public static String ACTION_PRESET = "sh.ftp.rocketninelabs.meditationassistant.PRESET";
    public static String ACTION_REMINDER = "sh.ftp.rocketninelabs.meditationassistant.DAILY_NOTIFICATION";
    public static String ACTION_UPDATED = "sh.ftp.rocketninelabs.meditationassistant.DAILY_NOTIFICATION_UPDATED";
    public static int REQUEST_FIT = 22;
    public Boolean debug_widgets = false; // Debug
    public long lastpostedsessionstart = 0;
    public boolean ispaused = false;
    public long pausestart = 0;
    public long pausetime = 0;
    public int previousRingerMode = -1;
    public String toastText = "";
    public String pendingNotificationAction = "";
    public Boolean asktorate = false;
    public DatabaseHandler db = null;
    public AlarmManager reminderAlarmManager = null;
    public PendingIntent reminderPendingIntent = null;
    public String theme = null;
    public String marketName = null;
    public UtilityMA utility = new UtilityMA();
    public UtilityAdsMA utility_ads = new UtilityAdsMA();
    public Integer previous_volume = null;
    AlertDialog alertDialog = null;
    String AUTH_TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email";
    private String appVersion = null;
    private Boolean appFull = null;
    private long timeToStopMeditate = 0;
    private long timeStartMeditate = 0;
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
    private long sessrunnablestarttime = 0;
    private boolean sesswassignedout = false;
    private Boolean sendusage = null;
    private Activity toastActivity;
    private long sessionduration = 0;
    private Integer webview_scale = null;
    private String timerMode = null;
    private String durationFormatted = "";
    private Activity signin_activity = null;
    private Bundle signin_options = new Bundle();
    private SharedPreferences prefs = null;

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
                //marketName = "google";
                marketName = "amazon";
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

    public boolean canVibrate() {
        try {
            Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            return vi.hasVibrator();
        } catch (NoSuchMethodError e) {
        } catch (Exception e) {
        }
        return true;
    }

    public void restoreVolume() {
        if (previous_volume != null) {
            AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previous_volume, 0);
            previous_volume = null;
        }
    }

    public void connectOnce() {
        if (!connectedonce) {
            getMediNET().connect();
            connectedonce = true;
        }
    }

    public void askToRateApp() {
        if (getMarketName().equals("bb")) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("https://appworld.blackberry.com/webstore/content/" + (BuildConfig.FLAVOR.equals("free") ? "59939924" : "59939922") + "/")), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (getMarketName().equals("google")) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (getMarketName().equals("amazon")) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + getApplicationContext().getPackageName())), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public void setNotificationControl() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        previousRingerMode = audioManager.getRingerMode();

        if (getPrefs().getString("pref_notificationcontrol", "").equals("vibrate")) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        } else if (getPrefs().getString("pref_notificationcontrol", "").equals("silent")) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    public void unsetNotificationControl() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (previousRingerMode >= 0 && getPrefs().getString("pref_notificationcontrol", "").equals("vibrate") || getPrefs().getString("pref_notificationcontrol", "").equals("silent")) {
            audioManager.setRingerMode(previousRingerMode);
        }

        previousRingerMode = -1;
    }

    public String getDurationFormatted() {
        return durationFormatted;
    }

    public void setDurationFormatted(String d) {
        if (!d.equals(durationFormatted)) {
            durationFormatted = d;
            // showNotification();
        }
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
        Log.d("MeditationAssistant", "Session: " + String.valueOf(medinet));
    }

    public String getMediNETKey() {
        Log.d("MeditationAssistant", "getMediNETKey() - medinetkey: " + String.valueOf(medinetkey) + " prefs: " + getPrefs().getString("key", ""));
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

    public void recalculateMeditationStreak() {
        Calendar dayCalendar = Calendar.getInstance();
        Integer daysback = 0;
        Integer recalculatedstreak = 0;
        Boolean sessionexiststoday = false;

        while (true) {
            if (db.numSessionsByDate(String.valueOf(dayCalendar.get(Calendar.DAY_OF_MONTH)) + "-"
                    + String.valueOf(dayCalendar.get(Calendar.MONTH) + 1) + "-"
                    + String.valueOf(dayCalendar.get(Calendar.YEAR))) > 0) {
                recalculatedstreak++;

                if (daysback == 0) {
                    sessionexiststoday = true;
                }
            } else if (daysback > 0) {
                break;
            }

            daysback++;
            dayCalendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        if (getMeditationStreak() < recalculatedstreak) {
            setMeditationStreak(recalculatedstreak, sessionexiststoday ? getMidnightTwoDaysTimestamp() : getMidnightOneDayTimestamp());
        }
    }

    public Integer getMeditationStreak() {
        long timestamp = System.currentTimeMillis() / 1000;

        if (meditationstreak == null || meditationstreakexpires < 1) { // streak
            // or
            // expires
            // timestamp
            // not
            // set
            meditationstreak = getPrefs().getInt("meditationstreak", 0);
            meditationstreakexpires = getPrefs().getLong("meditationstreakexpires",
                    0);
        }

        if (meditationstreakexpires > 0 && meditationstreakexpires < timestamp) { // expires
            // timestamp
            // has passed
            meditationstreak = 0;
            meditationstreakexpires = 0;

            getPrefs().edit().putInt("meditationstreak", meditationstreak).putLong("meditationstreakexpires", meditationstreakexpires).apply();
        }

        return meditationstreak;
    }

    public void addMeditationStreak() {
        addMeditationStreak(true);
    }

    public void addMeditationStreak(Boolean twodays) {
        Integer streak = getMeditationStreak();
        Log.d("MeditationAssistant", "addMeditationStreak() - Streak: " + String.valueOf(streak) + " Expires: in " + String.valueOf(meditationstreakexpires - getTimestamp()) + " seconds");
        if (meditationstreak == null || meditationstreak == 0 || meditationstreakexpires - getTimestamp() < 86400) {
            streak++;

            if (twodays) {
                setMeditationStreak(streak,
                        getMidnightTwoDaysTimestamp());
            } else {
                setMeditationStreak(streak,
                        getMidnightOneDayTimestamp());
            }
        }
    }

    public int getLongestMeditationStreak() {
        return getPrefs().getInt("longeststreak", 0);
    }

    public void setLongestMeditationStreak(int ms) {
        getPrefs().edit().putInt("longeststreak", ms).apply();
    }

    public long getMidnightOneDayTimestamp() {
        Calendar c_midnight_oneday = new GregorianCalendar();
        c_midnight_oneday.setTime(new Date());
        c_midnight_oneday.set(Calendar.HOUR_OF_DAY, 0);
        c_midnight_oneday.set(Calendar.MINUTE, 0);
        c_midnight_oneday.set(Calendar.SECOND, 0);
        c_midnight_oneday.set(Calendar.MILLISECOND, 0);
        c_midnight_oneday.add(Calendar.DATE, 1); // One day

        return c_midnight_oneday.getTimeInMillis() / 1000;
    }

    public long getMidnightTwoDaysTimestamp() {
        Calendar c_midnight_twodays = new GregorianCalendar();
        c_midnight_twodays.setTime(new Date());
        c_midnight_twodays.set(Calendar.HOUR_OF_DAY, 0);
        c_midnight_twodays.set(Calendar.MINUTE, 0);
        c_midnight_twodays.set(Calendar.SECOND, 0);
        c_midnight_twodays.set(Calendar.MILLISECOND, 0);
        c_midnight_twodays.add(Calendar.DATE, 2); // Two days

        return c_midnight_twodays.getTimeInMillis() / 1000;
    }

    public void notifySessionsUpdated() {
        Log.d("MeditationAssistant", "Sending session update notification");
        getPrefs().edit().putLong("sessionsupdate", getTimestamp()).apply();
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
                ArrayList<String> formatted_duration = new ArrayList<String>();
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
                ArrayList<String> formatted_duration = new ArrayList<String>();
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
                + String.valueOf(timeToStopMeditate));
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

    public void hideNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public void longToast(Activity activity, String text) {
        try {
            Looper.prepare();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (activity == null) {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG)
                    .show();
        } else {
            toastActivity = activity;
            toastText = text;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(toastActivity, toastText, Toast.LENGTH_LONG)
                            .show();
                }
            });
        }
    }

    public void longToast(String text) {
        longToast(null, text);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        utility.ma = this;

        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        Integer applaunches = getPrefs().getInt("applaunches", 0) + 1;
        getPrefs().edit().putInt("applaunches", applaunches).apply();

        Log.d("MeditationAssistant",
                "Meditation Assistant running (" + applaunches + " launches) on API level "
                        + String.valueOf(Build.VERSION.SDK_INT)
        );

        // Reset timer to default values
        if (!getPrefs().getBoolean("pref_rememberlasttimer", true)) {
            SharedPreferences.Editor editor = getPrefs().edit();
            editor.putString("timerHours",
                    getPrefs().getString("timerDefaultHours", "15"));
            editor.putString("timerMinutes",
                    getPrefs().getString("timerDefaultMinutes", "15"));
            editor.apply();
        }

        // Upgrade to the new delay and interval preferences
        /*long pref_delay = Integer.valueOf(getPrefs().getString("pref_delay", "-1"));
        if (pref_delay >= 0) {
            Log.d("MeditationAssistant", "Upgrading pref_delay to TimePreference: from " + String.valueOf(pref_delay) + " to " + String.format("%02d", pref_delay / 60) + ":" + String.format("%02d", pref_delay % 60));
            getPrefs().edit().putString("pref_session_delay", String.format("%02d", pref_delay / 60) + ":" + String.format("%02d", pref_delay % 60)).apply();
            getPrefs().edit().putString("pref_delay", "-1").apply();
            Log.d("MeditationAssistant", "New pref_session_delay value: " + getPrefs().getString("pref_session_delay", "00:15"));

        }
        long pref_interval = Integer.valueOf(getPrefs().getString("pref_interval", "-1"));
        if (pref_interval >= 0) {
            Log.d("MeditationAssistant", "Upgrading pref_interval to TimePreference: from " + String.valueOf(pref_interval) + " to " + String.format("%02d", pref_interval / 60) + ":" + String.format("%02d", pref_interval % 60));
            getPrefs().edit().putString("pref_session_delay", String.format("%02d", pref_interval / 60) + ":" + String.format("%02d", pref_interval % 60)).apply();
            getPrefs().edit().putString("pref_interval", "-1").apply();
        }*/

        db = DatabaseHandler.getInstance(getApplicationContext());

        reminderAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        /* Send the daily notification updated intent just in case the receiver hasn't been called yet */
        Log.d("MeditationAssistant", "Sending initial daily notification updated intent");
        Intent intent = new Intent();
        intent.setAction(MeditationAssistant.ACTION_UPDATED);
        sendBroadcast(intent);
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

    public boolean sendUsageReports() {
        if (sendusage == null) {
            sendusage = getPrefs().getBoolean("pref_sendusage", true);
        }
        return sendusage;
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
        editor.putString("keyupdate", String.valueOf(timestamp));
        editor.apply();
    }

    public void setMeditationStreak(Integer ms, long expires) {
        if (meditationstreak == null) {
            meditationstreak = getMeditationStreak();
        }
        if (ms >= meditationstreak) {
            meditationstreak = ms;
            meditationstreakexpires = expires;

            Calendar date = new GregorianCalendar();
            date.setTimeZone(TimeZone.getDefault());
            long timestamp = date.getTimeInMillis() / 1000;
            Log.d("MeditationAssistant",
                    "Streak: " + String.valueOf(meditationstreak) + ", expires: "
                            + String.valueOf(expires) + " (in "
                            + String.valueOf(expires - timestamp) + " seconds)"
            );

            getPrefs().edit().putInt("meditationstreak", meditationstreak).putLong("meditationstreakexpires", meditationstreakexpires).apply();

            if (meditationstreak > getLongestMeditationStreak()) {
                setLongestMeditationStreak(meditationstreak);
            }

            updateWidgets();
        } else {
            Log.d("MeditationAssistant",
                    "Not setting new meditation streak, current streak is higher");
        }

        /* Update all widgets */
        Intent update_widgets = new Intent(getApplicationContext(), MeditationProvider.class);
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

        Log.d("MeditationAssistant", "PAUSE: Un-paused.  Paused for " + String.valueOf(thispausetime) + " seconds (" + String.valueOf(pausetime) + " total)");

        ispaused = false;
        return thispausetime;
    }

    public void shortToast(Activity activity, String text) {
        try {
            Looper.prepare();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (activity == null) {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT)
                    .show();
        } else {
            toastActivity = activity;
            toastText = text;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(toastActivity, toastText, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    public void shortToast(String text) {
        shortToast(null, text);
    }

    public AlertDialog showAnnouncementDialog(String title) {
        try {
            Looper.prepare();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (getMediNET().activity != null
                && !getMediNET().announcement.equals("")) {
            AlertDialog announceDialog = new AlertDialog.Builder(
                    getMediNET().activity)
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
                    .setIcon(
                            getMediNET().activity
                                    .getResources()
                                    .getDrawable(
                                            getTheme()
                                                    .obtainStyledAttributes(
                                                            getMATheme(true),
                                                            new int[]{R.attr.actionIconGoToToday})
                                                    .getResourceId(0, 0)
                                    )
                    )
                    .setCancelable(false).create();

            announceDialog.show();

            return announceDialog;
        }

        return null;
    }

    public void showNotification() {
        if (!getPrefs().getBoolean("pref_notification", true)
                || getTimeStartMeditate() < 1) {
            hideNotification();
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
        if (getMeditationStreak() > 1) {
            streaktext = String.valueOf(getMeditationStreak());
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.sessionInProgress))
                .setContentText(getString(R.string.appName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentInfo(streaktext)
                .setContentIntent(pIntent)
                .addAction(R.drawable.ic_action_pause,
                        getString(R.string.pause), pIntentPause)
                .addAction(R.drawable.ic_action_stop,
                        getString(R.string.end), pIntentEnd).build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    public AlertDialog showSignInDialog(final Activity activity) {
        signin_activity = activity;

        AccountManager accountManager = AccountManager
                .get(getApplicationContext());
        final Account[] accounts = accountManager
                .getAccountsByType("com.google");

        final int size = accounts.length;

        if (size > 0) {
            String[] names = new String[size + 1];
            int i = 0;
            for (i = 0; i < size; i++) {
                names[i] = accounts[i].name;
            }
            names[i] = getString(R.string.signInWithOpenID);

            AlertDialog accountsAlertDialog = new AlertDialog.Builder(signin_activity)
                    .setTitle(getString(R.string.signInWith))
                    .setItems(names, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which != size) {
                                AccountManager am = AccountManager.get(getApplicationContext());

                                if (am != null) {
                                    am.getAuthToken(accounts[which], AUTH_TOKEN_TYPE, null, signin_activity, new OnTokenAcquired(), new Handler(new Handler.Callback() {

                                        public boolean handleMessage(Message msg) {
                                            Log.d("MeditationAssistant", "on error: " + msg.what);
                                            shortToast(getString(R.string.signInGoogleError));
                                            return false;
                                        }
                                    }));
                                }
                            } else {
                                showOpenIDSignInDialog(signin_activity);
                            }
                        }
                    })
                    .create();
            accountsAlertDialog.show();

            return accountsAlertDialog;
        } else {
            return showOpenIDSignInDialog(signin_activity);
        }
    }

    public AlertDialog showOpenIDSignInDialog(Activity activity) {
        if (alertDialog != null && alertDialog.getOwnerActivity() != null
                && alertDialog.getOwnerActivity() == activity) {
            Log.d("MeditationAssistant",
                    "Attempting to reuse MediNET sign in dialog");

            try {
                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }

                Log.d("MeditationAssistant", "Reusing MediNET sign in dialog");
                return alertDialog;
            } catch (WindowManager.BadTokenException e) {
                // Activity is not in the foreground
            }
        }

        int[] buttons = {R.id.btnGoogle, R.id.btnFacebook, R.id.btnAOL,
                R.id.btnTwitter, R.id.btnLive, R.id.btnOpenID};

        View view = LayoutInflater.from(activity).inflate(
                R.layout.medinet_signin,
                (ViewGroup) activity.findViewById(R.id.medinetsignin_root));

        for (int buttonid : buttons) {
            ImageButton btn = (ImageButton) view.findViewById(buttonid);

            if (btn.getId() == R.id.btnGoogle) {
                if (!getMAThemeString().equals("dark")) {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_google_light));
                } else {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_google));
                }
            } else if (btn.getId() == R.id.btnFacebook) {
                if (!getMAThemeString().equals("dark")) {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_facebook_light));
                } else {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_facebook));
                }
            } else if (btn.getId() == R.id.btnAOL) {
                if (!getMAThemeString().equals("dark")) {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_aol_light));
                } else {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_aol));
                }
            } else if (btn.getId() == R.id.btnTwitter) {
                if (!getMAThemeString().equals("dark")) {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_twitter_light));
                } else {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_twitter));
                }
            } else if (btn.getId() == R.id.btnLive) {
                if (!getMAThemeString().equals("dark")) {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_live_light));
                } else {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_live));
                }
            } else if (btn.getId() == R.id.btnOpenID) {
                if (!getMAThemeString().equals("dark")) {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_openid_light));
                } else {
                    btn.setImageDrawable(getResources().getDrawable(
                            R.drawable.logo_openid));
                }
            }

            btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    ImageButton img = (ImageButton) v;
                    Intent intent = new Intent(getApplicationContext(),
                            MediNETActivity.class);

                    if (img.getId() == R.id.btnGoogle) {
                        intent.putExtra("provider", "Google");
                    } else if (img.getId() == R.id.btnFacebook) {
                        intent.putExtra("provider", "Facebook");
                    } else if (img.getId() == R.id.btnAOL) {
                        intent.putExtra("provider", "AOL");
                    } else if (img.getId() == R.id.btnTwitter) {
                        intent.putExtra("provider", "Twitter");
                    } else if (img.getId() == R.id.btnLive) {
                        intent.putExtra("provider", "Live");
                    } else if (img.getId() == R.id.btnOpenID) {
                        intent.putExtra("provider", "OpenID");
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    if (alertDialog != null) {
                        try {
                            alertDialog.dismiss();
                        } catch (Exception e) {
                            // Do nothing
                        }
                    }

                    startActivity(intent);
                }
            });
        }

        alertDialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle(R.string.signInToMediNET)
                .setIcon(
                        activity.getResources().getDrawable(
                                getTheme().obtainStyledAttributes(getMATheme(true),
                                        new int[]{R.attr.actionIconForward})
                                        .getResourceId(0, 0)
                        )
                ).create();
        alertDialog
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (getMediNETKey() == "") {
                            getMediNET().status = "disconnected";
                            getMediNET().updated();
                        }
                    }
                });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (getMediNETKey() == "") {
                    getMediNET().status = "disconnected";
                    getMediNET().updated();
                }
            }
        });

        alertDialog.show();
        return alertDialog;
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
                    .setPositiveButton(R.string.wordimport,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    getMediNET().syncSessions();
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
                    .setTitle(R.string.importsessionstitle)
                    .setMessage(R.string.importsessionsmessage)
                    .setIcon(
                            getMediNET().activity
                                    .getResources()
                                    .getDrawable(
                                            getTheme()
                                                    .obtainStyledAttributes(
                                                            getMATheme(true),
                                                            new int[]{R.attr.actionIconDownCloud})
                                                    .getResourceId(0, 0)
                                    )
                    )
                    .create();

            staleDataDialog.show();

            return staleDataDialog;
        } else {
            longToast(getString(R.string.importSessionsHint));
        }

        return null;
    }

    public void updateWidgets() {
        AppWidgetManager man = AppWidgetManager
                .getInstance(getApplicationContext());
        /*int[] ids = man.getAppWidgetIds(new ComponentName(
                getApplicationContext(), MeditationProvider.class));*/
        Intent updateIntent = new Intent();
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        getApplicationContext().sendBroadcast(updateIntent);
    }

    public Boolean vibrationEnabled() {
        return (getPrefs().getBoolean("pref_vibrate", false) && canVibrate());
    }

    public void vibrateDevice() {
        if (vibrationEnabled()) {
            try {
                Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {225, 110, 225, 110, 225, 110};
                vi.vibrate(pattern, -1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {

            Intent launch = null;
            try {
                launch = (Intent) result.getResult().get(AccountManager.KEY_INTENT);

                if (launch == null) {
                    String authtoken = result.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    if (!authtoken.equals("")) {
                        getMediNET().signInWithAuthToken(authtoken);
                    }
                }
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }
            if (launch != null) {
                signin_activity.startActivityForResult(launch, 0);
            }
        }
    }
}