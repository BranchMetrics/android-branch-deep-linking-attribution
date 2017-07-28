package io.branch.referral.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import io.branch.referral.BranchUtil;
import io.branch.referral.Defines;

/**
 * Created by sojanpr on 7/25/17.
 * <p>
 * Class for describing metadata for a piece of content represented by a {@link io.branch.indexing.BranchUniversalObject}
 * </p>
 */
public class ContentMetadata implements Parcelable {
    /**
     * Schema for the qualifying content item. Please see {@link BranchContentSchema}
     */
    BranchContentSchema contentSchema;
    /**
     * Quantity of the thing associated with the qualifying content item
     */
    public Double quantity;
    /**
     * Any price associated with the qualifying content item
     */
    public Double price;
    /**
     * Currency type associated with the price
     */
    public CurrencyType currencyType;
    /**
     * Holds any associated store keeping unit
     */
    public String sku;
    /**
     * Name of any product specified by this metadata
     */
    public String productName;
    /**
     * Any brand name associated with this metadata
     */
    public String productBrand;
    /**
     * Category of product if this metadata is for a product
     * Value should be one of the enumeration from {@link ProductCategory}
     */
    public ProductCategory productCategory;
    /**
     * Variant of product if this metadata is for a product
     */
    public String productVariant;
    /**
     * Average rating for the qualifying content item
     */
    public Double averageRating;
    /**
     * Total number of ratings for the qualifying content item
     */
    public Integer ratingCount;
    /**
     * Maximum ratings for the qualifying content item
     */
    public Double maximumRating;
    /**
     * Street address associated with the qualifying content item
     */
    public String addressStreet;
    /**
     * City name associated with the qualifying content item
     */
    private String addressCity;
    /**
     * Region or province name associated with the qualifying content item
     */
    public String addressRegion;
    /**
     * Country name associated with the qualifying content item
     */
    public String addressCountry;
    /**
     * Postal code associated with the qualifying content item
     */
    public String addressPostalCode;
    /**
     * Latitude value  associated with the qualifying content item
     */
    public Double latitude;
    /**
     * Latitude value  associated with the qualifying content item
     */
    public Double longitude;

    private final ArrayList<String> imageCaptions;
    private final HashMap<String, String> customMetadata;

    public ContentMetadata() {
        imageCaptions = new ArrayList<>();
        customMetadata = new HashMap<>();
    }

    /**
     * Adds any image captions associated with the qualifying content item
     *
     * @param captions {@link String} image captions
     * @return {@link ContentMetadata} object for method chaining
     */
    public ContentMetadata addImageCaptions(String... captions) {
        Collections.addAll(imageCaptions, captions);
        return this;
    }

    /**
     * Adds any custom metadata associated with the qualifying content item
     *
     * @param key   Name of the custom data
     * @param value Value for the custom data
     * @return {@link ContentMetadata} object for method chaining
     */
    public ContentMetadata addCustomMetadata(String key, String value) {
        customMetadata.put(key, value);
        return this;
    }

    public ContentMetadata setContentSchema(BranchContentSchema contentSchema) {
        this.contentSchema = contentSchema;
        return this;
    }

    public ContentMetadata setQuantity(Double quantity) {
        this.quantity = quantity;
        return this;
    }

    public ContentMetadata setAddress(@Nullable String street, @Nullable String city, @Nullable String region, @Nullable String country, @Nullable String postalCode) {
        this.addressStreet = street;
        this.addressCity = city;
        this.addressRegion = region;
        this.addressCountry = country;
        this.addressPostalCode = postalCode;
        return this;
    }

    public ContentMetadata setLocation(@Nullable Double latitude, @Nullable Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        return this;
    }

    public ContentMetadata setRating(@Nullable Double averageRating, @Nullable Double maximumRating, @Nullable Integer ratingCount) {
        this.averageRating = averageRating;
        this.maximumRating = maximumRating;
        this.ratingCount = ratingCount;
        return this;
    }

    public ContentMetadata setPrice(Double price, @Nullable CurrencyType currencyType) {
        this.price = price;
        this.currencyType = currencyType;
        return this;
    }

    public ContentMetadata setProductBrand(String productBrand) {
        this.productBrand = productBrand;
        return this;
    }

