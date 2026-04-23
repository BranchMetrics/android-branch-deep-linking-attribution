# Branch-SDK-GPTDriver

MobileBoost / GPTDriver hybrid test module for the Branch Android SDK TestBed.

The module follows a hybrid philosophy: **deterministic Espresso first, AI-assisted validation only when Espresso matchers cannot express the intent**.

## What is this?

An Android instrumentation test module (`com.android.test` Gradle plugin) that drives the Branch SDK TestBed app through end-to-end scenarios and reports results to the MobileBoost cloud dashboard. Each test case:

1. Performs deterministic steps via Espresso — taps views by `R.id.*`, reads text fields, asserts with `ViewAssertions.matches(...)`.
2. When the assertion is visual, semantic, or multi-conditional, hands off to the [`gptdriver-lib`](https://docs.mobileboost.io/gpt-driver-sdk/espresso/view-xml-based-apps/setup) SDK — `driver.execute`, `driver.assertCondition`, `driver.assertBulk`, `driver.extract`, `driver.checkBulk`.
3. Reports `driver.setSessionStatus("success" | "failed")` to the MobileBoost dashboard from the individual tests.

## Quick start

```bash
# 1. Add your MobileBoost API key to local.properties
#    (the file is gitignored — your key never leaves your machine)
echo 'MOBILEBOOST_API_KEY=your_key_here' >> local.properties

# 2. Run the full suite on a connected device or emulator
./gradlew :Branch-SDK-GPTDriver:connectedDebugAndroidTest
```

To run a single class:

```bash
./gradlew :Branch-SDK-GPTDriver:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.branch.gptdriver.tests.LinkCreationHybridTest
```

To override the key without editing `local.properties`:

```bash
./gradlew :Branch-SDK-GPTDriver:connectedDebugAndroidTest \
  -PMOBILEBOOST_API_KEY=your_key_here
```

Each run opens a session on the MobileBoost dashboard; `driver.sessionURL` is available from within the test after initialization.

## Layout

```
Branch-SDK-GPTDriver/
├── build.gradle.kts                      — com.android.test module, gptdriver-lib dep
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/io/branch/gptdriver/
│       ├── BaseGptDriverTest.kt          — base class, driver init, activity rule
│       ├── LinkGenerationIdlingResource.kt
│       │                                 — Espresso idling resource polling the URL
│       │                                   text field until a real link appears
│       └── tests/
│           ├── LinkCreationDeterministicTest.kt   ← 100% Espresso, no AI
│           ├── LinkCreationAITest.kt              ← 100% AI-driven
│           ├── LinkCreationHybridTest.kt          ← Espresso actions + AI validation
│           ├── QRCodeHybridTest.kt
│           ├── ShareLinkHybridTest.kt
│           ├── SessionAndLogsHybridTest.kt
│           ├── UserIdentityHybridTest.kt
│           ├── EventLoggingHybridTest.kt
│           ├── DeepLinkColdOpenHybridTest.kt
│           ├── DeepLinkWarmOpenHybridTest.kt
│           ├── BrowserExperienceHybridTest.kt
│           ├── NotificationHybridTest.kt
│           ├── TrackingControlHybridTest.kt
│           ├── ConsumerProtectionHybridTest.kt
│           ├── ReferringParamsHybridTest.kt
│           └── PluginNotifyHybridTest.kt
├── mobileboost/                          — AI-only JSON suites used by the Path A
│   │                                       runner (MobileBoost cloud via Appium).
│   │                                       This module's hybrid tests run locally
│   │                                       via `connectedAndroidTest` — the JSON
│   │                                       suites are the separate cloud path.
│   ├── mobileboost-ids.json
│   ├── dependencies/*.json
│   └── suites/*.json
└── scripts/
    ├── capture-test-logs.sh              — run the suite with logcat capture,
    │                                        then print paths to the HTML
    │                                        report, JUnit XMLs, and the
    │                                        MobileBoost session URLs.
    └── format-test-results.sh            — parse connectedAndroidTest log into a
                                             markdown summary row
```

## Dependencies

- Maven: `io.mobileboost.gptdriver:gptdriver-lib:1.3.2` with the Selenium and Netty transitive exclusions declared in `build.gradle.kts`.
- `androidx.test:runner`, `androidx.test:rules`, `androidx.test.espresso:espresso-core`, `androidx.test.uiautomator`.
- `minSdk = 24`. The host app `Branch-SDK-TestBed` targets the same compile SDK as the rest of the workspace (`ANDROID_BUILD_SDK_VERSION_COMPILE` from the root `gradle.properties`).
- Runs on device or emulator. Physical devices require developer mode and USB debugging; emulators should use a Google Play system image (needed for some deep-link resolution paths).
- Uses AndroidX Test Orchestrator (`execution = "ANDROIDX_TEST_ORCHESTRATOR"`) so each test runs in its own instrumentation process — required for the reset-state-between-tests behavior that several hybrid tests rely on.

## Secret management

The API key is resolved in this order inside `build.gradle.kts`:

1. Gradle property `-PMOBILEBOOST_API_KEY=xxx`
2. `MOBILEBOOST_API_KEY` in `local.properties` (root of the repo, gitignored)
3. Environment variable `MOBILEBOOST_API_KEY`
4. Empty → `BaseGptDriverTest.setUp` throws `IllegalStateException` with a clear error message

The resolved value is baked into `BuildConfig.MOBILEBOOST_API_KEY` so the test process reads it at runtime without any file I/O.

Copy [`local.properties.example`](../local.properties.example) from the repo root, rename to `local.properties`, and paste your key. The example file is committed; the real file is gitignored by the `local.properties` rule in the root `.gitignore`.

## Test tagging

All hybrid test classes inherit `@LargeTest` from `BaseGptDriverTest.kt` so they show up in the `androidx.test.filters.LargeTest` suite. To run only the large-test subset (same as the full hybrid suite today, but the filter is forward-compatible):

```bash
./gradlew :Branch-SDK-GPTDriver:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.size=large
```

Individual `@SmallTest` / `@MediumTest` tags can be added per method if a faster dev-loop smoke subset is needed later; nothing about the current structure blocks that.

## Inspecting test behavior

Once a run has finished (or crashed mid-run), four artifacts give you a complete picture of what happened. The first three are produced by Gradle on every run; the fourth requires a logcat capture and is the richest source of per-test behavior.

### 1. Gradle HTML report

```
Branch-SDK-GPTDriver/build/reports/androidTests/connected/debug/index.html
```

The canonical summary: counts, failed-tests table, per-class breakdown, stack traces for failures. Open `index.html` in any browser — it is self-contained (CSS/JS alongside).

### 2. JUnit XML (per-device, per-class)

```
Branch-SDK-GPTDriver/build/outputs/androidTest-results/connected/
```

One XML file per device + class, e.g. `TEST-Pixel_9a(AVD) - 16_BrowserExperienceHybridTest.xml`. These include `<system-out>` and `<system-err>` blocks that carry stdout/stderr from *successful* tests as well — including the MobileBoost session URL that the gptdriver-lib SDK prints on initialization. Grep for `sessionURL=` or `app.mobileboost.io/` inside the XML for a quick lookup.

### 3. Markdown summary row

```bash
./Branch-SDK-GPTDriver/scripts/format-test-results.sh <path-to-gradle-log>
```

Emits a single markdown table row (date, SDK version, destination, pass/fail/skip, runtime) — useful for pasting into PR descriptions or release notes. See the header of the script for the exact output format.

### 4. Logcat + MobileBoost session URLs

The richest per-test source. Every call into the gptdriver-lib SDK (`driver.execute`, `driver.assertBulk`, `driver.assertCondition`, …) prints a `https://app.mobileboost.io/...` session URL to logcat. Opening that URL replays the AI's execution with video, step-by-step reasoning, and the screenshots the model evaluated — this is what you want for understanding how a test behaved, even if it passed.

To capture logcat alongside a Gradle run, use the helper script:

```bash
./Branch-SDK-GPTDriver/scripts/capture-test-logs.sh
```

On exit it prints the paths of:
- the captured logcat file (`Branch-SDK-GPTDriver/build/test-logs/logcat-<timestamp>.log`)
- the Gradle log
- the HTML report
- the JUnit XML directory
- every unique MobileBoost session URL it found in the logcat
- a startup-crash sanity check (`NoClassDefFoundError.*CollectionsKt`, `StartupException`) — expect 0

You can pass Gradle arguments through the wrapper, e.g. to run a single class:

```bash
./Branch-SDK-GPTDriver/scripts/capture-test-logs.sh \
  -Pandroid.testInstrumentationRunnerArguments.class=io.branch.gptdriver.tests.LinkCreationHybridTest
```

To capture logcat without going through the wrapper (for example, when running Gradle manually or from an IDE):

```bash
adb logcat -c
adb logcat -v threadtime > run.log &
./gradlew :Branch-SDK-GPTDriver:connectedDebugAndroidTest
kill %1
grep -oE "https://app.mobileboost.io/[A-Za-z0-9/_-]+" run.log | sort -u
```

### Flaky hybrid tests

A handful of hybrid tests rely on AI visual assertions and native UI timing that can flake on a cold emulator. The base class (`BaseGptDriverTest.kt`) and `GptDriverRetry.withRetry` already add per-step retries with exponential backoff, and each retry attempt is logged. When a test fails, the fastest path to a diagnosis is the MobileBoost session URL for that specific attempt — it shows exactly which screenshot the AI evaluated and what it decided.

## See also

- [`TESTING_GUIDE.md`](./TESTING_GUIDE.md) — writing new tests, Espresso decision matrix, TestBed resource IDs, imports template.
