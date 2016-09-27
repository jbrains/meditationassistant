package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends Activity {

    public String license = "";
    private MeditationAssistant ma = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(getMeditationAssistant().getMATheme());
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_about);

        TextView txtAboutAppName = (TextView) findViewById(R.id.txtAboutAppName);
        TextView txtAboutAppVersion = (TextView) findViewById(R.id.txtAboutAppVersion);

        if (BuildConfig.FLAVOR.equals("free")) {
            txtAboutAppName.setText(getString(R.string.appName));

            Button btnDonate = (Button) findViewById(R.id.btnDonate);
            btnDonate.setText(getString(R.string.removeAds));
        } else {
            txtAboutAppName.setText(getString(R.string.appNameShort));
        }

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

    public void sendMeEmail(View view) {
        Log.d("MeditationAssistant", "Open about");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL,
                new String[]{"tslocum@gmail.com"});
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(
                    getPackageName(), 0);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.appNameShort) + " "
                    + pInfo.versionName + " (" + getMeditationAssistant().capitalizeFirst(getMeditationAssistant().getMarketName()) + ")");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.appNameShort) + " (" + getMeditationAssistant().capitalizeFirst(getMeditationAssistant().getMarketName()) + ")");
        }
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

        startActivity(Intent.createChooser(intent, getString(R.string.sendEmail)));
    }

    public void openHowToMeditate(View view) {
        startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://medinet.ftp.sh/howtomeditate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void openTranslate(View view) {
        startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://medinet.ftp.sh/translate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void openDonate(View view) {
        if (BuildConfig.FLAVOR.equals("free")) {
            AlertDialog removeAdsDialog = new AlertDialog.Builder(this)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    if (getMeditationAssistant().getMarketName().equals("bb")) {
                                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("https://appworld.blackberry.com/webstore/content/59939922/")), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    } else if (getMeditationAssistant().getMarketName().equals("google")) {
                                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=sh.ftp.rocketninelabs.meditationassistant.full")), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    } else if (getMeditationAssistant().getMarketName().equals("amazon")) {
                                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=sh.ftp.rocketninelabs.meditationassistant.full")), getString(R.string.openWith)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    }
                                }
                            }
                    )
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {

                                }
                            })
                    .setTitle(getString(R.string.removeAds))
                    .setMessage(getString(R.string.removeAdsHelp))
                    .setIcon(getResources()
                                    .getDrawable(
                                            getTheme()
                                                    .obtainStyledAttributes(
                                                            getMeditationAssistant().getMATheme(true),
                                                            new int[]{R.attr.actionIconInfo})
                                                    .getResourceId(0, 0)
                                    )
                    ).create();

            removeAdsDialog.show();
        } else {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://medinet.ftp.sh/donate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }
}
