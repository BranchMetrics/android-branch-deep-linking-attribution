import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import io.branch.branchandroiddemo.MainActivity;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;

/**
 * Created by sojanpr on 10/13/16.
 * <p>
 * Instrumentation test case for testing Branch Android deep linking SDK
 * </p>
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BranchSDKTest extends InstrumentationTestCase {
    Context context_;
    boolean isInitialised_ = false;
    private static final String TAG = "BranchAndroidTestCase";


    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Before
    public void createBranchInstance() {
        context_ = InstrumentationRegistry.getTargetContext().getApplicationContext();
        Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = mInstrumentation.addMonitor(MainActivity.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(mInstrumentation.getTargetContext(), MainActivity.class.getName());
        mInstrumentation.startActivitySync(intent);

        Activity currentActivity = mInstrumentation.waitForMonitor(monitor);
        assertNotNull(currentActivity);
    }

    @Test
    public void testInitSession() {
        Log.d(TAG, "---- @Test::initSession() ----");
        final String[] initErrorMsg = {""};
        final CountDownLatch latch = new CountDownLatch(1);
        Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error != null) {
                    initErrorMsg[0] = error.getMessage();
                } else {
                    isInitialised_ = true;
                }
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            isInitialised_ = false;
        }

        assertTrue("Branch is not initialised " + initErrorMsg[0], isInitialised_);

    }

    @Test
    public void testShortLinkCreation() {
        Log.d(TAG, "---- @Test::getShortUrl() ----");
        BranchUniversalObject buo = new BranchUniversalObject()
                .setCanonicalIdentifier("item/1000")
                .setCanonicalUrl("https://branch.io/deepviews")
                .setTitle("Test_Title")
                .setContentDescription("Test_Description ")
                .setContentImageUrl("https://example.com/mycontent-12345.png")
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                .setContentType("application/vnd.businessobjects")
                //.setContentExpiration(new Date(1476566432000L)) // set contents expiration time if applicable
                .setPrice(5.00, CurrencyType.USD)
                .addKeyWord("Test_Keyword1")
                .addKeyWord("Test_Keyword2")
                .addContentMetadata("Test_Metadata_Key1", "Metadata_value1")
                .addContentMetadata("Test_Metadata_Key2", "Metadata_value2");

        LinkProperties linkProperties = new LinkProperties()
                .setChannel("Test_channel");

        String url = buo.getShortUrl(context_, linkProperties);
        Log.d(TAG, "Short url created " + url);

        boolean isShortLinkCreated = (!TextUtils.isEmpty(url)) && (!url.contains("/a/"));
        assertTrue("Branch short link creation failed. Url created is " + url, isShortLinkCreated);
    }


}
