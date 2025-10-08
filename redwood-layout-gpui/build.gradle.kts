plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":redwood-layout-api"))
      }
    }
    val commonTest by getting
  }
}

