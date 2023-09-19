package io.branch.coroutines

import android.content.Context
import io.branch.data.InstallReferrerResult
import io.branch.referral.Branch
import io.branch.referral.BranchLogger
import io.branch.referral.ServerRequest
import io.branch.referral.ServerResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

suspend fun installRequestJob(context: Context): ServerResponse? {
    return supervisorScope {
        val advertisingInfoObjectResult = async { getAdvertisingInfoObject(context) }
        val latestInstallReferrerResult = async { fetchLatestInstallReferrer(context) }

        BranchLogger.d("latestInstallReferrerResult " + latestInstallReferrerResult.await() + " advertisingInfoObjectResult " + advertisingInfoObjectResult.await())
        null
    }
}

suspend fun openRequestJob(context: Context): ServerResponse? {
    return supervisorScope {
        val advertisingInfoObjectResult = async { getAdvertisingInfoObject(context) }

        BranchLogger.d("advertisingInfoObjectResult " + advertisingInfoObjectResult.await())
        null
    }
}
