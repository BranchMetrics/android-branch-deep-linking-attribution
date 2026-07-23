# Branch Android SDK — Claude Instructions

Compact reference for fast orientation on this repo. Read before doing any work.

## Other docs

- `docs/ONBOARDING.md` — human developer onboarding guide; do not load into context unless asked
- `docs/ARCHITECTURE.md` — exhaustive technical reference; consult for deep dives, not routine work

---

## Module map

| Module | Purpose |
|--------|---------|
| `Branch-SDK/` | Production library — all work happens here |
| `Branch-SDK-TestBed/` | Sample app for manual/integration testing |
| `Branch-SDK-GPTDriver/` | AI-driven end-to-end test harness |

All production source: `Branch-SDK/src/main/java/io/branch/referral/`

---

## Core classes

| Class | Role |
|-------|------|
| `Branch.java` | Singleton entry point; owns the public API and wires all sub-systems |
| `PrefHelper.java` | All SharedPreferences reads/writes (`"branch_referral_shared_pref"`) |
| `ServerRequest.java` | Abstract base for every network request; defines the template method lifecycle |
| `ServerRequestQueue.java` | In-memory + disk-backed queue (`"BNC_Server_Request_Queue"`); `Semaphore(1)` enforces one in-flight request |
| `BranchRemoteInterface.java` | Abstract HTTP strategy; default impl is `BranchRemoteInterfaceUrlConnection` |
| `BranchActivityLifecycleObserver.java` | Registered globally; drives automatic session open/close via `activityCnt_` |
| `ServerRequestRegisterInstall.java` | `v1/install` — first-ever open |
| `ServerRequestRegisterOpen.java` | `v1/open` — every subsequent open |
| `ServerRequestLogEvent.java` | `v2/event/standard` and `v2/event/custom` |
| `ServerRequestInitSession.java` | Abstract parent of Install + Open; owns install-state detection and timestamp logic |
| `DeviceInfo.java` | Assembles device/OS signals for request payloads |
| `TrackingController.java` | Guards all analytics calls against `BranchAttributionLevel` |
| `ReferringUrlUtility.kt` | Parses GCLID/UTM params from intent URIs; attaches them with expiry windows |
| `AppStoreReferrer.java` | Entry point for Play/Google/Meta/OEM install referrer data |
| `BranchPluginSupport.java` | Cross-platform plugin bridge (RN, Flutter, Cordova) |
| `BranchEvent.java` | Fluent builder for standard and custom events (`util/`) |
| `LinkProperties.java` | Parcelable link metadata builder (`util/`) |
| `BranchUniversalObject.java` | Content model with commerce metadata (`io.branch.indexing`) |

---

## Initialization sequence

```
Application.onCreate()
  Branch.getAutoInstance(context)
    → reads branch key from manifest / branch.json
    → constructs: PrefHelper, TrackingController, DeviceInfo, BranchRemoteInterface,
                  ServerRequestQueue, BranchPluginSupport, BranchQRCodeCache
    → if context is Application: registers BranchActivityLifecycleObserver

Activity.onStart()
  Branch.sessionBuilder(activity).withCallback(cb).init()
    → readAndStripParam() — extracts URI, EXTRA_REFERRER, FB/Google intent extras
    → IDL check: if Intent has branch_data → fire callback immediately (no network)
    → getInstallOrOpenRequest() → ServerRequestRegisterInstall or ServerRequestRegisterOpen
    → ServerRequestQueue.insertRequestAtFront()
    → BranchPostTask (AsyncTask) executes via Semaphore
    → on success: prefHelper_.setSessionParams() → callback.onInitFinished()
```

---

## Request lifecycle (ServerRequest template)

1. `onPreExecute()` — add wait locks (`GAID_FETCH_WAIT_LOCK`, `INTENT_PENDING_WAIT_LOCK`, etc.)
2. `doFinalUpdateOnMainThread()` — add DMA params, attribution level, partner params
3. `doFinalUpdateOnBackgroundThread()` — add GAID, device tokens
4. `BranchRemoteInterface.make_restful_post()` — adds Branch key + SDK version headers
5. Response → `onRequestSucceeded()` or `handleFailure()` or `handleErrors()`

