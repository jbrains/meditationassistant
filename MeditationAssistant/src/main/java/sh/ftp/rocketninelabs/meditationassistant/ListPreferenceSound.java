package sh.ftp.rocketninelabs.meditationassistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;

public class ListPreferenceSound extends ListPreference {
    private int mClickedDialogEntryIndex;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private CharSequence mSummary;
    private String mValue;
    private MediaPlayer mMediaPlayer = null;
    private Context ctx = null;
    private SharedPreferences prefs = null;

    public ListPreferenceSound(Context context) {
        this(context, null);
    }

    public ListPreferenceSound(Context context, AttributeSet attrs) {
        super(context, attrs);
        ctx = context;

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ListPreference, 0, 0);
        // mEntries = a.getTextArray(R.styleable.ListPreference_entries);
        // mEntryValues = a.getTextArray(R.styleable.ListPreference_entryValues);
        setEntries(a.getTextArray(R.styleable.ListPreference_entries));
        setEntryValues(a.getTextArray(R.styleable.ListPreference_entryValues));

        a.recycle();
        mSummary = super.getSummary();
    }

    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public CharSequence[] getEntries() {
        return mEntries;
    }

    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }

    public void setEntries(int entriesResId) {
        setEntries(getContext().getResources().getTextArray(entriesResId));
    }

    public CharSequence getEntry() {
        int index = getValueIndex();
        return index >= 0 && mEntries != null ? mEntries[index] : null;
    }

    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }

    public void setEntryValues(CharSequence[] entryValues) {
        mEntryValues = entryValues;
    }

    public void setEntryValues(int entryValuesResId) {
        setEntryValues(getContext().getResources().getTextArray(
                entryValuesResId));
    }

    @Override
    public CharSequence getSummary() {
        final CharSequence entry = getEntry();
        if (mSummary == null || entry == null) {
            return super.getSummary();
        } else {
            //Log.d("MeditationAssistant", "getsummary(): " + String.valueOf(mSummary) + " " + String.valueOf(entry));
            try {
                return String.format(mSummary.toString(), entry);
            } catch (Exception e) {
                e.printStackTrace();
                return mSummary.toString();
            }
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        if (summary == null && mSummary != null) {
            mSummary = null;
        } else if (summary != null && !summary.equals(mSummary)) {
            mSummary = summary;
        }
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = value;
        persistString(value);
    }

    private int getValueIndex() {
        return findIndexOfValue(mValue);
    }

    public void setValueIndex(int index) {
        if (mEntryValues != null) {
            setValue(mEntryValues[index].toString());
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                }
            } catch (Exception e) {
                Log.d("MeditationAssistant", "Got exception while stopping and resetting sound in ListPreferenceSound");
                e.printStackTrace();
            }
        }
        if (positiveResult && mClickedDialogEntryIndex >= 0
                && mEntryValues != null) {
            String value = mEntryValues[mClickedDialogEntryIndex].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (mEntries == null || mEntryValues == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }
        mClickedDialogEntryIndex = getValueIndex();
        builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mClickedDialogEntryIndex = which;
                        String itemSelected = mEntryValues[mClickedDialogEntryIndex]
                                .toString();

                        Log.d("MeditationAssistant",
                                "Selected: " + String.valueOf(which) + " - "
                                        + itemSelected
                        );

                        if (itemSelected.equals("custom")) {
                            ListPreferenceSound.this.onClick(dialog,
                                    DialogInterface.BUTTON_POSITIVE);
                            dialog.dismiss();
                        } else if (!itemSelected.equals("none")) {
                            if (mMediaPlayer != null) {
                                try {
                                    if (mMediaPlayer.isPlaying()) {
                                        mMediaPlayer.stop();
                                        mMediaPlayer.reset();
                                    }
                                } catch (Exception e) {
                                    Log.d("MeditationAssistant", "Got exception while stopping and resetting sound in ListPreferenceSound");
                                    e.printStackTrace();
                                }
                            }

                            float mediaVolume = (float) (getPrefs().getInt("pref_sessionvolume", 50) * 0.01);

                            AssetFileDescriptor afd = ctx
                                    .getResources()
                                    .openRawResourceFd(
                                            MeditationSounds
                                                    .getMeditationSound(itemSelected)
                                    );
                            try {
                                mMediaPlayer = new MediaPlayer();
                                mMediaPlayer.setDataSource(
                                        afd.getFileDescriptor(),
                                        afd.getStartOffset(),
                                        afd.getDeclaredLength());
                                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                mMediaPlayer.setVolume(mediaVolume, mediaVolume);
                                mMediaPlayer.prepareAsync();
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if (mMediaPlayer != null) {
                                mMediaPlayer
                                        .setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                                            @Override
                                            public void onCompletion(
                                                    MediaPlayer mp) {
                                                mp.release();
                                            }
                                        });
                                mMediaPlayer
                                        .setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            @Override
                                            public void onPrepared(
                                                    MediaPlayer mp) {
                                                mp.start();
                                            }
                                        });
                            }
                        }
                    }
                }
        );
        builder.setPositiveButton(
                builder.getContext().getString(R.string.set),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("MeditationAssistant", "Set clicked");
                        ListPreferenceSound.this.onClick(dialog,
                                DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                }
        );
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        mValue = restoreValue ? getPersistedString(mValue)
                : (String) defaultValue;
    }

    private static class SavedState extends BaseSavedState {
        String value;

        public SavedState(Parcel source) {
            super(source);
            value = source.readString();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(value);
        }
    }

    public SharedPreferences getPrefs() {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        }
        return prefs;
    }
}
