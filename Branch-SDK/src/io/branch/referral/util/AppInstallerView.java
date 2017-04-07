package io.branch.referral.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.service.voice.VoiceInteractionService;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by sojanpr on 4/6/17.
 * <p>
 * A View class for handling conversion from instant apps to full app. This view will be visible only
 * in Instant apps and guides user to full app installation though playstore or custom url.
 * </p>
 */
public class AppInstallerView extends FrameLayout implements View.OnClickListener {
    private String customUrl;
    String packageName;

    public AppInstallerView(Context context) {
        super(context);
        init(context, null);
    }

    public AppInstallerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AppInstallerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] set = {
                    android.R.attr.text,
                    android.R.attr.textColor
            };
            TypedArray ta = context.obtainStyledAttributes(attrs, set);
            String buttonText = ta.getString(0);
            int textColor = ta.getInt(1, Color.WHITE);
            ta.recycle();

            if (!TextUtils.isEmpty(buttonText)) {
                TextView btnTextView = new TextView(context);
                btnTextView.setText(buttonText);
                btnTextView.setTextColor(textColor);
                this.setForegroundGravity(Gravity.CENTER);
                this.addView(btnTextView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btnTextView.setOnClickListener(this);
            }
        }

        setOnClickListener(this);
        setVisibility(VISIBLE);
        final PackageManager packageManager = context.getPackageManager();
        packageName = context.getPackageName();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (list != null && list.size() > 0) {
                setVisibility(GONE);
            }
        }

    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setCustomFullAppInstallUrl(String url) {
        customUrl = url;
    }

    public void setCustomView(View customView) {
        this.removeAllViews();
        this.addView(customView);
        customView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (customUrl != null) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(customUrl));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } else {
            try {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
            } catch (android.content.ActivityNotFoundException ignore) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
            }
        }
    }
}
