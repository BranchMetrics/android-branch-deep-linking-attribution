# CLAUDE.md

Branch Android SDK. One library module, `:Branch-SDK`, package root `io.branch.referral`.

**What the SDK does.** It creates and resolves Branch links, and it attributes app activity to them. When a link is tapped, the SDK reports the app launch to the Branch API and gets back the link's data, so the app can route the user to the right content and Branch can credit the campaign that drove them. The first launch after an app is installed is an **install**; every later launch is an **open**. That distinction runs through the whole codebase. Integrator-facing docs: https://help.branch.io/developers-hub/docs/android-sdk-overview

**You are on `6.0.0-beta.0`, an internal beta, not a stable release.** Breaking changes and clean API surfaces are fine here. Do not add backward-compatibility shims unless asked.

This branch has diverged substantially from `master`: request execution moved from AsyncTask to Kotlin coroutines, session state is a sealed `BranchSessionState`, link generation goes through `ModernLinkGenerator`, and there are new `v3` OPEN and deeplink request paths. **Anything written against `master` or the published 5.x docs is frequently stale here**, and so is the branch's own older documentation. Verify against source on this branch.

`gradle.properties` says `VERSION_NAME=5.20.999`, a pre-6.0 placeholder. Do not identify the branch by its version string.

## Read on demand

Not loaded automatically. Open the one that matches the task.

| File | Read it when |
| --- | --- |
| `docs/internal/code-map.md` | you need to find where something lives |
| `docs/internal/architecture.md` | you are touching init, the queue, OPEN/attribution, or persisted state (opens with a glossary) |
| `docs/internal/beta-deltas.md` | you need the long form of a trap above, the queue internals, or what this branch changed versus `master` |
| `docs/internal/testing.md` | you are writing or running tests, or a CI check failed |
| `docs/internal/working-agreements.md` | you are opening a PR or reviewing one |

## First hour

Java 17 for Gradle, `compileSdk 34`, `minSdk 21` (the `:Branch-SDK-GPTDriver` module sets 24). Kotlin 1.6.21, coroutines 1.6.4. **Pin to 1.6.x APIs. Do not bump Kotlin, coroutines, or minSdk.**

```bash
cp local.properties.example local.properties   # then set sdk.dir, or export ANDROID_HOME
./gradlew :Branch-SDK-TestBed:installDebug     # the sample app, on a device or emulator
```

**The TestBed needs no Branch account.** It ships a test key in `Branch-SDK-TestBed/src/main/AndroidManifest.xml` with `io.branch.sdk.TestMode` set true. Turn on wire logging with `Branch.enableLogging()` to read what actually goes to the API.

## Commands

```bash
./gradlew :Branch-SDK:testDebugUnitTest          # JVM plus Robolectric, no emulator
./gradlew :Branch-SDK:connectedDebugAndroidTest  # instrumented, needs a device
./gradlew :Branch-SDK:assembleDebug              # build the AAR
```

Both test source sets are substantial on this branch, unlike on `master`. Detail: `docs/internal/testing.md`.

## Non-obvious behavior

- **`ServerRequestQueue.java` is dead code that still compiles, and grep will send you to it first.** The live path is `Branch.requestQueue_`, a `BranchRequestQueueAdapter`, delegating to `BranchRequestQueue.kt`. The old class survives only because instrumented tests still construct it. Edit it and your change compiles, the unit suite passes, and nothing happens at runtime.
- **`getInitState()` returns a sealed `BranchSessionState`**, not the legacy `SESSION_STATE` enum, which still exists in source. `getInitState() == SESSION_STATE.INITIALISED` looks valid and does not compile. Check with `instanceof`.
- **`withDelay()` is broken here.** `USER_SET_WAIT_LOCK` is added but never removed anywhere in `src/main`, and the stuck-lock resolver covers the other five locks and not this one. A delayed `init()` hangs until the 30s timeout, then fails. A fix needs to give the lock a real owner, or model the delay without a dangling lock.
- **"Needs a session" gating is duplicated** across `ServerRequestQueue.java`, `BranchRequestQueue.kt`, and `BranchRequestQueueAdapter.kt`, and the copies have diverged. Reconcile all of them if you change gating.
- **The legacy `sessionBuilder(...).init()` entry point is being retired.** It still works and the TestBed still calls it, but do not build new init logic on it. The replacement is `modernization/core/ModernBranchCore.kt`.
- **New async work follows the `ModernLinkGenerator.kt` idiom**: a per-class `CoroutineScope(SupervisorJob() + Dispatchers.IO)` with a `shutdown()`, `withTimeout` for cancellation, one bridge for Java callers, and sealed exceptions mapped back to `BranchError`.
- **Route internal trace through `BranchLogger.v()`.** A second logging gate silently swallows trace when the two gates disagree.
- **Optional integrations are `compileOnly` and resolved by reflection.** Guard every reference, or the SDK crashes in any app that skipped the optional dependency while your build and tests stay green.

## Working agreements

- Never commit to the beta branch directly. Branch, open a PR, get CI green.
- The PR title references the ticket, and the key is yours, not a teammate's. Jira auto-links whatever key it sees.
- **This branch has no `.github/CODEOWNERS`**, so nothing requests a reviewer for you. Request `@BranchMetrics/sdk-maintainers` explicitly and verify with `gh pr view <n> --json reviewRequests`.
- Every user-visible change gets a `ChangeLog.md` entry under the new version heading.
- **Comments in public API are minimal**: what the function does and what the parameters mean. Nothing else.
- Keep changes surgical, so the diff stays reviewable.

Details: `docs/internal/working-agreements.md`.
