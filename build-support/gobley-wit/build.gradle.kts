plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

group = "dev.gobley"
version = "0.1.0-SNAPSHOT"

gradlePlugin {
  plugins {
    create("gobleyWit") {
      id = "dev.gobley.wit"
      implementationClass = "dev.gobley.wit.WitPlugin"
      displayName = "Gobley WIT Codegen Plugin"
      description = "Generates Kotlin/Wasm sources from WIT definitions and wires them into source sets"
    }
  }
}

repositories {
  // Resolve Kotlin fork from the workspace sibling kotlin/build/repo if present
  maven { url = uri("../../../kotlin/build/repo") }
  mavenLocal()
  mavenCentral()
  google()
}

dependencies {
  implementation(gradleApi())
  implementation(localGroovy())
  // Access Kotlin MPP extension types in our plugin using compileOnly
  val kotlinVersion = providers.gradleProperty("kotlin.version").orNull
    ?: System.getenv("KOTLIN_VERSION")
    ?: "2.3.0-wit.1"
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
