package io.branch.referral;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by sojanpr on 2/25/16.
 */
public class PromoViewActivity extends Activity {
    private WebView promoWebView_;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        promoWebView_ = new WebView(this);
        setContentView(promoWebView_);
        promoWebView_.loadUrl("https://branch.io");
    }
}
