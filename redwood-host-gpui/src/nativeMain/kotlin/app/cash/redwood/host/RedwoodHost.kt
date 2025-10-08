package app.cash.redwood.host

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.toKString
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.FloatVar
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.pointed
import kotlinx.cinterop.usePinned
import platform.posix.size_t
import redwood_host_gpui.rwd_gpui_vtable
import app.cash.redwood.protocol.Change
import app.cash.redwood.protocol.host.UiChange
import app.cash.redwood.protocol.host.HostProtocolAdapter
import app.cash.redwood.protocol.host.HostProtocol
import app.cash.redwood.protocol.host.hostRedwoodVersion
import app.cash.redwood.widget.WidgetSystem
import app.cash.redwood.widget.Widget
import app.cash.redwood.ui.basic.widget.RedwoodUiBasicWidgetSystem
import app.cash.redwood.ui.basic.gpui.GpuiRedwoodBasicWidgetFactory
import app.cash.redwood.ui.basic.gpui.GpuiBridge
import app.cash.redwood.ui.basic.gpui.GpuiHandle
import app.cash.redwood.ui.basic.gpui.gpui
import app.cash.redwood.layout.gpui.GpuiRedwoodLayoutWidgetFactory
import app.cash.redwood.lazylayout.gpui.GpuiRedwoodLazyLayoutWidgetFactory
import app.cash.redwood.ui.core.widget.RedwoodUiCoreWidgetSystem
import com.example.redwood.testapp.widget.TestSchemaWidgetSystem
import com.example.redwood.testapp.protocol.host.TestSchemaHostProtocol

// Minimal in-memory host to back the C API. This is a temporary demo decoder, not Redwood protocol.

private object HostState {
  val views = mutableMapOf<Long, ViewSession>()
}

private class ViewSession(
  val rootHandle: Long,
  val widgetSystem: WidgetSystem<Long>,
  val container: Widget.Children<Long>,
  val hostAdapter: HostProtocolAdapter<Long>,
)

@Serializable
private data class DemoChanges(val ops: List<DemoOp>)

@Serializable
private sealed class DemoOp {
  @Serializable @SerialName("create") data class Create(val id: Long, val type: String): DemoOp()
  @Serializable @SerialName("set_text") data class SetText(val id: Long, val text: String): DemoOp()
  @Serializable @SerialName("attach_root") data class AttachRoot(val id: Long): DemoOp()
}

// Placeholder GPUI bridge logs; replace with real vtable calls once registered.
private object GpuiLogBridge {
  fun createText(): Long { log("gpui.createText()"); return next() }
  fun createButton(): Long { log("gpui.createButton()"); return next() }
  fun createImage(): Long { log("gpui.createImage()"); return next() }
  fun appendChild(parent: Long, child: Long) { log("gpui.appendChild($parent, $child)") }
  fun setText(node: Long, text: String) { log("gpui.setText($node, '$text')") }
  fun setButtonText(node: Long, text: String) { log("gpui.setButtonText($node, '$text')") }
  fun setButtonEnabled(node: Long, enabled: Boolean) { log("gpui.setButtonEnabled($node, $enabled)") }
  fun setImageUrl(node: Long, url: String) { log("gpui.setImageUrl($node, '$url')") }

  private var counter = 1000L
  private fun next() = counter++
}

// GPUI vtable (static link model)
private var gpuiVtablePtr: COpaquePointer? = null
private fun vt(): CPointer<rwd_gpui_vtable>? = gpuiVtablePtr?.reinterpret()

private fun gpuiCreateText(): Long {
  val fn = vt()?.pointed?.create_text
  return if (fn != null) fn.invoke() else GpuiLogBridge.createText()
}

private fun gpuiCreateButton(): Long {
  val fn = vt()?.pointed?.create_button
  return if (fn != null) fn.invoke() else GpuiLogBridge.createButton()
}

private fun gpuiCreateImage(): Long {
  val fn = vt()?.pointed?.create_image
  return if (fn != null) fn.invoke() else GpuiLogBridge.createImage()
}

private fun gpuiAppendChild(parent: Long, child: Long) {
  val fn = vt()?.pointed?.append_child
  if (fn != null) fn.invoke(parent, child) else GpuiLogBridge.appendChild(parent, child)
}

private fun gpuiInsertChild(parent: Long, index: Int, child: Long) {
  val fn = vt()?.pointed?.insert_child
  if (fn != null) fn.invoke(parent, index, child) else GpuiLogBridge.appendChild(parent, child)
}

private fun gpuiRemoveChild(parent: Long, child: Long) {
  val fn = vt()?.pointed?.remove_child
  if (fn != null) fn.invoke(parent, child) else { /* ignore in log bridge */ }
}

