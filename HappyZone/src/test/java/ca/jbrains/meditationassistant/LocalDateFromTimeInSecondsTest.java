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
            Assertions.assertNotNull(stream, "Time Zone database stream is null. Where's the file?!");

            ZoneRulesProvider.registerProvider(new TzdbZoneRulesProvider(stream));
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
