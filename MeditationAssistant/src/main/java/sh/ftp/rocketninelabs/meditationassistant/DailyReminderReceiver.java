package sh.ftp.rocketninelabs.meditationassistant;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DailyReminderReceiver extends BroadcastReceiver {
    MeditationAssistant ma = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return;
        }
        try {
            ma = (MeditationAssistant) context.getApplicationContext();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (!getMeditationAssistant().getPrefs().getBoolean("pref_daily_reminder", false)) {
            cancelReminder(context);
            return; // The user has not enabled the daily reminder
        }
        Log.d("MeditationAssistant", "onReceive in DailyReminderReceiver");

        if (intent != null && intent.getAction() != null && intent.getAction().equals(MeditationAssistant.ACTION_REMINDER)) { // otherwise, it was just an update
            Log.d("MeditationAssistant", "Daily notification intent!");

            SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy", Locale.US);
            if (getMeditationAssistant().getTimeToStopMeditate() != 0) {
                Log.d("MeditationAssistant", "Skipping daily notification today, session in progress...");
            } else if (getMeditationAssistant().db.numSessionsByDate(Calendar.getInstance()) > 0) {
                Log.d("MeditationAssistant", "Skipping daily notification today, there has already been a session recorded...");
            } else {
                long last_reminder = getMeditationAssistant().getPrefs().getLong("last_reminder", 0);
                if (last_reminder == 0 || getMeditationAssistant().getTimestamp() - last_reminder > 120) {
                    getMeditationAssistant().getPrefs().edit().putLong("last_reminder", getMeditationAssistant().getTimestamp()).apply();

                    String reminderText = getMeditationAssistant().getPrefs().getString("pref_daily_reminder_text", "").trim();
                    if (reminderText.equals("")) {
                        reminderText = context.getString(R.string.reminderText);
                    }

                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentTitle(context.getString(R.string.meditate))
                                    .setContentText(reminderText)
                                    .setTicker(reminderText)
                                    .setAutoCancel(true);

                    if (getMeditationAssistant().getPrefs().getBoolean("pref_vibrate_reminder", true)) {
                        long[] vibrationPattern = {0, 200, 500, 200, 500};
                        notificationBuilder.setVibrate(vibrationPattern);
                    } else {
                        long[] vibrationPattern = {0, 0};
                        notificationBuilder.setVibrate(vibrationPattern);
                    }

                    if (getMeditationAssistant().getPrefs().getBoolean("pref_sound_reminder", true)) {
                        notificationBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
                    }

                    Intent notificationIntent = new Intent(context, MainActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addParentStack(MainActivity.class);
                    stackBuilder.addNextIntent(notificationIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );

                    //Intent launchMain = new Intent(context, MainActivity.class);
                    //PendingIntent launchNotification = PendingIntent.getActivity(context, 1008, launchMain, PendingIntent.FLAG_UPDATE_CURRENT);
                    notificationBuilder.setContentIntent(resultPendingIntent);

                    Notification notification = notificationBuilder.build();

                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(1946, notification);
                }
            }
        }

        String reminderTime = ma.getPrefs().getString("pref_daily_reminder_time", "19:00");
        String[] reminderTimeSplit = ((reminderTime != null && reminderTime != "") ? reminderTime : "19:00").split(":");
        Integer reminderHour = Integer.valueOf(reminderTimeSplit[0]);
        Integer reminderMinute = Integer.valueOf(reminderTimeSplit[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminderHour);
        calendar.set(Calendar.MINUTE, reminderMinute);
        calendar.set(Calendar.SECOND, 0);

        if (Calendar.getInstance().getTimeInMillis() > calendar.getTimeInMillis()) {
            calendar.add(Calendar.DATE, 1); // Tomorrow
        }

        cancelReminder(context);

        getMeditationAssistant().reminderPendingIntent = PendingIntent
                .getBroadcast(
                        context,
                        1946,
                        new Intent(
                                MeditationAssistant.ACTION_REMINDER),
                        PendingIntent.FLAG_CANCEL_CURRENT
                );

        /* Don't use setAlarmClock here as it will always place an alarm icon in the status bar */
        getMeditationAssistant().setAlarm(false, calendar.getTimeInMillis(), getMeditationAssistant().reminderPendingIntent);
        Log.d("MeditationAssistant", "Set daily reminder alarm for " + calendar.toString());
    }

    private void cancelReminder(Context context) {
        if (getMeditationAssistant().reminderPendingIntent != null) {
            try {
                getMeditationAssistant().getAlarmManager().cancel(getMeditationAssistant().reminderPendingIntent);
            } catch (Exception e) {
                Log.e("MeditationAssistant", "AlarmManager update was not canceled. " + e.toString());
            }
            try {
                PendingIntent.getBroadcast(context, 0, new Intent(
                                MeditationAssistant.ACTION_REMINDER),
                        PendingIntent.FLAG_CANCEL_CURRENT
                ).cancel();
            } catch (Exception e) {
                Log.e("MeditationAssistant", "PendingIntent broadcast was not canceled. " + e.toString());
            }
            try {
                getMeditationAssistant().reminderPendingIntent.cancel();
            } catch (Exception e) {
                Log.e("MeditationAssistant", "PendingIntent was not canceled. " + e.toString());
            }
        }
    }

    public MeditationAssistant getMeditationAssistant() {
        return ma;
    }
}