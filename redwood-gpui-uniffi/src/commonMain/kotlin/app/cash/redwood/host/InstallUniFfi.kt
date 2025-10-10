package app.cash.redwood.host

import app.cash.redwood.ui.basic.gpui.GpuiBridge
import app.cash.redwood.ui.basic.gpui.GpuiHandle
import app.cash.redwood.ui.basic.gpui.gpui
import redwood_gpui_bridge.*

/**
 * Install a UniFFI-backed GPUI bridge implementation.
 * This sets the global `gpui` used by Redwood GPUI backends.
 */
public fun installUniFfiGpuiBridge() {
  gpui = UniFfiGpuiBridge
}

private object UniFfiGpuiBridge : GpuiBridge {
  private var nextHandle = 10_000L
  private fun newHandle(): Long = nextHandle++

  private fun apply(changes: List<RedwoodChangeRec>, strings: List<String> = emptyList()) {
    val frame = RedwoodFrameRec(strings = strings, changes = changes)
    redwood_apply(1uL, frame)
  }

  override fun createText(): GpuiHandle {
    val id = newHandle()
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.Create,
      create = RedwoodChangeCreate(id = id.toULong(), widget = RedwoodWidget.Text),
      destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = null, set_image_url = null,
    )))
    return id
  }

  override fun createButton(): GpuiHandle {
    val id = newHandle()
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.Create,
      create = RedwoodChangeCreate(id = id.toULong(), widget = RedwoodWidget.Button),
      destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = null, set_image_url = null,
    )))
    return id
  }

  override fun createImage(): GpuiHandle {
    val id = newHandle()
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.Create,
      create = RedwoodChangeCreate(id = id.toULong(), widget = RedwoodWidget.Image),
      destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = null, set_image_url = null,
    )))
    return id
  }

  override fun createRow(): GpuiHandle {
    val id = newHandle()
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.Create,
      create = RedwoodChangeCreate(id = id.toULong(), widget = RedwoodWidget.Row),
      destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = null, set_image_url = null,
    )))
    return id
  }

  override fun createColumn(): GpuiHandle {
    val id = newHandle()
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.Create,
      create = RedwoodChangeCreate(id = id.toULong(), widget = RedwoodWidget.Column),
      destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = null, set_image_url = null,
    )))
    return id
  }

  override fun destroy(node: GpuiHandle) {
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.Destroy,
      create = null, destroy = RedwoodChangeDestroy(id = node.toULong()),
      append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = null, set_image_url = null,
    )))
  }

  override fun appendChild(parent: GpuiHandle, child: GpuiHandle) {
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.AppendChild,
      create = null, destroy = null, append_child = RedwoodChangeAppendChild(parent.toULong(), child.toULong()),
      insert_child = null, remove_child = null, set_text = null, set_enabled = null, set_image_url = null,
    )))
  }

  override fun insertChild(parent: GpuiHandle, index: Int, child: GpuiHandle) {
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.InsertChild,
      create = null, destroy = null, append_child = null, insert_child = RedwoodChangeInsertChild(parent.toULong(), index.toUInt(), child.toULong()),
      remove_child = null, set_text = null, set_enabled = null, set_image_url = null,
    )))
  }

  override fun removeChild(parent: GpuiHandle, child: GpuiHandle) {
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.RemoveChild,
      create = null, destroy = null, append_child = null, insert_child = null,
      remove_child = RedwoodChangeRemoveChild(parent.toULong(), child.toULong()), set_text = null, set_enabled = null, set_image_url = null,
    )))
  }

  override fun setPadding(node: GpuiHandle, left: Float, top: Float, right: Float, bottom: Float) {
    // Not implemented in preview bridge
  }

  override fun setSize(node: GpuiHandle, widthPx: Float?, heightPx: Float?) {
    // Not implemented in preview bridge
  }

  override fun setSpacing(node: GpuiHandle, gapPx: Float) {
    // Not implemented in preview bridge
  }

  override fun setAlign(node: GpuiHandle, main: Int, cross: Int) {
    // Not implemented in preview bridge
  }

  override fun setText(node: GpuiHandle, text: String) {
    val strings = listOf(text)
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.SetText,
      create = null, destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = RedwoodChangeSetText(node.toULong(), 0u), set_enabled = null, set_image_url = null,
    )), strings)
  }

  override fun setTextStyle(node: GpuiHandle, colorRgba: Int, fontSizePx: Float, weight: Float, italic: Boolean, lineHeightPx: Float?) {
    // Not implemented in preview bridge
  }

  override fun setButtonText(node: GpuiHandle, text: String) {
    // For now, reuse SetText on button
    setText(node, text)
  }

  override fun setButtonEnabled(node: GpuiHandle, enabled: Boolean) {
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.SetEnabled,
      create = null, destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = RedwoodChangeSetEnabled(node.toULong(), enabled), set_image_url = null,
    )))
  }

  override fun setButtonOnClick(node: GpuiHandle, token: Long) {
    // Not implemented in preview bridge
  }

  override fun setImageUrl(node: GpuiHandle, url: String) {
    val strings = listOf(url)
    apply(listOf(RedwoodChangeRec(
      kind = RedwoodChangeKind.SetImageUrl,
      create = null, destroy = null, append_child = null, insert_child = null, remove_child = null,
      set_text = null, set_enabled = null, set_image_url = RedwoodChangeSetImageUrl(node.toULong(), 0u),
    )), strings)
  }

  override fun setImageFit(node: GpuiHandle, fit: Int) {
    // Not implemented in preview bridge
  }

  override fun setImageRadius(node: GpuiHandle, radiusPx: Float) {
    // Not implemented in preview bridge
  }
}

