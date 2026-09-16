plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kingzulu.biblepresentation"
    compileSdk = 36

    defaultConfig {
        // Keep this ID permanently stable so new APKs update the installed King Zulu app.
        applicationId = "com.kingzulu.biblepresentation"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        // Supply with -PYOUVERSION_APP_KEY=... locally/CI. Never commit the credential.
        val yvKey = providers.gradleProperty("YOUVERSION_APP_KEY").orElse("")
        buildConfigField("String", "YOUVERSION_APP_KEY", "\"${yvKey.get()}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.android.gms:play-services-cast-framework:22.3.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
