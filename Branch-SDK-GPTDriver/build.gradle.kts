plugins {
    id("com.android.test")
    kotlin("android")
}

android {
    val ANDROID_BUILD_SDK_VERSION_COMPILE: String by project

    compileSdk = ANDROID_BUILD_SDK_VERSION_COMPILE.toInt()
    namespace = "io.branch.gptdriver"

    targetProjectPath = ":Branch-SDK-TestBed"

    defaultConfig {
        minSdk = 24
        targetSdk = ANDROID_BUILD_SDK_VERSION_COMPILE.toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        multiDexEnabled = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }
}

dependencies {
    // Kotlin stdlib is declared explicitly on this `com.android.test` module so the
    // test APK always ships the Kotlin runtime its own classes need.
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test:runner:1.5.2")
    implementation("androidx.test:rules:1.5.0")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestUtil("androidx.test:orchestrator:1.5.0")
}
