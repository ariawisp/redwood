package app.cash.redwood.ui.basic.gpui

/** Native GPUI node handle created by the Rust bridge (via UniFFI/cinterop). */
internal typealias GpuiHandle = Long

/** Minimal surface for GPUI calls used by the backend. Implement with cinterop. */
internal interface GpuiBridge {
  // Creation
  fun createText(): GpuiHandle
  fun createButton(): GpuiHandle
  fun createImage(): GpuiHandle
  fun createRow(): GpuiHandle
  fun createColumn(): GpuiHandle
  fun destroy(node: GpuiHandle)

  // Hierarchy
  fun appendChild(parent: GpuiHandle, child: GpuiHandle)
  fun insertChild(parent: GpuiHandle, index: Int, child: GpuiHandle)
  fun removeChild(parent: GpuiHandle, child: GpuiHandle)

  // Common styles
  fun setPadding(node: GpuiHandle, left: Float, top: Float, right: Float, bottom: Float)
  fun setSize(node: GpuiHandle, widthPx: Float?, heightPx: Float?)
  fun setSpacing(node: GpuiHandle, gapPx: Float)
  fun setAlign(node: GpuiHandle, main: Int, cross: Int)

  // Text
  fun setText(node: GpuiHandle, text: String)
  fun setTextStyle(
    node: GpuiHandle,
    colorRgba: Int,
    fontSizePx: Float,
    weight: Float,
    italic: Boolean,
    lineHeightPx: Float?
  )

  // Button
  fun setButtonText(node: GpuiHandle, text: String)
  fun setButtonEnabled(node: GpuiHandle, enabled: Boolean)
  fun setButtonOnClick(node: GpuiHandle, token: Long) // token identifies callback

  // Image
  fun setImageUrl(node: GpuiHandle, url: String)
  fun setImageFit(node: GpuiHandle, fit: Int) // 0=Fill,1=Contain,2=Cover
  fun setImageRadius(node: GpuiHandle, radiusPx: Float)
}

/** To be wired to your cinterop/UniFFI implementation at init time. */
internal lateinit var gpui: GpuiBridge

