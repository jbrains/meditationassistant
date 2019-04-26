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

        if (intent != null && intent.getAction() != null && intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            AppWidgetManager gm = AppWidgetManager.getInstance(context);
            int[] ids = gm.getAppWidgetIds(new ComponentName(context, MeditationProvider.class));
            ids = mergeInts(ids, gm.getAppWidgetIds(new ComponentName(context, MeditationProvider2.class)));
            ids = mergeInts(ids, gm.getAppWidgetIds(new ComponentName(context, MeditationProvider3.class)));

            onUpdate(context, gm, ids);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.startService(intent);

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
