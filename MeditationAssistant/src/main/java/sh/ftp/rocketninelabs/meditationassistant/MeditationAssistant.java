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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.webkit.CookieSyncManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MeditationAssistant extends Application {

    private static final String AUTH_PENDING = "auth_state_pending";
    public static String ACTION_PRESET = "sh.ftp.rocketninelabs.meditationassistant.PRESET";
    public static String ACTION_REMINDER = "sh.ftp.rocketninelabs.meditationassistant.DAILY_NOTIFICATION";
    public static String ACTION_UPDATED = "sh.ftp.rocketninelabs.meditationassistant.DAILY_NOTIFICATION_UPDATED";
    public static int REQUEST_FIT = 22;
    public String package_name;
    public Boolean debug_widgets = false; // Debug
    public long lastpostedsessionstart = 0;
    public boolean ispaused = false;
    public long pausestart = 0;
    public long pausetime = 0;
    public int previousRingerMode = -1;
    public String toastText = "";
    public DefaultHttpClient httpClient = null;
    public BasicCookieStore cookieStore = null;
    public BasicHttpContext httpContext = null;
    public String pendingNotificationAction = "";
    public Boolean asktorate = false;
    public DatabaseHandler db = null;
    public AlarmManager reminderAlarmManager = null;
    public PendingIntent reminderPendingIntent = null;
    public String theme = null;
    public boolean googleAPIAuthInProgress = false;
    public GoogleApiClient googleClient = null;
    public String marketName = null;
    AlertDialog alertDialog = null;
    String AUTH_TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email";
    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
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

    public static int dpToPixels(float dp, Context context) {
        Resources resources = context.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                resources.getDisplayMetrics());
    }

    public static DefaultHttpClient getThreadSafeHttpClient() {

        DefaultHttpClient client = new DefaultHttpClient();
        ClientConnectionManager mgr = client.getConnectionManager();
        HttpParams params = client.getParams();
        client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
                mgr.getSchemeRegistry()), params);
        return client;
    }

    public static void setAlphaCompat(View view, float alpha) {
        view.setAlpha(alpha);
    }

    public String getMarketName() {
        if (marketName == null) {
            String osname = System.getProperty("os.name");
            if (osname != null && osname.equals("qnx")) {
                marketName = "bb"; // BlackBerry
            } else { // To be uncommented based upon target market
                //marketName = "google";
                marketName = "fdroid";
                //marketName = "amazon";
                //marketName = "getjar";
                //marketName = "slideme";
            }
        }

        return marketName;
    }

    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(R.xml.analytics_tracker);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
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

    public void connectOnce() {
        if (!connectedonce) {
            getMediNET().connect();
            connectedonce = true;
        }
    }

    public String[] getStartPhrases() {
        String[] start_phrases = {"om tat sat", "om mani padme hum"};
        // start_phrases.add("namo akasagarbhaya om arya kamari mauli svaha");

        return start_phrases;
    }

    public void askToRateApp() {
        if (getMarketName().equals("bb")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("appworld://content/" + (getApplicationContext().getPackageName().equals("sh.ftp.rocketninelabs.meditationassistant") ? "59939924" : "59939922"))
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException e) {
                Log.d("MeditationAssistant", "Couldn't open play store");
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("appworld://content/" + (getApplicationContext().getPackageName().equals("sh.ftp.rocketninelabs.meditationassistant") ? "59939924" : "59939922"))
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } else if (getMarketName().equals("google")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id="
                                + getApplicationContext().getPackageName())
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException e) {
                Log.d("MeditationAssistant", "Couldn't open play store");
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id="
                                + getApplicationContext().getPackageName())
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } else if (getMarketName().equals("amazon")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("amzn://apps/android?p="
                                + getApplicationContext().getPackageName())
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException e) {
                Log.d("MeditationAssistant", "Couldn't open amazon store");
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://www.amazon.com/gp/mas/dl/android?p="
                                + getApplicationContext().getPackageName())
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
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

    public DefaultHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = getThreadSafeHttpClient();
            httpClient.setCookieStore(cookieStore);
        }

        return httpClient;
    }

    public BasicHttpContext getHttpContext() {
        if (cookieStore == null) {
            cookieStore = new BasicCookieStore();

            httpContext = new BasicHttpContext();
            // Bind custom cookie store to the local context
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        }
        return httpContext;
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

        package_name = getApplicationContext().getPackageName();

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
        CookieSyncManager.createInstance(getApplicationContext());

        reminderAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        /* Send the daily notification updated intent just in case the receiver hasn't been called yet */
        Log.d("MeditationAssistant", "Sending initial daily notification updated intent");
        Intent intent = new Intent();
        intent.setAction(MeditationAssistant.ACTION_UPDATED);
        sendBroadcast(intent);
    }

    public void setupGoogleClient(final Activity activity) {
        //if (googleClient != null) {
        ///    return;
        //}
        Log.d("MeditationAssistant", "Setting up Google Fit");

        googleClient = new GoogleApiClient.Builder(activity)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.d("MeditationAssistant", "!!! Connected to Google Fit");
                                // Now you can make calls to the Fitness APIs.
                                // Put application specific code here.
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.d("MeditationAssistant", "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.d("MeditationAssistant", "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.d("MeditationAssistant", "Connection failed. Cause: " + result.toString());
                                if (!result.hasResolution()) {
                                    // Show the localized error dialog
                                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                            activity, 0).show();
                                    return;
                                }
                                // The failure has a resolution. Resolve it.
                                // Called typically when the app is not yet authorized, and an
                                // authorization dialog is displayed to the user.
                                if (!googleAPIAuthInProgress) {
                                    try {
                                        Log.d("MeditationAssistant", "Attempting to resolve failed connection");
                                        googleAPIAuthInProgress = true;
                                        result.startResolutionForResult(activity,
                                                REQUEST_FIT);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.d("MeditationAssistant",
                                                "Exception while starting resolution activity", e);
                                    }
                                }
                            }
                        }
                )
                .build();
    }

    public void sendFitData() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR, -1);
        long startTime = cal.getTimeInMillis();

        Session session = new Session.Builder()
                .setName("No name")
                .setDescription("Duration: blah blah")
                .setIdentifier("meditation" + "11231241")
                .setActivity(FitnessActivities.MEDITATION)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
                .build();

        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                .build();

        // Then, invoke the Sessions API to insert the session and await the result,
