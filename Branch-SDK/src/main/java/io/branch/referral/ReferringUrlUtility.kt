package io.branch.referral

import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import androidx.annotation.VisibleForTesting

class ReferringUrlUtility (prefHelper: PrefHelper) {
    private val urlQueryParameters: MutableMap<String, BranchUrlQueryParameter>
    private var prefHelper: PrefHelper

    init {
        this.prefHelper = prefHelper
        urlQueryParameters = deserializeFromJson(prefHelper.referringURLQueryParameters)
        checkForAndMigrateOldGclid()
    }

    fun parseReferringURL(urlString: String) {
        val uri = Uri.parse(urlString)

        for (paramName in uri.queryParameterNames) {
            val paramValue = uri.getQueryParameter(paramName)
            PrefHelper.Debug("Found URL Query Parameter - Key: $paramName, Value: $paramValue")

            if (isSupportedQueryParameter(paramName)) {
                val param = findUrlQueryParam(paramName)
                param.value = paramValue
                param.timestamp = Date()
                param.isDeepLink = true

                // If there is no validity window, set to default.
                if (param.validityWindow == 0L) {
                    param.validityWindow = defaultValidityWindowForParam(paramName) as Long
                }

                urlQueryParameters[paramName] = param
            }
        }

        prefHelper.setReferringUrlQueryParameters(serializeToJson(urlQueryParameters))
        PrefHelper.Debug(prefHelper.referringURLQueryParameters.toString())
    }

    fun getURLQueryParamsForRequest(request: ServerRequest): JSONObject {
        val returnedParams = mutableMapOf<String, Any>()

        val gclid = addGclidValueFor(request)
        if (gclid.length() > 0) {
            val keys = gclid.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                returnedParams[key] = gclid.get(key)
            }
        }
        return JSONObject(returnedParams as Map<*, *>)
    }

    @VisibleForTesting
    private fun addGclidValueFor(request: ServerRequest): JSONObject {
        val returnParams = JSONObject()

        if (request !is ServerRequestRegisterInstall) {
            val gclid = urlQueryParameters["gclid"]
            if (gclid != null) {
                if (gclid.value != null && gclid.value != PrefHelper.NO_STRING_VALUE) {
                    returnParams.put(Defines.Jsonkey.ReferrerGclid.key, gclid.value)
                    returnParams.put("is_deeplink_gclid", gclid.isDeepLink)

                    gclid.isDeepLink = false
                    prefHelper.setReferringUrlQueryParameters(serializeToJson(urlQueryParameters))
                }
            }
        }

        return returnParams
    }

    @VisibleForTesting
    internal fun isSupportedQueryParameter(paramName: String): Boolean {
        val lowercase = paramName.lowercase()
        val validURLQueryParameters = listOf("gclid")
        return lowercase in validURLQueryParameters
    }

    @VisibleForTesting
    internal fun findUrlQueryParam(paramName: String): BranchUrlQueryParameter {
        return urlQueryParameters[paramName] ?: BranchUrlQueryParameter(name = paramName)
    }

    @VisibleForTesting
    internal fun defaultValidityWindowForParam(paramName: String): Long {
        return if (paramName == "gclid") {
            30 * 24 * 60 * 60 // 30 days = 2,592,000 seconds
        }
        else {
            0L // Default, means indefinite.
        }
    }

    @VisibleForTesting
    internal fun serializeToJson(urlQueryParameters: MutableMap<String, BranchUrlQueryParameter>): JSONObject {
        val json = JSONObject()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        for (param in urlQueryParameters.values) {
            val paramDict = JSONObject()
            paramDict.put("name", param.name)
            paramDict.put("value", param.value ?: JSONObject.NULL)
            paramDict.put("timestamp", param.timestamp?.let { dateFormat.format(it) })
            paramDict.put("isDeeplink", param.isDeepLink)
            paramDict.put("validityWindow", param.validityWindow)

            json.put(param.name, paramDict)
        }

        return json
    }

    @VisibleForTesting
    internal fun deserializeFromJson(json: JSONObject): MutableMap<String, BranchUrlQueryParameter> {
        val result = mutableMapOf<String, BranchUrlQueryParameter>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val temp = json.getJSONObject(key)

            val param = BranchUrlQueryParameter()
            param.name = temp.getString("name")

            if (!temp.isNull("value")) {
                param.value = temp.getString("value")
            }

            param.timestamp = temp.getString("timestamp")?.let { dateFormat.parse(it) }
            param.validityWindow = temp.getLong("validityWindow")

            if (!temp.isNull("isDeeplink")) {
                param.isDeepLink = temp.getBoolean("isDeeplink")
            } else {
                param.isDeepLink = false
            }

            param.name?.let { paramName ->
                result[paramName] = param
            }
        }

        return result
    }

    @VisibleForTesting
    internal fun checkForAndMigrateOldGclid() {
        //Check if there is an existing Gclid, validityWindow, etc. If there is, create a new BranchUrlQueryParameter for it.
        val oldGclid = urlQueryParameters["gclid"]
        if (oldGclid?.value == null) {
            val existingGclidValue = prefHelper.referrerGclid
            if (existingGclidValue != null) {
                val existingGclidValidityWindow = prefHelper.referrerGclidValidForWindow

                val gclid = BranchUrlQueryParameter(
                    name = "gclid",
                    value = existingGclidValue,
                    timestamp = Date(),
                    validityWindow = existingGclidValidityWindow,
                    isDeepLink = false
                )

                urlQueryParameters["gclid"] = gclid

                prefHelper.setReferringUrlQueryParameters(serializeToJson(urlQueryParameters))
                prefHelper.referrerGclid = null
                prefHelper.referrerGclidValidForWindow = 0

                Log.d("ReferringURLUtility","Updated old Gbraid to new BranchUrlQueryParameter")
            }
        }
    }
}


    data class BranchUrlQueryParameter(
    var name: String? = null,
    var value: String? = null,
    var timestamp: Date? = null,
    var isDeepLink: Boolean = false,
    var validityWindow: Long = 0
)
