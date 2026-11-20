package com.tidewater.freight;

/** Prints a few sample quotes. This is the one place that writes to the console. */
public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) {
        ShippingCalculator calculator =
                new ShippingCalculator(InMemoryRateRepository.withStandardZones());

        printQuote(calculator, "DOMESTIC", 10, "STANDARD");
        printQuote(calculator, "INTERNATIONAL", 40, "EXPRESS");
        printQuote(calculator, "MOON", 10, "STANDARD");
    }

    private static void printQuote(
            ShippingCalculator calculator, String zone, double weightKg, String serviceLevel) {
        String request = zone + " " + weightKg + "kg " + serviceLevel;
        try {
            Shipment shipment = new Shipment(zone, weightKg, ServiceLevel.fromCode(serviceLevel));
            System.out.println(request + " -> GHS " + calculator.calculateCost(shipment));
        } catch (ShippingCostException e) {
            System.out.println(request + " -> no quote: " + e.getMessage());
        }
    }
}
