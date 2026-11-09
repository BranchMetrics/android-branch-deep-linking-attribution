package io.branch.referral;

import static org.junit.Assert.assertEquals;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

/** Pins the public showShareSheet overload restored for 5.x source compatibility. */
@RunWith(RobolectricTestRunner.class)
public class ShowShareSheetApiCompatTest {

    @Test
    public void showShareSheetOverloadExists() throws NoSuchMethodException {
        Method showShareSheet = BranchUniversalObject.class.getMethod(
                "showShareSheet",
                Activity.class,
                LinkProperties.class,
                ShareSheetStyle.class,
                Branch.BranchLinkShareListener.class);
        assertEquals("showShareSheet(...) must return void", void.class, showShareSheet.getReturnType());
    }
}
