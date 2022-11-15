import  org.gradle.api.tasks.testing.logging.*

plugins {
    id("com.android.library") version "7.3.1" apply false
    id("com.android.application") version "7.3.1" apply false
    kotlin("android") version "1.6.21" apply false
}

val VERSION_NAME: String by project
val GROUP: String by project

allprojects {
    version = VERSION_NAME
    group = GROUP

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://maven.google.com") // Google"s Maven repository
        }
        google()
        maven {
            url = uri("https://developer.huawei.com/repo/") // Huawei's Maven Repository, use for compilation only
        }
    }
}

subprojects {
    tasks {
        withType<Javadoc> {
            enabled = true
        }
    }
}

tasks {
    withType<Test> {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(
                TestLogEvent.STARTED,
                TestLogEvent.SKIPPED,
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
            )
            showStandardStreams = true
        }
    }
}
