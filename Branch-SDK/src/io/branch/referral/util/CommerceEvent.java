package io.branch.referral.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evan Groth on 12/21/16.
 */
public class CommerceEvent {
    public BigDecimal revenue;
    public String currency;
    public String transactionID;
    public BigDecimal shipping;
    public BigDecimal tax;
    public String coupon;
    public String affiliation;
    public List<Product> products;

    public CommerceEvent() {
        this.revenue = new BigDecimal(0);
        this.currency = "";
        this.transactionID = "";
        this.shipping = new BigDecimal(0);
        this.tax = new BigDecimal(0);
        this.coupon = "";
        this.affiliation = "";
        this.products = new ArrayList<>();
    }

    public CommerceEvent(BigDecimal revenue, String currency, String transactionID, BigDecimal shipping, BigDecimal tax, String coupon, String affiliation, List<Product> products) {
        this.revenue = revenue;
        this.currency = currency;
        this.transactionID = transactionID;
        this.shipping = shipping;
        this.tax = tax;
        this.coupon = coupon;
        this.affiliation = affiliation;
        this.products = products;
    }

    public CommerceEvent(BigDecimal revenue, String currency, String transactionID, BigDecimal shipping, BigDecimal tax, String coupon, String affiliation, Product product) {
        this.revenue = revenue;
        this.currency = currency;
        this.transactionID = transactionID;
        this.shipping = shipping;
        this.tax = tax;
        this.coupon = coupon;
        this.affiliation = affiliation;
        this.products = new ArrayList<>();
        this.products.add(product);
    }

    public JSONObject getCommerceJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("revenue", this.revenue);
            jsonObject.put("currency", this.currency);
            jsonObject.put("transactionID", this.transactionID);
            jsonObject.put("shipping", this.shipping);
            jsonObject.put("tax", this.tax);
            jsonObject.put("coupon", this.coupon);
            jsonObject.put("affiliation", this.affiliation);
            jsonObject.put("products", getProducts());
        } catch ( JSONException e ) {

        }

        return jsonObject;
    }

    public List<JSONObject> getProducts() {
        List<JSONObject> products = new ArrayList<>();
        for ( Product p : this.products ) {
            products.add(p.getProductJSONObject());
        }
        return products;
    }
}
