package ca.jbrains.meditationassistant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.zone.TzdbZoneRulesProvider;
import org.threeten.bp.zone.ZoneRulesProvider;

import java.io.InputStream;

public class LocalDateFromTimeInSecondsTest {

    @BeforeEach
    void setUp() {
        if (ZoneRulesProvider.getAvailableZoneIds().isEmpty()) {
            InputStream stream = LocalDateFromTimeInSecondsTest.class.getClassLoader()
                    .getResourceAsStream("TZDB.dat");

            TzdbZoneRulesProvider tzdbZoneRulesProvider = new TzdbZoneRulesProvider(stream);

            ZoneRulesProvider.registerProvider(tzdbZoneRulesProvider);
        }
    }

    @Test
    void happyPath() {
        LocalDate expected = LocalDateJunkDrawer.localDateFromTimeInSeconds(0L);

        Assertions.assertEquals(expected, wip(0L));
    }

    private LocalDate wip(long l) {
        return LocalDate.now();
    }
}
