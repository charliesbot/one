import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.isFile) {
    FileInputStream(keystorePropertiesFile).use {
        keystoreProperties.load(it)
    }
}

android {
    namespace = "com.charliesbot.onewearos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.charliesbot.one"
        minSdk = 33
        targetSdk = 35
        versionCode = 7
        versionName = "1.0"

    }

    signingConfigs {
        create("release") {
            try {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } catch (e: Exception) {
                println("Warning: Keystore properties not found or invalid. Release signing may fail.")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    firebaseCrashlytics {
        nativeSymbolUploadEnabled = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.core.coroutines)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.annotations)
    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.tiles)
    implementation(libs.androidx.tiles.material)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.tiles.tooling.preview)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.androidx.wear.phone.interactions)
    implementation(libs.androidx.startup.runtime)
    implementation(project(":shared"))
    implementation(libs.firebase.crashlytics)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.tiles.tooling)
}