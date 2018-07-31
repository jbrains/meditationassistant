package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class JavaScriptInterface implements JavascriptCallback {
    Context context;
    MediNETActivity activity;

    public JavaScriptInterface(MediNETActivity mediNETActivity,
                               Context applicationContext) {
        context = applicationContext;
        activity = mediNETActivity;
    }

    @JavascriptInterface
    public void askToSignIn() {
        MeditationAssistant ma = (MeditationAssistant) this.activity
                .getApplication();
        ma.startAuth(this.activity, false);
        this.activity.finish();
    }

    @JavascriptInterface
    public void setKey(String key, String provider) {
        Log.d("MeditationAssistant", "Setting key" + key);
        MeditationAssistant ma = (MeditationAssistant) this.activity
                .getApplication();
        ma.setMediNETKey(key, provider);
        ma.getMediNET().provider = provider;
        ma.getMediNET().connect();
        /*
         * Bundle bundle = new Bundle(); bundle.putString("action",
         * "changekey"); Intent mIntent = new Intent();
         * mIntent.putExtras(bundle);
         * this.activity.setResult(this.activity.RESULT_OK, mIntent);
         */
        this.activity.finish();
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }
}
