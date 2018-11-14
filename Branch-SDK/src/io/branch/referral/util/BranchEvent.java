package io.branch.referral.util;


import android.content.Context;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

/**
 * <p>
 * Class for creating Branch events for tracking and analytical purpose.
 * This class represent a standard or custom BranchEvents. Standard Branch events are defined with {@link BRANCH_STANDARD_EVENT}.
 * Please use #logEvent() method to log the events for tracking.
 * </p>
 */
public class BranchEvent {
    private final String eventName;
    private final boolean isStandardEvent;
    private final JSONObject standardProperties;
    private final JSONObject customProperties;
    private final List<BranchUniversalObject> buoList;

    public BranchEvent(BRANCH_STANDARD_EVENT branchStandardEvent) {
        this(branchStandardEvent.getName(), true);
    }

    public BranchEvent(String eventName) {
        this(eventName, false);
    }

    private BranchEvent(String eventName, boolean isStandardEvent) {
        standardProperties = new JSONObject();
        customProperties = new JSONObject();
        this.eventName = eventName;

        this.isStandardEvent = isStandardEvent;
        buoList = new ArrayList<>();
    }

    /**
     * Set the transaction id associated with this event if there in any
     *
     * @param transactionID {@link String transactionID}
     */
    public BranchEvent setTransactionID(String transactionID) {
        return addStandardProperty(Defines.Jsonkey.TransactionID.getKey(), transactionID);
    }

    /**
     * Set the currency related with this transaction event
     *
     * @param currency iso4217Code for currency. Defined  in {@link CurrencyType}
     * @return This object for chaining builder methods
     */
    public BranchEvent setCurrency(CurrencyType currency) {
        return addStandardProperty(Defines.Jsonkey.Currency.getKey(), currency.toString());
    }

    /**
     * Set the revenue value  related with this transaction event
     *
     * @param revenue {@link double } revenue value
     * @return This object for chaining builder methods
     */
    public BranchEvent setRevenue(double revenue) {
        return addStandardProperty(Defines.Jsonkey.Revenue.getKey(), revenue);
    }

    /**
     * Set the shipping value  related with this transaction event
     *
     * @param shipping {@link double } shipping value
     * @return This object for chaining builder methods
     */
    public BranchEvent setShipping(double shipping) {
        return addStandardProperty(Defines.Jsonkey.Shipping.getKey(), shipping);
    }

    /**
     * Set the tax value  related with this transaction event
     *
     * @param tax {@link double } tax value
     * @return This object for chaining builder methods
     */
    public BranchEvent setTax(double tax) {
        return addStandardProperty(Defines.Jsonkey.Tax.getKey(), tax);
    }

    /**
     * Set any coupons associated with this transaction event
     *
     * @param coupon {@link String } with any coupon value
     * @return This object for chaining builder methods
     */
    public BranchEvent setCoupon(String coupon) {
        return addStandardProperty(Defines.Jsonkey.Coupon.getKey(), coupon);
    }

    /**
     * Set any affiliation for this transaction event
     *
     * @param affiliation {@link String } any affiliation value
     * @return This object for chaining builder methods
     */
    public BranchEvent setAffiliation(String affiliation) {
        return addStandardProperty(Defines.Jsonkey.Affiliation.getKey(), affiliation);
    }

    /**
     * Set description for this transaction event
     *
     * @param description {@link String } transaction description
     * @return This object for chaining builder methods
     */
    public BranchEvent setDescription(String description) {
        return addStandardProperty(Defines.Jsonkey.Description.getKey(), description);
    }

    /**
     * Set any search query associated with the event
     *
     * @param searchQuery {@link String} Search Query value
     * @return This object for chaining builder methods
     */
    public BranchEvent setSearchQuery(String searchQuery) {
        return addStandardProperty(Defines.Jsonkey.SearchQuery.getKey(), searchQuery);
    }

