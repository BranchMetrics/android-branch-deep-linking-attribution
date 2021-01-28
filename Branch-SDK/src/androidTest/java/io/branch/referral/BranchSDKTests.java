package io.branch.referral;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.mock.MockActivity;
import io.branch.referral.util.BranchCPID;

@RunWith(AndroidJUnit4.class)
public class BranchSDKTests extends BranchTest {
    private static final String TAG = "BranchSDKTests";
    // it takes a very long time
    private static final int TIMEOUT = 5000;

    @Before
    public void setUp() {
        super.setUp();
        initBranchInstance();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

//    @Test
//    public void testGetCPID() throws Throwable {
//        final CountDownLatch lock = new CountDownLatch(1);
//        branch.getCrossPlatformIds(new ServerRequestGetCPID.BranchCrossPlatformIdListener() {
//            @Override public void onDataFetched(BranchCPID cpidResponse, BranchError error) {
//                if (error == null) {
//                    Assert.assertNotNull(cpidResponse);
//                } else {
//                    Assert.fail("getCrossPlatformIds returned error, " + error.getMessage());
//                }
//                lock.countDown();
//            }
//        });
//
//        Assert.assertTrue(lock.await(TIMEOUT, TimeUnit.MILLISECONDS));
//    }

    synchronized private String getUrlFB(final CountDownLatch signal) throws InterruptedException {
        final ShortUrlResult shortUrl = new ShortUrlResult(null);

        new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
//                        Assert.assertNull(error); // todo fix?
                        Assert.assertNotNull(url);
                        if (url.contains("bnc_no_value")) {
                            Log.d("benas", "wtf... url = " + url);
                        }
                        // long url route = "/l/", short url route = "/a/"
                        Log.d("benas", "url = " + url);
                        Assert.assertTrue(url.startsWith("https://bnc.lt/a/"));
                        shortUrl.val = url;

                        signal.countDown();
                    }
                });
        return shortUrl.val;
    }

    private static class ShortUrlResult {
        public String val;
        ShortUrlResult(String url) { val = url; }
    }

//    @Test
//    public void test00GetShortUrlSyncFailure() {
//        String url = new BranchShortLinkBuilder(getTestContext()).getShortUrl();
//        Assert.assertNull(url);
//    }
//
//    @Test
//    public void test02GetShortURLAsync() throws InterruptedException {
//        final String urlFB = getUrlFB();
//        Assert.assertNotNull(urlFB);
//    }
//
//    @Test
//    public void test04GetShortURLAsync1Cached() throws InterruptedException {
//        final String urlFB = getUrlFB();
//
//        final CountDownLatch signal = new CountDownLatch(1);
//        new BranchShortLinkBuilder(getTestContext())
//                .setChannel("facebook")
//                .generateShortUrl(new BranchLinkCreateListener() {
//                    @Override
//                    public void onLinkCreate(String url, BranchError error) {
//                        Assert.assertNull(error);
//                        Assert.assertNotNull(url);
//                        Assert.assertSame(url, urlFB);
//
//                        signal.countDown();
//                    }
//                });
//        Assert.assertTrue(signal.await(TIMEOUT, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void test04GetShortURLAsync2Uncached() throws InterruptedException {
//        final String urlFB = getUrlFB();
//
//        final CountDownLatch signal = new CountDownLatch(1);
//        new BranchShortLinkBuilder(getTestContext())
//                .setChannel("twitter")
//                .generateShortUrl(new BranchLinkCreateListener() {
//                    @Override
//                    public void onLinkCreate(String url, BranchError error) {
//                        Assert.assertNull(error);
//                        Assert.assertNotNull(url);
//                        Assert.assertSame(url, urlFB);
//
//                        signal.countDown();
//                    }
//                });
//        Assert.assertTrue(signal.await(TIMEOUT, TimeUnit.MILLISECONDS));
//    }

    @Test
    public void test04GetShortURLSync() throws InterruptedException {
        activityScenario.moveToState(Lifecycle.State.RESUMED);

        Thread.sleep(TIMEOUT);// let session initialize

        CountDownLatch signal = new CountDownLatch(1);
        String urlFB = getUrlFB(signal);
        if (signal.getCount() > 0) {
            Assert.assertTrue(signal.await(TIMEOUT, TimeUnit.MILLISECONDS));
        }

//        Thread.sleep(TIMEOUT);// wait for urlFB

        String url = new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .getShortUrl();

        Thread.sleep(TIMEOUT);// wait for url?

        Log.d("benas", "url = " + url + ", urlFB = " + urlFB);
        Assert.assertTrue(url != null && url.equals(urlFB));

        url = new BranchShortLinkBuilder(getTestContext())
                .setChannel("linkedin")
                .getShortUrl();
        Assert.assertTrue(url != null && !url.equals(urlFB));

        Thread.sleep(TIMEOUT);// let the test complete before starting cleanup
    }
