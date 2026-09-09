package io.branch.branch_sdk_testbed_kotlin

import android.app.Application
import android.util.Log
import io.branch.referral.Branch
import io.branch.referral.BranchConfiguration
import io.branch.referral.BranchLogger

/**
 * Same test key as the Java TestBed, so links created here resolve against the same app.
 */
class TestbedApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = BranchConfiguration.Builder(TEST_KEY)
            .setTestMode(true)
            .setLogLevel(BranchLogger.BranchLogLevel.VERBOSE)
            .setLoggingCallback { message, _ -> Log.d(SDK_TAG, message) }
            .build()

        Branch.initialize(this, config)
        Log.i(TAG, "Branch.initialize done, key=$TEST_KEY")
    }

    companion object {
        const val TAG = "BranchCoroutines"
        private const val SDK_TAG = "BranchSDK"
        private const val TEST_KEY = "key_test_hdcBLUy1xZ1JD0tKg7qrLcgirFmPPVJc"
    }
}
