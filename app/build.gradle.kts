plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.nothing.assistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nothing.assistant"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {
    // ── Wear Compose Material 3 (Wear OS version — NOT phone material3) ──
    implementation("androidx.wear.compose:compose-material3:1.5.0")
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-navigation:1.5.0")

    // Wear OS platform (non-Compose helpers, input)
    implementation("androidx.wear:wear:1.4.0")
    implementation("androidx.wear:wear-input:1.2.0")

    // Core AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // ── Room (local chat history) ──
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // ── EncryptedSharedPreferences (API key storage) ──
    implementation("androidx.security:security-crypto:1.1.0")

    // ── Wear Tiles & ProtoLayout (quick-access Tile) ──
    implementation("androidx.wear.tiles:tiles:1.6.1")
    implementation("androidx.wear.protolayout:protolayout:1.4.1")
    implementation("androidx.wear.protolayout:protolayout-material:1.4.1")
    implementation("androidx.wear.protolayout:protolayout-expression:1.4.1")

    // ── OkHttp (lightweight HTTP for Gemini API) ──
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ── Guava (ListenableFuture for TileService) ──
    implementation("com.google.guava:guava:33.4.0-android")

    // ── Kotlin Coroutines ──
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ── JSON parsing (for Gemini response body) ──
    implementation("org.json:json:20240303")
}
