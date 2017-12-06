package sh.ftp.rocketninelabs.meditationassistant;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MeditationProvider extends AppWidgetProvider {

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
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null) {
            Log.d("MeditationAssistant", "Provider intent: " + intent.toString());
        }

        if (intent != null
                && intent.getAction() != null
                && intent.getAction().equals(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            AppWidgetManager gm = AppWidgetManager.getInstance(context);
            int[] ids = gm.getAppWidgetIds(new ComponentName(context,
                    MeditationProvider.class));
            ids = mergeInts(ids, gm.getAppWidgetIds(new ComponentName(context,
                    MeditationProvider2.class)));
            ids = mergeInts(ids, gm.getAppWidgetIds(new ComponentName(context,
                    MeditationProvider3.class)));

            onUpdate(context, gm, ids);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        Intent intent = new Intent(context, MeditationService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.startService(intent);

        String ids_formatted = "";
        for (int id : appWidgetIds) {
            ids_formatted += ", " + String.valueOf(id);
        }
        Log.d("MeditationAssistant", "Widget onUpdate, service started for IDs: " + ids_formatted);

        super.onUpdate(context, appWidgetManager, appWidgetIds);


		/*
         * int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		 * for (int widgetId : allWidgetIds) { RemoteViews remoteViews = new
		 * RemoteViews(context.getPackageName(), R.layout.widget_layout);
		 * 
		 * SActivity ctx = (SActivity) context; Log.d("MeditationAssistant",
		 * "!!!" + ctx.getApplication().toString()); // Set the text
		 * remoteViews.setTextViewText(R.id.txtDays, ctx.getApplication()
		 * .toString());
		 * 
		 * // Register an onClickListener // Intent intent = new Intent(context,
		 * MeditationProvider.class);
		 * 
		 * // intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE); //
		 * intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, //
		 * appWidgetIds);
		 * 
		 * // PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
		 * // 0, intent, PendingIntent.FLAG_UPDATE_CURRENT); //
		 * remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
		 * appWidgetManager.updateAppWidget(widgetId, remoteViews); }
		 */
    }
}
