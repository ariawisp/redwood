package app.cash.redwood.host

import app.cash.redwood.Modifier
import app.cash.redwood.widget.Widget
import com.example.redwood.testapp.modifier.BackgroundColor
import com.example.redwood.testapp.widget.Button
import com.example.redwood.testapp.widget.Split
import com.example.redwood.testapp.widget.TestSchemaWidgetFactory
import app.cash.redwood.ui.basic.gpui.gpui

internal class GpuiTestSchemaWidgetFactory : TestSchemaWidgetFactory<Long> {
  override fun TestRow() = throw UnsupportedOperationException()
  override fun ScopedTestRow() = throw UnsupportedOperationException()
  override fun Button(): Button<Long> = GpuiTestSchemaButton()
  override fun Button2() = throw UnsupportedOperationException()
  override fun TextInput() = throw UnsupportedOperationException()
  override fun BackgroundColor(value: Long, modifier: BackgroundColor) {
    // No-op for now.
  }
  override fun Split(): Split<Long> = throw UnsupportedOperationException()
}

internal class GpuiTestSchemaButton : Button<Long> {
  override val value: Long = gpui.createButton()
  override var modifier: Modifier = Modifier
  override val allChildren: List<Widget.Children<Long>> = emptyList()

  override fun text(text: String?) {
    gpui.setButtonText(value, text ?: "")
  }

  override fun onClick(onClick: (() -> Unit)?) {
    HostClickRegistry.register(value, onClick)
  }

  override fun color(color: UInt) {
    // TODO: map to style when available
  }
}

internal object HostClickRegistry {
  private val map = mutableMapOf<Long, (() -> Unit)?>()
  fun register(handle: Long, cb: (() -> Unit)?) {
    if (cb == null) map.remove(handle) else map[handle] = cb
  }
  fun click(handle: Long) {
    map[handle]?.invoke()
  }
}
