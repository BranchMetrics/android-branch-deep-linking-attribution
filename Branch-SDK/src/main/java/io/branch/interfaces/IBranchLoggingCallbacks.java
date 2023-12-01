package io.branch.interfaces;

public interface IBranchLoggingCallbacks {
    void onBranchLog(String logMessage, String severityConstantName);
}