private fun gpuiSetText(node: Long, text: String) {
  val fn = vt()?.pointed?.set_text
  if (fn != null) {
    val bytes = text.encodeToByteArray()
    bytes.usePinned { pinned ->
      fn.invoke(node, pinned.addressOf(0), bytes.size.toULong())
    }
  } else {
    GpuiLogBridge.setText(node, text)
  }
}

private fun gpuiSetButtonText(node: Long, text: String) {
  val fn = vt()?.pointed?.set_button_text
  if (fn != null) {
    val bytes = text.encodeToByteArray()
    bytes.usePinned { pinned ->
      fn.invoke(node, pinned.addressOf(0), bytes.size.toULong())
    }
  } else {
    GpuiLogBridge.setButtonText(node, text)
  }
}

// STOPGAP: helpers for additional vtable functions. Replace logging once all are wired.
private fun gpuiSetButtonEnabled(node: Long, enabled: Boolean) {
  val fn = vt()?.pointed?.set_button_enabled
  if (fn != null) fn.invoke(node, if (enabled) 1 else 0) else GpuiLogBridge.setButtonEnabled(node, enabled)
}

private fun gpuiSetImageUrl(node: Long, url: String) {
  val fn = vt()?.pointed?.set_image_url
  if (fn != null) {
    val bytes = url.encodeToByteArray()
    bytes.usePinned { pinned -> fn.invoke(node, pinned.addressOf(0), bytes.size.toULong()) }
  } else {
    GpuiLogBridge.setImageUrl(node, url)
  }
}

private fun gpuiSetImageFit(node: Long, fit: Int) {
  val fn = vt()?.pointed?.set_image_fit
  if (fn != null) fn.invoke(node, fit)
}

private fun gpuiSetImageRadius(node: Long, radiusPx: Float) {
  val fn = vt()?.pointed?.set_image_radius
  if (fn != null) fn.invoke(node, radiusPx)
}

private fun gpuiSetPadding(node: Long, l: Float, t: Float, r: Float, b: Float) {
  vt()?.pointed?.set_padding?.invoke(node, l, t, r, b)
}

private fun gpuiSetSize(node: Long, widthPx: Float?, heightPx: Float?) {
  // STOPGAP: pass null sizes; will wire exact pointers when needed.
  vt()?.pointed?.set_size?.invoke(node, null, null)
}

private fun gpuiSetSpacing(node: Long, gapPx: Float) {
  vt()?.pointed?.set_spacing?.invoke(node, gapPx)
}

private fun gpuiSetAlign(node: Long, mainAxis: Int, crossAxis: Int) {
  vt()?.pointed?.set_align?.invoke(node, mainAxis, crossAxis)
}

private fun applyDemoChanges(jsonUtf8: String): Int {
  val changes = Json.decodeFromString(DemoChanges.serializer(), jsonUtf8)
  val handles = mutableMapOf<Long, Long>()
  for (op in changes.ops) when (op) {
    is DemoOp.Create -> {
      val h = when (op.type.lowercase()) {
        "text" -> gpuiCreateText()
        "button" -> gpuiCreateButton()
        "image" -> gpuiCreateImage()
        else -> return 1
      }
      handles[op.id] = h
    }
    is DemoOp.SetText -> {
      val h = handles[op.id] ?: return 1
      gpuiSetText(h, op.text)
    }
    is DemoOp.AttachRoot -> {
      val h = handles[op.id] ?: return 1
      gpuiAppendChild(0L, h)
    }
  }
  return 0
}

// -------------------- C ABI exports --------------------

