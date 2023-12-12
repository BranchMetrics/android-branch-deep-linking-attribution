package io.branch.referral

import android.util.Log
import io.branch.interfaces.IBranchLoggingCallbacks

object BranchLogger {

    private const val TAG = "BranchSDK"

    @JvmStatic
    var loggingEnabled = false
    
    @JvmStatic
    var loggerCallback: IBranchLoggingCallbacks? = null

    private val isDebug = BuildConfig.DEBUG;

    /**
     * <p>Creates a <b>Error</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun e(message: String) {
        if (loggingEnabled && message.isNotEmpty()) {
            loggerCallback?.onBranchLog(message, "ERROR")

            if(isDebug) {
                Log.e(TAG, message)
            }
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
            loggerCallback?.onBranchLog(message, "WARN")

            if(isDebug) {
                Log.w(TAG, message)
            }
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
            loggerCallback?.onBranchLog(message, "INFO")

            if(isDebug) {
                Log.i(TAG, message)
            }
        }
    }

    /**
     * <p>Creates a <b>Debug</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun d(message: String?) {
        if (loggingEnabled && message?.isNotEmpty() == true) {
            loggerCallback?.onBranchLog(message, "DEBUG")

            if(isDebug) {
                Log.d(TAG, message)
            }
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
            loggerCallback?.onBranchLog(message, "VERBOSE")

            if(isDebug) {
                Log.v(TAG, message)
            }
        }
    }

    @JvmStatic
    fun logAlways(message: String) {
        if (message.isNotEmpty()) {
            loggerCallback?.onBranchLog(message, "INFO")

            if(isDebug) {
                Log.i(TAG, message)
            }
        }
    }

    @JvmStatic
    fun logException(message: String, t: Exception?) {
        if (message.isNotEmpty()) {
            loggerCallback?.onBranchLog(message, "ERROR")

            if(isDebug) {
                Log.e(TAG, message, t)
            }
        }
    }
}