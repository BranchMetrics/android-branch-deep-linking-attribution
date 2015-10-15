package io.branch.referral.indexing;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchShortLinkBuilder;
import io.branch.referral.Defines;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

/**
 * Created by sojanpr on 10/8/15.
 * <p/>
 * <p>Class for representing a piece of content in your application to work with Branch provided services such as
 * 1) Content Deep linking
 * 2) Content Analytics
 * 3) Content indexing
 * 4) Content Sharing
 * etc.
 * BranchUniversalObject provides convenient methods to work on your content with above Branch services  </p>
 */
public class BranchUniversalObject {
    /* Canonical identifier for the content referred. Normally the canonical path for your content in the app or web */
    private final String canonicalIdentifier_;
    /* Title for the BranchUniversalObject that identify your content referred */
    private final String title_;
    /* Description for the content referred */
    private String description_;
    /* An image url associated with the content referred */
    private String imageUrl_;
    /* Meta data provided for the content */
    private final HashMap<String, String> metaData_;
    /* Mime type for the content referred */
    private String type_;
    /* Content index mode */
    private CONTENT_INDEX_MODE indexMode_;
    /* Any keyword associated with the content. Used for indexing */
    private final ArrayList<String> keyWords_;
    /* Expiry date for the content and any associated links. Represented as epoch milli second */
    private long expiration_;

    /**
     * Defines the Content indexing modes
     * PUBLIC | PRIVATE
     */
    public enum CONTENT_INDEX_MODE {
        PUBLIC, /* Referred contents are publically indexable */
        // PUBLIC_IN_APP, /* Referred contents are publically available to the any app user */
        PRIVATE/* Referred contents are not publically indexable */
    }


    /**
     * <p>
     * Create a BranchUniversalObject with the given content.
     * </p>
     *
     * @param canonicalIdentifier A {@link String} with value of canonical identifier for the content referred by this object
     * @param title               A {@link String} with value for title of content referred by this object
     */
    public BranchUniversalObject(String canonicalIdentifier, String title) {
        canonicalIdentifier_ = canonicalIdentifier;
        title_ = title;
        metaData_ = new HashMap<>();
        keyWords_ = new ArrayList<>();
        description_ = "";
        type_ = "";
        indexMode_ = CONTENT_INDEX_MODE.PUBLIC; // Default content indexing mode is public
        expiration_ = 0L;
    }

    /**
     * <p/>
     * Set description for the content for the content referred by this object
     * <p/>
     *
     * @param description A {@link String} with value for the description of the content referred by this object
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentDescription(String description) {
        this.description_ = description;
        return this;
    }

    /**
     * <p/>
     * Set the url to any image associated with this content.
     * <p/>
     *
     * @param imageUrl A {@link String} specifying a url to an image associated with content referred by this object
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentImageUrl(String imageUrl) {
        this.imageUrl_ = imageUrl;
        return this;
    }

    /**
     * <p>
     * Adds the given {@link java.util.Map} to the meta data
     * </p>
     *
     * @param metaData A {@link HashMap} with {@link String} key value pairs
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject addContentMetaData(HashMap<String, String> metaData) {
        this.metaData_.putAll(metaData);
        return this;
    }

    /**
     * <p>
     * Adds the given {@link java.util.Map} to the meta data
     * </p>
     *
     * @param key   A {@link String} value for metadata key
     * @param value A {@link String} value for metadata value
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject addContentMetaData(String key, String value) {
        this.metaData_.put(key, value);
        return this;
    }

    /**
     * <p>
     * Sets the content type for this Object
     * </p>
     *
     * @param type {@link String} with value for the mime type of the content referred
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentType(String type) {
        this.type_ = type;
        return this;
    }

    /**
     * <p>
     * Set the indexing mode for the content referred in this object
     * </p>
     *
     * @param indexMode {@link io.branch.referral.indexing.BranchUniversalObject.CONTENT_INDEX_MODE} value for the content referred
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentIndexingMode(CONTENT_INDEX_MODE indexMode) {
        this.indexMode_ = indexMode;
        return this;
    }

    /**
     * <p>
     * Adds any keywords associated with the content referred
     * </p>
     *
     * @param keyWords An {@link ArrayList} of {@link String} values
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject addKeyWords(ArrayList<String> keyWords) {
        this.keyWords_.addAll(keyWords);
        return this;
    }

    /**
     * <p>
     * Add a keyword associated with the content referred
     * </p>
     *
     * @param keyWord A{@link String} with value for keyword
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject addKeyWord(String keyWord) {
        this.keyWords_.add(keyWord);
        return this;
    }

    /**
     * <p>
     * Set the content expiration time.
     * </p>
     *
     * @param expirationDate A {@link Date} value representing the expiration date.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentExpiration(Date expirationDate) {
        this.expiration_ = expirationDate.getTime();
        return this;
    }


    public ArrayList<String> getKeyWords() {
        return keyWords_;
    }

    public boolean isPublicallyIndexable() {
        return indexMode_ == CONTENT_INDEX_MODE.PUBLIC;
    }

    public HashMap<String, String> getMetaData() {
        return metaData_;
    }

    public long getExpirationTime() {
        return expiration_;
    }

    public String getCanonicalIdentifier() {
        return canonicalIdentifier_;
    }

    public String getDescription() {
        return description_;
    }

    public String getImageUrl() {
        return imageUrl_;
    }

    public String getTitle() {
        return title_;
    }

    public String getType() {
        return type_;
    }

    public JSONArray getKeywordsJsonArray(){
        JSONArray keywordArray = new JSONArray();
        for(String keyword : keyWords_){
            keywordArray.put(keyword);
        }
        return keywordArray;
    }

    //-------------------- Register views--------------------------//

    /**
     * Mark the content referred by this object as viewed. This increment the view count of the contents referred by this object.
     */
    public void markAsViewed() {
        markAsViewed(null);
    }

