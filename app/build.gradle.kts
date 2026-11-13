plugins {
    // AGP 9 compiles Kotlin itself, so there is no separate kotlin-android plugin.
    alias(libs.plugins.android.application)
    // The Compose compiler is a Kotlin compiler plugin; its version tracks Kotlin's.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "uk.howapped.countusdown"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.howapped.countusdown"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Lets Robolectric see res/ (strings, manifest) in JVM unit tests.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // The BOM pins every Compose library to versions that are tested together.
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(composeBom)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