    /**
     * Set the User ID associated with the event.
     * @param userID {@link String} User ID value
     * @return this object for chaining builder methods
     */
    public BranchEvent setUserID(String userID) {
        return addStandardProperty(Defines.Jsonkey.UserID.getKey(), userID);
    }

    /**
     * Set the Facebook User ID associated with the event.
     * @param facebookUserID {@link String} Facebook User ID value
     * @return this object for chaining builder methods
     */
    public BranchEvent setFacebookUserID(String facebookUserID) {
        return addStandardProperty(Defines.Jsonkey.FacebookUserID.getKey(), facebookUserID);
    }

    /**
     * Set the Google User ID associated with the event.
     * @param googleUserID {@link String} Google User ID value
     * @return this object for chaining builder methods
     */
    public BranchEvent setGoogleUserID(String googleUserID) {
        return addStandardProperty(Defines.Jsonkey.GoogleUserID.getKey(), googleUserID);
    }

    /**
     * Set the Twitter User ID associated with the event.
     * @param twitterUserID {@link String} Twitter User ID value
     * @return this object for chaining builder methods
     */
    public BranchEvent setTwitterUserID(String twitterUserID) {
        return addStandardProperty(Defines.Jsonkey.TwitterUserID.getKey(), twitterUserID);
    }

    /**
     * Set the User Email Address associated with the event.
     * @param userEmail {@link String} User Email Address value
     * @return this object for chaining builder methods
     */
    public BranchEvent setUserEmail(String userEmail) {
        return addStandardProperty(Defines.Jsonkey.UserEmail.getKey(), userEmail);
    }

    /**
     * Set the User Name associated with the event.
     * @param userName {@link String} User Name value
     * @return this object for chaining builder methods
     */
    public BranchEvent setUserName(String userName) {
        return addStandardProperty(Defines.Jsonkey.UserName.getKey(), userName);
    }

    /**
     * Set the latitude associated with the event.
     * @param latitude {@link float} latitude value
     * @return this object for chaining builder methods
     */
    public BranchEvent setLatitude(float latitude) {
        return addStandardProperty(Defines.Jsonkey.LocationLatitude.getKey(), latitude);
    }

    /**
     * Set the longitude associated with the event.
     * @param longitude {@link float} longitude value
     * @return this object for chaining builder methods
     */
    public BranchEvent setLongitude(float longitude) {
        return addStandardProperty(Defines.Jsonkey.LocationLongitude.getKey(), longitude);
    }

    /**
     * Set the altitude associated with the event.
     * @param altitude {@link float} altitude value
     * @return this object for chaining builder methods
     */
    public BranchEvent setAltitude(float altitude) {
        return addStandardProperty(Defines.Jsonkey.LocationAltitude.getKey(), altitude);
    }

    /**
     * Set the location associated with the event.
     * @param location {@link Location} location value
     * @return this object for chaining builder methods
     */
    public BranchEvent setLocation(Location location) {
        float lat = (float) location.getLatitude();
        float lon = (float) location.getLongitude();
        float alt = (float) location.getAltitude();

        return setLatitude(lat).setLongitude(lon).setAltitude(alt);
    }

    /**
     * Set the Ad Type associated with the event.
     * @param adType {@link AdType} Ad Type value
     * @return this object for chaining builder methods
     */
    public BranchEvent setAdType(AdType adType) {
        return addStandardProperty(Defines.Jsonkey.AdType.getKey(), adType.getName());
    }

