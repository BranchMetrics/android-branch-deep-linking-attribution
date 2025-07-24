package io.branch.branchandroiddemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import io.branch.branchandroiddemo.activities.LogDataActivity;
import io.branch.branchandroiddemo.activities.ShowQRCodeActivity;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.QRCode.BranchQRCode;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.LinkProperties;

public class BranchWrapper {

    static BranchWrapper branchwrapper;

    public Context ctx;

    public BranchWrapper(Context pCtx) {
        ctx = pCtx;
    }

    public void showLogWindow(String message, boolean isError, Context ctx, String buttonType) {

        Intent intent = new Intent(ctx, LogDataActivity.class);
        if (isError) {
            intent.putExtra(Constants.STATUS, Constants.FAIL);
        } else {
            intent.putExtra(Constants.STATUS, Constants.SUCCESS);
        }
        intent.putExtra(Constants.TYPE, buttonType);
        intent.putExtra(Constants.MESSAGE, message);
        ctx.startActivity(intent);
    }

    public void createDeepLink(Intent intent, Context ctx) {
        Common.getInstance().clearLog();
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {
            TestData testDataObj = new TestData();
            BranchUniversalObject buo = testDataObj.getParamBUOObject(testDataStr);
            LinkProperties lp = testDataObj.getParamLinkPropertiesObject(testDataStr);

            if (buo != null && lp != null) {
                String url = buo.getShortUrl(ctx, lp);
                showLogWindow("", false, ctx, Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK);
            } else {
                showLogWindow("Invalid Params (BUO / lp) ", true, ctx, Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK);
            }
        } else {
            showLogWindow("Test Data : Null", true, ctx, Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK);
        }
    }

