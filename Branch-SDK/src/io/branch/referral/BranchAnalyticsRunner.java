package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by sojanpr on 5/31/16.
 */
class BranchAnalyticsRunner {
    private static final String TAG = "BranchAnalyticsRunner";
    private static BranchAnalyticsRunner thisInstance_;
    private final String TRIGGER_URI_KEY = "trigger_uri";
    private final String CONTENT_PATH_KEY = "content_path";

    private final String EXTENDED_INTENT_URI_KEY = "external_intent_uri";
    private final String VIEW_METADATA_KEY = "view_metadata";

    private String triggerUri_;
    private String contentPath_;
    private Handler handler_;
    private WeakReference<Activity> lastActivityReference_;
    private final int VIEW_SETTLE_TIME = 1000; /* Time for a view to load its components */
    private final int APP_LAUNCH_DELAY = 1300; /* Time for an app to do initial pages like splash or logo */
    private String contentNavPath_; // User navigation path for the content.


    private Defines.ContentAnalyticMode content_analytic_mode_ = Defines.ContentAnalyticMode.Off;

    static BranchAnalyticsRunner getInstance() {
        if (thisInstance_ == null) {
            thisInstance_ = new BranchAnalyticsRunner();
        }
        return thisInstance_;
    }

    private BranchAnalyticsRunner() {
        handler_ = new Handler();

    }

    //------------------------- Public methods---------------------------------//
    public void onBranchInitialised(Defines.ContentAnalyticMode mode) {
        content_analytic_mode_ = mode;
    }

    public void onBranchClosing(Context context) {
        content_analytic_mode_ = Defines.ContentAnalyticMode.Off;
        if (PrefHelper.getInstance(context).getBranchAnalyticsData().length() > 0) {
            ServerRequest request = new ServerRequestUpdateContentEvents(context);
            Branch.getInstance().handleNewRequest(request);
        }
    }

    public void scanForContent(final Activity activity, boolean isSessionStart) {
        int viewRenderWait = VIEW_SETTLE_TIME;
        if (isSessionStart) {
            triggerUri_ = null;
        }

        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null && intent.getData() != null && intent.getData().getScheme() != null) {
                triggerUri_ = intent.getData().toString();
                // Add app launch delay if launching on init
                if (intent.getCategories() != null && intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                    viewRenderWait = viewRenderWait + APP_LAUNCH_DELAY;
                }
            }
        }
        //Scan for content only if the app is started by  a link click or if the content path for this view is already cached
        if (triggerUri_ != null || BranchAnalyticsPathCache.getInstance(activity).isContentPathAvailable(activity)) {
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = new WeakReference<>(activity);
            contentPath_ = getContentPath(activity);
            handler_.postDelayed(readContentRunnable, viewRenderWait);
        }
    }

    public void onActivityStarted(Activity activity, boolean isSessionStart) {
        scanForContent(activity, isSessionStart);
    }

    public void onActivityStopped(Activity activity) {
        if (lastActivityReference_ != null && lastActivityReference_.get() != null
                && lastActivityReference_.get().getClass().getName().equals(activity.getClass().getName())) {
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = null;
        }
    }

    // ---------------Private methods----------------------//


    private Runnable readContentRunnable = new Runnable() {
        @Override
        public void run() {
            if (content_analytic_mode_ != Defines.ContentAnalyticMode.Off) {
                try {
                    if (lastActivityReference_ != null && lastActivityReference_.get() != null) {
                        Activity activity = lastActivityReference_.get();
                        JSONObject contentEvent = new JSONObject();
                        contentEvent.put(Defines.Jsonkey.ContentAnalyticsMode.getKey(), content_analytic_mode_.toString());
                        contentEvent.put(Defines.Jsonkey.BranchViewAction.getKey(), Defines.Jsonkey.ContentActionView.toString());
                        contentEvent.put(Defines.Jsonkey.ContentPath.getKey(), contentPath_);
                        contentEvent.put(Defines.Jsonkey.ContentNavPath.getKey(), contentNavPath_);
                        contentEvent.put(Defines.Jsonkey.ReferralLink.getKey(), triggerUri_);

                        JSONArray viewMetaDataJson = new JSONArray();
                        contentEvent.put(Defines.Jsonkey.ContentData.getKey(), viewMetaDataJson);

                        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                        readThroughChildViews(rootView, viewMetaDataJson, activity.getResources());
                        BranchAnalyticsPathCache.getInstance(activity).addContentPath(contentPath_);

                        // Cache the analytics data for future use
                        PrefHelper.getInstance(activity).saveBranchAnalyticsData(contentEvent);
                        lastActivityReference_ = null;
                        triggerUri_ = null;
                        Log.d(TAG, "Scanned Result is " + contentEvent);
                    }

                } catch (JSONException ignore) {
                }
            }
        }
    };

    private void readThroughChildViews(ViewGroup view, JSONArray contentDataArray, Resources res) {
        ViewGroup viewGroup = ((ViewGroup) view);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView.getVisibility() == View.VISIBLE) {
                if (childView instanceof ViewGroup) {
                    readThroughChildViews((ViewGroup) childView, contentDataArray, res);
                } else {
                    String viewId = res.getResourceEntryName(childView.getId());
                    String viewVal = null;
                    try {
                        if (childView instanceof EditText) {
                            EditText editText = (EditText) childView;
                            viewVal = editText.getText().toString();
                        } else if (childView instanceof TextView) {
                            TextView txtView = (TextView) childView;
                            viewVal = txtView.getText().toString();
                        } else if (content_analytic_mode_ != Defines.ContentAnalyticMode.Deep) {
                            if (childView instanceof ImageView && !(childView instanceof ImageButton)) {
                                childView.setDrawingCacheEnabled(true);
                                Bitmap bmp = Bitmap.createBitmap(childView.getDrawingCache());
                                if (bmp != null) {
                                    viewVal = BitMapToString(bmp);
                                    bmp.recycle();
                                }
                            }
                        }

                        JSONObject contentData = new JSONObject();
                        contentData.put(viewId, viewVal);
                        contentData.put(Defines.Jsonkey.Type.getKey(), childView.getClass().getSimpleName());
                        contentDataArray.put(contentData);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

    }

    private String BitMapToString(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String bmpStr = Base64.encodeToString(b, Base64.DEFAULT);
        return bmpStr;
    }


    private String getContentPath(Activity activity) {
        String path = "android-app://" + activity.getPackageName() + "/" + activity.getClass().getSimpleName();
        return path;
    }

    private String getContentUrl(Activity activity) {
        String contentUrl = getContentPath(activity);
        if (activity.getIntent() != null) {
            String paramPrepender = "?";
            Bundle bundle = activity.getIntent().getExtras();
            if (bundle != null) {
                Set<String> keySet = bundle.keySet();
                for (String key : keySet) {
                    contentUrl = contentUrl + paramPrepender + key + "=" + bundle.get(key);
                    paramPrepender = "&";
                }
            }
            if (activity.getIntent().getData() != null) {
                contentUrl = contentUrl + paramPrepender + EXTENDED_INTENT_URI_KEY + "=" + activity.getIntent().getData();
            }
        }
        return contentUrl;
    }

}
