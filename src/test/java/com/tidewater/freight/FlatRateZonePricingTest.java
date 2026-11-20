package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FlatRateZonePricingTest {

    private static final double EXACT = 1e-9;

    @Test
    void baseCostIsRateTimesWeightPlusZoneFee() {
        FlatRateZonePricing regional = new FlatRateZonePricing("REGIONAL", 8.0, 10.0);

        // 8 x 5 + 10
        assertEquals(50.0, regional.baseCost(5), EXACT);
    }

    @Test
    void allowsAZoneWithNoFee() {
        assertDoesNotThrow(() -> new FlatRateZonePricing("DOMESTIC", 5.0, 0.0));
    }

    @ParameterizedTest(name = "rate {0} per kg is refused")
    @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY})
    void refusesARateThatIsNotAPositiveNumber(double ratePerKg) {
        assertThrows(IllegalArgumentException.class,
                () -> new FlatRateZonePricing("DOMESTIC", ratePerKg, 0.0));
    }

    @ParameterizedTest(name = "zone fee {0} is refused")
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY})
    void refusesAFeeThatIsNotZeroOrAPositiveNumber(double zoneFee) {
        assertThrows(IllegalArgumentException.class,
                () -> new FlatRateZonePricing("DOMESTIC", 5.0, zoneFee));
    }

    @Test
    void refusesAMissingZoneName() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlatRateZonePricing(null, 5.0, 0.0));
    }

    @Test
    void refusesABlankZoneName() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlatRateZonePricing(" ", 5.0, 0.0));
    }
}
