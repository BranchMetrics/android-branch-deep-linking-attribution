package io.branch.referral;

import org.json.JSONObject;

/**
 * Interface for fraud defense providers.
 *
 * <p>This interface decouples the main Branch SDK from fraud defense implementations.
 * The SDK never imports concrete fraud defense classes (e.g., BranchFraudDefense) -
 * it only knows this interface.</p>
 *
 * <p>Fraud defense includes multiple layers: device attestation, Play Integrity,
 * and other security checks.</p>
 *
 * <p>Similar to iOS BranchFraudDefenseProtocol pattern.</p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Vendor integrates BranchFraudDefense module
 * BranchFraudDefenseProvider fraudDefense = BranchFraudDefense.getInstance(context);
 * Branch.getInstance().setFraudDefenseProvider(fraudDefense);
 * }</pre>
 *
 * <p>If no provider is set, SDK functions normally without fraud defense.</p>
 */
public interface BranchFraudDefenseProvider {
    /**
     * Perform fraud defense checks and return fields to add to request.
     *
     * <p>Called by {@link ServerRequestRegisterInstall} before sending v1/install request.</p>
     *
     * @param requestBody Current request body (before fraud defense fields)
     * @return JSONObject with fraud defense fields to merge, or null if unavailable
     */
    JSONObject performAttestationCheck(JSONObject requestBody);
}
