// Standalone throwaway diagnostic project for EMT-3881. Not part of the
// Branch-SDK Gradle build — deliberately not wired into the repo root
// settings.gradle.kts so it can't affect SDK builds.
plugins {
    id("com.android.application") version "8.12.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
