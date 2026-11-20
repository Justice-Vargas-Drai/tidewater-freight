# Tidewater Freight — Shipping Cost Service

Works out the cost of a shipment from its destination zone, weight and service level.
This repository is the refactored version of an AI-generated starter, completed for the
AmaliTech **Code Quality & Testing** lab.

**Start with [ANALYSIS.md](ANALYSIS.md):** what was wrong with the original code and tests
(written before any code changed), and the Decisions section explaining the refactor.

## Requirements

- JDK 21
- Maven 3.9 or later (verified with Maven 3.9.16 and Java 21.0.13)

## Running it

**See some sample quotes, including an unknown zone being refused:**

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.tidewater.freight.Demo
```

**Run the tests:**

```bash
mvn test
```

**Run the tests and the quality gate:**

```bash
mvn verify
```

`mvn verify` fails the build if line or branch coverage drops below 100%, or if any PIT
mutant survives. `Demo` is excluded from both checks. Reports:

- Coverage: `target/site/jacoco/index.html`
- Mutation testing: `target/pit-reports/index.html`

## How it is organised

| Class | Job |
|---|---|
| `ShippingCalculator` | Zone base cost, plus the heavy-item surcharge above 30 kg, with the service level applied last |
| `Shipment` | One shipment; refuses invalid weights and missing details |
| `ServiceLevel` | STANDARD and EXPRESS, each with its price multiplier |
| `ZonePricing`, `FlatRateZonePricing` | Everything specific to one zone: rate per kg and zone fee |
| `RateRepository`, `InMemoryRateRepository` | Supplies each zone's pricing; injected into the calculator |
| `ShippingCostException` and subtypes | Why a shipment cannot be priced |
| `Demo` | Prints sample quotes; the only class that writes to the console |

**Adding a zone:** add a `FlatRateZonePricing` to `InMemoryRateRepository.withStandardZones()`,
or, if the zone has different rules, write a new `ZonePricing` implementation. The calculator
does not change.
