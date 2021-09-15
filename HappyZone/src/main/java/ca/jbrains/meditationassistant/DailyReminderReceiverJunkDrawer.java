package ca.jbrains.meditationassistant;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

public class DailyReminderReceiverJunkDrawer {
    @NotNull
    public static Calendar parseReminderTimeOnlyToTheMinute(String reminderTime) {
        String[] reminderTimeSplit = reminderTime.split(":");
        Integer reminderHour = Integer.valueOf(reminderTimeSplit[0]);
        Integer reminderMinute = Integer.valueOf(reminderTimeSplit[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminderHour);
        calendar.set(Calendar.MINUTE, reminderMinute);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }
}
