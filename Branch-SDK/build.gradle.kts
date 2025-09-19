import org.gradle.api.tasks.testing.logging.*

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
    signing
    id("org.gradle.test-retry") version "1.5.3"
    id("jacoco")
}
val coroutinesVersion = "1.6.4"
jacoco {
    toolVersion = "0.8.10"
}

configurations.all {
    resolutionStrategy {
        force("com.android.billingclient:billing:5.0.0")
    }
}
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- optional dependencies -----
    // Please note that the Branch SDK does not require any of the below optional dependencies to operate.
    // Import these into your app to enable these features.

    // Google Advertising ID
    compileOnly("com.google.android.gms:play-services-ads-identifier:18.0.1")
    // Huawei Open Advertising ID
    compileOnly("com.huawei.hms:ads-identifier:3.4.62.300")

    // Google Play install referrer, included in the build
    implementation("com.android.installreferrer:installreferrer:2.2")
    // Huawei install referrer
    compileOnly("com.huawei.hms:ads-installreferrer:3.4.39.302")
    // Samsung install referrer
    compileOnly("store.galaxy.samsung.installreferrer:samsung_galaxystore_install_referrer:4.0.0")
    // Xiaomi install referrer
    compileOnly("com.miui.referrer:homereferrer:1.0.0.7")

    // Google Play Billing library
    compileOnly("com.android.billingclient:billing:6.0.1")
    // In app browser experience
    compileOnly("androidx.browser:browser:1.8.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("org.skyscreamer:jsonassert:1.5.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

    androidTestImplementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    androidTestImplementation("com.huawei.hms:ads-identifier:3.4.62.300")
    androidTestImplementation("com.huawei.hms:ads-installreferrer:3.4.39.302")
    androidTestImplementation("com.huawei.hms:base:4.0.2.300")
    androidTestImplementation("com.android.billingclient:billing:6.0.1")
    androidTestImplementation("store.galaxy.samsung.installreferrer:samsung_galaxystore_install_referrer:4.0.0")
    androidTestImplementation("com.miui.referrer:homereferrer:1.0.0.7")

    testImplementation("org.json:json:20230227")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")

    // For JUnit tests
    testImplementation ("junit:junit:4.13.2")
    // Mockito core library
    testImplementation("org.mockito:mockito-core:5.4.0")
    // Mockito Kotlin extensions
    testImplementation ("org.mockito.kotlin:mockito-kotlin:4.1.0")
    // For Android instrumented tests (if needed)
    androidTestImplementation ("org.mockito:mockito-android:4.11.0")

    // Mockito needs these classes in the test class path
    testImplementation("androidx.browser:browser:1.8.0")
    testImplementation("com.android.billingclient:billing:6.0.1")

}

val VERSION_NAME: String by project
val ANDROID_BUILD_TOOLS_VERSION: String by project
val ANDROID_BUILD_SDK_VERSION_COMPILE: String by project
val ANDROID_BUILD_SDK_VERSION_MINIMUM: String by project
val VERSION_CODE: String by project

fun isReleaseBuild(): Boolean {
    return !VERSION_NAME.endsWith("SNAPSHOT")
}

android {
    compileSdk = ANDROID_BUILD_SDK_VERSION_COMPILE.toInt()
    defaultConfig {
        minSdk = ANDROID_BUILD_SDK_VERSION_MINIMUM.toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-consumer.txt")
    }

    lint {
        abortOnError = false
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        fun String.wrapInQuotes(): String {
            return "\"$this\""
        }

        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            buildConfigField("long", "VERSION_CODE", VERSION_CODE)
            buildConfigField("String", "VERSION_NAME", VERSION_NAME.wrapInQuotes())
        }
        release {
            buildConfigField("long", "VERSION_CODE", VERSION_CODE)
            buildConfigField("String", "VERSION_NAME", VERSION_NAME.wrapInQuotes())
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    namespace = "io.branch.referral"

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }

    signing {
        isRequired = isReleaseBuild()
    }
}

fun getRepositoryUsername(): String {
    return project.findProperty("NEXUS_USERNAME") as? String ?: ""
}

fun getRepositoryPassword(): String {
    return project.findProperty("NEXUS_PASSWORD") as? String ?: ""
}

