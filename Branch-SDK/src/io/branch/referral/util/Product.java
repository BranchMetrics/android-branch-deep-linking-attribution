package io.branch.referral.util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Evan Groth on 12/21/16.
 */

public class Product {
    public String sku;
    public String name;
    public Double price;
    public int quantity;
    public String brand;
    public String category;
    public String variant;
    public ProductCategory productCategory;

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
    }

    public Product() {
        this.sku = "";
        this.name = "";
        this.price = 0.0;
        this.quantity = 0;
        this.brand = "";
        this.category = "";
        this.variant = "";
        this.productCategory = null;
    }

    public Product(String sku, String name, Double price, int quantity, String brand, String variant, ProductCategory productCategory) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.brand = brand;
        this.variant = variant;
        this.productCategory = productCategory;
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
            jsonObject.put("category", this.productCategory);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
