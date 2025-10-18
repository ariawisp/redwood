@file:OptIn(RedwoodYogaApi::class)

package app.cash.redwood.layout.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.GpuiChildren
import app.cash.redwood.host.gpui.GpuiEnvironment
import app.cash.redwood.host.gpui.GpuiNode
import app.cash.redwood.host.gpui.RedwoodBoxNode
import app.cash.redwood.host.gpui.RedwoodFlexNode
import app.cash.redwood.host.gpui.RedwoodSpacerNode
import app.cash.redwood.host.gpui.RedwoodScrollHandle
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
import app.cash.redwood.yoga.AlignItems
import app.cash.redwood.yoga.AlignSelf
import app.cash.redwood.yoga.FlexDirection
import app.cash.redwood.yoga.JustifyContent
import app.cash.redwood.yoga.Node
import app.cash.redwood.yoga.RedwoodYogaApi

public interface GpuiScrollHandleProvider {
  public fun scrollHandle(): RedwoodScrollHandle?
}

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
  protected val node: RedwoodFlexNode,
  private val direction: FlexDirection,
) : Widget<GpuiNode> {
  private var widthConstraint: Constraint = Constraint.Wrap
  private var heightConstraint: Constraint = Constraint.Wrap
  private var scrollListener: ScrollListener? = null

  private val layoutNode = Node().apply {
    flexDirection = this@GpuiFlexContainer.direction
  }

  final override val value: GpuiNode = GpuiNode(
    handle = node.rawNode(),
    layoutController = environment.layoutController,
    layoutNode = layoutNode,
    measureSelf = false,
  )

  protected val childrenContainer = GpuiChildren(environment, node.children(), value)

  open override val allChildren: List<Widget.Children<GpuiNode>> = listOf(childrenContainer)

  final override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.applyModifier(value, environment.density)
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
    applyLayoutMargin(margin)
    value.markNeedsLayout()
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
    layoutNode.justifyContent = alignment.toYogaJustifyContent()
    value.markNeedsLayout()
  }

  protected fun setCrossAxisAlignment(alignment: CrossAxisAlignment) {
    node.setCrossAxisAlignment(alignment.toGpui())
    layoutNode.alignItems = alignment.toYogaAlignItems()
    value.markNeedsLayout()
  }

  private fun applyConstraints() {
    node.setConstraints(widthConstraint.toGpui(), heightConstraint.toGpui())
    updateLayoutSizing()
    value.markNeedsLayout()
  }

  private fun updateLayoutSizing() {
    when (direction) {
      FlexDirection.Row -> {
        layoutNode.flexGrow = if (widthConstraint == Constraint.Fill) 1f else 0f
        layoutNode.flexShrink = 0f
        layoutNode.alignSelf = if (heightConstraint == Constraint.Fill) AlignSelf.Stretch else AlignSelf.Auto
      }
      FlexDirection.Column -> {
        layoutNode.flexGrow = if (heightConstraint == Constraint.Fill) 1f else 0f
        layoutNode.flexShrink = 0f
        layoutNode.alignSelf = if (widthConstraint == Constraint.Fill) AlignSelf.Stretch else AlignSelf.Auto
      }
    }
  }

  private fun applyLayoutMargin(margin: Margin) {
    val edgeInsets = margin.toEdgeInsets(environment.density)
    layoutNode.marginStart = edgeInsets.start
    layoutNode.marginEnd = edgeInsets.end
    layoutNode.marginTop = edgeInsets.top
    layoutNode.marginBottom = edgeInsets.bottom
  }
}

private class GpuiRow(
  environment: GpuiEnvironment,
) : GpuiFlexContainer(environment, environment.surface.createRow(), FlexDirection.Row),
  Row<GpuiNode> {
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
) : GpuiFlexContainer(environment, environment.surface.createColumn(), FlexDirection.Column),
  Column<GpuiNode>,
  GpuiScrollHandleProvider {
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

  override fun scrollHandle(): RedwoodScrollHandle? = node.scrollHandle()
}

