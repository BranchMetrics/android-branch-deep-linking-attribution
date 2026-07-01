package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Deep-merges branch_sdk_secure_context from fraud defense responses into request JSON.
 *
 * Both addDeviceTrustParams and addSignatureAndNonceForParams return a JSONObject
 * containing a "branch_sdk_secure_context" key. This merger ensures sub-keys
 * (initialization_context, activity_context, context_key) accumulate rather than
 * overwrite each other.
 */
class SecureContextMerger {

    private static final String SECURE_CONTEXT = "branch_sdk_secure_context";

    /**
     * Merges the fraud defense response into the target request JSON.
     * Deep-merges the branch_sdk_secure_context sub-object.
     */
    static void merge(JSONObject fraudDefenseResponse, JSONObject target) throws JSONException {
        if (!fraudDefenseResponse.has(SECURE_CONTEXT)) {
            // No secure context — shallow merge everything
            Iterator<String> keys = fraudDefenseResponse.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                target.put(key, fraudDefenseResponse.get(key));
            }
            return;
        }

        JSONObject newContext = fraudDefenseResponse.getJSONObject(SECURE_CONTEXT);

        if (target.has(SECURE_CONTEXT)) {
            // Deep merge into existing
            JSONObject existingContext = target.getJSONObject(SECURE_CONTEXT);
            Iterator<String> keys = newContext.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                existingContext.put(key, newContext.get(key));
            }
        } else {
            target.put(SECURE_CONTEXT, newContext);
        }
    }
}
