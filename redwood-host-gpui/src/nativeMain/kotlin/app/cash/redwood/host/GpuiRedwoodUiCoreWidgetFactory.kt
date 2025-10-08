package app.cash.redwood.host

import app.cash.redwood.ui.core.modifier.FocusRequester
import app.cash.redwood.ui.core.widget.RedwoodUiCoreWidgetFactory

internal class GpuiRedwoodUiCoreWidgetFactory : RedwoodUiCoreWidgetFactory<Long> {
  override fun FocusRequester(value: Long, modifier: FocusRequester) {
    // No-op for now.
  }
}

