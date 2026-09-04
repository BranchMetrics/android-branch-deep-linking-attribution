# Architecture (master)

> Contributor-facing notes for work on this branch. Integration docs live at https://help.branch.io/developers-hub/docs/android-sdk-overview
> Last updated 2026-09-03, against `master` at 827655ea.

How the SDK actually behaves at runtime, and the invariants a change must not break. Read `code-map.md` first if you only need to locate a file.

## Terms

| Term | What it means |
| --- | --- |
| **install** / **open** | the first app launch after installation, versus every later launch. `getInstallOrOpenRequest()` picks between them, and the choice decides whether a new user is tied back to the link that brought them |
| **randomized bundle token** / **randomized device token** | opaque identifiers the Branch API assigns on a successful init and the SDK persists. The bundle token is per app install, the device token per device. Their presence is what "the SDK has a user" means |
| **session params** / **install params** | the link data returned by init. Session params are from the most recent init; install params are frozen from the first-ever one |
| **wait lock** | a `PROCESS_WAIT_LOCK` on a queued request. While any lock is attached the queue will not send that request. Locks are how the SDK waits for the intent, the ad ID, and the install referrer without blocking a thread |
| **LATD** | Last Attributed Touch Data. The API that reports which touch a conversion is credited to, served by `ServerRequestGetLATD` |
| **DMA** | the EU Digital Markets Act consent parameters, carried on requests and stored in `PrefHelper` |
| **attribution level** | the consumer-protection setting that decides how much the SDK may collect and send. `NONE` disables attribution while leaving deep linking working |

## Where this is going

The 6.0 line, on `6.0.0-beta.0`, replaces AsyncTask with Kotlin coroutines, models session state as a sealed `BranchSessionState`, and moves link generation to `ModernLinkGenerator`.

That line is moving away from session state as an asserted flag. On `master` it is still live: `SessionState` and `SessionID` are load-bearing and documented below. Do not port beta patterns back here, and do not remove either one as cleanup.

## Singleton wiring

`Branch` is a process singleton (`branchReferral_`). Apps obtain it with `Branch.getAutoInstance(Context)`. `getInstance()` is a pure accessor that never creates, and returns `null` if the SDK was never set up.

The private constructor `Branch(Context)` wires the sub-systems, most into `final` fields set once:

| Field | What it is |
| --- | --- |
| `prefHelper_` | `PrefHelper.getInstance(context)`, SharedPreferences-backed state |
| `requestQueue_` | `ServerRequestQueue.getInstance(context)`, the API request queue |
| `deviceInfo_` | device and hardware data for request bodies |
| `trackingController` | tracking-disabled state |
| `branchConfigurationController_` | resolved config flags |
| `branchRemoteInterface_` | `BranchRemoteInterfaceUrlConnection`. Non-final and swappable via `setBranchRemoteInterface`, which is how tests mock the network |
| `branchPluginSupport_`, `branchQRCodeCache_`, `linkCache_` | supporting caches |

`initBranchSDK()` is the one-time constructor caller and early-returns if the singleton exists. `shutDown()` is package-private, test-only, and nulls the singleton plus statics.

## Session init: intent to callback

Callers drive init through `Branch.sessionBuilder(activity).withCallback(...).init()`.

1. **Intent parsing.** `readAndStripParam(uri, activity)` runs a chain of extractors (`extractBranchLinkFromIntentExtra`, `extractClickID`, `extractAppLink`, `extractExternalUriAndIntentExtras`, and others) that write results into `PrefHelper`: link click id, app link, push identifier, external intent uri and extras, initial referrer. A Branch link in an intent is consumed exactly once, marked by the `BranchLinkUsed` intent extra.
2. **Request selection.** `getInstallOrOpenRequest()` picks `ServerRequestRegisterInstall` or `ServerRequestRegisterOpen` based on `requestQueue_.hasUser()`, true only when a randomized bundle token is already persisted.
3. **Queueing with wait locks.** `registerAppInit()` sets state `INITIALISING` and force-inserts the init request at the front of the queue, or reuses an in-flight self-init request and transfers the callback to preserve ordering. `initTasks()` attaches `PROCESS_WAIT_LOCK`s so the request does not fire until prerequisites resolve:
   - `INTENT_PENDING_WAIT_LOCK` until `onIntentReady` at `Activity.onResume`
   - `INSTALL_REFERRER_FETCH_WAIT_LOCK` on install only
   - `GAID_FETCH_WAIT_LOCK` always
   - `USER_SET_WAIT_LOCK` attached separately by `initializeSession` when `withDelay` is used
