package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TimePicker;

public class WearEditDurationActivity extends Activity {

    private TimePicker editDuration = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WearMeditationAssistant wma = (WearMeditationAssistant) getApplication();

        setContentView(R.layout.wear_editduration);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                editDuration = (TimePicker) stub.findViewById(R.id.editWearDuration);
                editDuration.setIs24HourView(true);

                editDuration.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                    @Override
                    public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                        wma.newDuration = String.valueOf(editDuration.getCurrentHour()) + ":" + String.valueOf(editDuration.getCurrentMinute());
                    }
                });
            }
        });
    }

    void setDuration(View v) {
        finish();
    }
}
