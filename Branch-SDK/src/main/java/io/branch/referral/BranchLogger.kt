package io.branch.referral

import android.text.TextUtils
import android.util.Log

object BranchLogger {

    private const val TAG = "BranchSDK"

    private var enableLogging_ = false

    @JvmStatic
    fun enableLogging(isLogEnabled: Boolean) {
        enableLogging_ = isLogEnabled
    }

    fun e(message: String) {
        if (enableLogging_ && message.isNotEmpty()) {
            Log.e(TAG, message)
        }
    }

    fun w(message: String) {
        if (enableLogging_ && message.isNotEmpty()) {
            Log.w(TAG, message)
        }
    }

    fun i(message: String) {
        if (enableLogging_ && message.isNotEmpty()) {
            Log.i(TAG, message)
        }
    }

    fun d(message: String) {
        if (enableLogging_ && message.isNotEmpty()) {
            Log.d(TAG, message)
        }
    }

    fun v(message: String) {
        if (enableLogging_ && message.isNotEmpty()) {
            Log.v(TAG, message)
        }
    }
}