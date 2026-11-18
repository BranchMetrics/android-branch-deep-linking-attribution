package io.branch.referral

import io.branch.coroutines.fetchReferrersKeepingArrived
import io.branch.coroutines.getLatestValidReferrerStore
import io.branch.data.InstallReferrerResult
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FetchReferrersKeepingArrivedTest {

    private val play = InstallReferrerResult("PlayStore", 1700000030, "utm_source=google-play", 1700000000, 1, 1, false)
    private val huawei = InstallReferrerResult("AppGallery", 1700000040, "utm_source=huawei", 1700000010, null, null, false)
    private val meta = InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, null, null, true)

    @Test
    fun timeoutKeepsReferrersThatArrived() = runBlocking {
        val start = System.currentTimeMillis()
        val result = fetchReferrersKeepingArrived(100, listOf(
            { play },
            { awaitCancellation() }
        ))
        val elapsed = System.currentTimeMillis() - start

        assertEquals(play, result)
        assertTrue("took $elapsed ms", elapsed in 100..1000)
    }

    @Test
    fun timeoutWithNothingArrivedReturnsNull() = runBlocking {
        val result = fetchReferrersKeepingArrived(100, listOf(
            { awaitCancellation() },
            { awaitCancellation() }
        ))

        assertNull(result)
    }

    @Test
    fun allArrivedMatchesLatestValidReferrerStore() = runBlocking {
        val all = listOf(play, huawei, null, null, meta)
        val result = fetchReferrersKeepingArrived(1000, all.map { referrer -> suspend { referrer } })

        assertEquals(getLatestValidReferrerStore(all), result)
    }

    @Test
    fun zeroTimeoutWaitsForAll() = runBlocking {
        val start = System.currentTimeMillis()
        val result = fetchReferrersKeepingArrived(0, listOf(
            { play },
            { delay(300); huawei }
        ))
        val elapsed = System.currentTimeMillis() - start

        assertEquals(huawei, result)
        assertTrue("took $elapsed ms", elapsed >= 300)
    }

    @Test
    fun throwingFetcherIsTreatedAsNull() = runBlocking {
        val result = fetchReferrersKeepingArrived(1000, listOf(
            { play },
            { throw IllegalStateException("referrer library failed") }
        ))

        assertEquals(play, result)
    }
}
