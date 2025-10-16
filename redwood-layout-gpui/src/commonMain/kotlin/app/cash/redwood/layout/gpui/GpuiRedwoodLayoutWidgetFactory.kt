package app.cash.redwood.layout.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.GpuiChildren
import app.cash.redwood.host.gpui.GpuiEnvironment
import app.cash.redwood.host.gpui.GpuiNode
import app.cash.redwood.host.gpui.RedwoodBoxNode
import app.cash.redwood.host.gpui.RedwoodFlexNode
import app.cash.redwood.host.gpui.RedwoodSpacerNode
import app.cash.redwood.host.gpui.ScrollListener
import app.cash.redwood.host.gpui.scrollListener
import app.cash.redwood.host.gpui.toEdgeInsets
import app.cash.redwood.host.gpui.toGpui
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.layout.api.MainAxisAlignment
import app.cash.redwood.layout.api.Overflow
import app.cash.redwood.layout.widget.Box
import app.cash.redwood.layout.widget.Column
import app.cash.redwood.layout.widget.RedwoodLayoutWidgetFactory
import app.cash.redwood.layout.widget.Row
import app.cash.redwood.layout.widget.Spacer
import app.cash.redwood.ui.Dp
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.Px
import app.cash.redwood.widget.Widget

public class GpuiRedwoodLayoutWidgetFactory(
  private val environment: GpuiEnvironment,
) : RedwoodLayoutWidgetFactory<GpuiNode> {
  override fun Row(): Row<GpuiNode> = GpuiRow(environment)

  override fun Column(): Column<GpuiNode> = GpuiColumn(environment)

  override fun Box(): Box<GpuiNode> = GpuiBox(environment)

  override fun Spacer(): Spacer<GpuiNode> = GpuiSpacer(environment)
}

private abstract class GpuiFlexContainer(
  protected val environment: GpuiEnvironment,
  private val node: RedwoodFlexNode,
) : Widget<GpuiNode> {
  private var widthConstraint: Constraint = Constraint.Wrap
  private var heightConstraint: Constraint = Constraint.Wrap
  private var scrollListener: ScrollListener? = null

  final override val value: GpuiNode = GpuiNode(node.rawNode())

  protected val childrenContainer = GpuiChildren(node.children())

  open override val allChildren: List<Widget.Children<GpuiNode>> = listOf(childrenContainer)

  final override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.modifier = value
      this.value.markNeedsLayout()
    }

  open fun width(width: Constraint) {
    if (widthConstraint != width) {
      widthConstraint = width
      applyConstraints()
    }
  }

  open fun height(height: Constraint) {
    if (heightConstraint != height) {
      heightConstraint = height
      applyConstraints()
    }
  }

  open fun margin(margin: Margin) {
    node.setMargin(margin.toEdgeInsets(environment.density))
  }

  open fun overflow(overflow: Overflow) {
    node.setOverflow(overflow.toGpui())
  }

  open fun onScroll(onScroll: ((Px) -> Unit)?) {
    scrollListener = scrollListener(onScroll)
    node.setScrollListener(scrollListener)
  }

  protected fun setMainAxisAlignment(alignment: MainAxisAlignment) {
    node.setMainAxisAlignment(alignment.toGpui())
  }

  protected fun setCrossAxisAlignment(alignment: CrossAxisAlignment) {
    node.setCrossAxisAlignment(alignment.toGpui())
  }

  private fun applyConstraints() {
    node.setConstraints(widthConstraint.toGpui(), heightConstraint.toGpui())
  }
}

