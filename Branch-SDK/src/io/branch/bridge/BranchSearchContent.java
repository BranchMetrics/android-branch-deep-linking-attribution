package io.branch.bridge;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.util.CurrencyType;


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

    private String packageName_ = "";
    private int resultPriority_ = RESULT_PRIORITY_CONTENT_LOCAL;

    public BranchSearchContent(BranchUniversalObject buo, String packageName, int resultPriority) {
        buo_ = buo;
        packageName_ = packageName;
        resultPriority_ = resultPriority;
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

    public int getResultPriority() {
        return resultPriority_;
    }

    public String getPackageName() {
        return packageName_;
    }

    public String getContentTitle() {
        String title = "";
        if (buo_ != null) {
            title = buo_.getTitle();
        }
        return title;
    }

    public String getContentDescription() {
        String desc = "";
        if (buo_ != null) {
            desc = buo_.getDescription();
        }
        return desc;
    }

    public String getImageUrl() {
        String imgUrl = "";
        if (buo_ != null) {
            imgUrl = buo_.getImageUrl();
        }
        return imgUrl;
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
        dest.writeString(packageName_);
        dest.writeInt(resultPriority_);
        dest.writeParcelable(buo_, 0);
    }

    private BranchSearchContent(Parcel in) {
        this();
        packageName_ = in.readString();
        resultPriority_ = in.readInt();
        buo_ = in.readParcelable(BranchSearchContent.class.getClassLoader());
    }
}

