package app.cash.redwood.lazylayout.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.GpuiEnvironment
import app.cash.redwood.host.gpui.GpuiNode
import app.cash.redwood.host.gpui.RedwoodScrollHandle
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.layout.api.Overflow
import app.cash.redwood.layout.gpui.GpuiRedwoodLayoutWidgetFactory
import app.cash.redwood.layout.gpui.GpuiScrollHandleProvider
import app.cash.redwood.lazylayout.api.ScrollItemIndex
import app.cash.redwood.lazylayout.widget.LazyList
import app.cash.redwood.lazylayout.widget.LazyListScrollProcessor
import app.cash.redwood.lazylayout.widget.LazyListUpdateProcessor
import app.cash.redwood.lazylayout.widget.LazyListUpdateProcessor.Binding
import app.cash.redwood.lazylayout.widget.RedwoodLazyLayoutWidgetFactory
import app.cash.redwood.lazylayout.widget.RefreshableLazyList
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.dp
import app.cash.redwood.widget.ChangeListener
import app.cash.redwood.widget.Widget

public class GpuiRedwoodLazyLayoutWidgetFactory(
  private val environment: GpuiEnvironment,
) : RedwoodLazyLayoutWidgetFactory<GpuiNode> {
  override fun LazyList(): LazyList<GpuiNode> = GpuiLazyList(environment)

  override fun RefreshableLazyList(): RefreshableLazyList<GpuiNode> =
    GpuiRefreshableLazyList(environment)
}

