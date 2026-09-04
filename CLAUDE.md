# CLAUDE.md

Branch Android SDK. One published library module, `:Branch-SDK`, released to Maven Central as `io.branch.sdk.android:library`. Package root `io.branch.referral`.

**What the SDK does.** It creates and resolves Branch links, and it attributes app activity to them. When a link is tapped, the SDK reports the app launch to the Branch API and gets back the link's data, so the app can route the user to the right content and Branch can credit the campaign that drove them. The first launch after an app is installed is an **install**; every later launch is an **open**. That distinction runs through the whole codebase, because install is the event that ties a new user back to the link they came from. Integrator-facing docs: https://help.branch.io/developers-hub/docs/android-sdk-overview

**You are on `master`, the 5.x release line.** The other live branch, `6.0.0-beta.0`, is a different architecture (coroutine request queue, sealed session state). Patterns from that branch do not belong here, and a fact from one line is not evidence about the other. Verify against the source on the branch you are on.

## Read on demand

Not loaded automatically. Open the one that matches the task.

| File | Read it when |
| --- | --- |
| `docs/internal/code-map.md` | you need to find where something lives |
| `docs/internal/architecture.md` | you are touching session init, the request queue, or persisted state (opens with a glossary) |
| `docs/internal/testing.md` | you are writing or running tests, or a CI check failed |
| `docs/internal/working-agreements.md` | you are opening a PR or reviewing one |

## First hour

Java 17 for Gradle, `compileSdk 34`, `minSdk 21` (the `:Branch-SDK-GPTDriver` module sets 24). Kotlin 1.6.21, coroutines 1.6.4.

```bash
cp local.properties.example local.properties   # then set sdk.dir, or export ANDROID_HOME
./gradlew :Branch-SDK-TestBed:installDebug     # the sample app, on a device or emulator
```

**The TestBed needs no Branch account.** It ships a test key in `Branch-SDK-TestBed/src/main/AndroidManifest.xml` with `io.branch.sdk.TestMode` set true, so you can create a link, tap it, and watch the SDK resolve it on first run. Turn on wire logging with `Branch.enableLogging()` to read what actually goes to the API; that log is what the CI wire gate validates.

## Commands

```bash
./gradlew :Branch-SDK:testDebugUnitTest          # JVM only, no device, covers three classes
./gradlew :Branch-SDK:connectedDebugAndroidTest  # the real suite, needs a device
./gradlew :Branch-SDK:assembleDebug              # build the AAR
```

Full test and CI detail, including the Layer 1 wire gate that runs on every PR into `master`: `docs/internal/testing.md`.

## Non-obvious behavior

- **`testDebugUnitTest` covers almost nothing.** Only three files are JVM tests; the real suite is instrumented and needs an emulator. A green unit run is not evidence your change works.
- **Request ordering is a guarantee the SDK makes to apps, not an accident.** Every call goes through one serial queue. Init is force-inserted at the head, because nothing else can be attributed until it returns, and `setIdentity` and `logout` are enqueued as no-network placeholders purely so they take effect in the order the app called them. Reorder or short-circuit the queue and events get attributed to the wrong user, with no test to tell you.
- **Several integrations are compiled against but not shipped** (ad IDs, Huawei/Samsung/Xiaomi referrers, billing, in-app browser). They are `compileOnly`, so the classes exist at compile time and may be absent at runtime in a host app. Touch that code without a reflection or try/catch guard and the SDK crashes in any app that skipped the optional dependency, while your build and tests stay green. The Google Play install referrer is the exception: a hard `implementation` dependency, always present.
- **The request queue is in-memory only.** It opens a SharedPreferences file that nothing ever reads or writes after construction. Anything queued when the process dies is silently lost.
- **`PrefHelper` string getters default to `NO_STRING_VALUE = "bnc_no_value"`.** Use `hasPrefValue(key)` to tell unset from empty; do not compare against the default.
- **`Branch.shutDown()` is package-private and test-only.** It nulls the singleton and resets statics, and the instrumented suite depends on that between runs.

## Working agreements

- Never commit to `master`. Branch, open a PR, get CI green.
- The PR title references the ticket, and the key is yours, not a teammate's. Jira auto-links whatever key it sees.
- Every PR must request `@BranchMetrics/sdk-maintainers`. It is not reliably requested for you; check with `gh pr view <n> --json reviewRequests`.
- Every user-visible change gets a `ChangeLog.md` entry under the new version heading.
- **Comments in public API are minimal**: what the function does and what the parameters mean. Nothing else.
- Keep changes surgical, so the diff stays reviewable: do not reformat, rename, or improve code your task did not require.

Details: `docs/internal/working-agreements.md`.
