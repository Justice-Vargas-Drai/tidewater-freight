package com.tidewater.freight;

import java.util.Arrays;

/** The service levels Tidewater offers, each with the multiplier it applies to a price. */
public enum ServiceLevel {
    STANDARD(1.0),
    EXPRESS(1.5);

    private final double priceMultiplier;

    ServiceLevel(double priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    /** Applies this service level to a subtotal that already includes every fee and surcharge. */
    public double applyTo(double subtotal) {
        return subtotal * priceMultiplier;
    }

    /**
     * Converts an external code such as "EXPRESS" into a service level. Only exact codes are
     * accepted: an unrecognised code is refused rather than quietly priced as standard.
     */
    public static ServiceLevel fromCode(String code) {
        if (code == null) {
            throw new UnknownServiceLevelException("A service level code is required");
        }
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new UnknownServiceLevelException("Unknown service level: " + code
                    + " (expected one of " + Arrays.toString(values()) + ")", e);
        }
    }
}
