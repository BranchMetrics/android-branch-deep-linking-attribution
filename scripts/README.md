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

- `REQUIRED_COMMON` — fields the SDK puts on every `/v1/*` request.
- `REQUIRED_PER_ENDPOINT` — additional fields per endpoint. Today this
  covers `is_hardware_id_real` and `first_install_time` on `/v1/install`;
  `randomized_device_token` and `randomized_bundle_token` on `/v1/open`;
  and `connection_type` on both `/v1/install` and `/v1/open` (only emitted
  on init/event requests, so `/v1/url` legitimately lacks it).

Required-field checks are scoped to `/v1/*` only. Captured non-v1
endpoints (e.g. `/v2/event/*`) get their payload printed for visibility
but do not fail the run — the L1 contract covers v1 only.

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
