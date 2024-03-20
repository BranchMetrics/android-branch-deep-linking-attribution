package io.branch.coroutines

import android.content.Context
import io.branch.data.PreInitDataResult
import io.branch.referral.BranchLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

suspend fun installRequestJob(context: Context): PreInitDataResult?  {
    BranchLogger.v("installRequestJob")
    return supervisorScope {
        try {
            val advertisingInfoObjectResult = async { getAdvertisingInfoObject(context) }
            val latestInstallReferrerResult = async { fetchLatestInstallReferrer(context) }

            PreInitDataResult(advertisingInfoObjectResult.await(), latestInstallReferrerResult.await())
        }
        catch (exception: Exception){
            BranchLogger.d(exception.message)
            null
        }
    }
}

suspend fun openRequestJob(context: Context): PreInitDataResult? {
    return supervisorScope {
        try {
            val advertisingInfoObjectResult = async { getAdvertisingInfoObject(context) }
            PreInitDataResult(advertisingInfoObjectResult.await(), null)
        }
        catch (exception: Exception) {
            BranchLogger.d(exception.message)
            null
        }
    }
}
