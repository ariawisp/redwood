plugins {
  kotlin("multiplatform")
}

kotlin {
  // Pure common for now; platform-specific wiring comes via the GPUI bridge at runtime
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":redwood-ui-basic-api"))
      }
    }
    val commonTest by getting
  }
}

