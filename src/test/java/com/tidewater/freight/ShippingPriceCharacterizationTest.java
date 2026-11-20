package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins the prices the inherited calculator produces for valid shipments, so the refactor
 * can prove it changed the structure and not the prices.
 */
class ShippingPriceCharacterizationTest {

    private static final double EXACT = 1e-9;

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
        ShippingCalculator calculator = new ShippingCalculator();

        assertEquals(expectedCost, calculator.calculateCost(zone, weightKg, serviceLevel), EXACT);
    }
}
