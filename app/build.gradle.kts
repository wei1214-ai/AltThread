plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Pin compilation to Android Studio's bundled JBR (JDK 21).
    // The IDE-hosted JRE (Trae) is missing jlink.exe, which breaks
    // AGP's JDK image transform. Toolchain 21 resolves to the full JBR.
    jvmToolchain(21)
}

android {
    namespace = "com.example.myapplicationkoG"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplicationkoG"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val backendUrl: String = providers.gradleProperty("BACKEND_BASE_URL")
            .getOrElse("http://10.0.2.2:8000/")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        // Must match the Kotlin jvmToolchain (21) or Gradle fails with
        // "Inconsistent JVM-target compatibility".
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.0.1")

    // Ktor
    implementation("io.ktor:ktor-client-android:2.3.12")

    // Coil
    implementation(libs.coil.compose)

    // Activity & Compose UI
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Navigation
    implementation(libs.navigation.compose)

    // Persistence
    implementation(libs.datastore.core)
    implementation(libs.datastore.preferences)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
