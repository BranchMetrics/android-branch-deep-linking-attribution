package io.branch.indexing;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import io.branch.referral.Branch;
import io.branch.referral.BranchLogger;
import io.branch.referral.BranchShortLinkBuilder;
import io.branch.referral.BranchUtil;
import io.branch.referral.Defines;
import io.branch.referral.TrackingController;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;

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
            branchUniversalObject.localIndexMode_ = jsonReader.readOutBoolean(Defines.Jsonkey.LocallyIndexable.getKey()) ? CONTENT_INDEX_MODE.PUBLIC : CONTENT_INDEX_MODE.PRIVATE;
            branchUniversalObject.creationTimeStamp_ = jsonReader.readOutLong(Defines.Jsonkey.CreationTimestamp.getKey());
            
            branchUniversalObject.metadata_ = ContentMetadata.createFromJson(jsonReader);
            // PRS : Handling a  backward compatibility issue here. Previous version of BUO Allows adding metadata key value pairs to the Object.
            // If the Json is received from a previous version of BUO it may have metadata set in the object. Adding them to custom metadata for now.
    
            JSONObject pendingJson = jsonReader.getJsonObject();
            Iterator<String> keys = pendingJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                branchUniversalObject.metadata_.addCustomMetadata(key, pendingJson.optString(key));
            }
            
        } catch (Exception e) {
            BranchLogger.d(e.getMessage());
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
            buoJsonModel.put(Defines.Jsonkey.LocallyIndexable.getKey(), localIndexMode_.ordinal());
            buoJsonModel.put(Defines.Jsonkey.CreationTimestamp.getKey(), creationTimeStamp_);
            
        } catch (JSONException e) {
            BranchLogger.d(e.getMessage());
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
        @SuppressWarnings("unchecked")
        ArrayList<String> keywordsTemp = (ArrayList<String>) in.readSerializable();
        if (keywordsTemp != null) {
            keywords_.addAll(keywordsTemp);
        }
        metadata_ = in.readParcelable(ContentMetadata.class.getClassLoader());
        localIndexMode_ = CONTENT_INDEX_MODE.values()[in.readInt()];
    }
}