    public ContentMetadata setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
        return this;
    }

    public ContentMetadata setProductName(String productName) {
        this.productName = productName;
        return this;
    }

    public ContentMetadata setProductVariant(String productVariant) {
        this.productVariant = productVariant;
        return this;
    }

    public ContentMetadata setSku(String sku) {
        this.sku = sku;
        return this;
    }


    /**
     * Gets the list of Image Captions
     *
     * @return {@link ArrayList<String>} containing the collection of image captions
     */
    public ArrayList<String> getImageCaptions() {
        return imageCaptions;
    }

    /**
     * Returns a Map of custom metadata associated with the qualifying content item
     *
     * @return {@link HashMap custom metadata keys and values
     */
    public HashMap<String, String> getCustomMetadata() {
        return customMetadata;
    }

    //---- Serialize / de-serialize methods -------------------------//

    public JSONObject convertToJson() {
        JSONObject metadataJson = new JSONObject();
        try {
            if (contentSchema != null) {
                metadataJson.put(Defines.Jsonkey.ContentSchema.getKey(), contentSchema.name());
            }
            if (quantity != null) {
                metadataJson.put(Defines.Jsonkey.Quantity.getKey(), quantity);
            }
            if (price != null) {
                metadataJson.put(Defines.Jsonkey.Price.getKey(), price);
            }
            if (currencyType != null) {
                metadataJson.put(Defines.Jsonkey.PriceCurrency.getKey(), currencyType.toString());
            }
            if (!TextUtils.isEmpty(sku)) {
                metadataJson.put(Defines.Jsonkey.SKU.getKey(), sku);
            }
            if (!TextUtils.isEmpty(productName)) {
                metadataJson.put(Defines.Jsonkey.ProductName.getKey(), productName);
            }
            if (!TextUtils.isEmpty(productBrand)) {
                metadataJson.put(Defines.Jsonkey.ProductBrand.getKey(), productBrand);
            }
            if (productCategory != null) {
                metadataJson.put(Defines.Jsonkey.ProductCategory.getKey(), productCategory.getName());
            }
            if (!TextUtils.isEmpty(productVariant)) {
                metadataJson.put(Defines.Jsonkey.ProductVariant.getKey(), productVariant);
            }
            if (averageRating != null) {
                metadataJson.put(Defines.Jsonkey.RatingAverage.getKey(), averageRating);
            }
            if (ratingCount != null) {
                metadataJson.put(Defines.Jsonkey.RatingCount.getKey(), ratingCount);
            }
            if (maximumRating != null) {
                metadataJson.put(Defines.Jsonkey.RatingMax.getKey(), maximumRating);
            }
            if (!TextUtils.isEmpty(addressStreet)) {
                metadataJson.put(Defines.Jsonkey.AddressStreet.getKey(), addressStreet);
            }
            if (!TextUtils.isEmpty(addressCity)) {
                metadataJson.put(Defines.Jsonkey.AddressCity.getKey(), addressCity);
            }
            if (!TextUtils.isEmpty(addressRegion)) {
                metadataJson.put(Defines.Jsonkey.AddressRegion.getKey(), addressRegion);
            }
            if (!TextUtils.isEmpty(addressCountry)) {
                metadataJson.put(Defines.Jsonkey.AddressCountry.getKey(), addressCountry);
            }
            if (!TextUtils.isEmpty(addressPostalCode)) {
                metadataJson.put(Defines.Jsonkey.AddressPostalCode.getKey(), addressPostalCode);
            }
            if (latitude != null) {
                metadataJson.put(Defines.Jsonkey.Latitude.getKey(), latitude);
            }
            if (longitude != null) {
                metadataJson.put(Defines.Jsonkey.Longitude.getKey(), longitude);
            }
            if (imageCaptions.size() > 0) {
                JSONArray imageCaptionsArray = new JSONArray();
                metadataJson.put(Defines.Jsonkey.ImageCaptions.getKey(), imageCaptionsArray);
                for (String caption : imageCaptions) {
                    imageCaptionsArray.put(caption);
                }
            }
            if (customMetadata.size() > 0) {
                JSONObject customFieldsObject = new JSONObject();
                for (String customDataKey : customMetadata.keySet()) {
                    customFieldsObject.put(customDataKey, customMetadata.get(customDataKey));
                }
                metadataJson.put(Defines.Jsonkey.BUOCustomFields.getKey(), customFieldsObject.toString());
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return metadataJson;
    }

    public static ContentMetadata createFromJson(BranchUtil.JsonReader jsonReader) {
        ContentMetadata contentMetadata = new ContentMetadata();
        contentMetadata.contentSchema = BranchContentSchema.getValue(jsonReader.readOutString(Defines.Jsonkey.ContentSchema.getKey()));
        contentMetadata.quantity = jsonReader.readOutDouble(Defines.Jsonkey.Quantity.getKey(), null);
        contentMetadata.price = jsonReader.readOutDouble(Defines.Jsonkey.Price.getKey(), null);
        contentMetadata.currencyType = CurrencyType.getValue(jsonReader.readOutString(Defines.Jsonkey.PriceCurrency.getKey()));
        contentMetadata.sku = jsonReader.readOutString(Defines.Jsonkey.SKU.getKey());
        contentMetadata.productName = jsonReader.readOutString(Defines.Jsonkey.ProductName.getKey());
        contentMetadata.productBrand = jsonReader.readOutString(Defines.Jsonkey.ProductBrand.getKey());
        contentMetadata.productCategory = ProductCategory.getValue(jsonReader.readOutString(Defines.Jsonkey.ProductCategory.getKey()));
        contentMetadata.productVariant = jsonReader.readOutString(Defines.Jsonkey.ProductVariant.getKey());
        contentMetadata.averageRating = jsonReader.readOutDouble(Defines.Jsonkey.RatingAverage.getKey(), null);
        contentMetadata.ratingCount = jsonReader.readOutInt(Defines.Jsonkey.RatingCount.getKey(), null);
        contentMetadata.maximumRating = jsonReader.readOutDouble(Defines.Jsonkey.RatingMax.getKey(), null);
        contentMetadata.addressStreet = jsonReader.readOutString(Defines.Jsonkey.AddressStreet.getKey());
        contentMetadata.addressCity = jsonReader.readOutString(Defines.Jsonkey.AddressCity.getKey());
        contentMetadata.addressRegion = jsonReader.readOutString(Defines.Jsonkey.AddressRegion.getKey());
        contentMetadata.addressCountry = jsonReader.readOutString(Defines.Jsonkey.AddressCountry.getKey());
        contentMetadata.addressPostalCode = jsonReader.readOutString(Defines.Jsonkey.AddressPostalCode.getKey());
        contentMetadata.latitude = jsonReader.readOutDouble(Defines.Jsonkey.Latitude.getKey());
        contentMetadata.longitude = jsonReader.readOutDouble(Defines.Jsonkey.Longitude.getKey());
        JSONArray imageCaptionJsonArray = jsonReader.readOutJsonArray(Defines.Jsonkey.ImageCaptions.getKey());
        if (imageCaptionJsonArray != null) {
            for (int i = 0; i < imageCaptionJsonArray.length(); i++) {
                contentMetadata.imageCaptions.add(imageCaptionJsonArray.optString(i));
            }
        }
        try {
            JSONObject customFieldsObj = new JSONObject(jsonReader.readOutString(Defines.Jsonkey.BUOCustomFields.getKey(), new JSONObject().toString()));
            Iterator<String> keys = customFieldsObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                contentMetadata.customMetadata.put(key, customFieldsObj.optString(key));
            }

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return contentMetadata;
    }


    //---------------------Marshaling and Unmarshaling----------//
    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ContentMetadata createFromParcel(Parcel in) {
            return new ContentMetadata(in);
        }

        public ContentMetadata[] newArray(int size) {
            return new ContentMetadata[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(contentSchema != null ? contentSchema.name() : "");
        dest.writeDouble(quantity);
        dest.writeDouble(price);
        dest.writeString(currencyType != null ? currencyType.name() : "");
        dest.writeString(sku);
        dest.writeString(productName);
        dest.writeString(productBrand);
        dest.writeString(productCategory != null ? productCategory.name() : "");
        dest.writeString(productVariant);
        dest.writeDouble(averageRating);
        dest.writeInt(ratingCount);
        dest.writeDouble(maximumRating);

        dest.writeString(addressStreet);
        dest.writeString(addressCity);
        dest.writeString(addressRegion);
        dest.writeString(addressCountry);
        dest.writeString(addressPostalCode);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);

        dest.writeSerializable(imageCaptions);
        dest.writeSerializable(customMetadata);
    }

    private ContentMetadata(Parcel in) {
        this();
        contentSchema = BranchContentSchema.getValue(in.readString());
        quantity = in.readDouble();
        price = in.readDouble();
        currencyType = CurrencyType.getValue(in.readString());
        sku = in.readString();
        productName = in.readString();
        productBrand = in.readString();
        productCategory = ProductCategory.getValue(in.readString());
        productVariant = in.readString();
        averageRating = in.readDouble();
        ratingCount = in.readInt();
        maximumRating = in.readDouble();

        addressStreet = in.readString();
        addressCity = in.readString();
        addressRegion = in.readString();
        addressCountry = in.readString();
        addressPostalCode = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        @SuppressWarnings("unchecked")
        ArrayList<String> imageCaptionsTemp = (ArrayList<String>) in.readSerializable();
        imageCaptions.addAll(imageCaptionsTemp);
        @SuppressWarnings("unchecked")
        HashMap<String, String> tempCustomMetadata = (HashMap<String, String>) in.readSerializable();
        customMetadata.putAll(tempCustomMetadata);
    }

}
