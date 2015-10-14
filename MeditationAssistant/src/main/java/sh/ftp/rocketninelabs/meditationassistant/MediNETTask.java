package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class MediNETTask extends AsyncTask<MediNET, Integer, MediNET> {
    public Context context;
    public String nextURL = null;
    public String action = "";
    public String actionextra = "";
    public MediNET medinet;
    public String x;
    public FragmentActivity fragment_activity = null;
    private BufferedReader webReader;
    private MeditationAssistant ma = null;
    private Crypt crypt = new Crypt();

    @Override
    protected MediNET doInBackground(MediNET... medinets) {
        this.medinet = medinets[0];
        this.medinet.result = "";

        if (isCancelled()) {
            Log.d("MeditationAssistant", "Task cancelled");
            return this.medinet;
        }

        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUTC = tz.getOffset(now.getTime());
        String appVersion = getMeditationAssistant().getMAAppVersion() + (getMeditationAssistant().getMAIsAppFull() ? "full" : "free");

        if (this.nextURL == null) {
            this.nextURL = "http://medinet.ftp.sh/client_android.php?v="
                    + MediNET.version.toString() + "&av="
                    + appVersion + "&am="
                    + getMeditationAssistant().getMarketName() + "&avn="
                    + String.valueOf(getMeditationAssistant().getMAAppVersionNumber()) + "&tz="
                    + String.valueOf(offsetFromUTC);
        }

        if (action.equals("signin")) {
            this.nextURL = "http://medinet.ftp.sh/client_android_login_oauth2.php?v="
                    + MediNET.version.toString() + "&av="
                    + appVersion + "&avn="
                    + String.valueOf(getMeditationAssistant().getMAAppVersionNumber()) + "&tz="
                    + String.valueOf(offsetFromUTC) + "&token=" + actionextra;
        }

        Log.d("MeditationAssistant", "URL => " + this.nextURL);

        HttpPost httpPost = new HttpPost(this.nextURL);
        HttpResponse webResponse = null;
        ArrayList<SessionSQL> sessionssql = null;

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("x", medinet
                    .getMeditationAssistant().getMediNETKey()));

            if (action.equals("connect")) {
                nameValuePairs.add(new BasicNameValuePair("action", "connect"));
            } else if (action.equals("postsession")) {
                nameValuePairs.add(new BasicNameValuePair("postsession", Crypt
                        .bytesToHex(crypt.encrypt(medinet.getSession().export()
                                .toString()))));
                if (actionextra.equals("manualposting")) {
                    nameValuePairs.add(new BasicNameValuePair("manualposting",
                            "true"));
                }
            } else if (action.equals("deletesession")) {
                nameValuePairs.add(new BasicNameValuePair("action",
                        "deletesession")); // Session start time
                nameValuePairs.add(new BasicNameValuePair("session",
                        actionextra));
            } else if (action.equals("syncsessions")) {
                nameValuePairs.add(new BasicNameValuePair("action",
                        "syncsessions"));
            } else if (action.equals("uploadsessions")) {
                JSONArray jsonsessions = new JSONArray();
                sessionssql = getMeditationAssistant().db.getAllLocalSessions();

                if (sessionssql.size() == 0) {
                    getMeditationAssistant().longToast(
                            medinet.activity,
                            getMeditationAssistant().getString(R.string.sessionsNotImported));

                    return medinet;
                }

                for (SessionSQL uploadsessionsql : sessionssql) {
                    MeditationSession uploadsession = new MeditationSession();
                    uploadsession.id = uploadsessionsql._id;
                    uploadsession.length = uploadsessionsql._length;
                    uploadsession.started = uploadsessionsql._started;
                    uploadsession.completed = uploadsessionsql._completed;
                    uploadsession.streakday = uploadsessionsql._streakday;
                    uploadsession.message = uploadsessionsql._message;

                    jsonsessions.put(uploadsession.export());
                }

                Log.d("MeditationAssistant", jsonsessions.toString());

                nameValuePairs.add(new BasicNameValuePair("uploadsessions", Crypt
                        .bytesToHex(crypt.encrypt(jsonsessions.toString()))));
            } else if (action.equals("signout")) {
                nameValuePairs
                        .add(new BasicNameValuePair("signout", "signout"));
            }

            Log.d("MeditationAssistant", "Post to " + this.nextURL + ", "
                    + nameValuePairs.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            webResponse = getMeditationAssistant().getHttpClient().execute(
                    httpPost, getMeditationAssistant().getHttpContext());
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            getMeditationAssistant().longToast(getMeditationAssistant().getString(R.string.sessionNotPosted));
            e.printStackTrace();
        }

        if (webResponse == null) {
            Log.d("MeditationAssistant", "Unable to connect to MediNET");
            medinet.status = "disconnected";

            if (!getMeditationAssistant().getMediNETKey().equals("")
                    && medinet.activity != null) {
                getMeditationAssistant().longToast(medinet.activity,
                        getMeditationAssistant().getString(R.string.unableToConnect));
                medinet.activity.updateTextsAsync();
            }

            return medinet;
        }
        String result = "";

        try {
            this.webReader = new BufferedReader(new InputStreamReader(
                    webResponse.getEntity().getContent()));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String line;
        try {
            while ((line = this.webReader.readLine()) != null) {
                result += line + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Calendar date = new GregorianCalendar();
        date.setTimeZone(TimeZone.getDefault());
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DAY_OF_MONTH, 2);
        Log.d("MeditationAssistant",
                "two days: " + date.getTimeZone().getDisplayName()
                        + String.valueOf(date.getTimeInMillis() / 1000)
        );

        Log.d("MeditationAssistant", "Result: " + result);
        if (webResponse.getFirstHeader("x-MediNET") != null) {
            if (webResponse.getFirstHeader("x-MediNET").getValue()
                    .equals("signin")) {
                this.medinet.askToSignIn();
            } else {
                if (action.equals("signin") && webResponse.getFirstHeader("x-MediNET-Key") != null) {
 /* Oauth2 sign in */
                    Log.d("MeditationAssistant", "Header key: "
                            + webResponse.getFirstHeader("x-MediNET-Key")
                            .getValue());
                    if (!webResponse.getFirstHeader("x-MediNET-Key")
                            .getValue().equals("")) {
                        getMeditationAssistant().setMediNETKey(webResponse.getFirstHeader("x-MediNET-Key")
                                .getValue(), "Google");
                        //getMeditationAssistant().getMediNET().connect();

                        try {
                            getMeditationAssistant().shortToast(
                                    medinet.activity,
                                    getMeditationAssistant().getString(R.string.mediNETConnected));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (webResponse.getFirstHeader("x-MediNET-Streak") != null) {
                    Log.d("MeditationAssistant", "Header streak: "
                            + webResponse.getFirstHeader("x-MediNET-Streak")
                            .getValue());
                    if (!webResponse.getFirstHeader("x-MediNET-Streak")
                            .getValue().equals("")) {
                        String streak_header = webResponse.getFirstHeader(
                                "x-MediNET-Streak").getValue();
                        if (streak_header.contains(",")) {
                            getMeditationAssistant().setMeditationStreak(Integer.valueOf(streak_header.split(",")[0]), Integer.valueOf(streak_header.split(",")[1]));
                            getMeditationAssistant().recalculateMeditationStreak();
                        }
                    }
                }
                if (webResponse.getFirstHeader("x-MediNET-Meditating") != null) {
                    if (!webResponse.getFirstHeader("x-MediNET-Meditating")
                            .getValue().equals("")) {
                        // TODO: Was this going to be a meditating-now feature?
                    }
                }
                if (webResponse.getFirstHeader("x-MediNET-MaxStreak") != null) {
                    if (!webResponse.getFirstHeader("x-MediNET-MaxStreak")
                            .getValue().equals("")) {
                        Integer maxstreak = Integer.valueOf(webResponse.getFirstHeader("x-MediNET-MaxStreak")
                                .getValue());
                        if (maxstreak > getMeditationAssistant().getLongestMeditationStreak()) {
                            getMeditationAssistant().setLongestMeditationStreak(maxstreak);
                        }
                    }
                }
            }
            Log.d("MeditationAssistant", "Header: "
                    + webResponse.getFirstHeader("x-MediNET").getValue());

            if (!result.equals("")
                    && !result.trim().startsWith("<")) {
                JSONObject jsonObj;
                try {
                    jsonObj = new JSONObject(result);
                    Log.d("MeditationAssistant",
                            "jsonobj: " + jsonObj.toString());
                    if (jsonObj.has("status")) {
                        medinet.status = jsonObj.getString("status");
                    }
                    if (jsonObj.has("result")) {
                        medinet.result = jsonObj.getString("result");
                    }
                    if (jsonObj.has("announce")) {
                        medinet.announcement = jsonObj.getString("announce");
                        try {
                            medinet.activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getMeditationAssistant()
                                            .showAnnouncementDialog(null);
                                }
                            });
                        } catch (Exception e) {
                            Log.d("MeditationAssistant",
                                    "Caught error while parsing announcement...");
                            e.printStackTrace();
                        }
                    }

                    if (action.equals("connect")) {
                        if (!getMeditationAssistant().getPrefs().getBoolean(
                                "asked_staledata", false)) {
                            try {
                                medinet.activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getMeditationAssistant()
                                                .showStaleDataDialog();
                                    }
                                });
                            } catch (Exception e) {
                                Log.d("MeditationAssistant",
                                        "Caught error while showing stale data dialog...");
                                e.printStackTrace();
                            }

                            getMeditationAssistant().getPrefs().edit()
                                    .putBoolean("asked_staledata", true)
                                    .apply();
                        }
                    } else if (action.equals("postsession")) {
                        if (medinet.result.equals("posted")) {
                            if (actionextra.equals("manualposting")) {
                                if (fragment_activity != null) {
                                    fragment_activity
                                            .runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    getMeditationAssistant()
                                                            .shortToast(
                                                                    getMeditationAssistant().getString(R.string.sessionPosted));
                                                }
                                            });
                                }
                            } else {
                                medinet.saveSession(false, true);
                            }
                        } else if (medinet.result.equals("alreadyposted")
                                && actionextra.equals("manualposting")
                                && fragment_activity != null) {
                            fragment_activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getMeditationAssistant().shortToast(
                                            getMeditationAssistant().getString(R.string.sessionAlreadyPosted));
                                }
                            });
                        }
                    } else if (action.equals("deletesession")) {
                        if (medinet.result.equals("deleted")) {
                            Log.d("MeditationAssistant", "Deleted session");

                            getMeditationAssistant().shortToast(
                                    medinet.activity,
                                    getMeditationAssistant().getString(R.string.sessionDeletedMediNET));

                            SessionSQL deletedsession = getMeditationAssistant().db.getSessionByStarted(Long.valueOf(actionextra));
                            if (deletedsession != null) {
                                deletedsession._isposted = (long) 0;
                                try {
                                    getMeditationAssistant().db.updateSession(deletedsession);
                                } catch (Exception e) {
                                    // Do nothing, it was probably deleted locally as well
                                }
                            }
                        } else if (medinet.result.equals("notdeleted")) {
                            getMeditationAssistant().shortToast(
                                    medinet.activity,
                                    getMeditationAssistant().getString(R.string.sessionNotFoundMediNET));
                        } else if (medinet.result.equals("accessdenied")) {
                            getMeditationAssistant()
                                    .shortToast(medinet.activity,
                                            getMeditationAssistant().getString(R.string.sessionNotProperAccount));
                        }
                    } else if (action.equals("uploadsessions")) {
                        if (sessionssql != null && medinet.result.equals("uploaded")) {
                            for (SessionSQL sessionsql : sessionssql) {
                                sessionsql._isposted = (long) 1;
                                getMeditationAssistant().db.updateSession(sessionsql);
                            }

                            Integer sessuploaded = sessionssql.size();
                            getMeditationAssistant().longToast(
                                    medinet.activity,
                                    String.format(getMeditationAssistant().getResources().getQuantityString(
                                            R.plurals.sessionsUploaded, sessuploaded,
                                            sessuploaded), String.valueOf(sessuploaded))
                            );
                        } else {
                            getMeditationAssistant().longToast(
                                    medinet.activity,
                                    getMeditationAssistant().getString(R.string.sessionsNotImported));
                        }
                    } else if (action.equals("syncsessions")) {
                        JSONArray jArray = jsonObj.getJSONArray("syncsessions");

                        Integer sessimported = 0;

                        SessionSQL sess;
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject session = jArray.getJSONObject(i);

                            if (getMeditationAssistant().db
                                    .getSessionByStarted(session
                                            .getLong("started")) == null) {
                                sessimported += 1;

                                sess = new SessionSQL();
                                sess._length = session.getLong("length");
                                sess._started = session.getLong("started");
                                sess._completed = session.getLong("completed"); // Added in API 5
                                sess._message = session.getString("message");
                                sess._streakday = session.getLong("streakday");
                                sess._isposted = (long) 1;

                                Log.d("MeditationAssistant",
                                        "Adding session started at "
                                                + String.valueOf(sess._started)
                                );
                                getMeditationAssistant().db.addSession(sess);
                            } else {
                                Log.d("MeditationAssistant",
                                        "Skipping session " + String.valueOf(session));
                            }
                        }

                        if (sessimported > 0) {
                            getMeditationAssistant().longToast(
                                    medinet.activity,
                                    String.format(getMeditationAssistant().getResources().getQuantityString(
                                            R.plurals.sessionsImported, sessimported,
                                            sessimported), String.valueOf(sessimported))
                            );
                        } else {
                            getMeditationAssistant().longToast(
                                    medinet.activity,
                                    getMeditationAssistant().getString(R.string.sessionsNotImported));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        publishProgress();
        medinet.updateAfterDelay();

        if (context == null) {
            Throwable t = new Throwable();
            t.printStackTrace();
        }

        return medinet;
    }

    public void doIt(MediNET m) {
        try {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m);
        } catch (Exception e) {
            execute(m);
            e.printStackTrace();
        }
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) medinet.activity.getApplication();
        }
        return ma;
    }

    @Override
    protected void onPostExecute(MediNET medinet) {
        Log.d("MeditationAssistant",
                "onPostExecute: " + String.valueOf(action));
        if (action != null) {
            if (action.equals("connect")) {
                try {
                    MainActivity mainActivity = medinet.activity;
                    mainActivity.updateTexts();
                    Log.d("MeditationAssistant",
                            "Updated texts from finished 'connect'");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        Log.d("MeditataionAssistant", "Progress update");
        medinet.updated();
        medinet.activity.updateTextsAsync();
    }
}
