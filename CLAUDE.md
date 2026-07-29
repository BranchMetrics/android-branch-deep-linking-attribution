# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This is the **Branch Android SDK** for deep linking and attribution. Production code is a single Android library module, `:Branch-SDK`, published to Maven Central as `io.branch.sdk.android:library`. Package root: `io.branch.referral`.

## Modules

- **`Branch-SDK`** — the published SDK library (`com.android.library`). All production code lives under `Branch-SDK/src/main/java/io/branch/referral/`.
- **`Branch-SDK-TestBed`** — a sample/demo app (`com.android.application`, `io.branch.branchandroidtestbed`) that depends on `:Branch-SDK`. Used for manual testing and as the target app for E2E tests.
- **`Branch-SDK-GPTDriver`** — a `com.android.test` module (MobileBoost/GPTDriver hybrid E2E tests) whose `targetProjectPath` is `:Branch-SDK-TestBed`. Deterministic Espresso first, AI-assisted validation only when Espresso can't express the intent. Requires a `MOBILEBOOST_API_KEY` (see `local.properties.example` / `Branch-SDK-GPTDriver/README.md`).

## Source layout

Most code is in `io.branch.referral`, but not all — check these sibling packages under `Branch-SDK/src/main/java/io/branch/` before assuming a class lives in `referral/`:

- **`referral/`** — the SDK core: `Branch`, `PrefHelper`, `ServerRequest*` + `ServerRequestQueue`, device/tracking/config, link & share builders. Sub-dirs: `network/` (`BranchRemoteInterface` + `…UrlConnection` HTTP impl), `util/` (`BranchEvent`, `BRANCH_STANDARD_EVENT`, `LinkProperties`, `CommerceEvent`, content-metadata types), `validators/` (integration & deep-link diagnostics), `QRCode/`.
- **`indexing/`** — `BranchUniversalObject` (the BUO content model used for link creation and tracking).
- **`coroutines/`** — Kotlin coroutine entry points for async fetches: `AdvertisingIds`, `DeviceSignals`, `InstallReferrers`.
- **`data/`** — `InstallReferrerResult` (result model for the referrer fetch).
- **`interfaces/`** — public callback interfaces (e.g. `IBranchLoggingCallbacks`).
- **`receivers/`** — `SharingBroadcastReceiver` (captures the chosen app from the system share sheet).

## Where to make changes

| Task | Start here |
| --- | --- |
| Session/init flow, deep-link callbacks, intent parsing | `Branch.java` (`sessionBuilder`, `initializeSession`, `registerAppInit`, `readAndStripParam`) |
| A new API request type or changing request bodies | subclass `ServerRequest`; wire dispatch/gating in `ServerRequestQueue.java`; add path to `Defines.RequestPath` |
| Persisted state / new SharedPreferences key | `PrefHelper.java` (add `KEY_*` + typed accessors) |
| Wire-format field names / enums / endpoints | `Defines.java` (`Jsonkey`, `RequestPath`, `IntentKeys`, `HeaderKey`) |
| Device/hardware signals on requests | `DeviceInfo.java`, `SystemObserver.java`, `coroutines/DeviceSignals.kt` |
| Ad ID (GAID/Huawei/etc.) fetch | `coroutines/AdvertisingIds.kt` |
| Install-referrer fetch | `coroutines/InstallReferrers.kt`, `data/InstallReferrerResult.kt`, `AppStoreReferrer.java`, `BranchPreinstall.java` |
| Link creation (short/long URLs) | `BranchShortLinkBuilder.java`, `BranchUrlBuilder.java`, `BranchLinkData.java`; content model in `indexing/BranchUniversalObject.java`, `util/LinkProperties.java` |
| Sharing / share sheet | `BranchShareSheetBuilder.java`, `ShareLinkManager.java`, `NativeShareLinkManager.java`, `receivers/SharingBroadcastReceiver.kt` |
| Custom analytics / commerce events | `util/BranchEvent.java`, `util/BRANCH_STANDARD_EVENT.java`, `util/CommerceEvent.java` |
| Tracking-disabled / consent / DMA | `TrackingController.java`, plus the DMA/consent keys in `PrefHelper.java` |
| `branch.json` / config flags | `BranchJsonConfig.java`, `BranchConfigurationController.kt`, `BranchUtil.java` (key resolution) |
| HTTP transport / retries / timeouts | `network/BranchRemoteInterfaceUrlConnection.java` (impl), `network/BranchRemoteInterface.java` (abstract) |
| QR codes | `QRCode/BranchQRCode.java`, `QRCode/ServerRequestCreateQRCode.java` |
| Integration / deep-link diagnostics | `validators/IntegrationValidator.java`, `validators/DeepLinkRoutingValidator.java` |
| Logging | `BranchLogger.kt`, `interfaces/IBranchLoggingCallbacks.java` |

