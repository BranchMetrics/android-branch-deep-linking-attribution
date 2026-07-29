# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **This is the `6.0.0-beta.0` branch — an internal beta, not a stable release.** Breaking changes and clean API surfaces are acceptable here; **do not add backward-compat shims** unless explicitly asked. This branch has diverged substantially from `master`: request execution has been migrated from `AsyncTask` to Kotlin coroutines/channels, session state is now a sealed `BranchSessionState`, and link generation goes through `ModernLinkGenerator`. Descriptions of the SDK written against `master` (or the 5.x published docs) are frequently **stale here** — verify against source on this branch.
>
> Version note: the branch is `6.0.0-beta.0`, but `gradle.properties` `VERSION_NAME` is currently `5.20.999` (a pre-6.0 placeholder). Don't identify the branch by version string.

This is the **Branch Android SDK** (deep linking / attribution). Published module: `:Branch-SDK`, package root `io.branch.referral`. The rest of this file concentrates on what is **specific to the beta** and **where the code lives**, so research goes straight to the right file instead of grepping the tree.

## Orienting efficiently

Use the "Where to make changes" table and "Source layout" below to jump straight to the relevant file rather than grepping the whole tree. Two things carry most of the risk on this branch, so scope your reading around them: the **request queue subsystem** (`BranchRequestQueue.kt` / `BranchRequestQueueAdapter.kt` — note `ServerRequestQueue.java` is orphaned) and the **coroutine-migrated paths** (`ModernLinkGenerator`, `BranchSessionState*`, `BranchAsyncNetworkLayer`). `file:line` anchors here are locate-then-confirm hints — line numbers drift on a live branch. When a class is coroutine-based on this branch but Java/AsyncTask on master, trust the source here over any 5.x doc.

## Core architecture (shared with master)

This is the foundation the beta changes sit on top of. (If a `CLAUDE.md` exists on `master`, it has the fuller treatment; this is the self-contained summary.)

**Singleton wiring (`Branch.java`).** `Branch` is a process singleton (`branchReferral_`); apps obtain it via `Branch.getAutoInstance(Context)`, and `getInstance()` is a pure accessor that never creates. The private constructor wires the sub-systems, most held in `final` fields set once: `prefHelper_` (persistent state), `requestQueue_` (API queue — on this branch a `BranchRequestQueueAdapter`), `deviceInfo_`, `trackingController`, `branchConfigurationController_`, `branchRemoteInterface_` (swappable/mocked in tests). `shutDown()` (package-private, test-only) nulls the singleton and resets statics — tests rely on it to reset global state.

**Session init entry points — in flux.** The legacy entry `Branch.sessionBuilder(activity).withCallback(...).init()` (the `InitSessionBuilder`, `Branch.java:2143+`) is **being retired** — it's already wrapped as legacy in `modernization/wrappers/PreservedBranchApi.kt` and is slated for deletion. It still works today (TestBed's `MainActivity` calls it), so you'll see it in existing code, but do **not** build new init logic around it. The replacement is the reactive path in `modernization/core/ModernBranchCore.kt`: `ModernBranchCore` exposes manager interfaces and `SessionManager.initSession(activity): Result<BranchSession>` as a `suspend` function, with state surfaced via `StateFlow<BranchSession?>`. When editing init, confirm which path the caller is on before changing behavior.

**Session init flow (intent → callback), the durable mechanism.** Regardless of entry point, init ultimately drives this sequence:
1. **Intent parsing** — `readAndStripParam()` runs extractors (`extractBranchLinkFromIntentExtra`, `extractClickID`, `extractAppLink`, `extractExternalUriAndIntentExtras`) that write results into `PrefHelper`. A Branch link is consumed exactly once, marked by the `BranchLinkUsed` intent extra.
2. **Request selection** — `getInstallOrOpenRequest()` picks `ServerRequestRegisterInstall` vs `ServerRequestRegisterOpen` based on whether a randomized bundle token is already persisted (`hasUser()`).
3. **Queue + wait locks** — `registerAppInit()` sets state `INITIALISING` and force-inserts init at the front of the queue; `initTasks()` attaches `PROCESS_WAIT_LOCK`s so it won't fire until prerequisites resolve: `INTENT_PENDING_WAIT_LOCK` (until `onIntentReady` at `Activity.onResume`), `INSTALL_REFERRER_FETCH_WAIT_LOCK` (install only), `GAID_FETCH_WAIT_LOCK` (always), `USER_SET_WAIT_LOCK` (when the delay option is used — see the known bug below).
4. **Response → callback** — on success the init request stores returned params into `PrefHelper` as **session params** (latest) and **install params** (first-ever), then invokes the caller's init callback. Init-state machine: `UNINITIALISED → INITIALISING → INITIALISED` (`getInitState()`); `INITIALISED` means the current intent is consumed and events can send.

