package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TimePreference extends DialogPreference {
    private int lastHour = 0;
    private int lastMinute = 0;
    private TimePicker picker = null;
    private Boolean is24hour = true;
    private Integer maxhours = 24;
    private Context ctx = null;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        ctx = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePreference);
        is24hour = a.getBoolean(R.styleable.TimePreference_is24hour, false);
        maxhours = a.getInteger(R.styleable.TimePreference_maxHours, 24);
        a.recycle();

        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }

    public static int getHour(String time) {
        String[] pieces = time.split(":");

        return (Integer.parseInt(pieces[0]));
    }

    public static int getMinute(String time) {
        String[] pieces = time.split(":");

        return (Integer.parseInt(pieces[1]));
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        picker.setIs24HourView(is24hour);
        if (maxhours != 24) {
            picker.mHourSpinner.setMaxValue(maxhours);
        }

        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        picker.setCurrentHour(lastHour);
        picker.setCurrentMinute(lastMinute);

        Log.d("MeditationAssistant", "TimePreference onBindDialogView: " + String.valueOf(lastHour) + ":" + String.valueOf(lastMinute));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            lastHour = picker.getCurrentHour();
            lastMinute = picker.getCurrentMinute();

            String time = String.valueOf(lastHour) + ":" + String.valueOf(lastMinute);

            Log.d("MeditationAssistant", "TimePreference positive result: " + time);

            if (callChangeListener(time)) {
                persistString(time);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time = null;
        String defaultv = "00:00";

        if (restoreValue) {
            if (defaultValue == null) {
                time = getPersistedString(defaultv);
            } else {
                time = getPersistedString(defaultValue.toString());
            }
        } else {
            time = defaultValue.toString();
        }

        /*if (getKey() != null) {
            time = PreferenceManager.getDefaultSharedPreferences(ctx).getString(getKey(), getKey().equals("pref_session_delay") ? "00:15" : (getKey().equals("pref_daily_reminder") ? "19:00" : "00:00"));
        }*/
        Log.d("MeditationAssistant", String.valueOf(getKey()) + " current value - " + PreferenceManager.getDefaultSharedPreferences(ctx).getString(getKey(), ""));

        Log.d("MeditationAssistant", "TimePreference (" + String.valueOf(getKey()) + ") restoreValue: " + String.valueOf(restoreValue) + " - defaultValue: " + String.valueOf(defaultValue) + " - defaultv: " + String.valueOf(defaultv) + " - time: " + String.valueOf(time));

        lastHour = getHour(time);
        lastMinute = getMinute(time);
    }
}
