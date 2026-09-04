# Architecture (6.0.0-beta.0)

> Contributor-facing notes for work on this branch. Integration docs live at https://help.branch.io/developers-hub/docs/android-sdk-overview
> Last updated 2026-09-03, against `6.0.0-beta.0` at 88b3bf28.

How the SDK behaves at runtime on this branch, and the invariants a change must not break. Read `code-map.md` first if you only need to locate a file.

## Terms

| Term | What it means |
| --- | --- |
| **install** / **open** | the first app launch after installation, versus every later launch. The choice decides whether a new user is tied back to the link that brought them |
| **OPEN** (as a noun) | a request to `v3/events/open` reporting a foreground launch for attribution. Distinct from the legacy `v1/open` `RegisterOpen` path, which still exists |
| **randomized bundle token** / **randomized device token** | opaque identifiers the Branch API assigns on a successful init and the SDK persists. The bundle token is per app install, the device token per device |
| **session params** / **install params** | the link data returned by init. Session params are from the most recent init; install params are frozen from the first-ever one |
| **wait lock** | a `PROCESS_WAIT_LOCK` on a queued request. While any lock is attached the queue will not send that request |
| **LATD** | Last Attributed Touch Data, served by `ServerRequestGetLATD` |
| **DMA** | the EU Digital Markets Act consent parameters |
| **consumer-protection attribution level** | the setting that decides how much the SDK may collect and send. `NONE` disables attribution while leaving deep linking working, and gates every OPEN on this branch |

## Where this is going

The 6.0 line replaces AsyncTask with Kotlin coroutines, models session state as a sealed `BranchSessionState`, and moves link generation to `ModernLinkGenerator`. The `v3` endpoints (`v3/events/open`, `v3/deeplink`) are new here.

The direction is away from session state as an asserted flag. `SDK_INIT_WAIT_LOCK` and `BranchSessionState` are the older model and are where the debt now sits. New code should not introduce a latch, one-shot boolean, or hold list whose job is to answer "is the session ready". Derived queue state, meaning asking the queue what it currently holds, is the sanctioned way to express ordering.

## Singleton wiring

`Branch` is a process singleton (`branchReferral_`). Apps create it with `Branch.initialize(Context, BranchConfiguration)`; `getInstance()` is a pure accessor that never creates.

The private constructor wires the sub-systems, most into `final` fields set once: `prefHelper_` (persistent state), `requestQueue_` (a `BranchRequestQueueAdapter` on this branch, `Branch.java:217`, assigned `:328`), `deviceInfo_`, `trackingController`, `branchConfigurationController_`, and `branchRemoteInterface_` (swappable, which is how tests mock the network).

`shutDown()` is package-private and test-only. It nulls the singleton and resets statics, and the instrumented suite depends on it between runs.

## Session init entry points, in flux

The legacy entry `Branch.sessionBuilder(activity).withCallback(...).init()` (the `InitSessionBuilder`, `Branch.java:2143+`) is **being retired**. It is already wrapped as legacy in `modernization/wrappers/PreservedBranchApi.kt` and is slated for deletion. It still works today, and the TestBed's `MainActivity` calls it, so you will see it in existing code. Do not build new init logic around it.

The replacement is `modernization/core/ModernBranchCore.kt`: `ModernBranchCore` exposes manager interfaces, with `SessionManager.initSession(activity): Result<BranchSession>` as a `suspend` function and state surfaced through a `StateFlow<BranchSession?>`. When editing init, confirm which path the caller is on before changing behavior.

## Session init flow, the durable mechanism

Regardless of entry point, init drives this sequence:

1. **Intent parsing.** `readAndStripParam()` runs extractors (`extractBranchLinkFromIntentExtra`, `extractClickID`, `extractAppLink`, `extractExternalUriAndIntentExtras`) that write results into `PrefHelper`. A Branch link is consumed exactly once, marked by the `BranchLinkUsed` intent extra.
2. **Request selection.** `getInstallOrOpenRequest()` picks `ServerRequestRegisterInstall` or `ServerRequestRegisterOpen` based on whether a randomized bundle token is already persisted (`hasUser()`).
3. **Queue plus wait locks.** `registerAppInit()` sets state `INITIALISING` and force-inserts init at the front of the queue. `initTasks()` attaches `PROCESS_WAIT_LOCK`s so it does not fire until prerequisites resolve: `INTENT_PENDING_WAIT_LOCK` (until `onIntentReady` at `Activity.onResume`), `INSTALL_REFERRER_FETCH_WAIT_LOCK` (install only), `GAID_FETCH_WAIT_LOCK` (always), and `USER_SET_WAIT_LOCK` when the delay option is used. That last lock currently has no removal site, which is the `withDelay()` trap listed in `CLAUDE.md`.
4. **Response to callback.** On success the init request stores returned params into `PrefHelper` as **session params** (latest) and **install params** (first-ever), then invokes the caller's init callback.

