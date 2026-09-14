# What differs from `master`, and what is broken

> Contributor-facing notes for work on this branch.
> Last updated 2026-09-03, against `6.0.0-beta.0` at 88b3bf28. Every defect below was verified at that commit; re-check before relying on one.

The single most useful page on this branch. Everything here is verifiable in source on `6.0.0-beta.0`.

## Request queue: the most-changed subsystem

Live path:

```text
Branch.java  (requestQueue_ : BranchRequestQueueAdapter)          Branch.java:217, assigned :328
  -> BranchRequestQueueAdapter.kt   (handleNewRequest, unlockProcessWait; reimplements session gating)
    -> BranchRequestQueue.kt        (synchronized List + Channel<Unit> wake-up; single consumer coroutine)
```

**`ServerRequestQueue.java` is orphaned.** It is not on the live path. The only remaining references to it in `src/main` are in comments. It is kept compiling solely for instrumented-test consumers (`ServerRequestTests`, `BranchCPIDTest`, `BranchEventTest`, `BranchPreinstallFileTest`, `BillingGooglePlayTests`, `BranchTestRequestUtil`, `BranchTest`). Do not add production code against it.

**Storage is `Collections.synchronizedList(...)`** (`BranchRequestQueue.kt:109`). The `Channel<Unit>` (`:110`) is only a wake-up signal, not the queue itself. There is **no `Mutex` or `Semaphore`**: master's single-in-flight `Semaphore(1)` guarantee is not enforced here. Serialization is incidental, from one consumer draining the FIFO head.

**Wait locks are still a plain `HashSet<PROCESS_WAIT_LOCK>`** on each `ServerRequest` (`ServerRequest.java:52-57,94`), and the consumer **polls** for locks to clear rather than being woken on release. Retry and timeout use `RequestRetryInfo` on the monotonic clock, with time-based force-unlock heuristics `tryResolveStuckLocks`, `tryResolveStuckSdkInitLock`, and `tryResolveStuckUserAgentLock`.

Constants (`BranchRequestQueue.kt:66-72`): `MAX_ITEMS = 25`, `MAX_RETRY_ATTEMPTS = 5`, `REQUEST_TIMEOUT_MS = 30_000L`, `RETRY_DELAY_MS = 100L`.

**"Needs a session" logic is duplicated** across `ServerRequestQueue.java`, `BranchRequestQueue.kt`, and `BranchRequestQueueAdapter.kt`, and the copies have diverged. Reconcile all of them if you change gating.

### Invariants any queue change must preserve

Both were broken once already. `git log` carries the detail.

- `eb040576`: `onIntentReady()` must read and persist intent params (`readAndStripParam`) **before** releasing `INTENT_PENDING_WAIT_LOCK`, or cold-start deep-link attribution is dropped.
- `e7e46854`: the retry-count ceiling must apply **only** to requests that are not waiting on a lock. Lock-waiters may fail only via the 30s timeout. `tryResolveStuckLocks` can force-remove a lock at roughly the 10s window; if that was the request's last lock, the retry ceiling applies again. It does not cover `USER_SET_WAIT_LOCK`. `shouldFailRequest()` (`BranchRequestQueue.kt:376+`) encodes this. Without it, 5 attempts at 100 ms force-fails a waiting request after roughly 500 ms.

### Known live bug: `withDelay()`

`USER_SET_WAIT_LOCK` is added by `withDelay()` (`Branch.java:1394`) but has **no removal site** anywhere in `src/main`. `removeSessionInitializationDelay()`, its historical owner, no longer exists on this branch (`git grep` returns zero hits in `Branch-SDK/src`). No stuck-lock resolver handles it either.

Net effect: a delayed `init()` hangs until the 30s timeout, then fails. `withDelay()` is effectively broken here. A fix needs to give the lock a real owner, or model the delay without a dangling lock.

## New request paths

Know these before touching OPEN or attribution behavior.

