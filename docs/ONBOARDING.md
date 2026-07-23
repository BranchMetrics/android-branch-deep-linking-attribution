# Branch Android SDK — Developer Onboarding

Welcome to the Branch Android SDK. This document gives you the mental model and practical knowledge to start contributing confidently.

---

## What this SDK does

Branch is a mobile deep linking and attribution platform. This SDK, embedded in Android apps, does three things:

1. **Captures deep links** — when a user taps a Branch link (in email, ads, social media, etc.) and your app opens, the SDK retrieves the link's metadata so you can route the user to the right screen.
2. **Attributes installs and opens** — Branch determines which campaign, channel, or link caused the user to install or open the app.
3. **Logs events** — purchases, sign-ups, and other actions are reported back to Branch for analytics and retargeting.

---

## Repository layout

```
android-branch-deep-linking-attribution/
├── Branch-SDK/              ← The SDK library you'll work in most of the time
│   └── src/
│       ├── main/java/io/branch/referral/   ← Core SDK code
│       ├── test/            ← JVM unit tests (Mockito)
│       └── androidTest/     ← Instrumented device/emulator tests
├── Branch-SDK-TestBed/      ← A sample Android app that exercises the SDK
├── Branch-SDK-GPTDriver/    ← AI-driven end-to-end test harness
├── docs/                    ← Documentation (you are here)
└── ARCHITECTURE.md          ← Full technical reference
```

The only module you'll normally change is `Branch-SDK`. `TestBed` is useful for manually verifying your changes against a real app flow.

---

## The mental model: three jobs, three layers

```
┌─────────────────────────────────────┐
│           Your App Code             │  ← calls Branch.sessionBuilder().init()
└──────────────────┬──────────────────┘
                   │
┌──────────────────▼──────────────────┐
│         Session & Attribution       │  ← Branch.java, BranchActivityLifecycleObserver
│   "What caused this install/open?"  │    ServerRequestRegisterInstall/Open
└──────────────────┬──────────────────┘
                   │
┌──────────────────▼──────────────────┐
│         Network & Persistence       │  ← ServerRequestQueue, BranchRemoteInterface,
│   "Send it reliably; remember it"   │    PrefHelper
└─────────────────────────────────────┘
```

---

## How a session works, step by step

**Step 1 — App launch** (`Application.onCreate()`)

The app calls `Branch.getAutoInstance(context)`. This creates the SDK singleton and registers a lifecycle observer that watches every Activity in the app.

**Step 2 — Activity start** (`Activity.onStart()`)

The developer calls:
```java
Branch.sessionBuilder(this)
    .withCallback((params, error) -> { /* handle deep link params */ })
    .init();
```

**Step 3 — Intent inspection**

The SDK inspects the `Intent` that launched the Activity. If it came from tapping a Branch link, the link's click ID is extracted from the URI. If the `Intent` already contains pre-fetched link data (`branch_data`), the callback fires immediately without waiting for the network — this is called **Instant Deep Link (IDL)**.

**Step 4 — Network request**

The SDK sends either a `v1/install` (first-ever launch) or `v1/open` (subsequent launch) request to Branch servers, including device info, timestamps, the link click ID, and any install referrer data from the Play Store.

**Step 5 — Callback**

The server responds with the link parameters (e.g., `{"$deeplink_path": "products/123", "campaign": "summer_sale"}`). These are persisted locally and delivered to the callback registered in step 2.

---

## Key classes to know

**`Branch.java`** — The public face of the SDK. It's a singleton. Nearly every SDK feature is accessed through it. When you don't know where something is, start here.

**`PrefHelper.java`** — Everything the SDK needs to remember between launches is stored here using Android SharedPreferences. Device tokens, session IDs, last deep-link data, privacy settings — it's all in this class.

**`ServerRequest.java`** (abstract) — A base class for every network call the SDK makes. Each request type (install, open, event, create link) is a subclass. If you're adding a new server interaction, you'll create a new subclass here.

**`ServerRequestQueue.java`** — Manages outgoing requests. Requests are queued in memory and also persisted to disk so they survive process restarts. Only one request is sent to the network at a time.

**`BranchRemoteInterface.java`** — The HTTP abstraction. The real implementation uses `HttpsURLConnection`. In tests, it's replaced with `MockRemoteInterface` that returns canned responses.

