package io.branch.referral

import android.util.Log

object BranchLogger {

    private const val TAG = "BranchSDK"

    @JvmStatic
    var loggingEnabled = false

    /**
     * <p>Creates a <b>Error</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun e(message: String) {
        if (loggingEnabled && message.isNotEmpty()) {
            Log.e(TAG, message)
        }
    }

    /**
     * <p>Creates a <b>Warning</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun w(message: String) {
        if (loggingEnabled && message.isNotEmpty()) {
            Log.w(TAG, message)
        }
    }

    /**
     * <p>Creates a <b>Info</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun i(message: String) {
        if (loggingEnabled && message.isNotEmpty()) {
            Log.i(TAG, message)
        }
    }

    /**
     * <p>Creates a <b>Debug</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun d(message: String) {
        if (loggingEnabled && message.isNotEmpty()) {
            Log.d(TAG, message)
        }
    }

    /**
     * <p>Creates a <b>Verbose</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun v(message: String) {
        if (loggingEnabled && message.isNotEmpty()) {
            Log.v(TAG, message)
        }
    }
}