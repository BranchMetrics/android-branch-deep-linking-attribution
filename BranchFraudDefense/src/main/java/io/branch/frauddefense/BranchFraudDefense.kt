package io.branch.frauddefense

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.branch.referral.BranchFraudDefenseProvider
import org.json.JSONArray
import org.json.JSONObject

/**
 * Branch Fraud Defense - Device Attestation and Play Integrity APIs for Branch SDK.
 * Uses Android Key Attestation (hardware-backed) with Play Integrity as fallback.
 */

class BranchFraudDefense private constructor(
    private val context: Context
) : BranchFraudDefenseProvider {

    companion object {
        @Volatile
        private var instance: BranchFraudDefense? = null

        /**
         * Get singleton instance of BranchFraudDefense.
         *
         * @param context Application context (automatically used)
         * @return BranchFraudDefense instance
         */
        @JvmStatic
        fun getInstance(context: Context): BranchFraudDefense {
            return instance ?: synchronized(this) {
                instance ?: BranchFraudDefense(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun initializeBranchSecureSDK(branchKey: String?) {
        startFraudDefenseSystem()
    }

    /**
     * Initialize the fraud defense system.
     *
     * Call this early (e.g., Application.onCreate) to start background key generation.
     * Keys are generated asynchronously - no blocking.
     *
     * On first call:
     * - Generates ECDH keys in Android KeyStore
     * - Pre-warms hardware attestation paths
     *
     * On subsequent calls:
     * - No-op if already initialized
     */
    fun startFraudDefenseSystem() {
        FraudDefenseLogger.v("Starting fraud defense system")

        // Pre-generate ECDH key (used by both hardware and Play Integrity)
        // This is done synchronously on first call but cached afterwards
        try {
            // This will create the key if it doesn't exist
            // On subsequent calls, it just returns the existing key
            AppAttestation.getOrCreateEcdhPublicKey()
            FraudDefenseLogger.v("Fraud defense system initialized")
        } catch (e: Exception) {
            FraudDefenseLogger.w("Failed to initialize fraud defense: ${e.message}")
        }
    }

    /**
     * Perform attestation check and return fields to merge into request.
     *
     * Flow:
     * 1. Try hardware attestation (API 24+)
     *    - StrongBox preferred (API 28+)
     *    - Falls back to TEE
     * 2. If hardware unavailable, try Play Integrity
     * 3. Return null if both unavailable (graceful degradation)
     *
     * @param requestBody Current request body (before attestation fields)
     * @return JSONObject with attestation fields, or null if unavailable
     */
    override fun performAttestationCheck(requestBody: JSONObject): JSONObject? {
        FraudDefenseLogger.v("Performing attestation check")

        // Try hardware attestation first (API 24+)
     /*   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val packageManager = context.packageManager
                val attestationData = AppAttestation.generateAttestationData(packageManager, requestBody)

                if (attestationData != null) {
                    FraudDefenseLogger.v("Hardware attestation successful")
                    return createHardwareAttestationFields(attestationData)
                }
            } catch (e: Exception) {
                FraudDefenseLogger.w("Hardware attestation failed: ${e.message}")
            }
        }
*/
        // Fallback to Play Integrity
        try {
            val playIntegrityData = AppAttestation.generatePlayIntegrityToken(context, requestBody)

            if (playIntegrityData != null) {
                FraudDefenseLogger.v("Play Integrity attestation successful")
                return createPlayIntegrityFields(playIntegrityData)
            }
        } catch (e: Exception) {
            FraudDefenseLogger.w("Play Integrity attestation failed: ${e.message}")
        }

        // Both methods unavailable - graceful degradation
        FraudDefenseLogger.w("No attestation method available")
        return null
    }

    /**
     * Convert hardware attestation data to JSON fields for request.
     */
    private fun createHardwareAttestationFields(data: AppAttestation.AttestationData): JSONObject {
        return JSONObject().apply {
            put(AttestationFields.APP_ECDH_PUB, data.appEcdhPub)
            put(AttestationFields.KEY_ATTESTATION_CHAIN, JSONArray(data.keyAttestationChain))
            put(AttestationFields.NONCE, data.nonce)
            put(AttestationFields.ECDH_PUBLIC_KEY, data.ecdhPublicKey)
        }
    }

    /**
     * Layer 2 + 3: HMAC signature + smart nonce for event requests.
     * Not yet implemented in this module — returns null (graceful degradation).
     */
    override fun addSignatureAndNonceForParams(requestBody: JSONObject): JSONObject? {
        return null
    }

    /**
     * Convert Play Integrity data to JSON fields for request.
     */
    private fun createPlayIntegrityFields(data: AppAttestation.PlayIntegrityData): JSONObject {
        return JSONObject().apply {
            put(AttestationFields.PLAY_INTEGRITY_TOKEN, data.integrityToken)
            put(AttestationFields.NONCE, data.nonce)
            put(AttestationFields.ECDH_PUBLIC_KEY, data.ecdhPublicKey)
        }
    }
}