## Build, test, lint

Toolchain: **Java 17** for Gradle (CI uses Corretto/Temurin 17), `compileSdk 34`, `minSdk 21`. Copy `local.properties.example` → `local.properties` (Android Studio fills in `sdk.dir`).

```bash
# JVM unit tests (fast, no emulator) — the everyday loop
./gradlew :Branch-SDK:testDebugUnitTest

# A single unit test class or method
./gradlew :Branch-SDK:testDebugUnitTest --tests "io.branch.referral.BranchConfigurationControllerTest"
./gradlew :Branch-SDK:testDebugUnitTest --tests "*BranchConfigurationControllerTest.someMethod"

# Instrumented tests (need a connected device/emulator) — the bulk of the suite
./gradlew :Branch-SDK:connectedDebugAndroidTest
# A single instrumented class
./gradlew :Branch-SDK:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.branch.referral.BranchApiTests

# Coverage (JaCoCo, aggregates unit + instrumented)
./gradlew :Branch-SDK:jacocoTestReport

# Lint (SDK module has abortOnError = false — lint won't fail the build)
./gradlew :Branch-SDK:lint
```

**Most of the test suite is instrumented** (`Branch-SDK/src/androidTest/`), so it requires an emulator/device. Only three files are JVM unit tests (`Branch-SDK/src/test/`): `BranchConfigurationControllerTest.kt`, `InstallReferrerResultTests.kt`, `BranchPartnerParametersTest.java`. Test tasks are configured with `test-retry` (up to 3 retries) — flakes get retried in CI.

### CI

- **`unit-and-instrumented-tests-action.yml`** — runs `testDebugUnitTest` + `connectedDebugAndroidTest` on API 21 and 34, plus `jacocoTestReport` → Codecov.
- **`sdk-l1-validation.yml`** — the "Layer 1" PR gate. Builds TestBed + GPTDriver APKs, runs an instrumented test on the emulator, pulls `branchlogs.txt`, and asserts required device/SDK fields are on the wire via `scripts/validate_l1_logs.py`. To reproduce the wire check locally: `python3 scripts/validate_l1_logs.py path/to/branchlogs.txt` (validator self-tests: `python3 -m unittest scripts.test_validate_l1_logs -v`). Required-field lists live at the top of `validate_l1_logs.py`; the check is presence-only and scoped to `/v1/*`.

## Architecture

### Singleton wiring (`Branch.java`)

`Branch` is a process singleton (`branchReferral_`). Apps obtain it with `Branch.getAutoInstance(Context)`; `getInstance()` is a pure accessor that never creates. The private constructor `Branch(Context)` (~line 321) wires up the sub-systems, most held in `final` fields set once at construction:

- `prefHelper_` — `PrefHelper.getInstance(context)` (SharedPreferences-backed state)
- `requestQueue_` — `ServerRequestQueue.getInstance(context)` (the API request queue)
- `deviceInfo_` — device/hardware data for request bodies
- `trackingController` — tracking-disabled state
- `branchConfigurationController_` — resolved config flags
- `branchRemoteInterface_` — `BranchRemoteInterfaceUrlConnection` (non-final; swappable via `setBranchRemoteInterface`, and mocked in tests)
- `branchPluginSupport_`, `branchQRCodeCache_`, `linkCache_`

`initBranchSDK()` is the one-time constructor caller and early-returns if the singleton already exists. `shutDown()` (package-private, test-only) nulls the singleton and resets statics — tests rely on this to reset global state between runs.

### Session init flow (intent → callback)

Callers drive init through the fluent `Branch.sessionBuilder(activity).withCallback(...).init()`. The flow:

