package com.tidewater.freight;

/** Thrown when a shipment's details cannot describe a real shipment, such as a negative weight. */
public class InvalidShipmentException extends ShippingCostException {

    public InvalidShipmentException(String message) {
        super(message);
    }
}
