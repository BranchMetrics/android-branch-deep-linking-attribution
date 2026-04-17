package io.branch.frauddefense

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec

/**
 * Handles Android Key Attestation for the Branch SDK.
 *
 * The attestation challenge is derived from following three components:
 *   1. A canonical query string of the request body — binds attestation to the exact payload.
 *   2. A ecdhPublicKeyB64 - This will be used in future for deriving HMAC secret.
 *   3. A cryptographically random 32-byte nonce — ensures freshness and uniqueness. SDK will get
 *      this value from Branch Backend
 *
 *   Challenge = SHA-256(canonicalQueryStringBytes || ecdhPublicKeyBytes || randomNonceBytes)
 *
 * The canonical query string is built by sorting all top-level request body keys
 * alphabetically and serializing as "key1=val1&key2=val2&...".
 *
 * The `nonce` field sent in the request body is the base64 of the random bytes.
 *
 * The server verifies by stripping attestation fields, rebuilding the canonical query string from the
 * remaining body, then recomputing SHA-256(canonical_utf8 || ecdhPublicKeyBytes || decoded_nonce) and comparing
 * with the challenge embedded in the leaf certificate's attestation extension.
 *
 * Requires API 24+. On API 28+ StrongBox is used when available, falling back to TEE.
 */
internal object AppAttestation {

    // Key used for Android Key Attestation — regenerated each install call.
    private const val KEY_ALIAS = "com.branch.ecdh_key"

    // Persistent P-256 key for future Diffie-Hellman / HMAC
    private const val ECDH_KEY_ALIAS = "com.branch.ecdh.longterm"

    private const val RANDOM_NONCE_SIZE_BYTES = 32

