plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.charliesbot.one.widget.wear"
  compileSdk = 37

  defaultConfig { minSdk = 33 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

kotlin { jvmToolchain(11) }

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.glance.wear)
  implementation(libs.androidx.glance.wear.core)
  implementation(libs.androidx.remote.core)
  implementation(libs.androidx.remote.creation.compose)
  implementation(libs.androidx.remote.tooling.preview)
  implementation(libs.androidx.tiles.tooling.preview)
  implementation(libs.androidx.wear.compose.ui.tooling)
  implementation(libs.androidx.wear.remote.material3)
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)
  implementation(project(":widget:common"))
  implementation(project(":core:domain"))
  implementation(project(":core:strings"))

  debugImplementation(libs.androidx.tiles.renderer)

  testImplementation(libs.junit)
}
