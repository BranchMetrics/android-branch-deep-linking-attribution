import org.gradle.api.tasks.testing.logging.*
import java.io.OutputStream

// EMT-3877 — tees an OutputStream to two sinks (console + report file) so the
// japicmp diff is both printed and persisted. Used by `apiCompatibilityReport`.
class TeeOutputStream(private val a: OutputStream, private val b: OutputStream) : OutputStream() {
    override fun write(byte: Int) { a.write(byte); b.write(byte) }
    override fun write(bytes: ByteArray, off: Int, len: Int) { a.write(bytes, off, len); b.write(bytes, off, len) }
    override fun flush() { a.flush(); b.flush() }
    override fun close() { b.close() } // never close System.out
}

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

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // App foreground/background detection at the process level (SDK-2463): ProcessLifecycleOwner.
    // Floor is 2.4.1: DefaultLifecycleObserver available since 2.4.0, compatible with Kotlin 1.6.21.
    implementation("androidx.lifecycle:lifecycle-process:2.4.1")

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

    // JUnit dependencies for unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    
    testImplementation("org.json:json:20230227")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")

    // Mockito core library
    testImplementation("org.mockito:mockito-core:5.4.0")
    // Mockito Kotlin extensions
    testImplementation ("org.mockito.kotlin:mockito-kotlin:4.1.0")
    // For Android instrumented tests (if needed)
    androidTestImplementation ("org.mockito:mockito-android:4.11.0")

    // Robolectric for Android unit testing
    testImplementation("org.robolectric:robolectric:4.10.3")

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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
        ?: "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
}

