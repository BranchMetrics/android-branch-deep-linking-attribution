package io.branch.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;

import io.branch.interfaces.IBranchLoggingCallbacks;
import io.branch.referral.validators.IntegrationValidator;

/**
 * EMT-4471 regression test: IntegrationValidator.validate(context) must not crash when called
 * before Branch.initialize(context, config) has run. Under the new architecture
 * Branch.getInstance() returns null in that case (it no longer auto-creates the singleton),
 * which used to NPE inside validateSDKIntegration() before it could report a diagnostic.
 *
 * Lives in io.branch.referral, not io.branch.referral.validators, because it needs
 * Branch.shutDown() (package-private) to force Branch.getInstance() == null.
 */
public class IntegrationValidatorTest extends BranchTestBase {

    private final Context context = RuntimeEnvironment.getApplication();

    @Before
    @Override
    public void setUpBase() {
        super.setUpBase();
        Branch.shutDown();
        ShadowLog.clear();
        BranchLogger.setLoggerCallback(null);
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.DEBUG);
    }

    @After
    @Override
    public void tearDownBase() {
        super.tearDownBase();
        Branch.shutDown();
        BranchLogger.setLoggerCallback(null);
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.DEBUG);
    }

    @Test
    public void validate_beforeInitialize_doesNotThrow() {
        IntegrationValidator.validate(context);
    }

    @Test
    public void validate_beforeInitialize_logsBranchNotInitialisedNotDashboardConfig() {
        IntegrationValidator.validate(context);

        boolean sawNotInitialised = false;
        boolean sawDashboardConfig = false;
        for (ShadowLog.LogItem item : ShadowLog.getLogs()) {
            if (item.msg == null) continue;
            if (item.msg.contains("Branch is not initialised")) {
                sawNotInitialised = true;
            }
            if (item.msg.contains("Unable to read Dashboard config")) {
                sawDashboardConfig = true;
            }
        }

        assertTrue("expected the 'Branch not initialised' diagnostic", sawNotInitialised);
        assertFalse("must not fall through to the dashboard-config diagnostic", sawDashboardConfig);
    }

    /**
     * EMT-4471 addendum defect B / fix B1: validate() used to unconditionally replace
     * BranchLogger.loggerCallback, silently dropping whatever logger callback the integrator
     * had already configured (e.g. the TestBed's own file/console sink).
     */
    @Test
    public void validate_chainsToPreviousLoggerCallback_insteadOfReplacingIt() {
        final List<String> previousCallbackMessages = new ArrayList<>();
        BranchLogger.setLoggerCallback(new IBranchLoggingCallbacks() {
            @Override
            public void onBranchLog(String logMessage, String severityConstantName) {
                previousCallbackMessages.add(logMessage);
            }
        });

        IntegrationValidator.validate(context);
        BranchLogger.d("post-validate marker message");

        assertTrue(
                "validate() must chain to the previously configured logger callback, not replace it",
                previousCallbackMessages.contains("post-validate marker message"));
        assertTrue(
                "the validator's own StringBuilder sink must still receive the message too",
                IntegrationValidator.getLogs().contains("post-validate marker message"));
    }

    /**
     * EMT-4471 addendum fix B1: validate() must never lower an already-configured log level
     * (e.g. VERBOSE) down to DEBUG.
     */
    @Test
    public void validate_doesNotLowerAnAlreadyConfiguredVerboseLevel() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.VERBOSE);

        IntegrationValidator.validate(context);

        assertEquals(BranchLogger.BranchLogLevel.VERBOSE, BranchLogger.getLoggingLevel());
    }

    @Test
    public void validate_raisesLevelToDebugWhenBelowDebug() {
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.ERROR);

        IntegrationValidator.validate(context);

        assertEquals(BranchLogger.BranchLogLevel.DEBUG, BranchLogger.getLoggingLevel());
    }
}
