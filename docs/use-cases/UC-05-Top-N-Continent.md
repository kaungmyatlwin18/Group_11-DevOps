# UC-05 – View Top N Countries in a Continent

## Characteristic Information

| Field | Details |
|---|---|
| **Use Case ID** | UC-05 |
| **Use Case Name** | View Top N Countries in a Continent |
| **Related GitHub Issue** | #8 |
| **Goal in Context** | As an Report Analyst, I want the N most populated countries in a continent so that I can compare the largest populations in that continent. |
| **Scope** | Group 11 Population Reporting System |
| **Level** | Primary task |
| **Primary Actor** | Report Analyst |
| **Trigger** | The user launches the Java application; `App.main()` connects to the World database and starts the automatic country reports. |
| **Preconditions** | The application can run and connect to a populated World MySQL database containing the `country` and `city` tables. |
| **Success End Condition** | Up to N countries in the chosen continent are displayed in descending population order. |
| **Failed End Condition** | The report cannot be generated because the database connection or SQL operation fails, or required parameters are invalid. |

> **Current implementation:** The reviewed program automatically runs the reports through `AutoDisplay`. It does **not** show an interactive report-selection menu. The main scenario below describes existing console behaviour rather than a proposed interface.

## Main Success Scenario

1. The user runs the Java application and a World database connection is established.
2. `AutoDisplay` reads `country.continent` and `country.continent.topN` JVM properties and passes the selected continent and N to the report method for Issue #8. When no non-blank values are supplied, the default is **Asia** and **N = 3**.
3. The shared `CountryReport.displayTopNCountriesInContinent(...)` method checks for a non-blank continent and a positive N.
4. A parameterised SQL query filters on `c.Continent = ?`, orders populations descending (country code ascending for ties), and applies `LIMIT ?`.
5. The system retrieves each matching country's code, name, continent, region, population and capital.
6. `ReportDisplay.printTable(...)` prints the Top N continent report.

## Extensions

- **E1 – Blank or null continent:** The direct report method rejects blank/null values; the automatic runner instead uses its default for missing or blank JVM settings.
- **E2 – N is non-numeric, non-integer, zero or negative:** The runner rejects the supplied JVM value with `IllegalArgumentException`.
- **E3 – No matches:** The console prints `No results found.`.
- **E4 – Fewer than N matching countries:** Only the existing matches are displayed.
- **E5 – Database/SQL failure:** `App` reports the database error.

The reviewed application does not provide a prompt to correct invalid parameters.

## Sub-Variations

- The automatic run defaults to **Asia** and **N = 3**.
- Before running `com.napier.sem.App` in IntelliJ, the user may set VM options: `-Dcountry.continent="Europe" -Dcountry.continent.topN=2`.
- Whitespace around the input is trimmed. Missing or blank settings use the defaults. A supplied non-integer or non-positive N is rejected.
- The same parameterised SQL, sorting and output columns are retained. No interactive selection menu is required.

## Expected Report Output

Each row contains **Code, Name, Continent, Region, Population, Capital**, with the highest population first. Country code is used to break ties in population. Results are shown as an aligned console table. Missing capital names display `N/A`.

## Schedule and Traceability

- **Sprint:** Sprint 1 – Country Reports (assigned to Sprint 1 in Group 11's GitHub Project).
- **GitHub issue:** #8 (shown as Done on the project board; this document does not replace code/test evidence).
- **Release:** Confirm the release name against the group's GitHub Releases; do not assume `1.0`.
- **Report class:** `TopNCountriesByContinentReport` (called by `AutoDisplay.java`).
- **Shared query method:** `CountryReport.displayTopNCountriesInContinent(...)`.
- **Output formatter:** `ReportDisplay.printTable(...)`.
- **Evidence:** Capture a genuine console-output screenshot using a non-default location and N, and retain it with the final assessment evidence.

*Documentation basis: the report runner, report wrapper, shared SQL and formatter. Runtime input enhancement is subject to build, test and review verification.*
