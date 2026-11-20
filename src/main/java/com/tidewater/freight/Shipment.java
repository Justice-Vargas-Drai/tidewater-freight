package com.tidewater.freight;

/**
 * One shipment to be priced. A shipment can only be created with valid details, so the
 * calculator never has to check them.
 */
public record Shipment(String zone, double weightKg, ServiceLevel serviceLevel) {

    public Shipment {
        if (zone == null || zone.isBlank()) {
            throw new InvalidShipmentException("A shipment needs a destination zone");
        }
        if (!Double.isFinite(weightKg) || weightKg <= 0) {
            throw new InvalidShipmentException(
                    "Shipment weight must be a positive number of kg, but was " + weightKg);
        }
        if (serviceLevel == null) {
            throw new InvalidShipmentException("A shipment needs a service level");
        }
    }
}
