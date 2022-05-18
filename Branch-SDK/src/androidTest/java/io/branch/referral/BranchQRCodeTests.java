package io.branch.referral;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

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

        try {
            qrCode.getQRCodeAsData(this.getTestContext(), buo, lp, new BranchQRCode.BranchQRCodeDataHandler() {
                @Override
                public void onSuccess(byte[] qrCodeData) {
                    Assert.assertNotNull(qrCodeData);
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

    @Test
    public void testQRCodeWithNoSettings() {
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

        try {
            qrCode.getQRCodeAsData(this.getTestContext(), buo, lp, new BranchQRCode.BranchQRCodeDataHandler() {
                @Override
                public void onSuccess(byte[] qrCodeData) {
                    Assert.assertNotNull(qrCodeData);
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

    @Test
    public void testQRCodeAsImage() {
        BranchQRCode qrCode = new BranchQRCode();
        BranchUniversalObject buo = new BranchUniversalObject();
        LinkProperties lp = new LinkProperties();

        try {
            qrCode.getQRCodeAsImage(this.branch.getCurrentActivity(), buo, lp, new BranchQRCode.BranchQRCodeImageHandler() {
                @Override
                public void onSuccess(Bitmap qrCodeImage) {
                    Assert.assertNotNull(qrCodeImage);
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

    @Test
    public void testQRCodeCache() {
        final BranchQRCode qrCode = new BranchQRCode();
        BranchUniversalObject buo = new BranchUniversalObject();
        LinkProperties lp = new LinkProperties();

        try {
            qrCode.getQRCodeAsData(this.getTestContext(), buo, lp, new BranchQRCode.BranchQRCodeDataHandler() {
                @Override
                public void onSuccess(byte[] qrCodeData) {
                    String paramsForCache = "{image_format=PNG}nullShare[]";
                    byte[] cachedQRCode = BranchQRCodeCache.getInstance().cache.get(paramsForCache);
                    Assert.assertEquals(qrCodeData, cachedQRCode);
                    Assert.assertNotNull(BranchQRCodeCache.getInstance().cache);
                    //Use Mock data instead
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


    }