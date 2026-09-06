plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.dheirav.thirsttrap"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.dheirav.thirsttrap"
        // minSdk 26: java.time with no desugaring, notification channels as a
        // first-class concept. The target phone is API 36; see docs/DEVICE.md.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-M0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // The plain debug APK is large enough that installs die mid-transfer
            // over wireless (Luna: 25 MB, 1-3 min, frequent failures; minified,
            // 45 s). Opt in with -PminifyDebug. Use project.hasProperty, not
            // providers.gradleProperty: a bare -PminifyDebug sets an EMPTY
            // string, which isPresent reports as absent.
            val minifyDebug = project.hasProperty("minifyDebug")
            isMinifyEnabled = minifyDebug
            isShrinkResources = minifyDebug
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
