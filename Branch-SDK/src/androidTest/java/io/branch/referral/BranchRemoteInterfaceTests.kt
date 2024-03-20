package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.branch.referral.Defines.RequestPath
import io.branch.referral.network.BranchRemoteInterfaceUrlConnection
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * Uses real internet connection
 */
@RunWith(AndroidJUnit4::class)
class BranchRemoteInterfaceTests : BranchTest() {
    private lateinit var branchRemoteInterface: BranchRemoteInterfaceUrlConnection
    private val retryCountKey = "retryNumber"
    private val api = "https://httpbin.org/status/"
    private val testBranchKey = "key_test_hdcBLUy1xZ1JD0tKg7qrLcgirFmPPVJc"

    @Before
    override fun setUp() {
        super.setUp()
        initBranchInstance()
        branchRemoteInterface = BranchRemoteInterfaceUrlConnection(branch)
    }

    @Test
    fun returns200() {
        val postJson = JSONObject()
        val serverResponse =
            branchRemoteInterface.make_restful_post(postJson, api + HTTP_OK, "", "")

        Assert.assertNotNull(serverResponse)
        Assert.assertTrue(postJson.has(retryCountKey))
        Assert.assertEquals(0, postJson[retryCountKey])
        Assert.assertEquals(HTTP_OK, serverResponse.statusCode)
    }

    @Test
    fun returns401() {
        val postJson = JSONObject()
        val serverResponse =
            branchRemoteInterface.make_restful_post(postJson, api + HTTP_UNAUTHORIZED, "", "")

        Assert.assertNotNull(serverResponse)
        Assert.assertTrue(postJson.has(retryCountKey))
        Assert.assertEquals(0, postJson[retryCountKey])
        Assert.assertEquals(HTTP_UNAUTHORIZED, serverResponse.statusCode)
    }

    @Test
    fun returns404() {
        val postJson = JSONObject()
        val serverResponse =
            branchRemoteInterface.make_restful_post(postJson, api + HTTP_NOT_FOUND, "", "")

        Assert.assertNotNull(serverResponse)
        Assert.assertTrue(postJson.has(retryCountKey))
        Assert.assertEquals(0, postJson[retryCountKey])
        Assert.assertEquals(HTTP_NOT_FOUND, serverResponse.statusCode)
    }

    @Test
    fun returns500() {
        val postJson = JSONObject()
        val serverResponse =
            branchRemoteInterface.make_restful_post(postJson, api + HTTP_INTERNAL_ERROR, "", "")

        Assert.assertNotNull(serverResponse)
        Assert.assertTrue(postJson.has(retryCountKey))
        Assert.assertEquals(2, postJson[retryCountKey])
        Assert.assertEquals(HTTP_INTERNAL_ERROR, serverResponse.statusCode)
    }

    @Test
    fun handleBranchOpen200() {
        val postJson = JSONObject(
            "{\"randomized_device_token\":\"1233314118321202956\",\"randomized_bundle_token\":\"1228392152678522395\",\"hardware_id\":\"000000000000000\",\"is_hardware_id_real\":true,\"anon_id\":\"179f2841-03e7-4ae5-9052-8d9f88fa91a3\",\"brand\":\"Google\",\"model\":\"Pixel 4a\",\"screen_dpi\":440,\"screen_height\":2138,\"screen_width\":1080,\"wifi\":true,\"ui_mode\":\"UI_MODE_TYPE_NORMAL\",\"os\":\"Android\",\"os_version\":33,\"country\":\"US\",\"language\":\"en\",\"local_ip\":\"192.168.254.15\",\"partner_data\":{\"fb\":{\"em\":\"194b86d986ad041666822dad7602f1a7bac1d9e286273e86141666ffb4b1909b\",\"ph\":\"7cd2dfb0b893767d1b0a4cacd831f6f9cf0ace6b1fbfc616c313c672d08f6196\"}},\"app_version\":\"5.7.1\",\"initial_referrer\":\"android-app:\\/\\/com.android.shell\",\"update\":1,\"latest_install_time\":1696304146460,\"latest_update_time\":1696555451411,\"first_install_time\":1696304146460,\"previous_update_time\":1696304146460,\"environment\":\"FULL_APP\",\"identity\":\"newID5\",\"gclid\":\"testgclid\",\"is_deeplink_gclid\":false,\"metadata\":{},\"app_store\":\"PlayStore\",\"lat_val\":1,\"sdk\":\"android5.7.1\",\"branch_key\":\"key_test_hdcBLUy1xZ1JD0tKg7qrLcgirFmPPVJc\",\"retryNumber\":0}\n}"
        )
        val serverResponse = branchRemoteInterface.make_restful_post(
            postJson,
            PrefHelper.BRANCH_BASE_URL_V1 + RequestPath.RegisterOpen,
            "",
            testBranchKey
        )

        Assert.assertNotNull(serverResponse)
        Assert.assertTrue(postJson.has(retryCountKey))
        Assert.assertEquals(0, postJson[retryCountKey])

        Assert.assertEquals(HTTP_OK, serverResponse.statusCode)

        val responseJson = serverResponse.`object`
        Assert.assertTrue(responseJson != null)
        Assert.assertTrue(responseJson.has(Defines.Jsonkey.Data.key))

        val dataJson = JSONObject(responseJson.getString(Defines.Jsonkey.Data.key))
        Assert.assertNotNull(dataJson.getBoolean(Defines.Jsonkey.Clicked_Branch_Link.key))
        Assert.assertNotNull(dataJson.getBoolean(Defines.Jsonkey.IsFirstSession.key))
    }

    @Test
    fun handleBranchOpen400NoKey() {
        val postJson = JSONObject()
        val serverResponse = branchRemoteInterface.make_restful_post(
            postJson,
            PrefHelper.BRANCH_BASE_URL_V1 + RequestPath.RegisterOpen,
            "",
            ""
        )

        Assert.assertNotNull(serverResponse)
        Assert.assertTrue(postJson.has(retryCountKey))
        Assert.assertEquals(0, postJson[retryCountKey])

        Assert.assertEquals(HTTP_BAD_REQUEST, serverResponse.statusCode)

        val responseJson = serverResponse.`object`
        Assert.assertTrue(responseJson != null)
        Assert.assertFalse(responseJson.has(Defines.Jsonkey.Data.key))
        Assert.assertTrue(responseJson.has("error"))
    }
}