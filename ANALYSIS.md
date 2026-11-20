# ANALYSIS.md — Tidewater Freight shipping-cost service

This analysis was written **before any code was changed**. It describes the starter
exactly as downloaded. Line numbers refer to `ShippingCalculator.java` unless stated.
The section sign (§) always refers to the AmaliTech Coding Standards: Java Edition.

## 1. Baseline (measured on the untouched starter)

| Measure | Result |
|---|---|
| `mvn test` | 3 tests run, 0 failures, BUILD SUCCESS |
| Demo: DOMESTIC 10 kg STANDARD | 50.0 |
| Demo: INTERNATIONAL 40 kg EXPRESS | 967.5 |
| Demo: unknown zone MOON, 10 kg | **0.0** (a free shipment, with no error) |
| JaCoCo, `ShippingCalculator` | 71% instructions, **50% branches** (5 of 10 missed), 6 of 17 lines missed |
| JaCoCo, `InMemoryRateRepository` | 86% instructions, 50% branches (the unknown-zone `throw` is never run) |
| PIT, whole project | 18 mutants generated, 3 killed: **17% mutation score**, **27% test strength** (3 of the 11 mutants the tests actually reached) |
| PIT, `ShippingCalculator` | 13 mutants: 1 killed, 8 survived, 4 never reached by any test |

The code compiles and every test is green, but the suite killed only 3 of 18 mutants.
Mutants are small artificial changes, not a sample of real bugs, so I read this as evidence
that the assertions are weak, not as a bug-detection rate. The project-wide figures from both
tools include `Demo.java`, which no test runs, so the calculator's own figures are listed
separately above.

## 2. Problems in the code (ranked, most serious first)

### C1. A failure is swallowed and turned into a price of GHS 0
- **Where:** `catch (Exception e) { return 0; }`, lines 34–36.
- **What goes wrong:** An unknown zone makes the repository throw, the calculator catches it,
  and the shipment is priced at 0. The same catch also hides a null zone, a null service
  level and any future bug. The Demo confirms it: `MOON -> 0.0`. The repository's
  `IllegalArgumentException` carries a clear message ("Unknown zone: MOON"), but the
  calculator throws that information away, and a caller cannot tell a failure from a price.
- **Why it matters for Tidewater:** Wrong money leaves the system silently. A shipment could
  be sent for free and nobody would be told. There is no log, no error and no test.
