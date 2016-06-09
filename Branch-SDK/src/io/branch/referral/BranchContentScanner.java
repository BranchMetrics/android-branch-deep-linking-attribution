package io.branch.referral;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Path;
import android.nfc.Tag;
import android.os.Handler;
import android.text.AndroidCharacter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.client.RedirectException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Set;

/**
 * Created by sojanpr on 5/31/16.
 */
class BranchContentScanner {
    private static final String TAG = "BranchContentScanner";
    private static BranchContentScanner thisInstance_;
    private final String TRIGGER_URI_KEY = "trigger_uri";
    private final String CONTENT_PATH_KEY = "content_path";
    private String triggerUri_;
    private String contentPath_;

    static BranchContentScanner getInstance() {
        if (thisInstance_ == null) {
            thisInstance_ = new BranchContentScanner();
        }
        return thisInstance_;
    }

    private BranchContentScanner() {
    }

    void scanForContent(final Activity activity) {
        if (activity != null) {
            if (activity.getIntent() != null && activity.getIntent().getData() != null && activity.getIntent().getData().getScheme() != null) {
                triggerUri_ = activity.getIntent().getData().toString();
            }
//            String path = activity.getClass().getCanonicalName();
//            if(contentPath_ != null && contentPath_.contains(path)) {
//                contentPath_ = contentPath_.substring(contentPath_.indexOf(path));
//            }
//            else {
//                contentPath_ += path + "/";
//            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (activity.getIntent() != null && activity.getIntent().getCategories() != null
                            && activity.getIntent().getCategories().contains("android.intent.category.LAUNCHER")) {
                        Log.d(TAG, "Skipping Launcher Activity " + activity.getClass().getSimpleName());
                    } else {
                        try {
                            JSONObject scannedData = new JSONObject();
                            scannedData.put(TRIGGER_URI_KEY, triggerUri_);
                            scannedData.put(CONTENT_PATH_KEY, contentPath_);
                            ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                            //ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
                            scanThroughChildViews(rootView, scannedData, activity.getResources());
                            Log.d(TAG, "Scanned Result is " + scannedData);
                        } catch (Exception ignore) {
                        }
                    }
                }
            }, 500);
        }

    }

    private JSONObject scanThroughChildViews(ViewGroup view, JSONObject scannedData, Resources res) {
        ViewGroup viewGroup = ((ViewGroup) view);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView.getVisibility() == View.VISIBLE) {
                if (childView instanceof ViewGroup) {
                    JSONObject childScannedData = scanThroughChildViews((ViewGroup) childView, scannedData, res);
                } else {
                    try {
                        if (childView instanceof EditText) {
                            EditText editText = (EditText) childView;
                            String key = editText.getHint() != null ? editText.getHint().toString() : res.getResourceEntryName(childView.getId());
                            scannedData.put(key, editText.getText());
                        } else if (childView instanceof TextView) {
                            TextView txtView = (TextView) childView;
                            scannedData.put(res.getResourceEntryName(childView.getId()), txtView.getText());
                        } else if (childView instanceof ImageView && !(childView instanceof ImageButton)) {
                            childView.setDrawingCacheEnabled(true);
                            Bitmap bmp = Bitmap.createBitmap(childView.getDrawingCache());
                            if (bmp != null) {
                                scannedData.put(res.getResourceEntryName(childView.getId()), BitMapToString(bmp));

                                bmp.recycle();
                            }
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        return scannedData;
    }

    private String BitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String bmpStr = Base64.encodeToString(b, Base64.DEFAULT);
        return bmpStr;
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);

        return resizedBitmap;
    }
}
