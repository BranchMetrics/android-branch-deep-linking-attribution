package io.branch.referral

import android.adservices.measurement.MeasurementManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors


object AttributionReportingManager {
    private var isMeasurementApiEnabled: Boolean = false

    fun checkMeasurementApiStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 7 ) {
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

    fun registerTrigger(context: Context, conversionId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 7) {
                if (isMeasurementApiEnabled()) {
                    BranchLogger.v("Registering trigger for conversion ID: $conversionId")
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
                    val sandboxVersion =
                        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 4) {
                            SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)
                        } else {
                            0
                        }
                    val appPackageName = context.packageName
                    val timestamp = System.currentTimeMillis()
                    val platform = "ANDROID_APP"
                    val os = "ANDROID"
                    val appVersion = SystemObserver.getAppVersion(context)
                    val eea = prefHelper.eeaRegion
                    val environment =
                        if (DeviceInfo.getInstance().isPackageInstalled) Defines.Jsonkey.NativeApp.key else Defines.Jsonkey.InstantApp.key;
                    val appStore = prefHelper.appStoreSource
                    val cpuType = SystemObserver.getCPUType()
                    val wifi = SystemObserver.getWifiConnected(context)

                    // Construct the trigger URI with all the available parameters
                    val params = listOf(
                        "conv_id" to conversionId,
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

                    val triggerUri = Uri.parse("${PrefHelper.BRANCH_BASE_URL_V2}v1/android-attribution-api/trigger?$params")
                    val executor = Executors.newSingleThreadExecutor()
                    val manager = MeasurementManager.get(context)

                    manager.registerTrigger(
                        triggerUri,
                        executor,
                        object : OutcomeReceiver<Any?, Exception> {
                            override fun onResult(result: Any?) {
                                BranchLogger.v("Trigger registered successfully with URI: $triggerUri")
                            }

                            override fun onError(e: Exception) {
                                BranchLogger.d("Error while registering trigger: ${e.message}")
                            }
                        }
                    )
                } else {
                    BranchLogger.v("Measurement API is not enabled. Did not register trigger.")
                }
            }
        }
    }
}