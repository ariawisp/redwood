package app.cash.redwood.ui.basic.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.ui.basic.widget.ContentScale
import app.cash.redwood.ui.basic.widget.Image

internal class GpuiImage(
  override val value: GpuiHandle,
) : Image<GpuiHandle> {
  override var modifier: Modifier = Modifier

  override fun url(url: String) {
    gpui.setImageUrl(value, url)
  }

  override fun contentScale(mode: ContentScale) {
    val fit = when (mode) {
      ContentScale.Fill -> 0
      ContentScale.Contain -> 1
      ContentScale.Cover -> 2
    }
    gpui.setImageFit(value, fit)
  }

  override fun cornerRadius(px: Float) {
    gpui.setImageRadius(value, px)
  }
}

