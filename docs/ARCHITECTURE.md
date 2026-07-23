# Branch Android SDK — Architecture Reference

Complete, exhaustive technical reference. For orientation use `docs/ONBOARDING.md`. For AI session context use `docs/AI_CONTEXT.md`.

---

## Table of Contents

1. [Module Structure](#1-module-structure)
2. [Package Layout](#2-package-layout)
3. [Core Entry Point — Branch.java](#3-core-entry-point--branchjava)
4. [Deep Linking Model](#4-deep-linking-model)
5. [Attribution Model](#5-attribution-model)
6. [Network Layer](#6-network-layer)
7. [Storage and Persistence](#7-storage-and-persistence)
8. [Design Patterns](#8-design-patterns)
9. [Extension Points](#9-extension-points)
10. [Test Infrastructure](#10-test-infrastructure)
11. [Component Relationship Map](#11-component-relationship-map)

---

## 1. Module Structure

The repository contains three Gradle modules wired together through `/settings.gradle.kts` and a root `/build.gradle.kts`.

| Module | Path | Purpose |
|--------|------|---------|
| `Branch-SDK` | `/Branch-SDK/` | Production SDK library published to Maven Central as `io.branch.sdk.android:library` |
| `Branch-SDK-TestBed` | `/Branch-SDK-TestBed/` | Sample/integration-test app exercising the SDK against real or mocked backends |
| `Branch-SDK-GPTDriver` | `/Branch-SDK-GPTDriver/` | AI-assisted (GPT) automated test harness that drives end-to-end "hybrid" tests |

---

## 2. Package Layout

All production source lives under `Branch-SDK/src/main/java/`:

```
io.branch.referral/           # Core SDK — the largest package
io.branch.referral.network/   # Network abstraction layer
io.branch.referral.util/      # Public data models (events, link properties, content)
io.branch.referral.validators/ # Developer integration validation tools
io.branch.indexing/           # BranchUniversalObject (content model)
io.branch.interfaces/         # Public interfaces (logging callbacks)
io.branch.coroutines/         # Kotlin coroutine utilities (device signals)
```

---

## 3. Core Entry Point — `Branch.java`

**File:** `Branch-SDK/src/main/java/io/branch/referral/Branch.java`

### Singleton construction

`Branch` is a strict singleton (`private static Branch branchReferral_`). The canonical factory methods are:

```java
public static Branch getAutoInstance(@NonNull Context context)
public static Branch getAutoInstance(@NonNull Context context, @NonNull String branchKey)
```

These methods read the Branch key from the manifest (or an explicit parameter), apply JSON config from `branch.json` assets, and call the private `initBranchSDK(context, branchKey)` which constructs the single instance. Subsequent calls return the cached instance.

The constructor wires together all sub-systems:

```java
private Branch(@NonNull Context context) {
    context_ = context;
    prefHelper_ = PrefHelper.getInstance(context);
    trackingController = new TrackingController(context);
    branchRemoteInterface_ = new BranchRemoteInterfaceUrlConnection(this);
    deviceInfo_ = new DeviceInfo(context);
    branchPluginSupport_ = new BranchPluginSupport(context);
    branchQRCodeCache_ = new BranchQRCodeCache(context);
    branchConfigurationController_ = new BranchConfigurationController();
    requestQueue_ = ServerRequestQueue.getInstance(context);
}
```

If the `Context` passed in is an `Application` instance, `setActivityLifeCycleObserver()` is called immediately, registering `BranchActivityLifecycleObserver` with `Application.registerActivityLifecycleCallbacks()`. This is the foundation of automatic session management.

### Session states

`Branch.SessionState` enum: `UNINITIALISED`, `INITIALISING`, `INITIALISED`. The state is checked at the queue insertion point to prevent duplicate in-flight init requests.

### Public API surface

| Method / Builder | Purpose |
|---|---|
| `static Branch sessionBuilder(Activity activity)` → `InitSessionBuilder` | Fluent builder to configure and start a session |
| `InitSessionBuilder.withCallback(BranchReferralInitListener)` | Registers session-init callback returning referring params |
| `InitSessionBuilder.withData(Uri)` | Supplies the intent URI for deep-link parsing |
| `InitSessionBuilder.withDelay(int ms)` | Delays init for hybrid runtimes that need JS engine startup |
| `InitSessionBuilder.ignoreIntent(boolean)` | Skips intent inspection (useful for re-inits where the original intent has stale data) |
| `InitSessionBuilder.init()` | Starts a new session |
| `InitSessionBuilder.reInit()` | Re-runs init mid-session (e.g., new intent from `onNewIntent()`) |
| `getLatestReferringParams()` | Returns last session's deep-link params (synchronous, from cache) |
| `getFirstReferringParams()` | Returns first-install deep-link params (synchronous, from cache) |
| `getLatestReferringParamsSync()` | Blocks until server response is received |
| `setIdentity(String userId, BranchReferralInitListener)` | Associates a developer identity with the device |
| `logout(LogoutStatusListener)` | Clears identity; rotates `bnc_randomized_bundle_token` |
| `setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel)` | Tiered privacy control (replaces deprecated `disableTracking`) |
| `setBranchRemoteInterface(BranchRemoteInterface)` | Replaces the HTTP layer |
| `setRequestTracingCallback(IBranchRequestTracingCallback)` | Intercept all request/response pairs |
| `enableLogging()` / `enableLogging(IBranchLoggingCallbacks)` | Activates logging, optionally piped through a callback |
| `deferInitForPluginRuntime(boolean)` | Suspends init until `notifyNativeToInit()` is called |
| `notifyNativeToInit()` | Releases the plugin runtime deferral lock |
| `setDMAParamsForEEA(boolean, boolean, boolean)` | Google DMA compliance consent signals |
| `static IntegrationValidator.validate(context)` | Developer-facing diagnostic tool |

### Initialization flow

1. `getAutoInstance()` is called (typically in `Application.onCreate()`), creating the singleton and registering the lifecycle observer.
2. In `Activity.onStart()`, the developer calls `Branch.sessionBuilder(activity).withCallback(cb).init()`.
3. `InitSessionBuilder.init()` calls `branch.readAndStripParam()` to extract URI and extras from the `Intent`.
4. IDL check: if the `Intent` contains `branch_data`, the callback fires immediately without waiting for the network.
5. `branch.getInstallOrOpenRequest()` produces either `ServerRequestRegisterInstall` or `ServerRequestRegisterOpen` based on whether `PrefHelper` has a stored app version.
6. The request is inserted into `ServerRequestQueue` and executed asynchronously via `BranchPostTask`.
7. On success, session tokens are persisted to `PrefHelper` and `BranchReferralInitListener.onInitFinished()` is fired.

---

## 4. Deep Linking Model

### Intent inspection

`BranchActivityLifecycleObserver.onActivityResumed()` calls `branch.onIntentReady(activity)`, which triggers `readAndStripParam(uri, activity)`. This method:

1. Inspects `Intent.getData()` for an App Link or Branch click URL.
2. Looks for `Intent.EXTRA_REFERRER` (the calling app's package name) for referral attribution.
3. Checks a whitelist of recognized extras (e.g., `"extra_launch_uri"` for Facebook Ads, `"al_applink_data"` for App Links).
4. Stores any extracted link-click identifier in `PrefHelper` under `bnc_link_click_id`.

### Instant Deep Linking (IDL)

When an app is launched by a Branch link that was previously resolved in the background (e.g., via `Branch.getLatestReferringParamsSync()` in a browser), the `Intent` may already contain a `branch_data` extra with the full link parameters. In that case:

- `isInstantDeepLinkPossible` is set to `true`.
- `BranchReferralInitListener.onInitFinished()` fires synchronously with the cached params before the network request is sent.
- The network request still executes; if the server returns different params, the callback fires a second time.

### URL query parameter attribution

`ReferringUrlUtility.kt` maintains a typed map of recognized URL query parameters. On every open request, it extracts GCLID, UTM source/medium/campaign/term/content, and other recognized parameters from the Intent URI. Each is stored as a `BranchUrlQueryParameter` with:
- `value` — the raw string value
- `timestamp` — when it was captured
- `isDeepLink` — whether it came from a Branch link vs. a web referrer
- `validityWindow` — how long it is considered attributable (GCLID: 30 days, UTM: configurable)

These are appended to `ServerRequestRegisterOpen` and `ServerRequestLogEvent` payloads.

### Link data structures

**`BranchLinkData`** — extends `JSONObject`. Fields: `tags`, `alias`, `type`, `channel`, `feature`, `stage`, `campaign`, `params`, `duration`. Used as the cache key in `Branch.linkCache_` (`ConcurrentHashMap<BranchLinkData, String>`), which stores previously-generated short URLs to avoid redundant network calls.

**`LinkProperties`** (`referral/util/LinkProperties.java`) — Parcelable builder-style class exposing the same fields plus `controlParams_` (a `HashMap<String, String>` of `$`-prefixed control parameters: `$deeplink_path`, `$android_url`, `$fallback_url`, `$ios_url`, etc.).

**`BranchUniversalObject`** (`io.branch.indexing/BranchUniversalObject.java`) — Content model wrapping `canonicalIdentifier`, `canonicalUrl`, `title`, `description`, `imageUrl`, and a `ContentMetadata` bag holding product/commerce metadata (price, SKU, category, condition, etc.). Key methods: `generateShortUrl(context, linkProperties, callback)`, `showShareSheet(activity, linkProperties, shareSheetStyle, callback)`.

---

## 5. Attribution Model

### Install vs. open detection

`ServerRequestInitSession.updateInstallStateAndTimestamps()` determines the event type before the request is sent:

| State constant | Value | Condition |
|---|---|---|
| `STATE_FRESH_INSTALL` | `0` | No stored `app_version` in `PrefHelper` |
| `STATE_UPDATE` | `2` | `firstInstallTime` ≠ `lastUpdateTime` within a 24-hour buffer |
| `STATE_NO_CHANGE` | `1` | Regular open — no version change |

The detected state, plus timestamps (`first_install_time`, `latest_install_time`, `latest_update_time`, `previous_update_time`), are sent in the request body. The Branch server applies its own deduplication and reinstall logic on top.

### Server request types

All endpoint paths are defined in `Defines.RequestPath`:

| Endpoint | Class | Trigger |
|---|---|---|
| `v1/install` | `ServerRequestRegisterInstall` | First-ever launch (no stored app version) |
| `v1/open` | `ServerRequestRegisterOpen` | Every subsequent launch |
| `v2/event/standard` | `ServerRequestLogEvent` | `BranchEvent` with a standard event name |
| `v2/event/custom` | `ServerRequestLogEvent` | `BranchEvent` with a custom event name |
| `v1/cpid/latd` | `ServerRequestGetLATD` | `Branch.getLastAttributedTouchData()` |
| `v1/url` | `ServerRequestCreateUrl` | Short link generation |
| `v1/qr-code` | `ServerRequestCreateQRCode` | QR code generation |
| `v1/logout` | `ServerRequestLogout` | `Branch.logout()` |
| `v1/profile` | `ServerRequestIdentifyUserRequest` | `Branch.setIdentity()` |
| `v1/app-link-settings` | (IntegrationValidator) | Diagnostic check only |

### Session and identity tokens

All stored in `SharedPreferences` via `PrefHelper` (file name `"branch_referral_shared_pref"`):

| Token key | Meaning |
|---|---|
| `bnc_randomized_device_token` | Device-level identifier; persistent across reinstalls if Android Backup is enabled |
| `bnc_randomized_bundle_token` | Per-install "user" token; rotates on every `logout()` call |
| `bnc_session_id` | Per-session ID; refreshed on each app foreground transition |
| `bnc_link_click_id` | ID of the Branch link click that led to this install/open |
| `bnc_identity` | Developer-set user identity string (from `setIdentity()`) |
| `bnc_identity_id` | Server-assigned identity ID corresponding to the developer identity |

### Attribution levels

`Defines.BranchAttributionLevel` — replaces the deprecated `disableTracking(boolean)`:

| Level | Advertising IDs | Device IDs | IP address | Webhooks / SAN |
|-------|----------------|-----------|------------|----------------|
| `FULL` | Yes | Yes | Yes | Yes |
| `REDUCED` | No | Yes | Yes | No SAN callouts |
| `MINIMAL` | No | Yes | Yes | No |
| `NONE` | No | No | No | No |

`NONE` = deep linking only. All analytics, event logging, and postbacks are disabled.

### Install referrer data sources

`AppStoreReferrer.processReferrerInfo()` is the common entry point, called by store-specific subclasses:

- **Google Play Install Referrer** — via `com.android.installreferrer.api.InstallReferrerClient`
- **Google Search Install Referrer** — via `com.google.android.gms.ads.identifier`
- **Meta Install Referrer** — distinguished by the `"Meta"` store string in the referrer
- **Xiaomi GetApps / Huawei AppGallery / Samsung Galaxy Store** — via `compileOnly` optional dependencies; loaded reflectively if present

---

## 6. Network Layer

### Abstraction

**`BranchRemoteInterface`** (`network/BranchRemoteInterface.java`) is the abstract network contract with two abstract methods:

```java
protected abstract BranchResponse doRestfulGet(String url) throws BranchRemoteException;
protected abstract BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException;
```

The public sealed entry points are `make_restful_get()` and `make_restful_post()`, which prepend the Branch key and SDK version as headers before delegating. All responses are parsed into `ServerResponse` objects.

### Default implementation

**`BranchRemoteInterfaceUrlConnection`** uses `HttpsURLConnection`:

| Parameter | Value |
|---|---|
| Read timeout | 5500 ms (`TIMEOUT`) |
| Connect timeout | 10000 ms (`CONNECT_TIMEOUT`) |
| Max retries | 3 (`MAX_RETRIES`) |
| Retry interval | 1000 ms |
| Retry triggers | 5xx responses, `SocketTimeoutException` |
| QR code responses | Binary decoded as Base64 |

Callers can inject a custom implementation via `Branch.setBranchRemoteInterface(BranchRemoteInterface)`.

### Request model

**`ServerRequest`** (abstract) — every network call is a subclass:

| Field | Type | Purpose |
|---|---|---|
| `params_` | `JSONObject` | POST body / GET parameters |
| `requestPath_` | `Defines.RequestPath` | URL endpoint |
| `uuid` | `String` | Per-request UUID for server-side deduplication |
| `creation_ts` | `long` | Timestamp for queue ordering |
| `waitLocks_` | `Set<PROCESS_WAIT_LOCK>` | Blocks execution until resolved |

**Wait locks** (`PROCESS_WAIT_LOCK` enum):
- `GAID_FETCH_WAIT_LOCK` — waiting for Google Advertising ID async fetch
- `INTENT_PENDING_WAIT_LOCK` — waiting for intent data to be available
- `SDK_INIT_WAIT_LOCK` — waiting for full SDK initialization
- `GOOGLE_PLAY_INSTALL_REFERRER_FETCH_WAIT_LOCK`
- `INSTALL_REFERRER_FETCH_WAIT_LOCK`

**Template method hooks** (all subclasses implement):
- `onPreExecute()` — called on main thread before queue insertion
- `doFinalUpdateOnMainThread()` — called just before execution on main thread (DMA params, attribution level, device info)
- `doFinalUpdateOnBackgroundThread()` — called on background thread (GAID, heavy device signals)
- `onRequestSucceeded(ServerResponse, Branch)` — success handler
- `handleFailure(int statusCode, String causeMsg)` — failure handler
- `handleErrors(Context)` — called for pre-flight validation errors
- `isGetRequest()` — determines HTTP method
- `clearCallbacks()` — clears all listener references to prevent leaks

**Persistence** — `ServerRequest.toJSON()` serializes the request to JSON for disk storage. `ServerRequest.fromJSON(JSONObject, Context)` and `getExtendedServerRequest(requestPath, post, context)` reconstruct the correct subclass on restore.

### Response model

**`ServerResponse`** — holds:
- `statusCode` — HTTP status code
- `object` — parsed `JSONObject` of the response body
- `requestId` — server-assigned request ID (from `X-Branch-Request-Id` header)
- `failReason` — string description if the request failed

**`BranchRemoteInterface.BranchResponse`** — raw transport-level wrapper (response string + HTTP status code).

**`BranchRemoteInterface.BranchRemoteException`** — typed error with codes: `ERR_BRANCH_NO_CONNECTIVITY`, `ERR_BRANCH_REQ_TIMED_OUT`, `ERR_BRANCH_UNABLE_TO_PARSE_RESPONSE`, etc.

### Execution model

`ServerRequestQueue.executeTimedBranchPostTask()` creates a `BranchPostTask extends BranchAsyncTask<ServerRequest, Void, ServerResponse>` for each request:

1. A `CountDownLatch(1)` enforces the global task timeout (`TASK_TIMEOUT = 15500 ms`).
2. If the latch is not released in time, `postTask.cancel(true)` is called and the request fails with `ERR_BRANCH_TASK_TIMEOUT`.
3. A `Semaphore(1)` (`serverSema_`) prevents concurrent execution — the next task does not start until the previous one releases the semaphore.
4. The in-memory queue is `Collections.synchronizedList(new LinkedList<ServerRequest>())` with a maximum of 25 pending items.

---

## 7. Storage and Persistence

### `PrefHelper`

**File:** `Branch-SDK/src/main/java/io/branch/referral/PrefHelper.java`

A singleton wrapping `SharedPreferences` (file: `"branch_referral_shared_pref"`). All SDK state reads and writes go through this class.

**Identity and session state:**

| Key constant | SharedPreferences key | Meaning |
|---|---|---|
| `KEY_RANDOMIZED_DEVICE_TOKEN` | `bnc_randomized_device_token` | Device-level ID |
| `KEY_RANDOMIZED_BUNDLE_TOKEN` | `bnc_randomized_bundle_token` | Per-install user token |
| `KEY_SESSION_ID` | `bnc_session_id` | Current session ID |
| `KEY_LINK_CLICK_ID` | `bnc_link_click_id` | Click ID from last attributed link |
| `KEY_IDENTITY` | `bnc_identity` | Developer-set user identity string |
| `KEY_IDENTITY_ID` | `bnc_identity_id` | Server-assigned identity ID |

**Deep-link data:**

| Key constant | SharedPreferences key | Meaning |
|---|---|---|
| `KEY_SESSION_PARAMS` | `bnc_session_params` | JSON of last session's deep-link data |
| `KEY_INSTALL_PARAMS` | `bnc_install_params` | JSON of first-install data (write-once) |
| `KEY_EXTERNAL_INTENT_URI` | `bnc_external_intent_uri` | Saved intent URI for next open request |
| `KEY_EXTERNAL_INTENT_EXTRA` | `bnc_external_intent_extra` | Saved intent extras for next open request |

**Timestamps:**

| Key constant | SharedPreferences key | Meaning |
|---|---|---|
| `KEY_ORIGINAL_INSTALL_TIME` | `bnc_original_install_time` | Timestamp of first-ever install |
| `KEY_LAST_KNOWN_UPDATE_TIME` | `bnc_last_known_update_time` | Timestamp of last app update |
| `KEY_PREVIOUS_UPDATE_TIME` | `bnc_previous_update_time` | Timestamp of previous app update |
| `KEY_LAST_STRONG_MATCH_TIME` | `bnc_last_strong_match_time` | Timestamp of last fingerprint match |

**Privacy and consent:**

| Key constant | SharedPreferences key | Meaning |
|---|---|---|
| `KEY_TRACKING_STATE` | `bnc_tracking_disabled` | Legacy tracking flag |
| `KEY_CONSUMER_PROTECTION_ATTRIBUTION_LEVEL` | `bnc_consumer_protection_attribution_level` | Current `BranchAttributionLevel` |
| `KEY_DMA_EEA` | `bnc_dma_eea` | Whether device is in EEA |
| `KEY_DMA_AD_PERSONALIZATION` | `bnc_dma_ad_personalization` | Ad personalization consent |
| `KEY_DMA_AD_USER_DATA` | `bnc_dma_ad_user_data` | Ad user data consent |

**Install referrer:**

| Key constant | SharedPreferences key | Meaning |
|---|---|---|
| `KEY_APP_STORE_SOURCE` | `bnc_app_store_source` | Store that provided the install referrer |
| `KEY_GOOGLE_PLAY_INSTALL_REFERRER_EXTRAS` | `bnc_google_play_install_referrer_extras` | Raw Play Store referrer string |
| `KEY_GCLID_VALUE` | `bnc_gclid_json_object` | GCLID value + 30-day expiry window |

**Other:**

| Key constant | SharedPreferences key | Meaning |
|---|---|---|
| `KEY_LOG_IAP_AS_EVENTS` | `bnc_log_iap_as_events` | Whether to auto-log Google Play IAP |
| `KEY_PARTNER_PARAMS` | `bnc_partner_params` | Serialized `BranchPartnerParameters` |

### `ServerRequestQueue` persistence

**File:** `Branch-SDK/src/main/java/io/branch/referral/ServerRequestQueue.java`

Uses a second `SharedPreferences` file (`"BNC_Server_Request_Queue"`) under key `"BNCServerRequestQueue"`. On each queue modification, the entire in-memory queue is serialized to this file. On startup, the file is deserialized to restore any requests that were pending before the process was killed. This guarantees at-least-once delivery for queued requests.

---

## 8. Design Patterns

### Singleton (double-checked locking)

- **`Branch`:** `private static volatile Branch branchReferral_` with `synchronized (Branch.class)` guard
- **`ServerRequestQueue`:** `private static ServerRequestQueue mInstance` with `synchronized (ServerRequestQueue.class)` guard
- **`PrefHelper`:** identical pattern
- **`BillingGooglePlay.kt`:** Kotlin `companion object` with `@Volatile lateinit var instance`

### Builder

- **`InitSessionBuilder`** (static inner class of `Branch`) — fluent session initialization with `withCallback()`, `withData()`, `withDelay()`, `ignoreIntent()`, `init()`, `reInit()`
- **`LinkProperties`** — fluent link metadata
- **`BranchEvent`** — fluent event logging with `addCustomDataProperty()`, `setOrderId()`, `setRevenue()`, etc.
- **`BranchUniversalObject`** — fluent content model with `setCanonicalIdentifier()`, `setTitle()`, `setContentMetadata()`, etc.

### Template Method

`ServerRequest` defines a fixed algorithm with abstract extension points. The execution engine in `BranchPostTask` calls the hooks in order; subclasses override only the parts relevant to their request type. This means the queueing, timeout, retry, and serialization logic is written once.

### Strategy

`BranchRemoteInterface` is a strategy interface for HTTP transport. The production strategy is `BranchRemoteInterfaceUrlConnection`. Tests inject `MockRemoteInterface`. Developers can inject their own for proxying, logging, or testing against staging servers.

### Observer / Callback

All async results use single-method listener interfaces (effectively SAM interfaces for Java). Key listeners:

- `BranchReferralInitListener.onInitFinished(JSONObject referringParams, BranchError error)` — session init
- `BranchUniversalReferralInitListener.onInitFinished(BranchUniversalObject, LinkProperties, BranchError)` — typed variant
- `BranchLinkCreateListener.onLinkCreate(String url, BranchError error)` — link creation
- `BranchLinkShareListener` — share sheet lifecycle (prepare, complete, share channel selected, etc.)
- `TrackingStateCallback.onTrackingStateChanged(boolean isTracking, JSONObject params, BranchError error)`
- `LogoutStatusListener.onLogoutFinished(boolean loggedOut, BranchError error)`
- `IBranchRequestTracingCallback.onRequestCompleted(String uri, JSONObject request, JSONObject response, String error, String requestUrl)`

### Lifecycle Observer

`BranchActivityLifecycleObserver implements Application.ActivityLifecycleCallbacks` is registered globally in `Branch.setActivityLifeCycleObserver()`. It maintains an `activityCnt_` integer: incremented in `onActivityStarted()`, decremented in `onActivityStopped()`. When `activityCnt_` drops to zero, `branch.closeSessionInternal()` is called. When it rises from zero, a new session init is triggered. This is the mechanism behind "automatic session management" — developers don't need to call SDK methods in every Activity's lifecycle.

### Factory Method

`ServerRequest.fromJSON(JSONObject, Context)` and `getExtendedServerRequest(requestPath, post, context)` reconstruct the correct `ServerRequest` subclass from a persisted JSON snapshot. This factory is called by `ServerRequestQueue` when restoring requests from disk.

---

## 9. Extension Points

### Custom network transport

`BranchRemoteInterface` is a fully public abstract class. Implement `doRestfulGet()` and `doRestfulPost()` and inject via `Branch.setBranchRemoteInterface(yourImpl)`. This replaces all HTTP I/O — useful for:
- Unit/instrumented tests (`MockRemoteInterface`)
- Traffic proxying to staging servers
- Custom certificate pinning
- Request/response inspection

### Logging callback

```java
public interface IBranchLoggingCallbacks {
    void onBranchLog(String logMessage, String severityConstant);
}
```
Set via `Branch.enableLogging(IBranchLoggingCallbacks)`. Receives all internal log messages with a severity string. Enables piping Branch logs into crash-reporting tools (Crashlytics, Sentry, etc.) or custom analytics.

### Request tracing callback

```java
public interface IBranchRequestTracingCallback {
    void onRequestCompleted(String uri, JSONObject request, JSONObject response, String error, String requestUrl);
}
```
Set via `Branch.setRequestTracingCallback()`. Called for every completed init-session request. Useful for network debugging and integration verification.

### Plugin runtime deferral

Cross-platform runtimes (React Native, Flutter, Cordova) that initialize asynchronously can defer SDK init:

1. `Branch.deferInitForPluginRuntime(true)` — causes `InitSessionBuilder.init()` to cache the builder and return without executing.
2. When the JS/Dart engine is ready, the plugin calls `Branch.notifyNativeToInit()` — this re-fires the cached `InitSessionBuilder.init()` and the normal flow resumes.

`BranchPluginSupport` provides `deviceDescription()` for plugin wrappers that need device info independently.

Plugin identity is set via:
```java
Branch.pluginName = "ReactNative";
Branch.pluginVersion = "5.x.x";
```
These are included in every request payload for diagnostic purposes.

### Partner parameters

`BranchPartnerParameters` — a standalone key-value store for third-party SDK integrations (e.g., Adjust, AppsFlyer co-attribution). Loaded into every init session request body via `prefHelper_.loadPartnerParams(post)`. Set via:
```java
Branch.getInstance().getPartnerParameters()
    .addCustomParameter("partner_key", "partner_value");
```

### DMA compliance

```java
Branch.setDMAParamsForEEA(boolean eeaRegion, boolean adPersonalizationConsent, boolean adUserDataUsageConsent)
```
Injected into init session and event requests via `shouldAddDMAParams()` / `addDMAParams()` overrides in `ServerRequestInitSession`.

### In-app billing integration

`BillingGooglePlay.kt` (Kotlin, declared as `compileOnly` in `build.gradle.kts`) listens for Google Play `Purchase` events and auto-logs them as Branch `PURCHASE` standard events. Enabled via:
```java
Branch.getInstance().setAutomaticIAPLogging(true);
```

### Integration Validator

`IntegrationValidator.validate(context)` — a developer-mode diagnostic that:
1. Hits `v1/app-link-settings` with the configured Branch key.
2. Runs a series of `IntegrationValidatorCheck` implementations covering URI scheme, App Links, package name, Branch key validity, and custom domain setup.
3. Displays results in `IntegrationValidatorDialog`.

Intended to be called once during development to verify the integration is correct before shipping.

---

## 10. Test Infrastructure

### JVM unit tests

**Location:** `Branch-SDK/src/test/java/io/branch/referral/`

Selected files:
- `InstallReferrerResultTests.kt`
- `BranchPartnerParametersTest.java`
- `BranchConfigurationControllerTest.kt`

Uses JUnit 4 + Mockito (`mockito-core:5.4.0`, `mockito-kotlin:4.1.0`). The `org.json:json` artifact is included as a test dependency to substitute for Android's JSON implementation, enabling JSON-heavy code to run on the JVM without Robolectric.

### Instrumented (Android) tests

**Location:** `Branch-SDK/src/androidTest/java/io/branch/referral/`

**Base class — `BranchTest.java`:**
```java
@Before
public void setUp() throws Exception {
    Branch.shutDown();
    clearSharedPrefs("branch_referral_shared_pref");
    clearSharedPrefs("BNC_Server_Request_Queue");
    Branch branch = Branch.getTestInstance(ApplicationProvider.getApplicationContext());
    branch.setBranchRemoteInterface(new MockRemoteInterface());
}
```

**`MockRemoteInterface.java`** — extends `BranchRemoteInterface`:
- Returns canned `JSONObject` responses for `v1/install`, `v1/open`, `v1/url`, `v1/qr-code`
- Sleeps for `TEST_REQUEST_TIMEOUT / 2` to simulate network latency
- `setNextResponse(JSONObject)` allows per-test response injection

Concrete test classes:
- `BranchEventTest` — event logging
- `ServerRequestTests` — request serialization and queue behavior
- `BranchQRCodeTests` — QR code generation flow
- `ReferringUrlUtilityTests` — GCLID/UTM parsing and expiry logic
- `PrefHelperTest` — SharedPreferences read/write contract
- `BranchCPIDTest` — last-attributed-touch data
- `DeviceInfoTest` — device signal assembly
- `SystemObserverTests` — hardware/OS signal detection
- `AdvertisingIdTests` — GAID fetch behavior
- `BillingGooglePlayTests` — IAP auto-logging

Uses `androidx.test` (AndroidJUnit4 runner, `ActivityScenario`, `ApplicationProvider`), `jsonassert`, and Espresso.

### GPT-driven end-to-end tests

**Location:** `Branch-SDK-GPTDriver/src/main/java/io/branch/gptdriver/tests/`

Hybrid test classes extend `BaseGptDriverTest`:
- `DeepLinkColdOpenHybridTest`
- `SessionAndLogsHybridTest`
- `EventLoggingHybridTest`
- `TrackingControlHybridTest`
- `UserIdentityHybridTest`

These drive the TestBed app against a real or mocked server, with AI assistance for scenario generation and assertion verification. Supporting infrastructure:
- `LinkGenerationIdlingResource` — Espresso `IdlingResource` for async link generation
- `GptDriverRetry` — retry annotation and rule for flaky network-dependent assertions

### Build-level test configuration

From `Branch-SDK/build.gradle.kts`:
- `org.gradle.test-retry`: max 3 retries per failing test
- Jacoco: `jacocoTestReport` task merges JVM and instrumented coverage into a unified report
- `enableAndroidTestCoverage = true` on the `debug` build type

---

## 11. Component Relationship Map

```
Application.onCreate()
  └─ Branch.getAutoInstance(context)
       ├─ reads branch key from AndroidManifest / branch.json
       ├─ constructs PrefHelper             — SharedPreferences persistence singleton
       ├─ constructs TrackingController     — gates all analytics on attribution level
       ├─ constructs DeviceInfo             — assembles hardware/OS/carrier signals
       ├─ constructs BranchRemoteInterfaceUrlConnection  — HTTP via HttpsURLConnection
       ├─ constructs ServerRequestQueue     — in-memory LinkedList + SharedPreferences disk cache
       ├─ constructs BranchPluginSupport    — cross-platform plugin bridge
       ├─ constructs BranchQRCodeCache      — in-memory QR code bitmap cache
       ├─ constructs BranchConfigurationController
       └─ if Application: registers BranchActivityLifecycleObserver
            └─ tracks activityCnt_; drives session open/close automatically

Activity.onStart()
  └─ Branch.sessionBuilder(activity).withCallback(cb).init()
       ├─ readAndStripParam(intent)
       │    ├─ extract URI click ID → PrefHelper.setLinkClickId()
       │    ├─ extract EXTRA_REFERRER → request payload
       │    └─ extract FB/Google/AppLinks extras → request payload
       ├─ IDL check: Intent has branch_data?
       │    └─ yes → callback.onInitFinished(cachedParams, null)  [immediate, before network]
       ├─ getInstallOrOpenRequest()
       │    ├─ PrefHelper has app_version? → ServerRequestRegisterOpen
       │    └─ no → ServerRequestRegisterInstall
       ├─ ServerRequestQueue.insertRequestAtFront(request)
       └─ executeTimedBranchPostTask()
            ├─ CountDownLatch(1) — enforces 15.5s task timeout
            ├─ Semaphore(1) — ensures single in-flight request
            ├─ request.doFinalUpdateOnMainThread()
            │    └─ adds DMA params, attribution level, partner params, device info
            ├─ request.doFinalUpdateOnBackgroundThread()
            │    └─ adds GAID, waits on GAID_FETCH_WAIT_LOCK
            ├─ BranchRemoteInterface.make_restful_post(url, params)
            │    └─ prepends Branch key + SDK version headers
            │    └─ BranchRemoteInterfaceUrlConnection.doRestfulPost()
            │         ├─ HttpsURLConnection with 5.5s read / 10s connect timeout
            │         └─ retry up to 3x on 5xx / SocketTimeout
            └─ on success: request.onRequestSucceeded(response, branch)
                 ├─ ServerRequestRegisterInstall: prefHelper_.setInstallParams(json)
                 ├─ ServerRequestRegisterOpen: prefHelper_.setSessionParams(json)
                 └─ callback.onInitFinished(referringParams, null)

BranchEvent.logEvent(context)
  └─ ServerRequestLogEvent (v2/event/standard or v2/event/custom)
       ├─ TrackingController.isTrackingDisabled()?
       │    └─ yes and event is analytics-only → drop request
       ├─ attaches BranchUniversalObject content items if set
       ├─ attaches ReferringUrlUtility query params (GCLID, UTM)
       └─ ServerRequestQueue.add(request) → same execution path as above
```
