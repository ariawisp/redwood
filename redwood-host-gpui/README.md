# Redwood GPUI Host

This module exposes Kotlin/Native bindings generated from the `gpui-uniffi` crate so Redwood widget
factories and view hosts can drive a GPUI surface. It currently targets macOS (arm64/x64) builds.

## Getting Started

1. Add the GPUI modules to your multiplatform project:
   * `:redwood-host-gpui` for the UniFFI bindings and high-level helpers
   * `:redwood-layout-gpui` and `:redwood-ui-basic-gpui` for the layout / UI widget factories
2. Create a GPUI application and window via `runGpuiApp { app -> app.createRedwoodView(...) }`.
3. Use the `GpuiRedwoodView.environment` to construct widget factories and populate
   `view.children` with your Redwood widget tree.

A runnable example lives in `samples/gpui-host`.

## Current Limitations

* Only macOS is wired up; GPUI&rsquo;s Linux/Windows backends are not yet exposed through the UniFFI
  layer.
* Box horizontal alignment maps `Stretch` to `Start` until GPUI exposes an explicit stretch mode.
* Box margins are tracked for layout invalidation but do not yet affect GPUI rendering.
* Reuse modifiers are ignored for now.
