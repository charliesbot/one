import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.jetbrains.kotlin.serialization)
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

dependencies { implementation(libs.kotlinx.serialization.core) }
