package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShipmentTest {

    @Test
    void keepsTheDetailsOfAValidShipment() {
        Shipment shipment = new Shipment("DOMESTIC", 0.5, ServiceLevel.EXPRESS);

        assertEquals("DOMESTIC", shipment.zone());
        assertEquals(0.5, shipment.weightKg());
        assertEquals(ServiceLevel.EXPRESS, shipment.serviceLevel());
    }

    @ParameterizedTest(name = "weight {0} kg is refused")
    @ValueSource(doubles = {
        0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
    })
    void refusesAWeightThatIsNotAPositiveNumber(double weightKg) {
        InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
                () -> new Shipment("DOMESTIC", weightKg, ServiceLevel.STANDARD));

        assertEquals("Shipment weight must be a positive number of kg, but was " + weightKg,
                failure.getMessage());
    }

    @ParameterizedTest(name = "zone \"{0}\" is refused")
    @ValueSource(strings = {"", "   "})
    void refusesABlankZone(String zone) {
        assertThrows(InvalidShipmentException.class,
                () -> new Shipment(zone, 10, ServiceLevel.STANDARD));
    }

    @Test
    void refusesAMissingZone() {
        assertThrows(InvalidShipmentException.class,
                () -> new Shipment(null, 10, ServiceLevel.STANDARD));
    }

    @Test
    void refusesAMissingServiceLevel() {
        assertThrows(InvalidShipmentException.class, () -> new Shipment("DOMESTIC", 10, null));
    }
}
