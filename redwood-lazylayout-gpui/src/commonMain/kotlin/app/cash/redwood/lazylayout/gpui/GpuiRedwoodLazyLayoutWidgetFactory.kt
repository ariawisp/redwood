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
  private val columnChildren = column.children
  private val rowSlots = mutableListOf<RowSlot>()
  private val scrollHandleProvider = column as? GpuiScrollHandleProvider
  private var scrollHandle: RedwoodScrollHandle? = null

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
      println("[GpuiLazyList] insertRows index=$index count=$count")
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
        val binding = slot.binding
        if (binding != null) {
          binding.unbind()
          slot.binding = null
        } else {
          slot.detach()
        }
      }
      reindex(startIndex = index)
      // Defer viewport update until end of changes or scroll event to avoid thrash.
    }

    override fun setContent(view: RowSlot, widget: Widget<GpuiNode>?) {
      val placeholder = when (widget?.let { it::class.simpleName }) {
        "SizeOnlyPlaceholderWidget", "GpuiSpacer" -> true
        else -> false
      }
      println(
        "[GpuiLazyList] setContent index=${view.index} widgetIsNull=${widget == null} " +
          "widgetType=${widget?.let { it::class.simpleName }} placeholder=$placeholder",
      )
      if (!placeholder && widget != null && view.index < 5) {
        println("[GpuiLazyList] row content preview index=${view.index} widget=${widget::class.simpleName}")
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
      scrollHandle = null
    }
  }

  override val items: Widget.Children<GpuiNode> = processor.items

  override val placeholder: Widget.Children<GpuiNode> = processor.placeholder

  private val scrollProcessor = object : LazyListScrollProcessor() {
    override fun contentSize(): Int = processor.size

    override fun programmaticScroll(firstIndex: Int, animated: Boolean) {
      ensureScrollHandle()?.scrollToTopOfItem(firstIndex.toUInt())
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

  private fun updateViewportForOffset() {
    val handle = ensureScrollHandle() ?: return
    if (rowSlots.isEmpty()) return

    val childrenCount = handle.childrenCount().toInt().coerceAtLeast(0)
    val targetVisible = 12

    // Bootstrap: if no children are mounted yet, bind an initial small window
    // so the loading strategy can stabilize and promote content.
    if (childrenCount == 0) {
      val maxIndex = (processor.size - 1).coerceAtLeast(0)
      val lastToBind = minOf(targetVisible - 1, maxIndex)
      bindVisibleRange(0, lastToBind)
      println("[GpuiLazyList][viewport] childrenCount=0 first=0 last=$lastToBind size=${processor.size}")
      scrollProcessor.onUserScroll(0, lastToBind)
      return
    }

    val firstChild = handle.topIndex().toInt().coerceIn(0, childrenCount - 1)
    val lastChild = handle.bottomIndex().toInt().coerceIn(firstChild, childrenCount - 1)

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

    println(
      "[GpuiLazyList][viewport] childrenCount=$childrenCount first=$first last=$last size=${processor.size}",
    )
    if (first < processor.size) {
      scrollProcessor.onUserScroll(first, last.coerceAtMost(processor.size - 1))
    }
  }

  private fun bindVisibleRange(first: Int, last: Int) {
    if (rowSlots.isEmpty()) return
    val safeFirst = first.coerceAtLeast(0)
    val safeLast = last.coerceAtLeast(safeFirst).coerceAtMost(rowSlots.lastIndex)

    // Unbind anything outside the desired range.
    for (i in 0 until rowSlots.size) {
      if (i < safeFirst || i > safeLast) {
        val binding = rowSlots[i].binding
        if (binding?.isBound == true) {
          println("[GpuiLazyList][debug] unbinding index=$i")
          binding.unbind()
          rowSlots[i].detach()
          rowSlots[i].binding = null
        }
      }
    }

    // Bind everything in the desired range.
    for (i in safeFirst..safeLast) {
      val slot = rowSlots[i]
      val binding = slot.binding
      if (binding?.isBound != true) {
        println("[GpuiLazyList][debug] binding index=$i")
        slot.binding = processor.bind(i, slot)
      }
    }
    println("[GpuiLazyList][debug] bindVisibleRange first=$safeFirst last=$safeLast")
  }

    private inner class RowSlot(
      index: Int,
    ) {
      var index: Int = index
      var binding: Binding<RowSlot, GpuiNode>? = null
      private var widget: Widget<GpuiNode>? = null

      fun hasWidget(): Boolean = widget != null

      private fun childIndex(): Int {
        var count = 0
        for (i in 0 until index) {
          if (rowSlots[i].widget != null) count++
        }
        return count
      }

      fun setContent(newWidget: Widget<GpuiNode>?) {
        if (widget === newWidget) return

        val existing = widget
        if (existing != null) {
        val ci = childIndex()
        println("[GpuiLazyList] removing child index=$index (childIndex=$ci) type=${existing::class.simpleName}")
        columnChildren.remove(ci, 1)
        }

        widget = newWidget

        if (newWidget != null) {
        val ci = childIndex()
        println("[GpuiLazyList] inserting child index=$index (childIndex=$ci) type=${newWidget::class.simpleName}")
        if (index < 5) {
          val totalChildren = newWidget.allChildren.sumOf { it.widgets.size }
          println("[GpuiLazyList] after insert index=$index childWidgetCount=$totalChildren")
        }
        columnChildren.insert(ci, newWidget)
        }
      }

      fun detach() {
      if (widget != null) {
        val ci = childIndex()
        columnChildren.remove(ci, 1)
        widget = null
      }
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
