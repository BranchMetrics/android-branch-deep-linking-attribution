package io.branch.referral;

import static org.junit.Assert.assertEquals;
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
 * EMT-3864: enableLogging() must show request/response traffic without the internal queue/lock
 * trace flood. Rather than a custom trace channel with its own gate, every statement is classified
 * to a standard android.util.Log severity: network request/response traffic logs at DEBUG, the
 * fine-grained queue/lock trace logs at VERBOSE. enableLogging() now defaults to DEBUG, so the
 * common path shows clean traffic; a developer diagnosing a stuck request opts into the extra
 * detail with enableLogging(BranchLogLevel.VERBOSE). One gate: the log level.
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
    }

    @After
    public void tearDown() {
        BranchLogger.setLoggerCallback(null);
        BranchLogger.setLoggingEnabled(false);
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.DEBUG);
    }

    @Test
    public void debugLevel_showsTraffic_hidesQueueTrace() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.DEBUG);

        BranchLogger.d("posting to https://api2.branch.io");
        BranchLogger.v("BranchRequestQueue.enqueue called");

        assertTrue("request/response traffic (DEBUG) must show at the default DEBUG level (the EMT-3864 fix)",
                captured.contains("posting to https://api2.branch.io"));
        assertFalse("queue/lock trace (VERBOSE) must stay out of the default DEBUG output",
                captured.contains("BranchRequestQueue.enqueue called"));
    }

    @Test
    public void verboseLevel_showsTrafficAndQueueTrace() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.VERBOSE);

        BranchLogger.d("posting to https://api2.branch.io");
        BranchLogger.v("WAIT_LOCK_DEBUG: request stuck with locks");

        assertTrue("traffic must still show at VERBOSE",
                captured.contains("posting to https://api2.branch.io"));
        assertTrue("queue/lock trace must appear once the level is raised to VERBOSE",
                captured.contains("WAIT_LOCK_DEBUG: request stuck with locks"));
    }

    @Test
    public void loggingDisabled_showsNothing() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.VERBOSE);
        BranchLogger.setLoggingEnabled(false);

        BranchLogger.d("posting to https://api2.branch.io");
        BranchLogger.v("WAIT_LOCK_DEBUG: request stuck with locks");

        assertTrue("no output may be emitted while logging is disabled", captured.isEmpty());
    }

    @Test
    public void noneLevel_silencesEveryLevel() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.NONE);

        BranchLogger.e("error");
        BranchLogger.w("warn");
        BranchLogger.i("info");
        BranchLogger.d("debug");
        BranchLogger.v("verbose");

        assertTrue("NONE must suppress every level, including ERROR", captured.isEmpty());
    }

    @Test
    public void noneLevel_silencesLogAlways() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.NONE);

        BranchLogger.logAlways("io.branch.sdk.android:library:6.0.0");

        assertTrue("logAlways bypasses shouldLog, so NONE must gate it explicitly or init still "
                + "emits one line per launch", captured.isEmpty());
    }

    @Test
    public void errorLevel_stillShowsErrors() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.ERROR);

        BranchLogger.e("error");
        BranchLogger.w("warn");

        assertTrue("ERROR must still emit at the ERROR level", captured.contains("error"));
        assertFalse("WARN must not emit at the ERROR level", captured.contains("warn"));
    }

    @Test
    public void configurationDefaultsToErrorLevel() {
        // A caller who says nothing about logging gets the quiet default; DEBUG (traffic) and
        // VERBOSE (queue trace) are both opt-in, per debugLevel_/verboseLevel_ above.
        BranchConfiguration config = new BranchConfiguration.Builder("key_live_x").build();
        assertEquals("an unconfigured BranchConfiguration must default to the quiet ERROR level",
                BranchLogger.BranchLogLevel.ERROR, config.getLogLevel());
    }
}
