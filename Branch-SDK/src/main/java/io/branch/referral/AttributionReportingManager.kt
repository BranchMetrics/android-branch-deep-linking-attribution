package io.branch.referral

import android.adservices.measurement.MeasurementManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.Executors

object AttributionReportingManager {
    private var isMeasurementApiEnabled: Boolean = false
    private const val MIN_AD_SERVICES_VERSION = 7

    fun checkMeasurementApiStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= MIN_AD_SERVICES_VERSION) {
            val executor = Executors.newSingleThreadExecutor()
            val manager = MeasurementManager.get(context)

            manager.getMeasurementApiStatus(executor, object : OutcomeReceiver<Int, Exception> {
                override fun onResult(result: Int) {
                    isMeasurementApiEnabled = result == MeasurementManager.MEASUREMENT_API_STATE_ENABLED
                    BranchLogger.v("Measurement API is ${if (isMeasurementApiEnabled) "enabled" else "not enabled"}")

                    executor.shutdown()
                }

                override fun onError(e: Exception) {
                    BranchLogger.d("Error while checking Measurement API status: ${e.message}")
                    executor.shutdown()
                }
            })
        }
    }

    fun isMeasurementApiEnabled(): Boolean = isMeasurementApiEnabled

    fun registerTrigger(context: Context, request: ServerRequest) {
        val scope = CoroutineScope(Dispatchers.IO + Job())

        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= MIN_AD_SERVICES_VERSION) {
                        if (isMeasurementApiEnabled()) {
                            BranchLogger.v("Registering trigger for ${request.requestPath} request")
                            val executor = Executors.newSingleThreadExecutor()
                            val manager = MeasurementManager.get(context)

                            val branchBaseURL = PrefHelper.getInstance(context).apiBaseUrl
                            val requestJson = request.toJSON().getJSONObject("REQ_POST")
                            val triggerUri = createURIFromJSON(branchBaseURL, requestJson)

                            //TODO: Trim the triggerURI if its over 10k characters by removing content items from the request JSON

                            manager.registerTrigger(triggerUri, executor, object : OutcomeReceiver<Any?, Exception> {
                                    override fun onResult(result: Any?) {
                                        BranchLogger.v("Trigger registered successfully with URI: $triggerUri")
                                        executor.shutdown()
                                    }

                                    override fun onError(e: Exception) {
                                        BranchLogger.w("Error while registering trigger with URI $triggerUri: ${e.message}")
                                        executor.shutdown()
                                    }
                                }
                            )
                        } else {
                            BranchLogger.v("Measurement API is not enabled. Did not register trigger.")
                        }
                    }
                }
            } catch (e: Exception) {
                BranchLogger.w("Error while registering source: ${e.message}")
            } finally {
                scope.cancel()
            }
        }
    }

    private fun createURIFromJSON(baseURL: String, json: JSONObject): Uri {
        val builder = Uri.parse(baseURL).buildUpon()
        val params = mutableMapOf<String, String>()
        parseJson("", json, params)

        params.forEach { (key, value) ->
            builder.appendQueryParameter(URLEncoder.encode(key, "UTF-8"), URLEncoder.encode(value, "UTF-8"))
        }

        return builder.build()
    }

    // Recursive function to flatten the JSON object into a map of strings
    private fun parseJson(prefix: String, json: JSONObject, params: MutableMap<String, String>) {
        json.keys().forEach { key ->
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            when (val value = json.get(key)) {
                is JSONObject -> {
                    parseJson(fullKey, value, params)
                }
                else -> {
                    params[fullKey] = value.toString()
                }
            }
        }
    }

}