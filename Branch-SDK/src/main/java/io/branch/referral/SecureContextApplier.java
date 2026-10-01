package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Applies a fraud defense response onto the request JSON.
 *
 * <p>By design a single request carries exactly one secure-context block: a first
 * install/open carries initialization_context (device attestation) and every later
 * request carries activity_context (HMAC signature + nonce). The two are never sent
 * together, so a shallow top-level copy is sufficient — this mirrors iOS, which just
 * does {@code [json addEntriesFromDictionary:fraudDefenseParams]} in
 * {@code BNCRequestFactory}.</p>
 */
public class SecureContextApplier {

    /**
     * Shallow-copies the fraud defense response onto the target request JSON.
     *
     * <p>Each call replaces the top-level {@code branch_sdk_secure_context} rather than
     * accumulating into it. That is correct because a request only ever receives one
     * fraud-defense contribution (attestation OR signature, never both — the callers
     * enforce this via the {@code bnc_device_trust_checked} if/else).</p>
     */
    public static void apply(JSONObject fraudDefenseResponse, JSONObject target) throws JSONException {
        Iterator<String> keys = fraudDefenseResponse.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            target.put(key, fraudDefenseResponse.get(key));
        }
    }
}
