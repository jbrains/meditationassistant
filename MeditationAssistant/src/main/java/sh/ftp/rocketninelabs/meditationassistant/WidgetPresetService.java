package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WidgetPresetService extends Service {
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
                .getApplicationContext());

        if (intent == null) {
            Log.d("MeditationAssistant", "Widget intent was null, exiting...");
            return START_STICKY;
        }

        int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        if (allWidgetIds != null && allWidgetIds.length > 0) {
            for (int widgetId : allWidgetIds) {
                WidgetPresetProvider.updateAppWidget(getApplicationContext(), appWidgetManager, widgetId);
            }
        }

        return START_STICKY;
    }
}
