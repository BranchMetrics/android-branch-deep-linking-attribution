package io.branch.referral.util;


import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final HashMap<String, Object> topLevelProperties;
    private final JSONObject standardProperties;
    private final JSONObject customProperties;
    private final List<BranchUniversalObject> buoList;

    /**
     * Constructor.
     * @param branchStandardEvent Branch Standard Event
     */
    public BranchEvent(BRANCH_STANDARD_EVENT branchStandardEvent) {
        this(branchStandardEvent.getName());
    }

    /**
     * Constructor.
     * This constructor can be used for free-form Events or Branch Standard Events.
     * Event names that match Standard Events will be treated as Standard Events.
     * @param eventName Event Name.
     */
    public BranchEvent(String eventName) {
        topLevelProperties = new HashMap<>();
        standardProperties = new JSONObject();
        customProperties = new JSONObject();
        this.eventName = eventName;

        boolean standardEvent = false;
        for (BRANCH_STANDARD_EVENT event : BRANCH_STANDARD_EVENT.values()) {
            if (eventName.equals(event.getName())) {
                standardEvent = true;
                break;
            }
        }

        this.isStandardEvent = standardEvent;
        buoList = new ArrayList<>();
    }

    /**
     * Set the Event Alias associated with the event.
     *
     * @param customerEventAlias {@link String customerEventAlias}
     */
    public BranchEvent setCustomerEventAlias(String customerEventAlias) {
        return addTopLevelProperty(Defines.Jsonkey.CustomerEventAlias.getKey(), customerEventAlias);
    }

    /**
     * Set the Ad Type associated with the event.
     * @param adType {@link AdType} Ad Type value
     * @return this object for chaining builder methods
     */
    public BranchEvent setAdType(AdType adType) {
        return addStandardProperty(Defines.Jsonkey.AdType.getKey(), adType.getName());
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

    private BranchEvent addTopLevelProperty(String propertyName, Object propertyValue) {
        if (!this.topLevelProperties.containsKey(propertyName)) {
            this.topLevelProperties.put(propertyName, propertyValue);
        } else {
            this.topLevelProperties.remove(propertyName);
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

    // Undocumented
    public String getEventName() {
        return eventName;
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

                if (topLevelProperties.size() > 0) {
                    for (Map.Entry<String, Object> entry : topLevelProperties.entrySet()) {
                        reqBody.put(entry.getKey(), entry.getValue());
                    }
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
        protected boolean shouldUpdateLimitFacebookTracking() {
            return true;
        }

        public boolean shouldRetryOnFail() {
            return true; // Branch event need to be retried on failure.
        }
    }
}
