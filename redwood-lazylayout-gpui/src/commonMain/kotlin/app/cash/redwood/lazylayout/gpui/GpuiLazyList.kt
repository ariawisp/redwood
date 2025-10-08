package app.cash.redwood.lazylayout.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.lazylayout.widget.LazyList
import app.cash.redwood.lazylayout.widget.RedwoodLazyLayoutWidgetFactory
import app.cash.redwood.ui.basic.gpui.GpuiHandle
import app.cash.redwood.widget.Widget

internal class GpuiLazyList(
  override val value: GpuiHandle,
) : LazyList {
  override var modifier: Modifier = Modifier
  private val children = object : Widget.Children<GpuiHandle> {
    private val list = mutableListOf<Widget<GpuiHandle>>()
    override val widgets: List<Widget<GpuiHandle>> get() = list
    override fun insert(index: Int, widget: Widget<GpuiHandle>) { list.add(index, widget) }
    override fun move(fromIndex: Int, toIndex: Int, count: Int) {
      if (count <= 0) return
      val moved = ArrayList<Widget<GpuiHandle>>(count)
      repeat(count) { moved.add(list.removeAt(fromIndex)) }
      var insertIndex = toIndex
      moved.forEach { w -> list.add(insertIndex++, w) }
    }
    override fun remove(index: Int, count: Int) { repeat(count) { list.removeAt(index) } }
    override fun onModifierUpdated(index: Int, widget: Widget<GpuiHandle>) {}
    override fun detach() { list.clear() }
  }
  override fun getChildren(): Widget.Children<GpuiHandle> = children
  override val allChildren: List<Widget.Children<GpuiHandle>> get() = listOf(children)
  override fun itemCount(count: Int) { /* no-op for now */ }
}

public class GpuiRedwoodLazyLayoutWidgetFactory : RedwoodLazyLayoutWidgetFactory {
  override fun LazyList(): LazyList = GpuiLazyList(0L)
  override fun RefreshableLazyList(): LazyList = GpuiLazyList(0L)
}
