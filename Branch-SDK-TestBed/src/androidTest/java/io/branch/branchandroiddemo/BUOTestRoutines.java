package io.branch.branchandroiddemo;

import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.BranchAsyncTask;
import io.branch.referral.BranchUtil;
import io.branch.referral.PrefHelper;
import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ProductCategory;

/**
 * Created by sojanpr on 7/18/17.
 * <p>
 * Class  for testing BUO specific test functions
 * </p>
 */
public class BUOTestRoutines {
    public static boolean TestBUOFunctionalities(Context context) {
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
            succeeded = doTestBUOSerialisation(testBuo) && doLinkCreationTest(context, testBuo);
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

        return (checkIfIdenticalJson(buo.getContentMetadata().convertToJson(), buo2.getContentMetadata().convertToJson(), false))
                && (checkIfIdenticalJson(testBuoJson1, testBuoJson2, false))
                && (checkIfIdenticalJson(testBuoJson1, buoCreatedFromParcel.convertToJson(), false));
    }

    private static boolean doLinkCreationTest(Context context, BranchUniversalObject buo) {
        boolean isLinkTestPassed = false;
        String url = buo.getShortUrl(context, new LinkProperties());
        try {
            JSONObject linkdata = new URLContentViewer().execute(url, PrefHelper.getInstance(context).readBranchKey(!BranchUtil.isTestModeEnabled(context))).get();
            isLinkTestPassed = checkIfIdenticalJson(buo.convertToJson(), linkdata.optJSONObject("data"), true);
            if (isLinkTestPassed) {
                isLinkTestPassed = checkIfIdenticalJson(BranchUniversalObject.createInstance(linkdata).convertToJson(), linkdata.optJSONObject("data"), true);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return isLinkTestPassed;

    }

    private static boolean checkIfIdenticalJson(JSONObject obj1, JSONObject obj2, boolean expectBranchExtras) {
        boolean isIdentical = false;
        if (obj1.length() == obj2.length() || expectBranchExtras) {
            try {
                Iterator<String> keys = obj1.keys();
                while (keys.hasNext()) {
                    String currKey = keys.next();
                    if (obj1.get(currKey).equals(obj2.opt(currKey))) {
                        obj2.remove(currKey);
                    }
                }

                Iterator<String> obj2Keys = obj2.keys();

                if (expectBranchExtras) {
                    isIdentical = true;
                    while (obj2Keys.hasNext()) {
                        String currKey = obj2Keys.next();
                        if (!(currKey.startsWith("~")
                                || currKey.startsWith("+")
                                || currKey.startsWith("$")
                                || currKey.equals("url")
                                || currKey.equals("source"))) {
                            isIdentical = false;
                            break;
                        }
                    }
                } else {
                    isIdentical = obj2.length() == 0;
                }
                if (!isIdentical) {
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

    private static class URLContentViewer extends BranchAsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... strings) {
            HttpsURLConnection connection = null;
            JSONObject respObject = new JSONObject();
            try {
                URL urlObject = new URL("https://api.branch.io/v1/url?url=" + strings[0] + "&" + "branch_key=" + strings[1]);
                connection = (HttpsURLConnection) urlObject.openConnection();
                connection.setConnectTimeout(1500);
                connection.setReadTimeout(1500);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    if (connection.getInputStream() != null) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        respObject = new JSONObject(rd.readLine());
                    }
                }
            } catch (Throwable ignore) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return respObject;
        }
    }

}
