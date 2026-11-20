package com.tidewater.freight;

/**
 * A zone priced at a fixed rate per kilogram plus a fixed zone fee.
 *
 * <p>Invalid values throw {@link IllegalArgumentException}, not a {@link ShippingCostException}:
 * they are mistakes in Tidewater's zone configuration, not problems with a customer's shipment
 * (see ANALYSIS.md, decision D3).
 */
public record FlatRateZonePricing(String zone, double ratePerKg, double zoneFee)
        implements ZonePricing {

    public FlatRateZonePricing {
        if (zone == null || zone.isBlank()) {
            throw new IllegalArgumentException("Zone pricing needs a zone name");
        }
        if (!Double.isFinite(ratePerKg) || ratePerKg <= 0) {
            throw new IllegalArgumentException(
                    "Rate per kg for " + zone + " must be a positive number, but was " + ratePerKg);
        }
        if (!Double.isFinite(zoneFee) || zoneFee < 0) {
            throw new IllegalArgumentException(
                    "Zone fee for " + zone + " must be zero or a positive number, but was "
                            + zoneFee);
        }
    }

    @Override
    public double baseCost(double weightKg) {
        return ratePerKg * weightKg + zoneFee;
    }
}
