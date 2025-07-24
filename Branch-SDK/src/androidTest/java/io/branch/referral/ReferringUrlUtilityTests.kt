package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class ReferringUrlUtilityTests : BranchTest() {

    private lateinit var referringUrlUtility: ReferringUrlUtility

    private fun openServerRequest(): ServerRequest {
        val jsonString = "{\"REQ_POST\":{\"randomized_device_token\":\"1144756305514505535\",\"randomized_bundle_token\":\"1160050998451292762\",\"hardware_id\":\"90570b07852c65e1\",\"is_hardware_id_real\":true,\"brand\":\"Google\",\"model\":\"sdk_gphone64_arm64\",\"screen_dpi\":440,\"screen_height\":2236,\"screen_width\":1080,\"wifi\":true,\"ui_mode\":\"UI_MODE_TYPE_NORMAL\",\"os\":\"Android\",\"os_version\":32,\"cpu_type\":\"aarch64\",\"build\":\"TPP2.220218.008\",\"locale\":\"en_US\",\"connection_type\":\"wifi\",\"device_carrier\":\"T-Mobile\",\"os_version_android\":\"12\",\"country\":\"US\",\"language\":\"en\",\"local_ip\":\"10.0.2.16\"},\"REQ_POST_PATH\":\"v1\\/open\",\"INITIATED_BY_CLIENT\":true}"
        val jsonObject = JSONObject(jsonString)
        return ServerRequest.fromJSON(jsonObject, Branch.init().applicationContext)
    }

    @Before
    fun initializeValues() {
        initBranchInstance()
        referringUrlUtility = ReferringUrlUtility(PrefHelper.getInstance(Branch.init().applicationContext))
    }

    @Test
    fun testReferringURLWithNoParams() {
        val url = "https://bnctestbed.app.link"
        val expected = JSONObject()

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLIgnoredParam() {
        val url = "https://bnctestbed.app.link?other=12345"
        val expected = JSONObject()

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURINonHierarchical() {
        val uri = "branchtest:non-hierarchical"
        val expected = JSONObject()

        referringUrlUtility.parseReferringURL(uri)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclid() {
        val url = "https://bnctestbed.app.link?gclid=12345"
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithEmptyURIScheme() {
        val url = "branchtest://home"
        val expected = JSONObject()

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }
    @Test
    fun testReferringURLWithURISchemeSanityCheck() {
        val url = "branchtest://?gclid=12345"
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidCapitalized() {
        val url = "https://bnctestbed.app.link?GCLID=12345"
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidMixedCase() {
        val url = "https://bnctestbed.app.link?GcLiD=12345"
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidNoValue() {
        val url = "https://bnctestbed.app.link?gclid="
        val expected = JSONObject("""{"gclid": "", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidValueCasePreserved() {
        val url = "https://bnctestbed.app.link?gclid=aAbBcC"
        val expected = JSONObject("""{"gclid": "aAbBcC", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidIgnoredParam() {
        val url = "https://bnctestbed.app.link?gclid=12345&other=abcde"
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidFragment() {
        val url = "https://bnctestbed.app.link?gclid=12345#header"
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidAsFragment() {
        val url = "https://bnctestbed.app.link?other=abcde#gclid=12345"
        val expected = JSONObject()

        referringUrlUtility.parseReferringURL(url)
        val params = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    @Test
    fun testReferringURLWithGclidOverwritesValue() {
        val url1 = "https://bnctestbed.app.link?gclid=12345"
        val expected1 = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

        val url2 = "https://bnctestbed.app.link?gclid=abcde"
        val expected2 = JSONObject("""{"gclid": "abcde", "is_deeplink_gclid": true}""")

        referringUrlUtility.parseReferringURL(url1)
        val params1 = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())
        assertTrue(areJSONObjectsEqual(expected1, params1))

        referringUrlUtility.parseReferringURL(url2)
        val params2 = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())
        assertTrue(areJSONObjectsEqual(expected2, params2))
    }

    @Test
    fun testCheckForAndMigrateOldGclid() {
        PrefHelper.getInstance(Branch.init().applicationContext).setReferringUrlQueryParameters(null);
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": false}""")

        PrefHelper.getInstance(Branch.init().applicationContext).referrerGclid = "12345"
        PrefHelper.getInstance(Branch.init().applicationContext).referrerGclidValidForWindow = 2592000;

        val utility = ReferringUrlUtility(PrefHelper.getInstance(Branch.init().applicationContext))
        val params = utility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    @Test
    fun testSerializeToJson() {
        val params = mutableMapOf<String, BranchUrlQueryParameter>()
        val date = Date(1672444800000L)
        val param = BranchUrlQueryParameter(
            name = "example",
            value = "value",
            timestamp = date,
            isDeepLink = true,
            validityWindow = 1000L
        )
        params["example"] = param

        val resultJson = referringUrlUtility.serializeToJson(params)

        val exampleJson = resultJson.getJSONObject("example")
        assertEquals("example", exampleJson.getString("name"))
        assertEquals("value", exampleJson.getString("value"))
        assertEquals(dateFormat.format(date), exampleJson.getString("timestamp"))
        assertEquals(true, exampleJson.getBoolean("isDeeplink"))
        assertEquals(1000L, exampleJson.getLong("validityWindow"))
    }

    @Test
    fun testDeserializeFromJson() {
        val date = Date(1672444800000L)
        val jsonString = """{
            "example": {
                "name": "example",
                "value": "value",
                "timestamp": "${dateFormat.format(date)}",
                "isDeeplink": true,
                "validityWindow": 1000
            }
        }"""
        val jsonObj = JSONObject(jsonString)

        val resultMap = referringUrlUtility.deserializeFromJson(jsonObj)

        val exampleParam = resultMap["example"]!!
        assertEquals("example", exampleParam.name)
        assertEquals("value", exampleParam.value)
        assertEquals(date, exampleParam.timestamp)
        assertEquals(true, exampleParam.isDeepLink)
        assertEquals(1000L, exampleParam.validityWindow)
    }

    @Test
    fun testSerializationDeserializationRoundTrip() {
        val params = mutableMapOf<String, BranchUrlQueryParameter>()
        val date = Date(1672444800000L)
        val param = BranchUrlQueryParameter(
            name = "example",
            value = "value",
            timestamp = date,
            isDeepLink = true,
            validityWindow = 1000L
        )
        params["example"] = param

        val serializedJson = referringUrlUtility.serializeToJson(params)
        val deserializedMap = referringUrlUtility.deserializeFromJson(serializedJson)

        assertTrue(params == deserializedMap)
    }

    @Test
    fun testDeserializeFromJsonWithInvalidJSON() {
        val badJsonString = "{ this is not valid json }"

        assertThrows(JSONException::class.java) {
            referringUrlUtility.deserializeFromJson(JSONObject(badJsonString))
        }
    }

    @Test
    fun testSetReferringQueryParams() {
        val prefHelper =  PrefHelper.getInstance(Branch.init().applicationContext)
        prefHelper.setReferringUrlQueryParameters(JSONObject())

        assertEquals("{}", prefHelper.referringURLQueryParameters.toString());
    }

    @Test
    fun testSetGclidValidityWindow() {
        val testValidityWindow = 1_000_000L
        val expectedValidityWindow = (testValidityWindow / 1000).toInt()

        val prefHelper =  PrefHelper.getInstance(Branch.init().applicationContext)
        prefHelper.referrerGclidValidForWindow = testValidityWindow

        val url = "https://bnctestbed.app.link?gclid=12345"
        referringUrlUtility.parseReferringURL(url)
        val gclid = prefHelper.referringURLQueryParameters["gclid"] as JSONObject

        assertEquals(expectedValidityWindow, gclid["validityWindow"])
    }

    @Test
    fun testGclidExpires() {
        val prefHelper =  PrefHelper.getInstance(Branch.init().applicationContext)
        prefHelper.referrerGclidValidForWindow = 1000

        val url = "https://bnctestbed.app.link?gclid=12345"
        referringUrlUtility.parseReferringURL(url)

        //Gclid is still valid and should be included in requests.
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")
        val paramsOne = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())
        assertTrue(areJSONObjectsEqual(expected, paramsOne))

        Thread.sleep(2000)

        //Gclid has expired now and should not be included in requests.
        val expectedEmptyJSON = JSONObject()
        val paramsTwo = referringUrlUtility.getURLQueryParamsForRequest(openServerRequest())
        assertTrue(areJSONObjectsEqual(expectedEmptyJSON, paramsTwo))
    }

    //Helper functions
    fun areJSONObjectsEqual(json1: JSONObject, json2: JSONObject): Boolean {
        if (json1.length() != json2.length()) return false

        for (keyAny in json1.keys()) {
            val key = keyAny as String
            if (!json2.has(key) || json1[key] != json2[key]) {
                return false
            }
        }

        return true
    }
}