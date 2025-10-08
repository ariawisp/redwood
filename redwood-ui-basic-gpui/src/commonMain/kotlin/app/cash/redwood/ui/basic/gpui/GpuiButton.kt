package app.cash.redwood.ui.basic.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.ui.basic.widget.Button

internal class GpuiButton(
  override val value: GpuiHandle,
  private val onClickRegistry: OnClickRegistry,
) : Button<GpuiHandle> {
  override var modifier: Modifier = Modifier

  override fun text(text: String) {
    gpui.setButtonText(value, text)
  }

  override fun enabled(enabled: Boolean) {
    gpui.setButtonEnabled(value, enabled)
  }

  override fun onClick(onClick: () -> Unit) {
    val token = onClickRegistry.register(onClick)
    gpui.setButtonOnClick(value, token)
  }
}

internal class OnClickRegistry {
  private val map = mutableMapOf<Long, () -> Unit>()
  private var next = 1L

  fun register(cb: () -> Unit): Long {
    val token = next++
    map[token] = cb
    return token
  }

  fun invoke(token: Long) {
    map[token]?.invoke()
  }

  fun clear() = map.clear()
}

