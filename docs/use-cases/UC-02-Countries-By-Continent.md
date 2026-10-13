# UC-02 – View Countries in a Continent by Population

## Characteristic Information

| Field | Details |
|---|---|
| **Use Case ID** | UC-02 |
| **Use Case Name** | View Countries in a Continent by Population |
| **Related GitHub Issue** | #4 |
| **Goal in Context** | As an Report Analyst, I want to see countries in a specified continent ordered by population so that I can compare countries within that continent. |
| **Scope** | Group 11 Population Reporting System |
| **Level** | Primary task |
| **Primary Actor** | Report Analyst |
| **Trigger** | The user launches the Java application; `App.main()` connects to the World database and starts the automatic country reports. |
| **Preconditions** | The application can run and connect to a populated World MySQL database containing the `country` and `city` tables. |
| **Success End Condition** | Countries in the specified continent are displayed from largest population to smallest. |
| **Failed End Condition** | The report cannot be generated because the database connection or SQL operation fails, or required parameters are invalid. |

> **Current implementation:** The reviewed program automatically runs the reports through `AutoDisplay`. It does **not** show an interactive report-selection menu. The main scenario below describes existing console behaviour rather than a proposed interface.

## Main Success Scenario

1. The user launches the Java application and it connects to the World database.
2. `AutoDisplay` calls `CountriesByContinentReport.display(connection, "Asia")` for Issue #4.
3. The shared `CountryReport.displayCountriesInContinent(...)` method checks that the continent value is not blank, then trims whitespace.
4. A parameterised SQL query selects countries where `c.Continent = ?` and looks up each country's capital city.
5. The query orders matching countries by population descending and then country code ascending.
6. `ReportDisplay.printTable(...)` displays the report in the console.

## Extensions

- **E1 – Blank or null continent:** The shared report method throws `IllegalArgumentException` with `Continent must not be blank`.
- **E2 – No matching countries:** The console displays `No results found.` beneath the headers.
- **E3 – Connection or SQL failure:** `App` reports the failure and does not produce the intended report.
- **E4 – Missing capital:** The capital field is shown as `N/A`.

`App` does not currently catch `IllegalArgumentException` or offer a re-entry prompt.

## Sub-Variations

- The automatic demonstration uses **Asia** (`DEFAULT_CONTINENT`).
- The underlying report method can take a different **non-blank** continent string when called by other application code.
- The reviewed application does not prompt the user to choose a continent at runtime.

## Expected Report Output

Each row contains **Code, Name, Continent, Region, Population, Capital**, with the highest population first. Country code is used to break ties in population. Results are shown as an aligned console table. Missing capital names display `N/A`.

## Schedule and Traceability

- **Sprint:** Sprint 1 – Country Reports (assigned to Sprint 1 in Group 11's GitHub Project).
- **GitHub issue:** #4 (shown as Done on the project board; this document does not replace code/test evidence).
- **Release:** Confirm the release name against the group's GitHub Releases; do not assume `1.0`.
- **Report class:** `CountriesByContinentReport` (called by `AutoDisplay.java`).
- **Shared query method:** `CountryReport.displayCountriesInContinent(...)`.
- **Output formatter:** `ReportDisplay.printTable(...)`.
- **Evidence:** Add a genuine console-output screenshot for Issue #4 under `docs/screenshots/` and link it in the project `README.md`.

*Documentation basis: `App.java`, `AutoDisplay.java`, `CountryReport.java` and `ReportDisplay.java` supplied for review. The six individual report-wrapper class bodies were not supplied; their invocation is visible in `AutoDisplay.java`.*
