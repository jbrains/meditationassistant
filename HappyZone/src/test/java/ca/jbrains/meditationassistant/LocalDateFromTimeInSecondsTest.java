package ca.jbrains.meditationassistant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.bp.zone.TzdbZoneRulesProvider;
import org.threeten.bp.zone.ZoneRulesProvider;

import java.io.InputStream;

// REFACTOR Introduce Parameterized Test pattern
public class LocalDateFromTimeInSecondsTest {

    @BeforeEach
    void setUp() {
        // CONTRACT Without this, we can't use LocalDate with times in tests.
        // Who needs to respect the DIP?! :P
        if (ZoneRulesProvider.getAvailableZoneIds().isEmpty()) {
            InputStream stream = LocalDateFromTimeInSecondsTest.class.getClassLoader()
                    .getResourceAsStream("TZDB.dat");
            Assertions.assertNotNull(stream, "Time Zone database stream is null. Where's the file?!");

            ZoneRulesProvider.registerProvider(new TzdbZoneRulesProvider(stream));
        }
    }

    @Test
    void zero() {
        Assertions.assertEquals(
                LocalDateJunkDrawer.localDateFromTimeInSeconds(0L),
                LocalDateJunkDrawer.localDateFromTimeInSeconds_newVersion(0L)
        );
    }

    @Test
    void one() {
        Assertions.assertEquals(
                LocalDateJunkDrawer.localDateFromTimeInSeconds(1L),
                LocalDateJunkDrawer.localDateFromTimeInSeconds_newVersion(1L)
        );
    }

    @Test
    void nextDay() {
        Assertions.assertEquals(
                LocalDateJunkDrawer.localDateFromTimeInSeconds(12L * 3600),
                LocalDateJunkDrawer.localDateFromTimeInSeconds_newVersion(12L * 3600)
        );
    }

    @Test
    void arbitrarilyLargeNumber() {
        Assertions.assertEquals(
                LocalDateJunkDrawer.localDateFromTimeInSeconds(1287697364597L),
                LocalDateJunkDrawer.localDateFromTimeInSeconds_newVersion(1287697364597L)
        );
    }

}
