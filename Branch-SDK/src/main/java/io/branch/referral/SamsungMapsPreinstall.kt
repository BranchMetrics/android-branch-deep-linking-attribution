package io.branch.referral

import android.content.Context
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.Executor

private const val samsungMapsAuthority = "com.samsung.android.mapsagent.providers.apptracking"
private const val samsungMapsUri = "content://$samsungMapsAuthority/info"
private const val samsungMapsDefaultTimeoutMs = 3000L
private const val samsungMapsResult = "RESULT"
private const val samsungMapsId = "MAPS_ID"
internal const val samsungMapsRecordState = "state"
internal const val samsungMapsRecordStatePending = "pending"
internal const val samsungMapsRecordStateAnswered = "answered"
internal val samsungMapsColumns = listOf(
    samsungMapsResult,
    "REASON",
    "APP_TRACKING_ID",
    samsungMapsId,
    "INSTALLED_TIME_TEXT",
    "ACTIVATED_TIME_TEXT",
    "COUNTRY",
    "DEVICE_NAME",
    "FIRST_QUERY"
)

/**
 * Time limit for the Samsung MAPS query: the install referrer timeout when set, otherwise 3 seconds.
 */
internal fun samsungMapsTimeoutMs(installReferrerTimeoutMs: Int): Long =
    if (installReferrerTimeoutMs > 0) installReferrerTimeoutMs.toLong() else samsungMapsDefaultTimeoutMs

/**
 * Queries Samsung MAPS once per install and, when MAPS installed the app, sets the
 * pre-install partner and campaign for the install request.
 */
internal suspend fun fetchSamsungMapsPreinstallData(context: Context) {
    val prefHelper = PrefHelper.getInstance(context)
    fetchSamsungMapsPreinstallData(
        prefHelper = prefHelper,
        timeoutMs = samsungMapsTimeoutMs(prefHelper.installReferrerTimeout),
        isProviderPresent = {
            context.packageManager.resolveContentProvider(samsungMapsAuthority, 0) != null
        },
        query = { appTrackingId ->
            context.contentResolver.query(
                Uri.parse(samsungMapsUri),
                null,
                context.packageName,
                arrayOf(appTrackingId),
                null
            )
        },
        executor = Dispatchers.IO.asExecutor()
    )
}

internal suspend fun fetchSamsungMapsPreinstallData(
    prefHelper: PrefHelper,
    timeoutMs: Long,
    isProviderPresent: () -> Boolean,
    query: (String) -> Cursor?,
    executor: Executor
) {
    try {
        val appTrackingId = prefHelper.samsungMapsAppTrackingId
        if (appTrackingId == null) {
            BranchLogger.v("No Samsung MAPS app tracking ID set. Not querying Samsung MAPS")
            return
        }

        val savedRecord = prefHelper.samsungMapsQueryRecord
        if (savedRecord != null) {
            BranchLogger.v("Samsung MAPS was already queried for this install. Reusing record: $savedRecord")
            applySamsungMapsRecord(prefHelper, savedRecord)
            return
        }

        // The query runs on its own thread, because a blocked ContentResolver query can't be cancelled.
        val deferredRecord = CompletableDeferred<JSONObject?>()
        executor.execute {
            val startTime = System.currentTimeMillis()
            try {
                if (!isProviderPresent()) {
                    BranchLogger.v("Samsung MAPS provider not found. Not querying Samsung MAPS")
                    deferredRecord.complete(null)
                    return@execute
                }

                // Committed before the query, so a timeout or a crash still counts as a query.
                prefHelper.setSamsungMapsQueryRecord(JSONObject().put(samsungMapsRecordState, samsungMapsRecordStatePending))

                val record = query(appTrackingId)?.use { cursor -> readSamsungMapsRecord(cursor) }
                if (record != null) {
                    prefHelper.setSamsungMapsQueryRecord(record)
                }
                BranchLogger.v("Samsung MAPS query took ${System.currentTimeMillis() - startTime} ms, answer: $record")
                deferredRecord.complete(record)
            } catch (exception: Exception) {
                BranchLogger.w("Caught Samsung MAPS query exception after ${System.currentTimeMillis() - startTime} ms: $exception")
                deferredRecord.complete(null)
            }
        }

        val record = withTimeoutOrNull(timeoutMs) { deferredRecord.await() }
        if (record != null) {
            applySamsungMapsRecord(prefHelper, record)
        } else if (!deferredRecord.isCompleted) {
            BranchLogger.v("Samsung MAPS query timed out after $timeoutMs ms. Sending install without Samsung MAPS data")
        }
    } catch (exception: Exception) {
        BranchLogger.w("Caught fetchSamsungMapsPreinstallData exception: $exception")
    }
}

/**
 * Reads the first row of a Samsung MAPS answer. A missing column is saved as null.
 * Returns null when the answer has no rows.
 */
internal fun readSamsungMapsRecord(cursor: Cursor): JSONObject? {
    if (!cursor.moveToFirst()) {
        BranchLogger.v("Samsung MAPS answer has no rows")
        return null
    }

    val record = JSONObject().put(samsungMapsRecordState, samsungMapsRecordStateAnswered)
    for (column in samsungMapsColumns) {
        val index = cursor.getColumnIndex(column)
        val value = if (index == -1) null else cursor.getString(index)
        record.put(column, value ?: JSONObject.NULL)
    }
    return record
}

private fun applySamsungMapsRecord(prefHelper: PrefHelper, record: JSONObject) {
    val result = record.opt(samsungMapsResult)
    if (result is String && result.equals("true", ignoreCase = true)) {
        BranchPreinstall.setSamsungMapsPreinstallData(prefHelper, record.opt(samsungMapsId) as? String)
    }
}
