@file:Suppress("GradleDependency", "ExpiredTargetSdkVersion", "OldTargetApi", "UseTomlInstead", "DuplicatePlatformDependency", "DuplicatePlatform", "Deprecation")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.smartcontractai"
    //noinspection GradleDependency
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartcontractai"
        minSdk = 24
        //noinspection GradleDependency
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        disable += "ExpiredTargetSdkVersion"
        disable += "TargetName"
        disable += "OldTargetApi"
        disable += "GradleDependency"
        disable += "UseTomlInstead"
        disable += "DuplicatePlatformDependency"
        disable += "MissingVersion"
        disable += "NewerVersionAvailable"
        disable += "UnusedAttribute"
        disable += "GradleOverrides"
        disable += "RemoveExplicitTypeArguments"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        viewBinding = true
    }
    //noinspection Deprecation
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xsuppress-version-warnings"
        )
    }
}

@Suppress("DuplicatePlatformDependency", "DuplicatePlatform")
dependencies {
    // AndroidX & Core
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.glide)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.inappmessaging.display)

    // Facebook Android SDK
    implementation(libs.facebook.android.sdk)

    // Compose BOM & Core UI
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.runtime.saveable)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.coil.compose)

    // Credentials & Auth
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}