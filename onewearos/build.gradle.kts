import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
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

// Support for runtime version override from CI
// Wear OS uses versionCode + 1 to avoid conflicts with phone app in Play Store
val defaultVersionCode = SimpleDateFormat("yyMMdd").format(Date())
val versionCodeProperty: String = (project.findProperty("versionCode") as String?) ?: defaultVersionCode
val versionNameProperty: String = (project.findProperty("versionName") as String?) ?: "1.$defaultVersionCode-dev"

android {
    namespace = "com.charliesbot.onewearos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.charliesbot.one"
        minSdk = 33
        targetSdk = 35
        versionCode = versionCodeProperty.toInt() + 1  // Wear OS gets +1 to avoid Play Store conflict
        versionName = versionNameProperty

    }

    signingConfigs {
        create("release") {
            try {
                val storePath = keystoreProperties.getProperty("storeFile")
                val fileObj = file(storePath)
                if (fileObj.exists()) {
                    storeFile = fileObj
                } else {
                    val userHome = System.getProperty("user.home")
                    storeFile = file("$userHome/.android/$storePath")
                }
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
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "One Dev")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            resValue("string", "app_name", "One")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    firebaseCrashlytics {
        nativeSymbolUploadEnabled = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

ksp {
    arg("KOIN_CONFIG_CHECK", "true")
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
    implementation(libs.androidx.compose.navigation)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.compose.layout)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.androidx.wear.phone.interactions)
    implementation(libs.androidx.wear.ongoing)
    implementation(libs.androidx.startup.runtime)
    implementation(project(":shared"))
    ksp(libs.androidx.room.compiler)
    implementation(libs.firebase.crashlytics)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}