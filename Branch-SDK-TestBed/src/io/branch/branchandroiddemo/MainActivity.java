package io.branch.branchandroiddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.branchandroiddemo.test.BUOTestRoutines;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.BranchError;
import io.branch.referral.BranchViewHandler;
import io.branch.referral.SharingHelper;
import io.branch.referral.util.BranchEventData;
import io.branch.referral.util.BranchStandardEvents;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ProductCategory;
import io.branch.referral.util.ShareSheetStyle;
import io.branch.referral.util.TrackCustomEventBuilder;
import io.branch.referral.util.TrackEventBuilder;
import io.branch.referral.util.TrackStandardEventBuilder;

public class MainActivity extends Activity {
    Branch branch;

    EditText txtShortUrl;
    TextView txtInstallCount;
    TextView txtRewardBalance;

    BranchUniversalObject branchUniversalObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtShortUrl = (EditText) findViewById(R.id.editReferralShortUrl);
        txtInstallCount = (TextView) findViewById(R.id.txtInstallCount);
        txtRewardBalance = (TextView) findViewById(R.id.txtRewardBalance);

        // Create a BranchUniversal object for the content referred on this activity instance
        branchUniversalObject = new BranchUniversalObject()
                .setCanonicalIdentifier("item/12345")
                .setCanonicalUrl("https://branch.io/deepviews")
                .setTitle("My Content Title")
                .setContentDescription("My Content Description ")
                .setContentImageUrl("https://example.com/mycontent-12345.png")
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                .setContentType("application/vnd.businessobjects")
                        //.setContentExpiration(new Date(1476566432000L)) // set contents expiration time if applicable
                .setPrice(5.00)
                .addKeyWord("My_Keyword1")
                .addKeyWord("My_Keyword2")
                .addContentMetadata("Metadata_Key1", "Metadata_value1")
                .addContentMetadata("Metadata_Key2", "Metadata_value2");