4. **Response to callback.** On success the init request stores returned params into `PrefHelper` as **session params** (latest) and **install params** (first-ever), then invokes `callback.onInitFinished(...)`.

`getLatestReferringParams()` reads session params; `getFirstReferringParams()` reads install params. The synchronous variants block on a `CountDownLatch` for up to 2500 ms and must be called off the UI thread.

**Init state.** `Branch.SessionState { INITIALISED, INITIALISING, UNINITIALISED }` (`Branch.java:242`), exposed by `getInitState()` (`Branch.java:1447`). `INITIALISED` means the SDK has consumed the current intent and can send events.

**Intent lock.** By default the SDK waits for `Activity.onResume` (`intentState_` moves PENDING to READY via `onIntentReady`) so it captures the freshest intent, including the `onNewIntent` single-top case. `bypassWaitingForIntent(true)` and `unlockPendingIntent()` are the escape hatches. `removeSessionInitializationDelay()` clears the `withDelay` lock.

**Instant Deep Linking**, off by default: when enabled, `init()` fires the callback synchronously from cached intent params, nulls the callback, and lets the network init run for analytics only.

**Plugin deferral.** `deferInitForPluginRuntime` caches the session builder until `notifyNativeToInit()` is called. Used by React Native and Flutter wrappers.

## ServerRequest and the queue

`ServerRequest` (abstract) is the base for every API call. It owns the POST/GET body (`params_`), a `Defines.RequestPath`, a set of wait locks (`locks_`), and a retry count.

`BRANCH_API_VERSION` has three values: `V1`, `V1_LATD`, `V2`. `setPost()` branches on `V1`. **V1** (`/v1/install`, `/v1/open`, `/v1/url`) puts device fields at the top level. Everything else, meaning **V2** (`/v2/event/...`) and **`V1_LATD`** (`ServerRequestGetLATD`), nests them under a `user_data` object.

Subclasses: `ServerRequestInitSession` (leading to `ServerRequestRegisterInstall` and `ServerRequestRegisterOpen`), `ServerRequestLogEvent` (V2), `ServerRequestCreateUrl`, `ServerRequestGetLATD` (V1_LATD), `QRCode/ServerRequestCreateQRCode`, `validators/ServerRequestGetAppConfig`, plus the queue-only operations `QueueOperationSetIdentity` and `QueueOperationLogout`, which are enqueued for ordering but are not network requests.

**`ServerRequestQueue` is serialized, single-flight, and in-memory:**

- The backing store is `Collections.synchronizedList(new LinkedList<>())` (`ServerRequestQueue.java:87`) guarded by a static `reqQueueLockObject` (`:41`). Not a `ConcurrentLinkedQueue`. Capacity `MAX_ITEMS = 25` (`:35`); on overflow it drops index 1, never the head.
- A `Semaphore(1)` (`serverSema_`, `:43`) plus an `int networkCount_` (`:45`) enforce **one in-flight request at a time**. `processNextQueueItem()` peeks the head; if it has no pending wait locks it runs on a `BranchPostTask` (a `BranchAsyncTask`) with a task-timeout latch. Completion resets `networkCount_` and re-posts `processNextQueueItem` on a main-thread `Handler`, which avoids a stack overflow on the timeout path.
- **Disk persistence is vestigial.** The constructor opens a SharedPreferences file `"BNC_Server_Request_Queue"` (`PREF_KEY = "BNCServerRequestQueue"`), but nothing reads or writes it after init. `ServerRequest.toJSON`/`fromJSON` are implemented but unused by the queue, and both carry a TODO to drop serialization entirely. Queued requests do not survive a process restart.

