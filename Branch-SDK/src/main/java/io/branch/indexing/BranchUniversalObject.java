package io.branch.indexing;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.TracingController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchLogger;
import io.branch.referral.BranchShareSheetBuilder;
import io.branch.referral.BranchShortLinkBuilder;
import io.branch.referral.BranchUtil;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.TrackingController;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

/**
 * <p>Class represents a single piece of content within your app, as well as any associated metadata.
 * It provides convenient methods for sharing, deep linking, and tracking how often that content is viewed. This information is then used to provide you with powerful content analytics
 * and deep linking.
 * </p>
 */
public class BranchUniversalObject implements Parcelable {
    /* Canonical identifier for the content referred. */
    private String canonicalIdentifier_;
    /* Canonical url for the content referred. This would be the corresponding website URL */
    private String canonicalUrl_;
    /* Title for the content referred by BranchUniversalObject */
    private String title_;
    /* Description for the content referred */
    private String description_;
    /* An image url associated with the content referred */
    private String imageUrl_;
    /* Meta data provided for the content. {@link ContentMetadata} object holds the metadata for this content */
    private ContentMetadata metadata_;
    /* Content index mode */
    private CONTENT_INDEX_MODE indexMode_;
    /* Any keyword associated with the content. Used for indexing */
    private final ArrayList<String> keywords_;
    /* Expiry date for the content and any associated links. Represented as epoch milli second */
    private long expirationInMilliSec_;
    /* Index mode for  local content indexing */
    private CONTENT_INDEX_MODE localIndexMode_;
    private long creationTimeStamp_;
    
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
        metadata_ = new ContentMetadata();
        keywords_ = new ArrayList<>();
        canonicalIdentifier_ = "";
        canonicalUrl_ = "";
        title_ = "";
        description_ = "";
        indexMode_ = CONTENT_INDEX_MODE.PUBLIC; // Default content indexing mode is public
        localIndexMode_ = CONTENT_INDEX_MODE.PUBLIC; // Default local indexing mode is public
        expirationInMilliSec_ = 0L;
        creationTimeStamp_ = System.currentTimeMillis();
    }
    
    /**
     * <p>
     * Set the canonical identifier for this BranchUniversalObject. Canonical identifier is normally the canonical path for your content in the application or web
     * </p>
     *
     * @param canonicalIdentifier A {@link String} with value for the canonical identifier
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setCanonicalIdentifier(@NonNull String canonicalIdentifier) {
        this.canonicalIdentifier_ = canonicalIdentifier;
        return this;
    }
    
    /**
     * <p>
     * Canonical url for the content referred. This would be the corresponding website URL.
     * </p>
     *
     * @param canonicalUrl A {@link String} with value for the canonical url
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setCanonicalUrl(@NonNull String canonicalUrl) {
        this.canonicalUrl_ = canonicalUrl;
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
    public BranchUniversalObject setTitle(@NonNull String title) {
        this.title_ = title;
        return this;
    }
    
    /**
     * <p>
     * Set description for the content for the content referred by this object
     * </p>
     *
     * @param description A {@link String} with value for the description of the content referred by this object
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentDescription(String description) {
        this.description_ = description;
        return this;
    }
    
    /**
     * <p>
     * Set the url to any image associated with this content.
     * </p>
     *
     * @param imageUrl A {@link String} specifying a url to an image associated with content referred by this object
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentImageUrl(@NonNull String imageUrl) {
        this.imageUrl_ = imageUrl;
        return this;
    }
    
    /**
     * @deprecated please use #setContentMetadata instead
     */
    public BranchUniversalObject addContentMetadata(HashMap<String, String> metadata) {
        if (metadata != null) {
            Iterator<String> keys = metadata.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                metadata_.addCustomMetadata(key, metadata.get(key));
            }
        }
        return this;
    }
    
    /**
     * @deprecated please use #setContentMetadata instead
     */
    public BranchUniversalObject addContentMetadata(String key, String value) {
        metadata_.addCustomMetadata(key, value);
        return this;
    }
    
    /**
     * Set the metadata associated with the content. Please see {@link ContentMetadata}
     *
     * @param metadata Instance of {@link ContentMetadata}. Holds the metadata for the contents of this BUO
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentMetadata(ContentMetadata metadata) {
        this.metadata_ = metadata;
        return this;
    }
    
    /**
     * @deprecated Please use {@link ContentMetadata#contentSchema}.
     * Please see {@link #setContentMetadata(ContentMetadata)}
     */
    public BranchUniversalObject setContentType(String type) {
        return this;
    }
    
    /**
     * <p>
     * Set the indexing mode for the content referred in this object
     * </p>
     *
     * @param indexMode {@link BranchUniversalObject.CONTENT_INDEX_MODE} value for the content referred
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setContentIndexingMode(CONTENT_INDEX_MODE indexMode) {
        this.indexMode_ = indexMode;
        return this;
    }
    
    /**
     * <p>
     * Set the Local indexing mode for the content referred in this object.
     * NOTE: The locally indexable contents are added to the local indexing services , if supported, when listing the contents on Google or other content indexing services.
     * So please make sure you are marking local index mode to {@link CONTENT_INDEX_MODE#PRIVATE} if you don't want to list the contents locally on device
     * </p>
     *
     * @param localIndexMode {@link BranchUniversalObject.CONTENT_INDEX_MODE} value for the content referred
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject setLocalIndexMode(CONTENT_INDEX_MODE localIndexMode) {
        this.localIndexMode_ = localIndexMode;
        return this;
    }
    
    /**
     * <p>
     * Adds any keywords associated with the content referred
     * </p>
     *
     * @param keywords An {@link ArrayList} of {@link String} values
     * @return This instance to allow for chaining of calls to set methods
     */
    @SuppressWarnings("unused")
    public BranchUniversalObject addKeyWords(ArrayList<String> keywords) {
        this.keywords_.addAll(keywords);
        return this;
    }
    
    /**
     * <p>
     * Add a keyword associated with the content referred
     * </p>
     *
     * @param keyword A{@link String} with value for keyword
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchUniversalObject addKeyWord(String keyword) {
        this.keywords_.add(keyword);
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
        this.expirationInMilliSec_ = expirationDate.getTime();
        return this;
    }
    
    /**
     * <p>
     * Set the price associated with content of this BUO if any
     * </p>
     *
     * @param price    A {@link Double} value specifying the price info associated with BUO
     * @param currency ISO 4217 currency code defined in {@link CurrencyType} for the price
     * @return This instance to allow for chaining of calls to set methods
     * @deprecated use {@link ContentMetadata#price} instead. Please check {@link BranchEvent} for more info on commerce event tracking with Branch
     */
    public BranchUniversalObject setPrice(double price, CurrencyType currency) {
        return this;
    }

    /**
     * <p>
     * Specifies whether the contents referred by this object is publically indexable
     * </p>
     *
     * @return A {@link boolean} whose value is set to true if index mode is public
     */
    public boolean isPublicallyIndexable() {
        return indexMode_ == CONTENT_INDEX_MODE.PUBLIC;
    }
    
    /**
     * <p>
     * Specifies whether the contents referred by this object is locally indexable
     * </p>
     *
     * @return A {@link boolean} whose value is set to true if index mode is public
     */
    public boolean isLocallyIndexable() {
        return localIndexMode_ == CONTENT_INDEX_MODE.PUBLIC;
    }
    
    /**
     * @deprecated Please use #getContentMetadata() instead.
     */
    public HashMap<String, String> getMetadata() {
        return metadata_.getCustomMetadata();
    }
    
    /**
     * Get the {@link ContentMetadata} associated with this BUO which holds the metadata for content represented
     *
     * @return {@link ContentMetadata} object representing the content metadata
     */
    public ContentMetadata getContentMetadata() {
        return metadata_;
    }
    
    /**
     * <p>
     * Get expiry date for the content and any associated links. Represented as epoch milli second
     * </p>
     *
     * @return A {@link Long} with content expiration time in epoch milliseconds
     */
    public long getExpirationTime() {
        return expirationInMilliSec_;
    }
    
    /**
     * <p>
     * Get the canonical identifier for this BranchUniversalObject
     * </p>
     *
     * @return A {@link String} with value for the canonical identifier
     */
    public String getCanonicalIdentifier() {
        return canonicalIdentifier_;
    }
    
    /**
     * <p>
     * Get the canonical url for this BranchUniversalObject
     * </p>
     *
     * @return A {@link String} with value for the canonical url
     */
    public String getCanonicalUrl() {
        return canonicalUrl_;
    }
    
    /**
     * <p>
     * Get description for the content for the content referred by this object
     * </p>
     *
     * @return A {@link String} with value for the description of the content referred by this object
     */
    public String getDescription() {
        return description_;
    }
    
    /**
     * <p>
     * Get the url to any image associated with this content.
     * </p>
     *
     * @return A {@link String} specifying a url to an image associated with content referred by this object
     */
    public String getImageUrl() {
        return imageUrl_;
    }
    
    /**
     * <p>
     * Get a title for the content referred by this object
     * </p>
     *
     * @return A {@link String} with value of for the content title
     */
    public String getTitle() {
        return title_;
    }
    
    /**
     * @deprecated please use {@link ContentMetadata#contentSchema}
     */
    public String getType() {
        return null;
    }
    
    /**
     * <p>
     * Gets the price associated with this BUO content
     * </p>
     *
     * @return A {@link Double} with value for price of the content of BUO
     * @deprecated please use {@link ContentMetadata#price} instead
     */
    public double getPrice() {
        return 0.0;
    }
    
    /**
     * <p>
     * Get the currency type of the price for this BUO
     * </p>
     *
     * @return {@link String} with ISO 4217 for this currency. Empty string if there is no currency type set
     * @deprecated Please check {@link BranchEvent} for more info on commerce event tracking with Branch
     */
    public String getCurrencyType() {
        return null;
    }
    
    /**
     * Get the keywords associated with this {@link BranchUniversalObject}
     *
     * @return A {@link JSONArray} with keywords associated with this {@link BranchUniversalObject}
     */
    public JSONArray getKeywordsJsonArray() {
        JSONArray keywordArray = new JSONArray();
        for (String keyword : keywords_) {
            keywordArray.put(keyword);
        }
        return keywordArray;
    }
    
    /**
     * Get the keywords associated with this {@link BranchUniversalObject}
     *
     * @return A {@link ArrayList} with keywords associated with this {@link BranchUniversalObject}
     */
    @SuppressWarnings("unused")
    public ArrayList<String> getKeywords() {
        return keywords_;
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
    public void registerView(@Nullable RegisterViewStatusListener callback) {
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
         * @param registered A {@link boolean} which is set to true if register content view succeeded
         * @param error      An instance of {@link BranchError} to notify any error occurred during registering a content view event.
         *                   A null value is set if the registering content view succeeds
         */
        void onRegisterViewFinished(boolean registered, BranchError error);
    }
    
    
    //--------------------- Create Link --------------------------//
    
    /**
     * Creates a short url for the BUO synchronously.
     *
     * @param context        {@link Context} instance
     * @param linkProperties An object of {@link LinkProperties} specifying the properties of this link
     * @return A {@link String} with value of the short url created for this BUO. A long url for the BUO is returned in case link creation fails
     */
    public String getShortUrl(@NonNull Context context, @NonNull LinkProperties linkProperties) {
        return getLinkBuilder(context, linkProperties).getShortUrl();
    }
    
    /**
     * Creates a short url for the BUO synchronously.
     *
     * @param context          {@link Context} instance
     * @param linkProperties   An object of {@link LinkProperties} specifying the properties of this link
     * @param defaultToLongUrl A {@link boolean} specifies if a long url should be returned in case of link creation error
     *                         If set to false, NULL is returned in case of link creation error
     * @return A {@link String} with value of the short url created for this BUO. NULL is returned in case link creation fails
     */
    public String getShortUrl(@NonNull Context context, @NonNull LinkProperties linkProperties, boolean defaultToLongUrl) {
        return getLinkBuilder(context, linkProperties).setDefaultToLongUrl(defaultToLongUrl).getShortUrl();
    }
    
    /**
     * Creates a short url for the BUO asynchronously
     *
     * @param context        {@link Context} instance
     * @param linkProperties An object of {@link LinkProperties} specifying the properties of this link
     * @param callback       An instance of {@link io.branch.referral.Branch.BranchLinkCreateListener} to receive the results
     */
    public void generateShortUrl(@NonNull Context context, @NonNull LinkProperties linkProperties, @Nullable Branch.BranchLinkCreateListener callback) {
        if (TrackingController.isTrackingDisabled(context) && callback != null) {
            callback.onLinkCreate(getLinkBuilder(context, linkProperties).getShortUrl(), null);
        } else {
            getLinkBuilder(context, linkProperties).generateShortUrl(callback);
        }
    }
    
    /**
     * Creates a short url for the BUO asynchronously.
     *
     * @param context          {@link Context} instance
     * @param linkProperties   An object of {@link LinkProperties} specifying the properties of this link
     * @param callback         An instance of {@link io.branch.referral.Branch.BranchLinkCreateListener} to receive the results
     * @param defaultToLongUrl A {@link boolean} specifies if a long url should be returned in case of link creation error
     *                         If set to false, NULL is returned in case of link creation error
     */
    public void generateShortUrl(@NonNull Context context, @NonNull LinkProperties linkProperties, @Nullable Branch.BranchLinkCreateListener callback, boolean defaultToLongUrl) {
        getLinkBuilder(context, linkProperties).setDefaultToLongUrl(defaultToLongUrl).generateShortUrl(callback);
    }
    
    
    //------------------ Share sheet -------------------------------------//
    
    public void showShareSheet(@NonNull Activity activity, @NonNull LinkProperties linkProperties, @NonNull ShareSheetStyle style, @Nullable Branch.BranchLinkShareListener callback) {
        showShareSheet(activity, linkProperties, style, callback, null);
    }
    
    public void showShareSheet(@NonNull Activity activity, @NonNull LinkProperties linkProperties, @NonNull ShareSheetStyle style, @Nullable Branch.BranchLinkShareListener callback, Branch.IChannelProperties channelProperties) {
        if (Branch.getInstance() == null) {  //if in case Branch instance is not created. In case of user missing create instance or BranchApp in manifest
            if (callback != null) {
                callback.onLinkShareResponse(null, null, new BranchError("Trouble sharing link. ", BranchError.ERR_BRANCH_NOT_INSTANTIATED));
            } else {
                BranchLogger.v("Sharing error. Branch instance is not created yet. Make sure you have initialised Branch.");
            }
        } else {
            BranchShareSheetBuilder shareLinkBuilder = new BranchShareSheetBuilder(activity, getLinkBuilder(activity, linkProperties));
            shareLinkBuilder.setCallback(new LinkShareListenerWrapper(callback, shareLinkBuilder, linkProperties))
                    .setChannelProperties(channelProperties)
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
            if (style.getStyleResourceID() > 0) {
                shareLinkBuilder.setStyleResourceID(style.getStyleResourceID());
            }
            shareLinkBuilder.setDividerHeight(style.getDividerHeight());
            shareLinkBuilder.setAsFullWidthStyle(style.getIsFullWidthStyle());
            shareLinkBuilder.setDialogThemeResourceID(style.getDialogThemeResourceID());
            shareLinkBuilder.setSharingTitle(style.getSharingTitle());
            shareLinkBuilder.setSharingTitle(style.getSharingTitleView());
            shareLinkBuilder.setIconSize(style.getIconSize());
            
            if (style.getIncludedInShareSheet() != null && style.getIncludedInShareSheet().size() > 0) {
                shareLinkBuilder.includeInShareSheet(style.getIncludedInShareSheet());
            }
            if (style.getExcludedFromShareSheet() != null && style.getExcludedFromShareSheet().size() > 0) {
                shareLinkBuilder.excludeFromShareSheet(style.getExcludedFromShareSheet());
            }
            shareLinkBuilder.shareLink();
        }
    }
    
    private BranchShortLinkBuilder getLinkBuilder(@NonNull Context context, @NonNull LinkProperties linkProperties) {
        BranchShortLinkBuilder shortLinkBuilder = new BranchShortLinkBuilder(context);
        return getLinkBuilder(shortLinkBuilder, linkProperties);
    }
    
    private BranchShortLinkBuilder getLinkBuilder(@NonNull BranchShortLinkBuilder shortLinkBuilder, @NonNull LinkProperties linkProperties) {
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
        if (linkProperties.getCampaign() != null) {
            shortLinkBuilder.setCampaign(linkProperties.getCampaign());
        }
        if (linkProperties.getMatchDuration() > 0) {
            shortLinkBuilder.setDuration(linkProperties.getMatchDuration());
        }
        if (!TextUtils.isEmpty(title_)) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.ContentTitle.getKey(), title_);
        }
        if (!TextUtils.isEmpty(canonicalIdentifier_)) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.CanonicalIdentifier.getKey(), canonicalIdentifier_);
        }
        if (!TextUtils.isEmpty(canonicalUrl_)) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.CanonicalUrl.getKey(), canonicalUrl_);
        }
        JSONArray keywords = getKeywordsJsonArray();
        if (keywords.length() > 0) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.ContentKeyWords.getKey(), keywords);
        }
        if (!TextUtils.isEmpty(description_)) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.ContentDesc.getKey(), description_);
        }
        if (!TextUtils.isEmpty(imageUrl_)) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.ContentImgUrl.getKey(), imageUrl_);
        }
        if (expirationInMilliSec_ > 0) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.ContentExpiryTime.getKey(), "" + expirationInMilliSec_);
        }
        shortLinkBuilder.addParameters(Defines.Jsonkey.PublicallyIndexable.getKey(), "" + isPublicallyIndexable());
        JSONObject metadataJson = metadata_.convertToJson();
        try {
            Iterator<String> keys = metadataJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                shortLinkBuilder.addParameters(key, metadataJson.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        HashMap<String, String> controlParam = linkProperties.getControlParams();
        for (String key : controlParam.keySet()) {
            shortLinkBuilder.addParameters(key, controlParam.get(key));
        }
        return shortLinkBuilder;
    }
    
    /**
     * Get the {@link BranchUniversalObject} associated with the latest deep linking. This should retrieve the
     * exact object used for creating the deep link. This should be called only after initialising Branch Session.
     *
     * @return A {@link BranchUniversalObject} used to create the deep link that started the this app session.
     * Null is returned if this session is not started by Branch link click
     */
    public static BranchUniversalObject getReferredBranchUniversalObject() {
        BranchUniversalObject branchUniversalObject = null;
        Branch branchInstance = Branch.getInstance();
        try {
            if (branchInstance != null && branchInstance.getLatestReferringParams() != null) {
                // Check if link clicked. Unless deep link debug enabled return null if there is no link click
                if (branchInstance.getLatestReferringParams().has("+clicked_branch_link") && branchInstance.getLatestReferringParams().getBoolean("+clicked_branch_link")) {
                    branchUniversalObject = createInstance(branchInstance.getLatestReferringParams());
                }
                // If debug params are set then send BUO object even if link click is false
                else if (branchInstance.getDeeplinkDebugParams() != null && branchInstance.getDeeplinkDebugParams().length() > 0) {
                    branchUniversalObject = createInstance(branchInstance.getLatestReferringParams());
                }
            }
        } catch (Exception e) {
            BranchLogger.d(e.getMessage());
        }
        return branchUniversalObject;
    }
    
    /**
     * Creates a new BranchUniversalObject with the data provided by {@link JSONObject}.
     *
     * @param jsonObject {@link JSONObject} to create the BranchUniversalObject
     * @return A {@link BranchUniversalObject} corresponding to the Json data passed in
     */
    public static BranchUniversalObject createInstance(JSONObject jsonObject) {
        
        BranchUniversalObject branchUniversalObject = null;
        try {
            branchUniversalObject = new BranchUniversalObject();
            BranchUtil.JsonReader jsonReader = new BranchUtil.JsonReader(jsonObject);
            branchUniversalObject.title_ = jsonReader.readOutString(Defines.Jsonkey.ContentTitle.getKey());
            branchUniversalObject.canonicalIdentifier_ = jsonReader.readOutString(Defines.Jsonkey.CanonicalIdentifier.getKey());
            branchUniversalObject.canonicalUrl_ = jsonReader.readOutString(Defines.Jsonkey.CanonicalUrl.getKey());
            branchUniversalObject.description_ = jsonReader.readOutString(Defines.Jsonkey.ContentDesc.getKey());
            branchUniversalObject.imageUrl_ = jsonReader.readOutString(Defines.Jsonkey.ContentImgUrl.getKey());
            branchUniversalObject.expirationInMilliSec_ = jsonReader.readOutLong(Defines.Jsonkey.ContentExpiryTime.getKey());
            JSONArray keywordJsonArray = null;
            Object keyWordArrayObject = jsonReader.readOut(Defines.Jsonkey.ContentKeyWords.getKey());
            if (keyWordArrayObject instanceof JSONArray) {
                keywordJsonArray = (JSONArray) keyWordArrayObject;
            } else if (keyWordArrayObject instanceof String) {
                keywordJsonArray = new JSONArray((String) keyWordArrayObject);
            }
            if (keywordJsonArray != null) {
                for (int i = 0; i < keywordJsonArray.length(); i++) {
                    branchUniversalObject.keywords_.add((String) keywordJsonArray.get(i));
                }
            }
            Object indexableVal = jsonReader.readOut(Defines.Jsonkey.PublicallyIndexable.getKey());
            if (indexableVal instanceof Boolean) {
                branchUniversalObject.indexMode_ = (Boolean) indexableVal ? CONTENT_INDEX_MODE.PUBLIC : CONTENT_INDEX_MODE.PRIVATE;
            } else if (indexableVal instanceof Integer) {
                // iOS compatibility issue. iOS send 0/1 instead of true or false
                branchUniversalObject.indexMode_ = (Integer) indexableVal == 1 ? CONTENT_INDEX_MODE.PUBLIC : CONTENT_INDEX_MODE.PRIVATE;
            }
            branchUniversalObject.localIndexMode_ = jsonReader.readOutBoolean(Defines.Jsonkey.LocallyIndexable.getKey()) ? CONTENT_INDEX_MODE.PUBLIC : CONTENT_INDEX_MODE.PRIVATE;
            branchUniversalObject.creationTimeStamp_ = jsonReader.readOutLong(Defines.Jsonkey.CreationTimestamp.getKey());
            
            branchUniversalObject.metadata_ = ContentMetadata.createFromJson(jsonReader);
            // PRS : Handling a  backward compatibility issue here. Previous version of BUO Allows adding metadata key value pairs to the Object.
            // If the Json is received from a previous version of BUO it may have metadata set in the object. Adding them to custom metadata for now.
            // Please note that #getMetadata() is deprecated and #getContentMetadata() should be the new way of getting metadata
            JSONObject pendingJson = jsonReader.getJsonObject();
            Iterator<String> keys = pendingJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                branchUniversalObject.metadata_.addCustomMetadata(key, pendingJson.optString(key));
            }
            
        } catch (Exception e) {
            BranchLogger.d(Objects.requireNonNull(e.getMessage()));
        }
        return branchUniversalObject;
    }
    
    //-------------Object flattening methods--------------------//
    
    /**
     * Convert the BUO to  corresponding Json representation
     *
     * @return A {@link JSONObject} which represent this BUO
     */
    public JSONObject convertToJson() {
        JSONObject buoJsonModel = new JSONObject();
        try {
            // Add all keys in plane format  initially. All known keys will be replaced with corresponding data type in the following section
            JSONObject metadataJsonObject = metadata_.convertToJson();
            Iterator<String> keys = metadataJsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                buoJsonModel.put(key, metadataJsonObject.get(key));
            }
            if (!TextUtils.isEmpty(title_)) {
                buoJsonModel.put(Defines.Jsonkey.ContentTitle.getKey(), title_);
            }
            if (!TextUtils.isEmpty(canonicalIdentifier_)) {
                buoJsonModel.put(Defines.Jsonkey.CanonicalIdentifier.getKey(), canonicalIdentifier_);
            }
            if (!TextUtils.isEmpty(canonicalUrl_)) {
                buoJsonModel.put(Defines.Jsonkey.CanonicalUrl.getKey(), canonicalUrl_);
            }
            if (keywords_.size() > 0) {
                JSONArray keyWordJsonArray = new JSONArray();
                for (String keyword : keywords_) {
                    keyWordJsonArray.put(keyword);
                }
                buoJsonModel.put(Defines.Jsonkey.ContentKeyWords.getKey(), keyWordJsonArray);
            }
            if (!TextUtils.isEmpty(description_)) {
                buoJsonModel.put(Defines.Jsonkey.ContentDesc.getKey(), description_);
            }
            if (!TextUtils.isEmpty(imageUrl_)) {
                buoJsonModel.put(Defines.Jsonkey.ContentImgUrl.getKey(), imageUrl_);
            }
            if (expirationInMilliSec_ > 0) {
                buoJsonModel.put(Defines.Jsonkey.ContentExpiryTime.getKey(), expirationInMilliSec_);
            }
            buoJsonModel.put(Defines.Jsonkey.PublicallyIndexable.getKey(), isPublicallyIndexable());
            buoJsonModel.put(Defines.Jsonkey.LocallyIndexable.getKey(), isLocallyIndexable());
            buoJsonModel.put(Defines.Jsonkey.CreationTimestamp.getKey(), creationTimeStamp_);
            
        } catch (JSONException e) {
            BranchLogger.d(Objects.requireNonNull(e.getMessage()));
        }
        return buoJsonModel;
    }
    
    //---------------------Marshaling and Unmarshaling----------//
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BranchUniversalObject createFromParcel(Parcel in) {
            return new BranchUniversalObject(in);
        }
        
        public BranchUniversalObject[] newArray(int size) {
            return new BranchUniversalObject[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(creationTimeStamp_);
        dest.writeString(canonicalIdentifier_);
        dest.writeString(canonicalUrl_);
        dest.writeString(title_);
        dest.writeString(description_);
        dest.writeString(imageUrl_);
        dest.writeLong(expirationInMilliSec_);
        dest.writeInt(indexMode_.ordinal());
        dest.writeSerializable(keywords_);
        dest.writeParcelable(metadata_, flags);
        dest.writeInt(localIndexMode_.ordinal());
    }
    
    private BranchUniversalObject(Parcel in) {
        this();
        creationTimeStamp_ = in.readLong();
        canonicalIdentifier_ = in.readString();
        canonicalUrl_ = in.readString();
        title_ = in.readString();
        description_ = in.readString();
        imageUrl_ = in.readString();
        expirationInMilliSec_ = in.readLong();
        indexMode_ = CONTENT_INDEX_MODE.values()[in.readInt()];
        @SuppressWarnings("unchecked")
        ArrayList<String> keywordsTemp = (ArrayList<String>) in.readSerializable();
        if (keywordsTemp != null) {
            keywords_.addAll(keywordsTemp);
        }
        metadata_ = in.readParcelable(ContentMetadata.class.getClassLoader());
        localIndexMode_ = CONTENT_INDEX_MODE.values()[in.readInt()];
    }
    
    /**
     * Class for intercepting share sheet events to report auto events on BUO
     */
    private class LinkShareListenerWrapper implements Branch.BranchLinkShareListener {
        private final Branch.BranchLinkShareListener originalCallback_;
        private final BranchShareSheetBuilder shareSheetBuilder_;
        private final LinkProperties linkProperties_;
        
        LinkShareListenerWrapper(Branch.BranchLinkShareListener originalCallback, BranchShareSheetBuilder shareLinkBuilder, LinkProperties linkProperties) {
            originalCallback_ = originalCallback;
            shareSheetBuilder_ = shareLinkBuilder;
            linkProperties_ = linkProperties;
        }
        
        @Override
        public void onShareLinkDialogLaunched() {
            if (originalCallback_ != null) {
                originalCallback_.onShareLinkDialogLaunched();
            }
        }
        
        @Override
        public void onShareLinkDialogDismissed() {
            if (originalCallback_ != null) {
                originalCallback_.onShareLinkDialogDismissed();
            }
        }
        
        @Override
        public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
            BranchEvent shareEvent = new BranchEvent(BRANCH_STANDARD_EVENT.SHARE);
            if (error == null) {
                shareEvent.addCustomDataProperty(Defines.Jsonkey.SharedLink.getKey(), sharedLink);
                shareEvent.addCustomDataProperty(Defines.Jsonkey.SharedChannel.getKey(), sharedChannel);
                shareEvent.addContentItems(BranchUniversalObject.this);
            } else {
                shareEvent.addCustomDataProperty(Defines.Jsonkey.ShareError.getKey(), error.getMessage());
            }

            shareEvent.logEvent(Branch.getInstance().getApplicationContext());

            if (originalCallback_ != null) {
                originalCallback_.onLinkShareResponse(sharedLink, sharedChannel, error);
            }
        }
        
        @Override
        public void onChannelSelected(String channelName) {
            if (originalCallback_ != null) {
                originalCallback_.onChannelSelected(channelName);
            }
            if (originalCallback_ instanceof Branch.ExtendedBranchLinkShareListener) {
                if (((Branch.ExtendedBranchLinkShareListener) originalCallback_).onChannelSelected(channelName, BranchUniversalObject.this, linkProperties_)) {
                    shareSheetBuilder_.setShortLinkBuilderInternal(getLinkBuilder(shareSheetBuilder_.getShortLinkBuilder(), linkProperties_));
                }
            }
        }
    }
}
