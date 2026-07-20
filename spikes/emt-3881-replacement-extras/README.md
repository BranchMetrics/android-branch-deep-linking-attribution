# EMT-3881 spike: does the OS chooser honor `EXTRA_REPLACEMENT_EXTRAS`?

Throwaway diagnostic for the "Option B" ADR (restore per-target `~channel`
attribution on Android's native share sheet via `Intent.EXTRA_REPLACEMENT_EXTRAS`).
This is **not** part of the Branch SDK build — it is a standalone two-app Gradle
project, deliberately not wired into the repo root `settings.gradle.kts`.

## What Option B assumes

AOSP's `com.android.internal.app.ChooserActivity` reads
`EXTRA_REPLACEMENT_EXTRAS` (a `Bundle` keyed by target package name, each value
a `Bundle` of extras to merge in) and does a plain `result.putExtras(replExtras)`
before launching the chosen target — `EXTRA_TEXT` is not special-cased, so
overriding it per target works on stock AOSP.

**The blocking risk this spike exists to answer:** `config_chooserActivity` is
an official OEM override point, and on Android 13+ the chooser ships as the
Play-updatable `IntentResolver` mainline module, independent of OS version.
There is no AOSP end-to-end test for this path and no evidence either way for
Samsung One UI or Xiaomi HyperOS.

## The two apps

- **`sender`** (`io.branch.spike.sender`) — button fires
  `Intent.createChooser` with an `ACTION_SEND` intent. Baseline `EXTRA_TEXT` is
  `SPIKE-BASELINE (no per-target override applied)`. `EXTRA_REPLACEMENT_EXTRAS`
  maps 5 target packages (the receiver app below, plus `com.whatsapp`,
  `com.facebook.katana`, `com.google.android.gm`, `com.instagram.android`) to
  distinct values shaped `SPIKE|pkg=<package>|token=<uuid>`. The exact expected
  mapping is logged to Logcat (`EMT3881_SPIKE_TX`) and shown on screen before
  the chooser opens, so you can compare it to what the receiver actually got.
  Also has an N-parallel-HTTP-request latency probe (see below).

- **`receiver`** (`io.branch.spike.receiver`) — declares an `ACTION_SEND` /
  `text/plain` intent filter so it shows up in the chooser. On receipt it logs
  (`EMT3881_SPIKE_RX`) and displays on screen the exact `EXTRA_TEXT` it got.
  This is the oracle: baseline text = chooser did **not** honor the API for
  this target; `SPIKE|pkg=io.branch.spike.receiver|token=...` = it did.

Only the receiver app can be conclusively checked this way (we control it).
The real-world targets (WhatsApp, Gmail, etc.) can only be checked by eye —
open the conversation/compose screen the target lands you on and see whether
the pre-filled text is the baseline or the replacement string.

## Build & install

```bash
cd spikes/emt-3881-replacement-extras
./gradlew assembleDebug
adb install -r sender/build/outputs/apk/debug/sender-debug.apk
adb install -r receiver/build/outputs/apk/debug/receiver-debug.apk
```

Requires `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) set; no `local.properties`
needed. Uses AGP 8.12.2 / Kotlin 1.9.24 / compileSdk & targetSdk 35, minSdk 26
— independent of the SDK's own compat matrix since this never ships.

## Test procedure (per device)

1. Install both APKs. Also install (or already have installed) as many of the
   real-world targets as you can — WhatsApp, Gmail, Instagram, Facebook — for
   the visual check.
2. Launch **Spike Sender**, tap **"Send with replacement extras"**.
3. Note the on-screen expected mapping (also in `adb logcat -s EMT3881_SPIKE_TX`).
4. From the chooser, pick **"EMT-3881 Spike Receiver"**. Confirm on-screen /
   in `adb logcat -s EMT3881_SPIKE_RX` whether the received `EXTRA_TEXT`
   matches the expected replacement or the baseline.
5. Repeat step 2, this time picking WhatsApp / Gmail / Instagram / Facebook
   from the chooser (one run per target — the chooser only delivers to the one
   target you pick). Visually confirm whether the pre-filled compose text is
   the replacement or the baseline string.
6. Record results in the table below.

## Latency probe (secondary, lower-priority open question)

The sender's second section fires **N** (default 8, configurable) concurrent
GET requests against a configurable endpoint and reports wall-clock, p50, and
p95 per batch. **This is a proxy measurement for "how long would it take to
mint N per-target short links before the sheet can open" — it does NOT call
the real Branch link-creation API.** The default endpoint
(`https://httpbin.org/get`) is a generic public echo service; for a number
that means something, point it at:

- a stub/echo server you control with response-time characteristics similar to
  Branch's short-link creation endpoint, or
- (if authorized and rate-limit-safe) a real Branch API endpoint hit with a
  throwaway/test key.

Treat the number this produces as a shape, not a fact — do not report it as
"Branch link-creation latency" in the ADR without that caveat attached.

## Device matrix

Run the chooser test (and ideally the latency probe once, to sanity-check it
doesn't itself introduce jank) on:

| Device                         | OS version      | Chooser (IntentResolver) module version      | Honored? (yes/no) | Notes                                                                                                                                        |
| ------------------------------ | --------------- | -------------------------------------------- | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Pixel emulator/device          | API 29          | N/A (pre-mainline chooser)                   |                   | not run — no system image locally                                                                                                            |
| Pixel emulator/device          | API 31          |                                              |                   | not run — no system image locally                                                                                                            |
| Pixel emulator/device          | API 33          |                                              |                   | not run — no system image locally                                                                                                            |
| **Pixel 8 Pro emulator (AVD)** | **14 / API 34** | **`2021-11`** (`com.android.intentresolver`) | **YES**           | 2026-07-20. Token matched exactly, twice, with fresh tokens per run. Chooser was the mainline `com.android.intentresolver/.ChooserActivity`. |
| Pixel emulator/device          | API 35          |                                              |                   | not run — no system image locally                                                                                                            |
| **Pixel 8 Pro emulator (AVD)** | **17 / API 37** | **`2021-11`** (`com.android.intentresolver`) | **YES**           | 2026-07-20. Token matched exactly. Beyond the originally planned matrix.                                                                     |
| Samsung (One UI)               |                 |                                              |                   | **NOT RUN — the blocking gate.** Needs physical hardware.                                                                                    |
| Xiaomi (HyperOS)               |                 |                                              |                   | **NOT RUN — the blocking gate.** Needs physical hardware.                                                                                    |

### Results so far (2026-07-20)

Two emulator rows pass. **The gate is still open**, because emulators run the AOSP
chooser and the unresolved risk is precisely the OEM forks that replace it via
`config_chooserActivity`. What the emulator runs _did_ buy:

- The AOSP path is now confirmed **empirically**, not only by reading `ChooserActivity` source.
- It holds on the Play-updatable `IntentResolver` mainline module, which the ADR
  flagged as untested code updating independently of the OS.
- It holds on API 37, well past the originally planned ceiling of API 35.

**Two distinct verifications, do not conflate them:**

- _This table_ is the **harness** (`minSdk 26`) proving the chooser honors
  `EXTRA_REPLACEMENT_EXTRAS`. It ran on API 34 and API 37.
- Separately, the **Branch SDK's own** per-target generation was exercised on device via
  `PerTargetChannelInstrumentedTest` (`Branch-SDK/src/androidTest/`). That runs on **API 34
  only** — the SDK is `minSdk 21`, and API 37 rejects installing a `minSdk < 24` APK
  (`INSTALL_FAILED_DEPRECATED_SDK_VERSION`). So the SDK-path device evidence is API 34; the
  harness-path evidence extends to API 37.

Controls run alongside, so the PASS is not self-fulfilling:

| Control                                             | Purpose                                                                              | Result                                        |
| --------------------------------------------------- | ------------------------------------------------------------------------------------ | --------------------------------------------- |
| Receiver launched directly with an arbitrary string | Prove the receiver echoes what it receives rather than hardcoding the expected value | Displayed `CONTROL-STRING-XYZ789` ✅          |
| Spike re-run, fresh UUID token                      | Prove the value flows per-run rather than being cached                               | New token `b1650b97` propagated end to end ✅ |

Reproduce with `run-spike.sh` (see the repo scratch notes) or the manual steps below.

> **Note on the receiver not appearing in the chooser.** An app installed via `adb`
> and never opened stays `FLAG_STOPPED` and is excluded from implicit intent
> resolution, so it will be missing from the chooser entirely. Launch the receiver
> once by hand before the first run. This cost real debugging time.

### Recording the chooser module version (Android 13+)

On Android 13+ the chooser is the Play-updatable `IntentResolver` mainline
module — it updates independently of the OS version, so a "yes" result is only
valid for the specific module version tested. Record it per device:

```bash
adb shell pm list packages --show-versioncode | grep -i intentresolver
# fallback / more detail if the above returns nothing:
adb shell dumpsys package com.google.android.intentresolver | grep -i versionCode
```

On API < 33 (or devices without the mainline module), note "N/A (pre-mainline
chooser)" in that column instead.

## Pass/fail criterion

**PASS** requires the receiver app to show the replacement text on **every**
device/OS combination in the matrix above, AND the manual visual check to show
replaced text on at least the Pixel rows for the real-world targets tested
(WhatsApp/Gmail at minimum — full coverage across all 4 real-world targets on
every device is best-effort, not required for pass).

**FAIL** — if `EXTRA_REPLACEMENT_EXTRAS` is not honored on ANY device in the
matrix (i.e. the receiver shows baseline text instead of the replacement, or
any real-world target shows unreplaced text), Option B is not viable as a
universal solution. **A FAIL result means the ADR falls back to Option A.**

## Running the gate on a physical device (step by step)

The emulator rows are done (AOSP chooser). What remains is the OEM gate — a
real Samsung (One UI) and a real Xiaomi (HyperOS), the choosers this harness
cannot simulate. Run these steps on each physical device.

1. **Check out the branch.**
   `git checkout feat/EMT-3881-per-target-channel`
   (or `git fetch origin feat/EMT-3881-per-target-channel && git checkout feat/EMT-3881-per-target-channel`).

2. **Build both spike APKs.**

   ```bash
   cd spikes/emt-3881-replacement-extras
   ./gradlew assembleDebug
   ```

   Produces `sender/build/outputs/apk/debug/sender-debug.apk` and
   `receiver/build/outputs/apk/debug/receiver-debug.apk`.

3. **Enable USB debugging on the device.** Settings → About phone → tap Build
   number 7×, then Developer options → enable USB debugging. On Xiaomi/HyperOS
   also enable "Install via USB" and "USB debugging (Security settings)".

4. **Connect and authorize.** Plug in over USB, accept the "Allow USB debugging"
   prompt on the phone, then confirm exactly one device is attached:
   `adb devices -l` (should list your phone, not `unauthorized`).

5. **Record the chooser module version** (the result is only valid for it):
   `adb shell dumpsys package com.google.android.intentresolver | grep -i versionName`
   (or the `com.android.intentresolver` variant). Note it, plus
   `adb shell getprop ro.build.version.release` and the One UI / HyperOS version
   from Settings.

6. **Install both APKs.**

   ```bash
   adb install -r -t receiver/build/outputs/apk/debug/receiver-debug.apk
   adb install -r -t sender/build/outputs/apk/debug/sender-debug.apk
   ```

7. **Open the receiver once, by hand.** Launch "EMT-3881 Spike Receiver" from the
   app drawer, then press Back. An app installed but never opened stays
   `FLAG_STOPPED` and will NOT appear in the chooser — this step is mandatory.

8. **Start capturing logs** in a terminal:
   `adb logcat -c && adb logcat -s EMT3881_SPIKE_TX:I EMT3881_SPIKE_RX:I EMT3881_SPIKE_LATENCY:I`

9. **Fire the chooser.** Open "EMT-3881 Spike Sender", tap **SEND WITH REPLACEMENT
   EXTRAS**. The screen (and the `EMT3881_SPIKE_TX` logs) show the baseline plus
   the expected per-package tokens.

10. **Pick the receiver in the share sheet.** Choose "EMT-3881 Spike Receiver"
    (scroll the target list if needed).

11. **Read the verdict.** The receiver screen shows `EXTRA_TEXT = …`, mirrored in
    the `EMT3881_SPIKE_RX` log:
    - `SPIKE|pkg=io.branch.spike.receiver|token=…` **→ PASS** (chooser honored the
      per-target override; the token must match the sender's `io.branch.spike.receiver`
      line from step 9).
    - `SPIKE-BASELINE (no per-target override applied)` **→ FAIL** (chooser ignored
      it — the gate fails on this device, ADR falls back to Option A).

12. **Run the two negative controls** so a PASS isn't self-fulfilling:
    - **Echo control:** `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "CONTROL-XYZ" -n io.branch.spike.receiver/.ReceiverActivity`
      → the receiver must display `CONTROL-XYZ` (proves it echoes, not hardcodes).
    - **Fresh-token control:** repeat steps 8–11; the token in step 11 must be a
      NEW value each run (proves the value flows per-run, not cached).

13. **Real-world target visual check (best-effort).** With WhatsApp/Gmail
    installed, fire the chooser again and pick one; confirm the pasted/received
    text carries `pkg=<that package>` rather than the baseline. These apps don't
    expose the raw text as cleanly as the receiver, so treat this as a visual
    sanity check, not the oracle.

14. **Latency probe (only if measuring LONG vs SHORT).** On the sender, set the
    endpoint field to the REAL Branch link-creation endpoint (not the default
    `httpbin.org`), set N to a realistic target count, tap **RUN LATENCY PROBE**,
    read p50/p95 from the screen or `EMT3881_SPIKE_LATENCY` log.

15. **Record the result** in the device matrix table above (OS version, chooser
    module version, honored yes/no, notes) and report it on PR #1379. All matrix
    rows PASS → the gate is closed and the flag can be enabled by default in a
    separate change. Any row FAILs → ADR falls back to Option A.