- **Standard:** §6.1 (do not ignore caught exceptions: the program "continues as if the
  operation had succeeded").
- **Evidence:** JaCoCo shows lines 34 and 36 never run. No test has ever triggered this path.

### C2. Zone rules are a hard-coded `if/else` chain, split across two classes (open/closed)
- **Where:** lines 14–19, plus the rates inside `InMemoryRateRepository`.
- **What goes wrong:** Adding a zone means editing `calculateCost`. Worse, the rate lives in the
  repository but the zone fee lives in the calculator. If a new zone is added to the
  repository only, no `if` branch matches, so it is silently charged **no fee** and no error
  is raised.
- **Why it matters for Tidewater:** The brief says a new zone is coming. This design makes that
  an edit to working, tested pricing code, with a silent undercharge as the failure mode.
- **Standard:** §12.2 (OCP: new behaviour should be a new class, not an edit to existing code).
  The deeper issue is that one zone's pricing is split across two classes, so adding a zone
  correctly means remembering to change both.

### C3. The rate source is hard-wired, so the class cannot be tested properly
- **Where:** line 6, `new InMemoryRateRepository()`.
- **What goes wrong:** The `RateRepository` interface exists, but nothing can inject another
  implementation. The calculator can still be tested with its real repository, including the
  unknown-zone path, but a test cannot substitute a repository with controlled rates or a
  simulated failure, such as a rate source that is unavailable.
- **Why it matters for Tidewater:** The calculator's rules cannot be tested apart from today's
  hard-coded rates, and a future rate source would mean editing the calculator.
- **Standard:** §12.5 (DIP). The standard's own "bad" example has exactly this shape.

### C4. No validation policy for inputs
- **Where:** the method signature and line 28.
- **What goes wrong:** A negative weight is accepted and can produce a negative or misleading
  price: DOMESTIC at −1 kg costs −5.0, while REGIONAL at −1 kg costs 2.0 because the fee offsets
  the negative base. `NaN` and infinite weights are accepted too. A null zone makes the repository
  throw `IllegalArgumentException`, and a null service level causes a `NullPointerException` at
  line 28. C1 turns both into 0. Other cases are undefined rather than clearly wrong: the code says nothing
  about a zero weight, and any service level that is not exactly `"EXPRESS"` (including
  `"express"` or a typo) is silently priced as standard.
- **Why it matters for Tidewater:** Bad input produces a number instead of an error, and the
  undefined cases mean nobody has decided what the service should do. In the refactor I will
  state a policy for each case and record it in the Decisions section.

### C5. One method does everything, including printing
- **Where:** `calculateCost`, line 32.
- **What goes wrong:** One method looks up the rate, applies the zone fee, the heavy surcharge and the
  express multiplier, and writes to `System.out`. Printing is a side effect of a method whose
  job is to calculate.
- **Why it matters for Tidewater:** Any rule change touches the same method as the others (the
  brief hints that tweaking one rule can break another). The console output is an unwanted side
  effect of a calculation, with no seam to switch it off or check it.
- **Standard:** §12.1 (SRP), §4.2 (one thing), §4.4 (no side effects).
- **Evidence:** PIT removed the `println` and every test still passed (line 32, SURVIVED).

### C6. Smaller issues
- **Magic numbers and strings** (10, 25, 20, 30, 1.5, and the zone and level names) with no names: §3.4.
- **An empty branch** (`// no extra fee`) and comments that restate the code: §10.1.
- **`double` for money,** which can round unpredictably. This is a design consideration, not an
  observed pricing bug. I will record my choice in the Decisions section.

## 3. Problems in the tests (ranked)

### T1. `calculatesExpressCost` mocks the class under test
- **Claims to check:** the express price for an INTERNATIONAL 40 kg shipment (967.5).
- **Actually checks:** that a Mockito mock returns the value it was told to return. The real
  calculator is never run.
- **Would let through:** any bug in the express, international or heavy-weight rules, or even
  the deletion of the whole method body.
- **Evidence:** PIT's "tests examined" for `ShippingCalculator` lists only the other two tests,
  because this one never executes the class. It is the most misleading test because the number
  967.5 looks like a verified answer for the most complex path.
- **Standard:** §9 (an assertion that would fail if the behaviour were wrong).

### T2. `returnsACostForAShipment` uses `assertNotNull` on a primitive
- **Claims to check:** that a REGIONAL shipment gets a cost.
- **Actually checks:** nothing. `cost` is a primitive `double`, which is boxed to a non-null
  `Double`, so the assertion can never fail.
- **Would let through:** any wrong value at all. It could only fail if an exception escaped,
  and C1's catch-all makes sure none ever does.

### T3. `calculatesACost` only asserts `cost > 0`
- **Claims to check:** a DOMESTIC 10 kg cost.
- **Actually checks:** that the result is positive. It is the only test in the calculator that
  kills a mutant (line 33, return replaced by 0.0). The suite killed none of the other 12
  calculator mutants: 8 survived and 4 were never reached.
- **Would let through:** a wrong rate, a wrong fee or a broken boundary. For example, PIT
  negated the DOMESTIC check on line 14, which makes a REGIONAL shipment skip its handling fee
  (40.0 instead of 50.0). Neither real-code test noticed, because 40.0 is still positive and
  not null.
- **Related fact:** the DOMESTIC 10 kg and REGIONAL 5 kg cases both happen to cost 50.0, so the
  existing tests could not even tell the two zones apart.

### T4. What the suite never does
- It never runs INTERNATIONAL, the heavy-weight surcharge or EXPRESS with the real code
  (lines 18, 19, 24, 29: NO_COVERAGE).
- It never tests the 30 kg boundary: both `>` mutants on line 23 survived.
- It never tests a failure: no unknown zone, no invalid weight, no null.
- Together these explain the 17% mutation score and the 50% branch coverage.

## 4. The most serious problem, and why

**C1, the swallowed failure.** The other problems make the code hard to change or hard to test.
This one makes it return **wrong money without anyone knowing**: an unknown zone is priced at
GHS 0 with no error, no log and no test. It is also the problem the tests hide best, because
no test runs that path (T4), so the green suite gives false confidence about exactly
the failure that matters. On the test side the most serious problem is T1, because it makes
the most complex rule look verified when nothing is checked.

## 5. What I will do about it (the Decisions section will be added at the end)

- Remove the blanket catch. Unknown zones and invalid input will raise specific custom
  exceptions with context, and nothing will return 0 for a failure.
- Give each zone's rules one home behind an abstraction, so a new zone is added without editing
  existing logic.
- Inject the rate source through the constructor.
- Move printing out of the calculation. `Demo` will do the printing.
- Replace all three tests with tests that assert exact prices and fail when a rule is broken.
  This includes the 30 kg boundary, each zone, standard and express, and the failure cases.
- Keep the behaviour for valid input unchanged: 50.0 for DOMESTIC 10 kg STANDARD and 967.5 for
  INTERNATIONAL 40 kg EXPRESS.


---

## 6. Decisions (added after the refactor)

Sections 1–5 above are unchanged from the commit made before any code changed.

### 6.1 Results

| Measure | Before | After |
|---|---|---|
| Tests | 3 (two prove nothing) | 51 test cases from 30 test methods (some parameterized), each asserting a real value or a specific failure |
| JaCoCo branch coverage | 50% (`ShippingCalculator`) | 100% in every class except `Demo` |
| PIT mutants killed (excluding `Demo`) | 3 of 15 | 18 of 18 |
| PIT test strength | 27% | 100% |
| Unknown zone `MOON` | priced at GHS 0, no error | `UnknownZoneException`: "No pricing rules for shipping zone: MOON" |
| Prices for valid shipments | — | unchanged: the 8 prices pinned against the original code still pass |

### 6.2 How I refactored safely

Before changing anything, I wrote `ShippingPriceCharacterizationTest`, which pins 8 exact prices
produced by the original calculator: one per zone, both service levels, and 30 kg, 30.5 kg and
31 kg around the surcharge boundary. I then changed one thing per commit and kept those 8 rows
green throughout. The rows were never edited; only the way the calculator is called changed.
That is the evidence that the refactor changed the structure and not the prices.

### 6.3 Key decisions

**D1. Failures travel to a level that can act on them (fixes C1).** I removed the
`catch (Exception e) { return 0; }`. The calculator has no catch at all, because it cannot do
anything useful with an unknown zone; the exception reaches the caller (§6.1). `Demo` is the
only place that catches, because it is the edge of the program and can report the failure to a
person. It catches only `ShippingCostException`, so a real bug still fails loudly instead of
being turned into a quote.

**D2. A small custom exception hierarchy.** `ShippingCostException` is the parent of
`UnknownZoneException`, `UnknownServiceLevelException` and `InvalidShipmentException`. A caller
that only needs "no valid quote" catches the parent; a caller that cares why catches a subtype
(for example, to answer "we don't ship there"). They are unchecked, so callers are not forced to
write catch blocks they cannot use, which is how empty catches like the original one get written.
`ServiceLevel.fromCode` is the one place that translates an exception: it turns Java's
`IllegalArgumentException` into `UnknownServiceLevelException` and keeps the original as the
cause (Coding Standards §6.3).

**D3. Customer errors and configuration errors are different signals.** A bad shipment (negative
weight, unknown zone) throws one of our exceptions, because it describes the customer's request.
A bad zone configuration (rate of 0, NaN fee, a zone defined twice) throws Java's
`IllegalArgumentException`, because it is a programming mistake in Tidewater's setup, and calling
it an invalid shipment would send whoever reads the error looking in the wrong place.

**D4. One home for each zone's pricing (fixes C2).** `ZonePricing` is an interface;
`FlatRateZonePricing` holds a zone's rate **and** fee together. The repository stores one
`ZonePricing` per zone and refuses a zone defined twice instead of silently overwriting it. The
calculator's zone `if/else` chain is gone. Zones are identified by name, not by an enum, so a new
zone can be added as data without editing any existing type.

**D5. The rate source is injected (fixes C3).** `ShippingCalculator` receives a `RateRepository`
through its constructor (§12.5). Production code passes `InMemoryRateRepository.withStandardZones()`;
unit tests pass a Mockito stub with simple made-up rates, including one that throws, which the
original design made impossible.

**D6. Invalid input cannot reach the calculator (fixes C4).** `Shipment` is a record that refuses
to be created with a missing zone, a missing service level, or a weight that is not a finite
number above zero. The calculator therefore never re-checks its input. Policies I chose, because
the brief does not state them:
- **Zero weight is refused.** A 0 kg shipment would still be charged the zone fee (GHS 10 for
  REGIONAL), which is not a meaningful price.
- **Only exact service-level codes are accepted.** `"express"` or a typo is refused instead of
  being priced as standard. Guessing the customer's intent with money involved is riskier than
  asking them to correct the code; a more forgiving parser could be added at the input boundary
  without touching the pricing.

**D7. One job per class (fixes C5).** The calculator adds the zone's base cost and the
heavy-item surcharge, then applies the service level. Printing moved to `Demo` (§4.4). The
heavy-item threshold and surcharge are named constants (§3.4), and each service level carries its
own multiplier.

**D8. Behaviour I kept deliberately.** Express multiplies the whole subtotal, including the zone
fee and heavy surcharge, and exactly 30 kg is not "heavy". Both are how the original charged, so
I kept them and pinned them with tests. Whether express should apply to fees is a question for
the business, not something a refactor should change silently.

**D9. I kept `double` for money.** The original service used `double`, and the 8 characterization
prices are preserved exactly. `double` is not exact for every input, though: DOMESTIC at 0.07 kg
costs 0.35000000000000003 instead of 0.35. Switching to `BigDecimal` would change every signature
and needs a rounding rule the brief does not give (to the pesewa? rounded up or to nearest?). I
record this as a known risk, noted in the code at `ShippingCalculator.calculateCost`: before these
prices are shown to customers or invoiced, money should move to `BigDecimal` with an agreed
rounding rule.

### 6.4 How the design absorbs the new zone

The brief says a new zone with its own rate rules is coming. There are two cases, and neither
edits the calculator:

- **It has a different rate and fee:** add one line to `InMemoryRateRepository.withStandardZones()`,
  for example `new FlatRateZonePricing("COASTAL", 6.0, 4.0)`.
- **It has different rules** (for example, tiered pricing by weight): write one new class that
  implements `ZonePricing` and register it the same way.

In the original code, the same change meant adding a rate in `InMemoryRateRepository` **and**
a new branch in `calculateCost`, editing tested pricing logic. If the second edit was forgotten,
no branch matched, the zone was silently charged no fee, and no test would have noticed.
`InMemoryRateRepositoryTest.anAddedZoneIsFoundWithoutChangingTheOthers` shows a new zone being
added as data alone.

### 6.5 What I kept, fixed and rewrote from the AI's code and tests

| AI-generated part | Decision | Why |
|---|---|---|
| `RateRepository` interface | **Kept, fixed** | The abstraction was right but unused. I changed it to return a zone's whole pricing and made it injectable. |
| `InMemoryRateRepository` | **Fixed** | It now holds rate and fee together, takes its zones from outside, throws `UnknownZoneException`, and refuses duplicates. |
| `ShippingCalculator` | **Rewrote** | It did five jobs and hid failures. Only the pricing order survived, and it is pinned by tests. |
| `Demo` | **Rewrote** | It now builds and injects the dependencies, and it is the only place that prints or catches. |
| `calculatesACost` | **Rewrote** | Same idea (price a domestic shipment) but it now asserts the exact price. `cost > 0` would pass a wrong fee, rate or boundary. |
| `calculatesExpressCost` | **Thrown away** | It mocked the class under test, so it never ran the calculator. Replaced by unit tests that mock the **dependency** (`RateRepository`) and assert the real calculator's result, including express. |
| `returnsACostForAShipment` | **Thrown away** | `assertNotNull` on a primitive `double` can never fail. Replaced by exact-price rows in the characterization test. |

### 6.6 My improvement beyond the brief: a quality gate

`mvn verify` now fails the build if line or branch coverage drops below 100% (the target in §9)
or if any PIT mutant survives. `Demo` is excluded from both because it is a console entry point
with no pricing logic.

Coverage alone cannot detect a test that runs code without checking it, which is exactly what was
wrong with the original suite. I checked this. I removed the assertion from the 30 kg unit test
and the 30 kg row from the characterization test, so nothing guarded the `> 30` boundary. The
coverage gate still passed, but the build failed with "Mutation score of 94 is below threshold of
100", because the `>` to `>=` mutant survived. With both restored, all 16 mutants that existed at
the time were killed and the build passed. After the hygiene fixes in 6.8, the suite kills 18 of 18.

The gate has limits. It only requires the suite to kill the mutants PIT generates for the
included classes, so it cannot prove that every rule is protected: a hollow test that duplicates
a good one is not detected, and PIT does not mutate the record constructors where the validation
lives (see 6.7). What it does is make the build fail when a change leaves one of those generated
mutants alive, as the 94% run above showed, so this kind of weakness cannot be merged unnoticed.

### 6.7 Known limits of the tools

- PIT's record filter skipped the constructors of the records (`Shipment` and most of
  `FlatRateZonePricing`), which is where their validation lives. Those rules are covered instead
  by JaCoCo (100% of branches) and by boundary tests written to fail if a comparison changes
  (0 kg, rate 0, fee 0, NaN and infinity).
