package io.branch.referral.util;


import android.content.Context;

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

    public enum CONDITION {OTHER, NEW, GOOD, FAIR, POOR, USED, REFURBISHED}

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
     * Set the condition of the content item. Value is one of the enum constants from {@link BranchEvent.CONDITION}
     *
     * @param condition {@link BranchEvent.CONDITION}
     * @return This object for chaining builder methods
     */
    public BranchEvent setCondition(CONDITION condition) {
        return addStandardProperty(Defines.Jsonkey.Condition.getKey(), condition.name());
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
     * Adds a custom property associated with this Branch Event
     *
     * @param propertyName  {@link String} Name of the custom property
     * @param propertyValue {@link String} Value of the custom property
     * @return This object for chaining builder methods
     */
    public BranchEvent addCustomProperty(String propertyName, String propertyValue) {
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
        public ServerRequestLogEvent(Context context, String requestPath) {
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
                if (isStandardEvent && buoList.size() > 0) {
                    JSONArray contentItemsArray = new JSONArray();
                    reqBody.put(Defines.Jsonkey.ContentItems.getKey(), contentItemsArray);
                    for (BranchUniversalObject buo : buoList) {
                        contentItemsArray.put(buo.convertToJson());
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            setPost(reqBody);
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
