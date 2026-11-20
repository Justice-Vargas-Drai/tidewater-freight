# ANALYSIS.md — Tidewater Freight shipping-cost service

This analysis was written **before any code was changed**. It describes the starter
exactly as downloaded. Line numbers refer to `ShippingCalculator.java` unless stated.

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