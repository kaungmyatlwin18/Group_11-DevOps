# Group 11 — Final development verification

**Tested baseline:** [`31d6c65`](https://github.com/kaungmyatlwin18/Group_11-DevOps/commit/31d6c65456e20c0f630487459218cd36386a52cc) (`develop`, 28 November 2026, Myanmar time). This record is not a master release or evidence of exhaustive input testing.

## 1. Reviewed corrections

| PR | Result |
|---|---|
| [#98](https://github.com/kaungmyatlwin18/Group_11-DevOps/pull/98) | Runtime country Top N configuration; merged at `cbc3cca`, Issue #97 closed |
| [#99](https://github.com/kaungmyatlwin18/Group_11-DevOps/pull/99) | Master coverage push trigger enabled; merged at `e53926a` |
| [#101](https://github.com/kaungmyatlwin18/Group_11-DevOps/pull/101) | Complete Docker entry point and failure handling; merged at `31d6c65`, Issue #100 closed |

## 2. Automated tests and CI

Local `mvn -B clean verify`: 445 tests recorded, 442 passed, 3 skipped, 0 failures and 0 errors; BUILD SUCCESS in 10.084 seconds. MySQL integration CI passed 445/445 without skips. The suites overlap.

- [Local Maven packaging and coverage generation screenshot](final-maven-build-success.png) (shows BUILD SUCCESS and 71 analysed classes, not the test-count summary).
- [Unit test job](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/runs/46155389733/job/141010799279): 445 recorded, 3 skipped, 0 failures/errors.
- [MySQL integration job](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/runs/46155389834/job/141010799596): 445 recorded, 0 skipped, 0 failures/errors.
- [Coverage job](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/runs/46155389760/job/141010799369): 445 recorded, 3 skipped, 0 failures/errors.
- [Maven and Docker job](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/runs/46155389736/job/141010799282): 445 recorded, 3 skipped, 0 failures/errors; 32 distinct expected report sections verified, including connection/completion and negative credentials verification.

## 3. IntelliJ functional execution

The custom configuration used:

```text
-DtopN.world=5 -Dcountry.continent=Europe -Dcountry.continent.topN=5 -Dcountry.region="Southeast Asia" -Dcountry.region.topN=2
```

- [Issue #8: Europe, N = 5](../screenshots/issue-08.png)
- [Issue #9: Southeast Asia, N = 2](../screenshots/issue-09.png)
- [Application completion and process exit code 0](final-application-completion.png)

All 32 report sections executed. The full report output was reviewed separately; screenshots show representative visible results. The existing requirement evidence is indexed in the root README.

## 4. Default Docker deployment

Executed `docker compose run --rm --no-deps -T app` using the rebuilt image and existing MySQL database, without an entry-point override. Default Top N parameters were used, not the IntelliJ overrides.

| Metric | Result |
|---|---:|
| Expected / actual / distinct report identifiers | 32 / 32 / 32 |
| Missing / unexpected / duplicate identifiers | 0 / 0 / 0 |
| Database error scan matches | 0 |
| Successful deployment exit code (original PowerShell output) | 0 |

Connection and completion were confirmed in the full log. The screenshot shows the summary and current branch/commit, but does not display the exit code. The original PowerShell output recorded exit code 0 separately.

- [Final Docker summary screenshot](final-docker-deployment-success.png)
- [Complete deployment output](group11-final-deployment.txt) — converted from UTF-16 to UTF-8 for GitHub viewing; line endings normalised and trailing layout padding removed. Report data and terminal encoding artefacts are retained.
- [Historical Issue #100 investigation](../bug-evidence/issue-100-deployment/README.md), including missing credentials exit code 1.

## 5. Coverage

| Metric | Covered | Total | Coverage |
|---|---:|---:|---:|
| Instruction | 5527 | 5754 | 96.05% |
| Branch | 435 | 528 | 82.39% |
| Line | 1172 | 1231 | 95.21% |
| Method | 202 | 212 | 95.28% |
| Class | 69 | 71 | 97.18% |
| Complexity | 393 | 476 | 82.56% |

[Local JaCoCo HTML screenshot](../screenshots/jacoco-final-coverage.png). Line and branch counts were independently confirmed in the coverage job log. Master measurement is pending.

## 6. Limitations and remaining submission work

The World dataset is historical. Cocos (Keeling) Islands, Gibraltar and Singapore have recorded city populations exceeding total population; the raw values are retained with an explicit warning. Negative non-city values are not valid demographic estimates. Some accented characters in the Windows Docker log are misrendered, consistent with a terminal encoding mismatch; the cause has not been independently isolated. Coverage and successful report execution do not prove all input combinations or exact functional correctness. All 32 requirement screenshot paths exist in the baseline repository tree; only #8 and #9 are refreshed here, and older screenshots are not newly captured evidence.

Pending: agreement on CR2 contribution percentages totalling 100%, final board evidence, independent documentation review, master release PR and verification, measured master coverage and final submission. No historical bug evidence is replaced by this record.
