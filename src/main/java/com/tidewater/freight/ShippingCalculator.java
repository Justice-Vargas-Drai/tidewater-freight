package com.tidewater.freight;

public class ShippingCalculator {

    // Temporary: still hard-wired; the next step injects the repository.
    private final RateRepository repository = InMemoryRateRepository.withStandardZones();

    public double calculateCost(String zone, double weightKg, String serviceLevel) {
        try {
            double cost = repository.findPricing(zone).baseCost(weightKg);

            // heavy-item surcharge
            if (weightKg > 30) {
                cost = cost + 20;
            }

            // express costs more
            if (serviceLevel.equals("EXPRESS")) {
                cost = cost * 1.5;
            }

            System.out.println("Shipping " + zone + " (" + weightKg + "kg, " + serviceLevel + "): GHS " + cost);
            return cost;
        } catch (Exception e) {
            // could not work out a rate (for example an unknown zone) - return zero
            return 0;
        }
    }
}
