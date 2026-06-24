package io.branch.referral;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import io.branch.interfaces.IBranchLoggingCallbacks;

/**
 * EMT-3864: enableLogging() must show request/response traffic without the queue/lock trace flood.
 * The ~150 queue/lock lines now go through BranchLogger.t(), gated by an opt-in trace flag
 * independent of the log level. enableLogging() shows only real traffic; Branch.setTraceLogging(true)
 * brings the diagnostics back on demand.
 *
 * Output is captured via the logger callback (BranchLogger routes to it when one is set), which is
 * deterministic in a JVM unit test.
 */
@RunWith(RobolectricTestRunner.class)
public class TraceLoggingTest {

    private final List<String> captured = new ArrayList<>();

    @Before
    public void setUp() {
        captured.clear();
        BranchLogger.setLoggerCallback(new IBranchLoggingCallbacks() {
            @Override
            public void onBranchLog(String logMessage, String severityConstantName) {
                captured.add(logMessage);
            }
        });
        BranchLogger.setLoggingEnabled(true);
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.VERBOSE);
        BranchLogger.setTraceLoggingEnabled(false);
    }

    @After
    public void tearDown() {
        BranchLogger.setLoggerCallback(null);
        BranchLogger.setTraceLoggingEnabled(false);
        BranchLogger.setLoggingEnabled(false);
    }

    @Test
    public void enableLoggingDefault_hidesQueueTrace() {
        BranchLogger.t("queue/lock internal trace");
        assertFalse("trace must stay silent while tracing is off (the EMT-3864 fix)",
                captured.contains("queue/lock internal trace"));
    }

    @Test
    public void realTraffic_visibleAtVerbose_regardlessOfTrace() {
        BranchLogger.v("posting to https://api2.branch.io");
        assertTrue("request/response traffic must show at VERBOSE even with tracing off",
                captured.contains("posting to https://api2.branch.io"));
    }

    @Test
    public void setTraceLoggingTrue_revealsQueueTrace() {
        Branch.setTraceLogging(true);
        BranchLogger.t("STUCK_LOCK_RESOLUTION: resolving");
        assertTrue("trace diagnostics must appear once setTraceLogging(true)",
                captured.contains("STUCK_LOCK_RESOLUTION: resolving"));
    }

    @Test
    public void trace_silentWhenLoggingDisabled_evenIfTraceOn() {
        BranchLogger.setLoggingEnabled(false);
        Branch.setTraceLogging(true);
        BranchLogger.t("should not appear");
        assertFalse("trace requires logging to be enabled to have any effect",
                captured.contains("should not appear"));
    }

    @Test
    public void setTraceLogging_whileLoggingOff_hintsToEnableLogging() {
        BranchLogger.setLoggingEnabled(false);
        Branch.setTraceLogging(true);
        boolean hinted = false;
        for (String line : captured) {
            if (line.contains("call Branch.enableLogging()")) {
                hinted = true;
                break;
            }
        }
        assertTrue("setTraceLogging(true) with logging off must hint that logging is required",
                hinted);
    }
}
