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
  private val targetVisibleWindow = 12
  private var lastBindFirst: Int = -1
  private var lastBindLast: Int = -1
  private var lastDetachFirst: Int = -1
  private var lastDetachLast: Int = -1

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
    lastBindFirst = -1
    lastBindLast = -1
    lastDetachFirst = -1
    lastDetachLast = -1
  }

  private fun updateViewportForOffset() {
    val handle = ensureScrollHandle() ?: return
    if (rowSlots.isEmpty()) return

    val mountedRowCount = rowSlots.count { it.hasWidget() }

    // Bootstrap: if no children are mounted yet, bind an initial small window
    // so the loading strategy can stabilize and promote content.
    if (mountedRowCount == 0) {
      val maxIndex = (processor.size - 1).coerceAtLeast(0)
      val lastToBind = minOf(targetVisibleWindow - 1, maxIndex)
      bindVisibleRange(
        visibleFirst = 0,
        visibleLast = lastToBind,
        bindFirst = 0,
        bindLast = lastToBind,
      )
      scrollProcessor.onUserScroll(0, lastToBind)
      return
    }

    val firstChildRaw = handle.topIndex().toInt()
    val lastChildRaw = handle.bottomIndex().toInt()
    val maxRowIndex = (mountedRowCount - 1).coerceAtLeast(0)
    val firstChild = (firstChildRaw - 1).coerceIn(0, maxRowIndex)
    val lastChild = (lastChildRaw - 1).coerceIn(firstChild, maxRowIndex)

    // Map visible child indices back to dataset indices without scanning all rows.
    val currentBindStart = if (lastBindFirst >= 0) lastBindFirst else 0
    var datasetFirst = (currentBindStart + firstChild).coerceIn(0, rowSlots.lastIndex)
    var datasetLast = (currentBindStart + lastChild).coerceIn(datasetFirst, rowSlots.lastIndex)
    val hasMapping = mountedRowCount > 0
    var first: Int
    var last: Int
    if (hasMapping) {
      val halfWindow = targetVisibleWindow / 2
      first = (datasetFirst - halfWindow).coerceAtLeast(0)
      val desiredLast = (datasetLast + halfWindow).coerceAtMost(rowSlots.lastIndex)
      last = maxOf(first, desiredLast)
    } else {
      first = 0
      last = (targetVisibleWindow - 1).coerceAtMost(rowSlots.lastIndex)
  }
    if (last - first + 1 < targetVisibleWindow) {
      last = (first + targetVisibleWindow - 1).coerceAtMost(rowSlots.lastIndex)
    }

    val preloadBefore = (targetVisibleWindow / 2).coerceAtLeast(2)
    val preloadAfter = (targetVisibleWindow / 2).coerceAtLeast(2)
    val bindFirst = (first - preloadBefore).coerceAtLeast(0)
    val bindLast = (last + preloadAfter).coerceAtMost(rowSlots.lastIndex)

    bindVisibleRange(
      visibleFirst = first,
      visibleLast = last,
      bindFirst = bindFirst,
      bindLast = bindLast,
    )

    if (first < processor.size) {
      scrollProcessor.onUserScroll(first, last.coerceAtMost(processor.size - 1))
    }
  }

  private fun bindVisibleRange(visibleFirst: Int, visibleLast: Int, bindFirst: Int, bindLast: Int) {
    if (rowSlots.isEmpty()) return
    val safeBindFirst = bindFirst.coerceAtLeast(0)
    val safeBindLast = bindLast.coerceAtLeast(safeBindFirst).coerceAtMost(rowSlots.lastIndex)

    val detachFirst = (visibleFirst - targetVisibleWindow).coerceAtLeast(0)
    val detachLast = (visibleLast + targetVisibleWindow).coerceAtMost(rowSlots.lastIndex)

    if (
      lastBindFirst == safeBindFirst &&
      lastBindLast == safeBindLast &&
      lastDetachFirst == detachFirst &&
      lastDetachLast == detachLast
    ) {
      return
    }

    val prevBindFirst = lastBindFirst
    val prevBindLast = lastBindLast
    val prevDetachFirst = lastDetachFirst
    val prevDetachLast = lastDetachLast

    // Update offscreen-before window (top spacer heights).
    if (prevDetachFirst >= 0) {
      if (detachFirst > prevDetachFirst) {
        for (i in prevDetachFirst until detachFirst) {
          rowSlots[i].moveOffscreenBefore()
        }
      } else if (detachFirst < prevDetachFirst) {
        for (i in detachFirst until prevDetachFirst) {
          rowSlots[i].prepareForAttach()
        }
      }
    }

    // Update offscreen-after window (bottom spacer heights).
    if (prevDetachLast >= 0) {
      if (detachLast > prevDetachLast) {
        for (i in (prevDetachLast + 1)..detachLast) {
          rowSlots[i].moveOffscreenAfter()
        }
      } else if (detachLast < prevDetachLast) {
        for (i in (detachLast + 1)..prevDetachLast) {
          rowSlots[i].prepareForAttach()
        }
      }
    }

    // Shrink binding window on the left.
    if (prevBindFirst >= 0 && safeBindFirst > prevBindFirst) {
      for (i in prevBindFirst until safeBindFirst) {
        if (i < detachFirst) rowSlots[i].moveOffscreenBefore() else rowSlots[i].prepareForAttach()
      }
    }

    // Shrink binding window on the right.
    if (prevBindLast >= 0 && safeBindLast < prevBindLast) {
      for (i in (safeBindLast + 1)..prevBindLast) {
        if (i > detachLast) rowSlots[i].moveOffscreenAfter() else rowSlots[i].prepareForAttach()
      }
    }

    // Grow binding window on the left.
    if (prevBindFirst == -1 || safeBindFirst < prevBindFirst) {
      val start = safeBindFirst
      val end = if (prevBindFirst == -1) safeBindLast else prevBindFirst - 1
      for (i in start..end) {
        rowSlots[i].prepareForAttach()
        if (i in safeBindFirst..safeBindLast) {
          val binding = rowSlots[i].binding
          if (binding?.isBound != true) {
            rowSlots[i].binding = processor.bind(i, rowSlots[i])
          }
        }
      }
    }

    // Grow binding window on the right.
    if (prevBindLast == -1 || safeBindLast > prevBindLast) {
      val start = if (prevBindLast == -1) safeBindFirst else prevBindLast + 1
      val end = safeBindLast
      for (i in start..end) {
        rowSlots[i].prepareForAttach()
        if (i in safeBindFirst..safeBindLast) {
          val binding = rowSlots[i].binding
          if (binding?.isBound != true) {
            rowSlots[i].binding = processor.bind(i, rowSlots[i])
          }
        }
      }
    }

    lastBindFirst = safeBindFirst
    lastBindLast = safeBindLast
    lastDetachFirst = detachFirst
    lastDetachLast = detachLast
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
