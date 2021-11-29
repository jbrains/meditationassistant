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
        return localDateFromTimeInSeconds_newVersion(timeInSeconds);
    }

    public static LocalDate localDateFromTimeInSeconds_newVersion(long timeInSeconds) {
        ZoneOffset localTimeZoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());

        return LocalDateTime
                .ofEpochSecond(timeInSeconds, 0, localTimeZoneOffset)
                .toLocalDate();
    }
}
