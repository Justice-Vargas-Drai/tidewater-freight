package com.tidewater.freight;

/** Thrown when a service-level code does not match any service Tidewater offers. */
public class UnknownServiceLevelException extends ShippingCostException {

    public UnknownServiceLevelException(String code) {
        super("Unknown service level: " + code);
    }

    public UnknownServiceLevelException(String code, Throwable cause) {
        super("Unknown service level: " + code, cause);
    }
}
