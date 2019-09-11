package sh.ftp.rocketninelabs.meditationassistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.MailTo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.Date;
import java.util.TimeZone;

public class MediNETActivity extends Activity {
    final Activity activity = this;
    protected FrameLayout webViewPlaceholder = null;
    protected WebView webView = null;
    private String provider = "";
    private MeditationAssistant ma = null;
    private Handler handler = new Handler();
    private boolean hide_refresh = false;

    public static Intent newEmailIntent(Context context, String address, String subject, String body, String cc) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_CC, cc);
        intent.setType("message/rfc822");
        return intent;
    }

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }

    public String getPageUrl(String page) {
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();

        return MeditationAssistant.URL_MEDINET + "/client_android.php?v="
                + MediNET.version.toString() + "&avn="
                + String.valueOf(getMeditationAssistant().getMAAppVersionNumber()) + "&th="
                + ma.getMAThemeString() + "&tz="
                + TimeZone.getDefault().getID() + "&x="
                + getMeditationAssistant().getMediNETKey() + "&page="
                + page;
    }

    public void goTo(String go_to) {
        String url;

        if (go_to.equals("gpl") || go_to.equals("lgpl")) {
            setTitle("");
            hide_refresh = true;

            if (go_to.equals("gpl")) {
                url = "file:///android_asset/gpl.html";
            } else {
                url = "file:///android_asset/lgpl.html";
            }
        } else {
            switch (go_to) {
                case "community":
                    setTitle(getString(R.string.community));
                    break;
                case "sessions":
                    setTitle(getString(R.string.sessions));
                    break;
                case "account":
                    setTitle(getString(R.string.account));
                    break;
                case "forum":
                    setTitle(getString(R.string.forum));
                    break;
                case "groups":
                    setTitle(getString(R.string.groups));
                    break;
                default:
                    return;
            }

            url = getPageUrl(go_to);
        }

        Log.d("MeditationAssistant", go_to + " - Going to: " + url);

        webView.loadUrl(url);
        setWindowBackground();
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    protected void initUI(Boolean activityOnCreate) {
        webViewPlaceholder = ((FrameLayout) findViewById(R.id.webViewPlaceholder));

        if (webView == null) {
            webView = new WebView(getApplicationContext());

            // potential fixes for keeping zoom
            // webView.getSettings().setLoadWithOverviewMode(true);
            // webView.getSettings().setUseWideViewPort(true);

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int progress) {
                    if (Build.VERSION.SDK_INT >= 15) { // setProgress added API-15
                        activity.setProgress(progress * 100);
                    }
                }
            });

            webView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    WebView.HitTestResult result = webView.getHitTestResult();
                    Log.d("MeditationAssistant", "Hit test");
                    if (result != null) {
                        if (result.getType() == WebView.HitTestResult.IMAGE_TYPE
                                || result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            Log.d("MeditationAssistant",
                                    "Image: " + result.toString());
                            Intent browserIntent = new Intent(
                                    Intent.ACTION_VIEW, Uri.parse(result
                                    .getExtra())
                            );
                            startActivity(browserIntent);
                            return true;
                        } else if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                            Log.d("MeditationAssistant",
                                    "Anchor: " + result.toString());
                            Intent browserIntent = new Intent(
                                    Intent.ACTION_VIEW, Uri.parse(result
                                    .getExtra())
                            );
                            startActivity(browserIntent);
                            return true;
                        }
                    }

                    return false;
                }
            });

            webView.setHapticFeedbackEnabled(true);
            webView.setLongClickable(true);
            // webView.setInitialScale(getMeditationAssistant().getWebViewScale());

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

            webView.setLayoutParams(params);
            webView.getSettings().setSupportZoom(true);
            // webView.getSettings().setBuiltInZoomControls(true);
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            webView.setScrollbarFadingEnabled(true);
            webView.getSettings().setLoadsImagesAutomatically(true);

            if (Build.VERSION.SDK_INT < 11) {
                webView.getSettings();
                webView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                // Fix background flicker
                webView.getSettings();
                webView.setBackgroundColor(Color.argb(1, 0, 0, 0));
            }

            // JavaScript
            webView.addJavascriptInterface(
                    new JavaScriptInterface(this, this.getApplicationContext()),
                    "MA");
            webView.getSettings().setJavaScriptEnabled(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (Uri.parse(url) != null && Uri.parse(url).getHost() != null && Uri.parse(url).getHost().equals("medinet.rocketnine.space")) {
                        if (webView.getTitle() != null && !webView.getTitle().trim().equals("")) {
                            setTitle(webView.getTitle());
                        }
                    } else {
                        /*
                         * setTitle(String.format(getString(R.string.
                         * signInWithProvider), provider));
                         */
                    }

                    setWindowBackground();
                    // view.setInitialScale(getMeditationAssistant().getWebViewScale());
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url != null && url.startsWith("mailto:")) {
                        MailTo mt = MailTo.parse(url);
                        Intent i = newEmailIntent(MediNETActivity.this, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
                        startActivity(i);
                        view.reload();
                        return true;
                    } else {
                        view.loadUrl(url);
                    }
                    return true;
                }
            });

            webView.getSettings().setSupportMultipleWindows(true);
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {
                    WebView.HitTestResult result = view.getHitTestResult();
                    String data = result.getExtra();

                    startActivity(new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(data)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                    return false;
                }
            });
        }

        // webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        webViewPlaceholder.addView(webView);
        // webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);

        if (activityOnCreate) {
            if (getIntent().hasExtra("page")
                    && (getIntent().getStringExtra("page").equals("community")
                    || getIntent().getStringExtra("page").equals(
                    "sessions")
                    || getIntent().getStringExtra("page").equals(
                    "account")
                    || getIntent().getStringExtra("page").equals(
                    "groups")
                    || getIntent().getStringExtra("page").equals(
                    "forum")
                    || getIntent().getStringExtra("page").equals("gpl") || getIntent()
                    .getStringExtra("page").equals("lgpl"))) {
                goTo(getIntent().getStringExtra("page"));
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (webView != null) {
            webViewPlaceholder.removeView(webView);
        }

        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.activity_medinet);

        initUI(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);

        setTheme(getMeditationAssistant().getMATheme());
        setContentView(R.layout.activity_medinet);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        getMeditationAssistant().utility.initializeTracker(this);

        initUI(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.medinet, menu);

        return true;
    }

    @Override
    public void onDestroy() {
        if (getMeditationAssistant().getMediNET() != null) {
            if (getMeditationAssistant().getMediNET().getStatus() != null
                    && getMeditationAssistant().getMediNET().getStatus()
                    .equals("connecting")) {
                getMeditationAssistant().getMediNET().setStatus("disconnected");
            }
            getMeditationAssistant().getMediNET().updated();
        }

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MeditationAssistant",
                "Selected menu item: " + String.valueOf(item.getItemId()));
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.refreshMediNET) {
            webView.loadUrl("javascript:window.location.reload(true)");
            return true;
        } else if (itemId == R.id.menuMediNETCommunity) {
            goTo("community");
            return true;
        } else if (itemId == R.id.menuMediNETSessions) {
            goTo("sessions");
            return true;
        } else if (itemId == R.id.menuMediNETForum) {
            goTo("forum");
            return true;
        } else if (itemId == R.id.menuMediNETGroups) {
            goTo("groups");
            return true;
        } else if (itemId == R.id.menuMediNETAccount) {
            goTo("account");
            return true;
        }/* else if (itemId == R.id.menuMediNETBack) {
            if (webView.canGoBack()) {
                webView.goBack();
            }
            return true;
        } else if (itemId == R.id.menuMediNETForward) {
            if (webView.canGoForward()) {
                webView.goForward();
            }
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (hide_refresh) {
            menu.findItem(R.id.refreshMediNET).setVisible(false);
        } else {
            menu.findItem(R.id.refreshMediNET).setVisible(true);
        }

        super.onPrepareOptionsMenu(menu);

        return true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        webView.saveState(outState);
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

    public void setWindowBackground() {
        if (getMeditationAssistant().getMATheme() != R.style.MeditationDarkTheme && getMeditationAssistant().getMATheme() != R.style.Buddhism) {
            getWindow().setBackgroundDrawable(
                    getResources().getDrawable(
                            android.R.drawable.screen_background_light)
            );
        } else {
            /*getWindow()
                    .setBackgroundDrawable(
                            getResources().getDrawable(
                                    android.R.drawable.background_holo_dark));*/
            webView.getSettings();
            webView.setBackgroundColor(Color.argb(1, 0, 0, 0));
        }
    }
}
