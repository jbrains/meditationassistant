package sh.ftp.rocketninelabs.meditationassistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, OnClickListener {
    // ------------------------------------------------------------------------------------------
    // Private attributes :
    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mSplashText, txtHelp, mValueText;
    private Context mContext;

    private MediaPlayer mediaPlayer;

    private String mDialogMessage, mSuffix;
    private int mDefault, mMax, mValue = 0;
    // ------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------
    // Constructor :
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        // Get string value for dialogMessage :
        int mDialogMessageId = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0)
            mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        else mDialogMessage = mContext.getString(mDialogMessageId);

        // Get string value for suffix (text attribute in xml file) :
        int mSuffixId = attrs.getAttributeResourceValue(androidns, "text", 0);
        if (mSuffixId == 0) mSuffix = attrs.getAttributeValue(androidns, "text");
        else mSuffix = mContext.getString(mSuffixId);

        // Get default and max seekbar values :
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(androidns, "max", 100);
    }
    // ------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------
    // DialogPreference methods :
    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        int sevenDP = (int) (7 * mContext.getResources().getDisplayMetrics().density + 0.5f);
        layout.setPadding(sevenDP, sevenDP, sevenDP, sevenDP);

        /*mSplashText = new TextView(mContext);
        mSplashText.setPadding(30, 10, 30, 10);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);*/

        /*mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(32);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);*/

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);

        txtHelp = new TextView(mContext);
        txtHelp.setTextSize(13);
        txtHelp.setText(mContext.getString(R.string.pref_sessionvolume_summary));
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 14, 0, 0);
        layout.addView(txtHelp, params);

        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        else
            mValue = (Integer) defaultValue;
    }
    // ------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------
    // OnSeekBarChangeListener methods :
    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        if (fromTouch) {
            previewVolume(value);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {
        previewVolume(seek.getProgress());
    }

    @Override
    public void onStopTrackingTouch(SeekBar seek) {
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getProgress() {
        return mValue;
    }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }
    // ------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------
    // Set the positive button listener and onClick action :
    @Override
    public void showDialog(Bundle state) {
        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (shouldPersist()) {
            mValue = mSeekBar.getProgress();
            persistInt(mSeekBar.getProgress());
            callChangeListener(Integer.valueOf(mSeekBar.getProgress()));
        }

        getDialog().dismiss();
    }
    // ------------------------------------------------------------------------------------------

    private void previewVolume(int value) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AssetFileDescriptor afd = mContext
                .getResources()
                .openRawResourceFd(MeditationSounds.getMeditationSound("gong"));
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getDeclaredLength());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(((MeditationAssistant) mContext.getApplicationContext()).audioStream())
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
            } else {
                mediaPlayer.setAudioStreamType(((MeditationAssistant) mContext.getApplicationContext()).audioStream());
            }
            float mediaVolume = (float) (value * 0.01);
            mediaPlayer.setVolume(mediaVolume, mediaVolume);
            mediaPlayer
                    .setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(
                                MediaPlayer mp) {
                            mp.start();
                        }
                    });
            mediaPlayer
                    .setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(
                                MediaPlayer mp) {
                            mp.release();
                        }
                    });
            mediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}