plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.branchgoogleplaybillingv8"
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
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    kapt("com.google.auto.service:auto-service-annotations:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // ----------Google Play Billing library----------
    // Main: Do NOT bundle (let the user provide it)
    compileOnly("com.android.billingclient:billing:8.0.0")
    // Mockito needs this class in the test class path to run Unit Tests
    testImplementation("com.android.billingclient:billing:8.0.0")
    // For Android instrumented tests
    androidTestImplementation("com.android.billingclient:billing:8.0.0")

    // ----------Branch SDK Implementations----------
    implementation(project(":Branch-SDK"))

}