//
//    @Test
//    public void test01SetIdentity() throws InterruptedException {
//        final CountDownLatch signal = new CountDownLatch(1);
//        prefHelper.setIdentity(PrefHelper.NO_STRING_VALUE);
//        branch.setIdentity("test_user_1", new BranchReferralInitListener() {
//            @Override
//            public void onInitFinished(JSONObject referringParams, BranchError error) {
//                Assert.assertNull(error);
//                Assert.assertNotNull(referringParams);
//                Assert.assertEquals(prefHelper.getIdentityID(), "85338557421453831");
//
//                JSONObject installParams = branch.getFirstReferringParams();
//                try {
//                    Assert.assertEquals(installParams.getString("name"), "test name");
//                    Assert.assertEquals(installParams.getString("message"), "hello there with short url");
//                } catch (JSONException ignore) {
//                }
//
//                signal.countDown();
//            }
//        });
//        Assert.assertTrue(signal.await(TIMEOUT, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void test03GetRewardsChanged() throws InterruptedException {
//        final CountDownLatch signal = new CountDownLatch(1);
//        prefHelper.setCreditCount("default", 9999999);
//
//        branch.loadRewards(new BranchReferralStateChangedListener() {
//            @Override
//            public void onStateChanged(boolean changed, BranchError error) {
//                Assert.assertNull(error);
//                Assert.assertTrue(changed);
//
//                signal.countDown();
//            }
//        });
//        Assert.assertTrue(signal.await(TIMEOUT, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void testGetRewardsUnchanged() throws InterruptedException {
//        final CountDownLatch signal = new CountDownLatch(1);
//        prefHelper.setCreditCount("default", prefHelper.getCreditCount("default"));
//
//        branch.loadRewards(new BranchReferralStateChangedListener() {
//            @Override
//            public void onStateChanged(boolean changed, BranchError error) {
//                Assert.assertNull(error);
//                Assert.assertFalse(changed);
//
//                signal.countDown();
//            }
//        });
//        Assert.assertTrue(signal.await(TIMEOUT, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void testZLoad() throws InterruptedException {
//        final CountDownLatch signalFinal = new CountDownLatch(1);
//        final CountDownLatch signal = new CountDownLatch(1);
//        for (int i = 0; i < 100; i++) {
//            final int idx = i;
//            new BranchShortLinkBuilder(getTestContext())
//                    .setChannel(i + "")
//                    .generateShortUrl(new BranchLinkCreateListener() {
//                        @Override
//                        public void onLinkCreate(String url, BranchError error) {
//                            Assert.assertNull(error);
//                            Assert.assertNotNull(url);
//                            if (idx == 99) {
//                                signal.countDown();
//                            }
//                        }
//                    });
//            Thread.sleep(50);
//        }
//        Assert.assertTrue(signal.await(TIMEOUT, TimeUnit.MILLISECONDS));
//
//        new BranchShortLinkBuilder(getTestContext())
//                .setFeature("loadTest")
//                .generateShortUrl(new BranchLinkCreateListener() {
//                    @Override
//                    public void onLinkCreate(String url, BranchError error) {
//                        Assert.assertNull(error);
//                        Assert.assertNotNull(url);
//                        signalFinal.countDown();
//                    }
//                });
//
//        Assert.assertTrue(signalFinal.await(TIMEOUT, TimeUnit.MILLISECONDS));
//    }
//
//
//
//    @Test
//    public void testAppContext() {
//        // Context of the app under test.
//        Assert.assertNotNull(getTestContext());
//    }
//
//    @Test
//    public void testPackageName() {
//        // Context of the app under test.
//        Context appContext = getTestContext();
//        Assert.assertEquals("io.branch.referral.test", appContext.getPackageName());
//    }
//
//    @Test
//    public void testSdkVersion() {
//        Assert.assertNotNull(Branch.getSdkVersionNumber());
//    }
}
