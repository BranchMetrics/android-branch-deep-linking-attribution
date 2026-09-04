# Testing and CI (master)

> Contributor-facing notes for work on this branch.
> Last updated 2026-09-03, against `master` at 827655ea.

## The shape of the suite

Two source sets, both under `io.branch.referral`:

| Source set | Runner | Contents |
| --- | --- | --- |
| `Branch-SDK/src/test/` | JVM, no device | **Three files only**: `BranchConfigurationControllerTest.kt`, `InstallReferrerResultTests.kt`, `BranchPartnerParametersTest.java` |
| `Branch-SDK/src/androidTest/` | emulator or device | 21 files. This is the real suite |

**Do not treat a green `testDebugUnitTest` as verification of a behavior change.** It exercises three narrow classes. Anything touching init, the request queue, `PrefHelper`, or the wire format has to run on a device.

Instrumented tests reset global state in `@Before` using `Branch.shutDown()` plus a shared-prefs clear, and swap the network with `setBranchRemoteInterface(new MockRemoteInterface())`. Follow the existing base classes rather than inventing a new harness.

Test tasks set `maxRetries = 3`, so a failing test gets up to three retries (four attempts total) before CI reports it red.

## Running

```bash
# JVM
./gradlew :Branch-SDK:testDebugUnitTest
./gradlew :Branch-SDK:testDebugUnitTest --tests "io.branch.referral.BranchConfigurationControllerTest"
./gradlew :Branch-SDK:testDebugUnitTest --tests "*BranchConfigurationControllerTest.someMethod"

# Instrumented (device required)
./gradlew :Branch-SDK:connectedDebugAndroidTest
./gradlew :Branch-SDK:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.branch.referral.BranchApiTests

# Coverage: aggregates unit and instrumented, depends on createDebugCoverageReport, so it needs a device
./gradlew :Branch-SDK:jacocoTestReport

# Lint: the SDK module sets abortOnError = false, so lint reports but never fails the build
./gradlew :Branch-SDK:lint
```

## Adding a test

Before adding a test, name the failure it catches that no existing test catches, and say so in the PR body.

Test the edge the code exists to handle, not the happy path. A guard, a cap, a truncation, a retry, a fallback: each was written for one specific input, and that input is the only one that verifies it.

Timing and ordering bugs in the queue do not reliably surface in CI. When you fix one, add a regression test that reproduces the exact scenario, not a unit test of the mechanism in isolation.

## CI

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `unit-and-instrumented-tests-action.yml` | every push | `testDebugUnitTest` plus `connectedDebugAndroidTest` on API 21 and 34, then `jacocoTestReport` to Codecov |
| `sdk-l1-validation.yml` | PRs into `master`, `main`, `feature/mobileboost-e2e-tests` | the Layer 1 wire gate, described below |
| `gptdriverautomation.yaml` | pushes to `Release-*`, or manual | drives the `:Branch-SDK-GPTDriver` MobileBoost E2E module |
| `stale.yml` | nightly | marks and closes inactive issues |

### The Layer 1 wire gate

`sdk-l1-validation.yml` builds the TestBed and GPTDriver APKs, runs an instrumented test on an emulator, pulls `branchlogs.txt` off the device, and asserts that the required device and SDK fields are actually on the wire. The check is **presence-only** and scoped to `/v1/*`: a required field is either there or it is not. Type and value-format checks are deliberately left to the backend ingestion gate.

Reproduce it locally against any captured log:

```bash
./scripts/run_l1_instrumented.sh <your MobileBoost key>   # installs both APKs, runs the test, pulls the log
python3 scripts/validate_l1_logs.py path/to/branchlogs.txt
python3 -m unittest scripts.test_validate_l1_logs -v      # the validator's own tests
```

The required-field lists live at the top of `scripts/validate_l1_logs.py`. Full notes: `scripts/README.md`.

The workflow deliberately bypasses `connectedDebugAndroidTest`, because for `com.android.test` modules using the test orchestrator with `clearPackageData=true`, AGP uninstalls the target package when the run finishes and the log can never be pulled. Do not "fix" that by routing it back through the standard task.
