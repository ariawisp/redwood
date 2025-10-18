package app.cash.redwood.host.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.widget.Widget

private const val DEBUG_CHILDREN = false

public class GpuiChildren(
  private val environment: GpuiEnvironment,
  private val handle: RedwoodChildrenHandle,
  private val parentNode: GpuiNode,
) : Widget.Children<GpuiNode> {
  private val widgetsList = mutableListOf<Widget<GpuiNode>>()

  override val widgets: List<Widget<GpuiNode>>
    get() = widgetsList

  override fun insert(index: Int, widget: Widget<GpuiNode>) {
    if (DEBUG_CHILDREN) {
      println(
        "GpuiChildren: insert widget=${widget::class.simpleName} node=${widget.value.debugId} parent=${parentNode.debugId}",
      )
    }
    widgetsList.add(index, widget)
    parentNode.attachChild(index, widget.value)
    handle.insert(index.toUInt(), widget.value.handle)
    applyModifier(widget, widget.modifier)
    environment.layoutController.onTreeChanged()
  }

  override fun move(fromIndex: Int, toIndex: Int, count: Int) {
    if (count == 0 || fromIndex == toIndex) return

    if (DEBUG_CHILDREN) {
      println(
        "GpuiChildren: move parent=${parentNode.debugId} from=$fromIndex to=$toIndex count=$count",
      )
    }

    handle.moveRange(fromIndex.toUInt(), toIndex.toUInt(), count.toUInt())

    val movingWidgets = widgetsList.subList(fromIndex, fromIndex + count).toList()
    repeat(count) {
      widgetsList.removeAt(fromIndex)
    }
    val destination = if (toIndex > fromIndex) toIndex - count else toIndex
    widgetsList.addAll(destination, movingWidgets)

    val movingNodes = mutableListOf<GpuiNode>()
    repeat(count) {
      movingNodes += parentNode.detachChild(fromIndex)
    }
    movingNodes.forEachIndexed { offset, child ->
      parentNode.attachChild(destination + offset, child)
    }

    environment.layoutController.onTreeChanged()
  }

  override fun remove(index: Int, count: Int) {
    if (count == 0) return

    if (DEBUG_CHILDREN) {
      println(
        "GpuiChildren: remove parent=${parentNode.debugId} index=$index count=$count",
      )
    }

    handle.remove(index.toUInt(), count.toUInt())
    repeat(count) {
      widgetsList.removeAt(index)
      parentNode.detachChild(index)
      // Do NOT dispose the removed widget here. Lazy lists frequently demote
      // and later re-promote the same widget instance; disposing its GPUI node
      // would make subsequent inserts fail with AlreadyDisposed.
    }
    environment.layoutController.onTreeChanged()
  }

  override fun onModifierUpdated(index: Int, widget: Widget<GpuiNode>) {
    applyModifier(widget, widget.modifier)
  }

  override fun detach() {
    if (DEBUG_CHILDREN) {
      println("GpuiChildren: detach parent=${parentNode.debugId}")
    }
    handle.detach()
    while (parentNode.layoutChildren.isNotEmpty()) {
      parentNode.detachChild(parentNode.layoutChildren.lastIndex).dispose()
    }
    widgetsList.clear()
    environment.layoutController.onTreeChanged()
  }

  public fun applyModifier(widget: Widget<GpuiNode>, modifier: Modifier) {
    widget.value.applyModifier(modifier, environment.density)
  }
}
