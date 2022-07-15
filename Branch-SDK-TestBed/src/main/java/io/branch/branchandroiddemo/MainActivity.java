package io.branch.branchandroiddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Date;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.BranchError;
import io.branch.referral.QRCode.BranchQRCode;
import io.branch.referral.BranchViewHandler;
import io.branch.referral.Defines;
import io.branch.referral.SharingHelper;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ProductCategory;
import io.branch.referral.util.ShareSheetStyle;


public class MainActivity extends Activity {
    private EditText txtShortUrl;
    private TextView txtInstallCount;

    private BranchUniversalObject branchUniversalObject;

    private final static String branchChannelID = "BranchChannelID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtShortUrl = findViewById(R.id.editReferralShortUrl);
        txtInstallCount = findViewById(R.id.txtInstallCount);
        ((ToggleButton) findViewById(R.id.tracking_cntrl_btn)).setChecked(Branch.getInstance().isTrackingDisabled());

        createNotificationChannel();

        // Create a BranchUniversal object for the content referred on this activity instance
        branchUniversalObject = new BranchUniversalObject()
                .setCanonicalIdentifier("item/12345")
                .setCanonicalUrl("https://branch.io/deepviews")
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                .setTitle("My Content Title")
                .setContentDescription("my_product_description1")
                .setContentImageUrl("https://example.com/mycontent-12345.png")
                .setContentImageUrl("https://test_img_url")
                .addKeyWord("My_Keyword1")
                .addKeyWord("My_Keyword2")
                .setContentMetadata(
                        new ContentMetadata().setProductName("my_product_name1")
                                .setProductBrand("my_prod_Brand1")
                                .setProductVariant("3T")
                                .setProductCategory(ProductCategory.BABY_AND_TODDLER)
                                .setProductCondition(ContentMetadata.CONDITION.EXCELLENT)
                                .setAddress("Street_name1", "city1", "Region1", "Country1", "postal_code")
                                .setLocation(12.07, -97.5)
                                .setSku("1994320302")
                                .setRating(6.0, 5.0, 7.0, 5)
                                .addImageCaptions("my_img_caption1", "my_img_caption_2")
                                .setQuantity(2.0)
                                .setPrice(23.2, CurrencyType.USD)
                                .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
                                .addCustomMetadata("Custom_Content_metadata_key1", "Custom_Content_metadata_val1")
                );