Task timeout: 15.5 s (`CountDownLatch`). Retry: 3× with 1 s intervals on 5xx / socket timeout.

---

## State that lives in PrefHelper

| Key | When it matters |
|-----|-----------------|
| `bnc_randomized_device_token` | Device identity — sent on every request |
| `bnc_randomized_bundle_token` | Per-install user token — rotates on `logout()` |
| `bnc_session_id` | Refreshed each foreground; sent on every request |
| `bnc_session_params` | Last session's deep-link JSON — returned by `getLatestReferringParams()` |
| `bnc_install_params` | First-install deep-link JSON — set once, cleared on `logout()` |
| `bnc_link_click_id` | Click ID from the Branch link that drove this install/open |
| `bnc_consumer_protection_attribution_level` | `FULL / REDUCED / MINIMAL / NONE` |
| `bnc_gclid_json_object` | GCLID + expiry (30-day window) |
| `bnc_dma_*` | Google DMA consent flags |

---

## Where to make changes

| Task | File(s) to touch |
|------|-----------------|
| Change what's sent on install/open | `ServerRequestRegisterInstall` / `ServerRequestRegisterOpen` + `ServerRequestInitSession` |
| Change what's sent on every event | `ServerRequestLogEvent` |
| Add a new request type | Subclass `ServerRequest`; add path to `Defines.RequestPath` |
| Change device/OS payload | `DeviceInfo.java` |
| Change how deep-link URI is parsed | `Branch.readAndStripParam()` + `ReferringUrlUtility.kt` |
| Change install-state detection | `ServerRequestInitSession.updateInstallStateAndTimestamps()` |
| Change privacy/attribution behavior | `TrackingController` + `Defines.BranchAttributionLevel` |
| Add a new SharedPreferences key | `PrefHelper` — add constant + getter/setter |
| Replace HTTP transport (e.g. for tests) | Subclass `BranchRemoteInterface`; inject via `Branch.setBranchRemoteInterface()` |
| Change session open/close triggers | `BranchActivityLifecycleObserver` |
| Add partner/plugin support | `BranchPluginSupport`; set `Branch.pluginName` / `Branch.pluginVersion` |

---

## Extension points (public surface)

- **Custom HTTP:** implement `BranchRemoteInterface` → `Branch.setBranchRemoteInterface()`
- **Logging:** implement `IBranchLoggingCallbacks` → `Branch.enableLogging(callback)`
- **Request tracing:** implement `IBranchRequestTracingCallback` → `Branch.setRequestTracingCallback()`
- **Plugin init deferral:** `Branch.deferInitForPluginRuntime(true)` + `notifyNativeToInit()`
- **Partner params:** `BranchPartnerParameters` — appended to every init/event request
- **DMA:** `Branch.setDMAParamsForEEA(eea, adPersonalization, adUserData)`
- **Auto IAP:** `BillingGooglePlay.kt` (`compileOnly`) — auto-logs Play purchases as `PURCHASE` events

---

## Test injection pattern

```java
// In BranchTest.java (abstract base for all instrumented tests)
Branch.shutDown();
// clears "branch_referral_shared_pref" and "BNC_Server_Request_Queue"
branch = Branch.getTestInstance(context);
branch.setBranchRemoteInterface(new MockRemoteInterface());
```

`MockRemoteInterface` returns canned JSON for `RegisterInstall`, `RegisterOpen`, `GetURL`, `QRCode` and sleeps `TEST_REQUEST_TIMEOUT / 2` to simulate latency.

---

## Non-obvious invariants

- The queue `Semaphore(1)` means all requests are serialized — there is never more than one in-flight HTTP call.
- IDL (Instant Deep Link) fires the init callback synchronously, before the network request completes. The subsequent server response can update params again.
- `bnc_install_params` is written exactly once and is never overwritten — it always reflects the first attributed install, even across app updates.
- `ServerRequestQueue` has its own separate SharedPreferences file from `PrefHelper` — they are independent stores.
- `TrackingController` is checked at the queue level; a tracking-disabled state does not prevent deep-link init requests, only analytics/event requests.
- Plugin runtime deferral (`deferInitForPluginRuntime`) causes `InitSessionBuilder.init()` to cache the builder and do nothing until `notifyNativeToInit()` is called.
