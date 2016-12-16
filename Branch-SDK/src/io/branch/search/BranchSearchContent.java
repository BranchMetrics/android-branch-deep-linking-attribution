package io.branch.search;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Defines;
import io.branch.roots.Roots;


/**
 * Created by sojanpr on 11/14/16.
 * <p>
 * Class  for representing the search results returned by Branch Search Service
 * </p>
 */
public class BranchSearchContent implements Parcelable {

    public static final int RESULT_PRIORITY_CONTENT_LOCAL = 0;         /* Contents available in local apps and accessed by local user*/
    public static final int RESULT_PRIORITY_CONTENT_REMOTE = 1;        /* Contents available in local app but viewed by other users  */
    public static final int RESULT_PRIORITY_LOCAL_APP = 2;             /* Apps available locally that matches the contents */
    public static final int RESULT_PRIORITY_NON_LOCAL_CONTENTS = 3;    /* Contents residing in a non installed  app */
    public static final int RESULT_PRIORITY_NON_LOCAL_APPS = 4;        /* App that are not installed but matches the search */


    private static final String PACKAGE_NAME_KEY = "package_name_key";
    private static final String RESULT_PRIORITY_KEY = "result_prority";
    private static final String CONTENT_BUO_KEY = "content_buo_key";


    private BranchUniversalObject buo_;

    private String canonicalId_ = "";
    private String packageName_ = "";
    private String contentUrl_ = "";
    private String contentImageUrl_ = "";
    private String contentTitle_ = "";
    private String contentDescription_ = "";
    private int resultPriority_ = RESULT_PRIORITY_CONTENT_LOCAL;

    public BranchSearchContent(BranchUniversalObject buo, String packageName, int resultPriority, String contentUrl) {
        buo_ = buo;
        canonicalId_ = buo.getCanonicalIdentifier();
        contentTitle_ = buo.getTitle();
        contentDescription_ = buo.getDescription();
        contentImageUrl_ = buo.getImageUrl();
        packageName_ = packageName;
        resultPriority_ = resultPriority;
        contentUrl_ = contentUrl;
    }

    public BranchSearchContent(String canonicalId, String title, String desc, String imgUrl, String packageName, int resultPriority, String contentUrl) {
        canonicalId_ = canonicalId;
        contentTitle_ = title;
        contentDescription_ = desc;
        contentImageUrl_ = imgUrl;
        packageName_ = packageName;
        resultPriority_ = resultPriority;
        contentUrl_ = contentUrl;
    }

    private BranchSearchContent() {

    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PACKAGE_NAME_KEY, packageName_);
            jsonObject.put(RESULT_PRIORITY_KEY, resultPriority_);
            jsonObject.put(CONTENT_BUO_KEY, buo_.convertToJson().toString());
        } catch (JSONException ignore) {
        }
        return jsonObject;
    }


    public static BranchSearchContent createFromJson(JSONObject branchSearchContentJson) {
        BranchSearchContent branchSearchContent = new BranchSearchContent();
        try {

            if (branchSearchContentJson.has(CONTENT_BUO_KEY)) {
                JSONObject buoJson = new JSONObject(branchSearchContentJson.getString(CONTENT_BUO_KEY));
                branchSearchContent.buo_ = BranchUniversalObject.createInstance(buoJson);
            }

            if (branchSearchContentJson.has(PACKAGE_NAME_KEY)) {
                branchSearchContent.packageName_ = branchSearchContentJson.getString(PACKAGE_NAME_KEY);
            }

            if (branchSearchContentJson.has(RESULT_PRIORITY_KEY)) {
                branchSearchContent.resultPriority_ = branchSearchContentJson.getInt(RESULT_PRIORITY_KEY);
            }
        } catch (JSONException ignore) {

        }
        return branchSearchContent;
    }

    public String getCanonicalId() {
        return canonicalId_;
    }

    public int getResultPriority() {
        return resultPriority_;
    }

    public String getPackageName() {
        return packageName_;
    }

    public String getContentTitle() {
        return contentTitle_;
    }

    public String getContentDescription() {
        return contentDescription_;
    }

    public String getImageUrl() {
        return contentImageUrl_;
    }

    public String getContentUrl() {
        return contentUrl_;
    }

    public boolean redirectToContent(Activity context) {
        boolean isRedirected = false;
        // if (buo_ != null) {
        new Roots(context, contentUrl_).connect();
        isRedirected = true;
        // }

        return isRedirected;
    }

    public boolean redirectToContentThroughPush(Activity context) {
        boolean isRedirected = false;
        //if (buo_ != null) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName_);
            if (i != null) {
                i.putExtra(Defines.Jsonkey.AndroidPushNotificationKey.getKey(), contentUrl_);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                context.startActivity(i);
                isRedirected = true;
            }
        } catch (Exception ignore) {

        }
        //}

        return isRedirected;
    }


    //---------------------Marshaling and Unmarshaling----------//
    @Override
    public int describeContents() {
        return 0;
    }


    public static final Parcelable.Creator<BranchSearchContent> CREATOR = new Parcelable.Creator<BranchSearchContent>() {
        public BranchSearchContent createFromParcel(Parcel in) {
            return new BranchSearchContent(in);
        }

        public BranchSearchContent[] newArray(int size) {
            return new BranchSearchContent[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(canonicalId_);
        dest.writeString(packageName_);
        dest.writeString(contentTitle_);
        dest.writeString(contentDescription_);
        dest.writeString(contentImageUrl_);
        dest.writeInt(resultPriority_);
        dest.writeParcelable(buo_, 0);
    }

    private BranchSearchContent(Parcel in) {
        this();
        canonicalId_ = in.readString();
        packageName_ = in.readString();
        contentTitle_ = in.readString();
        contentDescription_ = in.readString();
        contentImageUrl_ = in.readString();
        resultPriority_ = in.readInt();
        buo_ = in.readParcelable(BranchSearchContent.class.getClassLoader());
    }


}

