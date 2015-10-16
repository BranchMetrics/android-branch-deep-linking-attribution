package io.branch.referral.indexing;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchShortLinkBuilder;
import io.branch.referral.Defines;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

/**
 * <p>Class represents a single piece of content within your app, as well as any associated metadata.
 * It provides convenient methods for sharing, deep linking, and tracking how often that content is viewed. This information is then used to provide you with powerful content analytics
 * and deep linking.BranchUniversalObject provides convenient methods to work on your content with Branch services
 * </p>
 */
public class BranchUniversalObject {
    /* Canonical identifier for the content referred. Normally the canonical path for your content in the app or web */
    private String canonicalIdentifier_;
    /* Title for the content referred by BranchUniversalObject */
    private String title_;
    /* Description for the content referred */
    private String description_;
    /* An image url associated with the content referred */
    private String imageUrl_;
    /* Meta data provided for the content. This meta data is used as the link parameters for links created from this object */
    private final HashMap<String, String> metadata_;
    /* Mime type for the content referred */
    private String type_;
    /* Content index mode */
    private CONTENT_INDEX_MODE indexMode_;
    /* Any keyword associated with the content. Used for indexing */
    private final ArrayList<String> keyWords_;
    /* Expiry date for the content and any associated links. Represented as epoch milli second */
    private long expiration_;

    private LinkProperties linkProperties_;

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
     */
    public BranchUniversalObject() {
        metadata_ = new HashMap<>();
        keyWords_ = new ArrayList<>();
        canonicalIdentifier_ = "";
        title_ = "";
        description_ = "";
        type_ = "";
        indexMode_ = CONTENT_INDEX_MODE.PUBLIC; // Default content indexing mode is public
        expiration_ = 0L;
        linkProperties_ = new LinkProperties();
    }

    /**
     * <p>
     * Set the canonical identifier for this BranchUniversalObject. Canonical identifier is normally the canonical path for your content in the application or web
     * </p>
     *
     * @param canonicalIdentifier A {@link String} with value for the canonical identifier
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setCanonicalIdentifier(String canonicalIdentifier) {
        this.canonicalIdentifier_ = canonicalIdentifier;
        return this;
    }

    /**
     * <p>
     * Set a title for the content referred by this object
     * </p>
     *
     * @param title A {@link String} with value of for the content title
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setTitle(String title) {
        this.title_ = title;
        return this;
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
     * Adds the the given set of key value pairs to the metadata associated with this content. These key values are passed to another user on deep linking.
     * </p>
     *
     * @param metadata A {@link HashMap} with {@link String} key value pairs
     * @return This instance to allow for chaining of calls to set methods
     */
    @SuppressWarnings("unused")
    public BranchUniversalObject addContentMetadata(HashMap<String, String> metadata) {
        this.metadata_.putAll(metadata);
        return this;
    }

    /**
     * <p>
     * Adds the the given set of key value pair to the metadata associated with this content. These key value is passed to another user on deep linking.
     * </p>
     *
     * @param key   A {@link String} value for metadata key
     * @param value A {@link String} value for metadata value
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject addContentMetadata(String key, String value) {
        this.metadata_.put(key, value);
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
    @SuppressWarnings("unused")
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


    public boolean isPublicallyIndexable() {
        return indexMode_ == CONTENT_INDEX_MODE.PUBLIC;
    }

    public HashMap<String, String> getMetadata() {
        return metadata_;
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

    public LinkProperties getLinkProperties() {
        return linkProperties_;
    }

    public JSONArray getKeywordsJsonArray() {
        JSONArray keywordArray = new JSONArray();
        for (String keyword : keyWords_) {
            keywordArray.put(keyword);
        }
        return keywordArray;
    }

    //-------------------- Register views--------------------------//

    /**
     * Mark the content referred by this object as viewed. This increment the view count of the contents referred by this object.
     */
    public void registerView() {
        registerView(null);
    }

    /**
     * Mark the content referred by this object as viewed. This increment the view count of the contents referred by this object.
     *
     * @param callback An instance of {@link RegisterViewStatusListener} to listen to results of the operation
     */
    public void registerView(RegisterViewStatusListener callback) {
        if (Branch.getInstance() != null) {
            Branch.getInstance().registerView(this, callback);
        } else {
            if (callback != null) {
                callback.onRegisterViewFinished(false, new BranchError("Register view error", BranchError.ERR_BRANCH_NOT_INSTANTIATED));
            }
        }
    }


    /**
     * <p>
     * Callback interface for listening register content view status
     * </p>
     */
    public interface RegisterViewStatusListener {
        /**
         * Called on finishing the the register view process
         *
         * @param registered A {@link Boolean} which is set to true if register content view succeeded
         * @param error      An instance of {@link BranchError} to notify any error occurred during registering a content view event.
         *                   A null value is set if the registering content view succeeds
         */
        void onRegisterViewFinished(boolean registered, BranchError error);
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
            for (String key : metadata_.keySet()) {
                params.put(key, metadata_.get(key));
            }
            HashMap<String, String> controlParams = linkProperties.getControlParams();
            for (String key : controlParams.keySet()) {
                params.put(key, controlParams.get(key));
            }
        } catch (JSONException ignore) {
        }
        @SuppressWarnings("deprecation")
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


    @SuppressWarnings("deprecation")
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
        for (String key : metadata_.keySet()) {
            shortLinkBuilder.addParameters(key, metadata_.get(key));
        }
        for (String key : controlParam.keySet()) {
            shortLinkBuilder.addParameters(key, controlParam.get(key));
        }


