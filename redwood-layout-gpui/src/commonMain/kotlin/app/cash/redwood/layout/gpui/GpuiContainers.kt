package app.cash.redwood.layout.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.layout.api.MainAxisAlignment
import app.cash.redwood.layout.api.Overflow
import app.cash.redwood.layout.widget.Box
import app.cash.redwood.layout.widget.Column
import app.cash.redwood.layout.widget.RedwoodLayoutWidgetFactory
import app.cash.redwood.layout.widget.Row
import app.cash.redwood.layout.widget.Spacer
import app.cash.redwood.ui.Px
import app.cash.redwood.ui.Dp
import app.cash.redwood.ui.basic.gpui.GpuiHandle
import app.cash.redwood.ui.basic.gpui.gpui
import app.cash.redwood.widget.Widget

internal class GpuiChildren(private val parent: GpuiHandle) : Widget.Children<GpuiHandle> {
  private val list = mutableListOf<Widget<GpuiHandle>>()
  override val widgets: List<Widget<GpuiHandle>> get() = list

  override fun insert(index: Int, widget: Widget<GpuiHandle>) {
    list.add(index, widget)
    gpui.insertChild(parent, index, widget.value)
  }

  override fun move(fromIndex: Int, toIndex: Int, count: Int) {
    if (count <= 0) return
    val moved = ArrayList<Widget<GpuiHandle>>(count)
    repeat(count) { moved.add(list.removeAt(fromIndex)) }
    var insertIndex = toIndex
    moved.forEach { w ->
      list.add(insertIndex, w)
      gpui.insertChild(parent, insertIndex, w.value)
      insertIndex++
    }
  }

  override fun remove(index: Int, count: Int) {
    repeat(count) {
      val w = list.removeAt(index)
      gpui.removeChild(parent, w.value)
    }
  }

  override fun onModifierUpdated(index: Int, widget: Widget<GpuiHandle>) {
    // No-op for now.
  }

  override fun detach() {
    list.clear()
  }
}

internal class GpuiRow(
  override val value: GpuiHandle,
) : Row<GpuiHandle> {
  override var modifier: Modifier = Modifier
  private val _children = GpuiChildren(value)
  override val children: Widget.Children<GpuiHandle> get() = _children
  override val allChildren: List<Widget.Children<GpuiHandle>> get() = listOf(_children)
  private var widthConstraint: Constraint = Constraint.Wrap
  private var heightConstraint: Constraint = Constraint.Wrap
  private var mainAlign: Int = 0
  private var crossAlign: Int = 0
  override fun horizontalAlignment(alignment: MainAxisAlignment) {
    mainAlign = when (alignment) {
      MainAxisAlignment.Start -> 0
      MainAxisAlignment.Center -> 1
      MainAxisAlignment.End -> 2
      MainAxisAlignment.SpaceBetween -> 3
      MainAxisAlignment.SpaceAround -> 4
      MainAxisAlignment.SpaceEvenly -> 5
      else -> 0
    }
    gpui.setAlign(value, mainAlign, crossAlign)
  }
  override fun verticalAlignment(alignment: CrossAxisAlignment) {
    crossAlign = when (alignment) {
      CrossAxisAlignment.Start -> 0
      CrossAxisAlignment.Center -> 1
      CrossAxisAlignment.End -> 2
      CrossAxisAlignment.Stretch -> 3
      else -> 0
    }
    gpui.setAlign(value, mainAlign, crossAlign)
  }
  override fun width(constraint: Constraint) { widthConstraint = constraint; applySize() }
  override fun height(constraint: Constraint) { heightConstraint = constraint; applySize() }
  override fun margin(margin: app.cash.redwood.ui.Margin) {
    gpui.setPadding(value, margin.start.value.toFloat(), margin.top.value.toFloat(), margin.end.value.toFloat(), margin.bottom.value.toFloat())
  }
  override fun overflow(overflow: Overflow) { /* TODO: map overflow */ }
  override fun onScroll(onScroll: ((Px) -> Unit)?) {}

  private fun applySize() {
    val w = if (widthConstraint == Constraint.Fill) -1f else null
    val h = if (heightConstraint == Constraint.Fill) -1f else null
    gpui.setSize(value, w, h)
  }
}

