# Code map (master)

> Contributor-facing notes for work on this branch.
> Last updated 2026-09-03, against `master` at 827655ea.

Where things live, and which file to open for a given task. Line numbers drift; treat any `file:line` as a locate-then-confirm hint.

## Modules

| Module | What it is |
| --- | --- |
| `:Branch-SDK` | the published library (`com.android.library`). All production code under `Branch-SDK/src/main/java/io/branch/` |
| `:Branch-SDK-TestBed` | sample/demo app (`io.branch.branchandroidtestbed`), depends on `:Branch-SDK`. Manual testing, and the target app for E2E |
| `:Branch-SDK-GPTDriver` | `com.android.test` E2E module targeting the TestBed. Hybrid philosophy: deterministic Espresso first, AI-assisted validation only when Espresso matchers cannot express the intent. See `Branch-SDK-GPTDriver/README.md` |

## Packages

Most code is in `io.branch.referral`, but not all. Check these siblings before assuming.

- **`referral/`** the SDK core: `Branch`, `PrefHelper`, `ServerRequest*` and `ServerRequestQueue`, device/tracking/config, link and share builders.
  - `referral/network/` `BranchRemoteInterface` plus the `...UrlConnection` HTTP implementation
  - `referral/util/` `BranchEvent`, `BRANCH_STANDARD_EVENT`, `LinkProperties`, `CommerceEvent`, content-metadata types
  - `referral/validators/` integration and deep-link diagnostics
  - `referral/QRCode/`
- **`indexing/`** `BranchUniversalObject`, the BUO content model used for link creation and tracking
- **`coroutines/`** Kotlin entry points for async fetches: `AdvertisingIds`, `DeviceSignals`, `InstallReferrers`
- **`data/`** `InstallReferrerResult`
- **`interfaces/`** public callback interfaces, for example `IBranchLoggingCallbacks`
- **`receivers/`** `SharingBroadcastReceiver`, captures the chosen app from the system share sheet

## Where to make changes

| Task | Start here |
| --- | --- |
| Session init, deep-link callbacks, intent parsing | `Branch.java`: `sessionBuilder`, `initializeSession`, `registerAppInit`, `readAndStripParam` |
| A new API request type, or changing a request body | subclass `ServerRequest`; wire dispatch and gating in `ServerRequestQueue.java`; add the path to `Defines.RequestPath` |
| Persisted state, a new SharedPreferences key | `PrefHelper.java`: add `KEY_*` plus typed accessors |
| Wire-format field names, enums, endpoints | `Defines.java`: `Jsonkey`, `RequestPath`, `IntentKeys`, `HeaderKey` |
| Device or hardware signals on requests | `DeviceInfo.java`, `SystemObserver.java`, `coroutines/DeviceSignals.kt` |
| Ad ID fetch (GAID, Huawei, and so on) | `coroutines/AdvertisingIds.kt` |
| Install-referrer fetch | `coroutines/InstallReferrers.kt`, `data/InstallReferrerResult.kt`, `AppStoreReferrer.java`, `BranchPreinstall.java` |
| Link creation (short and long URLs) | `BranchShortLinkBuilder.java`, `BranchUrlBuilder.java`, `BranchLinkData.java`; content model in `indexing/BranchUniversalObject.java` and `util/LinkProperties.java` |
| Sharing and the share sheet | `BranchShareSheetBuilder.java`, `ShareLinkManager.java`, `NativeShareLinkManager.java`, `receivers/SharingBroadcastReceiver.kt` |
| Custom or commerce events | `util/BranchEvent.java`, `util/BRANCH_STANDARD_EVENT.java`, `util/CommerceEvent.java` |
| Tracking-disabled, consent, DMA | `TrackingController.java`, plus the DMA and consent keys in `PrefHelper.java` |
| `branch.json` and config flags | `BranchJsonConfig.java`, `BranchConfigurationController.kt`, `BranchUtil.java` for key resolution |
| HTTP transport, retries, timeouts | `network/BranchRemoteInterfaceUrlConnection.java` (impl), `network/BranchRemoteInterface.java` (abstract) |
| QR codes | `QRCode/BranchQRCode.java`, `QRCode/ServerRequestCreateQRCode.java` |
| Integration and deep-link diagnostics | `validators/IntegrationValidator.java`, `validators/DeepLinkRoutingValidator.java` |
| Logging | `BranchLogger.kt`, `interfaces/IBranchLoggingCallbacks.java` |