        findViewById(R.id.cmdIdentifyUser).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.setIdentity("test_user_10", new BranchReferralInitListener() {
                    @Override
                    public void onInitFinished(JSONObject referringParams, BranchError error) {
                        if (error != null) {
                            Log.i("BranchTestBed", "branch set Identity failed. Caused by -" + error.getMessage());
                        } else {
                            Log.i("BranchTestBed", "install params = " + referringParams.toString());
                        }
                    }
                });
            }
        });

        findViewById(R.id.cmdClearUser).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.logout(new Branch.LogoutStatusListener() {
                    @Override
                    public void onLogoutFinished(boolean loggedOut, BranchError error) {
                        Log.i("BranchTestBed", "onLogoutFinished " + loggedOut + " errorMessage " + error);
                    }
                });

                txtRewardBalance.setText("rewards = ");
                txtInstallCount.setText("install count =");
            }
        });

        findViewById(R.id.cmdPrintInstallParam).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject obj = branch.getFirstReferringParams();
                Log.i("BranchTestBed", "install params = " + obj.toString());
            }
        });

        findViewById(R.id.cmdRefreshShortURL).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                LinkProperties linkProperties = new LinkProperties()
                        .addTag("Tag1")
                        .setChannel("Sharing_Channel_name")
                        .setFeature("my_feature_name")
                        .addControlParameter("$android_deeplink_path", "custom/path/*")
                        .addControlParameter("$ios_url", "http://example.com/ios")
                        .setDuration(100);
                //.setAlias("myContentName") // in case you need to white label your link

                // Sync link create example
                txtShortUrl.setText(branchUniversalObject.getShortUrl(MainActivity.this, linkProperties));

                // Async Link creation example
               /* branchUniversalObject.generateShortUrl(MainActivity.this, linkProperties, new Branch.BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        String shortUrl = url;
                    }
                });*/

            }

        });


        findViewById(R.id.cmdRefreshReward).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                branch.loadRewards(new BranchReferralStateChangedListener() {
                    @Override
                    public void onStateChanged(boolean changed, BranchError error) {
                        if (error != null) {
                            Log.i("BranchTestBed", "branch load rewards failed. Caused by -" + error.getMessage());
                        } else {
                            Log.i("BranchTestBed", "changed = " + changed);
                            txtRewardBalance.setText("rewards = " + branch.getCredits());
                        }
                    }
                });
            }
        });

        findViewById(R.id.cmdRedeemFive).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.redeemRewards(5, new BranchReferralStateChangedListener() {
                    @Override
                    public void onStateChanged(boolean changed, BranchError error) {
                        if (error != null) {
                            Log.i("BranchTestBed", "branch redeem rewards failed. Caused by -" + error.getMessage());
                        } else {
                            if (changed) {
                                Log.i("BranchTestBed", "redeemed rewards = " + true);
                                txtRewardBalance.setText("rewards = " + branch.getCredits());
                            } else {
                                Log.i("BranchTestBed", "redeem rewards unknown error ");
                            }
                        }
                    }
                });
            }
        });

        findViewById(R.id.cmdCommitBuyAction).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.userCompletedAction("buy", new BranchViewHandler.IBranchViewEvents() {
                    @Override
                    public void onBranchViewVisible(String action, String branchViewID) {
                        Log.i("BranchTestBed", "onBranchViewVisible");
                    }

                    @Override
                    public void onBranchViewAccepted(String action, String branchViewID) {
                        Log.i("BranchTestBed", "onBranchViewAccepted");
                    }

                    @Override
                    public void onBranchViewCancelled(String action, String branchViewID) {
                        Log.i("BranchTestBed", "onBranchViewCancelled");
                    }

                    @Override
                    public void onBranchViewError(int errorCode, String errorMsg, String action) {
                        Log.i("BranchTestBed", "onBranchViewError " + errorMsg);
                    }
                });
            }
        });

        findViewById(R.id.cmdCommitBuyMetadataAction).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                JSONObject params = new JSONObject();
                try {
                    params.put("name", "Alex");
                    params.put("boolean", true);
                    params.put("int", 1);
                    params.put("double", 0.13415512301);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                branch.userCompletedAction("buy", params);
            }

        });

        findViewById(R.id.cmdGetCreditHistory).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("BranchTestBed", "Getting credit history...");
                Intent i = new Intent(getApplicationContext(), CreditHistoryActivity.class);
                startActivity(i);
            }
        });


        findViewById(R.id.report_view_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branchUniversalObject.registerView();
                // List on google search
                branchUniversalObject.listOnGoogleSearch(MainActivity.this);
            }
        });

        findViewById(R.id.share_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject obj = new JSONObject();
                LinkProperties linkProperties = new LinkProperties()
                        .addTag("myShareTag1")
                        .addTag("myShareTag2")
                                //.setAlias("mylinkName") // In case you need to white label your link
                        .setChannel("myShareChannel2")
                        .setFeature("mySharefeature2")
                        .setStage("10")
                        .setCampaign("Android campaign")
                        .addControlParameter("$android_deeplink_path", "custom/path/*")
                        .addControlParameter("$ios_url", "http://example.com/ios")
                        .setDuration(100);

                //noinspection deprecation
                ShareSheetStyle shareSheetStyle = new ShareSheetStyle(MainActivity.this, "My Sharing Message Title", "My Sharing message body")
                        .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), "Save this URl", "Link added to clipboard")
                        .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.TWITTER)
                        .setAsFullWidthStyle(true)
                        .setSharingTitle("Share With");
                // Define custom style for the share sheet list view
                //.setStyleResourceID(R.style.Share_Sheet_Style);

                branchUniversalObject.showShareSheet(MainActivity.this, linkProperties, shareSheetStyle, new Branch.BranchLinkShareListener() {

                            @Override
                            public void onShareLinkDialogLaunched() {
                            }

                            @Override
                            public void onShareLinkDialogDismissed() {
                            }

                            @Override
                            public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
                            }

                            @Override
                            public void onChannelSelected(String channelName) {
                            }

                        },
                        new Branch.IChannelProperties() {
                            @Override
                            public String getSharingTitleForChannel(String channel) {
                                return channel.contains("Messaging") ? "title for SMS" :
                                        channel.contains("Slack") ? "title for slack" :
                                                channel.contains("Gmail") ? "title for gmail" : null;
                            }

                            @Override
                            public String getSharingMessageForChannel(String channel) {
                                return channel.contains("Messaging") ? "message for SMS" :
                                        channel.contains("Slack") ? "message for slack" :
                                                channel.contains("Gmail") ? "message for gmail" : null;
                            }
                        });

            }
        });

        // Add optional deep link debug params
        //        try {
        //            JSONObject debugObj = new JSONObject();
        //            debugObj.put("DeeplinkTestKey1", "DeeplinkTestValue1");
        //            debugObj.put("DeeplinkTestKey2", "DeeplinkTestValue2");
        //            Branch.getInstance().setDeepLinkDebugMode(debugObj);
        //        }catch (JSONException ignore){
        //        }

        // Tracking events
        findViewById(R.id.cmdTrackCustomEvent).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new TrackCustomEventBuilder("Trading_Commodity")
                        .addCustomData("Trading_Item", "CLD")
                        .addCustomData("Sell_Rate", "+1.2%")
                        .track(MainActivity.this);
            }
        });

        findViewById(R.id.cmdTrackStandardEvent).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new TrackStandardEventBuilder(BranchStandardEvents.PURCHASE)
                        .addContentItems(
                                new BranchUniversalObject()
                                        .setCanonicalIdentifier("canonicalID/1234")
                                        .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                                        .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                                        .setPrice(101.20)
                                        .setProductBrand("my_brand1")
                                        .setProductCategory(ProductCategory.SPORTING_GOODS)
                                        .setProductName("my_product1")
                                        .setProductVariant("my_product_variant_1")
                                        .setQuantity(1D)
                                        .setRatingCount(5)
                                        .setMaximumRating(2.2)
                                        .setAverageRating(4.2)
                                        .setSku("1101123445")
                                        .setTitle("my_product_title1")
                                        .setContentDescription("my_product_description1")
                                        .setAddress("2440 Ash Street", "Palo Alto", "CA", "USA", "95067"),

                                new BranchUniversalObject()
                                        .setCanonicalIdentifier("canonicalID/5324")
                                        .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                                        .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                                        .setPrice(80.20)
                                        .setProductBrand("my_brand2")
                                        .setProductCategory(ProductCategory.APPAREL_AND_ACCESSORIES)
                                        .setProductName("my_product2")
                                        .setProductVariant("my_product_variant_2")
                                        .setQuantity(5d)
                                        .setRatingCount(5)
                                        .setMaximumRating(2.8)
                                        .setAverageRating(3.3)
                                        .setSku("110112467")
                                        .setTitle("my_product_title2")
                                        .setContentDescription("my_product_description2")
                                        .setAddress("2440 Heaven Lane", "Sand city", null, "USA", "95067")

                        )
                        .addCustomData("purchase_loc", "Palo Alto")
                        .addCustomData("store_pickup", "unavailable")
                        .addEventData(new BranchEventData("tras_Id_1232343434")
                                .setAffiliation("high_fi")
                                .setCoupon("promo-1234")
                                .setCurrency(CurrencyType.USD)
                                .setDescription("Preferred purchase")
                                .setRevenue(180.2)
                                .setShipping(10.5)
                                .setTax(13.5))
                        .setCallback(new TrackEventBuilder.ITrackEventListener() {
                            @Override
                            public void onEventTracked() {
                                Log.d("BranchTestBed", "onEventTracked");
                            }

                            @Override
                            public void onEventTrackingFailed(BranchError branchError) {
                                Log.d("BranchTestBed", "onEventTrackingFailed " + branchError.getMessage());
                            }
                        }).track(MainActivity.this);
            }
        });

        BUOTestRoutines.TestBUOSerialisation();
    }


    @Override
    protected void onStart() {
        super.onStart();
        branch = Branch.getInstance();
        branch.initSession(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error != null) {
                    Log.i("BranchTestBed", "branch init failed. Caused by -" + error.getMessage());
                } else {
                    Log.i("BranchTestBed", "branch init complete!");
                    if (branchUniversalObject != null) {
                        Log.i("BranchTestBed", "title " + branchUniversalObject.getTitle());
                        Log.i("BranchTestBed", "CanonicalIdentifier " + branchUniversalObject.getCanonicalIdentifier());
                        Log.i("ContentMetaData", "metadata " + branchUniversalObject.getMetadata());

                    }

                    if (linkProperties != null) {
                        Log.i("BranchTestBed", "Channel " + linkProperties.getChannel());
                        Log.i("BranchTestBed", "control params " + linkProperties.getControlParams());
                    }
                }
            }
        }, this.getIntent().getData(), this);

    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Checking if the previous activity is launched on branch Auto deep link.
        if (requestCode == getResources().getInteger(R.integer.AutoDeeplinkRequestCode)) {
            //Decide here where  to navigate  when an auto deep linked activity finishes.
            //For e.g. Go to HomeActivity or a  SignUp Activity.
            Intent i = new Intent(getApplicationContext(), CreditHistoryActivity.class);
            startActivity(i);
        }
    }
}
