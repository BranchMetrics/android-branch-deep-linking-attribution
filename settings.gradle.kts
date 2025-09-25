include(":Branch-SDK")
include(":Branch-SDK-TestBed")
include (":BillingGooglePlayModules:BillingV6V7", ":BillingGooglePlayModules:BillingV8")


pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
