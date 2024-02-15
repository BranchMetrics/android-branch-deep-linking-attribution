package io.branch.interfaces;

/**
 * A set of callbacks that interface with the SDK's internal logging.
 */
public interface IBranchLoggingCallbacks {
    /**
     * Callback method that returns each time a log is generated
     * @param logMessage The log message
     * @param severityConstantName any of DEBUG, ERROR, INFO, WARN, VERBOSE
     */
    void onBranchLog(String logMessage, String severityConstantName);
}
