package com.tidewater.freight;

/**
 * Everything that is specific to one shipping zone. A new zone is a new implementation or
 * a new instance of an existing one, registered with the rate repository; the calculator
 * does not change.
 */
public interface ZonePricing {

    String zone();

    /** The zone's price for this weight, before the heavy-item surcharge and service level. */
    double baseCost(double weightKg);
}