private class GpuiLazyList(
  private val environment: GpuiEnvironment,
) : LazyList<GpuiNode>, ChangeListener {
  private val layoutFactory = GpuiRedwoodLayoutWidgetFactory(environment)
  private val column = layoutFactory.Column().apply {
    overflow(Overflow.Scroll)
    height(Constraint.Fill)
  }
  private val topSpacer = layoutFactory.Spacer()
  private val bottomSpacer = layoutFactory.Spacer()
  private val columnChildren = column.children
  private val rowSlots = mutableListOf<RowSlot>()
  private val scrollHandleProvider = column as? GpuiScrollHandleProvider
  private var scrollHandle: RedwoodScrollHandle? = null
  private var topOffsetPx = 0f
  private var bottomOffsetPx = 0f

  init {
    columnChildren.insert(0, topSpacer)
    columnChildren.insert(1, bottomSpacer)
    topSpacer.height(0.dp)
    bottomSpacer.height(0.dp)
  }

  override var modifier: Modifier
    get() = column.modifier
    set(value) {
      column.modifier = value
    }

  override val value: GpuiNode
    get() = column.value

  private val processor = object : LazyListUpdateProcessor<RowSlot, GpuiNode>() {
    override fun createPlaceholder(original: GpuiNode): GpuiNode? {
      val spacer = layoutFactory.Spacer()

      val widthPx = original.measuredWidth().takeIf { it.isFinite() && it > 0f }
      if (widthPx != null) {
        val widthDp = environment.density.run { widthPx.toDp() }
        spacer.width(widthDp)
      }

      val heightPx = original.measuredHeight().takeIf { it.isFinite() && it > 0f }
      if (heightPx != null) {
        val heightDp = environment.density.run { heightPx.toDp() }
        spacer.height(heightDp)
      }

      return spacer.value
    }

    override fun insertRows(index: Int, count: Int) {
      repeat(count) { offset ->
        val slotIndex = index + offset
        val slot = RowSlot(slotIndex)
        rowSlots.add(slotIndex, slot)
        // Do not bind here; we only bind visible rows in updateViewportForOffset().
      }
      reindex(startIndex = index)
      // Defer viewport update until end of changes or scroll event to avoid thrash.
    }

    override fun deleteRows(index: Int, count: Int) {
      for (offset in count - 1 downTo 0) {
        val slotIndex = index + offset
        val slot = rowSlots.removeAt(slotIndex)
        slot.onRemove()
      }
      reindex(startIndex = index)
      // Defer viewport update until end of changes or scroll event to avoid thrash.
    }

    override fun setContent(view: RowSlot, widget: Widget<GpuiNode>?) {
      val placeholder = when (widget?.let { it::class.simpleName }) {
        "SizeOnlyPlaceholderWidget", "GpuiSpacer" -> true
        else -> false
      }
      // Mount both real rows and placeholders. Placeholders are sized via createPlaceholder().
      view.setContent(widget)
    }

    override fun detach(view: RowSlot) {
      view.detach()
    }

    override fun detach() {
      rowSlots.forEach { it.detach() }
      rowSlots.clear()
      resetOffsets()
      scrollHandle = null
    }
  }

  override val items: Widget.Children<GpuiNode> = processor.items

  override val placeholder: Widget.Children<GpuiNode> = processor.placeholder

  private val scrollProcessor = object : LazyListScrollProcessor() {
    override fun contentSize(): Int = processor.size

    override fun programmaticScroll(firstIndex: Int, animated: Boolean) {
      ensureScrollHandle()?.scrollToTopOfItem((firstIndex + 1).toUInt())
    }
  }

  init {
    column.onScroll {
      updateViewportForOffset()
    }
    ensureScrollHandle()
  }

  override fun isVertical(isVertical: Boolean) {
    // Only vertical lists are currently supported. Additional orientations may be added later.
  }

  override fun onViewportChanged(onViewportChanged: (Int, Int) -> Unit) {
    scrollProcessor.onViewportChanged(onViewportChanged)
    updateViewportForOffset()
  }

  override fun itemsBefore(itemsBefore: Int) {
    processor.itemsBefore(itemsBefore)
  }

  override fun itemsAfter(itemsAfter: Int) {
    processor.itemsAfter(itemsAfter)
  }

  override fun width(width: Constraint) {
    column.width(width)
  }

  override fun height(height: Constraint) {
    column.height(height)
  }

  override fun margin(margin: Margin) {
    column.margin(margin)
  }

  override fun crossAxisAlignment(crossAxisAlignment: CrossAxisAlignment) {
    column.horizontalAlignment(crossAxisAlignment)
  }

  override fun scrollItemIndex(scrollItemIndex: ScrollItemIndex) {
    scrollProcessor.scrollItemIndex(scrollItemIndex)
  }

  override fun onEndChanges() {
    processor.onEndChanges()
    scrollProcessor.onEndChanges()
    ensureScrollHandle()
    updateViewportForOffset()
  }

  private fun ensureScrollHandle(): RedwoodScrollHandle? {
    val existing = scrollHandle
    if (existing != null) {
      return existing
    }
    val handle = scrollHandleProvider?.scrollHandle()
    scrollHandle = handle
    return handle
  }

  private fun reindex(startIndex: Int) {
    for (i in startIndex until rowSlots.size) {
      rowSlots[i].index = i
    }
  }

  private fun adjustTopOffset(deltaPx: Float) {
    if (deltaPx == 0f) return
    topOffsetPx = (topOffsetPx + deltaPx).coerceAtLeast(0f)
    val heightDp = environment.density.run { topOffsetPx.toDp() }
    topSpacer.height(heightDp)
  }

  private fun adjustBottomOffset(deltaPx: Float) {
    if (deltaPx == 0f) return
    bottomOffsetPx = (bottomOffsetPx + deltaPx).coerceAtLeast(0f)
    val heightDp = environment.density.run { bottomOffsetPx.toDp() }
    bottomSpacer.height(heightDp)
  }

  private fun resetOffsets() {
    topOffsetPx = 0f
    bottomOffsetPx = 0f
    topSpacer.height(0.dp)
    bottomSpacer.height(0.dp)
  }

  private fun updateViewportForOffset() {
    val handle = ensureScrollHandle() ?: return
    if (rowSlots.isEmpty()) return

    val targetVisible = 12
    val mountedRowCount = rowSlots.count { it.hasWidget() }

    // Bootstrap: if no children are mounted yet, bind an initial small window
    // so the loading strategy can stabilize and promote content.
    if (mountedRowCount == 0) {
      val maxIndex = (processor.size - 1).coerceAtLeast(0)
      val lastToBind = minOf(targetVisible - 1, maxIndex)
      bindVisibleRange(0, lastToBind)
      scrollProcessor.onUserScroll(0, lastToBind)
      return
    }

    val firstChildRaw = handle.topIndex().toInt()
    val lastChildRaw = handle.bottomIndex().toInt()
    val maxRowIndex = (mountedRowCount - 1).coerceAtLeast(0)
    val firstChild = (firstChildRaw - 1).coerceIn(0, maxRowIndex)
    val lastChild = (lastChildRaw - 1).coerceIn(firstChild, maxRowIndex)

    // Map visible child indices back to dataset indices.
    var datasetFirst = -1
    var datasetLast = -1
    var runningChild = 0
    for (i in 0 until rowSlots.size) {
      if (rowSlots[i].hasWidget()) {
        if (runningChild == firstChild) datasetFirst = i
        if (runningChild == lastChild) datasetLast = i
        if (datasetFirst != -1 && datasetLast != -1) break
        runningChild += 1
      }
    }

    val hasMapping = datasetFirst >= 0 && datasetLast >= 0
    var first = if (hasMapping) datasetFirst else 0
    var last = if (hasMapping) datasetLast else first
    // Ensure we bind enough rows to visibly fill the viewport (approximate).
    if (last - first + 1 < targetVisible) {
      last = (first + targetVisible - 1).coerceAtMost(rowSlots.lastIndex)
    }

    // Ensure only the visible range is bound to views.
    bindVisibleRange(first, last)

    if (first < processor.size) {
      scrollProcessor.onUserScroll(first, last.coerceAtMost(processor.size - 1))
    }
  }

  private fun bindVisibleRange(first: Int, last: Int) {
    if (rowSlots.isEmpty()) return
    val safeFirst = first.coerceAtLeast(0)
    val safeLast = last.coerceAtLeast(safeFirst).coerceAtMost(rowSlots.lastIndex)

    for (i in 0 until rowSlots.size) {
      val slot = rowSlots[i]
      when {
        i < safeFirst -> slot.moveOffscreenBefore()
        i > safeLast -> slot.moveOffscreenAfter()
      }
    }

    if (safeFirst > safeLast) {
      return
    }

    for (i in safeFirst..safeLast) {
      val slot = rowSlots[i]
      slot.prepareForAttach()
      val binding = slot.binding
      if (binding?.isBound == true) continue
      slot.binding = processor.bind(i, slot)
    }
  }

  private enum class SlotLocation {
    Attached,
    OffscreenBefore,
    OffscreenAfter,
  }

  private inner class RowSlot(
    index: Int,
  ) {
    var index: Int = index
    var binding: Binding<RowSlot, GpuiNode>? = null
    private var widget: Widget<GpuiNode>? = null
    var cachedHeightPx: Float = 0f
      private set
    var location: SlotLocation = SlotLocation.Attached
      private set

    fun hasWidget(): Boolean = widget != null

    fun moveOffscreenBefore() {
      if (location == SlotLocation.OffscreenBefore) return
      val capturedHeight = captureHeightPx()
      when (location) {
        SlotLocation.OffscreenAfter -> adjustBottomOffset(-cachedHeightPx)
        SlotLocation.Attached -> {
          val existingBinding = binding
          if (existingBinding?.isBound == true) {
            existingBinding.unbind()
          }
          detach()
        }
        SlotLocation.OffscreenBefore -> Unit
      }
      cachedHeightPx = capturedHeight
      adjustTopOffset(cachedHeightPx)
      location = SlotLocation.OffscreenBefore
    }

    fun moveOffscreenAfter() {
      if (location == SlotLocation.OffscreenAfter) return
      val capturedHeight = captureHeightPx()
      when (location) {
        SlotLocation.OffscreenBefore -> adjustTopOffset(-cachedHeightPx)
        SlotLocation.Attached -> {
          val existingBinding = binding
          if (existingBinding?.isBound == true) {
            existingBinding.unbind()
          }
          detach()
        }
        SlotLocation.OffscreenAfter -> Unit
      }
      cachedHeightPx = capturedHeight
      adjustBottomOffset(cachedHeightPx)
      location = SlotLocation.OffscreenAfter
    }

    fun prepareForAttach() {
      when (location) {
        SlotLocation.Attached -> return
        SlotLocation.OffscreenBefore -> adjustTopOffset(-cachedHeightPx)
        SlotLocation.OffscreenAfter -> adjustBottomOffset(-cachedHeightPx)
      }
      cachedHeightPx = 0f
      location = SlotLocation.Attached
    }

    fun onRemove() {
      when (location) {
        SlotLocation.OffscreenBefore -> adjustTopOffset(-cachedHeightPx)
        SlotLocation.OffscreenAfter -> adjustBottomOffset(-cachedHeightPx)
        SlotLocation.Attached -> {
          val existingBinding = binding
          if (existingBinding?.isBound == true) {
            existingBinding.unbind()
          }
        }
      }
      detach()
    }

    private fun captureHeightPx(): Float {
      val measured = widget?.value?.measuredHeight()
      return if (measured != null && measured.isFinite() && measured > 0f) {
        measured
      } else {
        cachedHeightPx
      }
    }

    private fun childIndex(): Int {
      var count = 0
      for (i in 0 until index) {
        if (rowSlots[i].widget != null) count++
      }
      return 1 + count
    }

    fun setContent(newWidget: Widget<GpuiNode>?) {
      if (widget === newWidget) return

      val existing = widget
      if (existing != null) {
        val ci = childIndex()
        columnChildren.remove(ci, 1)
      }

      widget = newWidget

      if (newWidget != null) {
        val ci = childIndex()
        columnChildren.insert(ci, newWidget)
      }
    }

    fun detach() {
      if (widget != null) {
        val ci = childIndex()
        columnChildren.remove(ci, 1)
      }
      widget = null
      binding = null
    }
  }
}

