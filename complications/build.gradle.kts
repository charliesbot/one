plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.charliesbot.onewearos.complications"
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
  implementation(libs.play.services.wearable)
  implementation(libs.androidx.watchface.complications.data.source.ktx)
  implementation(project(":core"))
}
