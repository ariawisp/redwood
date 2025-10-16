package app.cash.redwood.host.gpui

import app.cash.redwood.Modifier

public class GpuiNode(
  internal val handle: RedwoodNodeHandle,
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
    runCatching {
      handle.destroy()
    }
  }
}
