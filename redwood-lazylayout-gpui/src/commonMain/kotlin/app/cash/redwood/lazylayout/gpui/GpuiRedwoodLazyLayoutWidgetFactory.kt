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
        slot.binding = bind(slotIndex, slot)
      }
      reindex(startIndex = index)
      updateViewportForOffset()
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
      updateViewportForOffset()
    }

    override fun setContent(view: RowSlot, widget: Widget<GpuiNode>?) {
      if (widget?.let { it::class.simpleName } == "SizeOnlyPlaceholderWidget") println("[GpuiLazyList] bindings consumed placeholder at index=${view.index}"); widgetIsNull=${widget == null}")
      view.setContent(widget)
      updateViewportForOffset()
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

    val itemCount = minOf(handle.childrenCount().toInt(), processor.size)
    if (itemCount == 0) return

    val first = handle.topIndex().toInt().coerceIn(0, itemCount - 1)
    val last = handle.bottomIndex().toInt().coerceIn(first, itemCount - 1)
    scrollProcessor.onUserScroll(first, last)
  }

  private inner class RowSlot(
    index: Int,
  ) {
    var index: Int = index
    var binding: Binding<RowSlot, GpuiNode>? = null
    private var widget: Widget<GpuiNode>? = null

    fun setContent(newWidget: Widget<GpuiNode>?) {
      if (widget === newWidget) return

      val existing = widget
      if (existing != null) {
        columnChildren.remove(index, 1)
      }

      widget = newWidget

      if (newWidget != null) {
        columnChildren.insert(index, newWidget)
      }
    }

    fun detach() {
      if (widget != null) {
        columnChildren.remove(index, 1)
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
