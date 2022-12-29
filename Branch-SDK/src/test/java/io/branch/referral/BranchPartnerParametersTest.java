package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static io.branch.referral.Defines.Jsonkey.PartnerData;

@RunWith(JUnit4.class)
public class BranchPartnerParametersTest {
    
    BranchPartnerParameters partnerParams;

    @Before
    public void setUp() {
        partnerParams = new BranchPartnerParameters();
    }

    @After
    public void tearDown() {
        partnerParams.clearAllParameters();;
    }

    @Test public void testStringHexNull() {
        Assert.assertFalse(partnerParams.isHexadecimal(null));
    }

    @Test public void testStringHexEmpty() {
        Assert.assertTrue(partnerParams.isHexadecimal(""));
    }

    @Test public void testStringHexDash() {
        Assert.assertFalse(partnerParams.isHexadecimal("-1"));
    }

    @Test public void testStringHexDecimal() {
        Assert.assertFalse(partnerParams.isHexadecimal("1.0"));
    }

    @Test public void testStringHexFraction() {
        Assert.assertFalse(partnerParams.isHexadecimal("2/4"));
    }
    
    @Test public void testStringHexAt() {
        Assert.assertFalse(partnerParams.isHexadecimal("test@12345"));
    }

    @Test public void testStringHexUpperG() {
        Assert.assertFalse(partnerParams.isHexadecimal("0123456789ABCDEFG"));
    }

    @Test public void testStringHexLowerG() {
        Assert.assertFalse(partnerParams.isHexadecimal("0123456789abcdefg"));
    }

    @Test public void testStringHexUpperCase() {
        Assert.assertTrue(partnerParams.isHexadecimal("0123456789ABCDEF"));
    }

    @Test public void testStringHexLowerCase() {
        Assert.assertTrue(partnerParams.isHexadecimal("0123456789abcdef"));
    }

    @Test public void testSha256HashSanityCheckValueNull() {
        Assert.assertFalse(partnerParams.isSha256Hashed(null));
    }

    @Test public void testSha256HashSanityCheckValueEmpty() {
        Assert.assertFalse(partnerParams.isSha256Hashed(""));
    }

    @Test public void testSha256HashSanityCheckValueTooShort() {
        // 63 char string
        Assert.assertFalse(partnerParams.isSha256Hashed("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcde"));
    }

    @Test public void testSha256HashSanityCheckValueTooLong() {
        // 65 char string
        Assert.assertFalse(partnerParams.isSha256Hashed("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdeff"));
    }

    @Test public void testSha256HashSanityCheckValueLowerCase() {
        // 64 char string
        Assert.assertTrue(partnerParams.isSha256Hashed("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"));
    }

    @Test public void testSha256HashSanityCheckValueUpperCase() {
        // 64 char string
        Assert.assertTrue(partnerParams.isSha256Hashed("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"));
    }

    @Test public void testSha256HashSanityCheckValueMixedCase() {
        // 64 char string
        Assert.assertTrue(partnerParams.isSha256Hashed("0123456789ABCDEF0123456789ABCDEF1234567890abcdef1234567890abcdef"));
    }

    @Test public void testJsonEmpty() throws JSONException {
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterEmpty() throws JSONException {
        partnerParams.addFacebookParameter("em", "");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterShort() throws JSONException {
        partnerParams.addFacebookParameter("em", "0123456789ABCDEF0123456789ABCDEF1234567890abcdef1234567890abcde");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterPhoneNumberIsIgnored() throws JSONException {
        partnerParams.addFacebookParameter("ph", "1-555-555-5555");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterEmailIsIgnored() throws JSONException {
        partnerParams.addFacebookParameter("em", "test@branch.io");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterBase64EncodedIsIgnored() throws JSONException {
        partnerParams.addFacebookParameter("em", "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterHashedValue() throws JSONException {
        partnerParams.addFacebookParameter("em", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals(
                "{\"fb\":{\"em\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\"}}",
                body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterExample() throws JSONException {
        partnerParams.addFacebookParameter("em", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");
        partnerParams.addFacebookParameter("ph", "b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals(
                "{\"fb\":{\"ph\":\"b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b\",\"em\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\"}}",
                body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonSnapParameterExample() throws JSONException {
        partnerParams.addSnapParameter("hashed_email_address", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");
        partnerParams.addSnapParameter("hashed_phone_number", "b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals(
                "{\"snap\":{\"hashed_email_address\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\",\"hashed_phone_number\":\"b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b\"}}",
                body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonMultipleParameterExample() throws JSONException {
        partnerParams.addFacebookParameter("em", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");
        partnerParams.addFacebookParameter("ph", "b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b");
        partnerParams.addSnapParameter("hashed_email_address", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");
        partnerParams.addSnapParameter("hashed_phone_number", "b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b");
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals(
                "{\"fb\":{\"ph\":\"b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b\",\"em\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\"},\"snap\":{\"hashed_email_address\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\",\"hashed_phone_number\":\"b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b\"}}",
                body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }

    @Test public void testJsonFBParameterClear() throws JSONException {
        partnerParams.addFacebookParameter("em", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");
        partnerParams.addFacebookParameter("em", "b90598b67534f00b1e3e68e8006631a40d24fba37a3a34e2b84922f1f0b3b29b");
        partnerParams.clearAllParameters();
        JSONObject body = new JSONObject();
        PrefHelper.loadPartnerParams(body, partnerParams);

        JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
    }
}
