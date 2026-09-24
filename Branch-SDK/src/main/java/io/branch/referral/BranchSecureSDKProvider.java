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
 * BranchSecureSDKProvider fraudDefense = BranchFraudDefense.getInstance(context);
 * Branch.getInstance().setFraudDefenseProvider(fraudDefense);
 * }</pre>
 *
 * <p>If no provider is set, SDK functions normally without fraud defense.</p>
 */
public interface BranchSecureSDKProvider {
    /**
     * Bootstraps the secure SDK (key generation, attestation pre-warming, challenge prefetch).
     *
     * <p>Called automatically by {@link Branch#setFraudDefenseProvider(BranchSecureSDKProvider)}
     * when a provider is registered, so the host app does not need to start it separately.
     * Mirrors iOS, where {@code setFraudDefenseHandler:} invokes
     * {@code initializeBranchSecureSDKWithBranchKey:}.</p>
     *
     * @param branchKey The Branch key for this app, or null if not yet configured
     */
    void initializeBranchSecureSDK(String branchKey);

    /**
     * Layer 1: performs device attestation and returns device-trust fields to add to the request.
     *
     * <p>At most one request at a time may carry an initialization_context. The provider claims
     * that slot here and returns null to every other caller until
     * {@link #releaseAttestationClaim()} runs, so a burst of opens cannot each attest.</p>
     *
     * @param requestBody Current request body (before fraud defense fields)
     * @return JSONObject with fraud defense fields to merge, or null if unavailable
     */
    JSONObject addDeviceTrustParams(JSONObject requestBody);

    /**
     * Releases the Layer 1 slot claimed by {@link #addDeviceTrustParams(JSONObject)}.
     *
     * <p>Call this once the request that carried the initialization_context has been answered:
     * on success, because attestation is done and later requests use Layer 2 anyway; on terminal
     * failure, so the next open can attest in its place. Skipping the failure case would hold the
     * slot for the rest of the process and leave the device unable to register.</p>
     *
     * <p>Default is a no-op, for providers that do not gate attestation.</p>
     */
    default void releaseAttestationClaim() {
    }

    /**
     * Layer 2 + 3: generates HMAC-SHA256 signature and smart nonce for the request.
     *
     * <p>Called for v2/event requests. Returns a branch_sdk_secure_context envelope
     * with an activity_context block containing request_signature and nonce,
     * or null if the HMAC secret is not yet available.</p>
     *
     * @param requestBody Current request body (before signature fields)
     * @return JSONObject with signature fields to merge, or null if unavailable
     */
    JSONObject addSignatureAndNonceForParams(JSONObject requestBody);
}