**Queue storage and `PrefHelper` storage are separate SharedPreferences files.** `PrefHelper` uses `"branch_referral_shared_pref"`; the unused queue file is `"BNC_Server_Request_Queue"`. The queue reaches `PrefHelper` only through `Branch.getInstance().prefHelper_`.

**Session gating.** A request other than install, logout, or set-identity fails with `ERR_NO_SESSION` if there is no user. `requestNeedsSession()` exempts init, create-url, logout, and set-identity; everything else waits behind `SDK_INIT_WAIT_LOCK` until `INITIALISED`. On init success, the new `SessionID`, `RandomizedBundleToken`, and `RandomizedDeviceToken` are written to `PrefHelper` **and** `updateAllRequestsInQueue()` rewrites those keys into every already-queued request's body.

## PrefHelper

Singleton wrapper over the `"branch_referral_shared_pref"` file with typed accessors. String getters default to `NO_STRING_VALUE = "bnc_no_value"` (`PrefHelper.java:48`), so code distinguishes unset from empty with `hasPrefValue(...)` (`:1207`). Prefer that over comparing to the default; `isDMAParamsInitialized` and `isAttributionLevelInitialized` are the pattern to copy.

Stores: branch key plus key **source**, randomized device and bundle tokens, identity, session params versus install params, link-click / app-link / push / external-intent identifiers, install and referrer data (GCLID self-expires after 30 days), consent and DMA flags, tracking state, network tuning. In-memory only, not persisted: request and install metadata, partner params, custom server and CDN URLs, logging and EU-endpoint flags.

**Branch key resolution precedence** (`BranchUtil.readBranchKey`): `branch.json`, then manifest meta-data (`io.branch.sdk.BranchKey` and `.test`, where test mode falls back to the live key), then string resources. Each source is persisted alongside a key-source string. `branch.json` is read by `BranchJsonConfig`, a separate singleton; `BranchConfigurationController` surfaces resolved flags and delegates its own storage to `prefHelper_`.

## Invariants

Break one of these and the failure is usually silent, and usually not caught by CI.

- **Ordering is a contract.** Init is force-moved to the front. `setIdentity` and `logout` are queue operations precisely so they execute in enqueue order relative to other requests (ChangeLog 5.20.0).
- **`clearPrefOnBranchKeyChange()`**, triggered when `setBranchKey` sees a different key, calls `prefsEditor_.clear()` but deliberately re-writes four values so an in-flight deep link survives: link-click id, link-click identifier, app link, push identifier.
- **Tracking-disabled** (`TrackingController`, or `setConsumerProtectionAttributionLevel(NONE)`) blocks network calls except deep linking. Disabling clears session, link, and referrer prefs but **not** identity or the device and bundle tokens. Only requests whose `prepareExecuteWithoutTracking()` returns true pass through; init requests strip all PII and set `TrackingDisabled=true` in that mode. There is no `SKIP_QUEUE` mechanism; gating is entirely `prepareExecuteWithoutTracking()` plus the `ERR_BRANCH_TRACKING_DISABLED` checks.
- **One-time writes.** `KEY_ORIGINAL_INSTALL_TIME` is written once, when 0. Enhanced-web-link UX type and load-time are consumed once then reset. `postInitClear` resets link, referrer, and intent identifiers only when at most one init request remains queued.
- **Retry policy** (`onRequestFailed`): a request is dropped on HTTP 400 through 451, on `ERR_BRANCH_TRACKING_DISABLED`, when `shouldRetryOnFail()` is false, or once `currentRetryCount` hits the no-connection retry max. Otherwise it stays for replay. Init failure resets state to `UNINITIALISED`, but only when no session params are stored yet, so the intra-app-linking case deliberately stays `INITIALISED`.
