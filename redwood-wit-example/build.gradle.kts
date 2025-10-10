plugins {
  kotlin("multiplatform")
  id("dev.gobley.wit")
}

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

kotlin {
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmWasi()

  sourceSets {
    val commonMain by getting {
      dependencies { }
    }
    val wasmWasiMain by getting {
      dependencies { }
    }
    val commonTest by getting
  }
}

wit {
  world = "example"
  packageName = "dev.example.witbindings"
  witDir = "wit"
  sourceSet = "wasmWasiMain"
}