1. **Intent parsing** — `readAndStripParam(uri/intent, activity)` runs a chain of extractors (`extractBranchLinkFromIntentExtra`, `extractClickID`, `extractAppLink`, `extractExternalUriAndIntentExtras`, …) that write results into `PrefHelper` (link click id, app link, push identifier, external intent uri/extra, initial referrer). A Branch link in an intent is consumed exactly once, marked by the `BranchLinkUsed` intent extra.
2. **Request selection** — `getInstallOrOpenRequest()` picks `ServerRequestRegisterInstall` vs `ServerRequestRegisterOpen` based on `requestQueue_.hasUser()` (true iff a randomized bundle token is already persisted).
3. **Queueing with wait locks** — `registerAppInit()` sets state `INITIALISING` and force-inserts the init request at the front of the queue (or reuses an in-flight self-init request and transfers the callback, to preserve ordering). `initTasks()` attaches `PROCESS_WAIT_LOCK`s so the request won't fire until prerequisites resolve: `INTENT_PENDING_WAIT_LOCK` (until `onIntentReady` at `Activity.onResume`), `INSTALL_REFERRER_FETCH_WAIT_LOCK` (install only), `GAID_FETCH_WAIT_LOCK` (always), and `USER_SET_WAIT_LOCK` (when `withDelay` is used).
4. **Response → callback** — on success the init request stores returned params into `PrefHelper` as **session params** (latest) and **install params** (first-ever), then invokes `callback.onInitFinished(...)`.

`getLatestReferringParams()` reads `PrefHelper.getSessionParams()`; `getFirstReferringParams()` reads `PrefHelper.getInstallParams()`. Sync variants block on a `CountDownLatch` (≤2500 ms) and must be called off the UI thread.

**Init state machine:** `SessionState { UNINITIALISED, INITIALISING, INITIALISED }`, exposed via `getInitState()`. `INITIALISED` means the SDK has consumed the current intent and can send events.

**Intent lock:** by default the SDK waits for `Activity.onResume` (`intentState_` PENDING→READY via `onIntentReady`) so it captures the freshest intent (e.g. `onNewIntent` single-top). `bypassWaitingForIntent(true)` or `unlockPendingIntent()` are escape hatches. `removeSessionInitializationDelay()` clears the `withDelay` lock.

**Instant Deep Linking (IDL, off by default):** when enabled, `init()` fires the callback synchronously from cached intent params, then nulls the callback and lets the network init run for analytics only.

**Plugin deferral:** `deferInitForPluginRuntime` caches the session builder until `notifyNativeToInit()` is called (used by cross-platform plugins like React Native/Flutter).

### ServerRequest lifecycle and the queue

`ServerRequest` (abstract) is the base for every API call. It owns the POST/GET body (`params_`), a `Defines.RequestPath`, a set of wait locks (`locks_`), and a retry count. `setPost()` branches on API version: **V1** (`/v1/install`, `/v1/open`, `/v1/url`) puts device fields at the top level; **V2** (`/v2/event/…`, LATD) nests them under a `user_data` object. Subclasses: `ServerRequestInitSession` (→ `ServerRequestRegisterInstall`, `ServerRequestRegisterOpen`), `ServerRequestLogEvent` (V2), `ServerRequestCreateUrl`, `ServerRequestGetLATD`, plus the queue-only operations `QueueOperationSetIdentity` and `QueueOperationLogout` (these are enqueued but are not network requests).

**`ServerRequestQueue` is serialized, single-flight, and currently in-memory:**

- The backing store is `Collections.synchronizedList(new LinkedList<>())` guarded by a static `reqQueueLockObject` — **not** a `ConcurrentLinkedQueue`. Capacity `MAX_ITEMS = 25`; on overflow it drops index 1 (never the head).
- A `Semaphore(1)` (`serverSema_`) plus an `int networkCount_` flag enforce **one in-flight request at a time**. `processNextQueueItem()` peeks the head; if it has no pending wait locks it runs it on a `BranchPostTask` (a `BranchAsyncTask`) with a task-timeout latch. Completion resets `networkCount_` and re-posts `processNextQueueItem` on a main-thread `Handler` (avoids stack overflow on the timeout path).
- **Disk persistence is currently vestigial.** The constructor opens a SharedPreferences file `"BNC_Server_Request_Queue"` (constant `PREF_KEY = "BNCServerRequestQueue"`) but nothing reads or writes it after init — the queue does not survive a process restart. `toJSON`/`fromJSON` on `ServerRequest` are similarly marked "TODO: in-memory only." Don't assume queued requests are durable.

