# Issue #100 - Docker deployment entry point

## 1. Issue details

- Issue: [#100 - Fix Docker deployment to execute the complete population reporting system](https://github.com/kaungmyatlwin18/Group_11-DevOps/issues/100)
- Implementation branch: `fix/docker-deployment-entry-point`
- Investigation date: 28 November 2026 (Myanmar time)
- Original develop commit: `94ac677`

## 2. Observed defect

The original Maven build passed with 434 tests recorded, 431 passed, three skipped and zero failures or errors.

However, the default Docker deployment displayed only five demonstration country rows and exited successfully without executing the 32 expected report sections.

The shaded JAR manifest incorrectly selected `com.napier.sem.Main` instead of `com.napier.sem.App`.

A separate diagnostic execution of App with missing database credentials also returned exit code 0 despite reporting a database error.

## 3. Diagnostic workaround

The existing Docker image was tested using a temporary application entry-point override:

```bash
docker compose run --rm --no-deps --entrypoint java app -cp /app/app.jar com.napier.sem.App
```

The diagnostic execution successfully connected to MySQL, executed all 32 report sections and returned exit code 0.

Evidence:

- [Diagnostic verification commands](deployment-override-commands.png)
- [Temporary entry-point override verification](deployment-override-verification.png)

This workaround confirmed the deployment entry-point problem but was not considered a permanent solution.

## 4. Implemented correction

The permanent correction included:

- Changing the Maven shaded JAR main class from Main to App.
- Adding database startup connection retry handling.
- Returning a non-zero exit code when database connection or report execution fails.
- Adding six deployment regression tests.
- Improving GitHub Actions to verify all 32 expected report identifiers.
- Detecting missing or duplicate report sections.
- Verifying database connection and report completion.
- Testing unsuccessful deployment when database credentials are missing.

No database schema or report-query changes were introduced.

### Implementation commits

| Commit | Description |
|---|---|
| [394f09a](https://github.com/kaungmyatlwin18/Group_11-DevOps/commit/394f09a) | Application deployment correction and regression tests |
| [21d3561](https://github.com/kaungmyatlwin18/Group_11-DevOps/commit/21d3561) | Automated CI deployment verification |

## 5. Local verification results

| Verification | Actual result |
|---|---|
| New deployment regression tests | 6 passed |
| Total Maven tests | 440 |
| Passed tests | 437 |
| Skipped integration tests | 3 |
| Failures | 0 |
| Errors | 0 |
| Maven build | BUILD SUCCESS |
| Docker image build | Successful |
| Report sections executed | 32 of 32 |
| Database connection | Successful |
| Report completion marker | Present |
| Database error scan | 0 matches |
| Successful deployment exit code | 0 |
| Missing credentials exit code | 1 |

The permanent deployment was executed using the default Docker entry point without a temporary override.

Evidence:

- [Permanent Docker deployment verification](deployment-permanent-verification.png)
- [Deployment regression test results](deployment-regression-tests%202.png)
- [Negative credentials test](deployment-negative-test.png)

## 6. GitHub Actions verification

The improved Maven and Docker CI workflow completed successfully for commit `21d3561`.

- Maven build and automated tests passed.
- Docker image build and application execution passed.
- All 32 distinct expected report identifiers were verified.
- Database connection and completion messages were confirmed.
- The missing credentials test correctly returned exit code 1.
- Container cleanup completed successfully.

During CI deployment, four temporary connection failures occurred while MySQL was starting. The application successfully retried and established a database connection.

The automated verification produced the following message:

```text
Deployment verified: 32 distinct expected report sections, connection and completion confirmed.
```

Evidence:

- [Successful GitHub Actions screenshot](deployment-ci-success.png)
- [GitHub Actions workflow execution log](https://github.com/kaungmyatlwin18/Group_11-DevOps/actions/runs/46075305705/job/140761536625)

## 7. Before-and-after comparison

| Verification | Before correction | After correction |
|---|---|---|
| Default Docker report sections | 0 of 32 | 32 of 32 |
| Application entry point | Main | App |
| Missing credentials exit code from App | 0 | 1 |
| Deployment regression tests | Not implemented | 6 passed |
| Automated report completeness verification | Not implemented | Passed |

## 8. Limitations and review status

The original five-country deployment failure screenshot is unavailable. The observed defect is documented using the original terminal results and reproduction procedure.

Successful execution of all 32 report sections establishes deployment completeness but does not independently prove the functional correctness of every report.

Existing dataset warnings are outside the scope of this correction.

The separate MySQL integration workflow, independent pull request approval and merge into develop remain pending.