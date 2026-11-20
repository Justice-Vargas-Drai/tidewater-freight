package com.tidewater.freight;

/** Supplies the pricing rules for each shipping zone. */
public interface RateRepository {

    /**
     * Returns the pricing rules for a zone.
     *
     * @throws UnknownZoneException if no rules exist for the zone
     */
    ZonePricing findPricing(String zone);
}