private class GpuiRow(
  environment: GpuiEnvironment,
) : GpuiFlexContainer(environment, environment.surface.createRow()), Row<GpuiNode> {
  override val children: Widget.Children<GpuiNode>
    get() = childrenContainer

  override val allChildren: List<Widget.Children<GpuiNode>>
    get() = super<GpuiFlexContainer>.allChildren

  override fun width(width: Constraint) = super.width(width)

  override fun height(height: Constraint) = super.height(height)

  override fun margin(margin: Margin) = super.margin(margin)

  override fun overflow(overflow: Overflow) = super.overflow(overflow)

  override fun onScroll(onScroll: ((Px) -> Unit)?) = super.onScroll(onScroll)

  override fun horizontalAlignment(horizontalAlignment: MainAxisAlignment) {
    setMainAxisAlignment(horizontalAlignment)
  }

  override fun verticalAlignment(verticalAlignment: CrossAxisAlignment) {
    setCrossAxisAlignment(verticalAlignment)
  }
}

private class GpuiColumn(
  environment: GpuiEnvironment,
) : GpuiFlexContainer(environment, environment.surface.createColumn()), Column<GpuiNode> {
  override val children: Widget.Children<GpuiNode>
    get() = childrenContainer

  override val allChildren: List<Widget.Children<GpuiNode>>
    get() = super<GpuiFlexContainer>.allChildren

  override fun width(width: Constraint) = super.width(width)

  override fun height(height: Constraint) = super.height(height)

  override fun margin(margin: Margin) = super.margin(margin)

  override fun overflow(overflow: Overflow) = super.overflow(overflow)

  override fun onScroll(onScroll: ((Px) -> Unit)?) = super.onScroll(onScroll)

  override fun horizontalAlignment(horizontalAlignment: CrossAxisAlignment) {
    setCrossAxisAlignment(horizontalAlignment)
  }

  override fun verticalAlignment(verticalAlignment: MainAxisAlignment) {
    setMainAxisAlignment(verticalAlignment)
  }
}

private class GpuiBox(
  private val environment: GpuiEnvironment,
) : Box<GpuiNode> {
  private val node = environment.surface.createBox()
  private val _children = GpuiChildren(node.children())

  private var widthConstraint: Constraint = Constraint.Wrap
  private var heightConstraint: Constraint = Constraint.Wrap
  private var margin: Margin = Margin.Zero

  override val value: GpuiNode = GpuiNode(node.rawNode())

  override val children: Widget.Children<GpuiNode>
    get() = _children

  override val allChildren: List<Widget.Children<GpuiNode>> = listOf(_children)

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.modifier = value
      this.value.markNeedsLayout()
    }

  override fun width(width: Constraint) {
    widthConstraint = width
    value.markNeedsLayout()
  }

  override fun height(height: Constraint) {
    heightConstraint = height
    value.markNeedsLayout()
  }

  override fun horizontalAlignment(horizontalAlignment: CrossAxisAlignment) {
    node.setHorizontalAlignment(horizontalAlignment.toMainAxisAlignment().toGpui())
  }

  override fun verticalAlignment(verticalAlignment: CrossAxisAlignment) {
    node.setVerticalAlignment(verticalAlignment.toGpui())
  }

  override fun margin(margin: Margin) {
    if (this.margin != margin) {
      this.margin = margin
      value.markNeedsLayout()
      environment.surface.requestLayout()
    }
  }
}

private fun CrossAxisAlignment.toMainAxisAlignment(): MainAxisAlignment =
  when (this) {
    CrossAxisAlignment.Start -> MainAxisAlignment.Start
    CrossAxisAlignment.Center -> MainAxisAlignment.Center
    CrossAxisAlignment.End -> MainAxisAlignment.End
    CrossAxisAlignment.Stretch -> MainAxisAlignment.Start
    else -> MainAxisAlignment.Start
  }

private class GpuiSpacer(
  private val environment: GpuiEnvironment,
) : Spacer<GpuiNode> {
  private val node = environment.surface.createSpacer()

  override val value: GpuiNode = GpuiNode(node.rawNode())

  override val allChildren: List<Widget.Children<GpuiNode>> = emptyList()

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.modifier = value
      this.value.markNeedsLayout()
    }

  override fun width(width: Dp) {
    val px = environment.density.run { width.toPx().toFloat() }
    node.setWidth(px)
  }

  override fun height(height: Dp) {
    val px = environment.density.run { height.toPx().toFloat() }
    node.setHeight(px)
  }
}
