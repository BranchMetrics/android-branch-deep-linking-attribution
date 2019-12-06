package io.branch.branchandroiddemo;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
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

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.BranchError;
import io.branch.referral.BranchShortLinkBuilder;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequestGetCPID;
import io.branch.referral.util.BranchCPID;

@RunWith(AndroidJUnit4.class)
public class BranchSDKTests {

    private Context mContext;
    private Branch branch;
    private PrefHelper prefHelper;

    CountDownLatch lock = new CountDownLatch(1);

    @Test
    public void testGetCPID() throws Throwable {
        branch.initSession();
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

        Assert.assertTrue(lock.await(5000, TimeUnit.MILLISECONDS));
    }

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext();
        branch = Branch.getInstance(mContext);
        prefHelper = PrefHelper.getInstance(mContext);
    }

    @After
    public void tearDown() {
        branch.resetUserSession();
    }

    private Context getTestContext() {
        return mContext;
    }

    private void initSession() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        branch.initSession(new BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                Assert.assertNull(error);
                signal.countDown();
            }
        });
        signal.await(1, TimeUnit.SECONDS);
    }

    private String _fburl;
    private String getUrlFB() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        _fburl = null;

        new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        Assert.assertNull(error);
                        Assert.assertNotNull(url);
                        Assert.assertTrue(url.startsWith("https://bnc.lt/l/"));
                        _fburl = url;

                        signal.countDown();
                    }
                });
        signal.await(5, TimeUnit.SECONDS);
        return _fburl;
    }

    @Test
    public void test00GetShortUrlSyncFailure() {
        String url = new BranchShortLinkBuilder(getTestContext()).getShortUrl();
        Assert.assertNull(url);
    }

    @Test
    public void test02GetShortURLAsync() throws InterruptedException {
        initSession();
        final String urlFB = getUrlFB();
        Assert.assertNotNull(urlFB);
    }

    @Test
    public void test04GetShortURLAsync1Cached() throws InterruptedException {
        initSession();
        final String urlFB = getUrlFB();

        final CountDownLatch signal = new CountDownLatch(1);
        new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        Assert.assertNull(error);
                        Assert.assertNotNull(url);
                        Assert.assertSame(url, urlFB);

                        signal.countDown();
                    }
                });
        signal.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void test04GetShortURLAsync2Uncached() throws InterruptedException {
        initSession();
        final String urlFB = getUrlFB();

        final CountDownLatch signal = new CountDownLatch(1);
        new BranchShortLinkBuilder(getTestContext())
                .setChannel("twitter")
                .generateShortUrl(new BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        Assert.assertNull(error);
                        Assert.assertNotNull(url);
                        Assert.assertSame(url, urlFB);

                        signal.countDown();
                    }
                });
        signal.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void test04GetShortURLSync() throws InterruptedException {
        initSession();
        final String urlFB = getUrlFB();

        String url = new BranchShortLinkBuilder(getTestContext())
                .setChannel("facebook")
                .getShortUrl();

        Assert.assertSame(url, urlFB);

        url = new BranchShortLinkBuilder(getTestContext())
                .setChannel("linkedin")
                .getShortUrl();
        Assert.assertNotEquals(url, urlFB);
    }

    @Test
    public void test01SetIdentity() throws InterruptedException {
        initSession();

        final CountDownLatch signal = new CountDownLatch(1);
        prefHelper.setIdentity(PrefHelper.NO_STRING_VALUE);
        branch.setIdentity("test_user_1", new BranchReferralInitListener() {
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
        signal.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void test03GetRewardsChanged() throws InterruptedException {
        initSession();

        final CountDownLatch signal = new CountDownLatch(1);
        prefHelper.setCreditCount("default", 9999999);

        branch.loadRewards(new BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                Assert.assertNull(error);
                Assert.assertTrue(changed);

                signal.countDown();
            }
        });
        signal.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testGetRewardsUnchanged() throws InterruptedException {
        initSession();

        final CountDownLatch signal = new CountDownLatch(1);
        prefHelper.setCreditCount("default", prefHelper.getCreditCount("default"));

        branch.loadRewards(new BranchReferralStateChangedListener() {
            @Override
            public void onStateChanged(boolean changed, BranchError error) {
                Assert.assertNull(error);
                Assert.assertFalse(changed);

                signal.countDown();
            }
        });
        signal.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testZLoad() throws InterruptedException {
        initSession();

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
            Thread.sleep(50);
        }
        signal.await(10, TimeUnit.SECONDS);

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

        signalFinal.await(1, TimeUnit.SECONDS);
    }
}
