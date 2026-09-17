plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kingzulu.biblepresentation"
    compileSdk = 36

    defaultConfig {
        // PERMANENT ID: never change this when renaming/rebranding King Zulu.
        // Android uses applicationId + signing identity to decide whether an APK is an update.
        applicationId = "com.kingzulu.biblepresentation"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2-beta"

        val yvKey = providers.gradleProperty("YOUVERSION_APP_KEY").orElse("")
        buildConfigField("String", "YOUVERSION_APP_KEY", "\"${yvKey.get()}\"")
    }

    // CI can provide one permanent King Zulu keystore through environment variables.
    // The key itself is NEVER committed to GitHub. Local/debug builds continue to work
    // without these values, while distributable beta builds can use the stable identity.
    val releaseStoreFile = System.getenv("KINGZULU_KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("KINGZULU_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("KINGZULU_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("KINGZULU_KEY_PASSWORD")
    val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        if (hasReleaseSigning) {
            create("kingZuluPermanent") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("kingZuluPermanent")
        }
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
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.android.gms:play-services-cast-framework:22.3.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
