package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MediNET {
    public static Integer version = 6;
    // API v1-5 was used on pre Android 4.1 (discontinued) releases
    // API v6 signifies non-discontinued (1.4.1+, Android 4.1+) app version

    public String status = "disconnected";
    public String result = "";
    public Activity activity;
    public MeditationSession session = new MeditationSession();
    public String provider = "";
    public ArrayList<MeditationSession> result_sessions = null;
    public String announcement = "";
    private Boolean debug = false;
    private MeditationAssistant ma = null;
    private MediNETTask task = null;
    private Handler handler = new Handler();
    private Runnable runnable;
    private Runnable runnable2;
    private Boolean runnable_finished = true;
    private AlertDialog alertDialog = null;
    private String browsetopage = "";

    public MediNET(MainActivity _activity) {
        activity = _activity;
        runnable = new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(this);

                try {
                    _activity.updateTexts();
                } catch (Exception e) {
                }
                getMeditationAssistant().updateWidgets();

                runnable_finished = true;
            }
        };
        runnable2 = new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(this);
                Log.d("MeditationAssistant", "Delayed update() running...");
                getMeditationAssistant().getMediNET().updated();
            }
        };
    }

    public static String durationToTimerString(long duration, boolean countDown) {
        int hours = (int) duration / 3600;
        int minutes = ((int) duration % 3600) / 60;
        if (countDown) {
            minutes += 1;
        }

        return String.valueOf(hours) + ":" + String.format("%02d", minutes);
    }

    public void browseTo(MainActivity act, String page) {
        activity = act;
        browsetopage = page;

        if (status.equals("success") || page.equals("community")) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent openActivity = new Intent(activity
                            .getApplicationContext(), MediNETActivity.class);
                    openActivity.putExtra("page", browsetopage);
                    openActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    openActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getMeditationAssistant().getApplicationContext()
                            .startActivity(openActivity);
                }
            });
        } else {
            getMeditationAssistant().startAuth(activity, false);
        }
    }

    public void resetSession() {
        session = new MeditationSession();
    }

    public boolean connect() {
        ConnectivityManager cm = (ConnectivityManager) getMeditationAssistant()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        Boolean internetconnected = (netInfo != null && netInfo.isConnectedOrConnecting());

        if (!internetconnected) {
            Log.d("MeditationAssistant",
                    "Cancelled MediNET connection:  Internet isn't connected");
            return false;
        }

        if (getMeditationAssistant().getMediNETKey().equals("")) {
            getMeditationAssistant().startAuth(activity, false);
            return false;
        }

        runnable_finished = true;
        if (!this.status.equals("success")) {
            JSONObject jobj = new JSONObject();
            try {
                Log.d("MeditationAssistant", "Begin connect");
                jobj.put("x", getMeditationAssistant().getMediNETKey());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }

            // display json object being sent
            // Log.d("MeditationAssistant", "JSON object send: " +
            // jobj.toString());

            status = "connecting";

            updated();
            if (task != null) {
                task.cancel(true);
            }
            task = new MediNETTask();
            task.action = "connect";

            Log.d("MeditationAssistant", "Executing MediNET Task");
            task.doIt(this);
            return true;
        }
        updated();
        return false;
    }

    public Boolean deleteSessionByStarted(long started) {
        if (task != null) {
            task.cancel(true);
        }
        task = new MediNETTask();
        task.action = "deletesession";
        task.actionextra = String.valueOf(started);
        if (debug) {
            task.nextURL += "&debug77";
        }
        task.doIt(this);
        return true;
    }

    public void signInWithAuthToken(String authtoken) {
        if (task != null) {
            task.cancel(true);
        }

        task = new MediNETTask();
        task.action = "signin";
        task.actionextra = authtoken;

        task.doIt(this);
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(MainActivity activity) {
        this.activity = activity;
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.activity.getApplication();
        }
        return ma;
    }

    public MeditationSession getSession() {
        return session;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean postSession(long updateSessionStarted, Activity activity, Runnable onComplete) {
        Log.d("MeditationAssistant", "Session.toString(): " + this.getSession().export().toString());
        if (task != null) {
            task.cancel(true);
        }

        if (activity != null) {
            getMeditationAssistant().getMediNET().activity = activity;
        }

        task = new MediNETTask();
        task.actionextranumber = updateSessionStarted;
        task.action = "uploadsessions";
        task.actionextra = "manualposting";
        task.onComplete = onComplete;
        if (debug) {
            task.nextURL += "&debug77";
        }
        task.doIt(this);

        return true;
    }

    public boolean saveSession(long updateSessionStarted, Boolean manualposting, Boolean posted) {
        Boolean saved;

        Long postedlong = (long) 0;
        if (posted) {
            postedlong = (long) 1;
        }

        // Only add streak if there isn't already a session for that day
        Calendar completedCalendar = Calendar.getInstance();
        completedCalendar.setTimeInMillis(getSession().completed * 1000);
        if (getMeditationAssistant().db.numSessionsByDate(completedCalendar) == 0) {
            if (!manualposting) {
                getMeditationAssistant().addMeditationStreak();

                if (getSession().streakday == 0) {
                    getSession().streakday = getMeditationAssistant().getMeditationStreak().get(0);
                }
            } else {
                Calendar c_midnight = new GregorianCalendar();
                c_midnight.setTime(new Date());
                c_midnight.set(Calendar.HOUR_OF_DAY, 0);
                c_midnight.set(Calendar.MINUTE, 0);
                c_midnight.set(Calendar.SECOND, 0);
                c_midnight.set(Calendar.MILLISECOND, 0);

                if ((c_midnight.getTimeInMillis() / 1000) < getSession().started && (getSession().started - (c_midnight.getTimeInMillis() / 1000)) < 86400) { // After midnight today
                    getMeditationAssistant().addMeditationStreak();

                    if (getSession().streakday == 0) {
                        getSession().streakday = getMeditationAssistant().getMeditationStreak().get(0);
                    }
                } else {
                    c_midnight.add(Calendar.DATE, -1);

                    if ((c_midnight.getTimeInMillis() / 1000) < getSession().started) { // After midnight yesterday
                        getMeditationAssistant().addMeditationStreak(false);

                        if (getSession().streakday == 0) {
                            getSession().streakday = getMeditationAssistant().getMeditationStreak().get(0);
                        }
                    }
                }
            }
        }

        Log.d("MeditationAssistant", "Saving session...");
        saved = getMeditationAssistant().db.addSession(new SessionSQL(getSession().id,
                getSession().started, getSession().completed, getSession().length,
                getSession().message, postedlong, getSession().streakday, getSession().modified), updateSessionStarted);

        resetSession();

        getMeditationAssistant().recalculateMeditationStreak(activity);

        if (!manualposting && getMeditationAssistant().db.getNumSessions() >= 3) {
            getMeditationAssistant().asktorate = true;
        }

        return saved;
    }

    public void signOut() {
        Log.d("MeditationAssistant", "Signing out");
        if (task != null) {
            task.cancel(true);
        }

        status = "stopped";
        getMeditationAssistant().setMediNETKey("", "");
        if (!getMeditationAssistant().getPrefs().getBoolean("pref_autosignin", false)) {
            getMeditationAssistant().getPrefs().edit().putString("key", "").apply();
        }
        updated();
    }

    public Boolean downloadSessions() {
        getMeditationAssistant().shortToast(getMeditationAssistant().getString(R.string.downloadingSessions));
        if (task != null) {
            task.cancel(true);
        }
        task = new MediNETTask();
        task.action = "downloadsessions";
        if (debug) {
            task.nextURL += "&debug77";
        }
        task.doIt(this);
        return true;
    }

    public Boolean uploadSessions() {
        getMeditationAssistant().shortToast(getMeditationAssistant().getString(R.string.uploadingSessions));
        if (task != null) {
            task.cancel(true);
        }
        task = new MediNETTask();
        task.action = "uploadsessions";
        if (debug) {
            task.nextURL += "&debug77";
        }
        task.doIt(this);
        return true;
    }

    public void updateAfterDelay() {
        Log.d("MeditationAssistant", "Update after delay: " + status);
        handler.postDelayed(runnable2, 1750);
    }

    public void updated() {
        Log.d("MeditationAssistant", "updated() " + status);

        getMeditationAssistant().notifyMediNETUpdated();
        if (runnable_finished) {
            runnable_finished = false;
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }
}
