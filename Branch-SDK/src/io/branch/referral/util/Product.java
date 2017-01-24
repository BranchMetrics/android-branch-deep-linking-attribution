package io.branch.referral.util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Evan Groth on 12/21/16.
 */

public class Product {
    private String sku;
    private String name;
    private Double price;
    private Integer quantity;
    private String brand;
    private String variant;
    private ProductCategory category;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public Product() {
    }

    public Product(String sku, String name, Double price, int quantity, String brand, String variant, ProductCategory category) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.brand = brand;
        this.variant = variant;
        this.category = category;
    }

    public JSONObject getProductJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sku", this.sku);
            jsonObject.put("name", this.name);
            jsonObject.put("price", this.price);
            jsonObject.put("quantity", this.quantity);
            jsonObject.put("brand", this.brand);
            jsonObject.put("variant", this.variant);
            jsonObject.put("category", this.category);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
