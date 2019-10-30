package sh.ftp.rocketninelabs.meditationassistant;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetStreakService extends Service {
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
                RemoteViews updateViews = new RemoteViews(this.getPackageName(),
                        R.layout.widget_streak);

                MeditationAssistant ma = (MeditationAssistant) this
                        .getApplication();

                updateViews.setTextColor(R.id.txtWidgetStreakDays, ma.getPrefs().getInt("pref_widgetcolor", -16777216));
                updateViews.setTextColor(R.id.txtWidgetStreakText, ma.getPrefs().getInt("pref_widgetcolor", -16777216));

                if (ma.getMeditationStreak().get(0) > 0) {
                    updateViews.setTextViewText(R.id.txtWidgetStreakDays,
                            String.valueOf(ma.getMeditationStreak().get(0)));
                    updateViews.setTextViewText(
                            R.id.txtWidgetStreakText,
                            getResources().getQuantityString(
                                    R.plurals.daysOfMeditationWithoutCount,
                                    ma.getMeditationStreak().get(0).intValue())
                    );
                } else {
                    updateViews.setTextViewText(R.id.txtWidgetStreakDays,
                            getString(R.string.ignore_om));
                    updateViews.setTextViewText(R.id.txtWidgetStreakText,
                            getString(R.string.meditateToday));
                }

                Intent clickintent = new Intent(getApplicationContext(), MainActivity.class);
                clickintent.setAction("widgetclick");
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        getApplicationContext(), 0, clickintent, 0);
                updateViews.setOnClickPendingIntent(R.id.layWidget, pendingIntent);
                updateViews.setOnClickPendingIntent(R.id.txtWidgetStreakDays, pendingIntent);
                updateViews.setOnClickPendingIntent(R.id.txtWidgetStreakText, pendingIntent);

                appWidgetManager.updateAppWidget(widgetId, updateViews);
            }
        }

        return START_STICKY;
    }
}
