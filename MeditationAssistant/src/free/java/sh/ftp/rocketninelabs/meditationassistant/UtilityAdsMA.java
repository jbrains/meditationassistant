package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class UtilityAdsMA {
    public MeditationAssistant ma = null;

    private AdView adMain = null;
    private AdView adProgress = null;
    private AdView adMediNET = null;
    private AdView adComplete = null;

    private MeditationAssistant getMeditationAssistant() {
        return ma;
    }

    public AdView getAdView(Activity activity) {
        if (activity.getClass().getSimpleName().equals("MainActivity")) {
            return adMain;
        }

        return null;
    }

    public void loadAd(Activity activity) {
        RelativeLayout adLayout = null;
        if (activity.getClass().getSimpleName().equals("MainActivity")) {
            adLayout = (RelativeLayout) activity.findViewById(R.id.adMain);
        } else if (activity.getClass().getSimpleName().equals("ProgressActivity")) {
            adLayout = (RelativeLayout) activity.findViewById(R.id.adProgress);
        } else if (activity.getClass().getSimpleName().equals("MediNETActivity")) {
            adLayout = (RelativeLayout) activity.findViewById(R.id.adMediNET);
        } else if (activity.getClass().getSimpleName().equals("CompleteActivity")) {
            adLayout = (RelativeLayout) activity.findViewById(R.id.adComplete);
        }

        if (adLayout != null) {
            Log.d("MeditationAssistant", "Fetching ad");

            AdView av = new AdView(activity);
            av.setAdSize(AdSize.SMART_BANNER);
            av.setAdUnitId("a15110a172d3cff");
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();
            av.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

            if (adLayout.getChildCount() > 0) {
                adLayout.removeAllViews();
            }

            adLayout.addView(av);
            av.loadAd(adRequest);

            if (activity.getClass().getSimpleName().equals("MainActivity")) {
                adMain = av;
            } else if (activity.getClass().getSimpleName().equals("ProgressActivity")) {
                adProgress = av;
            } else if (activity.getClass().getSimpleName().equals("MediNETActivity")) {
                adMediNET = av;
            } else if (activity.getClass().getSimpleName().equals("CompleteActivity")) {
                adComplete = av;
            }
        }
    }

    public void pauseAd(Activity activity) {
        AdView av = getAdView(activity);
        if (av != null) {
            av.pause();
        }
    }

    public void resumeAd(Activity activity) {
        AdView av = getAdView(activity);
        if (av != null) {
            av.resume();
        }
    }

    public void destroyAd(Activity activity) {
        AdView av = getAdView(activity);
        if (av != null) {
            av.destroy();
        }
    }
}