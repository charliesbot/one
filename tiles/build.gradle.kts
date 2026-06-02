plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.charliesbot.onewearos.tiles"
  compileSdk = 37

  defaultConfig { minSdk = 33 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

kotlin { jvmToolchain(11) }

dependencies {
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.tiles)
  implementation(libs.androidx.tiles.material)
  implementation(libs.androidx.protolayout)
  implementation(libs.androidx.protolayout.material)
  implementation(libs.play.services.wearable)
  implementation(project(":core"))
  compileOnly(platform(libs.androidx.compose.bom))
  compileOnly(libs.androidx.ui.graphics)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockk)
}
