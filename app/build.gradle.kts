plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hesham0_0.marassel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hesham0_0.marassel"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.hesham0_0.marassel.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
            "META-INF/NOTICE.md",
            "META-INF/gradle/incremental.annotation.processors"
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// ── Dependencies ──────────────────────────────────────────────────────────────
dependencies {

    // Compose (BOM manages all versions within the bundle)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.google.googleid)
    implementation(libs.junit.ktx)
    implementation(libs.androidx.work.testing)
    implementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Hilt — DI
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.compiler)

    // Firebase (BOM manages all Firebase versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Credential Manager — Google Sign-In
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // Coil — image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // UI Controller
    implementation(libs.accompanist.systemuicontroller)

    // ── Unit Tests ────────────────────────────────────────────────────────────
    testImplementation(libs.bundles.testing.unit)

    // ── Android / Instrumentation Tests ──────────────────────────────────────
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.work.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    // JUnit 4 runner
    testImplementation("junit:junit:4.13.2")

    // Kotlin coroutines test utilities (runTest, TestDispatcher, etc.)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // MockK — Kotlin-first mocking: coEvery, coVerify, slot, Ordering
    testImplementation("io.mockk:mockk:1.13.12")

    // Turbine — structured Flow testing (await, cancel, etc.)
    testImplementation("app.cash.turbine:turbine:1.1.0")

    // Robolectric — JVM Android stubs for WorkDataUtils (android.net.Uri)
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")

    // DataStore (available to test via direct Preferences construction)
    testImplementation("androidx.datastore:datastore-preferences:1.1.1")

    // ════════════════════════════════════════════════════════════════════════
    // Instrumented + UI Tests  (androidTest/)
    // ════════════════════════════════════════════════════════════════════════

    // JUnit 4 runner for instrumented tests
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")

    // Kotlin coroutines test
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // MockK for Android (instrumented)
    androidTestImplementation("io.mockk:mockk-android:1.13.12")

    // WorkManager testing — TestListenableWorkerBuilder
    androidTestImplementation("androidx.work:work-testing:2.9.1")

    // Compose UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Hilt testing (if using @HiltAndroidTest for future E2E tests)
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
}