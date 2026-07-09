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
    var loggingLevel: BranchLogLevel = BranchLogLevel.DEBUG

    @JvmStatic
    var loggingEnabled = false
    
    @JvmStatic
    var loggerCallback: IBranchLoggingCallbacks? = null

    private fun shouldLog(level: BranchLogLevel): Boolean = level.level <= loggingLevel.level

    // android.util.Log truncates a single entry at ~4000 chars, which chops long messages
    // (e.g. canonical strings carrying an attestation cert chain). Split into chunks so the
    // full message is emitted across multiple logcat lines. Chunks are raw (no injected
    // markers) so consecutive lines concatenate back to the exact original string.
    private const val MAX_LOG_CHUNK = 3500

    private fun platformLog(priority: Int, message: String) {
        if (message.length <= MAX_LOG_CHUNK) {
            Log.println(priority, TAG, message)
            return
        }
        var start = 0
        while (start < message.length) {
            val end = minOf(start + MAX_LOG_CHUNK, message.length)
            Log.println(priority, TAG, message.substring(start, end))
            start = end
        }
    }

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
                platformLog(Log.ERROR, message)
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
                platformLog(Log.WARN, message)
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
                platformLog(Log.INFO, message)
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
                platformLog(Log.DEBUG, message)
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
                platformLog(Log.VERBOSE, message)
            }
        }
    }

    @JvmStatic
    fun logAlways(message: String) {
        if (message.isNotEmpty()) {
            if (useCustomLogger()) {
                loggerCallback?.onBranchLog(message, "INFO")
            } else {
                platformLog(Log.INFO, message)
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