@Suppress("unused")
@kotlin.native.CName("redwood_host_init")
public fun redwood_host_init(): ULong {
  // Bind the GPUI bridge to the vtable-backed implementation so GPUI-backed
  // widget factories can create/manipulate nodes.
  try {
    gpui = object : GpuiBridge {
      override fun createText(): GpuiHandle = gpuiCreateText()
      override fun createButton(): GpuiHandle = gpuiCreateButton()
      override fun createImage(): GpuiHandle = gpuiCreateImage()
      override fun createRow(): GpuiHandle = vt()?.pointed?.create_row?.invoke() ?: 0L
      override fun createColumn(): GpuiHandle = vt()?.pointed?.create_column?.invoke() ?: 0L
      override fun destroy(node: GpuiHandle) { vt()?.pointed?.destroy?.invoke(node) }
      override fun appendChild(parent: GpuiHandle, child: GpuiHandle) { gpuiAppendChild(parent, child) }
      override fun insertChild(parent: GpuiHandle, index: Int, child: GpuiHandle) { gpuiInsertChild(parent, index, child) }
      override fun removeChild(parent: GpuiHandle, child: GpuiHandle) { gpuiRemoveChild(parent, child) }
      override fun setPadding(node: GpuiHandle, left: Float, top: Float, right: Float, bottom: Float) { vt()?.pointed?.set_padding?.invoke(node, left, top, right, bottom) }
      override fun setSize(node: GpuiHandle, widthPx: Float?, heightPx: Float?) {
        memScoped {
          val w = widthPx?.let { alloc<kotlinx.cinterop.FloatVar>().apply { value = it } }
          val h = heightPx?.let { alloc<kotlinx.cinterop.FloatVar>().apply { value = it } }
          vt()?.pointed?.set_size?.invoke(node, w?.ptr, h?.ptr)
        }
      }
      override fun setSpacing(node: GpuiHandle, gapPx: Float) { vt()?.pointed?.set_spacing?.invoke(node, gapPx) }
      override fun setAlign(node: GpuiHandle, main: Int, cross: Int) { vt()?.pointed?.set_align?.invoke(node, main, cross) }
      override fun setText(node: GpuiHandle, text: String) { gpuiSetText(node, text) }
      override fun setTextStyle(node: GpuiHandle, colorRgba: Int, fontSizePx: Float, weight: Float, italic: Boolean, lineHeightPx: Float?) { /* TODO */ }
      override fun setButtonText(node: GpuiHandle, text: String) { gpuiSetButtonText(node, text) }
      override fun setButtonEnabled(node: GpuiHandle, enabled: Boolean) { gpuiSetButtonEnabled(node, enabled) }
      override fun setButtonOnClick(node: GpuiHandle, token: Long) { /* TODO: wire callback into GPUI */ }
      override fun setImageUrl(node: GpuiHandle, url: String) { gpuiSetImageUrl(node, url) }
      override fun setImageFit(node: GpuiHandle, fit: Int) { gpuiSetImageFit(node, fit) }
      override fun setImageRadius(node: GpuiHandle, radiusPx: Float) { gpuiSetImageRadius(node, radiusPx) }
    }
  } catch (_: Throwable) {
    // If gpui is already initialized, ignore.
  }
  return 0u
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_shutdown")
public fun redwood_host_shutdown() {
  HostState.views.clear()
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_create_view")
public fun redwood_host_create_view(view_id: ULong): ULong {
  val id = view_id.toLong()
  val basic = GpuiRedwoodBasicWidgetFactory()
  val layout = GpuiRedwoodLayoutWidgetFactory()
  val lazy = GpuiRedwoodLazyLayoutWidgetFactory()
  val core = GpuiRedwoodUiCoreWidgetFactory()
  val widgetSystem = TestSchemaWidgetSystem(
    TestSchema = GpuiTestSchemaWidgetFactory(),
    RedwoodUiBasic = basic,
    RedwoodUiCore = core,
    RedwoodLayout = layout,
    RedwoodLazyLayout = lazy,
  )
  val rootHandle = 0L
  val container = object : Widget.Children<Long> {
    private val list = mutableListOf<Widget<Long>>()
    override val widgets: List<Widget<Long>> get() = list
    override fun insert(index: Int, widget: Widget<Long>) {
      list.add(index, widget)
      gpuiAppendChild(rootHandle, widget.value)
    }
    override fun move(fromIndex: Int, toIndex: Int, count: Int) {
      if (count <= 0) return
      val moved = ArrayList<Widget<Long>>(count)
      repeat(count) { moved.add(list.removeAt(fromIndex)) }
      var idx = toIndex
      moved.forEach { w ->
        list.add(idx, w)
        gpuiInsertChild(rootHandle, idx, w.value)
        idx++
      }
    }
    override fun remove(index: Int, count: Int) {
      repeat(count) {
        val w = list.removeAt(index)
        gpuiRemoveChild(rootHandle, w.value)
      }
    }
    override fun onModifierUpdated(index: Int, widget: Widget<Long>) {}
    override fun detach() { list.clear() }
  }
  val hostAdapter = HostProtocolAdapter(
    guestVersion = hostRedwoodVersion,
    container = container,
    protocol = TestSchemaHostProtocol.create(),
    widgetSystem = widgetSystem,
    eventSink = { uiEvent ->
      // For now just log the event; wiring to guest will happen via services.
      log("[event] $uiEvent")
    },
    leakDetector = app.cash.redwood.leaks.LeakDetector.none(),
  )
  HostState.views[id] = ViewSession(rootHandle, widgetSystem, container, hostAdapter)
  return 0u
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_destroy_view")
public fun redwood_host_destroy_view(view_id: ULong) {
  HostState.views.remove(view_id.toLong())
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_apply_changes")
public fun redwood_host_apply_changes(
  view_id: ULong,
  json_ptr: CValuesRef<ByteVar /* CChar */>?,
  json_len: platform.posix.size_t,
): ULong {
  val json = json_ptr?.toKString(length = json_len.toInt()) ?: return 1u
  val rc = applyDemoChanges(json)
  return if (rc == 0) 0u else 1u
}

/**
 * Experimental: Apply Redwood protocol changes (array of Change) encoded as JSON.
 * This is a scaffold; a proper HostProtocol and WidgetSystem must be supplied
 * before this will affect the GPUI tree.
 */
@Suppress("unused")
@kotlin.native.CName("redwood_host_apply_changes_protocol")
public fun redwood_host_apply_changes_protocol(
  view_id: ULong,
  json_ptr: CValuesRef<ByteVar /* CChar */>?,
  json_len: platform.posix.size_t,
): ULong {
  val jsonText = json_ptr?.toKString(length = json_len.toInt()) ?: return 1u
  val view = HostState.views[view_id.toLong()] ?: return 2u
  return try {
    val protocolChanges = Json.decodeFromString(ListSerializer(Change.serializer()), jsonText)
    val hostChanges = protocolChanges.mapNotNull { UiChange.fromProtocol(view.hostAdapter.protocol, it) }
    val start = kotlin.system.getTimeMillis()
    view.hostAdapter.sendChanges(hostChanges)
    val elapsed = kotlin.system.getTimeMillis() - start
    val maxBytes = 262_144 // 256 KiB
    val maxMs = 4
    val warn = (json_len.toLong() > maxBytes) || (elapsed > maxMs)
    if (warn) {
      log("[protocol][WARN] applied ${hostChanges.size} ui changes in ${elapsed}ms, bytes=${json_len}")
    } else {
      log("[protocol] applied ${hostChanges.size} ui changes in ${elapsed}ms, bytes=${json_len}")
    }
    0u
  } catch (t: Throwable) {
    log("[protocol] failed to decode changes: ${t.message}")
    1u
  }
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_preview_protocol_demo")
public fun redwood_host_preview_protocol_demo(view_id: ULong): ULong {
  val view = HostState.views[view_id.toLong()] ?: return 2u
  return try {
    val changes = listOf<UiChange>(
      UiCreate(app.cash.redwood.protocol.Id(1), app.cash.redwood.protocol.WidgetTag(4)), // TestSchema.Button
      UiPropertyChange(app.cash.redwood.protocol.Id(1), app.cash.redwood.protocol.PropertyTag(1), "Hello Redwood"), // text
      UiChildrenChange(app.cash.redwood.protocol.ChildrenChange.Add(
        id = app.cash.redwood.protocol.Id.Root,
        tag = app.cash.redwood.protocol.ChildrenTag.Root,
        childId = app.cash.redwood.protocol.Id(1),
        index = 0,
      )),
    )
    view.hostAdapter.sendChanges(changes)
    0u
  } catch (t: Throwable) {
    log("[protocol_demo] failed: ${t.message}")
    1u
  }
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_send_input")
public fun redwood_host_send_input(
  view_id: ULong,
  json_ptr: CValuesRef<ByteVar /* CChar */>?,
  json_len: platform.posix.size_t,
): ULong {
  return 0u
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_request_frame")
public fun redwood_host_request_frame(view_id: ULong): Int {
  return 1
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_button_click")
public fun redwood_host_button_click(
  view_id: ULong,
  button_handle: Long,
) {
  // Route to the host click registry; if no callback is registered this is a no-op.
  HostClickRegistry.click(button_handle)
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_set_theme")
public fun redwood_host_set_theme(
  view_id: ULong,
  json_ptr: CValuesRef<ByteVar /* CChar */>?,
  json_len: platform.posix.size_t,
): ULong {
  return 0u
}

@Suppress("unused")
@kotlin.native.CName("redwood_host_measure_text")
public fun redwood_host_measure_text(
  view_id: ULong,
  req_json_ptr: CValuesRef<ByteVar /* CChar */>?,
  req_json_len: platform.posix.size_t,
  resp_json_ptr: CValuesRef<ByteVar /* CChar */>?,
  resp_json_cap: CValuesRef<platform.posix.size_t>?,
): ULong {
  return 1u
}

private fun log(msg: String) {
  println("[redwood-host] $msg")
}

// GPUI vtable (static link model). For now we only store the pointer; real calls will be wired later.
private var gpuiVtablePtr: COpaquePointer? = null

@Suppress("unused")
@kotlin.native.CName("redwood_host_set_gpui_vtable")
public fun redwood_host_set_gpui_vtable(vtable: COpaquePointer?): ULong {
  gpuiVtablePtr = vtable
  return 0u
}
