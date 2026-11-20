package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Integration test: the real calculator with Tidewater's real zones. The prices were pinned
 * against the inherited calculator before the refactor, and must not change.
 */
class ShippingPriceCharacterizationTest {

    private static final double EXACT = 1e-9;

    private final ShippingCalculator calculator =
            new ShippingCalculator(InMemoryRateRepository.withStandardZones());

    @ParameterizedTest(name = "{0}, {1} kg, {2} costs GHS {3}")
    @CsvSource({
        "DOMESTIC,      10,   STANDARD, 50.0",
        "REGIONAL,      5,    STANDARD, 50.0",
        "INTERNATIONAL, 1,    STANDARD, 40.0",
        "DOMESTIC,      30,   STANDARD, 150.0",
        "DOMESTIC,      30.5, STANDARD, 172.5",
        "DOMESTIC,      31,   STANDARD, 175.0",
        "REGIONAL,      10,   EXPRESS,  135.0",
        "INTERNATIONAL, 40,   EXPRESS,  967.5"
    })
    void chargesTheSamePriceAsTheInheritedCalculator(
            String zone, double weightKg, String serviceLevel, double expectedCost) {
        Shipment shipment = new Shipment(zone, weightKg, ServiceLevel.fromCode(serviceLevel));

        assertEquals(expectedCost, calculator.calculateCost(shipment), EXACT);
    }

    @Test
    void refusesToPriceAnUnknownZoneInsteadOfChargingNothing() {
        Shipment shipment = new Shipment("MOON", 10, ServiceLevel.STANDARD);

        UnknownZoneException failure =
                assertThrows(UnknownZoneException.class, () -> calculator.calculateCost(shipment));
        assertEquals("No pricing rules for shipping zone: MOON", failure.getMessage());
    }
}
