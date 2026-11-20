package com.tidewater.freight;

/**
 * Base type for every reason a shipment cannot be priced. Callers that only need to know
 * "no valid quote" can catch this one type; callers that care why can catch a subtype.
 */
public class ShippingCostException extends RuntimeException {

    public ShippingCostException(String message) {
        super(message);
    }

    public ShippingCostException(String message, Throwable cause) {
        super(message, cause);
    }
}
