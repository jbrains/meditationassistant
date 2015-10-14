package sh.ftp.rocketninelabs.meditationassistant;

import android.view.View;
import android.view.animation.Animation;

public class FadeOutAnimationListener implements Animation.AnimationListener {
    View view;

    @Override
    public void onAnimationEnd(Animation animation) {
        view.setVisibility(View.GONE);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    public void setView(View v) {
        view = v;
    }
}