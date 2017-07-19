package io.branch.branchandroiddemo.test;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BranchContentSchema;
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
                    .addKeyWord("TestKeyword1")
                    .addKeyWord("Testkeyword2")
                    .addContentMetadata("Metadatakey1", "MetadataVal1")
                    .addContentMetadata("Metadatakey2", "MetadataVal2")
                    .addImageCaptions("img caption 1", "img caption 2")
                    .setCanonicalUrl("canonical/url")
                    .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
                    .setLocation(157.2, -97.2)
                    .setRunTime(1200)
                    .setContentType("my content type")
                    .setContentExpiration(new Date(4345343545555L))
                    .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                    .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                    .setPrice(101.20)
                    .setProductBrand("MyProdBrand")
                    .setProductCategory(ProductCategory.SPORTING_GOODS)
                    .setProductName("MyproductName")
                    .setProductVariant("prod-12")
                    .setQuantity(1D)
                    .setRatingCount(5)
                    .setMaximumRating(2.2)
                    .setAverageRating(4.2)
                    .setSku("1101123445")
                    .setTitle("my content title")
                    .setContentDescription("My content description")
                    .setAddress("2440 Ash Street", "Palo Alto", "CA", "USA", "95067");
            succeeded = doTestBUOSerialisation(testBuo);

        }
        if (succeeded) {
            Log.d("BranchTestBed", "Passed BUO serialisation - de-serialisation test.");
        }
        return succeeded;
    }

    private static boolean doTestBUOSerialisation(BranchUniversalObject buo) {
        boolean isPassed = false;
        JSONObject testBuoJson1 = buo.convertToJson();
        buo = BranchUniversalObject.createInstance(testBuoJson1);
        JSONObject testBuoJson2 = buo.convertToJson();

        if (testBuoJson1.length() == testBuoJson2.length()) {
            Iterator<String> keys = testBuoJson1.keys();
            try {
                while (keys.hasNext()) {
                    String currKey = keys.next();
                    if (testBuoJson1.get(currKey).equals(testBuoJson2.opt(currKey))) {
                        testBuoJson2.remove(currKey);
                    }
                }
                if (testBuoJson2.length() == 0) {
                    isPassed = true;
                } else {
                    Log.e("BranchTestBed", "Error Failed BUO serialisation - de-serialisation test. Unmatched keys in de-serialised version " + testBuoJson2.toString(4));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("BranchTestBed", "Error : BUO serialisation error.");
            }
        } else {
            Log.e("BranchTestBed", "Error : BUO serialisation error. Reason: Additional entries in de-serialised object");
        }
        return isPassed;
    }
}
