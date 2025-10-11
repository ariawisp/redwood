import app.cash.redwood.buildsupport.TargetGroup

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("app.cash.zipline")
}

redwoodBuild {
  targets(TargetGroup.TreehouseHost)
  publishing()
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.serialization.json)
        api(projects.redwoodTreehouse)
        api(projects.redwoodTreehouseHotreloadGuest)
        api(libs.zipline)
        api(libs.zipline.loader)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.assertk)
        implementation(libs.kotlin.test)
      }
    }
  }
}
