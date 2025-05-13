import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
}

dependencies {
    implementation(project(":Branch-SDK"))
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.huawei.hms:ads-identifier:3.4.62.300")

    implementation("com.android.billingclient:billing:6.0.1")
    implementation("com.huawei.hms:ads-installreferrer:3.4.39.302")
    implementation("store.galaxy.samsung.installreferrer:samsung_galaxystore_install_referrer:4.0.0")
    implementation("com.miui.referrer:homereferrer:1.0.0.7")

    implementation("androidx.browser:browser:1.8.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

android {
    val ANDROID_BUILD_SDK_VERSION_COMPILE: String by project
    val ANDROID_BUILD_SDK_VERSION_MINIMUM: String by project
    val VERSION_NAME: String by project
    val VERSION_CODE: String by project

    compileSdk = ANDROID_BUILD_SDK_VERSION_COMPILE.toInt()
    defaultConfig {
        applicationId = "io.branch.branchandroidtestbed"
        minSdk = ANDROID_BUILD_SDK_VERSION_MINIMUM.toInt()
        targetSdk = 34
        versionName = VERSION_NAME
        versionCode = VERSION_CODE.toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val propFile = file("signing.properties")
    val props = if (propFile.exists()) {
        val props = Properties()
        props.load(propFile.inputStream())
        props
    } else {
        null
    }

    if (props != null) {
        val pStoreFile = props["STORE_FILE"] as? String
        val pStorePassword = props["STORE_PASSWORD"] as? String
        val pKeyAlias = props["KEY_ALIAS"] as? String
        val pKeyPassword = props["KEY_PASSWORD"] as? String
        if (!pStoreFile.isNullOrBlank()
            && !pStorePassword.isNullOrBlank()
            && !pKeyAlias.isNullOrBlank()
            && !pKeyPassword.isNullOrBlank()
        ) {
            signingConfigs {
                create("release") {
                    storeFile = file(pStoreFile)
                    storePassword = pStorePassword
                    keyAlias = pKeyAlias
                    keyPassword = pKeyPassword
                }
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }
    namespace = "io.branch.branchandroidtestbed"
}
