package app.cash.redwood.ui.basic.gpui

import app.cash.redwood.host.gpui.GpuiEnvironment
import app.cash.redwood.host.gpui.GpuiNode
import app.cash.redwood.layout.gpui.GpuiRedwoodLayoutWidgetFactory
import app.cash.redwood.lazylayout.gpui.GpuiRedwoodLazyLayoutWidgetFactory
import app.cash.redwood.ui.basic.widget.RedwoodUiBasicWidgetSystem

@Suppress("FunctionName")
public fun GpuiRedwoodUiBasicWidgetSystem(
  environment: GpuiEnvironment,
): RedwoodUiBasicWidgetSystem<GpuiNode> {
  return RedwoodUiBasicWidgetSystem(
    RedwoodUiBasic = GpuiRedwoodUiBasicWidgetFactory(environment),
    RedwoodLayout = GpuiRedwoodLayoutWidgetFactory(environment),
    RedwoodLazyLayout = GpuiRedwoodLazyLayoutWidgetFactory(environment),
  )
}

