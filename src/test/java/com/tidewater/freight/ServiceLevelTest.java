package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceLevelTest {

    private static final double EXACT = 1e-9;

    @Test
    void standardLeavesTheSubtotalUnchanged() {
        assertEquals(100.0, ServiceLevel.STANDARD.applyTo(100.0), EXACT);
    }

    @Test
    void expressAddsHalfAgain() {
        assertEquals(150.0, ServiceLevel.EXPRESS.applyTo(100.0), EXACT);
    }

    @Test
    void readsTheExactServiceLevelCodes() {
        assertEquals(ServiceLevel.STANDARD, ServiceLevel.fromCode("STANDARD"));
        assertEquals(ServiceLevel.EXPRESS, ServiceLevel.fromCode("EXPRESS"));
    }

    @ParameterizedTest(name = "code \"{0}\" is refused")
    @ValueSource(strings = {"express", "OVERNIGHT", ""})
    void refusesAnUnknownCodeInsteadOfPricingItAsStandard(String code) {
        UnknownServiceLevelException failure =
                assertThrows(UnknownServiceLevelException.class, () -> ServiceLevel.fromCode(code));

        assertEquals("Unknown service level: " + code + " (expected one of [STANDARD, EXPRESS])",
                failure.getMessage());
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }

    @Test
    void refusesAMissingCode() {
        UnknownServiceLevelException failure =
                assertThrows(UnknownServiceLevelException.class, () -> ServiceLevel.fromCode(null));

        assertEquals("A service level code is required", failure.getMessage());
    }
}
