package io.branch.channels

import android.net.TrafficStats
import io.branch.referral.BranchLogger
import io.branch.referral.BranchLogger.d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SkipListChannel {
    val channel = Channel <URL>(capacity = 1)
    val mutex = Mutex()
    private val TIME_OUT = 1500

    suspend fun enqueue(url: URL) : JSONObject? {
        return withContext(Dispatchers.IO) {
            launch {
                channel.send(url)
            }
            executeNetworkRequest(channel.receive())
        }
    }

    private suspend fun executeNetworkRequest(url: URL): JSONObject? {
        mutex.withLock {
            BranchLogger.v("SkipListChannel executeNetworkRequest $url")

            TrafficStats.setThreadStatsTag(0)
            var respObject: JSONObject? = JSONObject()
            var connection: HttpsURLConnection? = null
            try {
                connection = url.openConnection() as HttpsURLConnection
                connection.connectTimeout = TIME_OUT
                connection.readTimeout = TIME_OUT
                val responseCode = connection.responseCode
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    if (connection.inputStream != null) {
                        val rd = BufferedReader(
                            InputStreamReader(
                                connection.inputStream
                            )
                        )
                        respObject = JSONObject(rd.readLine())
                    }
                }
            }
            catch (e: Exception) {
                d(e.message)
            }
            finally {
                connection?.disconnect()
            }
            return respObject
        }
    }
}