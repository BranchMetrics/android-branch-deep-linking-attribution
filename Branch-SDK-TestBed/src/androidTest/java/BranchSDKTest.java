import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.branchandroiddemo.MainActivity;
import io.branch.branchandroiddemo.R;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.PrefHelper;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by sojanpr on 10/13/16.
 * <p>
 * Instrumentation test case for testing Branch Android deep linking SDK
 * </p>
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SmallTest
public class BranchSDKTest extends InstrumentationTestCase {
    Context context_;
    boolean isInitialised_ = false;
    private static final String TAG = "BranchAndroidTestCase";
    BranchUniversalObject buo_;
    LinkProperties linkProperties_;
    private static String shortUrlCreated_;
    private static String errorMessage_;
    private static final int LINK_GEN_WAIT_TIME = 1000; // Link should be created in 1 second
    private static final int MAX_BRANCH_REQ_WAIT_TIME = 2000; // Wait time for a Branch Request to respond
    Instrumentation instrumentation_;
    Activity currActivity_;
    PrefHelper prefHelper_;
    boolean testFlag_; // General flag to be used with tests

    @Before
    public void createBranchInstance() {
        context_ = InstrumentationRegistry.getTargetContext().getApplicationContext();
        instrumentation_ = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation_.addMonitor(MainActivity.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(instrumentation_.getTargetContext(), MainActivity.class.getName());
        instrumentation_.startActivitySync(intent);

        currActivity_ = instrumentation_.waitForMonitor(monitor);
        assertNotNull(currActivity_);

        buo_ = new BranchUniversalObject()
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

        linkProperties_ = new LinkProperties()
                .setChannel("Test_channel");

        prefHelper_ = PrefHelper.getInstance(context_);
    }

    @Test
    public void test00InitSession() {
        Log.d(TAG, "\n---- @Test::initSession() ----");
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
            latch.await(MAX_BRANCH_REQ_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            isInitialised_ = false;
        }
        assertTrue("Branch is not initialised " + initErrorMsg[0], isInitialised_);
    }

    @Test
    public void test01ShortLinkCreation() {
        Log.d(TAG, "\n---- @Test::getShortUrl ----");
        shortUrlCreated_ = buo_.getShortUrl(context_, linkProperties_);
        Log.d(TAG, "Short url created " + shortUrlCreated_);

        boolean isShortLinkCreated = (!TextUtils.isEmpty(shortUrlCreated_)) && (!shortUrlCreated_.contains("/a/"));
        assertTrue("Branch short link creation failed. Url created is " + shortUrlCreated_, isShortLinkCreated);
    }

    @Test
    public void test02LinkCache() {
        Log.d(TAG, "\n---- @Test::LinkCache ----");
        String url = buo_.getShortUrl(context_, linkProperties_);
        assertEquals("Link is not retrieved from cache ", shortUrlCreated_, url);
    }

    @Test
    public void test03ShortLinkGeneration() {
        Log.d(TAG, "\n---- @Test::ShortLinkGeneration ----");
        linkProperties_.setFeature("TestAsyncLink creation");
        boolean timedOut = false;
        final CountDownLatch latch = new CountDownLatch(1);
        final long startTime = System.currentTimeMillis();
        buo_.generateShortUrl(context_, linkProperties_, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                latch.countDown();
                if (error != null) {
                    errorMessage_ = error.getMessage();
                }
                boolean isShortLinkGenerated = (!TextUtils.isEmpty(url)) && (!url.contains("/a/"));
                assertTrue("Branch short link generation failed . Url created is " + shortUrlCreated_ + "\n" + errorMessage_, isShortLinkGenerated);
                Log.d(TAG, "Link creation completed in " + (System.currentTimeMillis() - startTime) + " milli seconds");
            }
        });
        try {
            latch.await(LINK_GEN_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            timedOut = true;
        }
        assertFalse("Async link creation timed out. Waited for " + LINK_GEN_WAIT_TIME + "milli seconds", timedOut);
    }

    /**
     * Check if share sheet is shown
     */
    @Test
    public void test04ShareSheet() {
        Log.d(TAG, "\n---- @Test::ShareSheet ----");
        onView(withId(R.id.share_btn)).perform(click());
        // Check if share dialog is up
        onView(withText("Share With")).check(ViewAssertions.matches(isDisplayed()));
    }

    @Test
    public void test05SetIdentity() {
        Log.d(TAG, "\n---- @Test::SetIdentity ----");
        final CountDownLatch latch = new CountDownLatch(1);
        final String identityID = prefHelper_.getIdentityID();
        prefHelper_.setIdentity(PrefHelper.NO_STRING_VALUE);
        Branch.getInstance().setIdentity("test_user_1", new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                assertNull(error);
                assertNotNull(referringParams);
                assertTrue(prefHelper_.getIdentityID().equals("159812520658343392")); //"test_user_1" user id
                latch.countDown();
            }
        });
        try {
            latch.await(MAX_BRANCH_REQ_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            assertTrue("setIdentity timed out ", false);
        }
    }

    @Test
    public void test06LoadRewards() {
        Log.d(TAG, "\n---- @Test::LoadRewards ----");
        final CountDownLatch latch = new CountDownLatch(1);
        final int[] availableCredits = new int[1];
        Branch.getInstance().loadRewards(new Branch.BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                assertNull("Error while loading rewards ", error);
                availableCredits[0] = prefHelper_.getCreditCount();
            }
        });

        onView(withId(R.id.cmdCommitBuyAction)).perform(click());
        Branch.getInstance().loadRewards(new Branch.BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                assertNull("Error while loading rewards ", error);
                int updatedCredits = prefHelper_.getCreditCount();
                assertTrue(updatedCredits == (availableCredits[0] + 5)); // 5 credits are add in one buy
                latch.countDown();
            }
        });

        try {
            latch.await(MAX_BRANCH_REQ_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            assertTrue("Loading rewards timed out ", false);
        }

    }

    @Test
    public void test07RedeemRewards() {
        Log.d(TAG, "\n---- @Test::RedeemRewards ----");
        final CountDownLatch latch = new CountDownLatch(1);
        final int availableCredits = prefHelper_.getCreditCount();
        Branch.getInstance().redeemRewards(5, new Branch.BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                assertNull("Error while redeeming rewards", error);
                assertTrue("Local credit does not changed on redeeming rewards", changed);
            }
        });

        Branch.getInstance().loadRewards(new Branch.BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                assertNull("Error while loading rewards ", error);
                int updatedCredits = prefHelper_.getCreditCount();
                assertTrue(updatedCredits == (availableCredits - 5));
                latch.countDown();
            }
        });
        try {
            latch.await(MAX_BRANCH_REQ_WAIT_TIME * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            assertTrue("Redeeming rewards timed out ", false);
        }
    }


}
