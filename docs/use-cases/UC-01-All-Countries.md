# UC-01 – View All Countries by Population

## Characteristic Information

| Field | Details                                                                                                                       |
|---|-------------------------------------------------------------------------------------------------------------------------------|
| **Use Case ID** | UC-01                                                                                                                         |
| **Use Case Name** | View All Countries by Population                                                                                              |
| **Related GitHub Issue** | #2                                                                                                                            |
| **Goal in Context** | As an Report Analyst, I want to view all countries ranked by population so that I can compare populations across the world.   |
| **Scope** | Group 11 Population Reporting System                                                                                          |
| **Level** | Primary task                                                                                                                  |
| **Primary Actor** | Report Analyst                                                                                                                |
| **Trigger** | The user launches the Java application; `App.main()` connects to the World database and starts the automatic country reports. |
| **Preconditions** | The application can run and connect to a populated World MySQL database containing the `country` and `city` tables.           |
| **Success End Condition** | All countries returned by the World database are displayed in descending population order.                                    |
| **Failed End Condition** | The report cannot be generated because the database connection or SQL operation fails, or required parameters are invalid.    |

> **Current implementation:** The reviewed program automatically runs the reports through `AutoDisplay`. It does **not** show an interactive report-selection menu. The main scenario below describes existing console behaviour rather than a proposed interface.

## Main Success Scenario

1. The user launches the application.
2. `App.main()` establishes a database connection and invokes `AutoDisplay.displayAllReports(connection)`.
3. `AutoDisplay` calls `AllCountriesReport.display(connection)` for Issue #2.
4. The corresponding shared `CountryReport.displayAllCountries(...)` logic retrieves countries from the `country` table and joins the `city` table for capital names.
5. The SQL query sorts by `c.Population DESC, c.Code ASC` without a continent, region or Top N limit.
6. `ReportDisplay.printTable(...)` prints the country data as a console table.

## Extensions

- **E1 – Database connection is unavailable:** `App` prints `Database connection failed.` and stops if the connection is `null`.
- **E2 – SQL error:** `App` catches the `SQLException` and prints `Database error:` followed by the error message.
- **E3 – Empty result:** `ReportDisplay` prints the headers followed by `No results found.`.
- **E4 – Unknown capital:** The capital column shows `N/A`.

## Sub-Variations

- This report has **no geographic filter** and no Top N limit.
- The results may contain countries from any continent and region present in the World database.

## Expected Report Output

Each row contains **Code, Name, Continent, Region, Population, Capital**, with the highest population first. Country code is used to break ties in population. Results are shown as an aligned console table. Missing capital names display `N/A`.

## Schedule and Traceability

- **Sprint:** Sprint 1 – Country Reports (assigned to Sprint 1 in Group 11's GitHub Project).
- **GitHub issue:** #2 (shown as Done on the project board; this document does not replace code/test evidence).
- **Release:** Confirm the release name against the group's GitHub Releases; do not assume `1.0`.
- **Report class:** `AllCountriesReport` (called by `AutoDisplay.java`).
- **Shared query method:** `CountryReport.displayAllCountries(...)`.
- **Output formatter:** `ReportDisplay.printTable(...)`.
- **Evidence:** Add a genuine console-output screenshot for Issue #2 under `docs/screenshots/` and link it in the project `README.md`.

*Documentation basis: `App.java`, `AutoDisplay.java`, `CountryReport.java` and `ReportDisplay.java` supplied for review. The six individual report-wrapper class bodies were not supplied; their invocation is visible in `AutoDisplay.java`.*
