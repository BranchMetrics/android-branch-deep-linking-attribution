package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import io.branch.referral.util.CurrencyType

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingGooglePlayTests : BranchTest() {

    @Before
    fun initializeValues() {
        initBranchInstance()
    }

    @Test
    fun testCreateAndLogEventWithPurchase() {
        val purchaseJsonString =
            "{\"orderId\":\"GPA.3363-6943-2756-89326\",\"packageName\":\"io.branch.referral.test\",\"productId\":\"one_button_click\",\"purchaseTime\":1677102142024,\"purchaseState\":0,\"purchaseToken\":\"ehnaiklogbdnkmeeichalhgi.AO-J1OygqRTr05Xd7Tuiwk5sSg302sRmiM_zcVcsXdkGyUxlIkwVR0y2j0xfY_1LovxU5qIoJ2iNwILe5H3WvPTGOjESP378uQ\",\"quantity\":1,\"acknowledged\":false}"
        val purchaseSignature =
            "XDFlSNC9Gqs+PPmO3xOFdLMaQ4FbsBEpTxBuOd+6adEEcz5Uovlgep+F5Xbr08+x/xzCEyNzybDYDcNg/PTzwfoK6Aeq44mocW4CPA1w/r1rdmgtwBD8nAdWIr3BbwXmcl6LYEGA6dL0N+/3zzjNzK/VWdqXazSdRyXxtlHnx8wsBFdPCBs1e9LtEwUcganA6ot0ttO2ySCKYNne2pEm2ScU+uuWZqZJ00VM7KH9pT+SKOOlSs6rRuFEvbGsoPUdybZQ0WoiXg6JD2hz9/35mQJF4Lkjh2kVgTh5MV4sCNnbMuUmhX/d09+pK2Fw6xiUng3FClOetFV9MaTtsmbz/g=="
        val mockPurchase = Purchase(purchaseJsonString, purchaseSignature)

        BillingGooglePlay.getInstance().createAndLogEventForPurchase(testContext, mockPurchase, listOf(), CurrencyType.USD, 99.99, "IAP")

        val queue = ServerRequestQueue.getInstance(testContext)
        val eventRequest = queue.peekAt(0)
        val customData = eventRequest.getParams.getJSONObject("custom_data")
        val eventData = eventRequest.getParams.getJSONObject("event_data")

        Assert.assertEquals("true", customData.get("logged_from_IAP"))
        Assert.assertEquals("io.branch.referral.test", customData.get("package_name"))
        Assert.assertEquals("GPA.3363-6943-2756-89326", customData.get("order_id"))

        Assert.assertEquals(99.99, eventData.get("revenue"))
        Assert.assertEquals("USD", eventData.get("currency"))

        Assert.assertEquals(1, queue.size.toLong())
        Assert.assertEquals(
            Defines.RequestPath.TrackStandardEvent.path,
            eventRequest.requestPath
        )
        Assert.assertTrue(eventRequest.isWaitingOnProcessToFinish)
    }
}