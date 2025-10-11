# Treehouse Hot Reload (WASI)

This documentation describes the development-only hot reload tunnel for Treehouse
applications running inside a WASI/Zipline guest. It complements JetBrains'
JVM-centric Compose Hot Reload by coordinating full module swaps together with
Treehouse `StateSnapshot` capture and restore.

## Modules

The feature lives in two new modules:

* `redwood-treehouse-hotreload-guest`
  * Defines the guest-side bridge contract and utilities for registering it
    with the current `Zipline` instance.
* `redwood-treehouse-hotreload-host`
  * Provides orchestration helpers for hosts, including state management,
    progress tracking, and bridge lookups from a running `Zipline` session.

Both modules are multiplatform and depend only on the existing Treehouse core
(not on platform-specific host code).

## Guest Integration

1. Add a dependency on `redwood-treehouse-hotreload-guest` to the guest module
   that owns your `TreehouseUi`.
2. When wiring services inside the guest (usually in the JS entrypoint where
   `Zipline.bind` is called), register the bridge:

   ```kotlin
   val hotReloadClient = defaultTreehouseHotReloadClient(ziplineTreehouseUi)
   zipline.bindTreehouseHotReload(hotReloadClient)
   ```

   Provide a custom `TreehouseHotReloadClient` if you need additional metadata
   or custom restore behaviour.

## Host Integration

1. Ensure the host module depends on `redwood-treehouse-hotreload-host`. This is
   already wired into `redwood-treehouse-host`.
2. Override the new `TreehouseApp.Spec.hotReloadConfig` property and return a
   `TreehouseHotReloadConfig` when hot reload should be enabled (for example
   behind a development flag).
3. Optionally supply custom `captureRequestFactory` / `stateTracker` instances
   in the config for IDE or tooling integration.

When enabled, the host automatically:

* Captures a `TreehouseHotReloadFrame` before stopping the current Zipline
  session.
* Stores any returned `StateSnapshot` in the shared `StateStore`.
* Restores the frame after the refreshed module is started.

Failures while capturing or restoring fall back to a cold start and surface
through `TreehouseHotReloadStateTracker`.

## Limitations

* State persistence is limited to the data currently supported by
  `StateSnapshot`. Custom snapshotters are required for more complex types.
* Reloads still replace the entire WASI module; they are faster than a full app
  restart but slower than JVM bytecode patching.
* All participating guests must expose the bridge; otherwise reloads silently
  fall back to a cold restart.
* Only single-session coordination is handled today. Multi-window coordination
  will need additional session tracking.

## Next Steps (Future Work)

* IDE automation (Gradle tasks / CLI wrappers) to trigger reloads from source
  changes.
* Richer telemetry and diagnostics surfaced through the state tracker.
* Guest-side helpers for bespoke initialization work during restore.

## Manual Validation Checklist

Run this flow once WASI preview support stabilizes:

1. Launch the host application with `TreehouseHotReloadConfig.enabled = true`.
2. Start the Zipline development server in continuous mode and ensure the WASI
   guest binds the hot reload bridge.
3. Trigger a code change; verify that the host captures a frame, reloads the
   module, and restores previously entered UI state.
4. Introduce a serialization-incompatible change and confirm the state tracker
   surfaces a failure and falls back to a cold restart.
5. Disable the config flag and confirm the old cold-reload workflow still
   operates.
