# CLAUDE.md: stejar-transfers-java

Training material: invented bank, invented data. Stejar Bank SA does not exist.

## Purpose

HTTP service of Stejar Bank that computes the fee of a customer transfer before the customer
confirms it. `POST /fees` takes an amount, a currency (MDL or EUR), a transfer type (standard
or instant) and a customer segment (retail or premium), and returns the fee in MDL with the
rule applied. EUR amounts are converted with a rate from a third-party exchange-rate API.
The published tariff is `docs/fee-rules.md`.

## Stack

- Java 17 (`maven.compiler.release` 17 in `pom.xml`, CI runs JDK 17), Maven 3.9+. Maven
  wrapper included (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`), or use an installed `mvn`
  (IntelliJ IDEA also has a bundled Maven).
- Spring Boot 3.5 (parent `spring-boot-starter-parent`): `spring-boot-starter-web`,
  `spring-boot-starter-validation`. Jackson writes `BigDecimal` as a JSON number.
- Tests: JUnit 5, AssertJ and MockMvc (`spring-boot-starter-test`); Cucumber-JVM 7 on the
  JUnit Platform suite, with `cucumber-spring`. Declared but not used yet: Pact JVM consumer
  (`au.com.dius.pact.consumer:junit5`) and Playwright for Java.

## Folder map

```
src/main/java/md/stejar/transfers/
  StejarTransfersApplication.java   Spring Boot entry point
  AppConfiguration.java             beans: ExchangeRateClient, FeeCalculator
  RatesProperties.java              rates.* settings (src/main/resources/application.properties)
  api/        FeeController (GET /health, POST /fees), bodies, ApiExceptionHandler
  domain/     FeeCalculator, FeeRequest, Fee, enums, UnsupportedTransferException
  clients/    ExchangeRateClient, HttpExchangeRateClient, FixedRateClient
src/test/java/md/stejar/transfers/   unit tests (same packages) and bdd/ (Cucumber)
features/              Gherkin features: instant-fee.feature (the only copy)
crm/customer-lookup.html   static page of the internal CRM (for Playwright)
incidents/             incident tickets
docs/                  README-service.md, fee-rules.md, adr/ (empty)
privacy-agent/         GDPR intake agent material (Python): policy.md, requests/, output/
docs/pipeline/ci.yml       pipeline, not active yet (cycle 3 copies it to .github/workflows/): unit tests on push and PR, manual fake deploy
mvnw, mvnw.cmd, .mvn/wrapper/   Maven wrapper (Maven 3.9.11)
```

## Commands

Run from the repository root. Same commands on macOS, Linux and Windows (PowerShell or cmd).
To use the wrapper instead of an installed Maven, replace `mvn` with `./mvnw` (macOS, Linux)
or `.\mvnw.cmd` (Windows, PowerShell or cmd).

| Task | Command |
|---|---|
| Build (compile, unit tests, jar) | `mvn package` |
| Run on port 8080 | `mvn spring-boot:run` |
| Unit tests | `mvn test` |
| One test class | `mvn test "-Dtest=StandardMdlFeeTest"` |
| BDD tests (Cucumber) | `mvn test -Pbdd` |
| Playwright browser | `mvn exec:java@playwright-install` (downloads Chromium once) |

- `mvn test` runs the JUnit tests only (surefire excludes `RunCucumberTest`). `mvn test -Pbdd`
  runs only `bdd/RunCucumberTest`, which reads the features of `features/`.
  Today every scenario of `instant-fee.feature` has undefined steps: the BDD run is not green
  until the step definitions are written (Cucumber reports 9 undefined scenarios, Maven ends
  with BUILD FAILURE, surefire counts 6 errors).
- `mvn exec:java@playwright-install` prints no `BUILD SUCCESS` line: the Playwright installer
  ends the JVM itself. Exit code 0 means the browser is installed.
- No linter or formatter is configured. `javac` runs with `-Xlint:all` (warnings in the output).
- The service reads `RATES_BASE_URL`, `RATES_API_KEY` and `RATES_TIMEOUT_SECONDS` from the
  environment (`application.properties`). Without `RATES_BASE_URL` it uses `FixedRateClient`
  (rate 19.8765, no network). The `.env` file is not loaded automatically.

## Coding conventions

- Packages `md.stejar.transfers.api`, `.domain`, `.clients`; wiring in `md.stejar.transfers`.
  PascalCase classes, camelCase methods and fields, UPPER_SNAKE_CASE constants, 4-space indent.
- Value objects are records (`FeeRequest`, `Fee`, `ExchangeRate`, the API bodies).
- Money: always `BigDecimal`, never `float` or `double`. Build it from strings
  (`new BigDecimal("0.01")`), compare with `compareTo` or equal scales, and round with
  `setScale(2, RoundingMode.X)` where the rule says so. Rounding rules come from
  `docs/fee-rules.md`. Fees are in MDL.
- JSON field names are snake_case (`customer_segment`), mapped with `@JsonProperty`.
- Constructor injection only. Configuration only from the environment (`RatesProperties`).

## Test conventions

- Unit tests in `src/test/java`, in the package of the class under test, class
  `<Subject>Test`, methods in camelCase that state the expected behaviour, e.g.
  `StandardMdlFeeTest.hasAMinimumOf5`. JUnit 5 with AssertJ assertions.
- Arrange, act, assert, separated by blank lines. Cases with `@ParameterizedTest`.
- Domain tests build `FeeCalculator` directly with `FixedRateClient` (rate 20.0000);
  `FeesEndpointTest` uses `@WebMvcTest` and MockMvc with the same rate.
- No network call in tests: use `FixedRateClient` or a fake implementing `ExchangeRateClient`.
- BDD: features in `features/` at the root (do not copy them under `src/test/resources`),
  step definitions in `src/test/java/md/stejar/transfers/bdd/`.
  `CucumberSpringConfiguration` starts the service with MockMvc and a `FixedRateClient`
  (rate 19.8765).

## Architecture boundaries

- `api -> domain -> clients`. Never the other way.
- `api` maps HTTP bodies to domain objects and back, and validates the bodies.
- `domain` holds the fee rules. It has no HTTP and no Spring: plain Java. It depends on the
  `ExchangeRateClient` interface, not on a concrete client.
- `clients` talks to third parties (`HttpExchangeRateClient` uses `java.net.http`).
  `AppConfiguration` chooses the client: HTTP when `RATES_BASE_URL` is set, else fixed.
- `ApiExceptionHandler` maps errors to HTTP: `UnsupportedTransferException` becomes 422
  `{"error": ...}`, an invalid body becomes 422 `{"error": "invalid request"}`,
  `ExchangeRateException` becomes 502 `{"error": "exchange rate unavailable"}`.

## Not implemented yet

- Instant transfer fee: `POST /fees` with `"type": "instant"` returns HTTP 422
  `{"error": "instant transfers not supported yet"}`. The acceptance criteria are in
  `features/instant-fee.feature`; no step definition exists yet.
- No Pact or Playwright test yet (dependencies declared). No agent code in `privacy-agent/`.

## Do not edit without asking

- `.github/workflows/`, `privacy-agent/requests/`
- `.env` (fake values, committed on purpose for the training)

## Definition of done

- Unit tests green: `mvn test`.
- BDD green for the features in scope: `mvn test -Pbdd`, no undefined step;
  a scenario left red is named in the commit message, with its reason.
- No new warning in the Maven output (compiler or tests).
- New behaviour comes with tests; money stays in `BigDecimal`; layer boundaries respected.