`getInitState()` (`Branch.java:1366`) returns the sealed `BranchSessionState`: `Uninitialized`, `Initializing`, `Initialized`, `Resetting`, `Failed`, checked with `instanceof`. The legacy `SESSION_STATE` enum still exists in source but is not what this method returns. `Initialized` means the current intent is consumed and events can send.

`getLatestReferringParams()` reads session params; `getFirstReferringParams()` reads install params. The synchronous variants block on a latch for up to 2500 ms and must be called off the UI thread.

## ServerRequest lifecycle

`ServerRequest` (abstract) is the base for every API call. It owns the POST/GET body (`params_`), a `Defines.RequestPath`, its wait-lock set, and a retry count.

`BRANCH_API_VERSION` has three values: `V1`, `V1_LATD`, `V2`. `setPost()` branches on `V1`: **V1** (`v1/install`, `v1/open`, `v1/url`) puts device fields at the top level, while **V2** (`v3/events/standard`, `v3/events/custom`) and **`V1_LATD`** nest them under `user_data`.

Subclasses: `ServerRequestInitSession` leading to `ServerRequestRegisterInstall` and `ServerRequestRegisterOpen`; `ServerRequestLogEvent` (V2); `ServerRequestCreateUrl`; `ServerRequestGetLATD`; plus the client-only queue operations `QueueOperationSetIdentity` and `QueueOperationLogout`, which are enqueued for ordering but skip the network.

**Session gating rule.** A request that is not init, create-url, logout, or set-identity needs a valid session before it can send: session id plus randomized device token plus randomized bundle token, all populated only by a successful init response. Everything else waits behind `SDK_INIT_WAIT_LOCK`. On init success, the new session and token values are written to `PrefHelper` and propagated into every already-queued request's body.

This logic is **duplicated across `ServerRequestQueue.java`, `BranchRequestQueue.kt`, and `BranchRequestQueueAdapter.kt`**, and the copies have diverged. Reconcile all three if you change gating.

## PrefHelper

Singleton wrapper over the `"branch_referral_shared_pref"` SharedPreferences file with typed accessors. String getters default to `NO_STRING_VALUE = "bnc_no_value"`, so code distinguishes unset from empty with `hasPrefValue(...)`. Prefer that over comparing to the default.

Stores: branch key plus key **source**, randomized device and bundle tokens, identity, session params versus install params, link / app-link / push identifiers, install and referrer data, consent and DMA flags, tracking state, network tuning.

**Branch key resolution precedence**: `branch.json`, then manifest meta-data (`io.branch.sdk.BranchKey` and `.test`), then string resources. Each source is persisted alongside a key-source string.

**Tracking-disabled** (`TrackingController`, or attribution level `NONE`) blocks network calls except deep linking. Disabling clears session, link, and referrer prefs but **not** identity or the device and bundle tokens. Only requests whose `prepareExecuteWithoutTracking()` returns true pass through.

## Invariants

These live in code shared with `master` and hold on this branch too. Breaking one usually fails silently, and usually not in CI.

- **Ordering is a contract.** Init is force-moved to the front. `setIdentity` and `logout` are queue operations precisely so they execute in enqueue order relative to other requests.
- **`clearPrefOnBranchKeyChange()`**, triggered when `setBranchKey` sees a different key, calls `prefsEditor_.clear()` but deliberately re-writes four values so an in-flight deep link survives: link-click id, link-click identifier, app link, push identifier.
- **One-time writes.** `KEY_ORIGINAL_INSTALL_TIME` is written once, when 0. Enhanced-web-link UX type and load-time are consumed once then reset. `postInitClear` resets link, referrer, and intent identifiers only when at most one init request remains queued.
- **Retry policy.** A request is dropped on HTTP 400 through 451, on `ERR_BRANCH_TRACKING_DISABLED`, when `shouldRetryOnFail()` is false, or once the retry ceiling is hit. Otherwise it stays for replay.

## Optional integrations

Ad ID, Huawei/Samsung/Xiaomi install referrer, billing, and in-app browser dependencies are `compileOnly`. The SDK works without any of them and detects them by reflection at runtime. Guard every reference so the SDK still builds and runs when the optional dependency is absent.
