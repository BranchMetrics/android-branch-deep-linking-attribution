package io.branch.referral;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.QRCode.BranchQRCode;
import io.branch.referral.util.LinkProperties;

@RunWith(AndroidJUnit4.class)
public class BranchQRCodeTests extends BranchTest {

    @Before
    public void initializeValues(){
        initBranchInstance();
    }

    @Test
    public void testQRCodeWithSettings() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                BranchQRCode qrCode = new BranchQRCode()
                        .setCodeColor("#a4c639")
                        .setBackgroundColor(Color.WHITE)
                        .setMargin(1)
                        .setWidth(512)
                        .setImageFormat(BranchQRCode.BranchImageFormat.PNG)
                        .setCenterLogo("https://cdn.branch.io/branch-assets/1598575682753-og_image.png");

                BranchUniversalObject buo = new BranchUniversalObject()
                        .setCanonicalIdentifier("test/123")
                        .setTitle("My Test Title")
                        .setContentDescription("My Test Description")
                        .setContentImageUrl("https://lorempixel.com/400/400");

                LinkProperties lp = new LinkProperties()
                        .setChannel("facebook")
                        .setFeature("sharing")
                        .setCampaign("test 123 launch")
                        .setStage("test");

                final CountDownLatch lock = new CountDownLatch(1);

                try {
                    qrCode.getQRCodeAsData(getTestContext(), buo, lp, new BranchQRCode.BranchQRCodeDataHandler() {
                        @Override
                        public void onSuccess(byte[] qrCodeData) {
                            Assert.assertNotNull(qrCodeData);
                            lock.countDown();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            }
        });
    }

    @Test
    public void testQRCodeWithNoSettings() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                BranchQRCode qrCode = new BranchQRCode();

                BranchUniversalObject buo = new BranchUniversalObject()
                        .setCanonicalIdentifier("test/123")
                        .setTitle("My Test Title")
                        .setContentDescription("My Test Description")
                        .setContentImageUrl("https://lorempixel.com/400/400");

                LinkProperties lp = new LinkProperties()
                        .setChannel("facebook")
                        .setFeature("sharing")
                        .setCampaign("test 123 launch")
                        .setStage("test");

                final CountDownLatch lock = new CountDownLatch(1);

                try {
                    qrCode.getQRCodeAsData(getTestContext(), buo, lp, new BranchQRCode.BranchQRCodeDataHandler() {
                        @Override
                        public void onSuccess(byte[] qrCodeData) {
                            Assert.assertNotNull(qrCodeData);
                            lock.countDown();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            }
        });
    }

    @Test
    public void testQRCodeAsImage() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                BranchQRCode qrCode = new BranchQRCode();
                BranchUniversalObject buo = new BranchUniversalObject();
                LinkProperties lp = new LinkProperties();

                final CountDownLatch lock = new CountDownLatch(1);

                try {
                    qrCode.getQRCodeAsImage(branch.getCurrentActivity(), buo, lp, new BranchQRCode.BranchQRCodeImageHandler() {
                        @Override
                        public void onSuccess(Bitmap qrCodeImage) {
                            Assert.assertNotNull(qrCodeImage);
                            lock.countDown();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            }
        });
    }

    @Test
    public void testQRCodeCache() {
        initSessionResumeActivity(null, new Runnable() {
            @Override
            public void run() {
                final BranchQRCode qrCode = new BranchQRCode();
                BranchUniversalObject buo = new BranchUniversalObject();
                LinkProperties lp = new LinkProperties();

                final CountDownLatch lock = new CountDownLatch(1);
                try {

                    qrCode.getQRCodeAsData(getTestContext(), buo, lp, new BranchQRCode.BranchQRCodeDataHandler() {
                        @Override
                        public void onSuccess(byte[] qrCodeData) {
                            try {
                                JSONObject expectedCachedParams = new JSONObject("{\"feature\":\"Share\",\"stage\":\"\",\"data\":{\"$publicly_indexable\":true,\"$locally_indexable\":true},\"channel\":\"\",\"qr_code_settings\":{\"image_format\":\"PNG\"},\"campaign\":\"\",\"branch_key\":\"key_live_testing_only\",\"tags\":[]}");
                                byte[] cachedQRCodeData = BranchQRCodeCache.getInstance().checkQRCodeCache(expectedCachedParams);

                                Assert.assertEquals(qrCodeData, cachedQRCodeData);
                                lock.countDown();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            }
        });


    }


    }