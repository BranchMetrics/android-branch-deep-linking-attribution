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
    }
}

dependencies {
    // Kept defensively. The Kotlin Gradle plugin already adds the stdlib, and the
    // test APK carries no kotlin.* classes either way: at runtime they come from
    // the target APK (Branch-SDK-TestBed).
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))

    // SDK types for drivers that call the SDK directly. Provided at runtime by the target APK.
    compileOnly(project(":Branch-SDK"))

    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test:runner:1.5.2")
    implementation("androidx.test:rules:1.5.0")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestUtil("androidx.test:orchestrator:1.5.0")
}
