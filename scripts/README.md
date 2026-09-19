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

## What gets validated

Presence-only. A required field is either there (pass) or absent (fail). No
type checks, no value-format checks — those are intentionally left to the
backend ingestion gate.

The required field list lives at the top of `validate_l1_logs.py`:

- `REQUIRED_COMMON` — fields the SDK puts on every enforced request.
- `REQUIRED_PER_ENDPOINT` — additional fields per endpoint. `/v1/install`
  and `/v3/events/open` add `is_hardware_id_real` and `first_install_time`;
  `/v1/open` adds `randomized_device_token` and `randomized_bundle_token`;
  `connection_type` is on all init/event requests, so `/v1/url`
  legitimately lacks it.
- `MANDATORY_ENDPOINT` — `/v3/events/open`. Every session initializes here,
  fresh install included, so a capture without it means init never went out.
  Seeing `/v1/install` is itself an error: init no longer uses it.

Device/SDK field enforcement covers `/v1/*` and `/v3/events/open`
(`ENFORCED_PREFIXES`). `/v3/events/standard` and `/v3/events/custom` are exempt:
they come from `ServerRequestLogEvent`, which is V2, so device fields sit under
`user_data` and the schema differs.

Secure-context checking is separate and covers every signed endpoint
(`SIGNED_ENDPOINTS`): `/v3/events/open`, `/v3/events/standard`,
`/v3/events/custom`, `/v1/url` and `/v1/cpid/latd`. These are the endpoints that
override `ServerRequest.applySecureContext`. A signed endpoint is checked
wherever it appears, whatever its surrounding payload looks like.

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