    public void nativeShare(Activity activity, Intent intent, Context ctx) {
        Common.getInstance().clearLog();
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {
            TestData testDataObj = new TestData();
            BranchUniversalObject buo = testDataObj.getParamBUOObject(testDataStr);
            LinkProperties lp = testDataObj.getParamLinkPropertiesObject(testDataStr);

            if (buo != null && lp != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    Branch.init().share(activity, buo, lp, "Sharing Branch Short URL", "Using Native Chooser Dialog");
                } else {
                    showLogWindow("Unsupported Version", false, ctx, Constants.UNKNOWN);
                }
            } else {
                showLogWindow("Invalid Params (BUO / lp) ", true, ctx, Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK);
            }
        } else {
            showLogWindow("Test Data : Null", true, ctx, Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK);
        }
    }

    public void logEvent(Intent intent, Context ctx){
        Common.getInstance().clearLog();
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {
            TestData testDataObj = new TestData();
            BranchUniversalObject buo = testDataObj.getParamBUOObject(testDataStr);
            BranchEvent branchEvent = testDataObj.getBranchEventObject(testDataStr);

            if ( buo != null && branchEvent != null) {
                branchEvent.addContentItems(buo);
                branchEvent.logEvent(ctx);
                showLogWindow("",false, ctx,Constants.TRACK_CONTENT);
            } else {
                showLogWindow("Invalid Params (BUO / event) ",true, ctx,Constants.TRACK_CONTENT);
            }
        } else {
            showLogWindow( "Test Data : Null" , true, ctx,Constants.TRACK_CONTENT);
        }
    }
    public void setIdentity(Intent intent, Context ctx){
        Common.getInstance().clearLog();
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {
            TestData testDataObj = new TestData();
            String userName = testDataObj.getUserName(testDataStr);
            if ( userName != null && (userName.isEmpty() == false)) {
                Branch.init().setIdentity(userName);
            } else {
                showLogWindow("Invalid username.",true, ctx,Constants.TRACK_USER);
            }
        } else {
            showLogWindow( "Test Data : Null" , true, ctx,Constants.TRACK_USER);
        }
    }

    public void delayInitializationIfRequired(Intent intent){
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {
            TestData testDataObj = new TestData();
            Boolean delayInit = testDataObj.getBoolParamValue(testDataStr, "DelayInitialization");
        }
    }

    public void initSession(Activity activity, Intent intent, Context ctx){
        Common.getInstance().clearLog();
        intent.putExtra(Constants.FORCE_NEW_SESSION, true);
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {
            TestData testDataObj = new TestData();
            String url = testDataObj.getParamValue(testDataStr, "URL");
            if ( url != null && (url.isEmpty() == false)) {
                Uri uri = Uri.parse(url);
                Branch.InitSessionBuilder initSessionBuilder = Branch.sessionBuilder(activity).withCallback(new Branch.BranchReferralInitListener() {
                    @Override
                    public void onInitFinished(JSONObject referringParams, BranchError error) {
                        if (error == null) {
                            Log.i("BRANCH SDK", referringParams.toString());
                        } else {
                            Log.i("BRANCH SDK error", error.getMessage());
                        }
                        showLogWindow("",false, ctx,Constants.INIT_SESSION);
                    }
                }).withData(uri);
                initSessionBuilder.init();
            } else {
                showLogWindow("Invalid URL.",true, ctx,Constants.INIT_SESSION);
            }
        } else {
            showLogWindow( "Test Data : Null" , true, ctx,Constants.INIT_SESSION);
        }
    }

    public void setDMAParams(Intent intent){

        Common.getInstance().clearLog();
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {

            TestData testDataObj = new TestData();
            boolean eeaRegion = testDataObj.getBoolParamValue(testDataStr,"dma_eea");
            boolean adPersonalizationConsent = testDataObj.getBoolParamValue(testDataStr,"dma_ad_personalization");
            boolean adUserDataUsageConsent = testDataObj.getBoolParamValue(testDataStr,"dma_ad_user_data");

            Branch.init().setDMAParamsForEEA(eeaRegion, adPersonalizationConsent, adUserDataUsageConsent);

        } else {
            showLogWindow( "Test Data : Null" , true, ctx,Constants.SET_DMA_Params);
        }
    }

    public void getQRCode(Activity activity, Intent intent, Context ctx){
        Common.getInstance().clearLog();
        String testDataStr = intent.getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);
        if (testDataStr != null) {
            TestData testDataObj = new TestData();
            BranchUniversalObject buo = testDataObj.getParamBUOObject(testDataStr);
            LinkProperties lp = testDataObj.getParamLinkPropertiesObject(testDataStr);

            if (buo != null && lp != null) {
                Log.e("qr code","create qr code");
                BranchQRCode qrCode = new BranchQRCode() //All QR code settings are optional
                        .setCodeColor("#a4c639")
                        .setBackgroundColor(Color.WHITE)
                        .setMargin(1)
                        .setWidth(512)
                        .setImageFormat(BranchQRCode.BranchImageFormat.PNG)
                        .setCenterLogo("https://cdn.branch.io/branch-assets/1598575682753-og_image.png");
                try {
                    qrCode.getQRCodeAsImage(activity, buo, lp, new BranchQRCode.BranchQRCodeImageHandler() {
                        @Override
                        public void onSuccess(Bitmap qrCodeImage) {

                            //Do something with the QR code here.
                            Log.e("qr code","generated");
                            Common.qrCodeImage = qrCodeImage;
                            Intent qrIntent = new Intent(activity, ShowQRCodeActivity.class);
                            ctx.startActivity(qrIntent);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.d("Failed to get QR code", String.valueOf(e));
                            showLogWindow("Failed to get QR code" , true, ctx, Constants.CREATE_QR_CODE);

                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                showLogWindow("Invalid Params (BUO / lp) ", true, ctx, Constants.CREATE_QR_CODE);
            }
        } else {
            showLogWindow("Test Data : Null", true, ctx, Constants.CREATE_QR_CODE);
        }
    }
}
