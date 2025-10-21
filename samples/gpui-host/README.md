# GPUI Redwood Sample

This Kotlin/Native executable shows the `:redwood-host-gpui` bindings in action. It starts a GPUI
window, wires up the GPUI-backed layout and UI-basic widget factories, and renders a small
interactive view (text, text field, and button).

```bash
# macOS (Apple Silicon host)
./gradlew :samples:gpui-host:runReleaseExecutableMacosArm64

# macOS (Intel host)
./gradlew :samples:gpui-host:runReleaseExecutableMacosX64

# Linux
./gradlew :samples:gpui-host:runReleaseExecutableLinuxX64

# Windows
./gradlew :samples:gpui-host:runReleaseExecutableMingwX64
```

Linux and Windows builds require running Gradle on the respective platforms so the UniFFI bindings
and Kotlin/Native artifacts can be produced with the native toolchains GPUI expects. Likewise, the
macOS targets are tied to the architecture of the host the build is running on.