`getLatestReferringParams()` reads session params; `getFirstReferringParams()` reads install params. Sync variants block on a latch (≤2500 ms) and must be called off the UI thread.

**`ServerRequest` lifecycle.** `ServerRequest` (abstract) is the base for every API call — it owns the POST/GET body (`params_`), a `Defines.RequestPath`, its wait-lock set, and a retry count. `setPost()` branches on API version: **V1** (`v1/install`, `v1/open`, `v1/url`) puts device fields at top level; **V2** (`v2/event/*`, LATD) nests them under `user_data`. Subclasses: `ServerRequestInitSession` → `ServerRequestRegisterInstall` / `ServerRequestRegisterOpen`; `ServerRequestLogEvent` (V2); `ServerRequestCreateUrl`; `ServerRequestGetLATD`; plus the client-only queue operations `QueueOperationSetIdentity` / `QueueOperationLogout` (enqueued for ordering, but skip the network).

**Session gating rule** (the logic duplicated across the queue classes — the actual rule): a request that is **not** init / create-url / logout / set-identity needs a valid session before it can send — i.e. session ID + randomized device token + randomized bundle token, all populated only by a successful init response. Everything else waits behind `SDK_INIT_WAIT_LOCK` until `INITIALISED`. On init success, new session/token values are written to `PrefHelper` **and** propagated into every already-queued request's body.

**`PrefHelper` — persistent state.** Singleton wrapper over the `"branch_referral_shared_pref"` SharedPreferences file with typed accessors. String getters default to `NO_STRING_VALUE = "bnc_no_value"`, so code distinguishes "unset" from "empty" with `hasPrefValue(...)` — prefer that over comparing to defaults. Stores branch key + key **source**, randomized device/bundle tokens, identity, session-vs-install params, link/app-link/push identifiers, install & referrer data, consent/DMA flags, tracking state, network tuning. **Branch-key resolution precedence:** `branch.json` → manifest meta-data (`io.branch.sdk.BranchKey`/`.test`) → string resources; each source is persisted alongside a key-source string.

**Tracking-disabled** (`TrackingController`, or attribution level `NONE`) blocks network calls except deep-linking; disabling clears session/link/referrer prefs but **not** identity or device/bundle tokens. Only requests whose `prepareExecuteWithoutTracking()` returns true pass through.

## Beta-specific architecture (what differs from master)

### Request queue — the most-changed subsystem

Live request path on this branch:

```
Branch.java  (requestQueue_ : BranchRequestQueueAdapter)         Branch.java:217, assigned :328
  → BranchRequestQueueAdapter.kt   (handleNewRequest, unlockProcessWait; reimplements session gating)
    → BranchRequestQueue.kt        (synchronized List + Channel<Unit> wake-up trigger; single consumer coroutine)
```

Facts to know before touching it (all verifiable in source on this branch):

