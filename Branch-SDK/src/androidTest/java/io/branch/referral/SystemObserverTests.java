package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class SystemObserverTests extends BranchTest {

    @Test
    public void testAnonID() {
        initBranchInstance();
        String anonID = SystemObserver.getAnonID(getTestContext());
        try {
            UUID.fromString(anonID);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testAnonIDChangesWithDisableTracking() {
        // TODO: figure out how to handle disable tracking, seems the tracking controller is not very testable
    }
}