**`sendOpen()` / `sendOpen(JSONObject responseData)`** (`Branch.java:2414`, `:2428`) enqueue a `RequestOpen` targeting `v3/events/open` (`Defines.RequestPath.EventsOpen`, `coroutines/RequestOpen.kt`). This is not the legacy `v1/open` `RegisterOpen` path, which still exists separately. Both overloads fire **only when the consumer-protection attribution level is not `NONE`**; both check `getConsumerProtectionAttributionLevel()` first.

Callers:
1. `setConsumerProtectionAttributionLevel(level)` when re-enabling attribution
2. `BranchProcessLifecycleObserver.onStart` (`observers/BranchProcessLifecycleObserver.kt`). Foreground OPENs are now driven by AndroidX `ProcessLifecycleOwner`, which fires only on real process foreground, not on config-change recreation such as fold, rotate, or multi-window. This removes duplicate OPENs by construction.
3. after a successful `RequestDeepLink`

**`requestDeepLinkData(uri, callback)`** (`Branch.java:2726`, public) manually resolves a URI. It builds a `RequestDeepLink` (`coroutines/RequestDeepLink.kt`, a `ServerRequestInitSession` subclass) hitting the new `v3/deeplink` endpoint (`Defines.RequestPath.Deeplink`) and routes it through `requestQueue_.handleNewRequest(...)`. It maps `link_click_id`, app-link-url, and scheme-uri into the POST. On success it writes `sessionParams`, fires the callback with `latestReferringParams`, and, when attribution is not `NONE`, chains a `sendOpen(response)`. It is coroutine-friendly and intended to be called from a `LifecycleScope`.

## Other beta subsystems

- **`ModernLinkGenerator.kt`** is the coroutine link-creation path replacing the AsyncTask pattern, and it establishes the branch's async idiom: per-class `CoroutineScope(SupervisorJob() + Dispatchers.IO)` with a `shutdown()`, `withTimeout` for cancellation, a single `runBlocking` or `scope.launch { ... withContext(Main) { callback } }` bridge for Java callers, and sealed exceptions mapped back to `BranchError`. **Reuse this idiom** for new async code.
- **`BranchSessionState*.kt`** is the sealed session state (`Uninitialized`, `Initializing`, `Initialized`, `Resetting`, `Failed`) in a `MutableStateFlow` (`BranchSessionStateManager`, `BranchSessionStateProvider`). The StateFlow is currently consumed via `.value`, `instanceof`, and manual listeners, not reactively collected.
- **`network/BranchAsyncNetworkLayer.kt`** is the coroutine network layer with non-blocking `delay()` backoff.
- **`modernization/`** holds `PublicApiRegistry`, `wrappers/PreservedBranchApi.kt` (legacy API shims), and `core/ModernBranchCore.kt` (the new reactive session entry point).
- **Logging.** `BranchLogger` has levels `ERROR` through `VERBOSE` and no separate trace channel. Per `604fd770`, route internal and queue trace through the existing `.v()` level. **Do not add a second logging gate.** Trace lines are prose with an all-caps label, for example `STUCK_LOCK_DETECTION:`, not `key=value` pairs, so grepping a field name will not find them.

## Public API diff is report-only

`apiCompatibilityReport` (`Branch-SDK/build.gradle.kts:418`, `bf422f32`) diffs the public API against a hardcoded last-5.x-release Maven coordinate. It is **not wired into any workflow** and does not fail the build without `-PapiDiffStrict`. There is no baseline artifact to update. Running it is informational. Intentional API changes are expected on the beta.

## Removed and restored APIs

Check `git log` before assuming an API's state. `reInit()` and `isReInitializing` were removed from `InitSessionBuilder`. Some 5.x source-compat aliases were deliberately restored earlier in the beta: the no-arg `Branch.logout()`, a relocated LATD listener alias, and the synchronous deep-link param getters.
