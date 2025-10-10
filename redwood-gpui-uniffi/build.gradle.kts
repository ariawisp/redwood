import gobley.gradle.GobleyHost
import org.gradle.api.GradleException

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.kotlin.plugin.atomicfu")
  id("dev.gobley.cargo")
  id("dev.gobley.uniffi")
}

// Point Gobley at the Rust crate in the Zed workspace.
cargo {
  packageDirectory = layout.projectDirectory.dir("../crates/redwood_gpui_bridge")
}

uniffi {
  // Generate Kotlin bindings from the compiled library (macro-based UniFFI).
  generateFromLibrary()
}

kotlin {
  // Minimal native target for local dev. Add more as needed.
  macosArm64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        // Access gpui bridge interface and global from backend module.
        implementation(projects.redwoodUiBasicGpui)
      }
    }
  }
}
