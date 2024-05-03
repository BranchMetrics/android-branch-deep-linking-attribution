package io.branch.referral

import android.adservices.measurement.MeasurementManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
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

                    if (isMeasurementApiEnabled) {
                        registerSource(context)
                    }
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

    fun registerSource(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= MIN_AD_SERVICES_VERSION) {
                if (isMeasurementApiEnabled()) {
                    val manager = MeasurementManager.get(context)
                    val executor = Executors.newSingleThreadExecutor()
                    val params = getParams(context)
                    val branchBaseURL = PrefHelper.getInstance(context).apiBaseUrl
                    val sourceUri = Uri.parse("${branchBaseURL}${Defines.RequestPath.RegisterSource}?$params")

                    manager.registerSource(sourceUri, null, executor, object : OutcomeReceiver<Any?, Exception> {
                        override fun onResult(result: Any?) {
                            BranchLogger.v("Source registered successfully with URI: $sourceUri")
                            executor.shutdown()
                        }

                        override fun onError(e: Exception) {
                            BranchLogger.w("Error while registering source: ${e.message}")
                            executor.shutdown()
                        }
                    })
                }
            }
        }
    }

    fun registerTrigger(context: Context, conversionId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= MIN_AD_SERVICES_VERSION) {
                if (isMeasurementApiEnabled()) {
                    BranchLogger.v("Registering trigger for conversion ID: $conversionId")

                    val executor = Executors.newSingleThreadExecutor()
                    val params = getParams(context) + "&conversion_id=$conversionId"
                    val branchBaseURL = PrefHelper.getInstance(context).apiBaseUrl
                    val triggerUri = Uri.parse("${branchBaseURL}${Defines.RequestPath.RegisterTrigger}?$params")
                    val manager = MeasurementManager.get(context)

                    manager.registerTrigger(triggerUri, executor, object : OutcomeReceiver<Any?, Exception> {
                            override fun onResult(result: Any?) {
                                BranchLogger.v("Trigger registered successfully with URI: $triggerUri")
                                executor.shutdown()
                            }

                            override fun onError(e: Exception) {
                                BranchLogger.w("Error while registering trigger: ${e.message}")
                                executor.shutdown()
                            }
                        }
                    )
                } else {
                    BranchLogger.v("Measurement API is not enabled. Did not register trigger.")
                }
            }
        }
    }

    private fun getParams(context: Context): String {
        val systemObserver = DeviceInfo.getInstance().systemObserver
        val prefHelper = PrefHelper.getInstance(context)

        val gaid = systemObserver.aid
        val hardwareId = SystemObserver.getUniqueID(context, false).id
        val brand = SystemObserver.getPhoneBrand()
        val model = SystemObserver.getPhoneModel()
        val osVersion = SystemObserver.getOSVersion()
        val apiLevel = SystemObserver.getAPILevel()
        val localIp = SystemObserver.getLocalIPAddress()
        val connectionType = SystemObserver.getConnectionType(context)
        val carrier = SystemObserver.getCarrier(context)
        val screenDisplay = SystemObserver.getScreenDisplay(context)
        val dpi = screenDisplay.densityDpi
        val screenWidth = screenDisplay.widthPixels
        val screenHeight = screenDisplay.heightPixels
        val uiMode = SystemObserver.getUIMode(context)
        val osName = SystemObserver.getOS(context)
        val language = SystemObserver.getISO2LanguageCode()
        val country = SystemObserver.getISO2CountryCode()
        val sandboxVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)
        val appPackageName = context.packageName
        val timestamp = System.currentTimeMillis()
        val platform = "ANDROID_APP"
        val os = "ANDROID"
        val appVersion = SystemObserver.getAppVersion(context)
        val eea = prefHelper.eeaRegion
        val environment = if (DeviceInfo.getInstance().isPackageInstalled) Defines.Jsonkey.NativeApp.key else Defines.Jsonkey.InstantApp.key;
        val appStore = prefHelper.appStoreSource
        val cpuType = SystemObserver.getCPUType()
        val wifi = SystemObserver.getWifiConnected(context)

        return listOf(
            "gaid" to gaid,
            "hardware_id" to hardwareId,
            "brand" to brand,
            "model" to model,
            "os_version" to osVersion,
            "api_level" to apiLevel.toString(),
            "local_ip" to localIp,
            "connection_type" to connectionType,
            "carrier" to carrier,
            "dpi" to dpi.toString(),
            "screen_width" to screenWidth.toString(),
            "screen_height" to screenHeight.toString(),
            "ui_mode" to uiMode,
            "os_name" to osName,
            "language" to language,
            "country" to country,
            "privacy_sandbox_version" to sandboxVersion.toString(),
            "app_package_name" to appPackageName,
            "timestamp" to timestamp.toString(),
            "platform" to platform,
            "os" to os,
            "app_version" to appVersion,
            "eea" to eea.toString(),
            "environment" to environment,
            "app_store" to appStore,
            "cpu_type" to cpuType,
            "wifi" to wifi.toString()
        ).joinToString("&") { "${it.first}=${Uri.encode(it.second)}" }
    }
}