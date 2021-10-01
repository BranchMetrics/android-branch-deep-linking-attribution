package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BranchUtilTest {

    private final int[] resourceIds = new int[]{
            0,
            1,
            32,
            323429,
            Integer.MAX_VALUE,
            -1,
            -50,
            -476063,
            Integer.MIN_VALUE
    };

    private final String[] encodedIds = new String[]{
            "resourceID 0x0",
            "resourceID 0x1",
            "resourceID 0x20",
            "resourceID 0x4ef65",
            "resourceID 0x7fffffff",
            "resourceID 0xffffffff",
            "resourceID 0xffffffce",
            "resourceID 0xfff8bc61",
            "resourceID 0x80000000"
    };

    private final String[] replacePositiveJsonTests = new String[] {
            //positive test cases
            "{ \"field\": 1 }",
            "{ \"field\": \"two\" }",
            "{ \"field\": [] }",
            "{ \"field\": [ 1, 2, \"three\", 4.0 ] }",
            "{ \"field\": { \"field\": 5 } }",
    };

    private final String[] replaceNegativeJsonTests = new String[] {
            "{}",
            "{ \"replaced\": 0 }",
            "{ \"field\": 1, \"replaced\": 2 }",
            "{ \"field\": \"two\", \"replaced\": \"four\" }",
            "{ \"field\": [], \"replaced\": [] }",
            "{ \"field\": [ 1, 2, \"three\", 4.0 ], \"replaced\": [ 5, 6, \"seven\", 8.0 ] }",
            "{ \"field\": { \"field\": 5 }, \"replaced\": { \"field\": 6 } }"
    };

    @Test
    public void testEncodeResourceId() {
        for (int i = 0; i < resourceIds.length; i++) {
            Assert.assertEquals(encodedIds[i], BranchUtil.encodeResourceId(resourceIds[i]));
        }
    }

    @Test
    public void testDecodeResourceId() {
        Assert.assertEquals(resourceIds[6], BranchUtil.decodeResourceId(encodedIds[6]));
        for (int i = 0; i < encodedIds.length; i++) {
            Assert.assertEquals(resourceIds[i], BranchUtil.decodeResourceId(encodedIds[i]));
        }
    }

    @Test
    public void testReplaceJsonKey() {
        // Run all positive tests, verify that the old field is gone and new field is there and the value is retained
        for (String replacePositiveJsonTest : replacePositiveJsonTests) {
            try {
                JSONObject jsonObject = new JSONObject(replacePositiveJsonTest);
                Object value = jsonObject.opt("field");
                Assert.assertNotNull(value);
                Assert.assertNull(jsonObject.optJSONObject("replaced"));
                BranchUtil.replaceJsonKey(jsonObject, "field", "replaced");
                Assert.assertNull(jsonObject.opt("field"));
                Assert.assertEquals(value, jsonObject.opt("replaced"));
            } catch (JSONException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }

        // Run all negative tests, verify that the original json document is unchanged
        for (String replaceNegativeJsonTest : replaceNegativeJsonTests) {
            try {
                JSONObject jsonObject1 = new JSONObject(replaceNegativeJsonTest);
                JSONObject jsonObject2 = new JSONObject(replaceNegativeJsonTest);
                BranchUtil.replaceJsonKey(jsonObject2, "field", "replaced");
                Assert.assertEquals(jsonObject1.toString(), jsonObject2.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
    }
}