- **`ServerRequestQueue.java` is orphaned.** It is not on the live path — `Branch.java` uses `BranchRequestQueueAdapter`, and the only remaining references to `ServerRequestQueue` in `src/main` are in *comments*. It is kept compiling solely for **androidTest** consumers (`ServerRequestTests`, `BranchCPIDTest`, `BranchEventTest`, `BranchPreinstallFileTest`, `BillingGooglePlayTests`, `BranchTestRequestUtil`, `BranchTest`). Do not add new production code against it.
- **Storage is `Collections.synchronizedList(...)`** (`BranchRequestQueue.kt:109`); the `Channel<Unit>` (`:110`) is only a wake-up signal, not the queue. There is **no `Mutex`/`Semaphore`** — master's single-in-flight `Semaphore(1)` guarantee is not enforced here; serialization is incidental (one consumer draining the FIFO head).
- **Wait locks are still a plain `HashSet<PROCESS_WAIT_LOCK>`** on each `ServerRequest` (`ServerRequest.java:52-57,94`), and the consumer **polls** for locks to clear (`RETRY_DELAY_MS = 100L`, `BranchRequestQueue.kt:72`) rather than being woken on release. Retry/timeout uses `RequestRetryInfo` on the monotonic clock, with time-based force-unlock heuristics `tryResolveStuckLocks` / `tryResolveStuckSdkInitLock` / `tryResolveStuckUserAgentLock` (`:566+`). Constants: `MAX_RETRY_ATTEMPTS = 5`, `REQUEST_TIMEOUT_MS = 30_000L` (`:70-71`).
- **"Needs a session" logic is duplicated** across `ServerRequestQueue.java`, `BranchRequestQueue.kt`, and `BranchRequestQueueAdapter.kt`, and the copies have diverged — reconcile all of them if you change gating.

**Invariants any queue change MUST preserve** (from beta bug-fix history — see `git log`):

- **EMT-3860** (`eb040576`): `onIntentReady()` must read/persist intent params (`readAndStripParam`) **before** releasing `INTENT_PENDING_WAIT_LOCK`, or cold-start deep-link attribution is dropped. This was broken once already.
- **EMT-3859** (`e7e46854`): the retry-count ceiling must apply **only** to non-lock-waiting requests; lock-waiters may fail only via the 30s timeout. `shouldFailRequest()` (`BranchRequestQueue.kt:376+`) encodes this — otherwise ~500ms (5×100ms) force-fails waiting requests.
- Timing/ordering bugs here **do not reliably surface in CI** — add regression tests reproducing the exact scenario, not just unit tests of the mechanism in isolation.

**Known live bug:** `USER_SET_WAIT_LOCK` is added by `withDelay()` (`Branch.java:1394`) but has **no removal site** anywhere in `src/main`, and `removeSessionInitializationDelay()` (its historical owner) no longer exists on this branch (`git grep` → 0 hits). No stuck-lock resolver handles it either. Net effect: a delayed `init()` hangs until the 30s timeout, then fails — **`withDelay()` is effectively broken here.** A fix needs to give the lock a real owner or model the delay without a dangling lock.

### OPEN and deep-link request paths (new on the beta)

Two request paths were added alongside the init flow — know them before touching OPEN/attribution behavior:

- **`sendOpen()` / `sendOpen(JSONObject responseData)`** (`Branch.java:2414`, `:2428`) enqueue a `RequestOpen` (`v1/open`, `coroutines/RequestOpen.kt`) — but **only when the consumer-protection attribution level is not `NONE`** (both check `getConsumerProtectionAttributionLevel()` first). Callers: (1) `setConsumerProtectionAttributionLevel(level)` when re-enabling attribution, (2) `BranchProcessLifecycleObserver.onStart` — foreground OPENs are now driven by **AndroidX `ProcessLifecycleOwner`** (SDK-2463), which fires only on real process foreground, not on config-change recreation (fold/rotate/multi-window), removing duplicate OPENs by construction, and (3) after a successful `RequestDeepLink`.
- **`requestDeepLinkData(uri, callback)`** (`Branch.java:2726`, public) manually resolves a URI: it builds a `RequestDeepLink` (`coroutines/RequestDeepLink.kt`, a `ServerRequestInitSession` subclass) hitting the **new `v3/deeplink`** endpoint (`Defines.RequestPath.Deeplink`) and routes it through `requestQueue_.handleNewRequest(...)`. It maps `link_click_id` / app-link-url / scheme-uri into the POST, and on success writes `sessionParams`, fires the callback with `latestReferringParams`, and (if attribution ≠ NONE) chains a `sendOpen(response)`. Coroutine-friendly; intended to be called from a `LifecycleScope`.

### Other beta subsystems

