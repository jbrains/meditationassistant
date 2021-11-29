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
    void happyPath() {
        Assertions.assertEquals(
                LocalDateJunkDrawer.localDateFromTimeInSeconds(0L),
                wip(0L)
        );
    }

    private LocalDate wip(long l) {
        return LocalDate.of(1969, 12, 31);
    }
}
