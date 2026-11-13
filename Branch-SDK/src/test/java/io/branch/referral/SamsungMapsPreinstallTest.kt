package io.branch.referral

import android.content.Context
import android.database.Cursor
import io.branch.referral.Defines.PreinstallKey
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SamsungMapsPreinstallTest {

    private val appTrackingId = "ABC123DEF456"
    private val partnerKey = PreinstallKey.partner.key
    private val campaignKey = PreinstallKey.campaign.key
    private val directExecutor = Executor { it.run() }

    private lateinit var sharedPreferences: FakeSharedPreferences
    private lateinit var context: Context
    private lateinit var prefHelper: PrefHelper
    private val providerChecks = AtomicInteger(0)
    private val queries = AtomicInteger(0)

    @Before
    fun setUp() {
        PrefHelper.shutDown()
        sharedPreferences = FakeSharedPreferences()
        context = mock(Context::class.java)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        prefHelper = PrefHelper.getInstance(context)
    }

    @After
    fun tearDown() {
        PrefHelper.shutDown()
    }

    private fun cursorWith(row: Map<String, String?>): Cursor {
        val cursor = mock(Cursor::class.java)
        val columns = row.keys.toList()
        `when`(cursor.moveToFirst()).thenReturn(true)
        `when`(cursor.getColumnIndex(anyString())).thenAnswer { columns.indexOf(it.getArgument<String>(0)) }
        `when`(cursor.getString(anyInt())).thenAnswer { row[columns[it.getArgument<Int>(0)]] }
        return cursor
    }

    private fun emptyCursor(): Cursor {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.moveToFirst()).thenReturn(false)
        return cursor
    }

    private fun fullRow(result: String) = linkedMapOf<String, String?>(
        "RESULT" to result,
        "REASON" to "reason",
        "APP_TRACKING_ID" to appTrackingId,
        "MAPS_ID" to "12345",
        "INSTALLED_TIME_TEXT" to "26:10:01:10:00",
        "ACTIVATED_TIME_TEXT" to "26:10:01:10:05",
        "COUNTRY" to "USA",
        "DEVICE_NAME" to "SM-S928U",
        "FIRST_QUERY" to "true"
    )

    private fun fetch(
        providerPresent: Boolean = true,
        timeoutMs: Long = 3000L,
        executor: Executor = directExecutor,
        query: (String) -> Cursor?
    ) = runBlocking {
        fetchSamsungMapsPreinstallData(
            prefHelper = prefHelper,
            timeoutMs = timeoutMs,
            isProviderPresent = {
                providerChecks.incrementAndGet()
                providerPresent
            },
            query = { id ->
                queries.incrementAndGet()
                query(id)
            },
            executor = executor
        )
    }

    private fun installPartner(): String? = prefHelper.getInstallMetaData(partnerKey)

    private fun installCampaign(): String? = prefHelper.getInstallMetaData(campaignKey)

    @Test
    fun readsAllNineColumns() {
        val row = fullRow("true")

        val record = readSamsungMapsRecord(cursorWith(row))!!

        assertEquals(samsungMapsRecordStateAnswered, record.getString(samsungMapsRecordState))
        for ((column, value) in row) {
            assertEquals(value, record.getString(column))
        }
    }

    @Test
    fun missingColumnsBecomeNull() {
        val record = readSamsungMapsRecord(cursorWith(mapOf("RESULT" to "true")))!!

        assertEquals("true", record.getString("RESULT"))
        for (column in samsungMapsColumns.drop(1)) {
            assertTrue(column, record.isNull(column))
        }
    }

    @Test
    fun resultTrueSetsPartnerAndMapsIdAsCampaign() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        var queriedId: String? = null

        fetch { id ->
            queriedId = id
            cursorWith(fullRow("true"))
        }

        assertEquals(appTrackingId, queriedId)
        assertEquals(BranchPreinstall.SAMSUNG_MAPS_PREINSTALL_PARTNER, installPartner())
        assertEquals("12345", installCampaign())
        assertEquals("12345", prefHelper.samsungMapsQueryRecord!!.getString("MAPS_ID"))
    }

    @Test
    fun resultTrueWithoutMapsIdDefaultsCampaign() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        val row = fullRow("true").apply { put("MAPS_ID", null) }

        fetch { cursorWith(row) }

        assertEquals(BranchPreinstall.SAMSUNG_MAPS_PREINSTALL_PARTNER, installPartner())
        assertEquals(BranchPreinstall.SAMSUNG_MAPS_DEFAULT_CAMPAIGN, installCampaign())
    }

    @Test
    fun resultTrueWithoutMapsIdColumnDefaultsCampaign() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)

        fetch { cursorWith(mapOf("RESULT" to "true")) }

        assertEquals(BranchPreinstall.SAMSUNG_MAPS_DEFAULT_CAMPAIGN, installCampaign())
    }

    @Test
    fun resultTrueIgnoresCase() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)

        fetch { cursorWith(fullRow("TRUE")) }

        assertEquals(BranchPreinstall.SAMSUNG_MAPS_PREINSTALL_PARTNER, installPartner())
    }

    @Test
    fun resultFalseSetsNothingButSavesRecord() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)

        fetch { cursorWith(fullRow("false")) }

        assertEquals(0, prefHelper.installMetadata.length())
        assertEquals("false", prefHelper.samsungMapsQueryRecord!!.getString("RESULT"))
    }

    @Test
    fun noIdSetMeansNoQueryAndNoRecord() {
        fetch { cursorWith(fullRow("true")) }

        assertEquals(0, providerChecks.get())
        assertEquals(0, queries.get())
        assertNull(prefHelper.samsungMapsQueryRecord)
        assertEquals(0, prefHelper.installMetadata.length())
    }

    @Test
    fun noProviderMeansNoQueryAndNoRecord() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)

        fetch(providerPresent = false) { cursorWith(fullRow("true")) }

        assertEquals(1, providerChecks.get())
        assertEquals(0, queries.get())
        assertNull(prefHelper.samsungMapsQueryRecord)
    }

    @Test
    fun queryThrowsLeavesPendingRecordAndNoValues() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)

        fetch { throw SecurityException("READ_APP_INFO not granted") }

        assertEquals(samsungMapsRecordStatePending, prefHelper.samsungMapsQueryRecord!!.getString(samsungMapsRecordState))
        assertEquals(0, prefHelper.installMetadata.length())
    }

    @Test
    fun emptyAnswerLeavesPendingRecordAndNoValues() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)

        fetch { emptyCursor() }

        assertEquals(samsungMapsRecordStatePending, prefHelper.samsungMapsQueryRecord!!.getString(samsungMapsRecordState))
        assertEquals(0, prefHelper.installMetadata.length())
    }

    @Test
    fun secondInstallBuildReusesRecordWithoutQuerying() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        fetch { cursorWith(fullRow("true")) }

        // A new process: install metadata is in memory only, the record is persisted.
        PrefHelper.shutDown()
        prefHelper = PrefHelper.getInstance(context)
        assertNull(installPartner())

        fetch { cursorWith(fullRow("true")) }

        assertEquals(1, queries.get())
        assertEquals(BranchPreinstall.SAMSUNG_MAPS_PREINSTALL_PARTNER, installPartner())
    }

    @Test
    fun pendingRecordMeansNoQueryAndNoValues() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        prefHelper.setSamsungMapsQueryRecord(JSONObject().put(samsungMapsRecordState, samsungMapsRecordStatePending))

        fetch { cursorWith(fullRow("true")) }

        assertEquals(0, providerChecks.get())
        assertEquals(0, queries.get())
        assertEquals(0, prefHelper.installMetadata.length())
    }

    @Test
    fun recordSurvivesBranchKeyChange() {
        prefHelper.setBranchKey("key_live_first")
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        prefHelper.setSamsungMapsQueryRecord(JSONObject().put(samsungMapsRecordState, samsungMapsRecordStatePending))

        assertTrue(prefHelper.setBranchKey("key_live_second"))

        assertEquals(samsungMapsRecordStatePending, prefHelper.samsungMapsQueryRecord!!.getString(samsungMapsRecordState))
        assertNull(prefHelper.samsungMapsAppTrackingId)
    }

    @Test
    fun branchKeyChangeWithoutRecordDoesNotCreateOne() {
        prefHelper.setBranchKey("key_live_first")

        prefHelper.setBranchKey("key_live_second")

        assertNull(prefHelper.samsungMapsQueryRecord)
    }

    @Test
    fun timeLimitIsThreeSecondsWhenInstallReferrerTimeoutIsZero() {
        assertEquals(3000L, samsungMapsTimeoutMs(0))
        assertEquals(500L, samsungMapsTimeoutMs(500))
    }

    @Test
    fun blockedQueryStopsWaitingAtTheLimitAndLateAnswerIsNotApplied() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val start = System.currentTimeMillis()
            fetch(timeoutMs = 200L, executor = executor) {
                release.await(2, TimeUnit.SECONDS)
                cursorWith(fullRow("true"))
            }
            val elapsed = System.currentTimeMillis() - start

            assertTrue("waited $elapsed ms", elapsed < 1000)
            assertEquals(samsungMapsRecordStatePending, prefHelper.samsungMapsQueryRecord!!.getString(samsungMapsRecordState))
            assertNull(installPartner())

            // The late answer is saved for a rebuilt install request, but not applied to this one.
            release.countDown()
            executor.shutdown()
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
            assertEquals("true", prefHelper.samsungMapsQueryRecord!!.getString("RESULT"))
            assertNull(installPartner())
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun partnerSetByAppIsNotOverwritten() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        prefHelper.addInstallMetadata(partnerKey, "app_partner")

        fetch { cursorWith(fullRow("true")) }

        assertEquals("app_partner", installPartner())
        assertNull(installCampaign())
    }

    @Test
    fun campaignSetByAppIsNotOverwritten() {
        prefHelper.setSamsungMapsAppTrackingId(appTrackingId)
        prefHelper.addInstallMetadata(campaignKey, "app_campaign")

        fetch { cursorWith(fullRow("true")) }

        assertEquals("app_campaign", installCampaign())
        assertNull(installPartner())
    }
}
