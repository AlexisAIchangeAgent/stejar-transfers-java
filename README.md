# stejar-transfers-java

Training material. Invented bank. Stejar Bank SA, its customers and its figures are invented.

Transfer-fee service of Stejar Bank: `POST /fees` returns the fee of a transfer in MDL,
`GET /health` returns `{"status": "ok"}`. Java 17, Spring Boot 3, Maven.

## Getting started

Windows 11: use PowerShell. macOS: use Terminal. Run every command from the repository folder.

```
git clone https://github.com/AlexisAIchangeAgent/stejar-transfers-java.git
cd stejar-transfers-java
```

Or download the ZIP of the latest release on GitHub (Releases, then Source code (zip)), unzip it,
and open a terminal in the folder. Step 3 then creates the git repository: the training commits
in it.

| Step | Windows | macOS |
|---|---|---|
| 1. Check the tools (changes nothing) | `powershell -ExecutionPolicy Bypass -File .\check.ps1` | `bash check.sh` |
| 2. Only if a line says KO: install the missing tools, open a new terminal, run step 1 again | `powershell -ExecutionPolicy Bypass -File .\install-tools.ps1` | `bash install-tools.sh` |
| 3. Install the dependencies of the project (network, a few minutes) | `powershell -ExecutionPolicy Bypass -File .\setup.ps1` | `bash setup.sh` |
| 4. Start Claude Code | `claude` | `claude` |

Step 3 ends with "Ready for cycle 1". If your IT team installs the tools for you, give them
`INSTALL-TOOLS.md`.

## Before the training

Run these once, with network access. During the training, Claude Code works without network.

`setup.ps1` (Windows) and `setup.sh` (macOS) run these steps for you, then the unit tests: see
Getting started above. The commands below do the same by hand.

```
mvn test
mvn exec:java@playwright-install
python3 -m venv privacy-agent/.venv
privacy-agent/.venv/bin/python -m pip install -r privacy-agent/requirements.txt
```

On Windows, the last line is `privacy-agent\.venv\Scripts\python -m pip install -r privacy-agent\requirements.txt`.
If `python3` is not found on Windows, use `python` (or `py -3`) in the first line instead.

## Requirements

- JDK 17 or newer (the code is compiled for Java 17).
- Maven 3.9 or newer on the `PATH`, or the Maven bundled with IntelliJ IDEA, or the Maven
  wrapper of this repository: `./mvnw` (macOS, Linux) or `.\mvnw.cmd` (Windows, PowerShell
  or cmd). Everywhere below, `mvn` can be replaced by the wrapper.

In IntelliJ IDEA: File, Open, select `pom.xml`, open as project.

## Build

```
mvn package
```

## Run

```
mvn spring-boot:run
```

The service keeps running in this terminal (Ctrl+C stops it). Open a second terminal, then,
from macOS or Linux:

```
curl -X POST localhost:8080/fees -H "Content-Type: application/json" \
  -d '{"amount": 2000.00, "currency": "MDL", "type": "standard", "customer_segment": "retail"}'
```

From Windows PowerShell:

```
Invoke-RestMethod -Method Post -Uri http://localhost:8080/fees -ContentType "application/json" `
  -Body '{"amount": 2000.00, "currency": "MDL", "type": "standard", "customer_segment": "retail"}'
```

Without `RATES_BASE_URL` in the environment, the service uses a fixed EUR to MDL rate
(19.8765) and makes no network call.

## Test

```
mvn test          # unit tests (JUnit 5)
mvn test -Pbdd    # BDD features of features/ (Cucumber), instant fee steps not written yet
```

While the instant fee steps are not written, `mvn test -Pbdd` ends with BUILD FAILURE:
Cucumber reports 9 undefined scenarios. This is expected.

Playwright browser, needed for the Playwright tests of the training (downloads Chromium once):

```
mvn exec:java@playwright-install
```

This command prints no `BUILD SUCCESS` line (the Playwright installer ends the JVM itself).
Exit code 0 means the browser is installed.

## Folder map

| Folder | Content |
|---|---|
| `src/main/java/md/stejar/transfers/` | the service: `api/`, `domain/`, `clients/` |
| `src/test/java/md/stejar/transfers/` | JUnit tests, `bdd/` Cucumber runner |
| `features/` | Gherkin features (read by the Cucumber runner) |
| `crm/` | static page of the internal CRM |
| `incidents/` | incident tickets |
| `docs/` | service documentation, published tariff, `adr/` |
| `privacy-agent/` | GDPR intake policy and synthetic requests (Python) |
| `docs/pipeline/` | CI pipeline, not active yet: cycle 3 copies it to `.github/workflows/` |

See `CLAUDE.md` for the conventions.
