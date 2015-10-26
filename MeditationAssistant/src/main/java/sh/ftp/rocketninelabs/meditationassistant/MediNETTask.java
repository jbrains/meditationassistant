package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

public class MediNETTask extends AsyncTask<MediNET, Integer, MediNET> {
    public Context context;
    public String nextURL = null;
    public String action = "";
    public String actionextra = "";
    public MediNET medinet;
    public FragmentActivity fragment_activity = null;
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
        String appVersion = getMeditationAssistant().getMAAppVersion() + BuildConfig.FLAVOR;

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

        ArrayList<SessionSQL> sessionssql = null;

        HashMap<String, String> postData = new HashMap<String, String>();
        try {
            postData.put("x", medinet
                    .getMeditationAssistant().getMediNETKey());

            if (action.equals("connect")) {
                postData.put("action", "connect");
            } else if (action.equals("postsession")) {
                postData.put("postsession", Crypt
                        .bytesToHex(crypt.encrypt(medinet.getSession().export()
                                .toString())));
                if (actionextra.equals("manualposting")) {
                    postData.put("manualposting",
                            "true");
                }
            } else if (action.equals("deletesession")) {
                postData.put("action",
                        "deletesession"); // Session start time
                postData.put("session",
                        actionextra);
            } else if (action.equals("syncsessions")) {
                postData.put("action",
                        "syncsessions");
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

                postData.put("uploadsessions", Crypt
                        .bytesToHex(crypt.encrypt(jsonsessions.toString())));
            } else if (action.equals("signout")) {
                postData.put("signout", "signout");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            getMeditationAssistant().longToast(getMeditationAssistant().getString(R.string.sessionNotPosted));
            e.printStackTrace();
        }

        /*


            Log.d("MeditationAssistant", "Post to " + this.nextURL + ", "
                    + postData.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(postData));
            webResponse = getMeditationAssistant().getHttpClient().execute(
                    httpPost);



        HttpPost httpPost = new HttpPost(this.nextURL);
        HttpResponse webResponse = null;
         */

        String result = "";
        HttpURLConnection medinetConnection = null;

        try {
            URL medinetURL = new URL(this.nextURL);
            medinetConnection = (HttpURLConnection) medinetURL.openConnection();

            medinetConnection.setChunkedStreamingMode(0);
            medinetConnection.setRequestMethod("POST");
            medinetConnection.setDoInput(true);
            medinetConnection.setDoOutput(true);

            OutputStream os = medinetConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getMeditationAssistant().getPostDataString(postData));

            medinetConnection.connect();

            writer.flush();
            writer.close();
            os.close();
            int responseCode = medinetConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(medinetConnection.getInputStream()));
                while ((line = br.readLine()) != null) {
                    result += line;
                }
            } else {
                result = "";
            }

            if (result.equals("")) {
                Log.d("MeditationAssistant", "Unable to connect to MediNET");
                medinet.status = "disconnected";

                if (!getMeditationAssistant().getMediNETKey().equals("")
                        && medinet.activity != null) {
                    getMeditationAssistant().longToast(medinet.activity,
                            getMeditationAssistant().getString(R.string.unableToConnect));
                    medinet.activity.updateTextsAsync();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return medinet;
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
        if (medinetConnection.getHeaderField("x-MediNET") != null) {
            if (medinetConnection.getHeaderField("x-MediNET")
                    .equals("signin")) {
                this.medinet.askToSignIn();
            } else {
                if (action.equals("signin") && medinetConnection.getHeaderField("x-MediNET-Key") != null) {
 /* Oauth2 sign in */
                    Log.d("MeditationAssistant", "Header key: "
                            + medinetConnection.getHeaderField("x-MediNET-Key"));
                    if (!medinetConnection.getHeaderField("x-MediNET-Key").equals("")) {
                        getMeditationAssistant().setMediNETKey(medinetConnection.getHeaderField("x-MediNET-Key"), "Google");
                        //getMeditationAssistant().getMediNET().connect();

                        try {
                            getMeditationAssistant().shortToast(
                                    medinet.activity,
                                    getMeditationAssistant().getString(R.string.mediNETConnected));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (medinetConnection.getHeaderField("x-MediNET-Streak") != null) {
                    Log.d("MeditationAssistant", "Header streak: "
                            + medinetConnection.getHeaderField("x-MediNET-Streak"));
                    if (!medinetConnection.getHeaderField("x-MediNET-Streak").equals("")) {
                        String streak_header = medinetConnection.getHeaderField("x-MediNET-Streak");
                        if (streak_header.contains(",")) {
                            getMeditationAssistant().setMeditationStreak(Integer.valueOf(streak_header.split(",")[0]), Integer.valueOf(streak_header.split(",")[1]));
                            getMeditationAssistant().recalculateMeditationStreak();
                        }
                    }
                }
                if (medinetConnection.getHeaderField("x-MediNET-Meditating") != null) {
                    if (!medinetConnection.getHeaderField("x-MediNET-Meditating").equals("")) {
                        // TODO: Was this going to be a meditating-now feature?
                    }
                }
                if (medinetConnection.getHeaderField("x-MediNET-MaxStreak") != null) {
                    if (!medinetConnection.getHeaderField("x-MediNET-MaxStreak").equals("")) {
                        Integer maxstreak = Integer.valueOf(medinetConnection.getHeaderField("x-MediNET-MaxStreak"));
                        if (maxstreak > getMeditationAssistant().getLongestMeditationStreak()) {
                            getMeditationAssistant().setLongestMeditationStreak(maxstreak);
                        }
                    }
                }
            }
            Log.d("MeditationAssistant", "Header: "
                    + medinetConnection.getHeaderField("x-MediNET"));

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

        medinetConnection.disconnect();
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
