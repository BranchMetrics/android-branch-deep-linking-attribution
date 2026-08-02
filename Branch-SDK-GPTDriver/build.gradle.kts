import java.util.Properties

plugins {
    id("com.android.test")
    kotlin("android")
}

val mobileboostApiKey: String by lazy {
    findProperty("MOBILEBOOST_API_KEY")?.toString()?.takeIf { it.isNotEmpty() }
        ?: rootProject.file("local.properties").let { file ->
            if (file.exists()) {
                val props = Properties()
                file.inputStream().use { props.load(it) }
                props.getProperty("MOBILEBOOST_API_KEY")?.takeIf { it.isNotEmpty() }
            } else null
        }
        ?: System.getenv("MOBILEBOOST_API_KEY")?.takeIf { it.isNotEmpty() }
        ?: ""
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

        buildConfigField(
            "String",
            "MOBILEBOOST_API_KEY",
            "\"$mobileboostApiKey\""
        )
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }

    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/io.netty.versions.properties",
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt"
                )
            )
        }
    }
}

dependencies {
    // Kotlin stdlib must be declared explicitly on `com.android.test` modules.
    // Without this, the test APK can ship without `kotlin.collections.CollectionsKt`,
    // which causes `androidx.startup.InitializationProvider` to crash the process
    // before `Application.onCreate` when the gptdriver-lib transitively pulls
    // `androidx.lifecycle:lifecycle-process` (Kotlin).
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))

    // GPTDriver for View/XML-based apps
    // Docs: https://docs.mobileboost.io/gpt-driver-sdk/espresso/view-xml-based-apps/setup
    implementation("io.mobileboost.gptdriver:gptdriver-lib:1.3.2") {
        exclude(group = "org.seleniumhq.selenium", module = "selenium-chrome-driver")
        exclude(group = "org.seleniumhq.selenium", module = "selenium-firefox-driver")
        exclude(group = "org.seleniumhq.selenium", module = "selenium-edge-driver")
        exclude(group = "org.seleniumhq.selenium", module = "selenium-safari-driver")
        exclude(group = "org.seleniumhq.selenium", module = "selenium-ie-driver")
        exclude(group = "io.netty")
    }

    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test:runner:1.5.2")
    implementation("androidx.test:rules:1.5.0")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestUtil("androidx.test:orchestrator:1.5.0")
}
