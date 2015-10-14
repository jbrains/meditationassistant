package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

public class MonthAdapter extends MonthAdapterMA {

    public MonthAdapter(Context c, int month, int year, DisplayMetrics metrics, ProgressActivity _pa,
                        MeditationAssistant _ma) {
        super(c, month, year, metrics, _pa, _ma);
    }

    @Override
    protected void onDate(int[] date, int position, View item) {
    }
}