package com.tidewater.freight;

/** Thrown when a service-level code is missing or does not match a service Tidewater offers. */
public class UnknownServiceLevelException extends ShippingCostException {

    public UnknownServiceLevelException(String message) {
        super(message);
    }

    public UnknownServiceLevelException(String message, Throwable cause) {
        super(message, cause);
    }
}
