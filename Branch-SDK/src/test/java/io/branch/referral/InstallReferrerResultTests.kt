package io.branch.referral

import android.content.Context
import io.branch.coroutines.getLatestInstallTimeStamp
import io.branch.coroutines.getLatestValidReferrerStore
import io.branch.data.InstallReferrerResult
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Arrays

@RunWith(JUnit4::class)
class InstallReferrerResultTests {
    @Test
    fun allValidReferrersLatestWins() {
        val test1 = InstallReferrerResult(
            "test1",
            1,
            "test1",
            1,
            1,
            1,
            true

        )

        val test2 = InstallReferrerResult(
            "test2",
            Long.MAX_VALUE,
            "test2",
            Long.MAX_VALUE,
            1,
            1,
            true
        )

        val test3 = InstallReferrerResult(
            "test3",
            2,
            "test3",
            2,
            1,
            1,
            true
        )

        val test4 = InstallReferrerResult(
            "test4",
            3,
            "test4",
            3,
            1,
            1,
            true
        )

        val installReferrers: MutableList<InstallReferrerResult?> = ArrayList()
        installReferrers.add(test1)
        installReferrers.add(test2)
        installReferrers.add(test3)
        installReferrers.add(test4)

        val actual = getLatestValidReferrerStore(installReferrers)
        Assert.assertEquals(test2, actual)
    }

    @Test
    fun NoValidReferrersReturnsNull() {
        val installReferrers: MutableList<InstallReferrerResult?> = ArrayList()
        installReferrers.add(null)
        installReferrers.add(null)
        installReferrers.add(null)
        installReferrers.add(null)

        val actual = getLatestValidReferrerStore(installReferrers)
        Assert.assertNull(actual)
    }

    @Test
    fun oneValidReferrerReturnsItself() {
        val test1 = InstallReferrerResult(
            "test1",
            1,
            "test1",
            1,
            1,
            1,
            true
        )

        val installReferrers: MutableList<InstallReferrerResult?> = ArrayList()
        installReferrers.add(test1)

        val actual = getLatestValidReferrerStore(installReferrers)
        Assert.assertEquals(test1, actual)
    }

    @Test
    fun testMetaInstallReferrerCases() {
        // Case 1: Meta referrer is click-through with non-organic Play Store referrer
        var metaReferrerClickThrough =
            InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, 1, 1, true)
        val playStoreReferrer = InstallReferrerResult(
            "PlayStore",
            1700000030,
            "utm_source=google-play&utm_medium=cpc",
            1700000000,
            1,
            1,
            true
        )
        var allReferrers = listOf(metaReferrerClickThrough, playStoreReferrer)
        var result = getLatestValidReferrerStore(allReferrers)
        Assert.assertEquals(metaReferrerClickThrough, result)

        // Case 2: Meta referrer is view-through with organic Play Store referrer
        var metaReferrerViewThrough =
            InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, 1, 1, false)
        var latestPlayStoreReferrer = InstallReferrerResult(
            "PlayStore",
            1700000030,
            "utm_source=google-play&utm_medium=organic",
            0,
            1,
            1,
            true
        )
        allReferrers = Arrays.asList(metaReferrerViewThrough, latestPlayStoreReferrer)
        result = getLatestValidReferrerStore(allReferrers)
        Assert.assertEquals(metaReferrerViewThrough, result)

        // Case 3: Meta referrer is view-through with non-organic Play Store referrer
        metaReferrerViewThrough =
            InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, 1, 1, false)
        latestPlayStoreReferrer = InstallReferrerResult(
            "PlayStore",
            1700000030,
            "utm_source=google-play&utm_medium=cpc",
            1700000000,
            1,
            1,
            true
        )
        allReferrers = Arrays.asList(metaReferrerViewThrough, latestPlayStoreReferrer)
        result = getLatestValidReferrerStore(allReferrers)
        Assert.assertEquals(latestPlayStoreReferrer, result)

        // Case 4: Meta referrer is outdated click-through with non-organic Play Store referrer
        metaReferrerClickThrough =
            InstallReferrerResult("Meta", 1700000030, "referrer", 1700000000, 1, 1, true)
        latestPlayStoreReferrer = InstallReferrerResult(
            "PlayStore",
            1700000500,
            "utm_source=google-play&utm_medium=cpc",
            1700000450,
            1,
            1,
            true
        )
        allReferrers = Arrays.asList(metaReferrerClickThrough, latestPlayStoreReferrer)
        result = getLatestValidReferrerStore(allReferrers)
        Assert.assertEquals(latestPlayStoreReferrer, result)

        // Case 5: Meta, Google Play, and Samsung Referrer (latest) are available
        metaReferrerClickThrough =
            InstallReferrerResult("Meta", 1700000000, "referrer", 1700000000, 1, 1, true)
        latestPlayStoreReferrer = InstallReferrerResult(
            "PlayStore",
            1700000000,
            "utm_source=google-play&utm_medium=cpc",
            1700000000,
            1,
            1,
            true
        )
        val samsungReferrer = InstallReferrerResult(
            "Samsung",
            1700001000,
            "utm_source=samsung-store&utm_medium=cpc",
            1700001000,
            1,
            1,
            true
        )
        allReferrers =
            Arrays.asList(metaReferrerClickThrough, latestPlayStoreReferrer, samsungReferrer)
        result = getLatestValidReferrerStore(allReferrers)
        Assert.assertEquals(samsungReferrer, result)
    }

    @Test
    fun testLatestInstallReferrerSort() {
        val referrer1 = InstallReferrerResult("Test1", 100, "test1Referrer", 1, 1,100)
        val referrer2 = InstallReferrerResult("Test2", 1, "test1Referrer", 1,1, 1)
        val referrer3 = null
        val referrer4 = InstallReferrerResult("Test1", 101, "test1Referrer", 101, 1,1)

        val result = getLatestInstallTimeStamp((mutableListOf(referrer1, referrer2, referrer3, referrer4)))

        assert(result == referrer4)
    }
}
