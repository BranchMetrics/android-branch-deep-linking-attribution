include(":Branch-SDK")
include(":Branch-SDK-TestBed")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
include(":BranchGooglePlayBilling")
include(":BranchInstallReferrer")
