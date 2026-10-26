plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.s.sync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.s.sync"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "2.1.4"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies { }