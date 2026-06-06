import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.charliesbot.shared.core.designsystem.common"
  compileSdk = 37

  defaultConfig { minSdk = 31 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

dependencies {
  api(project(":core:model"))
  implementation(project(":core:strings"))
  api(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  api(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.ui.tooling.preview)

  testImplementation(libs.junit)
  testImplementation(libs.mockk)
  debugImplementation(libs.ui.tooling)
}
