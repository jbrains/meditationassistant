package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends Activity {

    public String license = "";
    private MeditationAssistant ma = null;
    private int easterEggTaps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(getMeditationAssistant().getMATheme());
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_about);

        TextView txtAboutAppName = (TextView) findViewById(R.id.txtAboutAppName);
        TextView txtAboutAppVersion = (TextView) findViewById(R.id.txtAboutAppVersion);

        txtAboutAppName.setText(getString(R.string.appNameShort));

        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            txtAboutAppVersion.setText(String.format(getString(R.string.version), pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            txtAboutAppVersion.setVisibility(View.GONE);
            e.printStackTrace();
        }

        LinearLayout layAbout = (LinearLayout) findViewById(R.id.layAbout);
        layAbout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                ImageView charis = (ImageView) findViewById(R.id.charis);
                charis.setVisibility(View.VISIBLE);
                charis.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(), R.anim.spin));

                easterEggTaps++;
                if (easterEggTaps == 3) {
                    getMeditationAssistant().longToast("Hold again to send the app developer an application log (to help with debugging) via email");
                } else if (easterEggTaps == 4) {
                    getMeditationAssistant().sendLogcat();
                }

                return true;
            }
        });

        getMeditationAssistant().utility.initializeTracker(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.about, menu);

        if (!getMeditationAssistant().getMarketName().equals("google")) {
            MenuItem share = menu.findItem(R.id.action_share_app);
            share.setVisible(false);
        }

        if (getMeditationAssistant().getMarketName().equals("fdroid")) {
            MenuItem rate = menu.findItem(R.id.action_rate);
            rate.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (i == R.id.action_share_app) {
            try {
                Intent intent = getMeditationAssistant().utility.getAppShareIntent();
                startActivityForResult(intent, 1337);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (i == R.id.action_rate) {
            getMeditationAssistant().rateApp();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    public void learnMore(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MeditationAssistant.URL_SOURCE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void openHowToMeditate(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MeditationAssistant.URL_MEDINET + "/howtomeditate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void openTranslate(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MeditationAssistant.URL_MEDINET + "/translate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void openDonate(View view) {
        getMeditationAssistant().showDonationDialog(AboutActivity.this);
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }
}
