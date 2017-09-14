package io.branch.branchandroiddemo.test;

import android.os.Parcel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.ProductCategory;

/**
 * Created by sojanpr on 7/18/17.
 * <p>
 * Class  for testing BUO specific test functions
 * </p>
 */
public class BUOTestRoutines {
    public static boolean TestBUOSerialisation() {
        boolean succeeded = false;

        //Step 1 test buo with min params
        BranchUniversalObject testBuo = new BranchUniversalObject();
        testBuo.setCanonicalIdentifier("my_canonical_id");

        //Step 2 test buo with max params
        if (doTestBUOSerialisation(testBuo)) {
            testBuo = new BranchUniversalObject()
                    .setCanonicalIdentifier("myprod/1234")
                    .setCanonicalUrl("https://test_canonical_url")
                    .setContentDescription("est_content_description")
                    .setContentExpiration(new Date(122323432444L))
                    .setContentImageUrl("https://test_content_img_url")
                    .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                    .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                    .setTitle("test_title")
                    .setContentMetadata(
                            new ContentMetadata()
                                    .addCustomMetadata("custom_metadata_key1", "custom_metadata_val1")
                                    .addCustomMetadata("custom_metadata_key1", "custom_metadata_val1")
                                    .addImageCaptions("image_caption_1", "image_caption2", "image_caption3")
                                    .setAddress("Street_Name", "test city", "test_state", "test_country", "test_postal_code")
                                    .setRating(5.2, 6.0, 5)
                                    .setLocation(-151.67, -124.0)
                                    .setPrice(10.0, CurrencyType.USD)
                                    .setProductBrand("test_prod_brand")
                                    .setProductCategory(ProductCategory.APPAREL_AND_ACCESSORIES)
                                    .setProductName("test_prod_name")
                                    .setProductCondition(ContentMetadata.CONDITION.EXCELLENT)
                                    .setProductVariant("test_prod_variant")
                                    .setQuantity(1.5)
                                    .setSku("test_sku")
                                    .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
                    )
                    .addKeyWord("keyword1")
                    .addKeyWord("keyword2");
            succeeded = doTestBUOSerialisation(testBuo);
        }
        if (succeeded) {
            Log.d("BranchTestBed", "Passed BUO serialisation - de-serialisation test.");
        } else {
            Log.d("BranchTestBed", "Failed BUO serialisation - de-serialisation test.");
        }
        return succeeded;
    }

    private static boolean doTestBUOSerialisation(BranchUniversalObject buo) {
        boolean isPassed = false;
        JSONObject testBuoJson1 = buo.convertToJson();
        BranchUniversalObject buo2 = BranchUniversalObject.createInstance(testBuoJson1);
        JSONObject testBuoJson2 = buo2.convertToJson();

        Parcel parcel = Parcel.obtain();
        buo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        BranchUniversalObject buoCreatedFromParcel = (BranchUniversalObject) BranchUniversalObject.CREATOR.createFromParcel(parcel);

        return (checkIfIdenticalJson(buo.getContentMetadata().convertToJson(), buo2.getContentMetadata().convertToJson()))
                && (checkIfIdenticalJson(testBuoJson1, testBuoJson2))
                && (checkIfIdenticalJson(testBuoJson1, buoCreatedFromParcel.convertToJson()));
    }

    private static boolean checkIfIdenticalJson(JSONObject obj1, JSONObject obj2) {
        boolean isIdentical = false;
        if (obj1.length() == obj2.length()) {
            Iterator<String> keys = obj1.keys();
            try {
                while (keys.hasNext()) {
                    String currKey = keys.next();
                    if (obj1.get(currKey).equals(obj2.opt(currKey))) {
                        obj2.remove(currKey);
                    }
                }
                if (obj2.length() == 0) {
                    isIdentical = true;
                } else {
                    Log.e("BranchTestBed", "Error : BUO serialisation error. Reason: Additional entries in de-serialised object " + obj2);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("BranchTestBed", "Error : BUO serialisation error.");
            }
        } else {
            Log.e("BranchTestBed", "Serialised versions buo versions are not matching \n" + obj1 + "\n\n" + obj2);
        }

        return isIdentical;
    }

}