fun getReleaseRepositoryUrl(): String {
    return project.findProperty("RELEASE_REPOSITORY_URL") as? String
        ?: "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
}

fun getSnapshotRepositoryUrl(): String {
    return project.findProperty("SNAPSHOT_REPOSITORY_URL") as? String
        ?: "https://central.sonatype.com/repository/maven-snapshots/"
}

fun getRepositoryUrl(): String {
    return if (isReleaseBuild()) {
        getReleaseRepositoryUrl()
    } else {
        getSnapshotRepositoryUrl()
    }
}


publishing {
    val GROUP: String by project
    val VERSION_NAME: String by project
    val POM_ARTIFACT_ID: String by project
    val POM_NAME: String by project
    val POM_DESCRIPTION: String by project
    val POM_URL: String by project
    val POM_SCM_URL: String by project
    val POM_SCM_CONNECTION: String by project
    val POM_SCM_DEV_CONNECTION: String by project
    val POM_LICENCE_NAME: String by project
    val POM_LICENCE_URL: String by project
    val POM_LICENCE_DIST: String by project
    val POM_DEVELOPER_ID: String by project
    val POM_DEVELOPER_NAME: String by project

    publications {
        fun MavenPublication.common() {
            group = GROUP
            artifactId = POM_ARTIFACT_ID
            version = VERSION_NAME
            pom {
                name.set(POM_NAME)
                url.set(POM_URL)
                description.set(POM_DESCRIPTION)
                developers {
                    developer {
                        id.set(POM_DEVELOPER_ID)
                        name.set(POM_DEVELOPER_NAME)
                    }
                }
                licenses {
                    license {
                        name.set(POM_LICENCE_NAME)
                        url.set(POM_LICENCE_URL)
                        distribution.set(POM_LICENCE_DIST)
                    }
                }

                scm {
                    url.set(POM_SCM_URL)
                    connection.set(POM_SCM_CONNECTION)
                    developerConnection.set(POM_SCM_DEV_CONNECTION)
                }

                withXml {
                    fun groovy.util.Node.getChild(name: String): groovy.util.Node {
                        return (get(name) as groovy.util.NodeList).first() as groovy.util.Node
                    }

                    fun groovy.util.Node.getChildOrNull(name: String): groovy.util.Node? {
                        return (get(name) as groovy.util.NodeList).firstOrNull() as? groovy.util.Node
                    }

                    val node = asNode()
                    val dependencies = node.getChild("dependencies")
                    dependencies.children().filterIsInstance<groovy.util.Node>()
                        .forEach { dependency ->
                            val artifactId = dependency.getChild("artifactId")
                            if (artifactId.text() == "okhttp" || artifactId.text() == "firebase-appindexing") {
                                // Ensure optional flag is set
                                val optional = dependency.getChildOrNull("optional")
                                if (optional != null) {
                                    optional.setValue("true")
                                } else {
                                    dependency.appendNode("optional", "true")
                                }

                                // Ensure scope is set to 'compile'
                                val scope = dependency.getChildOrNull("scope")
                                if (scope != null) {
                                    scope.setValue("compile")
                                }
                                else {
                                    dependency.appendNode("scope", "compile")
                                }
                            }
                        }
                }
            }
        }

        register<MavenPublication>("debug") {
            afterEvaluate {
                common()
                from(components["debug"])
            }
        }

        register<MavenPublication>("release") {
            val releasePublication = this
            afterEvaluate {
                common()
                from(components["release"])
                signing {
                    sign(releasePublication)
                }
            }
        }

        repositories {
            maven {
                url = uri(getRepositoryUrl())
                isAllowInsecureProtocol = true
                credentials {
                    username = getRepositoryUsername()
                    password = getRepositoryPassword()
                }
            }
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
            showExceptions = true
        }
        retry {
            maxRetries.set(3)
        }
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }
}

tasks.create<JacocoReport>("jacocoTestReport") {
    group = "Reporting"
    description = "Generate Jacoco code coverage reports after running tests."
    dependsOn("testDebugUnitTest","createDebugCoverageReport")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    sourceDirectories.setFrom("${project.projectDir}/src/main/java")
    classDirectories.setFrom("${project.buildDir}/intermediates/javac/debug/classes")
    executionData.setFrom(
        fileTree(project.buildDir) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
            )
        }
    )
}