private class GpuiBox(
  private val environment: GpuiEnvironment,
) : Box<GpuiNode> {
  private val node = environment.surface.createBox()
  private val layoutNode = Node().apply {
    flexDirection = FlexDirection.Column
  }

  private var widthConstraint: Constraint = Constraint.Wrap
  private var heightConstraint: Constraint = Constraint.Wrap
  private var matchParentWidth: Boolean = false
  private var matchParentHeight: Boolean = false
  private var margin: Margin = Margin.Zero

  override val value: GpuiNode = GpuiNode(
    handle = node.rawNode(),
    layoutController = environment.layoutController,
    layoutNode = layoutNode,
    measureSelf = false,
  )

  private val _children = GpuiChildren(environment, node.children(), value)

  init {
    applyConstraints()
  }

  override val children: Widget.Children<GpuiNode>
    get() = _children

  override val allChildren: List<Widget.Children<GpuiNode>> = listOf(_children)

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.applyModifier(value, environment.density)
    }

  override fun width(width: Constraint) {
    widthConstraint = width
    applyConstraints()
  }

  override fun height(height: Constraint) {
    heightConstraint = height
    applyConstraints()
  }

  override fun horizontalAlignment(horizontalAlignment: CrossAxisAlignment) {
    matchParentWidth = horizontalAlignment == CrossAxisAlignment.Stretch
    node.setHorizontalAlignment(horizontalAlignment.toGpui())
    layoutNode.alignItems = horizontalAlignment.toYogaAlignItems()
    applyConstraints()
  }

  override fun verticalAlignment(verticalAlignment: CrossAxisAlignment) {
    matchParentHeight = verticalAlignment == CrossAxisAlignment.Stretch
    node.setVerticalAlignment(verticalAlignment.toGpui())
    layoutNode.justifyContent = verticalAlignment.toYogaBoxJustifyContent()
    applyConstraints()
  }

  override fun margin(margin: Margin) {
    if (this.margin != margin) {
      this.margin = margin
      node.setMargin(margin.toEdgeInsets(environment.density))
      val edgeInsets = margin.toEdgeInsets(environment.density)
      layoutNode.marginStart = edgeInsets.start
      layoutNode.marginEnd = edgeInsets.end
      layoutNode.marginTop = edgeInsets.top
      layoutNode.marginBottom = edgeInsets.bottom
      value.markNeedsLayout()
    }
  }

  private fun applyConstraints() {
    val width = when {
      widthConstraint == Constraint.Fill || matchParentWidth -> Constraint.Fill
      else -> Constraint.Wrap
    }
    val height = when {
      heightConstraint == Constraint.Fill || matchParentHeight -> Constraint.Fill
      else -> Constraint.Wrap
    }
    node.setConstraints(width.toGpui(), height.toGpui())

    layoutNode.alignSelf = if (width == Constraint.Fill) AlignSelf.Stretch else AlignSelf.Auto
    layoutNode.flexGrow = if (height == Constraint.Fill) 1f else 0f
    layoutNode.flexShrink = 1f

    value.markNeedsLayout()
  }
}

private class GpuiSpacer(
  private val environment: GpuiEnvironment,
) : Spacer<GpuiNode> {
  private val node = environment.surface.createSpacer()

  override val value: GpuiNode = GpuiNode(
    handle = node.rawNode(),
    layoutController = environment.layoutController,
  )

  override val allChildren: List<Widget.Children<GpuiNode>> = emptyList()

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.applyModifier(value, environment.density)
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

private fun MainAxisAlignment.toYogaJustifyContent(): JustifyContent = when (this) {
  MainAxisAlignment.Start -> JustifyContent.FlexStart
  MainAxisAlignment.Center -> JustifyContent.Center
  MainAxisAlignment.End -> JustifyContent.FlexEnd
  MainAxisAlignment.SpaceBetween -> JustifyContent.SpaceBetween
  MainAxisAlignment.SpaceAround -> JustifyContent.SpaceAround
  MainAxisAlignment.SpaceEvenly -> JustifyContent.SpaceEvenly
  else -> JustifyContent.FlexStart
}

private fun CrossAxisAlignment.toYogaAlignItems(): AlignItems = when (this) {
  CrossAxisAlignment.Start -> AlignItems.FlexStart
  CrossAxisAlignment.Center -> AlignItems.Center
  CrossAxisAlignment.End -> AlignItems.FlexEnd
  CrossAxisAlignment.Stretch -> AlignItems.Stretch
  else -> AlignItems.FlexStart
}

private fun CrossAxisAlignment.toYogaBoxJustifyContent(): JustifyContent = when (this) {
  CrossAxisAlignment.Start -> JustifyContent.FlexStart
  CrossAxisAlignment.Center -> JustifyContent.Center
  CrossAxisAlignment.End -> JustifyContent.FlexEnd
  CrossAxisAlignment.Stretch -> JustifyContent.FlexStart
  else -> JustifyContent.FlexStart
}