internal class GpuiColumn(
  override val value: GpuiHandle,
) : Column<GpuiHandle> {
  override var modifier: Modifier = Modifier
  private val _children = GpuiChildren(value)
  override val children: Widget.Children<GpuiHandle> get() = _children
  override val allChildren: List<Widget.Children<GpuiHandle>> get() = listOf(_children)
  private var widthConstraint: Constraint = Constraint.Wrap
  private var heightConstraint: Constraint = Constraint.Wrap
  private var mainAlign: Int = 0
  private var crossAlign: Int = 0
  override fun horizontalAlignment(alignment: CrossAxisAlignment) {
    crossAlign = when (alignment) {
      CrossAxisAlignment.Start -> 0
      CrossAxisAlignment.Center -> 1
      CrossAxisAlignment.End -> 2
      CrossAxisAlignment.Stretch -> 3
      else -> 0
    }
    gpui.setAlign(value, mainAlign, crossAlign)
  }
  override fun verticalAlignment(alignment: MainAxisAlignment) {
    mainAlign = when (alignment) {
      MainAxisAlignment.Start -> 0
      MainAxisAlignment.Center -> 1
      MainAxisAlignment.End -> 2
      MainAxisAlignment.SpaceBetween -> 3
      MainAxisAlignment.SpaceAround -> 4
      MainAxisAlignment.SpaceEvenly -> 5
      else -> 0
    }
    gpui.setAlign(value, mainAlign, crossAlign)
  }
  override fun width(constraint: Constraint) { widthConstraint = constraint; applySize() }
  override fun height(constraint: Constraint) { heightConstraint = constraint; applySize() }
  override fun margin(margin: app.cash.redwood.ui.Margin) {
    gpui.setPadding(value, margin.start.value.toFloat(), margin.top.value.toFloat(), margin.end.value.toFloat(), margin.bottom.value.toFloat())
  }
  override fun overflow(overflow: Overflow) { /* TODO */ }
  override fun onScroll(onScroll: ((Px) -> Unit)?) {}

  private fun applySize() {
    val w = if (widthConstraint == Constraint.Fill) -1f else null
    val h = if (heightConstraint == Constraint.Fill) -1f else null
    gpui.setSize(value, w, h)
  }
}

internal class GpuiBox(
  override val value: GpuiHandle,
) : Box<GpuiHandle> {
  override var modifier: Modifier = Modifier
  private val _children = GpuiChildren(value)
  override val children: Widget.Children<GpuiHandle> get() = _children
  override val allChildren: List<Widget.Children<GpuiHandle>> get() = listOf(_children)
  private var widthConstraint: Constraint = Constraint.Wrap
  private var heightConstraint: Constraint = Constraint.Wrap
  override fun width(constraint: Constraint) { widthConstraint = constraint; applySize() }
  override fun height(constraint: Constraint) { heightConstraint = constraint; applySize() }
  override fun horizontalAlignment(alignment: CrossAxisAlignment) { /* TODO */ }
  override fun verticalAlignment(alignment: CrossAxisAlignment) { /* TODO */ }
  override fun margin(margin: app.cash.redwood.ui.Margin) {
    gpui.setPadding(value, margin.start.value.toFloat(), margin.top.value.toFloat(), margin.end.value.toFloat(), margin.bottom.value.toFloat())
  }

  private fun applySize() {
    val w = if (widthConstraint == Constraint.Fill) -1f else null
    val h = if (heightConstraint == Constraint.Fill) -1f else null
    gpui.setSize(value, w, h)
  }
}

internal class GpuiSpacer(
  override val value: GpuiHandle,
) : Spacer<GpuiHandle> {
  override var modifier: Modifier = Modifier
  override fun width(width: Dp) {}
  override fun height(height: Dp) {}
  override val allChildren: List<Widget.Children<GpuiHandle>> get() = emptyList()
}

public class GpuiRedwoodLayoutWidgetFactory : RedwoodLayoutWidgetFactory<GpuiHandle> {
  override fun Row(): Row<GpuiHandle> {
    val h = gpui.createRow()
    return GpuiRow(h)
  }
  override fun Column(): Column<GpuiHandle> {
    val h = gpui.createColumn()
    return GpuiColumn(h)
  }
  override fun Box(): Box<GpuiHandle> {
    val h = gpui.createColumn() // approximate as column
    return GpuiBox(h)
  }
  override fun Spacer(): Spacer<GpuiHandle> {
    // No concrete node; use a zero-handle placeholder
    return GpuiSpacer(0L)
  }
}
