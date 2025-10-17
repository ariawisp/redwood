package app.cash.redwood.lazylayout.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.GpuiEnvironment
import app.cash.redwood.host.gpui.GpuiNode
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.layout.api.Overflow
import app.cash.redwood.layout.gpui.GpuiRedwoodLayoutWidgetFactory
import app.cash.redwood.lazylayout.api.ScrollItemIndex
import app.cash.redwood.lazylayout.widget.LazyList
import app.cash.redwood.lazylayout.widget.RedwoodLazyLayoutWidgetFactory
import app.cash.redwood.lazylayout.widget.RefreshableLazyList
import app.cash.redwood.ui.Margin
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
) : LazyList<GpuiNode> {
  private val layoutFactory = GpuiRedwoodLayoutWidgetFactory(environment)
  private val column = layoutFactory.Column().apply {
    overflow(Overflow.Scroll)
    // Ensure the lazy list consumes available viewport height by default so rows are visible.
    height(Constraint.Fill)
  }

  override var modifier: Modifier
    get() = column.modifier
    set(value) {
      column.modifier = value
    }

  private val placeholderChildren = object : Widget.Children<GpuiNode> {
    private val _widgets = mutableListOf<Widget<GpuiNode>>()

    override val widgets: List<Widget<GpuiNode>>
      get() = _widgets

    override fun insert(index: Int, widget: Widget<GpuiNode>) {
      if (index >= _widgets.size) {
        _widgets += widget
      } else {
        _widgets.add(index, widget)
      }
    }

    override fun move(fromIndex: Int, toIndex: Int, count: Int) {
      val moved = mutableListOf<Widget<GpuiNode>>()
      repeat(count) {
        moved += _widgets.removeAt(fromIndex)
      }
      _widgets.addAll(toIndex, moved)
    }

    override fun remove(index: Int, count: Int) {
      repeat(count) {
        _widgets.removeAt(index)
      }
    }

    override fun onModifierUpdated(index: Int, widget: Widget<GpuiNode>) {
      // Placeholders are not rendered.
    }

    override fun detach() {
      _widgets.clear()
    }
  }

  override val value: GpuiNode
    get() = column.value

  override val items: Widget.Children<GpuiNode>
    get() = column.children

  override val placeholder: Widget.Children<GpuiNode>
    get() = placeholderChildren

  override fun isVertical(isVertical: Boolean) {
    // Only vertical lists are currently supported. Additional orientations may be added later.
  }

  override fun onViewportChanged(onViewportChanged: (Int, Int) -> Unit) {
    // Viewport tracking is not yet supported for the GPUI lazy list host.
  }

  override fun itemsBefore(itemsBefore: Int) {
  }

  override fun itemsAfter(itemsAfter: Int) {
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
    // Programmatic scrolling is not yet implemented.
  }
}

private class GpuiRefreshableLazyList(
  environment: GpuiEnvironment,
) : RefreshableLazyList<GpuiNode> {
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
}
