package app.cash.redwood.host.gpui

import app.cash.redwood.ui.Density

/**
 * Bundles the GPUI surface handle and the density information required to translate
 * Redwood values into the host coordinate space.
 */
public data class GpuiEnvironment(
  val surface: RedwoodSurfaceHandle,
  val density: Density,
)
