package io.branch.referral;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.util.BranchCPID;

@RunWith(AndroidJUnit4.class)
public class BranchSDKTests extends BranchTest {
    private static final String TAG = "BranchSDKTests";

    private PrefHelper prefHelper;

    @Before
    public void setUp() {
        super.setUp();
        initBranchInstance();
        prefHelper = PrefHelper.getInstance(mContext);
    }

    @Test
    public void testGetCPID() throws Throwable {
        final CountDownLatch lock = new CountDownLatch(1);
        branch.getCrossPlatformIds(new ServerRequestGetCPID.BranchCrossPlatformIdListener() {
            @Override public void onDataFetched(BranchCPID cpidResponse, BranchError error) {
                if (error == null) {
                    Assert.assertNotNull(cpidResponse);
                } else {
                    Assert.fail("getCrossPlatformIds returned error, " + error.getMessage());
                }
                lock.countDown();
            }
        });

        Assert.assertTrue(lock.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test00GetShortUrlSyncFailure() {
        String url = new BranchShortLinkBuilder(getTestContext()).getShortUrl();
        Assert.assertNull(url);
    }

    @Test
    public void test02GetShortURLAsync() throws InterruptedException {
        final FBUrl urlFB = new FBUrl(null);
        getFBUrl(urlFB);
        Assert.assertNotNull(urlFB.val);
    }

    @Test
    public void test04GetShortURLAsync1Cached() throws InterruptedException {
        final FBUrl urlFB = new FBUrl(null);
        getFBUrl(urlFB);

        final CountDownLatch signal = new CountDownLatch(1);
        new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        Assert.assertNull(error);
                        Assert.assertNotNull(url);
                        Assert.assertSame(url, urlFB.val);

                        signal.countDown();
                    }
                });
        Assert.assertTrue(signal.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test04GetShortURLAsync2Uncached() throws InterruptedException {
        final FBUrl urlFB = new FBUrl(null);
        getFBUrl(urlFB);

        final CountDownLatch signal = new CountDownLatch(1);
        new BranchShortLinkBuilder(getTestContext())
                .setChannel("twitter")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        Assert.assertNull(error);
                        Assert.assertNotNull(url);
                        Assert.assertSame(url, urlFB.val);

                        signal.countDown();
                    }
                });
        Assert.assertTrue(signal.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test04GetShortURLSync() throws InterruptedException {
        initSessionResumeActivity();

        FBUrl urlFB = new FBUrl(null);
        getFBUrl(urlFB);

        String url = new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .getShortUrl();

        Log.d("benas", "url = " + url + ", urlFB = " + urlFB.val);
        Assert.assertTrue(url != null && url.equals(urlFB.val));

        url = new BranchShortLinkBuilder(getTestContext())
                .setChannel("linkedin")
                .getShortUrl();
        Assert.assertFalse(url != null && !url.equals(urlFB.val));
    }

    @Test
    public void test01SetIdentity() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        prefHelper.setIdentity(PrefHelper.NO_STRING_VALUE);
        branch.setIdentity("test_user_1", new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                Assert.assertNull(error);
                Assert.assertNotNull(referringParams);
                Assert.assertEquals(prefHelper.getIdentityID(), "85338557421453831");

                JSONObject installParams = branch.getFirstReferringParams();
                try {
                    Assert.assertEquals(installParams.getString("name"), "test name");
                    Assert.assertEquals(installParams.getString("message"), "hello there with short url");
                } catch (JSONException ignore) {
                }

                signal.countDown();
            }
        });
        Assert.assertTrue(signal.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test03GetRewardsChanged() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        prefHelper.setCreditCount("default", 9999999);

        branch.loadRewards(new Branch.BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                Assert.assertNull(error);
                Assert.assertTrue(changed);

                signal.countDown();
            }
        });
        Assert.assertTrue(signal.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetRewardsUnchanged() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        prefHelper.setCreditCount("default", prefHelper.getCreditCount("default"));

        branch.loadRewards(new Branch.BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                Assert.assertNull(error);
                Assert.assertFalse(changed);

                signal.countDown();
            }
        });
        Assert.assertTrue(signal.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testZLoad() throws InterruptedException {
        initSessionResumeActivity();
        final CountDownLatch signalFinal = new CountDownLatch(1);
        final CountDownLatch signal = new CountDownLatch(1);
        for (int i = 0; i < 100; i++) {
            final int idx = i;
            new BranchShortLinkBuilder(getTestContext())
                    .setChannel(i + "")
                    .generateShortUrl(new BranchLinkCreateListener() {
                        @Override
                        public void onLinkCreate(String url, BranchError error) {
                            Assert.assertNull(error);
                            Assert.assertNotNull(url);
                            if (idx == 99) {
                                signal.countDown();
                            }
                        }
                    });
        }
        Assert.assertTrue(signal.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));

        new BranchShortLinkBuilder(getTestContext())
                .setFeature("loadTest")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        Assert.assertNull(error);
                        Assert.assertNotNull(url);
                        signalFinal.countDown();
                    }
                });

        Assert.assertTrue(signalFinal.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
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
    public void testSdkVersion() {
        Assert.assertNotNull(Branch.getSdkVersionNumber());
    }

    synchronized private void getFBUrl(final FBUrl res) throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        Assert.assertNull(error); // todo fix?
                        Assert.assertNotNull(url);
                        // long url route = "/l/", short url route = "/a/"
                        Log.d("benas", "urlFB = " + url);
                        Assert.assertTrue(url.startsWith("https://bnc.lt/a/"));
                        res.val = url;

                        signal.countDown();
                    }
                });
        Thread.sleep(TEST_TIMEOUT);
        Assert.assertEquals(0, signal.getCount());
    }

    private static class FBUrl {
        public String val;
        FBUrl(String url) { val = url; }
    }
}
