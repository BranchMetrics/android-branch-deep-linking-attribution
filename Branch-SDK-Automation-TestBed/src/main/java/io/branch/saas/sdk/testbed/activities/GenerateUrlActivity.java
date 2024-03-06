package io.branch.saas.sdk.testbed.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.util.Calendar;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.QRCode.BranchQRCode;
import io.branch.referral.SharingHelper;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;
import io.branch.saas.sdk.testbed.Common;
import io.branch.saas.sdk.testbed.Constants;
import io.branch.saas.sdk.testbed.R;

public class GenerateUrlActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText etChannel, etFeature, etChampaign, etStage,
            etDesktopUrl, etAdditionalData, etAndroidUrl, etIosUrl, etQrCodeLogoUrl;
    private String clickType;
    private final static String branchChannelID = "BranchChannelID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_url);
        getSupportActionBar().setTitle("Generate URL");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        clickType = getIntent().getStringExtra(Constants.TYPE);
        createNotificationChannel();

        etChannel = findViewById(R.id.et_channel);
        etChampaign = findViewById(R.id.et_campaign);
        etFeature = findViewById(R.id.et_feature);
        etStage = findViewById(R.id.et_stage);
        etDesktopUrl = findViewById(R.id.et_desktop_url);
        etAdditionalData = findViewById(R.id.et_additional_data);
        etAndroidUrl = findViewById(R.id.et_android_url);
        etIosUrl = findViewById(R.id.et_ios_url);
        if (clickType.equals(Constants.CREATE_QR_CODE)) {
            findViewById(R.id.til_qr_code_logo).setVisibility(View.VISIBLE);
            etQrCodeLogoUrl = findViewById(R.id.et_qr_code_logo_url);
        }

        Button btSubmit = findViewById(R.id.bt_submit);
        btSubmit.setOnClickListener(this);
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Common.getInstance().clearLog();
        String channel = etChannel.getText().toString();
        String feature = etFeature.getText().toString();
        String champaign = etChampaign.getText().toString();
        String stage = etStage.getText().toString();
        String desktopUrl = etDesktopUrl.getText().toString();
        String androidUrl = etAndroidUrl.getText().toString();
        String additionalData = etAdditionalData.getText().toString();
        String iosUrl = etIosUrl.getText().toString();
        Common.lp = new LinkProperties()
                .setChannel(TextUtils.isEmpty(channel) ? null : channel)
                .setFeature(TextUtils.isEmpty(feature) ? null : feature)
                .setCampaign(TextUtils.isEmpty(champaign) ? null : champaign)
                .setStage(TextUtils.isEmpty(stage) ? null : stage)
                .addControlParameter("$desktop_url", TextUtils.isEmpty(desktopUrl) ? null : desktopUrl)
                .addControlParameter("$android_url", TextUtils.isEmpty(androidUrl) ? null : androidUrl)
                .addControlParameter("custom", TextUtils.isEmpty(additionalData) ? null : additionalData)
                .addControlParameter("$ios_url", TextUtils.isEmpty(iosUrl) ? null : iosUrl)
                .addControlParameter("custom_random", Long.toString(Calendar.getInstance().getTimeInMillis()));
        if (clickType.equals(Constants.CREATE_SHARE_LINK)) {
            ShareSheetStyle shareSheetStyle = new ShareSheetStyle(this, "Check this out!", "This stuff is awesome: ")
                    .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), "Copy", "Added to clipboard")
                    .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK_MESSENGER)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.TWITTER)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.FLICKR)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.GOOGLE_DOC)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.WHATS_APP)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.PINTEREST)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.HANGOUT)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.INSTAGRAM)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.WECHAT)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.SNAPCHAT)
                    .addPreferredSharingOption(SharingHelper.SHARE_WITH.GMAIL)
                    .setAsFullWidthStyle(true)
                    .setSharingTitle("Share With");
            Common.branchUniversalObject.showShareSheet(this,
                    Common.lp,
                    shareSheetStyle,
                    new Branch.ExtendedBranchLinkShareListener() {
                        @Override
                        public void onShareLinkDialogLaunched() {
                        }

                        @Override
                        public void onShareLinkDialogDismissed() {
                        }

                        @Override
                        public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
                            Log.e("BRANCH SDK", "link---> " + sharedLink);
                        }

                        @Override
                        public void onChannelSelected(String channelName) {
                            Log.e("BRANCH SDK", "channelName-->" + channelName);
                        }

                        @Override
                        public boolean onChannelSelected(String channelName, BranchUniversalObject buo, LinkProperties linkProperties) {
                            return false;
                        }
                    });
        } else if (clickType.equals(Constants.CREATE_NATIVE_SHARE_LINK)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Branch.getInstance().share(this, Common.branchUniversalObject, Common.lp, new Branch.BranchNativeLinkShareListener() {
                            @Override
                            public void onLinkShareResponse(String sharedLink, BranchError error) {
                                Log.d("Native Share Sheet:", "Link Shared: " + sharedLink);
                            }

                            @Override
                            public void onChannelSelected(String channelName) {
                                Log.d("Native Share Sheet:", "Channel Selected: " + channelName);
                            }

                        },
                        "Sharing Branch Short URL", "Using Native Chooser Dialog");
            }

        } else if (clickType.equals(Constants.CREATE_QR_CODE)) {
            Log.e("qr code","create qr code");
            String qrCodeCenterLogoUrlStr = etQrCodeLogoUrl.getText().toString();
            BranchQRCode qrCode = new BranchQRCode() //All QR code settings are optional
                    .setCodeColor("#a4c639")
                    .setBackgroundColor(Color.WHITE)
                    .setMargin(1)
                    .setWidth(512)
                    .setImageFormat(BranchQRCode.BranchImageFormat.PNG)
                    .setCenterLogo(TextUtils.isEmpty(qrCodeCenterLogoUrlStr) ? null : qrCodeCenterLogoUrlStr);
            try {
                qrCode.getQRCodeAsImage(GenerateUrlActivity.this, Common.branchUniversalObject, Common.lp, new BranchQRCode.BranchQRCodeImageHandler() {
                    @Override
                    public void onSuccess(Bitmap qrCodeImage) {

                        //Do something with the QR code here.
                        Log.e("qr code","generated");
                        Common.qrCodeImage = qrCodeImage;
                        Intent intent = new Intent(GenerateUrlActivity.this, ShowQRCodeActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.d("Failed to get QR code", String.valueOf(e));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Common.branchUniversalObject.generateShortUrl(this, Common.lp, (url, error) -> {
                Intent intent = new Intent(GenerateUrlActivity.this, LogDataActivity.class);
                if (error == null) {
                    intent.putExtra(Constants.STATUS, Constants.SUCCESS);
                    intent.putExtra(Constants.TYPE, clickType);
                    intent.putExtra(Constants.ANDROID_URL, url);
                    if (clickType.equals(Constants.CREATE_SEND_NOTIFICATION)) {
                        intent.putExtra(Constants.MESSAGE, "Url is generated.\n Notification sent successfully");
                        showNotification(url);
                    } else if (clickType.equals(Constants.HANDLE_LINKS)) {
                        intent.putExtra(Constants.MESSAGE, "Url is generated.\n Here is the Short URL:" + url);
                        intent.putExtra(Constants.FORCE_NEW_SESSION, true);
                    } else {
                        intent.putExtra(Constants.MESSAGE, "Url is generated.\n Here is the Short URL:" + url);
                    }
                } else {
                    intent.putExtra(Constants.STATUS, Constants.FAIL);
                    intent.putExtra(Constants.MESSAGE, "Unable to generate short url:" + error.getMessage());
                }
                startActivity(intent);
                finish();
            });
        }
    }

    private void showNotification(String url) {
        Intent intent = new Intent(GenerateUrlActivity.this, ReadDeepLinkActivity.class);

//        intent.setData(Uri.parse(url));
        intent.putExtra(Constants.ANDROID_URL, url);
        intent.putExtra(Constants.FORCE_NEW_SESSION, true);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(GenerateUrlActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(GenerateUrlActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(GenerateUrlActivity.this, branchChannelID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("BranchTest")
                .setContentText(url)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(GenerateUrlActivity.this);
        notificationManager.notify(1, builder.build());
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
}