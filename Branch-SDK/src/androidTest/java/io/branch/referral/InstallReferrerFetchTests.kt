package io.branch.referral

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.branch.coroutines.fetchLatestInstallReferrer
import io.branch.coroutines.getGooglePlayStoreReferrerDetails
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallReferrerFetchTests : BranchTest() {

    // runBlocking: runTest's virtual time would fire withTimeout early.
    @Test
    fun googlePlayReferrerFetchCompletes() = runBlocking<Unit> {
        val result = withTimeout(30_000) { getGooglePlayStoreReferrerDetails(mContext) }
        Log.i("InstallReferrerFetchTests", "Google Play referrer result: $result")
    }

    @Test
    fun latestInstallReferrerFetchCompletes() = runBlocking<Unit> {
        val result = withTimeout(30_000) { fetchLatestInstallReferrer(mContext) }
        Log.i("InstallReferrerFetchTests", "Latest install referrer result: $result")
    }
}
