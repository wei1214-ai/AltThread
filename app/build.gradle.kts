plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Use the full JDK installed on this machine.
    jvmToolchain(20)
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
        val openrouterKey: String = (project.findProperty("openrouterApiKey") as String?)
            ?: System.getenv("OPENROUTER_API_KEY") ?: ""
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openrouterKey\"")
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
        // Must match the Kotlin jvmToolchain (20) or Gradle fails with
        // "Inconsistent JVM-target compatibility".
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Keep ONNX models uncompressed so on-device inference can mmap them.
    aaptOptions {
        noCompress += "onnx"
    }
}

dependencies {
    //Crop pics
    implementation("com.github.yalantis:ucrop:2.2.11")
    implementation("androidx.appcompat:appcompat:1.7.1")

    implementation(platform(libs.compose.bom))

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.0.1")

    // Ktor
    implementation("io.ktor:ktor-client-android:3.0.0")

    // Add Kotlinx Serialization JSON library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
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

    // On-device inference: YOLO + SAM 2.1
    implementation(libs.onnxruntime.android)
    implementation(libs.opencv)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
