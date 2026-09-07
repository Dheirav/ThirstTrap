plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Plain Kotlin/JVM, deliberately — NOT com.android.library.
 *
 * The Android SDK is not on this module's classpath, so `android.*`, `Context`,
 * `Bitmap` and Room annotations cannot leak in. The boundary is enforced by the
 * compiler rather than by review, which is what makes a later Kotlin
 * Multiplatform move a configuration change instead of a rewrite
 * (docs/HANDOVER.md decision D2).
 *
 * It also means these tests run on the JVM in about a second with no device and
 * no emulator — which is what makes it cheap enough to run the drying-curve
 * suite on every change.
 */

kotlin {
    jvmToolchain(17)
}

dependencies {
    // api, not implementation: PlantRepository exposes Flow in its signatures,
    // so every consumer needs it on their compile classpath too.
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    testLogging { events("passed", "skipped", "failed") }
}
