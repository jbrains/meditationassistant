package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class UtilityMA {
    public MeditationAssistant ma = null;
    public GoogleApiClient googleClient = null;
    public HashMap<MeditationAssistant.TrackerName, Tracker> mTrackers = new HashMap<MeditationAssistant.TrackerName, Tracker>();
    public boolean googleAPIAuthInProgress = false;

    private MeditationAssistant getMeditationAssistant() {
        return ma;
    }

    synchronized public Tracker getTracker(Activity activity, MeditationAssistant.TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(activity);
            Tracker t = analytics.newTracker(R.xml.analytics_tracker);
            t.set("Market", getMeditationAssistant().getMarketName());
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    public void initializeTracker(Activity activity) {
        if (getMeditationAssistant().sendUsageReports()) {
            getTracker(activity, MeditationAssistant.TrackerName.APP_TRACKER);
        }
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
                                                MeditationAssistant.REQUEST_FIT);
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

    public void trackingStart(Activity activity) {
        if (getMeditationAssistant().sendUsageReports()) {
            GoogleAnalytics.getInstance(activity).reportActivityStart(activity);
        }
    }

    public void trackingStop(Activity activity) {
        if (getMeditationAssistant().sendUsageReports()) {
            GoogleAnalytics.getInstance(activity).reportActivityStop(activity);
        }
    }

    public void connectGoogleClient() {
        if (googleClient != null) {
            googleClient.connect();
        }
    }

    public void disconnectGoogleClient() {
        if (googleClient != null && googleClient.isConnected()) {
            googleClient.disconnect();
        }
    }

    public void onGoogleClientResult() {
        // Make sure the app is not already connected or attempting to connect
        if (!googleClient.isConnecting() && !googleClient.isConnected()) {
            connectGoogleClient();

            sendFitData();
        }
    }

    public Intent getAppShareIntent() {
        return new AppInviteInvitation.IntentBuilder(getMeditationAssistant().getString(R.string.appNameShort))
                .setMessage(getMeditationAssistant().getString(R.string.invitationBlurb))
                .build();
    }
}