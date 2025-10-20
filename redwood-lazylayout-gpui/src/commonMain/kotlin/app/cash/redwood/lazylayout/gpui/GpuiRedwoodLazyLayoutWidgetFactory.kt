package app.cash.redwood.lazylayout.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.GpuiEnvironment
import app.cash.redwood.host.gpui.GpuiNode
import app.cash.redwood.host.gpui.RedwoodNodeHandle
import app.cash.redwood.host.gpui.RedwoodScrollHandle
import app.cash.redwood.host.gpui.RedwoodUniformListAdapter
import app.cash.redwood.host.gpui.RedwoodUniformListNode
import app.cash.redwood.host.gpui.RedwoodUniformListRenderer
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.layout.gpui.GpuiRedwoodLayoutWidgetFactory
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
import kotlin.math.max
import kotlin.math.min

private const val TRACE_ENV = "REDWOOD_LAZY_TRACE"

private val traceLazyList: Boolean by lazy { traceFlagEnabled(TRACE_ENV) }

private fun traceLazy(message: () -> String) {
  if (traceLazyList) {
    println("[redwood-lazy] ${message()}")
  }
}

internal expect fun traceFlagEnabled(name: String): Boolean

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
  private val container = layoutFactory.Box().apply {
    height(Constraint.Fill)
  }
  private val containerChildren = container.children

  private val uniformListHandle: RedwoodUniformListNode = environment.surface.createUniformList()
  private val uniformListChildren = uniformListHandle.children()
  private val uniformListAdapter: RedwoodUniformListAdapter = uniformListHandle.adapter()
  private val uniformListNode = GpuiNode(
    handle = uniformListHandle.rawNode(),
    layoutController = environment.layoutController,
    environment = environment,
  )
  private val uniformListWidget = UniformListWidget(environment, uniformListNode)

  private val rowSlots = mutableListOf<RowSlot>()

  private val processor = object : LazyListUpdateProcessor<RowSlot, GpuiNode>() {
    override fun createPlaceholder(original: GpuiNode): GpuiNode? = null

    override fun insertRows(index: Int, count: Int) {
      repeat(count) { offset ->
        rowSlots.add(index + offset, RowSlot(index + offset))
      }
      reindex(index)
      traceLazy { "insertRows index=$index count=$count size=${rowSlots.size}" }
    }

    override fun deleteRows(index: Int, count: Int) {
      for (offset in count - 1 downTo 0) {
        val slot = rowSlots.removeAt(index + offset)
        slot.detach()
      }
      reindex(index)
      traceLazy { "deleteRows index=$index count=$count size=${rowSlots.size}" }
    }

    override fun setContent(view: RowSlot, widget: Widget<GpuiNode>?) {
      view.setContent(widget)
    }

    override fun detach(view: RowSlot) {
      view.detach()
    }

    override fun detach() {
      rowSlots.forEach { it.detach() }
      rowSlots.clear()
      currentBindRange = null
      currentVisibleRange = null
      scrollHandle = null
      uniformListAdapter.clear()
      uniformListHandle.setItemCount(0u)
      traceLazy { "detach() cleared list" }
    }
  }

  private val scrollProcessor = object : LazyListScrollProcessor() {
    override fun contentSize(): Int = processor.size

    override fun programmaticScroll(firstIndex: Int, animated: Boolean) {
      ensureScrollHandle()?.scrollToTopOfItem((firstIndex + 1).toUInt())
    }
  }

  private val renderer = Renderer()

  private var scrollHandle: RedwoodScrollHandle? = null
  private var currentVisibleRange: IntRange? = null
  private var currentBindRange: IntRange? = null

  private val preloadBefore = 4
  private val preloadAfter = 4

  init {
    containerChildren.insert(0, uniformListWidget)
    uniformListHandle.setRenderer(renderer)
    uniformListHandle.setItemCount(0u)
    // Ensure the list fills its container so the viewport has non-zero height.
    uniformListNode.wantsFillWidth = true
    uniformListNode.wantsFillHeight = true
    uniformListNode.markNeedsLayout()
  }

  override var modifier: Modifier
    get() = container.modifier
    set(value) {
      container.modifier = value
    }

  override val value: GpuiNode
    get() = container.value

  override val items: Widget.Children<GpuiNode> = processor.items

  override val placeholder: Widget.Children<GpuiNode> = processor.placeholder

  override fun isVertical(isVertical: Boolean) {
    // Only vertical lists are currently supported.
  }

  override fun onViewportChanged(onViewportChanged: (Int, Int) -> Unit) {
    scrollProcessor.onViewportChanged(onViewportChanged)
    currentVisibleRange?.let { range ->
      onViewportChanged(range.first, range.last)
    }
  }

  override fun itemsBefore(itemsBefore: Int) {
    processor.itemsBefore(itemsBefore)
  }

  override fun itemsAfter(itemsAfter: Int) {
    processor.itemsAfter(itemsAfter)
  }

  override fun width(width: Constraint) {
    container.width(width)
  }

  override fun height(height: Constraint) {
    container.height(height)
  }

  override fun margin(margin: Margin) {
    container.margin(margin)
  }

  override fun crossAxisAlignment(crossAxisAlignment: CrossAxisAlignment) {
    container.horizontalAlignment(crossAxisAlignment)
  }

  override fun scrollItemIndex(scrollItemIndex: ScrollItemIndex) {
    scrollProcessor.scrollItemIndex(scrollItemIndex)
  }

  override fun onEndChanges() {
    processor.onEndChanges()
    scrollProcessor.onEndChanges()
    val size = processor.size
    uniformListHandle.setItemCount(size.toUInt())
    if (size > 0) {
      // Bind the first row before measuring so the row has non-zero height.
      updateBindings(0, 0)
      uniformListHandle.setMeasureIndex(0u)
    }
    ensureScrollHandle()
  }

  private fun reindex(startIndex: Int) {
    for (i in startIndex until rowSlots.size) {
      rowSlots[i].index = i
    }
  }

  private fun ensureScrollHandle(): RedwoodScrollHandle? {
    val existing = scrollHandle
    if (existing != null) {
      return existing
    }
    return try {
      uniformListHandle.scrollHandle().also { scrollHandle = it }
    } catch (_: Throwable) {
      null
    }
  }

  private fun updateBindings(requestedFirst: Int, requestedLast: Int) {
    if (rowSlots.isEmpty()) {
      currentVisibleRange = null
      currentBindRange = null
      scrollProcessor.onUserScroll(0, 0)
      return
    }

    val clampedFirst = requestedFirst.coerceIn(0, rowSlots.lastIndex)
    val clampedLast = requestedLast.coerceIn(clampedFirst, rowSlots.lastIndex)

    val bindFirst = max(0, clampedFirst - preloadBefore)
    val bindLast = min(rowSlots.lastIndex, clampedLast + preloadAfter)

    traceLazy {
      "updateBindings requested=[$requestedFirst,$requestedLast] clamped=[$clampedFirst,$clampedLast] bind=[$bindFirst,$bindLast] size=${rowSlots.size}"
    }

    for (index in bindFirst..bindLast) {
      rowSlots[index].ensureBound()
    }

    for (index in rowSlots.indices) {
      if (index < bindFirst || index > bindLast) {
        rowSlots[index].detach()
      }
    }

    currentBindRange = bindFirst..bindLast
    currentVisibleRange = clampedFirst..clampedLast
    scrollProcessor.onUserScroll(clampedFirst, clampedLast)
  }

  private inner class Renderer : RedwoodUniformListRenderer {
    private var bindingInProgress = false
    private var pendingVisibleRange: IntRange? = null

    override fun itemCount(): UInt {
      val size = processor.size.toUInt()
      traceLazy { "renderer.itemCount size=$size" }
      return size
    }

    override fun renderItem(index: UInt): RedwoodNodeHandle {
      val slot = rowSlots.getOrNull(index.toInt())
      if (slot == null) {
        traceLazy { "renderer.renderItem index=$index -> placeholder (missing slot)" }
        return RedwoodNodeHandle.placeholder()
      }
      val node = slot.ensureBound()
      if (node == null) {
        traceLazy { "renderer.renderItem index=$index -> placeholder (no node)" }
        return RedwoodNodeHandle.placeholder()
      }
      traceLazy { "renderer.renderItem index=$index -> node" }
      return node.rawNode()
    }

    override fun onVisibleRangeChanged(first: UInt, last: UInt) {
      val size = processor.size
      if (size == 0) {
        currentVisibleRange = null
        currentBindRange = null
        rowSlots.forEach { it.detach() }
        scrollProcessor.onUserScroll(0, 0)
        traceLazy { "renderer.onVisibleRangeChanged first=$first last=$last (empty)" }
        return
      }
      val firstIndex = first.toInt().coerceIn(0, size - 1)
      val lastIndex = last.toInt().coerceIn(firstIndex, size - 1)
      traceLazy { "renderer.onVisibleRangeChanged first=$firstIndex last=$lastIndex size=$size" }

      // Skip if unchanged to avoid redundant work/logs.
      currentVisibleRange?.let { prev ->
        if (prev.first == firstIndex && prev.last == lastIndex) {
          return
        }
      }

      // Defer/serialize binding work to avoid re-entrant render->bind loops.
      pendingVisibleRange = firstIndex..lastIndex
      if (bindingInProgress) return

      while (true) {
        val range = pendingVisibleRange ?: break
        pendingVisibleRange = null
        bindingInProgress = true
        updateBindings(range.first, range.last)
        bindingInProgress = false
      }
    }
  }

  private inner class RowSlot(
    var index: Int,
  ) {
    private var widget: Widget<GpuiNode>? = null
    var binding: Binding<RowSlot, GpuiNode>? = null

    fun ensureBound(): GpuiNode? {
      if (binding?.isBound != true) {
        binding = processor.bind(index, this)
        traceLazy { "RowSlot.ensureBound index=$index -> bound" }
      }
      return widget?.value
    }

    fun setContent(widget: Widget<GpuiNode>?) {
      if (this.widget === widget) return
      val nodeId = widget?.value?.rawNode()?.hashCode()
      traceLazy {
        "RowSlot.setContent index=$index hasWidget=${widget != null} nodeId=${nodeId ?: "null"}"
      }
      val previous = this.widget
      if (previous != null) {
        val ci = childIndex()
        uniformListChildren.remove(ci.toUInt(), 1u)
        traceLazy { "RowSlot.removeChild index=$index childIndex=$ci" }
      }

      this.widget = widget
      if (widget != null) {
        val ci = childIndex()
        uniformListChildren.insert(ci.toUInt(), widget.value.rawNode())
        traceLazy { "RowSlot.insertChild index=$index childIndex=$ci" }
      }
    }

    fun detach() {
      binding?.unbind()
      binding = null
      if (widget != null) {
        traceLazy { "RowSlot.detach index=$index" }
        setContent(null)
      }
      // No uniformListAdapter cache writes here; Rust caches in render path.
    }

    private fun childIndex(): Int {
      var count = 0
      for (i in 0 until index) {
        if (rowSlots[i].widget != null) {
          count++
        }
      }
      return count
    }
  }
}

private class UniformListWidget(
  private val environment: GpuiEnvironment,
  private val node: GpuiNode,
) : Widget<GpuiNode> {
  private var currentModifier: Modifier = Modifier

  override val value: GpuiNode
    get() = node

  override var modifier: Modifier
    get() = currentModifier
    set(value) {
      currentModifier = value
      node.applyModifier(value, environment.density)
    }

  override val allChildren: List<Widget.Children<GpuiNode>> = emptyList()
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
