plugins {
  kotlin("multiplatform")
}

kotlin {
  // macOS ARM64 dev target; add others later as needed.
  macosArm64 {
    binaries {
      sharedLib { baseName = "redwood_host_gpui" }
      staticLib { baseName = "redwood_host_gpui" }
    }
    compilations.getByName("main") {
      cinterops {
        val hostapi by creating {
          defFile(project.file("src/nativeInterop/cinterop/hostapi.def"))
          headers(project.file("include/redwood_host.h"))
          includeDirs(project.file("include"))
        }
      }
    }
  }

  sourceSets {
    val nativeMain by getting {
      dependencies {
        api(projects.redwoodProtocolHost)
        api(projects.testApp.schema.widget)
        api(projects.redwoodLayoutApi)
        api(projects.redwoodUiBasicWidget)
        api(projects.redwoodUiCoreWidget)
        api(projects.redwoodLayoutWidget)
        api(projects.redwoodLazylayoutWidget)
        api(projects.redwoodUiBasicGpui)
        api(projects.redwoodLayoutGpui)
        api(projects.redwoodLazylayoutGpui)
        api(projects.testApp.schema.protocolHost)
      }
    }
    val nativeTest by getting
  }
}

// Export helper: copy the release static lib + header into the Zed repo's vendor dir for local dev.
// This assumes the workspace layout where `redwood/` and `zed/` are siblings under the same root.
val workspaceRoot = rootProject.rootDir.parentFile
val zedVendorDir = File(workspaceRoot, "zed/vendor/redwood_host/macos-arm64")

tasks.register<Copy>("exportToZedVendor") {
  group = "distribution"
  description = "Copies libredwood_host_gpui.a and header into zed/vendor/redwood_host/macos-arm64"
  // Ensure the static lib exists
  dependsOn("linkReleaseStaticMacosArm64")

  from(layout.buildDirectory.file("bin/macosArm64/releaseStatic/libredwood_host_gpui.a"))
  from(project.file("include/redwood_host.h"))
  into(zedVendorDir)
}
