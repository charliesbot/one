plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.charliesbot.one.widget"
  compileSdk = 37

  defaultConfig { minSdk = 31 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

kotlin { jvmToolchain(11) }

dependencies {
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)
  implementation(libs.androidx.glance)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  implementation(libs.androidx.glance.preview)
  implementation(libs.androidx.glance.appwidget.preview)
  implementation(libs.androidx.core.ktx)
  implementation(project(":core"))
  implementation(project(":core:domain"))
  implementation(project(":core:strings"))
  implementation(project(":widget:common"))

  testImplementation(libs.junit)
}
