package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class CompleteActivity extends Activity {
    private MeditationAssistant ma;
    private Runnable completeRunnable = new Runnable() {
        @Override
        public void run() {
            if (getMeditationAssistant().getMediNET().result.equals("posted")) {
                getMeditationAssistant().shortToast(getString(R.string.sessionPosted));
                finish();
            } else if (getMeditationAssistant().getMediNET().result.equals("alreadyposted")) {
                getMeditationAssistant().longToast(getString(R.string.sessionAlreadyPosted));
                finish();
            } else {
                getMeditationAssistant().longToast(getString(R.string.sessionNotPosted));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(getMeditationAssistant().getMATheme());
        setContentView(R.layout.activity_complete);

        getMeditationAssistant().hideNotification(); // Called twice because it seems to help

        if (getMeditationAssistant().getPrefs().getString("pref_full_screen", "").equals("always")) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        long timestamp = System.currentTimeMillis() / 1000;
        if (getMeditationAssistant().getTimeStartMeditate() == 0
                || (timestamp - getMeditationAssistant().getTimeStartMeditate()) <= 0) {
            // Reset timestamps for current session
            getMeditationAssistant().setTimeStartMeditate(0);
            getMeditationAssistant().setTimeToStopMeditate(0);

            getMeditationAssistant().restoreVolume();
            finish();
            return;
        }

        setTheme(getMeditationAssistant().getMATheme());
        /*if (getMeditationAssistant().getMATheme() != R.style.MADark) {
            getWindow().setBackgroundDrawable(
                    getResources().getDrawable(
                            android.R.drawable.screen_background_light));
        }*/

        setContentView(R.layout.activity_complete);

        getWindow().setWindowAnimations(0);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        Boolean manual = false;
        if (getIntent().hasExtra("manual")) {
            if (getIntent().getBooleanExtra("manual", false)) {
                manual = true;
            }
        }

        getMeditationAssistant().unsetNotificationControl();
        getMeditationAssistant().hideNotification(); // Called twice because it seems to help

        EditText editSessionMessage = (EditText) findViewById(R.id.editSessionMessage);
        if (editSessionMessage.getText().toString().equals("")
                && getMeditationAssistant().getPrefs().getBoolean("pref_remembermessage", false)) {
            editSessionMessage.setText(getMeditationAssistant().getPrefs().getString("lastmessage", ""));
        }

        if (getMeditationAssistant()
                .getTimeStartMeditate() == 0) {
            /* Getting null pointers on getTimeStartMeditate() */
            Log.d("MeditationAssistant", "getTimeStartMeditate() was 0!  Exiting AlarmReceiverActivity...");
            finish();
            return;
        }

        // Meditation Session
        getMeditationAssistant().getMediNET().resetSession();
        getMeditationAssistant().getMediNET().session.started = getMeditationAssistant()
                .getTimeStartMeditate();
        if (getMeditationAssistant().getTimerMode().equals("endat")) {
            Log.d("MeditationAssistant", String.valueOf(Math.min(timestamp, getMeditationAssistant().getTimeToStopMeditate())) + " - "
                    + String.valueOf(getMeditationAssistant().getTimeStartMeditate()) + " - " + String.valueOf(getMeditationAssistant().pausetime));
            getMeditationAssistant().getMediNET().session.length = Math.min(timestamp, getMeditationAssistant().getTimeToStopMeditate())
                    - getMeditationAssistant().getTimeStartMeditate() - getMeditationAssistant().pausetime;
        } else {
            Log.d("MeditationAssistant", String.valueOf(timestamp) + " - "
                    + String.valueOf(getMeditationAssistant().getTimeStartMeditate()) + " - " + String.valueOf(getMeditationAssistant().pausetime));
            getMeditationAssistant().getMediNET().session.length = timestamp
                    - getMeditationAssistant().getTimeStartMeditate() - getMeditationAssistant().pausetime;
        }
        getMeditationAssistant().getMediNET().session.length += 7; // Add seven seconds to account for slow wake-ups
        getMeditationAssistant().getMediNET().session.completed = timestamp;

        // Reset timestamps for current session
        getMeditationAssistant().setTimeStartMeditate(0);
        getMeditationAssistant().setTimeToStopMeditate(0);

        Log.d("MeditationAssistant",
                "Session length: "
                        + String.valueOf(getMeditationAssistant().getMediNET().session.length)
        );
        if (getMeditationAssistant().getMediNET().session.length > 0) {
            TextView txtDuration = (TextView) findViewById(R.id.txtDuration);
            txtDuration.setText(MediNET
                    .durationToTimerString(getMeditationAssistant()
                            .getMediNET().session.length, false));

            String text_size = getMeditationAssistant().getPrefs().getString("pref_text_size", "normal");
            if (text_size.equals("tiny")) {
                txtDuration.setTextSize(85);
            } else if (text_size.equals("small")) {
                txtDuration.setTextSize(115);
            } else if (text_size.equals("large")) {
                txtDuration.setTextSize(175);
            } else if (text_size.equals("extralarge")) {
                txtDuration.setTextSize(200);
            } else { // Normal
                txtDuration.setTextSize(153);
            }

            String finishSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_finish", "");
            if (!manual && !finishSoundPath.equals("none")) {
                if (finishSoundPath.equals("custom")) {
                    finishSoundPath = getMeditationAssistant().getPrefs().getString("pref_meditation_sound_finish_custom", "");
                    getMeditationAssistant().playSound(0, finishSoundPath, true);
                } else {
                    getMeditationAssistant().playSound(MeditationSounds.getMeditationSound(finishSoundPath), "", true);
                }
            } else {
                getMeditationAssistant().restoreVolume();
            }
        } else {
            getMeditationAssistant().restoreVolume();
        }

        getMeditationAssistant().utility.initializeTracker(this);

        if (!manual) {
            getMeditationAssistant().vibrateDevice();

            String autosave = getMeditationAssistant().getPrefs().getString("pref_autosave", "");
            if (autosave.equals("save")) {
                saveMediNET(null);
            } else if (autosave.equals("post")) {
                postMediNET(null);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.complete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void askDismiss() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.askDiscardText))
                .setPositiveButton(getString(R.string.discard),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }
                ).setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    public void dismiss(View view) {
        if (getMeditationAssistant().getPrefs().getBoolean("pref_askdismiss", true)) {
            askDismiss();
        } else {
            finish();
        }
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }

    private String getSessionMessage() {
        EditText editSessionMessage = (EditText) findViewById(R.id.editSessionMessage);
        return editSessionMessage.getText().toString().trim();
    }

    @Override
    public void onBackPressed() {
        if (getMeditationAssistant().getPrefs().getBoolean("pref_askdismiss", true)) {
            askDismiss();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
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

    public void postMediNET(View view) {
        if (getMeditationAssistant().getMediNETKey().equals("")) {
            getMeditationAssistant().startAuth(CompleteActivity.this, true);
            return;
        }

        saveLastMessage();

        getMeditationAssistant().shortToast(getString(R.string.sessionPosting));
        getMeditationAssistant().getMediNET().session.message = getSessionMessage();
        getMeditationAssistant().getMediNET().session.modified = getMeditationAssistant().getTimestamp();
        getMeditationAssistant().getMediNET().postSession(0, null, completeRunnable);
    }

    private void saveLastMessage() {
        getMeditationAssistant().getPrefs().edit().putString("lastmessage", getSessionMessage()).apply();
    }

    public void saveMediNET(View view) {
        saveLastMessage();

        getMeditationAssistant().getMediNET().session.message = getSessionMessage();
        getMeditationAssistant().getMediNET().session.modified = getMeditationAssistant().getTimestamp();
        getMeditationAssistant().getMediNET().saveSession(0, false, false);
        getMeditationAssistant().shortToast(getString(R.string.sessionSaved));
        finish();
    }
}