**`BranchActivityLifecycleObserver.java`** — Watches every Activity's lifecycle globally. It's how the SDK knows when the app goes to the foreground or background without requiring developers to call `onStart`/`onStop` manually.

---

## How attribution works

When a user installs the app after tapping a Branch link, here's the chain:

1. The Branch link click is recorded on Branch servers, producing a `link_click_id`.
2. The Play Store receives the click ID via the install referrer mechanism.
3. When the app first launches, `AppStoreReferrer` reads the referrer string from the Play Store.
4. The `v1/install` request carries that click ID to Branch servers.
5. Branch servers match the click to the original link and return the link's metadata.

For subsequent opens (not installs), the flow is the same but uses `v1/open` and typically relies on the URI in the Intent rather than the Play Store referrer.

**Install vs. update vs. open** is detected in `ServerRequestInitSession.updateInstallStateAndTimestamps()` by checking whether `PrefHelper` has a stored app version. No stored version = fresh install.

---

## Privacy and tracking

The SDK supports four levels of data collection, controlled by `Branch.setConsumerProtectionAttributionLevel()`:

| Level | What gets sent |
|-------|---------------|
| `FULL` | Everything — advertising IDs, device IDs, full analytics |
| `REDUCED` | Device IDs only, no advertising IDs |
| `MINIMAL` | Device IDs and IP only |
| `NONE` | Deep linking only — no analytics at all (GDPR/CCPA mode) |

`TrackingController` enforces this at the queue level. Deep-link init requests are always allowed through. Analytics/event requests are gated.

---

## Running the tests

**JVM unit tests** (fast, no device needed):
```bash
./gradlew :Branch-SDK:test
```

**Instrumented tests** (require a connected device or emulator):
```bash
./gradlew :Branch-SDK:connectedAndroidTest
```

Tests use `MockRemoteInterface` to avoid real network calls. The abstract `BranchTest` base class resets all SDK state (SharedPreferences, in-memory queue, singleton) before each test so tests don't bleed into each other.

To add a new instrumented test: extend `BranchTest`, your setup is handled by the base class.

---

## Common tasks

**I want to add a new field to the install/open request**
→ Edit `ServerRequestRegisterInstall` or `ServerRequestRegisterOpen`. Look at `doFinalUpdateOnMainThread()` or `doFinalUpdateOnBackgroundThread()` for where fields are appended.

**I want to add a new SharedPreferences value**
→ Add a constant key and getter/setter in `PrefHelper.java`.

**I want to add a new public API method on `Branch`**
→ Add it to `Branch.java`. If it involves a network call, create a new `ServerRequest` subclass and add the path to `Defines.RequestPath`.

**I want to understand why a request isn't being sent**
→ Check `TrackingController.isTrackingDisabled()` and the `PROCESS_WAIT_LOCK` set on the request. Both can block execution.

**I want to trace what's happening in a running app**
→ Call `Branch.enableLogging()` in your test app. Or implement `IBranchRequestTracingCallback` to intercept request/response pairs.

**I want to test with simulated network responses**
→ Implement `BranchRemoteInterface` and inject it via `Branch.setBranchRemoteInterface(yourImpl)`. See `MockRemoteInterface.java` for an example.

---

## Things that will surprise you

- **IDL fires the callback before the network completes.** The callback can fire twice for a single session: once immediately from cached data (IDL), and again when the server confirms. Design your deep-link routing accordingly.
- **`bnc_install_params` is write-once.** The first attributed install's deep-link data is persisted forever (until `logout()`). `getFirstReferringParams()` always returns this. It is never overwritten by a subsequent install of the same app.
- **Requests are always serialized.** There is never more than one in-flight HTTP call. If you're adding async work, be aware that it all queues behind everything else.
- **Plugin runtime deferral is a real state.** If `Branch.deferInitForPluginRuntime(true)` was called (by React Native, Flutter, etc.), `sessionBuilder().init()` does nothing until `Branch.notifyNativeToInit()` is called. This can look like the SDK is broken when it's actually waiting.
- **Two separate SharedPreferences files.** `PrefHelper` uses `"branch_referral_shared_pref"`. `ServerRequestQueue` uses `"BNC_Server_Request_Queue"`. They are independent. Clearing one does not affect the other.

---

## Further reading

- `ARCHITECTURE.md` — exhaustive technical reference with all design patterns and extension points
- `Branch-SDK/src/androidTest/java/io/branch/referral/` — tests are often the best documentation for specific behaviors