    private BranchEvent addStandardProperty(String propertyName, Object propertyValue) {
        if (propertyValue != null) {
            try {
                this.standardProperties.put(propertyName, propertyValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            this.standardProperties.remove(propertyName);
        }
        return this;
    }

    /**
     * Adds a custom data property associated with this Branch Event
     *
     * @param propertyName  {@link String} Name of the custom property
     * @param propertyValue {@link String} Value of the custom property
     * @return This object for chaining builder methods
     */
    public BranchEvent addCustomDataProperty(String propertyName, String propertyValue) {
        try {
            this.customProperties.put(propertyName, propertyValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Use this method to add any {@link BranchUniversalObject} associated with this event.
     *
     * @param contentItems BranchUniversalObjects associated with this event
     * @return his object for chaining builder methods
     * NOTE : Use this method when the event is a {@link BRANCH_STANDARD_EVENT}. BranchUniversalObjects will be ignored otherwise
     */
    public BranchEvent addContentItems(BranchUniversalObject... contentItems) {
        Collections.addAll(buoList, contentItems);
        return this;
    }

    /**
     * Use this method to add any {@link BranchUniversalObject} associated with this event.
     *
     * @param contentItems A list of BranchUniversalObjects associated with this event
     * @return his object for chaining builder methods
     * NOTE : Use this method when the event is a {@link BRANCH_STANDARD_EVENT}. BranchUniversalObjects will be ignored otherwise
     */
    public BranchEvent addContentItems(List<BranchUniversalObject> contentItems) {
        buoList.addAll(contentItems);
        return this;
    }

    /**
     * Logs this BranchEvent to Branch for tracking and analytics
     *
     * @param context Current context
     * @return {@code true} if the event is logged to Branch
     */
    public boolean logEvent(Context context) {
        boolean isReqQueued = false;
        String reqPath = isStandardEvent ? Defines.RequestPath.TrackStandardEvent.getPath() : Defines.RequestPath.TrackCustomEvent.getPath();
        if (Branch.getInstance() != null) {
            Branch.getInstance().handleNewRequest(new ServerRequestLogEvent(context, reqPath));
            isReqQueued = true;
        }
        return isReqQueued;
    }

    private class ServerRequestLogEvent extends ServerRequest {
        ServerRequestLogEvent(Context context, String requestPath) {
            super(context, requestPath);
            JSONObject reqBody = new JSONObject();
            try {
                reqBody.put(Defines.Jsonkey.Name.getKey(), eventName);
                if (customProperties.length() > 0) {
                    reqBody.put(Defines.Jsonkey.CustomData.getKey(), customProperties);
                }

                if (standardProperties.length() > 0) {
                    reqBody.put(Defines.Jsonkey.EventData.getKey(), standardProperties);
                }
                if (buoList.size() > 0) {
                    JSONArray contentItemsArray = new JSONArray();
                    reqBody.put(Defines.Jsonkey.ContentItems.getKey(), contentItemsArray);
                    for (BranchUniversalObject buo : buoList) {
                        contentItemsArray.put(buo.convertToJson());
                    }
                }
                setPost(reqBody);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            updateEnvironment(context, reqBody);
        }

        @Override
        public boolean handleErrors(Context context) {
            return false;
        }

        @Override
        public void onRequestSucceeded(ServerResponse response, Branch branch) {
        }

        @Override
        public void handleFailure(int statusCode, String causeMsg) {
        }

        @Override
        public boolean isGetRequest() {
            return false;
        }

        @Override
        public void clearCallbacks() {
        }

        @Override
        public BRANCH_API_VERSION getBranchRemoteAPIVersion() {
            return BRANCH_API_VERSION.V2; //This is a v2 event
        }

        @Override
        public boolean isGAdsParamsRequired() {
            return true;
        }
    
        @Override
        protected boolean shouldUpdateLimitFacebookTracking() {
            return true;
        }

        public boolean shouldRetryOnFail() {
            return true; // Branch event need to be retried on failure.
        }
    }


    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String VIEW = "View";
    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String ADD_TO_WISH_LIST = "Add to Wishlist";
    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String ADD_TO_CART = "Add to Cart";
    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String PURCHASE_STARTED = "Purchase Started";
    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String PURCHASED = "Purchased";
    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String SHARE_STARTED = "Share Started";
    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String SHARE_COMPLETED = "Share Completed";
    /**
     * @deprecated Please use #BranchEvent(BRANCH_STANDARD_EVENT) instead
     */
    public static final String CANONICAL_ID_LIST = "$canonical_identifier_list";
}
