package app.cash.redwood.host.gpui

import app.cash.redwood.Modifier

public class GpuiNode(
  internal val handle: RedwoodNodeHandle,
  private val onRequestFocus: (() -> Boolean)? = null,
) {
  public var modifier: Modifier = Modifier

  public fun markNeedsLayout() {
    runCatching {
      handle.markNeedsLayout()
    }
  }

  public fun dispose() {
    runCatching {
      handle.dispose()
    }
  }

  public fun requestFocus(): Boolean {
    return onRequestFocus?.invoke() == true
  }
}