**Queue vs PrefHelper storage are separate SharedPreferences files.** PrefHelper uses `"branch_referral_shared_pref"`; the (unused) queue file is `"BNC_Server_Request_Queue"`. The queue reaches PrefHelper only via `Branch.getInstance().prefHelper_` for session values.

**Session gating:** a request other than install/logout/set-identity fails with `ERR_NO_SESSION` if there's no user. `requestNeedsSession()` exempts init, create-url, logout, and set-identity; everything else waits behind `SDK_INIT_WAIT_LOCK` until `INITIALISED`. On init success, the new `SessionID` / `RandomizedBundleToken` / `RandomizedDeviceToken` are written to PrefHelper **and** `updateAllRequestsInQueue()` rewrites those keys into every already-queued request's body.

### PrefHelper — persistent state (`PrefHelper.java`)

Singleton wrapper over the `"branch_referral_shared_pref"` SharedPreferences file with typed get/set accessors. String getters default to `NO_STRING_VALUE = "bnc_no_value"` — code distinguishes "unset" from "empty" using `hasPrefValue(...)` (see `isDMAParamsInitialized`, `isAttributionLevelInitialized`), so prefer that pattern over comparing against defaults. Stores: branch key + key **source**, randomized device/bundle tokens, identity, session params vs install params, link-click / app-link / push / external-intent identifiers, install & referrer data (GCLID self-expires after 30 days), consent/DMA flags, tracking state, and network tuning. In-memory-only (not persisted): request/install metadata, partner params, custom server/CDN URLs, logging + EU-endpoint flags.

**Branch key resolution precedence** (`BranchUtil.readBranchKey`): `branch.json` → manifest meta-data (`io.branch.sdk.BranchKey` / `.test`; test mode falls back to the live key) → string resources. Each source is persisted alongside a key-**source** string. `branch.json` is read by `BranchJsonConfig` (a separate singleton); `BranchConfigurationController` surfaces resolved config flags and delegates its own storage to `prefHelper_`.

## Non-obvious invariants

- **Request ordering is a guarantee, not a side effect.** Init is force-moved to the front; `setIdentity`/`logout` are queue operations specifically so they execute in enqueue order relative to other requests (see ChangeLog 5.20.0). Don't reorder or short-circuit queue handling casually.
- **`clearPrefOnBranchKeyChange()`** (triggered when `setBranchKey` sees a different key) does `prefsEditor_.clear()` but deliberately re-writes four values so an in-flight deep link survives: link-click id, link-click identifier, app link, push identifier.
- **Tracking-disabled** (`TrackingController`, or `setConsumerProtectionAttributionLevel(NONE)`) blocks network calls except deep-linking. Disabling clears session/link/referrer prefs but **not** identity or device/bundle tokens. Only requests whose `prepareExecuteWithoutTracking()` returns true are allowed through; init requests strip all PII and set `TrackingDisabled=true` in that mode. There is no `SKIP_QUEUE` mechanism — gating is entirely via `prepareExecuteWithoutTracking()` and the `ERR_BRANCH_TRACKING_DISABLED` checks.
- **One-time writes:** `KEY_ORIGINAL_INSTALL_TIME` is written once (when 0); enhanced-web-link UX type/load-time are consumed once then reset; `postInitClear` resets link/referrer/intent identifiers only when ≤1 init request remains queued.
- **Retry policy** (`onRequestFailed`): a request is dropped on HTTP 400–451, on `ERR_BRANCH_TRACKING_DISABLED`, when `!shouldRetryOnFail()`, or once `currentRetryCount` hits the no-connection retry max; otherwise it stays for replay. Init failure resets state to `UNINITIALISED`.

## Optional integrations

Ad-ID / install-referrer / billing / in-app-browser dependencies are `compileOnly` in `Branch-SDK/build.gradle` — the SDK works without any of them and detects them by reflection at runtime. When touching that code, guard every reference so the SDK still builds and runs when the optional dependency is absent (Google/Huawei/Samsung/Xiaomi install referrers, Play Billing, `androidx.browser`).
