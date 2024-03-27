package io.branch.saas.sdk.testbed.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.saas.sdk.testbed.Common;
import io.branch.saas.sdk.testbed.Constants;
import io.branch.saas.sdk.testbed.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btBuoObj, btCreateDeepLink, btShare, btNativeShare, btTrackUser,
            btReadDeepLink, btNotification, btBuoObjWithMeta, btNavigateToContent, btTrackContent, btHandleLinks, btCreateQrCode, btSetDMAParams, btLogEvent;
    private TextView tvMessage, tvUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btBuoObj = findViewById(R.id.bt_buo_obj);
        btCreateDeepLink = findViewById(R.id.bt_create_deep_link);
        btShare = findViewById(R.id.bt_share);
        btNativeShare = findViewById(R.id.bt_native_share);
        btReadDeepLink = findViewById(R.id.bt_read_deep_link);
        tvMessage = findViewById(R.id.tv_message);
        tvUrl = findViewById(R.id.tv_url);
        btBuoObjWithMeta = findViewById(R.id.bt_buo_obj_with_meta);
        btNavigateToContent = findViewById(R.id.bt_navigate_to_content);
        btTrackContent = findViewById(R.id.bt_track_content);
        btCreateQrCode = findViewById(R.id.bt_create_qr_code);
        btSetDMAParams = findViewById(R.id.bt_set_dma_params);
        btLogEvent = findViewById(R.id.bt_logEvent_with_callback);
        btHandleLinks = findViewById(R.id.bt_handle_links);

        ToggleButton trackingCntrlBtn = findViewById(R.id.tracking_cntrl_btn);
        btBuoObj.setOnClickListener(this);
        btCreateDeepLink.setOnClickListener(this);
        btShare.setOnClickListener(this);
        btNativeShare.setOnClickListener(this);
        btTrackContent.setOnClickListener(this);
        btHandleLinks.setOnClickListener(this);
        btTrackUser = findViewById(R.id.bt_track_user);
        btNotification = findViewById(R.id.bt_notification);
        btNotification.setOnClickListener(this);
        btTrackUser.setOnClickListener(this);
        btReadDeepLink.setOnClickListener(this);
        btBuoObjWithMeta.setOnClickListener(this);
        btNavigateToContent.setOnClickListener(this);
        btCreateQrCode.setOnClickListener(this);
        btSetDMAParams.setOnClickListener(this);
        btLogEvent.setOnClickListener(this);

        trackingCntrlBtn.setChecked(Branch.getInstance().isTrackingDisabled());
        trackingCntrlBtn.setOnCheckedChangeListener((buttonView, isChecked) -> Branch.getInstance().disableTracking(isChecked));


        /*LinkProperties lp = new LinkProperties()
                .setChannel("facebook")
                .setFeature("sharing")
                .setCampaign("content 123 launch")
                .setStage("new user")
                .addControlParameter("$desktop_url", "https://example.com/home")
                .addControlParameter("custom", "data")
                .addControlParameter("custom_random", Long.toString(Calendar.getInstance().getTimeInMillis()));

        buo.generateShortUrl(this, lp, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (error == null) {
                    Log.i("BRANCH SDK", "got my Branch link to share: " + url);
                    Toast.makeText(MainActivity.this, "link==>" + url, Toast.LENGTH_SHORT).show();
                }
            }
        });*/
    }

    @Override
    public void onClick(View view) {
        if (view == btSetDMAParams) {
            Branch.expectDelayedSessionInitialization(true);
        } else {
            Branch.getInstance().setIdentity("automation_test_user");
        }
        Branch.enableLogging();
        if (view == btBuoObj) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.BUO_REFERENCE);
            startActivity(intent);
        } else if (view == btCreateDeepLink) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK);
            startActivity(intent);
        } else if (view == btNativeShare) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.CREATE_NATIVE_SHARE_LINK);
            startActivity(intent);
        } else if (view == btShare) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.CREATE_SHARE_LINK);
            startActivity(intent);
        } else if (view == btTrackUser) {
            Common.getInstance().clearLog();
            Branch.getInstance().setIdentity("automation_test_user", (referringParams, error) -> {
                Intent intent = new Intent(MainActivity.this, LogDataActivity.class);
                intent.putExtra(Constants.TYPE, Constants.TRACK_USER);
                if (referringParams != null) {
                    intent.putExtra(Constants.MESSAGE, "Track User Data");
                    Log.i("track user result-->", "" + referringParams);
                    intent.putExtra(Constants.STATUS, Constants.SUCCESS);
                } else {
                    intent.putExtra(Constants.MESSAGE, "Failed to load Track User Data");
                    intent.putExtra(Constants.STATUS, Constants.FAIL);
                    Log.i("track user  error-->", "" + error.getMessage());
                }
                startActivity(intent);
            });
        } else if (view == btNotification) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.CREATE_SEND_NOTIFICATION);
            startActivity(intent);
        } else if (view == btReadDeepLink) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.CREATE_SEND_READ_DEEP_LINK);
            startActivity(intent);
            finish();
        } else if (view == btNavigateToContent) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.NAVIGATE_TO_CONTENT);
            startActivity(intent);
        } else if (view == btTrackContent) {
            Intent intent = new Intent(this, TrackContentActivity.class);
            startActivity(intent);
        } else if (view == btCreateQrCode) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.CREATE_QR_CODE);
            startActivity(intent);
        } else if (view == btHandleLinks) {
            Intent intent = new Intent(this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.HANDLE_LINKS);
            startActivity(intent);
        } else if (view == btSetDMAParams) {

            getIntent().putExtra("branch_force_new_session", true);

            Intent intent = new Intent(this, LogDataActivity.class);
            intent.putExtra(Constants.TYPE, Constants.SET_DMA_Params);
            String paramsString = (String) getIntent().getStringExtra("testData");

            if (paramsString == null) {
                intent.putExtra(Constants.STATUS, Constants.FAIL);
                intent.putExtra(Constants.MESSAGE, "Failed to get input params. This test requires DMA related params set as intent extras.\n Example: getIntent().putExtra(\"testData\", \"{\\\"dma_eea\\\":\\\"Yes\\\",\\\"dma_ad_personalization\\\":\\\"Yes\\\",\\\"dma_ad_user_data\\\":\\\"Yes\\\",\\\"Include\\\":\\\"Yes\\\",\\\"URL\\\":\\\"https:\\\\/\\\\/timber.test-app.link\\\\/80OHAnv8DHb\\\"}\");");
                startActivity(intent);
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(paramsString);
                    String url = jsonObject.getString("URL");
                    boolean eeaRegion = (jsonObject.getString("dma_eea").equalsIgnoreCase("yes"))? true:false;
                    boolean adPersonalizationConsent = (jsonObject.getString("dma_ad_personalization").equalsIgnoreCase("yes"))? true:false;
                    boolean adUserDataUsageConsent = (jsonObject.getString("dma_ad_user_data").equalsIgnoreCase("yes"))? true:false;

                    Branch.getInstance().setDMAParamsForEEA(eeaRegion, adPersonalizationConsent, adUserDataUsageConsent);

                    intent.putExtra(Constants.ANDROID_URL, url);
                    Common.getInstance().clearLog();
                    Branch.sessionBuilder(this).withCallback(new Branch.BranchReferralInitListener() {
                        @Override
                        public void onInitFinished(JSONObject referringParams, BranchError error) {
                            if (error == null) {
                                Log.i("BRANCH SDK", referringParams.toString());
                                intent.putExtra(Constants.STATUS, Constants.SUCCESS);

                            } else {
                                Log.i("BRANCH SDK", error.getMessage());
                                intent.putExtra(Constants.STATUS, Constants.FAIL);
                                intent.putExtra(Constants.MESSAGE, "Failed:" + error.getMessage());
                            }
                            startActivity(intent);
                        }
                    }).withData(Uri.parse(url)).init();
                } catch (JSONException e) {
                    intent.putExtra(Constants.STATUS, Constants.FAIL);
                    intent.putExtra(Constants.MESSAGE, "Failed:" + e.getMessage() + paramsString);
                    startActivity(intent);
                }
            }
        } else if (view == btLogEvent) {
            Intent intent = new Intent(this, LogDataActivity.class);
            intent.putExtra(Constants.TYPE, Constants.SET_DMA_Params);
            // TODO :  We can improve this test by passing params for creating event as intent extras
            BranchUniversalObject buo = new BranchUniversalObject().setCanonicalIdentifier("content/12345");
            Common.getInstance().clearLog();

            new BranchEvent(BRANCH_STANDARD_EVENT.ADD_TO_CART)
                    .setCustomerEventAlias("my_custom_alias")
                    .setDescription("Customer added item to cart")
                    .setSearchQuery("Test Search query")
                    .addCustomDataProperty("Custom_Event_Property_Key1", "Custom_Event_Property_val1")
                    .addCustomDataProperty("Custom_Event_Property_Key2", "Custom_Event_Property_val2")
                    .addContentItems(buo) // Associate `BranchUniversalObject` with event
                    .logEvent(MainActivity.this, new BranchEvent.BranchLogEventCallback() {
                        @Override
                        public void onSuccess(int responseCode) {
                            Log.i("BRANCH SDK - ", "Sent Branch Commerce Event: " + responseCode);
                            intent.putExtra(Constants.STATUS, Constants.SUCCESS);
                            startActivity(intent);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.i("BRANCH SDK", e.getMessage());
                            intent.putExtra(Constants.STATUS, Constants.FAIL);
                            intent.putExtra(Constants.MESSAGE, "Failed:" + e.getMessage());
                            startActivity(intent);
                        }
                    });
        }
    }
}