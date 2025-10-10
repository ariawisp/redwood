package dev.gobley.wit

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

open class WitExtension {
  var world: String = ""
  var packageName: String = ""
  var witDir: String = "wit"
  var sourceSet: String = "wasmJsMain"
}

abstract class WitGenerateKotlinTask : org.gradle.api.DefaultTask() {
  @org.gradle.api.tasks.Input
  lateinit var world: String

  @org.gradle.api.tasks.Input
  lateinit var packageName: String

  @org.gradle.api.tasks.InputDirectory
  lateinit var witDir: File

  @org.gradle.api.tasks.OutputDirectory
  lateinit var outputDir: File

  @org.gradle.api.tasks.TaskAction
  fun generate() {
    if (!witDir.exists()) {
      throw IllegalStateException("WIT dir not found: ${witDir}")
    }
    outputDir.mkdirs()

    // Prefer a prebuilt wit-bindgen binary via WIT_BINDGEN_BIN, otherwise run via cargo from the sibling repo.
    val envBin = System.getenv("WIT_BINDGEN_BIN")
    val bindgenCmd: List<String>
    if (!envBin.isNullOrBlank()) {
      bindgenCmd = listOf(envBin, "kotlin", "--generate-stubs", "--out-dir", outputDir.absolutePath, "--world", world, witDir.absolutePath)
    } else {
      // Run cargo for the local repo at ../../wit-bindgen
      val repoRoot = project.rootProject.rootDir.toPath().resolve("../wit-bindgen").normalize()
      val manifest = repoRoot.resolve("Cargo.toml").toFile()
      if (!manifest.exists()) {
        throw IllegalStateException("Could not find wit-bindgen repo at $repoRoot; set WIT_BINDGEN_BIN to a built binary")
      }
      bindgenCmd = listOf(
        "cargo", "run", "--quiet",
        "--manifest-path", manifest.absolutePath,
        "--features", "kotlin",
        "--", "kotlin", "--generate-stubs",
        "--out-dir", outputDir.absolutePath,
        "--world", world,
        witDir.absolutePath
      )
    }

    project.logger.lifecycle("[dev.gobley.wit] Running: ${bindgenCmd.joinToString(" ")}")
    val pb = ProcessBuilder(bindgenCmd)
      .directory(project.rootProject.rootDir)
      .redirectErrorStream(true)
    val proc = pb.start()
    val out = proc.inputStream.bufferedReader().readText()
    val ok = proc.waitFor(180, TimeUnit.SECONDS)
    if (!ok || proc.exitValue() != 0) {
      project.logger.error(out)
      throw RuntimeException("wit-bindgen failed (exit=${if (ok) proc.exitValue() else "timeout"})")
    } else {
      project.logger.info(out)
    }
  }
}

class WitPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val ext = target.extensions.create<WitExtension>("wit")

    target.afterEvaluate {
      // Resolve source set Kotlin dir to attach generated sources.
      val sourceSetName = ext.sourceSet
      val genDir = File(buildDir, "generated/wit/kotlin/$sourceSetName")
      val witDir = File(project.projectDir, ext.witDir)

      val task: TaskProvider<WitGenerateKotlinTask> = tasks.register("witGenerateKotlin${sourceSetName.replaceFirstChar { it.uppercase() }}", WitGenerateKotlinTask::class) {
        group = "gobley-wit"
        description = "Generate Kotlin bindings from WIT for $sourceSetName"
        this.world = ext.world
        this.packageName = ext.packageName
        this.witDir = witDir
        this.outputDir = genDir
      }

      // Attach to Kotlin MPP source set if present.
      // We don't depend on kotlin plugin types to keep plugin lean; wire via conventional path.
      project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
        // Attach generated dir to the requested Kotlin source set using typed KGP API
        project.extensions.configure<KotlinMultiplatformExtension>("kotlin") {
          val ss = sourceSets.findByName(sourceSetName)
          if (ss != null) {
            ss.kotlin.srcDir(genDir)
          } else {
            project.logger.warn("[dev.gobley.wit] Source set '$sourceSetName' not found; generated sources at ${genDir} not attached")
          }
        }
      }

      // Ensure codegen runs before Kotlin compilation of the target source set when present.
      val targetSuffix = sourceSetName.removeSuffix("Main").replaceFirstChar { it.uppercase() }
      val compileTaskName = "compileKotlin$targetSuffix"
      tasks.matching { it.name == compileTaskName }.configureEach { dependsOn(task) }
    }
  }
}
