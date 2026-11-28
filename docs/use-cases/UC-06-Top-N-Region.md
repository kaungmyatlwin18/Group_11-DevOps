# UC-06 – View Top N Countries in a Region

## Characteristic Information

| Field | Details |
|---|---|
| **Use Case ID** | UC-06 |
| **Use Case Name** | View Top N Countries in a Region |
| **Related GitHub Issue** | #9 |
| **Goal in Context** | As an Report Analyst, I want the N most populated countries in a region so that I can compare the leading populations within that region. |
| **Scope** | Group 11 Population Reporting System |
| **Level** | Primary task |
| **Primary Actor** | Report Analyst |
| **Trigger** | The user launches the Java application; `App.main()` connects to the World database and starts the automatic country reports. |
| **Preconditions** | The application can run and connect to a populated World MySQL database containing the `country` and `city` tables. |
| **Success End Condition** | Up to N countries from the specified region are shown from largest to smallest population. |
| **Failed End Condition** | The report cannot be generated because the database connection or SQL operation fails, or required parameters are invalid. |

> **Current implementation:** The reviewed program automatically runs the reports through `AutoDisplay`. It does **not** show an interactive report-selection menu. The main scenario below describes existing console behaviour rather than a proposed interface.

## Main Success Scenario

1. The user launches the application and `App.main()` connects to the World database.
2. `AutoDisplay` reads `country.region` and `country.region.topN` JVM properties and passes the selected region and N to the report method for Issue #9. When no non-blank values are supplied, the default is **Eastern Asia** and **N = 3**.
3. `CountryReport.displayTopNCountriesInRegion(...)` validates that the region is non-blank and N is positive.
4. The parameterised SQL query filters rows with `c.Region = ?`, orders them by descending population with country code as a tie-breaker, and limits results with `LIMIT ?`.
5. The system retrieves the matching records and capital names.
6. `ReportDisplay.printTable(...)` outputs the Top N region report in the console.

## Extensions

- **E1 – Blank/null region:** The direct report method rejects blank/null values; the automatic runner instead uses its default for missing or blank JVM settings.
- **E2 – N is non-numeric, non-integer, zero or negative:** The runner rejects the supplied JVM value with `IllegalArgumentException`.
- **E3 – No matching region data:** The formatter prints `No results found.`.
- **E4 – Fewer than N matches:** The report contains only available records.
- **E5 – Database/SQL failure:** `App` prints a database error message.

No interactive error-recovery prompt is present in the reviewed code.

## Sub-Variations

- The automatic run defaults to **Eastern Asia** and **N = 3**.
- Before running `com.napier.sem.App` in IntelliJ, the user may set VM options: `-Dcountry.region="Southeast Asia" -Dcountry.region.topN=2`.
- Whitespace around the input is trimmed. Missing or blank settings use the defaults. A supplied non-integer or non-positive N is rejected.
- The same parameterised SQL, sorting and output columns are retained. No interactive selection menu is required.

## Expected Report Output

Each row contains **Code, Name, Continent, Region, Population, Capital**, with the highest population first. Country code is used to break ties in population. Results are shown as an aligned console table. Missing capital names display `N/A`.

## Schedule and Traceability

- **Sprint:** Sprint 1 – Country Reports (assigned to Sprint 1 in Group 11's GitHub Project).
- **GitHub issue:** #9 (shown as Done on the project board; this document does not replace code/test evidence).
- **Release:** Confirm the release name against the group's GitHub Releases; do not assume `1.0`.
- **Report class:** `TopNCountriesByRegionReport` (called by `AutoDisplay.java`).
- **Shared query method:** `CountryReport.displayTopNCountriesInRegion(...)`.
- **Output formatter:** `ReportDisplay.printTable(...)`.
- **Evidence:** Capture a genuine console-output screenshot using a non-default location and N, and retain it with the final assessment evidence.

*Documentation basis: the report runner, report wrapper, shared SQL and formatter. Runtime input enhancement is subject to build, test and review verification.*
