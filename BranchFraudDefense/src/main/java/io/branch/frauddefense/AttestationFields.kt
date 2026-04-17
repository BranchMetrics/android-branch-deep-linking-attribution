package io.branch.frauddefense

/**
 * Field names for attestation data in Branch API requests.
 *
 * These match the field names expected by Branch backend.
 */
internal object AttestationFields {
    // Hardware attestation fields
    const val APP_ECDH_PUB = "app_ecdh_pub"
    const val KEY_ATTESTATION_CHAIN = "key_attestation_chain"
    const val NONCE = "nonce"
    const val ECDH_PUBLIC_KEY = "ecdh_public_key"

    // Play Integrity fields
    const val PLAY_INTEGRITY_TOKEN = "play_integrity_token"
}
