@file:OptIn(RedwoodYogaApi::class)

package app.cash.redwood.host.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.LayoutFrame
import app.cash.redwood.host.gpui.MeasureInput
import app.cash.redwood.host.gpui.MeasureMode as HostMeasureMode
import app.cash.redwood.host.gpui.SizeF
import app.cash.redwood.ui.Density
import app.cash.redwood.yoga.MeasureCallback
import app.cash.redwood.yoga.MeasureMode as YogaMeasureMode
import app.cash.redwood.yoga.Node
import app.cash.redwood.yoga.Size
import app.cash.redwood.yoga.RedwoodYogaApi

public class GpuiNode(
  internal val handle: RedwoodNodeHandle,
  internal val layoutController: GpuiLayoutController,
  internal val layoutNode: Node = Node(),
  internal val shouldApplyLayoutFrame: Boolean = true,
  private val measureSelf: Boolean = true,
  private val onRequestFocus: (() -> Boolean)? = null,
) {
  internal val layoutChildren: MutableList<GpuiNode> = mutableListOf()
  internal var parent: GpuiNode? = null

  public var modifier: Modifier = Modifier
    private set

  init {
    layoutNode.context = this
    if (measureSelf) {
      layoutNode.measureCallback = RedwoodMeasureCallback
    }
  }

  public fun markNeedsLayout() {
    runCatching {
      handle.markNeedsLayout()
    }
    layoutController.requestLayout()
  }

  public fun dispose() {
    layoutNode.context = null
    runCatching {
      handle.dispose()
    }
  }

  public fun requestFocus(): Boolean {
    return onRequestFocus?.invoke() == true
  }

  public fun applyModifier(modifier: Modifier, density: Density) {
    this.modifier = modifier
    layoutNode.applyModifier(modifier, density)
    markNeedsLayout()
  }

  internal fun attachChild(index: Int, child: GpuiNode) {
    child.detachFromParent()
    child.parent = this
    layoutChildren.add(index, child)
    layoutNode.children.add(index, child.layoutNode)
  }

  internal fun detachChild(index: Int): GpuiNode {
    val removed = layoutChildren.removeAt(index)
    layoutNode.children.removeAt(index)
    removed.parent = null
    return removed
  }

  internal fun detachFromParent() {
    val currentParent = parent ?: return
    val index = currentParent.layoutChildren.indexOf(this)
    if (index >= 0) {
      currentParent.layoutChildren.removeAt(index)
      currentParent.layoutNode.children.removeAt(index)
    }
    parent = null
  }

  internal fun setLayoutFrame(frame: LayoutFrame) {
    if (!shouldApplyLayoutFrame) return
    runCatching {
      handle.setLayoutFrame(frame)
    }
  }

  private object RedwoodMeasureCallback : MeasureCallback {
    override fun measure(
      node: Node,
      width: Float,
      widthMode: YogaMeasureMode,
      height: Float,
      heightMode: YogaMeasureMode,
    ): Size {
      val gpuiNode = node.context as? GpuiNode ?: return ZERO_SIZE
      val input = MeasureInput(
        width = width.toMeasureValue(),
        widthMode = widthMode.toHostMode(),
        height = height.toMeasureValue(),
        heightMode = heightMode.toHostMode(),
      )

      val size = runCatching { gpuiNode.handle.measure(input) }.getOrNull()
        ?: SizeF(width = 0f, height = 0f)
      return Size(size.width, size.height)
    }
  }

  private companion object {
    private val ZERO_SIZE = Size(0f, 0f)
  }
}

private fun Float.toMeasureValue(): Float = when {
  isNaN() || !isFinite() -> 0f
  this < 0f -> 0f
  else -> this
}

private fun YogaMeasureMode.toHostMode(): HostMeasureMode = when (toString()) {
  "Undefined" -> HostMeasureMode.UNDEFINED
  "Exactly" -> HostMeasureMode.EXACTLY
  "AtMost" -> HostMeasureMode.AT_MOST
  else -> HostMeasureMode.UNDEFINED
}