    /**
     * Generates attestation data to be included in the v1/install request.
     *
     * @param packageManager Used to check StrongBox availability on API 28+.
     * @param requestBody    Current POST body snapshot (all params added before attestation).
     * @return [AttestationData] with base64-encoded public key, certificate chain, and nonce,
     *         or null if the device does not support hardware attestation (API < 24) or if key
     *         generation fails.
     */
    fun generateAttestationData(packageManager: PackageManager, requestBody: JSONObject): AttestationData? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            FraudDefenseLogger.v("AppAttestation: skipped — API ${Build.VERSION.SDK_INT} < 24")
            return null
        }

        return try {
            val ecdhPubKeyBytes = getOrCreateEcdhPublicKey()
            val randomNonce = generateRandomNonce()
            val challenge = buildChallenge(randomNonce, requestBody, ecdhPubKeyBytes)
            val certChain = generateAttestedKeyPair(packageManager, challenge)

            val leaf = certChain[0] as X509Certificate
            val appEcdhPub = Base64.encodeToString(leaf.publicKey.encoded, Base64.NO_WRAP)
            val chainB64 = certChain.map { cert ->
                Base64.encodeToString(cert.encoded, Base64.NO_WRAP)
            }
            val nonceB64 = Base64.encodeToString(randomNonce, Base64.NO_WRAP)
            val ecdhPublicKeyB64 = Base64.encodeToString(ecdhPubKeyBytes, Base64.NO_WRAP)

            AttestationData(appEcdhPub, chainB64, nonceB64, ecdhPublicKeyB64)
        } catch (e: Exception) {
            FraudDefenseLogger.w("AppAttestation: key generation failed — ${e.message}")
            null
        }
    }

    private fun generateRandomNonce(): ByteArray {
        val bytes = ByteArray(RANDOM_NONCE_SIZE_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    /**
     * Derives the 32-byte attestation challenge:
     *   SHA-256(canonicalQueryStringBytes || ecdhPublicKeyBytes || randomNonceBytes
     */
    internal fun buildChallenge(randomNonce: ByteArray, requestBody: JSONObject, ecdhPublicKey: ByteArray): ByteArray {
        val canonicalBytes = canonicalQueryString(requestBody).toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .apply {
                update(canonicalBytes)
                update(ecdhPublicKey)  // ECDH pub key between canonical and nonce
                update(randomNonce)
            }
            .digest()
    }

    /**
     * Returns the persistent P-256 ECDH public key from AndroidKeyStore, generating it on
     * first call. The private key never leaves hardware. This key is intended for future
     * Diffie-Hellman key exchange and HMAC signature verification.
     *
     * Internal visibility so BranchFraudDefense can pre-generate this key.
     */
    internal fun getOrCreateEcdhPublicKey(): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(ECDH_KEY_ALIAS)) {
            return keyStore.getCertificate(ECDH_KEY_ALIAS).publicKey.encoded
        }

        val specBuilder = KeyGenParameterSpec.Builder(
            ECDH_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )
        keyPairGenerator.initialize(specBuilder.build())
        return keyPairGenerator.generateKeyPair().public.encoded
    }

    /**
     * Sorts all top-level keys of [obj] alphabetically and serializes as:
     *   "key1=value1&key2=value2&..."
     *
     * For non-string values (nested JSONObjects, arrays, numbers, booleans) the value's
     * default string representation is used.
     */
    private fun canonicalQueryString(obj: JSONObject): String {
        val keys = obj.keys().asSequence().sorted().toList()
        return keys.joinToString("&") { key -> "$key=${obj.get(key)}" }
    }

    private fun generateAttestedKeyPair(
        packageManager: PackageManager,
        nonce: ByteArray
    ): Array<java.security.cert.Certificate> {
        val hasStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

        val specBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAttestationChallenge(nonce)

        if (hasStrongBox) {
            specBuilder.setIsStrongBoxBacked(true)
        }

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )
        keyPairGenerator.initialize(specBuilder.build())
        keyPairGenerator.generateKeyPair()

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getCertificateChain(KEY_ALIAS)
    }

    /**
     * Attestation data produced by [generateAttestationData].
     *
     * @property appEcdhPub Base64 DER-encoded EC public key from the leaf certificate
     *                      (the hardware-attested key).
     * @property keyAttestationChain Ordered list of base64 DER-encoded certificates
     *                               (leaf → intermediate → … → Google Hardware Attestation Root CA).
     * @property nonce Base64-encoded 32-byte random value. The server recomputes the cert challenge
     *                 as SHA-256(canonicalQueryString_utf8 || ecdh_public_key_bytes || decoded_nonce)
     *                 and verifies it matches the challenge embedded in the leaf certificate.
     * @property ecdhPublicKey Base64-encoded persistent P-256 public key for future
     *                         Diffie-Hellman key exchange and HMAC signature verification.
     */
    internal data class AttestationData(
        val appEcdhPub: String,
        val keyAttestationChain: List<String>,
        val nonce: String,
        val ecdhPublicKey: String
    )

    // ── Play Integrity fallback ───────────────────────────────────────────────

    /**
     * Fallback for devices where hardware attestation is unavailable (API < 24 or key
     * generation failure). Requests a Play Integrity token using the same canonical
     * challenge as the hardware path so the server can verify payload binding.
     *
     * The nonce passed to Play Integrity is the base64url encoding of:
     *   SHA-256(canonicalQueryString_utf8 || ecdhPublicKeyBytes || randomNonceBytes)
     *
     * Requires the app to include `com.google.android.play:integrity` as a dependency.
     * Returns null if the library is absent, the device has no Play services, or the
     * token request fails.
     *
     * @param context     Application context.
     * @param requestBody Current POST body snapshot (all params added before attestation).
     */
    fun generatePlayIntegrityToken(context: Context, requestBody: JSONObject): PlayIntegrityData? {
        return try {
            val ecdhPubKeyBytes = getOrCreateEcdhPublicKey()
            val randomNonce = generateRandomNonce()
            val challenge = buildChallenge(randomNonce, requestBody, ecdhPubKeyBytes)

            // Play Integrity requires a base64url-encoded nonce (URL-safe, no padding)
            val nonceBase64url = Base64.encodeToString(
                challenge, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val token = PlayIntegrityWrapper.requestToken(context, nonceBase64url)

            PlayIntegrityData(
                integrityToken = token,
                nonce          = Base64.encodeToString(randomNonce, Base64.NO_WRAP),
                ecdhPublicKey  = Base64.encodeToString(ecdhPubKeyBytes, Base64.NO_WRAP)
            )
        } catch (e: Throwable) {
            FraudDefenseLogger.w("AppAttestation: Play Integrity token request failed — ${e.message}")
            null
        }
    }

    /**
     * Play Integrity attestation data — used when hardware attestation is unavailable.
     *
     * @property integrityToken  Encrypted token from Google Play Integrity API. Decrypted
     *                           server-side using Google's key; contains device verdict.
     * @property nonce           Base64-encoded 32-byte random value used to derive the
     *                           Play Integrity nonce, for server-side challenge verification.
     * @property ecdhPublicKey   Base64-encoded persistent P-256 public key for future
     *                           Diffie-Hellman key exchange and HMAC signature verification.
     */
    data class PlayIntegrityData(
        val integrityToken: String,
        val nonce: String,
        val ecdhPublicKey: String
    )

}
