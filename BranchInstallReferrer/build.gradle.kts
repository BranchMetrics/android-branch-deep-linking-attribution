plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.branch.installreferrer"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // Google Play install referrer, included in the build
    implementation("com.android.installreferrer:installreferrer:2.2")
    // Huawei install referrer
    compileOnly("com.huawei.hms:ads-installreferrer:3.4.39.302")
    // Samsung install referrer
    compileOnly("store.galaxy.samsung.installreferrer:samsung_galaxystore_install_referrer:4.0.0")
    // Xiaomi install referrer
    compileOnly("com.miui.referrer:homereferrer:1.0.0.7")

    androidTestImplementation("com.huawei.hms:ads-installreferrer:3.4.39.302")
    androidTestImplementation("store.galaxy.samsung.installreferrer:samsung_galaxystore_install_referrer:4.0.0")

    // ----------Branch SDK Implementations----------
    implementation(project(":Branch-SDK"))
}