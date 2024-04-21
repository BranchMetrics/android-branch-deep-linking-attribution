include(":Branch-SDK")
include(":Branch-SDK-TestBed")
include(":Branch-SDK-Automation-TestBed")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
include(":app")
include(":branchsdk-link-clicktest")
