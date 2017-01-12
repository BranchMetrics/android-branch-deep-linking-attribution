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

import java.math.BigDecimal;
import java.util.Random;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.BranchError;
import io.branch.referral.BranchViewHandler;
import io.branch.referral.SharingHelper;
import io.branch.referral.util.CommerceEvent;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.Product;
import io.branch.referral.util.ProductCategory;
import io.branch.referral.util.ShareSheetStyle;

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
                .setPrice(5.00, CurrencyType.USD)
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
                // Define custom styel for the share sheet list view
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
        sendCommerceEvent();
    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    private void sendCommerceEvent() {
//    ANIMALS_AND_PET_SUPPLIES("Animals & Pet Supplies"),
//    APPAREL_AND_ACCESSORIES("Apparel & Accessories"),
//    ARTS_AND_ENTERTAINMENT("Arts & Entertainment"),
//    BABY_AND_TODDLER("Baby & Toddler"),
//    BUSINESS_AND_INDUSTRIAL("Business & Industrial"),
//    CAMERA_AND_OPTICS("Camera & Optics"),
//    ELECTRONICS("Electronics"),
//    FOOD_BEVERAGE_AND_TOBACCO("Food, Beverage & Tobacco"),
//    FURNITURE("Furniture"),
//    HARDWARE("Hardware"),
//    HEALTH_AND_BEAUTY("Health & Beauty"),
//    HOME_AND_GARDEN("Home & Garden"),
//    LUGGAGE_AND_BAGS("Luggage & Bags"),
//    MATURE("mature"),
//    MEDIA("media"),
//    OFFICE_SUPPLIES("Office Supplies"),
//    RELIGIOUS_AND_CEREMONIAL("Religious & Ceremonial"),
//    SOFTWARE("Software"),
//    SPORTING_GOODS("Sporting Goods"),
//    TOYS_AND_GAMES("Toys & Games"),
//    VEHICLES_AND_PARTS("Vehicles & Parts");
        Random rnd = new Random(System.currentTimeMillis());
        Branch branch = Branch.getInstance();
        Product product = new Product("acme007", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.ANIMALS_AND_PET_SUPPLIES);
        Product product2 = new Product("acme008", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.APPAREL_AND_ACCESSORIES);
        Product product3 = new Product("acme009", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.ARTS_AND_ENTERTAINMENT);
        Product product4 = new Product("acme009", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.BABY_AND_TODDLER);
        Product product5 = new Product("acme009", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.BUSINESS_AND_INDUSTRIAL);
        Product product6 = new Product("acme009", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.CAMERA_AND_OPTICS);
        Product product7 = new Product("acme009", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.ELECTRONICS);
        Product product8 = new Product("acme009", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.FOOD_BEVERAGE_AND_TOBACCO);
        Product product9 = new Product("acme009", "Acme brand 1 ton weight", 1.01, rnd.nextInt(500), "Acme", "uh", ProductCategory.FURNITURE);
        Product product10 = new Product("acme009", "Acme brand 1 ton weight", 1.10, rnd.nextInt(500), "Acme", "uh", ProductCategory.HARDWARE);
        Product product11 = new Product("acme009", "Acme brand 1 ton weight", 1.10, rnd.nextInt(500), "Acme", "uh", ProductCategory.HEALTH_AND_BEAUTY);
        Product product12 = new Product("acme009", "Acme brand 1 ton weight", 1.10, rnd.nextInt(500), "Acme", "uh", ProductCategory.HOME_AND_GARDEN);
        Product product13 = new Product("acme009", "Acme brand 1 ton weight", 1.10, rnd.nextInt(500), "Acme", "uh", ProductCategory.LUGGAGE_AND_BAGS);
        Product product14 = new Product("acme009", "Acme brand 1 ton weight", 1.10, rnd.nextInt(500), "Acme", "uh", ProductCategory.MATURE);
        Product product15 = new Product("acme009", "Acme brand 1 ton weight", 1.10, rnd.nextInt(500), "Acme", "uh", ProductCategory.MEDIA);
        Product product16 = new Product("acme009", "Acme brand 1 ton weight", 1.10, rnd.nextInt(500), "Acme", "uh", ProductCategory.OFFICE_SUPPLIES);
        //Product product17 = new Product("acme009", "Acme brand 1 ton weight", 1.0, rnd.nextInt(500), "Acme", "uh", ProductCategory.RELIGIOUS_AND_CEREMONIAL);
        //Product product18 = new Product("acme009", "Acme brand 1 ton weight", 1.0, rnd.nextInt(500), "Acme", "uh", ProductCategory.SOFTWARE);
        //Product product19 = new Product("acme009", "Acme brand 1 ton weight", 1.0, rnd.nextInt(500), "Acme", "uh", ProductCategory.SPORTING_GOODS);
        //Product product20 = new Product("acme009", "Acme brand 1 ton weight", 1.0, rnd.nextInt(500), "Acme", "uh", ProductCategory.TOYS_AND_GAMES);
        //Product product21 = new Product("acme009", "Acme brand 1 ton weight", 1.0, rnd.nextInt(500), "Acme", "uh", ProductCategory.VEHICLES_AND_PARTS);
        CommerceEvent commerceEvent = new CommerceEvent(1101.99, CurrencyType.USD, "tr00x8", 100.25, 1.632,
                "Acme weights coupon", "ACME by Amazon", product);
        commerceEvent.addProduct(product2);
        commerceEvent.addProduct(product3);
        commerceEvent.addProduct(product4);
        commerceEvent.addProduct(product5);
        commerceEvent.addProduct(product6);
        commerceEvent.addProduct(product7);
        commerceEvent.addProduct(product8);
        commerceEvent.addProduct(product9);
        commerceEvent.addProduct(product10);
        commerceEvent.addProduct(product11);
        commerceEvent.addProduct(product12);
        commerceEvent.addProduct(product13);
        commerceEvent.addProduct(product14);
        commerceEvent.addProduct(product15);
        commerceEvent.addProduct(product16);
        //commerceEvent.addProduct(product17);
        //commerceEvent.addProduct(product18);
        //commerceEvent.addProduct(product19);
        //commerceEvent.addProduct(product20);
        //commerceEvent.addProduct(product21);
        JSONObject jsonObject = new JSONObject();
        try { jsonObject.put("metakey", "metavalue"); } catch ( JSONException e ) {}
        branch.sendCommerceEvent(commerceEvent, jsonObject, null);
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
