package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ReferringUrlUtilityTests : BranchTest() {

    private lateinit var referringUrlUtility: ReferringUrlUtility

    private fun openServerRequest(): ServerRequest {
        val jsonString = "{\"REQ_POST\":{\"randomized_device_token\":\"1144756305514505535\",\"randomized_bundle_token\":\"1160050998451292762\",\"hardware_id\":\"90570b07852c65e1\",\"is_hardware_id_real\":true,\"brand\":\"Google\",\"model\":\"sdk_gphone64_arm64\",\"screen_dpi\":440,\"screen_height\":2236,\"screen_width\":1080,\"wifi\":true,\"ui_mode\":\"UI_MODE_TYPE_NORMAL\",\"os\":\"Android\",\"os_version\":32,\"cpu_type\":\"aarch64\",\"build\":\"TPP2.220218.008\",\"locale\":\"en_US\",\"connection_type\":\"wifi\",\"device_carrier\":\"T-Mobile\",\"os_version_android\":\"12\",\"country\":\"US\",\"language\":\"en\",\"local_ip\":\"10.0.2.16\"},\"REQ_POST_PATH\":\"v1\\/open\",\"INITIATED_BY_CLIENT\":true}"
        val jsonObject = JSONObject(jsonString)
        return ServerRequest.fromJSON(jsonObject, Branch.getInstance().applicationContext)
    }

    private fun eventServerRequest(): ServerRequest {
        val jsonString = "{\"REQ_POST\":{\"name\":\"ADD_TO_CART\",\"custom_data\":{\"Custom_Event_Property_Key1\":\"Custom_Event_Property_val1\",\"Custom_Event_Property_Key2\":\"Custom_Event_Property_val2\"},\"event_data\":{\"affiliation\":\"test_affiliation\",\"coupon\":\"Coupon Code\",\"currency\":\"USD\",\"description\":\"Customer added item to cart\",\"shipping\":0,\"tax\":9.75,\"revenue\":1.5,\"search_query\":\"Test Search query\"},\"customer_event_alias\":\"my_custom_alias\",\"content_items\":[{\"content_schema\":\"COMMERCE_PRODUCT\",\"quantity\":2,\"price\":23.2,\"currency\":\"USD\",\"sku\":\"1994320302\",\"product_name\":\"my_product_name1\",\"product_brand\":\"my_prod_Brand1\",\"product_category\":\"Baby & Toddler\",\"condition\":\"EXCELLENT\",\"product_variant\":\"3T\",\"rating\":6,\"rating_average\":5,\"rating_count\":5,\"rating_max\":7,\"address_street\":\"Street_name1\",\"address_city\":\"city1\",\"address_region\":\"Region1\",\"address_country\":\"Country1\",\"address_postal_code\":\"postal_code\",\"latitude\":12.07,\"longitude\":-97.5,\"image_captions\":[\"my_img_caption1\",\"my_img_caption_2\"],\"Custom_Content_metadata_key1\":\"Custom_Content_metadata_val1\",\"og_title\":\"My Content Title\",\"canonical_identifier\":\"item\\/12345\",\"canonical_url\":\"https:\\/\\/branch.io\\/deepviews\",\"keywords\":[\"My_Keyword1\",\"My_Keyword2\"],\"og_description\":\"my_product_description1\",\"og_image_url\":\"https:\\/\\/test_img_url\",\"publicly_indexable\":false,\"locally_indexable\":true,\"creation_timestamp\":1680729573957}],\"user_data\":{\"android_id\":\"c96c8110-f98a-49cc-9ccf-a91962a3d099\",\"brand\":\"Google\",\"model\":\"sdk_gphone64_arm64\",\"screen_dpi\":440,\"screen_height\":2236,\"screen_width\":1080,\"ui_mode\":\"UI_MODE_TYPE_NORMAL\",\"os\":\"Android\",\"os_version\":32,\"cpu_type\":\"aarch64\",\"build\":\"TPP2.220218.008\",\"locale\":\"en_US\",\"connection_type\":\"wifi\",\"device_carrier\":\"T-Mobile\",\"os_version_android\":\"12\",\"country\":\"US\",\"language\":\"en\",\"local_ip\":\"10.0.2.16\",\"randomized_device_token\":\"1144756305514505535\",\"developer_identity\":\"testDevID\",\"app_store\":\"PlayStore\",\"app_version\":\"5.4.0\",\"sdk\":\"android\",\"sdk_version\":\"5.4.0\",\"user_agent\":\"Mozilla\\/5.0 (Linux; Android 12; Build\\/TPP2.220218.008; wv) AppleWebKit\\/537.36 (KHTML, like Gecko) Version\\/4.0 Chrome\\/109.0.5414.86 Mobile Safari\\/537.36\"}},\"REQ_POST_PATH\":\"v2\\/event\\/standard\"}\n"
        val jsonObject = JSONObject(jsonString)
        return ServerRequest.fromJSON(jsonObject, Branch.getInstance().applicationContext)
    }

    @Before
    fun initializeValues() {
        initBranchInstance()
        referringUrlUtility = ReferringUrlUtility(PrefHelper.getInstance(Branch.getInstance().applicationContext))
    }

    @Test
    fun testSupportedQueryParameters() {
        assertTrue(referringUrlUtility.isSupportedQueryParameter("gclid"))
        assertFalse(referringUrlUtility.isSupportedQueryParameter("unsupported_param"))
    }

    @Test
    fun testDefaultValidityWindowForParam() {
        assertEquals(2592000L, referringUrlUtility.defaultValidityWindowForParam("gclid"))
        assertEquals(0L, referringUrlUtility.defaultValidityWindowForParam("unsupported_param"))
    }

    @Test
    fun testSerializeAndDeserializeJson() {
        val urlQueryParameters = mutableMapOf<String, BranchUrlQueryParameter>()
        val param = BranchUrlQueryParameter(
            name = "gclid",
            value = "test_value",
            timestamp = Date(),
            isDeepLink = true,
            validityWindow = 2592000L
        )
        urlQueryParameters["gclid"] = param

        val serializedJson = referringUrlUtility.serializeToJson(urlQueryParameters)
        val deserializedMap = referringUrlUtility.deserializeFromJson(serializedJson)

        assertEquals(urlQueryParameters.size, deserializedMap.size)
        assertEquals(param.name, deserializedMap["gclid"]?.name)
        assertEquals(param.value, deserializedMap["gclid"]?.value)
        assertEquals(param.isDeepLink, deserializedMap["gclid"]?.isDeepLink)
        assertEquals(param.validityWindow, deserializedMap["gclid"]?.validityWindow)
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
    fun testReferringURLWithGclid() {
        val url = "https://bnctestbed.app.link?gclid=12345"
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": true}""")

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
        PrefHelper.getInstance(Branch.getInstance().applicationContext).setReferringUrlQueryParameters(null);
        val expected = JSONObject("""{"gclid": "12345", "is_deeplink_gclid": false}""")

        PrefHelper.getInstance(Branch.getInstance().applicationContext).referrerGclid = "12345"
        PrefHelper.getInstance(Branch.getInstance().applicationContext).referrerGclidValidForWindow = 2592000;

        val utility = ReferringUrlUtility(PrefHelper.getInstance(Branch.getInstance().applicationContext))
        val params = utility.getURLQueryParamsForRequest(openServerRequest())

        assertTrue(areJSONObjectsEqual(expected, params))
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