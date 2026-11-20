plugins {
    id("com.android.application")
}

android {
    val ANDROID_BUILD_SDK_VERSION_COMPILE: String by project
    val ANDROID_BUILD_SDK_VERSION_MINIMUM: String by project

    compileSdk = ANDROID_BUILD_SDK_VERSION_COMPILE.toInt()
    namespace = "com.android.vending"

    defaultConfig {
        applicationId = "com.android.vending"
        minSdk = ANDROID_BUILD_SDK_VERSION_MINIMUM.toInt()
        targetSdk = ANDROID_BUILD_SDK_VERSION_COMPILE.toInt()
        // installreferrer requires >= 80837300.
        versionCode = 90000000
        // Checked by scripts/run_l1_referrer.sh.
        versionName = "test-play-store"
    }
}
