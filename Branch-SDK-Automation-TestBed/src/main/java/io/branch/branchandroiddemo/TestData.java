package io.branch.branchandroiddemo;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Calendar;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ProductCategory;

public class TestData {

    public BranchUniversalObject getParamBUOObject(String testData){
        BranchUniversalObject buoObject = null;
        try {
            JSONObject testDataObj = null;

            testDataObj = (JSONObject) new JSONParser().parse(testData);

            if (testDataObj.containsKey("BUOData")){
                JSONObject j = (JSONObject) testDataObj.get("BUOData");

                ContentMetadata contentMetadata = null;
                Gson gson = new Gson();
                BUOData buoData = gson.fromJson(j.toString(), BUOData.class);

                if (buoData != null) {
                    contentMetadata = new ContentMetadata()
                            .setProductName(buoData.productName)
                            .setProductBrand(buoData.productBrand)
                            .setProductVariant(buoData.productVariant)
                            .setProductCondition(TextUtils.isEmpty(buoData.productCondition) ? null : ContentMetadata.CONDITION.valueOf(buoData.productCondition))
                            .setProductCategory(TextUtils.isEmpty(buoData.productCategory) ? null : ProductCategory.valueOf(buoData.productCategory))
                            .setAddress(buoData.street, buoData.city, buoData.region, buoData.country, buoData.postalCode)
                            .setLocation(TextUtils.isEmpty(buoData.latitude) ? null : Double.parseDouble(buoData.latitude), TextUtils.isEmpty(buoData.longitude) ? null : Double.parseDouble(buoData.longitude))
                            .setSku(buoData.sku)
                            .setRating(TextUtils.isEmpty(buoData.rating) ? null : Double.parseDouble(buoData.rating),
                                    TextUtils.isEmpty(buoData.averageRating) ? null : Double.parseDouble(buoData.averageRating),
                                    TextUtils.isEmpty(buoData.maximumRating) ? null : Double.parseDouble(buoData.maximumRating),
                                    TextUtils.isEmpty(buoData.ratingCount) ? null : Integer.parseInt(buoData.ratingCount))
                            .addImageCaptions(buoData.imageCaption)
                            .setQuantity(TextUtils.isEmpty(buoData.quantity) ? null : Double.parseDouble(buoData.quantity))
                            .setPrice(TextUtils.isEmpty(buoData.price) ? null : Double.parseDouble(buoData.price), TextUtils.isEmpty(buoData.currencyType) ? CurrencyType.USD : CurrencyType.valueOf(buoData.currencyType))
                            .setContentSchema(TextUtils.isEmpty(buoData.contentSchema) ? null : BranchContentSchema.valueOf(buoData.contentSchema))
                            .addCustomMetadata("Custom_Content_metadata_key1", buoData.customMetadata);

                    buoObject = new BranchUniversalObject()
                            .setCanonicalIdentifier(buoData.canonicalIdentifier)
                            .setTitle(buoData.contentTitle)
                            .setContentDescription(buoData.contentDesc)
                            .setContentImageUrl(buoData.imageUrl)
                            
                            .setContentMetadata(contentMetadata);
                }
            }
            Log.i("BRANCH SDK error", "No BUO");
        } catch (ParseException e) {
            System.out.println("position: " + e.getPosition());
            System.out.println(e);
        }
        return buoObject;
    }
    public LinkProperties getParamLinkPropertiesObject(String testData){
        LinkProperties linkProperties = null;
        try {
            JSONObject testDataObj = null;
            testDataObj = (JSONObject) new JSONParser().parse(testData);
            if (testDataObj.containsKey("CreateLinkReference")) {
                JSONObject lpJSONObj = (JSONObject) testDataObj.get("CreateLinkReference");
                Gson gson = new Gson();
                LinkPropertiesData lpData = gson.fromJson(lpJSONObj.toString(), LinkPropertiesData.class);
                if (lpData != null) {
                    linkProperties = new LinkProperties()
                            .setChannel(TextUtils.isEmpty(lpData.channelName) ? null : lpData.channelName)
                            .setFeature(TextUtils.isEmpty(lpData.feature) ? null : lpData.feature)
                            .setCampaign(TextUtils.isEmpty(lpData.campaign) ? null : lpData.campaign)
                            .setStage(TextUtils.isEmpty(lpData.stage) ? null : lpData.stage)
                            .addControlParameter("$desktop_url", TextUtils.isEmpty(lpData.desktopUrl) ? null : lpData.desktopUrl)
                            .addControlParameter("$android_url", TextUtils.isEmpty(lpData.androidUrl) ? null : lpData.androidUrl)
                            .addControlParameter("$fallback_url", TextUtils.isEmpty(lpData.androidUrl) ? null : lpData.androidUrl)
                            .addControlParameter("custom", TextUtils.isEmpty(lpData.additionalData) ? null : lpData.additionalData)
                            .addControlParameter("$ios_url", TextUtils.isEmpty(lpData.iOSUrl) ? null : lpData.iOSUrl)
                            .addControlParameter("custom_random", Long.toString(Calendar.getInstance().getTimeInMillis()));
                }
            }
        } catch (ParseException pe) {
            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        } catch (Exception e) {
            Log.i("BRANCH SDK error", e.toString());
        }

        return linkProperties;
    }

    public BranchEvent getBranchEventObject(String testData){
        BranchEvent branchEvent = null;
        try {
            JSONObject testDataObj = null;
            testDataObj = (JSONObject) new JSONParser().parse(testData);
            if (testDataObj.containsKey("TrackContentData")) {
                JSONObject evJSONObj = (JSONObject) testDataObj.get("TrackContentData");
                Gson gson = new Gson();
                BranchEventData eventData = gson.fromJson(evJSONObj.toString(), BranchEventData.class);
                if (eventData != null) {
                    branchEvent = new BranchEvent(BRANCH_STANDARD_EVENT.valueOf(TextUtils.isEmpty(eventData.event) ? BRANCH_STANDARD_EVENT.ADD_TO_CART.toString() : eventData.event));
                }
            }
        } catch (ParseException pe) {
            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        } catch (Exception e) {
            Log.i("BRANCH SDK error", e.toString());
        }

        return branchEvent;
    }

    public String getUserName(String testData){
        try {
            JSONObject testDataObj = null;
            testDataObj = (JSONObject) new JSONParser().parse(testData);
            if (testDataObj.containsKey("UserName")) {
                String username = (String) testDataObj.get("UserName");
                return username;
            }
        } catch (ParseException pe) {
            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        } catch (Exception e) {
            Log.i("BRANCH SDK error", e.toString());
        }
        return null;
    }

    public String getParamValue(String testData, String key){
        try {
            JSONObject testDataObj = null;
            testDataObj = (JSONObject) new JSONParser().parse(testData);
            if (testDataObj.containsKey(key)) {
                String value = (String) testDataObj.get(key);
                return value;
            }
        } catch (ParseException pe) {
            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        } catch (Exception e) {
            Log.i("BRANCH SDK error", e.toString());
        }
        return null;
    }

    public Boolean getBoolParamValue(String testData, String key){
        try {
            JSONObject testDataObj = null;
            testDataObj = (JSONObject) new JSONParser().parse(testData);
            if (testDataObj.containsKey(key)) {
                Boolean boolValue = (Boolean) testDataObj.get(key);
                return boolValue;
            }
        } catch (ParseException pe) {
            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        } catch (Exception e) {
            Log.i("BRANCH SDK error", e.toString());
        }
        return false;
    }
}
