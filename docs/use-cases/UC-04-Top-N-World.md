# UC-04 – View Top N Countries in the World

## Characteristic Information

| Field | Details |
|---|---|
| **Use Case ID** | UC-04 |
| **Use Case Name** | View Top N Countries in the World |
| **Related GitHub Issue** | #7 |
| **Goal in Context** | As an Report Analyst, I want to retrieve the N most populated countries worldwide so that I can focus on the largest populations. |
| **Scope** | Group 11 Population Reporting System |
| **Level** | Primary task |
| **Primary Actor** | Report Analyst |
| **Trigger** | The user launches the Java application; `App.main()` connects to the World database and starts the automatic country reports. |
| **Preconditions** | The application can run and connect to a populated World MySQL database containing the `country` and `city` tables. |
| **Success End Condition** | Up to N countries worldwide are displayed in descending population order. |
| **Failed End Condition** | The report cannot be generated because the database connection or SQL operation fails, or required parameters are invalid. |

> **Current implementation:** The reviewed program automatically runs the reports through `AutoDisplay`. It does **not** show an interactive report-selection menu. The main scenario below describes existing console behaviour rather than a proposed interface.

## Main Success Scenario

1. The user launches the application; `App.main()` establishes the database connection.
2. Before the report runs, `AutoDisplay.worldTopN()` chooses N from the `topN.world` JVM property, then `TOP_N_WORLD` environment variable, or the default **3**.
3. `AutoDisplay` calls `TopNCountriesInWorldReport.display(connection, N)` for Issue #7.
4. The shared `CountryReport.displayTopNCountriesInWorld(...)` method checks that N is greater than zero.
5. The SQL query sorts world countries by population descending (country code ascending for ties) and applies `LIMIT ?`.
6. The system prints the resulting Top N country rows in a formatted console table.

## Extensions

- **E1 – N is not a whole number:** `AutoDisplay.worldTopN()` throws `IllegalArgumentException`.
- **E2 – N is zero or negative:** `AutoDisplay.worldTopN()` or the shared report method throws `IllegalArgumentException`.
- **E3 – Fewer than N rows exist:** Only the available rows are displayed.
- **E4 – Empty result:** `No results found.` appears in the console.
- **E5 – Database/SQL error:** `App` prints the database error.

The reviewed `App` does not catch `IllegalArgumentException`; it does not ask the user to re-enter N.

## Sub-Variations

- **Default:** N = **3**.
- **JVM property:** e.g. `-DtopN.world=5` (highest precedence).
- **Environment variable:** `TOP_N_WORLD=5` (used if the JVM property is absent or blank).
- **No live input prompt:** N is configured before running the application.

## Expected Report Output

Each row contains **Code, Name, Continent, Region, Population, Capital**, with the highest population first. Country code is used to break ties in population. Results are shown as an aligned console table. Missing capital names display `N/A`.

## Schedule and Traceability

- **Sprint:** Sprint 1 – Country Reports (assigned to Sprint 1 in Group 11's GitHub Project).
- **GitHub issue:** #7 (shown as Done on the project board; this document does not replace code/test evidence).
- **Release:** Confirm the release name against the group's GitHub Releases; do not assume `1.0`.
- **Report class:** `TopNCountriesInWorldReport` (called by `AutoDisplay.java`).
- **Shared query method:** `CountryReport.displayTopNCountriesInWorld(...)`.
- **Output formatter:** `ReportDisplay.printTable(...)`.
- **Evidence:** Add a genuine console-output screenshot for Issue #7 under `docs/screenshots/` and link it in the project `README.md`.

*Documentation basis: `App.java`, `AutoDisplay.java`, `CountryReport.java` and `ReportDisplay.java` supplied for review. The six individual report-wrapper class bodies were not supplied; their invocation is visible in `AutoDisplay.java`.*
