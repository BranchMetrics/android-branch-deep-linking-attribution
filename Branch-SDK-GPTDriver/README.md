# Branch-SDK-GPTDriver

Keyless L1 wire-validation driver module for the Branch Android SDK TestBed.

## What it is

A `com.android.test` module that targets `:Branch-SDK-TestBed`. Its tests are plain Espresso drivers: each one launches the TestBed, exercises one flow, and leaves the SDK's wire log in the TestBed's `branchlogs.txt`. The L1 gate then judges that log. No API key and no external service are needed.

The module and package names (`Branch-SDK-GPTDriver`, `io.branch.gptdriver`) are historical and will be renamed separately.

## Run it

```bash
./gradlew :Branch-SDK-TestBed:assembleDebug :Branch-SDK-GPTDriver:assembleDebug
./scripts/run_l1_instrumented.sh
python3 scripts/validate_l1_logs.py branchlogs.txt
```

`run_l1_instrumented.sh` needs a running device or emulator. It installs both APKs, runs `TEST_CLASS` through `am instrument` (default `io.branch.gptdriver.tests.LinkCreationDeterministicTest`), and pulls `branchlogs.txt` from the TestBed. CI runs the same steps in `.github/workflows/sdk-l1-validation.yml`.

## Layout

```
Branch-SDK-GPTDriver/
├── build.gradle.kts
└── src/main/java/io/branch/gptdriver/tests/
    └── LinkCreationDeterministicTest.kt
```
