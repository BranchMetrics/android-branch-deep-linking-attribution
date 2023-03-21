package io.branch.saas.sdk.testbed.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.saas.sdk.testbed.Common;
import io.branch.saas.sdk.testbed.Constants;
import io.branch.saas.sdk.testbed.R;
import io.branch.saas.sdk.testbed.views.AdvancedWebView;

public class ReadDeepLinkActivity extends AppCompatActivity implements AdvancedWebView.Listener {

    private AdvancedWebView mWebView;
    private boolean isFromReadDeepLink, isFromNotification, isDataLoaded;
    private Uri uri;
    private String redirectedUrl, deepLinkUrl, clickType;
    private TextView tvRedirectedLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_deeplink);
//        Uri uri = getIntent().getData();
        String url = getIntent().getStringExtra(Constants.ANDROID_URL);
        clickType = getIntent().getStringExtra(Constants.TYPE);
        Log.e("data-->", "here----" + clickType);
        isFromNotification = getIntent().getBooleanExtra(Constants.FORCE_NEW_SESSION, false);
        if (!TextUtils.isEmpty(url)) {
            uri = Uri.parse(url);
            isFromReadDeepLink = false;
        } else {
            uri = getIntent().getData();
            isFromReadDeepLink = true;
        }
        deepLinkUrl = uri.toString();
        TextView textView = findViewById(R.id.tv_read_link);
        tvRedirectedLink = findViewById(R.id.tv_redirected_link);
        Button btSubmit = findViewById(R.id.bt_submit);
        mWebView = findViewById(R.id.webview);
        mWebView.setAnimationCacheEnabled(false);

        if (!TextUtils.isEmpty(clickType) && clickType.equals(Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK)) {
            btSubmit.setVisibility(View.GONE);
        } else
            btSubmit.setVisibility(View.VISIBLE);
        if (uri != null) {
            textView.setText(uri.toString());
            mWebView.setListener(this, this);
            Log.e("url-->", "-->" + uri.toString());
            mWebView.loadUrl(uri.toString());

        }
        btSubmit.setOnClickListener(view -> {
            Intent intent = new Intent(ReadDeepLinkActivity.this, LogDataActivity.class);
            if (isFromReadDeepLink)
                intent.putExtra(Constants.TYPE, Constants.AFTER_READ_DEEP_LINK);
            else
                intent.putExtra(Constants.TYPE, Constants.LOG_DATA);
            if (isDataLoaded) {
                intent.putExtra(Constants.STATUS, Constants.SUCCESS);
            } else {
                intent.putExtra(Constants.STATUS, Constants.FAIL);
            }
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!TextUtils.isEmpty(clickType) && clickType.equals(Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK)) {
            return;
        }
        Common.getInstance().clearLog();
        Branch.InitSessionBuilder initSessionBuilder = Branch.sessionBuilder(this).withCallback(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    isDataLoaded = true;
                    Log.i("BRANCH SDK", referringParams.toString());
                } else {
                    Log.i("BRANCH SDK error", error.getMessage());
                }
            }
        }).withData(uri);//.init();
        if (isFromNotification)
            initSessionBuilder.reInit();
        else
            initSessionBuilder.init();

        if (isFromReadDeepLink) {
            isDataLoaded = true;
            // latest
            JSONObject sessionParams = Branch.getInstance().getLatestReferringParams();
            Log.i("BRANCH SDK session", "sessionParams-->" + sessionParams);

            // first
            JSONObject installParams = Branch.getInstance().getFirstReferringParams();
            Log.i("BRANCH SDK install", "installParams-->" + installParams);
        }
    }


    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
        // ...
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        mWebView.onPause();
        // ...
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mWebView.onDestroy();
        // ...
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);
        // ...
    }

    @Override
    public void onBackPressed() {
        if (!mWebView.onBackPressed()) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        Log.e("onPageStarted", "-->" + url);
        if (!url.equals(deepLinkUrl) && TextUtils.isEmpty(redirectedUrl)) {
            tvRedirectedLink.setText(url);
            tvRedirectedLink.setVisibility(View.VISIBLE);
            this.redirectedUrl = url;
        }
    }

    @Override
    public void onPageFinished(String url) {
        Log.e("onPageFinished", "-->" + url);
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        Log.e("onPageError", "-->" + failingUrl);
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {

    }

    @Override
    public void onExternalPageRequest(String url) {
        Log.e("onExternalPageRequest", "-->" + url);
    }
}