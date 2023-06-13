package io.branch.saas.sdk.testbed.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.ProductCategory;
import io.branch.saas.sdk.testbed.Common;
import io.branch.saas.sdk.testbed.R;

public class MetadataActivity extends AppCompatActivity {
    private Spinner productCategory;
    private Spinner productCondition;
    private Spinner currencyType;
    private Spinner contentSchema;

    private String productCategoryStr, conditionStr, currencyStr, contentSchemaStr;

    private EditText etProductName, etProductBrand, etProductVariant, etStreetName, etCity,
            etRegion, etCountry, etPostalCode, etLongitude, etLatitude, etSku, etRating,
            etAvgRating, etMaxRating, etRatingCount,
            etImageCaption, etQuantity, etPrice, etCustomMetadata;

    private String productNameStr, productBrandStr, productVariantStr, streetNameStr, cityStr,
            regionStr, countryStr, postalCodeStr, longitudeStr, latitudeStr, skuStr, ratingStr,
            avgRatingStr, maxRatingStr, ratingCountStr,
            imageCaptionStr, quantityStr, priceStr, customMetadataStr;

    private Button submitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata);

        productCategory = findViewById(R.id.sp_product_category);
        productCondition = findViewById(R.id.sp_product_condition);
        currencyType = findViewById(R.id.sp_currency_type);
        contentSchema = findViewById(R.id.sp_content_schema);

        etProductName = findViewById(R.id.et_product_name);
        etProductBrand = findViewById(R.id.et_product_brand);
        etProductVariant = findViewById(R.id.et_product_variant);
        etStreetName = findViewById(R.id.et_street_name);
        etCity = findViewById(R.id.et_city);
        etRegion = findViewById(R.id.et_region_name);
        etCountry = findViewById(R.id.et_country_name);
        etPostalCode = findViewById(R.id.et_postal_code);
        etLongitude = findViewById(R.id.et_longitude);
        etLatitude = findViewById(R.id.et_latitude);
        etSku = findViewById(R.id.et_setSku);
        etRating = findViewById(R.id.et_rating);
        etAvgRating = findViewById(R.id.et_avg_rating);
        etMaxRating = findViewById(R.id.et_max_rating);
        etRatingCount = findViewById(R.id.et_rating_count);
        etImageCaption = findViewById(R.id.et_image_caption);
        etQuantity = findViewById(R.id.et_quantity);
        etPrice = findViewById(R.id.et_price);
        etCustomMetadata = findViewById(R.id.et_add_custom_metadata);

        submitBtn = findViewById(R.id.bt_submit);


        setSpinnerAdapter(R.array.product_category, productCategory, "productCategory");
        setSpinnerAdapter(R.array.product_condition, productCondition, "productCondition");
        setSpinnerAdapter(R.array.currency_type, currencyType, "currencyType");
        setSpinnerAdapter(R.array.content_schema, contentSchema, "contentSchema");


        submitBtn.setOnClickListener(view -> addMeta());


    }

    private void addMeta() {
        productNameStr = etProductName.getText().toString();
        productBrandStr = etProductBrand.getText().toString();
        productVariantStr = etProductVariant.getText().toString();
        streetNameStr = etStreetName.getText().toString();
        cityStr = etCity.getText().toString();
        regionStr = etRegion.getText().toString();
        countryStr = etCountry.getText().toString();
        postalCodeStr = etPostalCode.getText().toString();
        longitudeStr = etLongitude.getText().toString();
        latitudeStr = etLatitude.getText().toString();
        skuStr = etSku.getText().toString();
        ratingStr = etRating.getText().toString();
        avgRatingStr = etAvgRating.getText().toString();
        maxRatingStr = etMaxRating.getText().toString();
        ratingCountStr = etRatingCount.getText().toString();
        imageCaptionStr = etImageCaption.getText().toString();
        quantityStr = etQuantity.getText().toString();
        priceStr = etPrice.getText().toString();
        customMetadataStr = etCustomMetadata.getText().toString();
        Common.getInstance().contentMetadata = new ContentMetadata()
                .setProductName(productNameStr)
                .setProductBrand(productBrandStr)
                .setProductVariant(productVariantStr)
                .setProductCondition(TextUtils.isEmpty(conditionStr) ? null : ContentMetadata.CONDITION.valueOf(conditionStr))
                .setProductCategory(TextUtils.isEmpty(productCategoryStr) ? null : ProductCategory.valueOf(productCategoryStr))
                .setAddress(streetNameStr, cityStr, regionStr, countryStr, postalCodeStr)
                .setLocation(TextUtils.isEmpty(latitudeStr) ? null : Double.parseDouble(latitudeStr), TextUtils.isEmpty(longitudeStr) ? null : Double.parseDouble(longitudeStr))
                .setSku(skuStr)
                .setRating(TextUtils.isEmpty(ratingStr) ? null : Double.parseDouble(ratingStr),
                        TextUtils.isEmpty(avgRatingStr) ? null : Double.parseDouble(avgRatingStr),
                        TextUtils.isEmpty(maxRatingStr) ? null : Double.parseDouble(maxRatingStr),
                        TextUtils.isEmpty(ratingCountStr) ? null : Integer.parseInt(ratingCountStr))
                .addImageCaptions(imageCaptionStr)
                .setQuantity(TextUtils.isEmpty(quantityStr) ? null : Double.parseDouble(quantityStr))
                .setPrice(TextUtils.isEmpty(priceStr) ? null : Double.parseDouble(priceStr), CurrencyType.valueOf(currencyStr))
                .setContentSchema(TextUtils.isEmpty(contentSchemaStr) ? null : BranchContentSchema.valueOf(contentSchemaStr))
                .addCustomMetadata("Custom_Content_metadata_key1", customMetadataStr);
        finish();
    }

    private void setSpinnerAdapter(int arrayId, Spinner spinner, String type) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
                switch (type) {
                    case "productCategory":
                        productCategoryStr = spinner.getSelectedItem().toString();
                        productCategoryStr = productCategoryStr.replace("&amp;", "&");
                        break;
                    case "productCondition":
                        conditionStr = spinner.getSelectedItem().toString();
                        conditionStr = conditionStr.replace("&amp;", "&");
                        break;
                    case "currencyType":
                        currencyStr = spinner.getSelectedItem().toString();
                        currencyStr = currencyStr.replace("&amp;", "&");
                        break;
                    case "contentSchema":
                        contentSchemaStr = spinner.getSelectedItem().toString();
                        contentSchemaStr = contentSchemaStr.replace("&amp;", "&");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, arrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner.setAdapter(adapter);
    }


}