package app.cash.redwood.ui.basic.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.ui.basic.widget.Text

internal class GpuiText(
  override val value: GpuiHandle,
) : Text<GpuiHandle> {
  override var modifier: Modifier = Modifier

  override fun text(text: String) {
    gpui.setText(value, text)
  }
}

