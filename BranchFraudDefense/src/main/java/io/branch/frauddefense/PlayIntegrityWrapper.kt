package io.branch.frauddefense

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest

/**
 * Thin wrapper around the Play Integrity API.
 *
 * All direct references to Play Integrity classes are isolated here so that:
 *  - Any API change (class rename, method rename) produces a compile error in this file.
 *  - [AppAttestation] calls this after checking if Play Integrity API is available.
 *
 * Play Integrity API is included in BranchFraudDefense module:
 *   implementation("com.google.android.play:integrity:1.4.0")
 */
internal object PlayIntegrityWrapper {

    /**
     * Requests a Play Integrity token synchronously.
     *
     * @param context       Application context.
     * @param nonceBase64url Base64url-encoded nonce (no padding) derived from the request challenge.
     * @return              The encrypted integrity token string.
     */
    fun requestToken(context: Context, nonceBase64url: String): String {
        val manager = IntegrityManagerFactory.create(context)
        val request = IntegrityTokenRequest.builder()
            .setNonce(nonceBase64url)
            .setCloudProjectNumber(394719403839)
            .build()
        return Tasks.await(manager.requestIntegrityToken(request)).token()
    }
}
