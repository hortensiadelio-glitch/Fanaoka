plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "mg.fanaoka.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "mg.fanaoka.app"
        minSdk = 26
        targetSdk = 32
        versionCode = 2
        versionName = "0.1.2"
    }

    signingConfigs {
        create("release") {
            // Values are supplied only by GitHub Actions secrets during tagged releases.
            storeFile = file(System.getenv("FANAOKA_KEYSTORE_PATH") ?: "fanaoka-upload.jks")
            storePassword = System.getenv("FANAOKA_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("FANAOKA_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("FANAOKA_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