        return shortLinkBuilder;
    }

    /**
     * Get the {@link BranchUniversalObject} associated with the latest deep linking. This should retrieve the
     * exact object used for creating the deep link. This should be called only after initialising Branch Session.
     *
     * @return  A {@link BranchUniversalObject} used to create the deep link that started the this app session.
     * Null is returned if this session is not started by Branch link click
     */
    public static BranchUniversalObject getReferredBrachUniversalObject() {
        BranchUniversalObject branchUniversalObject = null;
        Branch branchInstance = Branch.getInstance();
        if (branchInstance != null && branchInstance.getLatestReferringParams() != null) {
            JSONObject latestParam = branchInstance.getLatestReferringParams();
            try {
                if (latestParam.has("+clicked_branch_link") && latestParam.getBoolean("+clicked_branch_link")) {
                    branchUniversalObject = new BranchUniversalObject();

                    if (latestParam.has(Defines.Jsonkey.ContentTitle.getKey())) {
                        branchUniversalObject.title_ = latestParam.getString(Defines.Jsonkey.ContentTitle.getKey());
                        latestParam.remove(Defines.Jsonkey.ContentTitle.getKey());
                    }
                    if (latestParam.has(Defines.Jsonkey.CanonicalIdentifier.getKey())) {
                        branchUniversalObject.canonicalIdentifier_ = latestParam.getString(Defines.Jsonkey.CanonicalIdentifier.getKey());
                        latestParam.remove(Defines.Jsonkey.CanonicalIdentifier.getKey());
                    }
                    if (latestParam.has(Defines.Jsonkey.ContentKeyWords.getKey())) {
                        JSONArray keywordJsonArray = latestParam.getJSONArray(Defines.Jsonkey.ContentKeyWords.getKey());
                        for (int i = 0; i < keywordJsonArray.length(); i++) {
                            branchUniversalObject.keyWords_.add((String) keywordJsonArray.get(i));
                        }
                        latestParam.remove(Defines.Jsonkey.ContentKeyWords.getKey());
                    }
                    if (latestParam.has(Defines.Jsonkey.ContentDesc.getKey())) {
                        branchUniversalObject.description_ = latestParam.getString(Defines.Jsonkey.ContentDesc.getKey());
                        latestParam.remove(Defines.Jsonkey.ContentDesc.getKey());
                    }
                    if (latestParam.has(Defines.Jsonkey.ContentImgUrl.getKey())) {
                        branchUniversalObject.imageUrl_ = latestParam.getString(Defines.Jsonkey.ContentImgUrl.getKey());
                        latestParam.remove(Defines.Jsonkey.ContentImgUrl.getKey());
                    }
                    if (latestParam.has(Defines.Jsonkey.ContentType.getKey())) {
                        branchUniversalObject.type_ = latestParam.getString(Defines.Jsonkey.ContentType.getKey());
                        latestParam.remove(Defines.Jsonkey.ContentType.getKey());
                    }
                    if (latestParam.has(Defines.Jsonkey.ContentExpiryTime.getKey())) {
                        branchUniversalObject.expiration_ = latestParam.getLong(Defines.Jsonkey.ContentExpiryTime.getKey());
                        latestParam.remove(Defines.Jsonkey.ContentExpiryTime.getKey());
                    }

                    ///-----------Link Properties----------------///
                    if (latestParam.has("~channel")) {
                        branchUniversalObject.linkProperties_.setChannel(latestParam.getString("~channel"));
                        latestParam.remove("~channel");
                    }
                    if (latestParam.has("~creation_source")) {
                        latestParam.remove("~creation_source");
                    }
                    if (latestParam.has("~feature")) {
                        branchUniversalObject.linkProperties_.setFeature(latestParam.getString("~feature"));
                        latestParam.remove("~feature");
                    }
                    if (latestParam.has("~id")) {
                        latestParam.remove("~id");
                    }
                    if (latestParam.has("~stage")) {
                        branchUniversalObject.linkProperties_.setStage(latestParam.getString("~stage"));
                        latestParam.remove("~stage");
                    }
                    if (latestParam.has("~duration")) {
                        branchUniversalObject.linkProperties_.setDuration(latestParam.getInt("~duration"));
                        latestParam.remove("~duration");
                    }
                    if (latestParam.has("$match_duration")) {
                        branchUniversalObject.linkProperties_.setDuration(latestParam.getInt("$match_duration"));
                        latestParam.remove("$match_duration");
                    }
                    if (latestParam.has("~tags")) {
                        JSONArray tagsArray = latestParam.getJSONArray("~tags");
                        for (int i = 0; i < tagsArray.length(); i++) {
                            branchUniversalObject.linkProperties_.addTag(tagsArray.getString(i));
                        }
                        latestParam.remove("~tags");
                    }

                    latestParam.remove("+match_guaranteed");
                    latestParam.remove("+click_timestamp");
                    latestParam.remove("+is_first_session");
                    latestParam.remove("+clicked_branch_link");
                    latestParam.remove("$match_duration");

                    Iterator<String> keys = latestParam.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (key.startsWith("$")) {
                            branchUniversalObject.linkProperties_.addControlParameter(key, latestParam.getString(key));
                        } else {
                            branchUniversalObject.addContentMetadata(key, latestParam.getString(key));
                        }
                    }
                }

            } catch (Exception ignore) {
            }
        }
        return branchUniversalObject;
    }

}
