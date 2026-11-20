package com.tidewater.freight;

/** Thrown when no pricing rules exist for the requested zone. */
public class UnknownZoneException extends ShippingCostException {

    public UnknownZoneException(String zone) {
        super("No pricing rules for shipping zone: " + zone);
    }
}
