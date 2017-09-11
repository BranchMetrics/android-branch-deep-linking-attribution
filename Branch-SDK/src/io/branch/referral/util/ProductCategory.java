package io.branch.referral.util;

import android.text.TextUtils;

/**
 * Created by Evan on 1/4/17.
 * <p>
 * Enumeration for product categories to be used with Branch commercial events
 * </p>
 */

public enum ProductCategory {
    ANIMALS_AND_PET_SUPPLIES("Animals & Pet Supplies"),
    APPAREL_AND_ACCESSORIES("Apparel & Accessories"),
    ARTS_AND_ENTERTAINMENT("Arts & Entertainment"),
    BABY_AND_TODDLER("Baby & Toddler"),
    BUSINESS_AND_INDUSTRIAL("Business & Industrial"),
    CAMERA_AND_OPTICS("Cameras & Optics"),
    ELECTRONICS("Electronics"),
    FOOD_BEVERAGE_AND_TOBACCO("Food, Beverages & Tobacco"),
    FURNITURE("Furniture"),
    HARDWARE("Hardware"),
    HEALTH_AND_BEAUTY("Health & Beauty"),
    HOME_AND_GARDEN("Home & Garden"),
    LUGGAGE_AND_BAGS("Luggage & Bags"),
    MATURE("Mature"),
    MEDIA("Media"),
    OFFICE_SUPPLIES("Office Supplies"),
    RELIGIOUS_AND_CEREMONIAL("Religious & Ceremonial"),
    SOFTWARE("Software"),
    SPORTING_GOODS("Sporting Goods"),
    TOYS_AND_GAMES("Toys & Games"),
    VEHICLES_AND_PARTS("Vehicles & Parts");

    private String name;

    ProductCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ProductCategory getValue(String name) {
        ProductCategory productCategoryResult = null;
        if (!TextUtils.isEmpty(name)) {
            for (ProductCategory productCategory : ProductCategory.values()) {
                if (productCategory.name.equalsIgnoreCase(name)) {
                    productCategoryResult = productCategory;
                    break;
                }
            }
        }
        return productCategoryResult;
    }
}