- `Demo` is excluded from both gates, as explained above.

### 6.8 Changes after the Professional Hygiene check

I used Evalu8's hygiene check before submitting and acted on it as follows.

**Fixed:**
- The heavy-item constants are now `private`.
- A missing service-level code has its own message ("A service level code is required"). An
  unknown code now lists the valid codes, which is more useful to a caller than Java's
  "No enum constant ..." message; the original exception is still kept as the cause.
- `InMemoryRateRepository.findPricing` rejects a null or blank zone explicitly, instead of
  relying on `HashMap.get(null)` happening to return nothing.
- Weak assertions were strengthened: the zero-fee test now checks the price, the null-zone test
  checks the message (and covers empty and blank), and the null-guard tests check which argument
  was missing, so deleting a guard would fail a test.
- Removed the unused `mockito-junit-jupiter` dependency.
- The README now documents the Maven version and how to run the quality gate.

**Clarified in code rather than changed:**
- Why `FlatRateZonePricing` throws `IllegalArgumentException` (D3), in its class comment.
- The `double` money risk (D9), at the calculation itself.
- The standard zones are seed data, in `withStandardZones()`. Loading rates from a file would be
  speculative today (YAGNI, §2.2).
- `Demo` notes that a production caller would log the exception rather than only print its message.

**Not changed:** making the 30 kg threshold configurable. It is a fixed business rule today, and
nothing in the brief says it varies.

After these changes: 51 test cases pass, line and branch coverage stay at 100%, and PIT kills
18 of 18 mutants.
