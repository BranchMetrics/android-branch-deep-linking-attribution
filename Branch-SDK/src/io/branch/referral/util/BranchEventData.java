package io.branch.referral.util;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Defines;

/**
 * Created by sojanpr on 7/7/17.
 * <p>
 * Class for defining Branch standard event data. This class is used to provide event data to Branch Standard events
 * Check {@link TrackStandardEventBuilder#addEventData(BranchEventData)} method
 * </p>
 */
public class BranchEventData {
    private final String transactionID;
    private CurrencyType currency;
    private Double revenue;
    private Double shipping;
    private Double tax;
    private String coupon;
    private String affiliation;
    private String description;

    /**
     * Creates the Branch Event data object using the given transaction id.
     * Use the builder methods to set other optional parameters to BranchEventData
     *
     * @param transactionID {@link String} Transaction ID
     */
    public BranchEventData(String transactionID) {
        this.transactionID = transactionID;
        currency = null;
        revenue = null;
        shipping = null;
        tax = null;
        coupon = null;
        affiliation = null;
        description = null;
    }

    /**
     * Set the currency related with this transaction event
     *
     * @param currency iso4217Code for currency. Defined  in {@link CurrencyType}
     * @return This object for chaining builder methods
     */
    public BranchEventData setCurrency(CurrencyType currency) {
        this.currency = currency;
        return this;
    }

    /**
     * Set the revenue value  related with this transaction event
     *
     * @param revenue {@link double } revenue value
     * @return This object for chaining builder methods
     */
    public BranchEventData setRevenue(double revenue) {
        this.revenue = revenue;
        return this;
    }

    /**
     * Set the shipping value  related with this transaction event
     *
     * @param shipping {@link double } shipping value
     * @return This object for chaining builder methods
     */
    public BranchEventData setShipping(double shipping) {
        this.shipping = shipping;
        return this;
    }

    /**
     * Set the tax value  related with this transaction event
     *
     * @param tax {@link double } tax value
     * @return This object for chaining builder methods
     */
    public BranchEventData setTax(double tax) {
        this.tax = tax;
        return this;
    }

    /**
     * Set any coupons associated with this transaction event
     *
     * @param coupon {@link String } with any coupon value
     * @return This object for chaining builder methods
     */
    public BranchEventData setCoupon(String coupon) {
        this.coupon = coupon;
        return this;
    }

    /**
     * Set any affiliation for this transaction event
     *
     * @param affiliation {@link String } any affiliation value
     * @return This object for chaining builder methods
     */
    public BranchEventData setAffiliation(String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    /**
     * Set description for this transaction event
     *
     * @param description {@link String } transaction description
     * @return This object for chaining builder methods
     */
    public BranchEventData setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Converts BranchEventData to Json representation
     *
     * @return {@link JSONObject} representation of this object
     */
    JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Defines.Jsonkey.TransactionID.getKey(), transactionID);
            if (currency != null) {
                jsonObject.put(Defines.Jsonkey.Currency.getKey(), currency.toString());
            }
            if (revenue != null) {
                jsonObject.put(Defines.Jsonkey.Revenue.getKey(), revenue);
            }
            if (shipping != null) {
                jsonObject.put(Defines.Jsonkey.Shipping.getKey(), shipping);
            }
            if (tax != null) {
                jsonObject.put(Defines.Jsonkey.Tax.getKey(), tax);
            }
            if (coupon != null) {
                jsonObject.put(Defines.Jsonkey.Coupon.getKey(), coupon);
            }
            if (affiliation != null) {
                jsonObject.put(Defines.Jsonkey.Affiliation.getKey(), affiliation);
            }
            if (description != null) {
                jsonObject.put(Defines.Jsonkey.Description.getKey(), description);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}
