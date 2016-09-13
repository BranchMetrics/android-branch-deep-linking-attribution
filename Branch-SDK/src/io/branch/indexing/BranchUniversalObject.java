package io.branch.indexing;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchShortLinkBuilder;
import io.branch.referral.Defines;
import io.branch.referral.util.BranchEvent;
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
    /* Meta data provided for the content. This meta data is used as the link parameters for links created from this object */
    private final HashMap<String, String> metadata_;
    /* Mime type for the content referred */
    private String type_;
    /* Content index mode */
    private CONTENT_INDEX_MODE indexMode_;
    /* Any keyword associated with the content. Used for indexing */
    private final ArrayList<String> keywords_;
    /* Expiry date for the content and any associated links. Represented as epoch milli second */
    private long expirationInMilliSec_;
    /* Price associated with the content of this BUO */
    private Double price_;
    /* Type of the currency associated with the price */
    private CurrencyType currency_;

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
        keywords_ = new ArrayList<>();
        canonicalIdentifier_ = "";
        canonicalUrl_ = "";
        title_ = "";
        description_ = "";
        type_ = "";
        indexMode_ = CONTENT_INDEX_MODE.PUBLIC; // Default content indexing mode is public
        expirationInMilliSec_ = 0L;
        currency_ = CurrencyType.USD;
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
    public BranchUniversalObject setContentImageUrl(@NonNull String imageUrl) {
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
     * @param indexMode {@link BranchUniversalObject.CONTENT_INDEX_MODE} value for the content referred
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
     */
    public BranchUniversalObject setPrice(double price, CurrencyType currency) {
        price_ = price;
        currency_ = currency;
        return this;
    }

    /**
     * <p/>
     * Publish this BUO with Google app indexing so that the contents will be available with google search
     * with branch link pointing to the app.
     * </p?>
     *
     * @param context Application context
     */
    public void listOnGoogleSearch(Context context) {
        AppIndexingHelper.addToAppIndex(context, this);
    }

    /**
     * <p>
     * Method to report user actions happened on this BUO. Use this method to report the user actions for analytics purpose.
     * </p>
     *
     * @param action A {@link String }with value of user action name.  See {@link io.branch.referral.util.BranchEvent} for Branch defined user events.
     */
    public void userCompletedAction(String action) {
        userCompletedAction(action, null);
    }

    /**
     * <p>
     * Method to report user actions happened on this BUO. Use this method to report the user actions for analytics purpose.
     * </p>
     *
     * @param action   A {@link String }with value of user action name.  See {@link io.branch.referral.util.BranchEvent} for Branch defined user events.
     * @param metadata A HashMap containing any additional metadata need to add to this user event
     */
    public void userCompletedAction(String action, HashMap<String, String> metadata) {
        JSONObject actionCompletedPayload = new JSONObject();
        try {
            JSONArray canonicalIDList = new JSONArray();
            canonicalIDList.put(canonicalIdentifier_);
            actionCompletedPayload.put(BranchEvent.CANONICAL_ID_LIST, canonicalIDList);
            actionCompletedPayload.put(canonicalIdentifier_, convertToJson());
            if (metadata != null) {
                for (String key : metadata.keySet()) {
                    actionCompletedPayload.put(key, metadata.get(key));
                }
            }
            if (Branch.getInstance() != null) {
                Branch.getInstance().userCompletedAction(action, actionCompletedPayload);
            }
        } catch (JSONException ignore) {
        }
    }

    /**
     * <p>
     * Specifies whether the contents referred by this object is publically indexable
     * </p>
     *
     * @return A {@link Boolean} whose value is set to true if index mode is public
     */
    public boolean isPublicallyIndexable() {
        return indexMode_ == CONTENT_INDEX_MODE.PUBLIC;
    }

    /**
     * <p>
     * Get the meta data provided for the content referred bt this object
     * </p>
     *
     * @return A {@link HashMap} containing metadata for the provided for this {@link BranchUniversalObject}
     */
    public HashMap<String, String> getMetadata() {
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
     * <p/>
     * Get description for the content for the content referred by this object
     * <p/>
     *
     * @return A {@link String} with value for the description of the content referred by this object
     */
    public String getDescription() {
        return description_;
    }

    /**
     * <p/>
     * Get the url to any image associated with this content.
     * <p/>
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
     * <p>
     * Get a title for the content referred by this object
     * </p>
     *
     * @return A {@link String} with value of for the content title
     */
    public String getType() {
        return type_;
    }

    /**
     * <p>
     * Gets the price associated with this BUO content
     * </p>
     *
     * @return A {@link Double} with value for price of the content of BUO
     */
    public double getPrice() {
        return price_ != null ? price_ : 0.0;
    }

    /**
     * <p>
     * Get the currency type of the price for this BUO
     * </p>
     *
     * @return {@link String} with ISO 4217 for this currency. Empty string if there is no currency type set
     */
    public String getCurrencyType() {
        return currency_.toString();
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
         * @param registered A {@link Boolean} which is set to true if register content view succeeded
         * @param error      An instance of {@link BranchError} to notify any error occurred during registering a content view event.
         *                   A null value is set if the registering content view succeeds
         */
        void onRegisterViewFinished(boolean registered, BranchError error);
    }


    //--------------------- Create Link --------------------------//


    public String getShortUrl(@NonNull Context context, @NonNull LinkProperties linkProperties) {
        return getLinkBuilder(context, linkProperties).getShortUrl();
    }

    public void generateShortUrl(@NonNull Context context, @NonNull LinkProperties linkProperties, @Nullable Branch.BranchLinkCreateListener callback) {
        getLinkBuilder(context, linkProperties).generateShortUrl(callback);
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
                Log.e("BranchSDK", "Sharing error. Branch instance is not created yet. Make sure you have initialised Branch.");
            }
        } else {
            Branch.ShareLinkBuilder shareLinkBuilder = new Branch.ShareLinkBuilder(activity, getLinkBuilder(activity, linkProperties))
                    .setCallback(new LinkShareListenerWrapper(callback))
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
            shareLinkBuilder.setSharingTitle(style.getSharingTitle());
            shareLinkBuilder.setSharingTitle(style.getSharingTitleView());
            shareLinkBuilder.shareLink();
        }
    }

    private BranchShortLinkBuilder getLinkBuilder(@NonNull Context context, @NonNull LinkProperties linkProperties) {
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
        if (!TextUtils.isEmpty(type_)) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.ContentType.getKey(), type_);
        }
        if (expirationInMilliSec_ > 0) {
            shortLinkBuilder.addParameters(Defines.Jsonkey.ContentExpiryTime.getKey(), "" + expirationInMilliSec_);
        }
        shortLinkBuilder.addParameters(Defines.Jsonkey.PublicallyIndexable.getKey(), "" + isPublicallyIndexable());
        if (price_ != null) {
            shortLinkBuilder.addParameters(BranchEvent.PURCHASE_AMOUNT, "" + price_);
            shortLinkBuilder.addParameters(BranchEvent.PURCHASE_CURRENCY, currency_.toString());
        }
        for (String key : metadata_.keySet()) {
            shortLinkBuilder.addParameters(key, metadata_.get(key));
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
        } catch (Exception ignore) {
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

            if (jsonObject.has(Defines.Jsonkey.ContentTitle.getKey())) {
                branchUniversalObject.title_ = jsonObject.getString(Defines.Jsonkey.ContentTitle.getKey());
            }
            if (jsonObject.has(Defines.Jsonkey.CanonicalIdentifier.getKey())) {
                branchUniversalObject.canonicalIdentifier_ = jsonObject.getString(Defines.Jsonkey.CanonicalIdentifier.getKey());
            }
            if (jsonObject.has(Defines.Jsonkey.CanonicalUrl.getKey())) {
                branchUniversalObject.canonicalUrl_ = jsonObject.getString(Defines.Jsonkey.CanonicalUrl.getKey());
            }
            if (jsonObject.has(Defines.Jsonkey.ContentDesc.getKey())) {
                branchUniversalObject.description_ = jsonObject.getString(Defines.Jsonkey.ContentDesc.getKey());
            }
            if (jsonObject.has(Defines.Jsonkey.ContentImgUrl.getKey())) {
                branchUniversalObject.imageUrl_ = jsonObject.getString(Defines.Jsonkey.ContentImgUrl.getKey());
            }
            if (jsonObject.has(Defines.Jsonkey.ContentType.getKey())) {
                branchUniversalObject.type_ = jsonObject.getString(Defines.Jsonkey.ContentType.getKey());
            }
            if (jsonObject.has(Defines.Jsonkey.ContentExpiryTime.getKey())) {
                branchUniversalObject.expirationInMilliSec_ = jsonObject.getLong(Defines.Jsonkey.ContentExpiryTime.getKey());
            }
            if (jsonObject.has(Defines.Jsonkey.PublicallyIndexable.getKey())) {
                branchUniversalObject.indexMode_ = jsonObject.getBoolean(Defines.Jsonkey.PublicallyIndexable.getKey()) ? CONTENT_INDEX_MODE.PUBLIC : CONTENT_INDEX_MODE.PRIVATE;
            }
            if (jsonObject.has(BranchEvent.PURCHASE_AMOUNT)) {
                branchUniversalObject.price_ = jsonObject.getDouble(BranchEvent.PURCHASE_AMOUNT);
            }
            if (jsonObject.has(BranchEvent.PURCHASE_CURRENCY)) {
                branchUniversalObject.currency_ = CurrencyType.valueOf(jsonObject.getString(BranchEvent.PURCHASE_CURRENCY));
            }

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                branchUniversalObject.addContentMetadata(key, jsonObject.getString(key));
            }

            if (jsonObject.has(Defines.Jsonkey.ContentKeyWords.getKey())) {
                JSONArray keywordJsonArray = null;
                Object keyWordArrayObject = jsonObject.get(Defines.Jsonkey.ContentKeyWords.getKey());
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
            }
        } catch (Exception ignore) {
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
            if (!TextUtils.isEmpty(type_)) {
                buoJsonModel.put(Defines.Jsonkey.ContentType.getKey(), type_);
            }
            if (expirationInMilliSec_ > 0) {
                buoJsonModel.put(Defines.Jsonkey.ContentExpiryTime.getKey(), expirationInMilliSec_);
            }
            buoJsonModel.put(Defines.Jsonkey.PublicallyIndexable.getKey(), isPublicallyIndexable());
            if (price_ != null) {
                buoJsonModel.put(BranchEvent.PURCHASE_AMOUNT, price_);
                buoJsonModel.put(BranchEvent.PURCHASE_CURRENCY, currency_.toString());
            }

            Set<String> metadataKeys = metadata_.keySet();
            for (String metadataKey : metadataKeys) {
                buoJsonModel.put(metadataKey, metadata_.get(metadataKey));
            }

        } catch (JSONException ignore) {
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
        dest.writeString(canonicalIdentifier_);
        dest.writeString(canonicalUrl_);
        dest.writeString(title_);
        dest.writeString(description_);
        dest.writeString(imageUrl_);
        dest.writeString(type_);
        dest.writeLong(expirationInMilliSec_);
        dest.writeInt(indexMode_.ordinal());
        double priceVal = price_ != null ? price_ : -1;
        dest.writeDouble(priceVal);
        dest.writeInt(currency_.ordinal());
        dest.writeSerializable(keywords_);

        int metaDataSize = metadata_.size();
        dest.writeInt(metaDataSize);
        for (HashMap.Entry<String, String> entry : metadata_.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    private BranchUniversalObject(Parcel in) {
        this();
        canonicalIdentifier_ = in.readString();
        canonicalUrl_ = in.readString();
        title_ = in.readString();
        description_ = in.readString();
        imageUrl_ = in.readString();
        type_ = in.readString();
        expirationInMilliSec_ = in.readLong();
        indexMode_ = CONTENT_INDEX_MODE.values()[in.readInt()];
        price_ = in.readDouble();
        if (price_ < 0) {
            price_ = null;
        }
        currency_ = CurrencyType.values()[in.readInt()];
        @SuppressWarnings("unchecked")
        ArrayList<String> keywordsTemp = (ArrayList<String>) in.readSerializable();
        keywords_.addAll(keywordsTemp);
        int metadataSize = in.readInt();
        for (int i = 0; i < metadataSize; i++) {
            metadata_.put(in.readString(), in.readString());
        }
    }

    /**
     * Class for intercepting share sheet events to report auto events on BUO
     */
    private class LinkShareListenerWrapper implements Branch.BranchLinkShareListener {
        private final Branch.BranchLinkShareListener originalCallback_;

        public LinkShareListenerWrapper(Branch.BranchLinkShareListener originalCallback) {
            originalCallback_ = originalCallback;
        }

        @Override
        public void onShareLinkDialogLaunched() {
            userCompletedAction(BranchEvent.SHARE_STARTED);
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
            HashMap<String, String> metaData = new HashMap<>();
            if (error == null) {
                metaData.put(Defines.Jsonkey.SharedLink.getKey(), sharedLink);
            } else {
                metaData.put(Defines.Jsonkey.ShareError.getKey(), error.getMessage());
            }
            userCompletedAction(BranchEvent.SHARE_COMPLETED, metaData);

            if (originalCallback_ != null) {
                originalCallback_.onLinkShareResponse(sharedLink, sharedChannel, error);
            }
        }

        @Override
        public void onChannelSelected(String channelName) {
            if (originalCallback_ != null) {
                originalCallback_.onChannelSelected(channelName);
            }
        }
    }

}
