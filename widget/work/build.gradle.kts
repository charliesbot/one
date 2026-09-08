plugins { alias(libs.plugins.android.library) }

android {
  namespace = "com.charliesbot.one.widget.work"
  compileSdk = 37
  defaultConfig { minSdk = 31 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin { jvmToolchain(11) }

dependencies {
  implementation(project(":widget:common"))
  implementation(libs.androidx.work.runtime.ktx)
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)

  testImplementation(libs.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.kotlinx.coroutines.test)
}
