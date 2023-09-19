package io.branch.channels

import android.content.Context
import android.net.TrafficStats
import io.branch.referral.BranchLogger
import io.branch.referral.BranchLogger.d
import io.branch.referral.PrefHelper
import io.branch.referral.UniversalResourceAnalyser
import io.branch.referral.UniversalResourceAnalyser.VERSION_KEY
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
    val channel = Channel <Context>(capacity = 1)
    val mutex = Mutex()
    private val TIME_OUT = 1500

    // This is the path for updating skip url list. Check for the next version of the file
    private val UPDATE_URL_PATH = "%sdk/uriskiplist_v#.json"


    suspend fun enqueue(context: Context) : JSONObject? {
        return withContext(Dispatchers.IO) {
            launch {
                channel.send(context)
            }
            executeNetworkRequest(channel.receive())
        }
    }

    private suspend fun executeNetworkRequest(context: Context): JSONObject? {
        mutex.withLock {
            TrafficStats.setThreadStatsTag(0)
            var respObject: JSONObject? = JSONObject()
            var connection: HttpsURLConnection? = null
            try {
                val update_url_path = UPDATE_URL_PATH.replace(
                    "%",
                    PrefHelper.getCDNBaseUrl()
                )
                val a = Integer.toString(
                    UniversalResourceAnalyser.getInstance(context).skipURLFormats.optInt(
                        VERSION_KEY
                    ) + 1)
                val urlObject = URL(
                    update_url_path.replace("#", a)
                )
                connection = urlObject.openConnection() as HttpsURLConnection
                connection!!.connectTimeout = TIME_OUT
                connection!!.readTimeout = TIME_OUT
                val responseCode = connection!!.responseCode
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    if (connection!!.inputStream != null) {
                        val rd = BufferedReader(
                            InputStreamReader(
                                connection!!.inputStream
                            )
                        )
                        respObject = JSONObject(rd.readLine())
                        BranchLogger.v("respObject $respObject")
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