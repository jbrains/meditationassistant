package sh.ftp.rocketninelabs.meditationassistant;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class MeditationService extends Service {
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
                .getApplicationContext());
        Log.d("MeditationAssistant", "Widget onStartCommand(): " + String.valueOf(intent));

        if (intent == null) {
            Log.d("MeditationAssistant", "Widget intent was null, exiting...");
            return START_STICKY;
        }

        int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        if (allWidgetIds != null && allWidgetIds.length > 0) {
            for (int widgetId : allWidgetIds) {
                RemoteViews updateViews = new RemoteViews(this.getPackageName(),
                        R.layout.widget_layout);
                getApplication();

                MeditationAssistant ma = (MeditationAssistant) this
                        .getApplication();

                if (ma.getMeditationStreak() > 0) {
                    updateViews.setTextViewText(R.id.txtWidgetDays,
                            String.valueOf(ma.getMeditationStreak()));
                    updateViews.setTextViewText(
                            R.id.txtWidgetText,
                            getResources().getQuantityString(
                                    R.plurals.daysOfMeditationWithoutCount,
                                    ma.getMeditationStreak())
                    );
                } else {
                    updateViews.setTextViewText(R.id.txtWidgetDays,
                            getString(R.string.ignore_om));
                    updateViews.setTextViewText(R.id.txtWidgetText,
                            getString(R.string.meditateToday));
                }

                Intent clickintent = new Intent(getApplicationContext(), MainActivity.class);
                clickintent.setAction("widgetclick");
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        getApplicationContext(), 0, clickintent, 0);
                updateViews.setOnClickPendingIntent(R.id.layWidget, pendingIntent);
                updateViews.setOnClickPendingIntent(R.id.txtWidgetDays, pendingIntent);
                updateViews.setOnClickPendingIntent(R.id.txtWidgetText, pendingIntent);

                appWidgetManager.updateAppWidget(widgetId, updateViews);
            }
        }

        return START_STICKY;
    }
}
