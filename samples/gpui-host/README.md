# GPUI Redwood Sample

This Kotlin/Native executable shows the `:redwood-host-gpui` bindings in action. It starts a
macOS GPUI window, wires up the GPUI-backed layout and UI-basic widget factories, and renders a
small interactive view (text, text field, and button).

```bash
./gradlew :samples:gpui-host:runReleaseExecutableMacosArm64
```

Keep in mind that the current GPUI bridge only targets macOS. Linux and Windows support depend on
GPUI adding the requisite backends.
