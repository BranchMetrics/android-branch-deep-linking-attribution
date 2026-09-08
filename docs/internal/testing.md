# Testing and CI (6.0.0-beta.0)

> Contributor-facing notes for work on this branch.
> Last updated 2026-09-03, against `6.0.0-beta.0` at 88b3bf28.

## Two source sets, both under `io.branch.referral`

| Source set | Runner | Notes |
| --- | --- | --- |
| `Branch-SDK/src/test/` | JVM plus Robolectric | 42 files. Base class `BranchTestBase.kt`. `UnconfinedTestDispatcher` and the `kotlinx-coroutines-test` virtual clock are already on the classpath, see `ModernLinkGeneratorTest` |
| `Branch-SDK/src/androidTest/` | emulator or device | 30 files. `Branch.shutDown()` plus a shared-prefs clear in `@Before`, network swapped with `MockRemoteInterface` |

The unit source set is substantial here, unlike on `master` where only three files exist. Changes on this branch generally want tests in **both** sets.

**`Branch.getTestInstance` does not exist**, on this branch or on `master`. Instrumented tests use `Branch.getInstance()` plus `setBranchRemoteInterface(new MockRemoteInterface())`.

Several queue unit tests reflect on private internals (`RequestRetryInfo`, `shouldFailRequest`, `tryResolveStuck*`). Changing those internals means rewriting the tests, not deleting them.

## Running

```bash
# JVM
./gradlew :Branch-SDK:testDebugUnitTest
./gradlew :Branch-SDK:testDebugUnitTest --tests "io.branch.referral.ModernLinkGeneratorTest"

# Instrumented (device required)
./gradlew :Branch-SDK:connectedDebugAndroidTest
./gradlew :Branch-SDK:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.branch.referral.BranchApiTests

# Coverage: aggregates both, depends on createDebugCoverageReport, so it needs a device
./gradlew :Branch-SDK:jacocoTestReport
```

## Adding a test

Before adding a test, name the failure it catches that no existing test catches, and say so in the PR body.

Test the edge the code exists to handle, not the happy path. A guard, a cap, a truncation, a retry, a fallback: each was written for one specific input, and that input is the only one that verifies it.

**Timing and ordering bugs in the queue do not reliably surface in CI.** When you fix one, add a regression test that reproduces the exact scenario, not a unit test of the mechanism in isolation. The queue's intent-lock ordering and its retry-ceiling rule have each been broken once already.

## CI

| Workflow | Runs on this branch? | What it does |
| --- | --- | --- |
| `unit-and-instrumented-tests-action.yml` | **no** | it is `disabled_manually` at repo level and has zero runs in its history, so nothing has ever exercised the suite in CI on any branch. Tracked as EMT-4261 |
| `sdk-l1-validation.yml` | yes | EMT-4077 added `6.0.0-beta.0` to both triggers, so it runs on push to this branch and on PRs into it. A PR targeting a branch further up a stack is not covered, since only the listed bases match; use `workflow_dispatch` there |
| `gptdriverautomation.yaml` | pushes to `Release-*`, or manual | drives the `:Branch-SDK-GPTDriver` MobileBoost E2E module |
| `apiCompatibilityReport` | not a workflow at all | a Gradle task that diffs the public API against a pinned 5.x baseline. Report-only; pass `-PapiDiffStrict` to make a break fail the build |

The L1 wire gate does check the wire format here, and as of EMT-4081 it judges each capture against its scenario contract rather than on field presence alone. What is not checked is the unit and instrumented suite, since that workflow is disabled. To reproduce a wire check locally: `./scripts/run_l1_instrumented.sh <your MobileBoost key>` installs both APKs, runs `TEST_CLASS` and pulls the log, then `python3 scripts/validate_l1_logs.py path/to/branchlogs.txt --scenario <NAME>` judges it. Notes in `scripts/README.md`.