fun getSnapshotRepositoryUrl(): String {
    return project.findProperty("SNAPSHOT_REPOSITORY_URL") as? String
        ?: "https://oss.sonatype.org/content/repositories/snapshots/"
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
        // Opt-in fast loop: `-PexcludeSlowTests` skips tests that wait out a real timeout
        // (see io.branch.referral.SlowTest). Default runs keep full coverage.
        if (project.hasProperty("excludeSlowTests")) {
            useJUnit {
                excludeCategories("io.branch.referral.SlowTest")
            }
        }
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

// ---------------------------------------------------------------------------
// EMT-3877 — Public API diff gate ("accidental API removal" report)
//
// Compares the PUBLIC API of the current 6.0 SDK build against the last stable
// 5.x release and REPORTS removed / binary-incompatible public API so a human
// can review intentional vs. accidental breaks. The 6.0 line intentionally
// changes some APIs, so this runs in REPORT mode by default and does NOT fail
// the build. Pass `-PapiDiffStrict` to make it fail on binary-incompatible
// changes (e.g. to wire a blocking CI check once the API is frozen).
//
// Mechanism: resolve the baseline AAR from Maven Central into a detached
// configuration, build the current AAR, extract `classes.jar` from both
// (AARs are zips), then run the self-contained japicmp CLI fat jar via
// JavaExec. The CLI runs in its own JVM and needs no plugin classpath
// injection, which keeps it robust on AGP 8.12.2 / Gradle 8.13.
// ---------------------------------------------------------------------------
run {
    // Last stable 5.x release on Maven Central used as the compatibility baseline.
    val apiBaselineCoordinates = "io.branch.sdk.android:library:5.21.1"
    val japicmpVersion = "0.23.1"
    val apiDiffReportDir = layout.buildDirectory.dir("reports/api-diff")

    // Detached, non-transitive configuration: we only want the baseline AAR
    // itself (its classes.jar), not its dependency graph.
    val apiBaseline = configurations.create("apiBaseline") {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }
    // Detached configuration holding the japicmp CLI fat jar.
    val japicmpCli = configurations.create("japicmpCli") {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }
    dependencies {
        add(apiBaseline.name, "$apiBaselineCoordinates@aar")
        add(japicmpCli.name, "com.github.siom79.japicmp:japicmp:$japicmpVersion:jar-with-dependencies")
    }

    // Extract classes.jar from the resolved baseline AAR -> build/api-diff/baseline-classes.jar
    val extractBaselineApiJar = tasks.register<Copy>("extractBaselineApiJar") {
        group = "verification"
        description = "Extracts classes.jar from the baseline ($apiBaselineCoordinates) AAR."
        from({ zipTree(apiBaseline.singleFile) }) { include("classes.jar") }
        into(apiDiffReportDir.map { it.dir("baseline") })
        rename { "baseline-classes.jar" }
    }

    // Extract classes.jar from the freshly built current AAR -> build/api-diff/current-classes.jar
    val extractCurrentApiJar = tasks.register<Copy>("extractCurrentApiJar") {
        group = "verification"
        description = "Extracts classes.jar from the current build's release AAR."
        dependsOn("bundleReleaseAar")
        // The release AAR is the single output of bundleReleaseAar.
        from({ zipTree(tasks.named("bundleReleaseAar").get().outputs.files.singleFile) }) {
            include("classes.jar")
        }
        into(apiDiffReportDir.map { it.dir("current") })
        rename { "current-classes.jar" }
    }

    tasks.register<JavaExec>("apiCompatibilityReport") {
        group = "verification"
        description =
            "EMT-3877: reports removed / binary-incompatible PUBLIC API vs the last stable 5.x release " +
                "($apiBaselineCoordinates). Report mode by default; pass -PapiDiffStrict to fail on breaks."
        dependsOn(extractBaselineApiJar, extractCurrentApiJar)

        val baselineJar = apiDiffReportDir.map { it.file("baseline/baseline-classes.jar") }
        val currentJar = apiDiffReportDir.map { it.file("current/current-classes.jar") }
        val textReport = apiDiffReportDir.map { it.file("api-diff.txt") }
        val htmlReport = apiDiffReportDir.map { it.file("api-diff.html") }
        val strict = project.hasProperty("apiDiffStrict")

        inputs.files(baselineJar, currentJar)
        outputs.files(textReport, htmlReport)

        classpath = japicmpCli
        mainClass.set("japicmp.JApiCmp")

        // The library's public classes live under io.branch; restrict the diff to
        // those (drops noise from bundled/optional deps). --ignore-missing-classes
        // is required because the AAR's classes.jar does not carry its transitive
        // dependencies, so referenced superclasses/interfaces are absent.
        argumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "-o", baselineJar.get().asFile.absolutePath,
                "-n", currentJar.get().asFile.absolutePath,
                "-a", "public",                 // only public/protected API surface
                "--only-incompatible",          // only binary-incompatible changes (removals, signature breaks)
                "--include", "io.branch",        // restrict to the SDK's own packages
                "--ignore-missing-classes",      // transitive deps are not on the classpath
                "--html-file", htmlReport.get().asFile.absolutePath,
            )
        })

        // Capture japicmp's stdout to the text report while still printing it
        // to the console (tee).
        doFirst {
            apiDiffReportDir.get().asFile.mkdirs()
            standardOutput = TeeOutputStream(System.out, textReport.get().asFile.outputStream())
        }

        // Strict mode (opt-in): make japicmp exit non-zero on binary breaks so the
        // build fails. Default (report mode) ignores the exit code and only reports.
        if (strict) {
            args("--error-on-binary-incompatibility")
            isIgnoreExitValue = false
        } else {
            isIgnoreExitValue = true
        }

        doLast {
            // Close the tee's file stream: Gradle's JavaExec does not close a
            // user-provided standardOutput, so the FileOutputStream would leak a
            // handle each run in the daemon. TeeOutputStream.close() closes only
            // the file stream, never System.out.
            (standardOutput as? TeeOutputStream)?.close()
            logger.lifecycle("")
            logger.lifecycle("[EMT-3877] Public API diff vs $apiBaselineCoordinates")
            logger.lifecycle("  text report: ${textReport.get().asFile}")
            logger.lifecycle("  html report: ${htmlReport.get().asFile}")
            if (!strict) {
                logger.lifecycle("  mode: REPORT (build not failed). Re-run with -PapiDiffStrict to fail on breaks.")
            }
        }
    }
}
