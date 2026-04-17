package io.branch.frauddefense

import android.util.Log

/**
 * Simple logger for BranchFraudDefense module.
 * Delegates to Android Log with consistent tag.
 */
internal object FraudDefenseLogger {
    private const val TAG = "BranchFraudDefense"

    fun v(message: String) {
        Log.v(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
}