- **`ModernLinkGenerator.kt`** — coroutine link-creation path (replaces the AsyncTask pattern). Establishes the branch's async idiom: per-class `CoroutineScope(SupervisorJob() + Dispatchers.IO)` + `shutdown()`, `withTimeout` for cancellation, a single `runBlocking` / `scope.launch{ … withContext(Main){callback} }` bridge for Java callers, sealed exception mapping back to `BranchError`. **Reuse this idiom** for new async code.
- **`BranchSessionState*.kt`** — sealed session state (`Uninitialized/Initializing/Initialized/Resetting/Failed`) in a `MutableStateFlow` (`BranchSessionStateManager`, `BranchSessionStateProvider`). The StateFlow is currently consumed via `.value` / `instanceof` / manual listeners, **not** reactively collected.
- **`network/BranchAsyncNetworkLayer.kt`** — coroutine network layer with non-blocking `delay()` backoff.
- **Logging:** `BranchLogger` (levels `ERROR..VERBOSE`, no separate trace channel). Per EMT-3864 (`604fd770`), route internal/queue trace through the existing `.v()` level — **do not add a second logging gate.** Trace lines use stable `key=value` prefixes.
- **Public API is gated:** a CI check diffs the public API against the last 5.x release (EMT-3877, `bf422f32`). Intentional API changes are expected on the beta — update the baseline deliberately rather than working around the gate.
- **Removed / restored APIs (check `git log` before assuming an API's state):** `reInit()` / `isReInitializing` were removed from `InitSessionBuilder` (EMT-3883). Some 5.x source-compat aliases were deliberately restored earlier in the beta (no-arg `Branch.logout()`, a relocated LATD listener alias, synchronous deep-link param getters).

## Source layout

Code spans several `io/branch/*` packages, not just `referral/`:

- **`referral/`** — SDK core: `Branch`, `PrefHelper`, `ServerRequest*`, the coroutine queue (`BranchRequestQueue.kt`, `BranchRequestQueueAdapter.kt`), `ModernLinkGenerator.kt`, `BranchSessionState*.kt`, device/tracking/config. Sub-dirs: `network/` (HTTP incl. `BranchAsyncNetworkLayer.kt`), `util/` (`BranchEvent`, `LinkProperties`, `CommerceEvent`, content-metadata), `validators/`, `QRCode/`.
- **`indexing/`** — `BranchUniversalObject` (BUO content model).
- **`coroutines/`** — async fetch entry points (`AdvertisingIds`, `DeviceSignals`, `InstallReferrers`) **and** the newer coroutine request classes `RequestOpen` (`v1/open`) and `RequestDeepLink` (`v3/deeplink`).
- **`observers/`** — `BranchProcessLifecycleObserver` (process-level foreground detection → OPEN, SDK-2463).
- **`data/`** — `InstallReferrerResult`.
- **`interfaces/`** — public callback interfaces (e.g. `IBranchLoggingCallbacks`).
- **`receivers/`** — `SharingBroadcastReceiver`.

### Where to make changes

| Task | Start here |
| --- | --- |
| Session/init flow, deep-link callbacks, intent parsing | mechanism in `Branch.java` (`initializeSession`, `registerAppInit`, `readAndStripParam`, `onIntentReady`); new entry point in `modernization/core/ModernBranchCore.kt` (`SessionManager.initSession`). Legacy `sessionBuilder`/`InitSessionBuilder` is being retired — don't extend it |
| OPEN / foreground re-open / attribution gating | `Branch.sendOpen(...)` + `observers/BranchProcessLifecycleObserver.kt`; request in `coroutines/RequestOpen.kt` |
| Manual deep-link resolution (`v3/deeplink`) | `Branch.requestDeepLinkData(...)` + `coroutines/RequestDeepLink.kt` |
| Request queueing / wait-lock / retry behavior | `BranchRequestQueue.kt` + `BranchRequestQueueAdapter.kt` (**not** `ServerRequestQueue.java` — orphaned) |
| A new API request type or changing request bodies | subclass `ServerRequest`; route in the adapter/queue; add path to `Defines.RequestPath` |
| Persisted state / new SharedPreferences key | `PrefHelper.java` (`KEY_*` + typed accessors; distinguish unset via `hasPrefValue`) |
| Wire-format field names / enums / endpoints | `Defines.java` (`Jsonkey`, `RequestPath`, `IntentKeys`, `HeaderKey`) |
| Session state / lifecycle | `BranchSessionStateManager.kt`, `BranchSessionState.kt` |
| New async / coroutine work | follow the `ModernLinkGenerator.kt` idiom |
| Link creation | `ModernLinkGenerator.kt`, `BranchShortLinkBuilder.java`, `BranchUrlBuilder.java`; content model in `indexing/BranchUniversalObject.java`, `util/LinkProperties.java` |
| Custom / commerce events | `util/BranchEvent.java`, `util/BRANCH_STANDARD_EVENT.java`, `util/CommerceEvent.java` |
| Ad ID / install-referrer / device signals | `coroutines/AdvertisingIds.kt`, `coroutines/InstallReferrers.kt`, `coroutines/DeviceSignals.kt`, `DeviceInfo.java` |
| Tracking-disabled / consent / DMA | `TrackingController.java` + DMA/consent keys in `PrefHelper.java` |
| `branch.json` / config flags | `BranchJsonConfig.java`, `BranchConfigurationController.kt`, `BranchUtil.java` (key resolution) |
| HTTP transport / retries / timeouts | `network/BranchRemoteInterfaceUrlConnection.java`, `network/BranchAsyncNetworkLayer.kt` |
| Logging | `BranchLogger.kt` (route internal trace through `.v()`) |

## Build, test, lint

Toolchain: **Java 17** for Gradle, `compileSdk 34`, `minSdk 21`, **Kotlin 1.6.21**, coroutines **1.6.4** (pin to 1.6.x APIs; do not bump Kotlin/minSdk). Copy `local.properties.example` → `local.properties`.

```bash
# JVM unit tests (Robolectric; base BranchTestBase.kt) — the everyday loop
./gradlew :Branch-SDK:testDebugUnitTest
./gradlew :Branch-SDK:testDebugUnitTest --tests "io.branch.referral.SomeTestClass"

# Instrumented tests (emulator/device; base BranchTest.java + MockRemoteInterface)
./gradlew :Branch-SDK:connectedDebugAndroidTest
./gradlew :Branch-SDK:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.branch.referral.BranchApiTests

# Coverage
./gradlew :Branch-SDK:jacocoTestReport
```

**Two test source sets under `io.branch.referral`:** `src/test/` (Robolectric, `UnconfinedTestDispatcher`, virtual-clock via `kotlinx-coroutines-test` already on the classpath — see `ModernLinkGeneratorTest`) and `src/androidTest/` (emulator, `Branch.shutDown()` + `clearSharedPrefs` in `@Before`, `MockRemoteInterface`). Changes on this branch generally want tests in **both**. Note: **`Branch.getTestInstance` does not exist** here — instrumented tests use `Branch.getInstance()` + `setBranchRemoteInterface(new MockRemoteInterface())`. Several queue unit tests reflect on private internals (`RequestRetryInfo`, `shouldFailRequest`, `tryResolveStuck*`); changing those internals means rewriting the tests, not just leaving them.

### Modules & CI

- **`:Branch-SDK`** (library), **`:Branch-SDK-TestBed`** (sample app, `io.branch.branchandroidtestbed`), **`:Branch-SDK-GPTDriver`** (`com.android.test` MobileBoost/GPTDriver E2E, targets TestBed; needs `MOBILEBOOST_API_KEY`).
- CI: `unit-and-instrumented-tests-action.yml` (unit + instrumented on API 21/34 + JaCoCo→Codecov); `sdk-l1-validation.yml` (PR gate — builds TestBed+GPTDriver, runs an instrumented test, validates on-wire `/v1/*` fields via `scripts/validate_l1_logs.py`; local: `python3 scripts/validate_l1_logs.py branchlogs.txt`). A public-API diff gate (EMT-3877) runs against the last 5.x release.

## Optional integrations

Ad-ID / install-referrer / billing / in-app-browser deps are `compileOnly` — the SDK works without them and detects them by reflection. Guard every reference so the SDK still builds and runs when the optional dependency is absent.
