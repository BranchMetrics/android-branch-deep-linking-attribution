package io.branch.referral;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.branch.referral.Branch.BranchLinkCreateListener;

@RunWith(AndroidJUnit4.class)
public class BranchApiTests extends BranchTest {
    private static final String TAG = "BranchSDKTests";

    private PrefHelper prefHelper;

    @Before
    public void setUp() {
        super.setUp();
        initBranchInstance();
        prefHelper = PrefHelper.getInstance(mContext);
    }

    @After
    public void tearDown() throws InterruptedException {
        branch.linkCache_.clear();
        super.tearDown();
    }

    @Test
    public void test00GetShortUrlSync() {
        String url = new BranchShortLinkBuilder(getTestContext()).getShortUrl();
        Assert.assertNotNull(url);
    }

    @Test
    public void test02GetShortURLAsync() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                final FBUrl urlFB = new FBUrl(null);
                try {
                    getFBUrl(urlFB);
                } catch (InterruptedException e) {
                    Assert.fail();
                }
                Assert.assertNotNull(urlFB.val);
            }
        });
    }

    @Test
    public void test04GetShortURLAsync1Cached() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                final FBUrl urlFB = new FBUrl(null);
                try {
                    getFBUrl(urlFB);
                } catch (InterruptedException e) {
                    Assert.fail();
                }

                final CountDownLatch signal = new CountDownLatch(1);
                new BranchShortLinkBuilder(getTestContext())
                        .setChannel("facebook")
                        .generateShortUrl(new BranchLinkCreateListener() {
                            @Override
                            public void onLinkCreate(String url, BranchError error) {
                                Assert.assertNull(error);
                                Assert.assertNotNull(url);
                                Assert.assertTrue(url.equals(urlFB.val));

                                signal.countDown();
                            }
                        });
                try {
                    Assert.assertTrue(signal.await(TEST_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Assert.fail("timeout");
                }
            }
        });
    }

    @Test
    public void test04GetShortURLAsync2Uncached() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                final FBUrl urlFB = new FBUrl(null);
                try {
                    getFBUrl(urlFB);
                } catch (InterruptedException e) {
                    Assert.fail();
                }

                final CountDownLatch signal = new CountDownLatch(1);
                new BranchShortLinkBuilder(getTestContext())
                        .setChannel("twitter")
                        .generateShortUrl(new BranchLinkCreateListener() {
                            @Override
                            public void onLinkCreate(String url, BranchError error) {
                                Assert.assertNull(error);
                                Assert.assertNotNull(url);
                                Assert.assertNotEquals(url, urlFB.val);

                                signal.countDown();
                            }
                        });
                try {
                    Assert.assertTrue(signal.await(TEST_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Assert.fail();
                }
            }
        });
    }

    @Test
    public void test04GetShortURLSync() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                FBUrl urlFB = new FBUrl(null);
                try {
                    getFBUrl(urlFB);
                } catch (InterruptedException e) {
                    Assert.fail();
                }

                String urlFB2 = new BranchShortLinkBuilder(getTestContext())
                        .setChannel("facebook")
                        .getShortUrl();

                Assert.assertNotNull(urlFB2);
                Assert.assertEquals(urlFB2, urlFB.val);

                String linkedinUrl = new BranchShortLinkBuilder(getTestContext())
                        .setChannel("linkedin")
                        .getShortUrl();

                Assert.assertNotNull(linkedinUrl);
                BranchLogger.v("linkedinUrl: " + linkedinUrl + ", urlFB.val: " + urlFB.val);
                Assert.assertNotEquals(linkedinUrl, urlFB.val);
            }
        });
    }

    @Test
    public void test05GetShortURLSync() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                JSONObject params = new JSONObject();
                try {
                    params.putOpt("a", "b");
                    params.putOpt("b", "c");
                    params.putOpt("c", "d");
                } catch (JSONException e) {
                    Assert.fail();
                }

                FBUrl urlFB = new FBUrl(null);
                try {
                    getFBUrl(urlFB, params, null);
                } catch (InterruptedException e) {
                    Assert.fail();
                }

                String urlFB2 = new BranchShortLinkBuilder(getTestContext())
                        .setChannel("facebook")
                        .setParameters(params)
                        .getShortUrl();

                Assert.assertNotNull(urlFB2);
                Assert.assertEquals(urlFB2, urlFB.val);

                try {
                    params.putOpt("z", "z");
                } catch (JSONException e) {
                    Assert.fail();
                }
                String urlFB3 = new BranchShortLinkBuilder(getTestContext())
                        .setChannel("facebook")
                        .setParameters(params)
                        .getShortUrl();

                Assert.assertNotNull(urlFB3);
                Assert.assertNotEquals(urlFB3, urlFB.val);
            }
        });
    }

    @Test
    public void test06GetShortURLSync() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                List<String> tags  = new ArrayList<>();
                tags.add("a");

                FBUrl urlFB = new FBUrl(null);
                try {
                    getFBUrl(urlFB, null, tags);
                } catch (InterruptedException e) {
                    Assert.fail();
                }

                String urlFB2 = new BranchShortLinkBuilder(getTestContext())
                        .setChannel("facebook")
                        .addTags(tags)
                        .getShortUrl();

                Assert.assertNotNull(urlFB2);
                Assert.assertEquals(urlFB2, urlFB.val);

                tags.add("z");

                String urlFB3 = new BranchShortLinkBuilder(getTestContext())
                        .setChannel("facebook")
                        .addTags(tags)
                        .getShortUrl();

                Assert.assertNotNull(urlFB3);
                Assert.assertNotEquals(urlFB3, urlFB.val);
            }
        });
    }

    @Test
    public void test01SetIdentity() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                final CountDownLatch signal = new CountDownLatch(1);
                prefHelper.setIdentity(PrefHelper.NO_STRING_VALUE);
                branch.setIdentity("test_user_1", new Branch.BranchReferralInitListener() {
                    @Override
                    public void onInitFinished(JSONObject referringParams, BranchError error) {
                        Assert.assertNull(error);
                        Assert.assertNotNull(referringParams);
                        Assert.assertEquals(prefHelper.getRandomizedBundleToken(), "880938553226608667");

                        JSONObject installParams = branch.getFirstReferringParams();
                        try {
                            Assert.assertEquals(installParams.getString("name"), "test name");
                            Assert.assertEquals(installParams.getString("message"), "hello there with short url");
                        } catch (JSONException e) {
                            BranchLogger.d(e.getMessage());
                        }

                        signal.countDown();
                    }
                });
                try {
                    Assert.assertTrue(signal.await(TEST_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Assert.fail();
                }
            }
        });
    }

    @Test
    public void testZLoad() {
        final long now = System.currentTimeMillis();
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                BranchLogger.v("benas after init session = " + (now - System.currentTimeMillis()));
                final CountDownLatch signalFinal = new CountDownLatch(1);
                final CountDownLatch signal = new CountDownLatch(1);
                final int reps = 20;
                final AtomicInteger callbackInvocations = new AtomicInteger(0);
                for (int i = 0; i < reps; i++) {
                    new BranchShortLinkBuilder(getTestContext())
                            .setChannel(i + "")
                            .generateShortUrl(new BranchLinkCreateListener() {
                                @Override
                                public void onLinkCreate(String url, BranchError error) {
                                    BranchLogger.v("benas callback " + callbackInvocations.get() + ": " + (now - System.currentTimeMillis()));
                                    BranchLogger.v("url = " + url + ", error = " + error);
                                    Assert.assertNull(error);
                                    Assert.assertNotNull(url);
                                    BranchLogger.v("idx = " + callbackInvocations.get());
                                    if (callbackInvocations.getAndIncrement() == reps - 1) {
                                        signal.countDown();
                                    }
                                }
                            });
                }
                try {
                    Assert.assertTrue(signal.await((reps * TEST_REQUEST_TIMEOUT + TEST_INIT_SESSION_TIMEOUT), TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Assert.fail("timeout");
                }

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

                try {
                    Assert.assertTrue(signalFinal.await(TEST_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Assert.fail("timeout");
                }
                Assert.assertNotNull(activityScenario);
            }
        });
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
        Assert.assertEquals(io.branch.referral.BuildConfig.VERSION_NAME, Branch.getSdkVersionNumber());
    }

    private void getFBUrl(final FBUrl res) throws InterruptedException {
        getFBUrl(res, null, null);
    }

    private void getFBUrl(final FBUrl res, @Nullable JSONObject params, @Nullable List<String> tags) throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        BranchShortLinkBuilder linkBuilder = new BranchShortLinkBuilder(getTestContext()).setChannel("facebook");
        if (params != null) linkBuilder.setParameters(params);
        if (tags != null) linkBuilder.addTags(tags);
        linkBuilder.generateShortUrl(new BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                Assert.assertNull(error);
                Assert.assertNotNull(url);
                // long url route = "/a/", short url route = "/l/"
                Assert.assertTrue(url.startsWith("https://bnc.lt/l/"));
                res.val = url;

                signal.countDown();
            }
        });
        Assert.assertTrue(signal.await(TEST_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private static class FBUrl {
        public String val;
        FBUrl(String url) { val = url; }
    }
}
