package io.branch.referral;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

/**
 * Base Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4.class)
public class BranchTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void tearDown() {
        Branch.shutDown();
        mContext = null;
    }

    @Test
    public void testAppContext() {
        // Context of the app under test.
        Assert.assertNotNull(getTestContext());
    }

    @Test
    public void testPackageName() {
        // Context of the app under test.
        Context appContext = getTestContext();

        Assert.assertEquals("io.branch.referral.test", appContext.getPackageName());
    }

    @Test
    public void initBranchInstance() {
        Branch branch = Branch.getAutoInstance(getTestContext());

        Assert.assertEquals(branch, Branch.getInstance());
    }

    @Test
    public void initBranchTestInstance() {
        Branch branch = Branch.getAutoTestInstance(getTestContext());

        Assert.assertEquals(branch, Branch.getInstance());
    }

    Context getTestContext() {
        return mContext;
    }

    // Set the Branch Instance.
    // This is useful for creating a Mock instance and setting it in the SDK
    void setBranchInstance(Branch branchInstance) throws Throwable {
        Field f = Branch.class.getDeclaredField("branchReferral_");
        f.setAccessible(true);
        f.set(null, branchInstance);
    }
}