        findViewById(R.id.cmdIdentifyUser).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Branch.getInstance().setIdentity("test_user_10", new BranchReferralInitListener() {
                    @Override
                    public void onInitFinished(JSONObject referringParams, BranchError error) {
                        Log.e("BranchSDK_Tester", "install params = " + referringParams.toString());
                        if (error != null) {
                            Log.e("BranchSDK_Tester", "branch set Identity failed. Caused by -" + error.getMessage());
                        } else {
                            Log.e("BranchSDK_Tester", "install params = " + referringParams.toString());
                        }
                    }
                });
            }
        });

        findViewById(R.id.cmdClearUser).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Branch.getInstance().logout(new Branch.LogoutStatusListener() {
                    @Override
                    public void onLogoutFinished(boolean loggedOut, BranchError error) {
                        Log.e("BranchSDK_Tester", "onLogoutFinished " + loggedOut + " errorMessage " + error);
                    }
                });

                txtInstallCount.setText(R.string.install_count_empty);
            }
        });

        findViewById(R.id.cmdPrintInstallParam).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject obj = Branch.getInstance().getFirstReferringParams();
                Log.e("BranchSDK_Tester", "install params = " + obj.toString());
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

                // Sync link create example.  This makes a network call on the UI thread
                // txtShortUrl.setText(branchUniversalObject.getShortUrl(MainActivity.this, linkProperties));

                // Async Link creation example
                branchUniversalObject.generateShortUrl(MainActivity.this, linkProperties, new Branch.BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        if (error != null) {
                            txtShortUrl.setText(error.getMessage());
                        } else {
                            txtShortUrl.setText(url);
                        }
                    }
                });
            }
        });

        findViewById(R.id.cmdCommitBuyAction).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Branch.getInstance().userCompletedAction("buy", new BranchViewHandler.IBranchViewEvents() {
                    @Override
                    public void onBranchViewVisible(String action, String branchViewID) {
                        Log.e("BranchSDK_Tester", "onBranchViewVisible");
                    }

                    @Override
                    public void onBranchViewAccepted(String action, String branchViewID) {
                        Log.e("BranchSDK_Tester", "onBranchViewAccepted");
                    }

                    @Override
                    public void onBranchViewCancelled(String action, String branchViewID) {
                        Log.e("BranchSDK_Tester", "onBranchViewCancelled");
                    }

                    @Override
                    public void onBranchViewError(int errorCode, String errorMsg, String action) {
                        Log.e("BranchSDK_Tester", "onBranchViewError " + errorMsg);
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
                Branch.getInstance().userCompletedAction("buy", params);
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
                LinkProperties linkProperties = new LinkProperties()
                        .addTag("myShareTag1")
                        .addTag("myShareTag2")
//                        .setAlias("mylinkName") // In case you need to white label your link
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

                            /*
                             * Use {@link io.branch.referral.Branch.ExtendedBranchLinkShareListener} if the params need to be modified according to the channel selected by the user.
                             * This allows modification of content or link properties through callback {@link #onChannelSelected(String, BranchUniversalObject, LinkProperties)} }
                             */
//                            @Override
//                            public boolean onChannelSelected(String channelName, BranchUniversalObject buo, LinkProperties linkProperties) {
//                                linkProperties.setAlias("http://bnc.lt/alias_link");
//                                buo.setTitle("Custom Title for selected channel : " + channelName);
//                                return true;
//                            }

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

        findViewById(R.id.notif_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                String shortURL = branchUniversalObject.getShortUrl(MainActivity.this, new LinkProperties().addControlParameter("key11", "value11"));
                if (shortURL == null) {
                    Log.e("BranchSDK_Tester", "branchUniversalObject.getShortUrl = null");
                    return;
                }
//                intent.setData(Uri.parse(shortURL));
                intent.putExtra(Defines.IntentKeys.BranchURI.getKey(), shortURL);
                intent.putExtra(Defines.IntentKeys.ForceNewBranchSession.getKey(), true);
                PendingIntent pendingIntent =  PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, branchChannelID)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("BranchTest")
                        .setContentText(shortURL)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                notificationManager.notify(1, builder.build());
            }
        });

        findViewById(R.id.settings_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        // Tracking events
        findViewById(R.id.cmdTrackCustomEvent).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new BranchEvent("Logged_In")
                        .addCustomDataProperty("Custom_Event_Property_Key11", "Custom_Event_Property_val11")
                        .addCustomDataProperty("Custom_Event_Property_Key22", "Custom_Event_Property_val22")
                        .logEvent(MainActivity.this);
            }
        });

        findViewById(R.id.cmdTrackStandardEvent).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
                        .setAffiliation("test_affiliation")
                        .setCoupon("test_coupon")
                        .setCurrency(CurrencyType.USD)
                        .setDescription("Event _description")
                        .setShipping(10.2)
                        .setTax(12.3)
                        .setRevenue(1.5)
                        .setTransactionID("12344555")
                        .setSearchQuery("Test Search query")
                        .addCustomDataProperty("Custom_Event_Property_Key1", "Custom_Event_Property_val1")
                        .addCustomDataProperty("Custom_Event_Property_Key2", "Custom_Event_Property_val2")
                        .addContentItems(branchUniversalObject)
                        .logEvent(MainActivity.this);
            }
        });

        ((ToggleButton) findViewById(R.id.tracking_cntrl_btn)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Branch.getInstance().disableTracking(isChecked);
            }
        });

        findViewById(R.id.qrCode_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                BranchQRCode qrCode = new BranchQRCode()
                        .setCodeColor("#57dbe0")
                        .setBackgroundColor("#2a2e2e")
                        .setMargin(2)
                        .setWidth(512)
                        .setImageFormat(BranchQRCode.BranchImageFormat.PNG)
                        .setCenterLogo("https://cdn.branch.io/branch-assets/1598575682753-og_image.png");

                BranchUniversalObject buo = new BranchUniversalObject()
                        .setCanonicalIdentifier("content/12345")
                        .setTitle("My Content Title")
                        .setContentDescription("My Content Description")
                        .setContentImageUrl("https://lorempixel.com/400/400");

                LinkProperties lp = new LinkProperties()
                        .setChannel("facebook")
                        .setFeature("sharing")
                        .setCampaign("content 123 launch")
                        .setStage("new user");

                try {
                    qrCode.getQRCodeAsImage(MainActivity.this, buo, lp, new BranchQRCode.BranchQRCodeImageHandler() {
                        @Override
                        public void onSuccess(Bitmap qrCodeImage) {
                            try {
                                AlertDialog.Builder ImageDialog = new AlertDialog.Builder(MainActivity.this);
                                ImageDialog.setTitle("Your QR Code");
                                ImageView showImage = new ImageView(MainActivity.this);

                                showImage.setImageBitmap(qrCodeImage);
                                ImageDialog.setView(showImage);

                                ImageDialog.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                    }
                                });
                                ImageDialog.show();

                            } catch (Exception e) {
                                Log.d("Adding Image to Alert", "Failed");
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.d("Fail in main activity", String.valueOf(e));

                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(branchChannelID, "BranchChannel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Very interesting description");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) return;
            notificationManager.createNotificationChannel(channel);
        }
    }

    // https://www.baeldung.com/sha-256-hashing-java
    private static String getHashedValue(@NonNull String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(value.getBytes(Charset.forName("UTF-8"))));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Branch.getInstance().addFacebookPartnerParameterWithName("em", getHashedValue("sdkadmin@branch.io"));
        Branch.getInstance().addFacebookPartnerParameterWithName("ph", getHashedValue("6516006060"));
        Log.e("BranchSDK_Tester", "initSession");
        Branch.sessionBuilder(this).withCallback(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error != null) {
                    Log.e("BranchSDK_Tester", "branch init failed. Caused by -" + error.getMessage());
                } else {
                    Log.e("BranchSDK_Tester", "branch init complete!");
                    if (branchUniversalObject != null) {
                        Log.e("BranchSDK_Tester", "title " + branchUniversalObject.getTitle());
                        Log.e("BranchSDK_Tester", "CanonicalIdentifier " + branchUniversalObject.getCanonicalIdentifier());
                        Log.e("BranchSDK_Tester", "metadata " + branchUniversalObject.getContentMetadata().convertToJson());
                    }

                    if (linkProperties != null) {
                        Log.e("BranchSDK_Tester", "Channel " + linkProperties.getChannel());
                        Log.e("BranchSDK_Tester", "control params " + linkProperties.getControlParams());
                    }
                }


                // QA purpose only
                // TrackingControlTestRoutines.runTrackingControlTest(MainActivity.this);
                // BUOTestRoutines.TestBUOFunctionalities(MainActivity.this);

            }
        }).withData(this.getIntent().getData()).init();

        // Branch integration validation: Validate Branch integration with your app
        // NOTE : The below method will run few checks for verifying correctness of the Branch integration.
        // Please look for "BranchSDK_Doctor" in the logcat to see the results.
        // IMP : Do not make this call in your production app
        
        //IntegrationValidator.validate(MainActivity.this);

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
        Branch.sessionBuilder(this).withCallback(new BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error != null) {
                    Log.e("BranchSDK_Tester", error.getMessage());
                } else if (referringParams != null) {
                    Log.e("BranchSDK_Tester", referringParams.toString());
                }
            }
        }).reInit();

        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Checking if the previous activity is launched on branch Auto deep link.
        if (requestCode == getResources().getInteger(R.integer.AutoDeeplinkRequestCode)) {
            //Decide here where  to navigate  when an auto deep linked activity finishes.
            //For e.g. Go to HomeActivity or a  SignUp Activity.
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
        }
    }


}