private class GpuiRefreshableLazyList(
  environment: GpuiEnvironment,
) : RefreshableLazyList<GpuiNode>, ChangeListener {
  private val delegate = GpuiLazyList(environment)
  private var onRefresh: (() -> Unit)? = null
  private var currentModifier: Modifier = delegate.modifier

  override var modifier: Modifier
    get() = currentModifier
    set(value) {
      currentModifier = value
      delegate.modifier = value
    }

  override val value: GpuiNode
    get() = delegate.value

  override val items: Widget.Children<GpuiNode>
    get() = delegate.items

  override val placeholder: Widget.Children<GpuiNode>
    get() = delegate.placeholder

  override fun isVertical(isVertical: Boolean) = delegate.isVertical(isVertical)

  override fun onViewportChanged(onViewportChanged: (Int, Int) -> Unit) =
    delegate.onViewportChanged(onViewportChanged)

  override fun itemsBefore(itemsBefore: Int) = delegate.itemsBefore(itemsBefore)

  override fun itemsAfter(itemsAfter: Int) = delegate.itemsAfter(itemsAfter)

  override fun width(width: Constraint) = delegate.width(width)

  override fun height(height: Constraint) = delegate.height(height)

  override fun margin(margin: Margin) = delegate.margin(margin)

  override fun crossAxisAlignment(crossAxisAlignment: CrossAxisAlignment) =
    delegate.crossAxisAlignment(crossAxisAlignment)

  override fun scrollItemIndex(scrollItemIndex: ScrollItemIndex) =
    delegate.scrollItemIndex(scrollItemIndex)

  override fun refreshing(refreshing: Boolean) {
    // Pull-to-refresh indicator not yet implemented.
  }

  override fun onRefresh(onRefresh: (() -> Unit)?) {
    this.onRefresh = onRefresh
  }

  override fun pullRefreshContentColor(pullRefreshContentColor: UInt) {
    // No-op.
  }

  fun triggerRefresh() {
    onRefresh?.invoke()
  }

  override fun onEndChanges() = delegate.onEndChanges()
}
