# L1 wire-validation scripts

`validate_l1_logs.py` is the Layer-1 PR gate that asserts the Android SDK is
putting the right device/SDK fields on the wire. It is run by
`.github/workflows/sdk-l1-validation.yml` against a `branchlogs.txt` produced
by the L1 instrumented test, and can be run locally against any captured log.

## Running locally

After capturing a `branchlogs.txt` (from a CI artifact, or by running the
instrumented test by hand), point the validator at it:

```bash
python3 scripts/validate_l1_logs.py path/to/branchlogs.txt
```

To run the validator's own test suite:

```bash
python3 -m unittest scripts.test_validate_l1_logs -v
```

To check a capture against a named scenario contract (see below), pass
`--scenario`:

```bash
python3 scripts/validate_l1_logs.py branchlogs.txt --scenario harness
```

## What gets validated

Presence-only. A required field is either there (pass) or absent (fail). No
type checks, no value-format checks — those are intentionally left to the
backend ingestion gate.

No endpoint is globally mandatory — a capture can legitimately contain no
init at all (e.g. a standalone link-creation call). The one thing always
flagged as an error is `/v1/install`: init no longer uses it, on the beta or
otherwise, so seeing it means the fix regressed. What each scenario must
emit belongs in a per-scenario contract, not a global rule.

The required field list lives at the top of `validate_l1_logs.py`:

- `REQUIRED_COMMON` — fields the SDK puts on every enforced request.
- `REQUIRED_PER_ENDPOINT` — additional fields per endpoint. `/v1/install`
  adds `is_hardware_id_real`, `first_install_time` and `hardware_id`;
  `/v1/open` adds `randomized_device_token`, `randomized_bundle_token` and
  `hardware_id`; `/v3/deeplink` and `/v3/events/open` share
  `REQUIRED_V3_SESSION` (`anon_id`, `first_install_time`,
  `is_hardware_id_real`) since both carry the same device block on the
  beta's two-request open flow; `connection_type` is on all init/event
  requests, so `/v1/url` legitimately lacks it.

Device/SDK field enforcement covers `/v1/*`, `/v3/events/open` (both via
`ENFORCED_PREFIXES`), and anything else with an entry in
`REQUIRED_PER_ENDPOINT` (e.g. `/v3/deeplink`). `/v3/events/standard` and
`/v3/events/custom` are exempt: they come from `ServerRequestLogEvent`, which
is V2, so device fields sit under `user_data` and the schema differs.

Secure-context checking is separate and covers every signed endpoint
(`SIGNED_ENDPOINTS`): `/v3/events/open`, `/v3/events/standard`,
`/v3/events/custom`, `/v1/url` and `/v1/cpid/latd`. These are the endpoints that
override `ServerRequest.applySecureContext`. A signed endpoint is checked
wherever it appears, whatever its surrounding payload looks like.

### Scenario contracts

`--scenario NAME` checks a capture against a fixed shape registered in
`SCENARIO_CONTRACTS`: exact per-endpoint request counts, relative ordering
between two endpoints, and how many of an endpoint's requests must carry a
given field. Every contract is measured from a real capture, not written
from a ticket, and `assert_contract` holds no endpoint name of its own —
the same engine serves both platforms. Retries are collapsed first
(`collapse_retries`, keyed on `branch_sdk_request_unique_id`) so a flaky
network resending one logical request does not inflate a count. Omit
`--scenario` to skip contract checking and get per-request field checks only.

### Secure SDK context

On the init request the validator also checks `branch_sdk_secure_context`:

- `context_key` on every request that carries the block.
- `initialization_context` — required on the first `/v3/events/open`, with
  `client_public_key`, `challenge`, and **exactly one** of
  `attestation_object` (device with hardware Key Attestation) or
  `play_integrity_token` (device without).
- `activity_context` — `request_signature` and `nonce`, on every signed request
  after registration, including the event endpoints.
- The two contexts are mutually exclusive; carrying both is an error.

Presence-only here too. Nothing verifies a signature or an attestation
chain — that is `tools/verify_android_key_attestation.py` and
`tools/verify_play_integrity.py`.

Lookups tolerate `user_data` nesting so a future move from top-level to
nested placement does not break the gate.

## What gets printed on success

For every captured request: the full payload plus a per-field check table
showing the actual value that went over the wire. Silent passes are not
possible because every field's value is visible in the CI log.

## Platform parity

The iOS sibling validator lives in `ios-branch-deep-linking-attribution`
and uses the same architecture but a different required field set —
`wifi` and `ui_mode` are Android-only by design. Cross-platform alignment
of these device-context fields is tracked under the v4 Conversion API
workstream, not this gate.
