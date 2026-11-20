package com.tidewater.freight;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds zone pricing in memory. Each zone's rate and fee live together in one entry. */
public class InMemoryRateRepository implements RateRepository {

    private final Map<String, ZonePricing> pricingByZone = new HashMap<>();

    public InMemoryRateRepository(Collection<? extends ZonePricing> zonePricings) {
        for (ZonePricing pricing : zonePricings) {
            if (pricingByZone.putIfAbsent(pricing.zone(), pricing) != null) {
                throw new IllegalArgumentException(
                        "Pricing for zone " + pricing.zone() + " is defined twice");
            }
        }
    }

    /**
     * Seed data: Tidewater's current zones, with the same rates and fees as the original
     * service. A production rate source would load these from configuration instead.
     */
    public static InMemoryRateRepository withStandardZones() {
        return new InMemoryRateRepository(List.of(
                new FlatRateZonePricing("DOMESTIC", 5.0, 0.0),
                new FlatRateZonePricing("REGIONAL", 8.0, 10.0),
                new FlatRateZonePricing("INTERNATIONAL", 15.0, 25.0)));
    }

    @Override
    public ZonePricing findPricing(String zone) {
        if (zone == null || zone.isBlank()) {
            throw new UnknownZoneException(zone);
        }
        ZonePricing pricing = pricingByZone.get(zone);
        if (pricing == null) {
            throw new UnknownZoneException(zone);
        }
        return pricing;
    }
}
