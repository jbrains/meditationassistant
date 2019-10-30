package sh.ftp.rocketninelabs.meditationassistant;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetPresetProvider extends AppWidgetProvider {

    private int[] mergeInts(int[] arg1, int[] arg2) {
        int[] result = new int[arg1.length + arg2.length];
        System.arraycopy(arg1, 0, result, 0, arg1.length);
        System.arraycopy(arg2, 0, result, arg1.length, arg2.length);
        return result;
    }

    @Override
    public void onEnabled(Context context) {
        Log.d("MeditationAssistant", "Widget onEnabled");
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            PresetWidgetActivity.deleteWidgetPrefs(context, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int widgetId) {
        RemoteViews updateViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_preset);

        MeditationAssistant ma = (MeditationAssistant) context.getApplicationContext();

        updateViews.setTextColor(R.id.txtWidgetPresetText, ma.getPrefs().getInt("pref_widgetcolor", -16777216));

        CharSequence widgetText = PresetWidgetActivity.loadTitlePref(context, widgetId);
        if (widgetText == "") {
            widgetText = "Preset " + PresetWidgetActivity.loadPresetPref(context, widgetId);
        }
        updateViews.setTextViewText(R.id.txtWidgetPresetText, widgetText);

        Intent clickintent = new Intent(context, MainActivity.class);
        clickintent.setAction("widgetclick");
        clickintent.putExtra("widgetid", widgetId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, widgetId, clickintent, 0);
        updateViews.setOnClickPendingIntent(R.id.layWidget, pendingIntent);
        updateViews.setOnClickPendingIntent(R.id.txtWidgetPresetText, pendingIntent);

        appWidgetManager.updateAppWidget(widgetId, updateViews);
    }
}
