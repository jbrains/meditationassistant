package ca.jbrains.meditationassistant;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.*;

import java.util.Calendar;

public class LocalDateJunkDrawer {
    // REFACTOR Logically belongs in the Android View Adapter layer/DMZ
    @NotNull
    public static LocalDate localDateFromJavaUtilCalendarComponentValues(int year, int monthOfYear, int dayOfMonth) {
        return LocalDate.of(year, monthOfYear + 1, dayOfMonth);
    }

    public static LocalDate localDateFromTimeInSeconds(Long timeInSeconds) {
        // REFACTOR Compute directly from LocalDate.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInSeconds * 1000);
        return localDateFromJavaUtilCalendarComponentValues(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    public static LocalDate wip(long l) {
        ZoneOffset localTimeZoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());

        return LocalDateTime
                .ofEpochSecond(l, 0, localTimeZoneOffset)
                .toLocalDate();
    }
}
