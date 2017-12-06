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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
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

    @Override
    protected MediNET doInBackground(MediNET... medinets) {
        this.medinet = medinets[0];
        this.medinet.result = "";

        if (isCancelled()) {
            Log.d("MeditationAssistant", "Task cancelled");
            return this.medinet;
        } else {
            Log.d("MeditationAssistant", "MediNET doInBackground...");
        }

        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUTC = tz.getOffset(now.getTime());
        String appVersion = getMeditationAssistant().getMAAppVersion() + BuildConfig.FLAVOR;

        if (this.nextURL == null) {
            this.nextURL = "https://medinet.rocketnine.space/om?v="
                    + MediNET.version.toString() + "&av="
                    + appVersion + "&am="
                    + getMeditationAssistant().getMarketName() + "&avn="
                    + String.valueOf(getMeditationAssistant().getMAAppVersionNumber()) + "&tz="
                    + TimeZone.getDefault().getID();
        }

        Log.d("MeditationAssistant", "URL => " + this.nextURL);

        ArrayList<SessionSQL> sessions = new ArrayList<SessionSQL>();

        HashMap<String, String> postData = new HashMap<String, String>();
        try {
            postData.put("x", medinet
                    .getMeditationAssistant().getMediNETKey());
            postData.put("action",
                    action);

            switch (action) {
                case "signin":
                    postData.put("token",
                            actionextra);
                    break;
                case "deletesession":
                    postData.put("session",
                            actionextra); // Session start time
                    break;
                case "uploadsessions":
                    JSONArray jsonsessions = new JSONArray();

                    if (actionextra.equals("completeposting") || actionextra.equals("manualposting")) {
                        postData.put("postsession", actionextra);
                        sessions.add(new SessionSQL(medinet.getSession().id, medinet.getSession().started, medinet.getSession().completed, medinet.getSession().length, medinet.getSession().message, (long) 0, medinet.getSession().streakday));
                    } else if (getMeditationAssistant().db.getNumSessions() == 0) {
                        getMeditationAssistant().longToast(
                                medinet.activity,
                                getMeditationAssistant().getString(R.string.sessionsNotImported));

                        return medinet;
                    } else {
                        sessions = getMeditationAssistant().db.getAllSessions();
                    }

                    for (SessionSQL up : sessions) {
                        MeditationSession uploadsession = new MeditationSession();
                        uploadsession.id = up._id;
                        uploadsession.length = up._length;
                        uploadsession.started = up._started;
                        uploadsession.completed = up._completed;
                        uploadsession.streakday = up._streakday;
                        uploadsession.message = up._message;

                        jsonsessions.put(uploadsession.export());
                    }

                    postData.put("uploadsessions", jsonsessions.toString());
                    break;
            }
        } catch (Exception e) {
            getMeditationAssistant().longToast(getMeditationAssistant().getString(R.string.sessionNotPosted));
            e.printStackTrace();
        }
        try {
            Log.i("MA", "Post data: " + getMeditationAssistant().getPostDataString(postData));
        } catch (UnsupportedEncodingException e) {
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
            medinetConnection.setReadTimeout(10000);
            medinetConnection.setConnectTimeout(15000);
            medinetConnection.setRequestMethod("POST");
            medinetConnection.setDoInput(true);
            medinetConnection.setDoOutput(true);

            OutputStream os = medinetConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getMeditationAssistant().getPostDataString(postData));
            writer.flush();
            writer.close();
            os.close();

            medinetConnection.connect();
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

        Log.d("MeditationAssistant", "Result: " + result);
        if (medinetConnection.getHeaderField("x-MediNET") != null) {
            if (medinetConnection.getHeaderField("x-MediNET")
                    .equals("signin")) {
                getMeditationAssistant().startAuth(medinet.activity, false);
            } else {
                if (action.equals("signin") && medinetConnection.getHeaderField("x-MediNET-Key") != null) { /* Oauth2 sign in */
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
                        }
                    } else if (action.equals("uploadsessions")) {
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
                            } else if (actionextra.equals("completeposting")) {
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
                        } else {
                            Integer sessionsuploaded = 0;
                            if (jsonObj.has("sessionsuploaded")) {
                                sessionsuploaded = jsonObj.getInt("sessionsuploaded");
                            }

                            if (sessions.size() > 0 && medinet.result.equals("uploaded") && sessionsuploaded > 0) {
                                for (SessionSQL sessionsql : sessions) {
                                    sessionsql._isposted = (long) 1;
                                    getMeditationAssistant().db.updateSession(sessionsql);
                                }

                                Integer sessuploaded = sessions.size();
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
                        }
                    } else if (action.equals("downloadsessions")) {
                        JSONArray jArray = jsonObj.getJSONArray("downloadsessions");

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
        } else {
            Log.d("MeditationAssistant", "MediNET header was missing!");
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
