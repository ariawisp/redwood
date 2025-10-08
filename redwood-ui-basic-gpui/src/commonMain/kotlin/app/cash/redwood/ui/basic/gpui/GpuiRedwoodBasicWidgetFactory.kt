package app.cash.redwood.ui.basic.gpui

import app.cash.redwood.ui.basic.modifier.Reuse
import app.cash.redwood.ui.basic.widget.Button
import app.cash.redwood.ui.basic.widget.Image
import app.cash.redwood.ui.basic.widget.RedwoodUiBasicWidgetFactory
import app.cash.redwood.ui.basic.widget.Text
import app.cash.redwood.ui.basic.widget.TextInput

/**
 * Basic widget factory for the GPUI backend. The generic W type is a native handle to a GPUI node.
 */
public class GpuiRedwoodBasicWidgetFactory : RedwoodUiBasicWidgetFactory<GpuiHandle> {
  private val onClickRegistry = OnClickRegistry()

  override fun Text(): Text<GpuiHandle> {
    val h = gpui.createText()
    return GpuiText(h)
  }

  override fun Button(): Button<GpuiHandle> {
    val h = gpui.createButton()
    return GpuiButton(h, onClickRegistry)
  }

  override fun Image(): Image<GpuiHandle> {
    val h = gpui.createImage()
    return GpuiImage(h)
  }

  override fun TextInput(): TextInput<GpuiHandle> {
    // Minimal placeholder; properties are no-ops for now.
    return object : TextInput<GpuiHandle> {
      override val value: GpuiHandle = 0L
      override var modifier = app.cash.redwood.Modifier
      override fun hint(hint: String?) {}
      override fun onChange(onChange: ((app.cash.redwood.ui.basic.api.TextFieldState) -> Unit)?) {}
      override fun state(state: app.cash.redwood.ui.basic.api.TextFieldState) {}
      override val allChildren: List<app.cash.redwood.widget.Widget.Children<GpuiHandle>> = emptyList()
    }
  }

  override fun Reuse(value: GpuiHandle, modifier: Reuse) {
    // No-op; handled by HostProtocolAdapter reuse pool.
  }
}
