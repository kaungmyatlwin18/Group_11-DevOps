# Group 11 - Bug Reporting System

## Purpose

The bug reporting system enables the team to identify,
document, track and resolve defects in the Population
Reporting System using GitHub Issues.

## Reporting a Bug

1. Open the repository's Issues tab.
2. Select New issue.
3. Select the Bug report template.
4. Describe the defect and reproduction steps.
5. Record expected and actual behaviour.
6. Provide environment details and supporting evidence.
7. Apply the bug label.
8. Add the issue to the Group 11 project board.

## Severity Levels

| Severity | Description |
| --- | --- |
| Critical | Application cannot operate or major functionality is unavailable. |
| High | A major requirement produces incorrect results. |
| Medium | A feature has a defect but a workaround exists. |
| Low | Minor display, usability or documentation issue. |

## Bug Resolution Workflow

1. Record and reproduce the reported defect.
2. Review its severity and priority.
3. Assign a responsible developer.
4. Move the issue to In Progress.
5. Create a dedicated bug-fix branch.
6. Implement the fix and appropriate regression tests.
7. Run Maven tests and verify the build.
8. Commit and push the changes.
9. Create a pull request targeting develop.
10. Review the changes and confirm CI checks pass.
11. Merge the approved pull request.
12. Verify the corrected behaviour.
13. Close the bug and move it to Done.

## Branch Naming Convention

fix/issue-N-short-description

Replace N with the actual GitHub issue number.

## Bug Fix Commit Convention

fix(scope): describe the resolved defect (#N)

## Verification

Bug fixes should be supported by reproducible evidence,
automated tests where appropriate, successful CI checks
and reviewed pull requests.

Only genuine defects should be recorded as bugs.

## Assessment Reference

SET09803 DevOps - Code Review 2
Checklist Submission 2: Bug reporting system set-up.
