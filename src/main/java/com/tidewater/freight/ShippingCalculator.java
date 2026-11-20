package com.tidewater.freight;

import java.util.Objects;

/**
 * Works out the cost of a shipment: the zone's base cost, plus the heavy-item surcharge,
 * with the service level applied last to the whole subtotal.
 */
public class ShippingCalculator {

    static final double HEAVY_ITEM_THRESHOLD_KG = 30.0;
    static final double HEAVY_ITEM_SURCHARGE = 20.0;

    private final RateRepository rateRepository;

    public ShippingCalculator(RateRepository rateRepository) {
        this.rateRepository = Objects.requireNonNull(rateRepository, "rateRepository");
    }

    /**
     * Returns the cost of the shipment in GHS.
     *
     * @throws UnknownZoneException if the repository has no pricing for the shipment's zone
     */
    public double calculateCost(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        ZonePricing pricing = rateRepository.findPricing(shipment.zone());
        double subtotal = pricing.baseCost(shipment.weightKg())
                + heavyItemSurcharge(shipment.weightKg());
        return shipment.serviceLevel().applyTo(subtotal);
    }

    private double heavyItemSurcharge(double weightKg) {
        if (weightKg > HEAVY_ITEM_THRESHOLD_KG) {
            return HEAVY_ITEM_SURCHARGE;
        }
        return 0.0;
    }
}