    /**
     * Mark the content referred by this object as viewed. This increment the view count of the contents referred by this object.
     *
     * @param callback An instance of {@link MarkViewStatusListener} to listen to results of the operation
     */
    public void markAsViewed(MarkViewStatusListener callback) {
        if (Branch.getInstance() != null) {
            Branch.getInstance().markContentViewed(this, callback);
        } else {
            if (callback != null) {
                callback.onMarkViewFinished(false, new BranchError("Register view error", BranchError.ERR_BRANCH_NOT_INSTANTIATED));
            }
        }
    }


    /**
     * <p>
     * Callback interface for listening register content view status
     * </p>
     */
    public interface MarkViewStatusListener {
        /**
         * Called on finishing the the register view process
         *
         * @param registered A {@link Boolean} which is set to true if register content view succeeded
         * @param error      An instance of {@link BranchError} to notify any error occurred during registering a content view event.
         *                   A null value is set if the registering content view succeeds
         */
        void onMarkViewFinished(boolean registered, BranchError error);
    }


    //--------------------- Create Link --------------------------//


    public String getShortUrl(Context context, LinkProperties linkProperties) {
        return getLinkBuilder(context, linkProperties).getShortUrl();
    }

    public void generateShortUrl(Context context, LinkProperties linkProperties, Branch.BranchLinkCreateListener callback) {
        getLinkBuilder(context, linkProperties).generateShortUrl(callback);
    }

    //------------------ Share sheet -------------------------------------//

    public void showShareSheet(Activity activity, LinkProperties linkProperties, ShareSheetStyle style, Branch.BranchLinkShareListener callback) {
        JSONObject params = new JSONObject();
        try {
            for (String key : metaData_.keySet()) {
                params.put(key, metaData_.get(key));
            }
            HashMap<String, String> controlParams = linkProperties.getControlParams();
            for (String key : controlParams.keySet()) {
                params.put(key, controlParams.get(key));
            }
        } catch (JSONException ignore) {
        }
        Branch.ShareLinkBuilder shareLinkBuilder = new Branch.ShareLinkBuilder(activity, getLinkBuilder(activity, linkProperties))
                .setCallback(callback)
                .setSubject(style.getMessageTitle())
                .setMessage(style.getMessageBody());

        if (style.getCopyUrlIcon() != null) {
            shareLinkBuilder.setCopyUrlStyle(style.getCopyUrlIcon(), style.getCopyURlText(), style.getUrlCopiedMessage());
        }
        if (style.getMoreOptionIcon() != null) {
            shareLinkBuilder.setMoreOptionStyle(style.getMoreOptionIcon(), style.getMoreOptionText());
        }
        if (style.getDefaultURL() != null) {
            shareLinkBuilder.setDefaultURL(style.getDefaultURL());
        }
        if (style.getPreferredOptions().size() > 0) {
            shareLinkBuilder.addPreferredSharingOptions(style.getPreferredOptions());
        }
        shareLinkBuilder.shareLink();
    }


    private BranchShortLinkBuilder getLinkBuilder(Context context, LinkProperties linkProperties) {
        BranchShortLinkBuilder shortLinkBuilder = new BranchShortLinkBuilder(context);
        if (linkProperties.getTags() != null) {
            shortLinkBuilder.addTags(linkProperties.getTags());
        }
        if (linkProperties.getFeature() != null) {
            shortLinkBuilder.setFeature(linkProperties.getFeature());
        }
        if (linkProperties.getAlias() != null) {
            shortLinkBuilder.setAlias(linkProperties.getAlias());
        }
        if (linkProperties.getChannel() != null) {
            shortLinkBuilder.setChannel(linkProperties.getChannel());
        }
        if (linkProperties.getStage() != null) {
            shortLinkBuilder.setStage(linkProperties.getStage());
        }
        if (linkProperties.getMatchDuration() > 0) {
            shortLinkBuilder.setDuration(linkProperties.getMatchDuration());
        }

        shortLinkBuilder.addParameters(Defines.Jsonkey.ContentTitle.getKey(), title_);
        shortLinkBuilder.addParameters(Defines.Jsonkey.CanonicalIdentifier.getKey(), canonicalIdentifier_);
        shortLinkBuilder.addParameters(Defines.Jsonkey.ContentKeyWords.getKey(), getKeywordsJsonArray());
        shortLinkBuilder.addParameters(Defines.Jsonkey.ContentDesc.getKey(), description_);
        shortLinkBuilder.addParameters(Defines.Jsonkey.ContentImgUrl.getKey(), imageUrl_);
        shortLinkBuilder.addParameters(Defines.Jsonkey.ContentType.getKey(), type_);
        shortLinkBuilder.addParameters(Defines.Jsonkey.ContentExpiryTime.getKey(), "" + expiration_);

        HashMap<String, String> controlParam = linkProperties.getControlParams();
        for (String key : metaData_.keySet()) {
            shortLinkBuilder.addParameters(key, metaData_.get(key));
        }
        for (String key : controlParam.keySet()) {
            shortLinkBuilder.addParameters(key, controlParam.get(key));
        }


        return shortLinkBuilder;
    }

}
