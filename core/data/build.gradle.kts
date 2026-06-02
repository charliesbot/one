import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.androidx.room)
  alias(libs.plugins.ksp)
  alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
  namespace = "com.charliesbot.shared.core.data"
  compileSdk = 37

  defaultConfig { minSdk = 30 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { buildConfig = true }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

room { schemaDirectory("$projectDir/schemas") }

dependencies {
  implementation(project(":core"))
  implementation(project(":core:domain"))
  implementation(project(":core:model"))
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)
  implementation(platform(libs.kotlinx.coroutines.bom))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.play.services.wearable)
  implementation(libs.kotlinx.serialization.json)
  ksp(libs.androidx.room.compiler)
}
