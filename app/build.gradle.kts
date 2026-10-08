import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Release signing, read from local.properties, which is gitignored.
 *
 * Absent means an unsigned release build rather than a broken one: someone who
 * clones this repo has no keystore and should still be able to compile. An
 * unsigned APK will not install, which is the correct and obvious failure.
 *
 * Losing this keystore means never being able to ship an update that upgrades
 * an existing install. For a diary whose data cannot be reconstructed, that is
 * not an inconvenience, so it belongs backed up somewhere that is not this
 * machine.
 */
private val signingProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
private val keystorePath: String? = signingProps.getProperty("releaseKeystore")
private val hasKeystore = keystorePath != null && rootProject.file(keystorePath).exists()

/**
 * The version code, counted off the git history rather than typed in.
 *
 * It has to rise with every build handed out. Play refuses a duplicate code
 * permanently, and a phone will not take an update whose code is not higher
 * than the one it already has. A hand-edited integer failed that twice: the
 * release of 2026-10-01 and the one of 2026-10-08 are different APKs and both
 * claim version 1, so neither can ever update the other.
 *
 * The commit count only grows, so it cannot be forgotten and cannot repeat.
 * The one thing that would break it is rewriting published history, which
 * would make it go backwards; that is already ruled out for this repo.
 *
 * Read through `providers.exec` so the configuration cache stays valid, and
 * falls back to [VERSION_CODE_FLOOR] when there is no git at all, because
 * someone building from a source archive should still get an APK. The floor is
 * deliberately below the real count: a fallback build must never outrank a
 * real one and silently block its update.
 */
private val VERSION_CODE_FLOOR = 1

private val gitVersionCode: Int = try {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        workingDir = rootProject.projectDir
    }.standardOutput.asText.get().trim().toIntOrNull() ?: VERSION_CODE_FLOOR
} catch (_: Exception) {
    VERSION_CODE_FLOOR
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
        versionCode = gitVersionCode
        // Shown in Settings and written into every backup, so it is what a
        // tester will quote back. The code goes next to it there, because the
        // name alone does not say which of several builds they are on.
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = rootProject.file(keystorePath!!)
                storePassword = signingProps.getProperty("releaseStorePassword")
                keyAlias = signingProps.getProperty("releaseKeyAlias")
                keyPassword = signingProps.getProperty("releaseKeyPassword")
            }
        }
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
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        // AGP 8 turns this off by default; the debug menu is gated on it.
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    // The home-screen widget. Glance is Compose for RemoteViews, so the widget
    // is written the same way as the rest of the app rather than in XML.
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.coil.compose)
    implementation(libs.zxing.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
