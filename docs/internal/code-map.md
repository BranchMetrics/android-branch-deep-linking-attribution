# Code map (6.0.0-beta.0)

> Contributor-facing notes for work on this branch.
> Last updated 2026-09-03, against `6.0.0-beta.0` at 88b3bf28.

Where things live, and which file to open for a given task. Line numbers drift on a live branch; treat any `file:line` as a locate-then-confirm hint.

## Modules

| Module | What it is |
| --- | --- |
| `:Branch-SDK` | the library. All production code under `Branch-SDK/src/main/java/io/branch/` |
| `:Branch-SDK-TestBed` | sample app (`io.branch.branchandroidtestbed`), depends on `:Branch-SDK`. Target app for E2E |
| `:Branch-SDK-GPTDriver` | `com.android.test` E2E module targeting the TestBed. Hybrid philosophy: deterministic Espresso first, AI-assisted validation only when Espresso matchers cannot express the intent. See `Branch-SDK-GPTDriver/README.md` |

## Packages

Code spans several `io/branch/*` packages, not just `referral/`.

- **`referral/`** the SDK core: `Branch`, `PrefHelper`, `ServerRequest*`, the coroutine queue (`BranchRequestQueue.kt`, `BranchRequestQueueAdapter.kt`), `ModernLinkGenerator.kt`, `BranchSessionState*.kt`, device, tracking, and config.
  - `referral/network/` HTTP, including `BranchAsyncNetworkLayer.kt`
  - `referral/util/` `BranchEvent`, `LinkProperties`, `CommerceEvent`, content-metadata types
  - `referral/validators/`, `referral/QRCode/`
  - `referral/modernization/` `BranchApiPreservationManager.kt`, `registry/PublicApiRegistry.kt`, `wrappers/PreservedBranchApi.kt` and `LegacyBranchWrapper.kt` (legacy API shims), `core/ModernBranchCore.kt` (the new reactive session entry point), `adapters/CallbackAdapterRegistry.kt`, `analytics/ApiUsageAnalytics.kt`
- **`coroutines/`** async fetch entry points (`AdvertisingIds`, `DeviceSignals`, `InstallReferrers`) **and** the newer coroutine request classes `RequestOpen.kt` (`v3/events/open`) and `RequestDeepLink.kt` (`v3/deeplink`)
- **`observers/`** (a directory under `io.branch`, but the classes declare `package io.branch.referral`) `BranchProcessLifecycleObserver.kt`, process-level foreground detection driving OPEN
- **`indexing/`** `BranchUniversalObject`, the BUO content model
- **`data/`** `InstallReferrerResult`
- **`interfaces/`** public callback interfaces, for example `IBranchLoggingCallbacks`
- **`receivers/`** `SharingBroadcastReceiver`

## Where to make changes

| Task | Start here |
| --- | --- |
| Session init, deep-link callbacks, intent parsing | mechanism in `Branch.java` (`initializeSession`, `registerAppInit`, `readAndStripParam`, `onIntentReady`); new entry point in `modernization/core/ModernBranchCore.kt` (`SessionManager.initSession`). The legacy `sessionBuilder`/`InitSessionBuilder` is being retired, do not extend it |
| OPEN, foreground re-open, attribution gating | `Branch.sendOpen(...)` plus `observers/BranchProcessLifecycleObserver.kt`; request in `coroutines/RequestOpen.kt` |
| Manual deep-link resolution (`v3/deeplink`) | `Branch.requestDeepLinkData(...)` plus `coroutines/RequestDeepLink.kt` |
| Request queueing, wait locks, retry behavior | `BranchRequestQueue.kt` and `BranchRequestQueueAdapter.kt`. **Not** `ServerRequestQueue.java`, which is orphaned |
| A new API request type, or changing a request body | subclass `ServerRequest`; route it in the adapter and queue; add the path to `Defines.RequestPath` |
| Session state and lifecycle | `BranchSessionState.kt`, `BranchSessionStateManager.kt`, `BranchSessionStateProvider.kt` |
| New async or coroutine work | follow the `ModernLinkGenerator.kt` idiom |
| Persisted state, a new SharedPreferences key | `PrefHelper.java`: `KEY_*` plus typed accessors; distinguish unset with `hasPrefValue` |
| Wire-format field names, enums, endpoints | `Defines.java`: `Jsonkey`, `RequestPath`, `IntentKeys`, `HeaderKey` |
| Link creation | `ModernLinkGenerator.kt`, `BranchShortLinkBuilder.java`, `BranchUrlBuilder.java`; content model in `indexing/BranchUniversalObject.java` and `util/LinkProperties.java` |
| Custom or commerce events | `util/BranchEvent.java`, `util/BRANCH_STANDARD_EVENT.java`, `util/CommerceEvent.java` |
| Ad ID, install referrer, device signals | `coroutines/AdvertisingIds.kt`, `coroutines/InstallReferrers.kt`, `coroutines/DeviceSignals.kt`, `DeviceInfo.java` |
| Tracking-disabled, consent, DMA | `TrackingController.java` plus the DMA and consent keys in `PrefHelper.java` |
| `branch.json` and config flags | `BranchJsonConfig.java`, `BranchConfigurationController.kt`, `BranchUtil.java` for key resolution |
| HTTP transport, retries, timeouts | `network/BranchRemoteInterfaceUrlConnection.java`, `network/BranchAsyncNetworkLayer.kt` |
| Logging | `BranchLogger.kt`, routing internal trace through `.v()` |
