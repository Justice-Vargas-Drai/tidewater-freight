package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the calculator's own rules. The repository is a Mockito stub with simple,
 * made-up rates (GHS 2 per kg plus a GHS 3 zone fee), so each expected price can be worked
 * out by hand and does not depend on Tidewater's real rates.
 */
class ShippingCalculatorTest {

    private static final double EXACT = 1e-9;
    private static final String TEST_ZONE = "TEST";

    private RateRepository rateRepository;
    private ShippingCalculator calculator;

    @BeforeEach
    void setUp() {
        rateRepository = mock(RateRepository.class);
        when(rateRepository.findPricing(TEST_ZONE))
                .thenReturn(new FlatRateZonePricing(TEST_ZONE, 2.0, 3.0));
        calculator = new ShippingCalculator(rateRepository);
    }

    @Test
    void standardShipmentCostsTheZoneBaseCost() {
        // 2 x 10 + 3
        assertEquals(23.0, calculator.calculateCost(shipment(10, ServiceLevel.STANDARD)), EXACT);
    }

    @Test
    void shipmentOfExactlyThirtyKgHasNoHeavySurcharge() {
        // 2 x 30 + 3, no surcharge because the rule is "more than 30 kg"
        assertEquals(63.0, calculator.calculateCost(shipment(30, ServiceLevel.STANDARD)), EXACT);
    }

    @Test
    void shipmentJustOverThirtyKgPaysTheHeavySurcharge() {
        // 2 x 30.5 + 3 + 20
        assertEquals(84.0, calculator.calculateCost(shipment(30.5, ServiceLevel.STANDARD)), EXACT);
    }

    @Test
    void expressMultipliesTheWholeSubtotalIncludingFeesAndSurcharge() {
        // (2 x 40 + 3 + 20) x 1.5
        assertEquals(154.5, calculator.calculateCost(shipment(40, ServiceLevel.EXPRESS)), EXACT);
    }

    @Test
    void unknownZoneFailureFromTheRepositoryReachesTheCaller() {
        UnknownZoneException unknownZone = new UnknownZoneException("MOON");
        when(rateRepository.findPricing("MOON")).thenThrow(unknownZone);
        Shipment toTheMoon = new Shipment("MOON", 10, ServiceLevel.STANDARD);

        UnknownZoneException thrown =
                assertThrows(UnknownZoneException.class, () -> calculator.calculateCost(toTheMoon));
        assertSame(unknownZone, thrown);
    }

    @Test
    void refusesToBeCreatedWithoutARateRepository() {
        assertThrows(NullPointerException.class, () -> new ShippingCalculator(null));
    }

    @Test
    void refusesToPriceAMissingShipment() {
        assertThrows(NullPointerException.class, () -> calculator.calculateCost(null));
    }

    private static Shipment shipment(double weightKg, ServiceLevel serviceLevel) {
        return new Shipment(TEST_ZONE, weightKg, serviceLevel);
    }
}
