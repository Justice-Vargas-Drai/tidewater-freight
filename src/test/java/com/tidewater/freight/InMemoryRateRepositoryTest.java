package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryRateRepositoryTest {

    private final InMemoryRateRepository standardZones = InMemoryRateRepository.withStandardZones();

    @Test
    void holdsTheOriginalRatesAndFeesForEachZone() {
        assertEquals(new FlatRateZonePricing("DOMESTIC", 5.0, 0.0),
                standardZones.findPricing("DOMESTIC"));
        assertEquals(new FlatRateZonePricing("REGIONAL", 8.0, 10.0),
                standardZones.findPricing("REGIONAL"));
        assertEquals(new FlatRateZonePricing("INTERNATIONAL", 15.0, 25.0),
                standardZones.findPricing("INTERNATIONAL"));
    }

    @Test
    void anAddedZoneIsFoundWithoutChangingTheOthers() {
        ZonePricing coastal = new FlatRateZonePricing("COASTAL", 6.0, 4.0);
        InMemoryRateRepository repository = new InMemoryRateRepository(List.of(coastal));

        assertSame(coastal, repository.findPricing("COASTAL"));
    }

    @Test
    void unknownZoneThrowsWithTheZoneInTheMessage() {
        UnknownZoneException failure =
                assertThrows(UnknownZoneException.class, () -> standardZones.findPricing("MOON"));

        assertEquals("No pricing rules for shipping zone: MOON", failure.getMessage());
    }

    @Test
    void missingZoneThrowsUnknownZone() {
        assertThrows(UnknownZoneException.class, () -> standardZones.findPricing(null));
    }

    @Test
    void refusesTheSameZoneDefinedTwice() {
        List<ZonePricing> duplicated = List.of(
                new FlatRateZonePricing("DOMESTIC", 5.0, 0.0),
                new FlatRateZonePricing("DOMESTIC", 6.0, 0.0));

        assertThrows(IllegalArgumentException.class,
                () -> new InMemoryRateRepository(duplicated));
    }
}
