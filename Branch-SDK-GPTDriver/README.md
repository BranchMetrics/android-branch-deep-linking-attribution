# Branch-SDK-GPTDriver

Keyless L1 wire-validation driver module for the Branch Android SDK TestBed.

## What it is

A `com.android.test` module that targets `:Branch-SDK-TestBed`. Its tests are plain instrumentation tests: each one launches the TestBed, exercises one flow, and leaves the SDK's wire log in the TestBed's `branchlogs.txt`. The L1 gate then judges that log. No API key and no external service are needed.

The module and package names (`Branch-SDK-GPTDriver`, `io.branch.gptdriver`) are historical and will be renamed separately.

## Run it

Needs a running device or emulator. This runs the `organic_open` scenario the way CI does:

```bash
./gradlew :Branch-SDK-TestBed:assembleDebug
./gradlew :Branch-SDK-GPTDriver:assembleDebug
TEST_CLASS=N1OrganicOpenWireTest OUTPUT_LOG=wire-organic_open.txt CLEAR_LOG=1 WIPE_FIRST=1 ./scripts/run_l1_instrumented.sh
python3 scripts/validate_l1_logs.py wire-organic_open.txt --scenario organic_open
```

`.github/workflows/sdk-l1-validation.yml` has the command for every scenario CI runs. With neither `TEST_CLASS` nor `COLD_SCENARIO` set, `run_l1_instrumented.sh` runs `LinkCreationDeterministicTest`, which CI does not use.

## Layout

```
Branch-SDK-GPTDriver/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    └── java/io/branch/gptdriver/
        ├── LinkFieldReader.kt                  reads the generated link from the TestBed
        ├── LinkGenerationIdlingResource.kt     waits for the TestBed's link field
        └── tests/
            ├── N1OrganicOpenWireTest.kt          organic_open
            ├── ScenarioLinkGenerator.kt          creates the link a cold run opens, unless
            │                                     COLD_LINK_URL gives one (cold_uriScheme)
            ├── WireScenarioDriver.kt             steps the warm and hot drivers share
            ├── W1WarmHttpsWireTest.kt            warm_https_onNewIntent
            ├── W2WarmUriSchemeWireTest.kt        warm_uriScheme
            ├── H1HotAppLinkWireTest.kt           hot_https_foreground
            ├── H2HotUriSchemeWireTest.kt         hot_uriScheme
            ├── N2NoStickinessWireTest.kt         no_stickiness and no_stickiness_none,
            │                                     two methods run with TEST_METHODS
            └── LinkCreationDeterministicTest.kt  Espresso link creation, not run by CI
```