// which is possible here because of the AsyncTask. Always include a timeout when
// calling await() to avoid hanging that can occur from the service being shutdown
// because of low memory or other conditions.
        Log.d("MeditationAssistant", "Inserting the session in the History API");
        com.google.android.gms.common.api.Status insertStatus =
                Fitness.SessionsApi.insertSession(googleClient, insertRequest)
                        .await(1, TimeUnit.MINUTES);

// Before querying the session, check to see if the insertion succeeded.
        if (!insertStatus.isSuccess()) {
            Log.d("MeditationAssistant", "There was a problem inserting the session: " +
                    insertStatus.getStatusMessage());
            return;
        }

// At this point, the session has been inserted and can be read.
        Log.i("MeditationAssistant", "Session insert was successful!");
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

        String titleText = getString(R.string.sessionInProgress);
        String contentText = getString(R.string.appName);

		/*
         * if (!getDurationFormatted().equals("") && getTimeToStopMeditate() >
		 * (System .currentTimeMillis() / 1000)) { titleText =
		 * getDurationFormatted(); contentText =
		 * getString(R.string.sessionInProgress); }
		 */

        Notification notification = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentInfo(streaktext)
                .setContentIntent(pIntent)
                .addAction(R.drawable.ic_action_pause,
                        getString(R.string.pause), pIntentPause)
                .addAction(R.drawable.ic_action_stop,
                        getString(R.string.end), pIntentEnd).build();

        // .setContentText(contentText).setOngoing(true)
        // .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
        // notification_icon))
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

            AlertDialog accountsAlertDialog = new AlertDialog.Builder(activity)
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
                                showOpenIDSignInDialog(activity);
                            }
                        }
                    })
                    .create();
            accountsAlertDialog.show();

            return accountsAlertDialog;
        } else {
            return showOpenIDSignInDialog(activity);
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

    public Boolean getMAIsAppFull() {
        if (appFull == null) {
            appFull = !(getPackageName().equals(
                    "sh.ftp.rocketninelabs.meditationassistant"));
        }

        return appFull;
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