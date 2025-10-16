package app.cash.redwood.host.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.widget.Widget

public class GpuiChildren(
  private val handle: RedwoodChildrenHandle,
) : Widget.Children<GpuiNode> {
  private val widgetsList = mutableListOf<Widget<GpuiNode>>()

  override val widgets: List<Widget<GpuiNode>>
    get() = widgetsList

  override fun insert(index: Int, widget: Widget<GpuiNode>) {
    widgetsList.add(index, widget)
    handle.insert(index.toUInt(), widget.value.handle)
    applyModifier(widget, widget.modifier)
  }

  override fun move(fromIndex: Int, toIndex: Int, count: Int) {
    if (count == 0 || fromIndex == toIndex) return

    handle.moveRange(fromIndex.toUInt(), toIndex.toUInt(), count.toUInt())

    val moving = widgetsList.subList(fromIndex, fromIndex + count)
    val moved = moving.toList()
    moving.clear()
    val destination = if (toIndex > fromIndex) toIndex - count else toIndex
    widgetsList.addAll(destination, moved)
  }

  override fun remove(index: Int, count: Int) {
    if (count == 0) return

    handle.remove(index.toUInt(), count.toUInt())
    repeat(count) {
      val removed = widgetsList.removeAt(index)
      removed.value.dispose()
    }
  }

  override fun onModifierUpdated(index: Int, widget: Widget<GpuiNode>) {
    applyModifier(widget, widget.modifier)
  }

  override fun detach() {
    handle.detach()
    widgetsList.forEach { it.value.dispose() }
    widgetsList.clear()
  }

  public fun applyModifier(widget: Widget<GpuiNode>, modifier: Modifier) {
    widget.value.modifier = modifier
    widget.value.markNeedsLayout()
  }
}
