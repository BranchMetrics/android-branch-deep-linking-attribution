include(":Branch-SDK")
include(":Branch-SDK-TestBed")
include(":BranchFraudDefense")

// Branch Secure SDK, built from the sibling repo so the TestBed app can host it directly.
// Its `compileOnly` Branch dependency resolves to :Branch-SDK here (see securesdk/build.gradle.kts).
include(":securesdk")
project(":securesdk").projectDir = file("../branch-secure-sdk-android/securesdk")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
