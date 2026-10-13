# UC-03 – View Countries in a Region by Population

## Characteristic Information

| Field | Details |
|---|---|
| **Use Case ID** | UC-03 |
| **Use Case Name** | View Countries in a Region by Population |
| **Related GitHub Issue** | #6 |
| **Goal in Context** | As an Report Analyst, I want to list countries in a selected region by population so that I can compare their relative population sizes. |
| **Scope** | Group 11 Population Reporting System |
| **Level** | Primary task |
| **Primary Actor** | Report Analyst |
| **Trigger** | The user launches the Java application; `App.main()` connects to the World database and starts the automatic country reports. |
| **Preconditions** | The application can run and connect to a populated World MySQL database containing the `country` and `city` tables. |
| **Success End Condition** | Countries belonging to the specified region are displayed in descending population order. |
| **Failed End Condition** | The report cannot be generated because the database connection or SQL operation fails, or required parameters are invalid. |

> **Current implementation:** The reviewed program automatically runs the reports through `AutoDisplay`. It does **not** show an interactive report-selection menu. The main scenario below describes existing console behaviour rather than a proposed interface.

## Main Success Scenario

1. The user launches the Java application; `App.main()` connects to the World database.
2. `AutoDisplay` calls `CountriesByRegionReport.display(connection, "Eastern Asia")` for Issue #6.
3. The shared `CountryReport.displayCountriesInRegion(...)` method validates the region and trims whitespace.
4. The parameterised SQL query selects rows matching `c.Region = ?`, including capital names from the `city` table.
5. Countries are sorted by `c.Population DESC, c.Code ASC`.
6. The formatter prints the region report as an aligned console table.

## Extensions

- **E1 – Blank or null region:** `IllegalArgumentException` is thrown with `Region must not be blank`.
- **E2 – No countries in the region:** The output shows `No results found.`.
- **E3 – Database/SQL failure:** `App` prints the database error and the report is not completed.
- **E4 – Missing capital name:** The output contains `N/A`.

There is no interactive retry step for invalid input in the reviewed code.

## Sub-Variations

- The automatic run uses **Eastern Asia** (`DEFAULT_REGION`).
- The underlying method accepts another non-blank region value supplied by calling code.
- The current console application does not provide a runtime region-selection menu.

## Expected Report Output

Each row contains **Code, Name, Continent, Region, Population, Capital**, with the highest population first. Country code is used to break ties in population. Results are shown as an aligned console table. Missing capital names display `N/A`.

## Schedule and Traceability

- **Sprint:** Sprint 1 – Country Reports (assigned to Sprint 1 in Group 11's GitHub Project).
- **GitHub issue:** #6 (shown as Done on the project board; this document does not replace code/test evidence).
- **Release:** Confirm the release name against the group's GitHub Releases; do not assume `1.0`.
- **Report class:** `CountriesByRegionReport` (called by `AutoDisplay.java`).
- **Shared query method:** `CountryReport.displayCountriesInRegion(...)`.
- **Output formatter:** `ReportDisplay.printTable(...)`.
- **Evidence:** Add a genuine console-output screenshot for Issue #6 under `docs/screenshots/` and link it in the project `README.md`.

*Documentation basis: `App.java`, `AutoDisplay.java`, `CountryReport.java` and `ReportDisplay.java` supplied for review. The six individual report-wrapper class bodies were not supplied; their invocation is visible in `AutoDisplay.java`.*
