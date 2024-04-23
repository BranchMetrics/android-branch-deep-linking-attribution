package io.branch.referral

import android.util.Log
import io.branch.interfaces.IBranchLoggingCallbacks
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

object BranchLogger {

    private const val TAG = "BranchSDK"

    enum class BranchLogLevel(val level: Int) {
        ERROR(1), WARN(2), INFO(3), DEBUG(4), VERBOSE(5)
    }

    @JvmStatic
    var logLevel: BranchLogLevel = BranchLogLevel.DEBUG

    @JvmStatic
    var loggingEnabled = false
    
    @JvmStatic
    var loggerCallback: IBranchLoggingCallbacks? = null

    private fun shouldLog(level: BranchLogLevel): Boolean = level.level <= logLevel.level

    /**
     * <p>Creates a <b>Error</b> message in the debugger. If debugging is disabled, this will fail silently.</p>
     *
     * @param message A {@link String} value containing the debug message to record.
     */
    @JvmStatic
    fun e(message: String) {
        if (loggingEnabled && shouldLog(BranchLogLevel.ERROR) && message.isNotEmpty()) {
            if (useCustomLogger()) {
                loggerCallback?.onBranchLog(message, "ERROR")
            } else {
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
        if (loggingEnabled && shouldLog(BranchLogLevel.WARN) && message.isNotEmpty()) {
            if (useCustomLogger()) {
                loggerCallback?.onBranchLog(message, "WARN")
            } else {
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
        if (loggingEnabled && shouldLog(BranchLogLevel.INFO) && message.isNotEmpty()) {
            if(useCustomLogger()) {
                loggerCallback?.onBranchLog(message, "INFO")
            } else {
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
        if (loggingEnabled && shouldLog(BranchLogLevel.DEBUG) && message?.isNotEmpty() == true) {
            if (useCustomLogger()) {
                loggerCallback?.onBranchLog(message, "DEBUG")
            } else {
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
        if (loggingEnabled && shouldLog(BranchLogLevel.VERBOSE) && message.isNotEmpty()) {
            if (useCustomLogger()) {
                loggerCallback?.onBranchLog(message, "VERBOSE")
            } else {
                Log.v(TAG, message)
            }
        }
    }

    @JvmStatic
    fun logAlways(message: String) {
        if (message.isNotEmpty()) {
            if (useCustomLogger()) {
                loggerCallback?.onBranchLog(message, "INFO")
            } else {
                Log.i(TAG, message)
            }
        }
    }

    /**
     * If an implementation of IBranchLoggingCallbacks is passed, forward logging messages to callback
     * Else, maintain the original behavior of Branch.enableLogging().
     */
    private fun useCustomLogger(): Boolean {
        return loggerCallback != null
    }

    @JvmStatic
    fun stackTraceToString(exception: java.lang.Exception): String {
        val writer: Writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))

        return writer.toString()
    }
}