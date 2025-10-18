@file:OptIn(RedwoodYogaApi::class)

package app.cash.redwood.host.gpui

import app.cash.redwood.host.gpui.LayoutFrame
import app.cash.redwood.yoga.RedwoodYogaApi
import app.cash.redwood.yoga.Size

/**
 * Drives Yoga layout for a GPUI-backed Redwood surface. The controller runs synchronously on the
 * GPUI thread whenever layout is requested and keeps the Rust-side nodes updated with the latest
 * frames.
 */
public class GpuiLayoutController {
  private var rootNode: GpuiNode? = null
  private var viewportWidth: Float = Size.UNDEFINED
  private var viewportHeight: Float = Size.UNDEFINED

  private var pendingLayout: Boolean = false
  private var layoutInProgress: Boolean = false
  private var treeDirty: Boolean = true

  fun attachRoot(root: GpuiNode) {
    rootNode = root
    treeDirty = true
  }

  fun updateViewport(size: GpuiWindowSize) {
    viewportWidth = size.width
    viewportHeight = size.height
    if (debugViewportEnabled()) {
      println("[gpui-host][viewport] width=$viewportWidth height=$viewportHeight")
    }
    requestLayout()
  }

  fun onTreeChanged() {
    treeDirty = true
    requestLayout()
  }

  fun requestLayout() {
    pendingLayout = true
    if (!layoutInProgress) {
      performLayout()
    }
  }

  private fun performLayout() {
    val root = rootNode ?: return
    if (!pendingLayout) return

    layoutInProgress = true
    try {
      while (pendingLayout) {
        pendingLayout = false
        layoutOnce(root)
      }
    } finally {
      layoutInProgress = false
    }
  }

  private fun layoutOnce(root: GpuiNode) {
    if (treeDirty) {
      root.layoutNode.markEverythingDirty()
      treeDirty = false
    }

    val ownerWidth = viewportWidth.takeIf { it > 0f } ?: Size.UNDEFINED
    val ownerHeight = viewportHeight.takeIf { it > 0f } ?: Size.UNDEFINED

    root.layoutNode.requestedWidth = ownerWidth
    root.layoutNode.requestedMaxWidth = Size.UNDEFINED
    root.layoutNode.requestedHeight = ownerHeight
    root.layoutNode.requestedMaxHeight = Size.UNDEFINED

    root.layoutNode.measureOnly(ownerWidth, ownerHeight)

    applyLayoutFrames(root, 0f, 0f)
  }

  private fun applyLayoutFrames(node: GpuiNode, offsetX: Float, offsetY: Float) {
    val yogaNode = node.layoutNode
    if (node.shouldApplyLayoutFrame) {
      val frame = LayoutFrame(
        x = offsetX + yogaNode.left,
        y = offsetY + yogaNode.top,
        width = yogaNode.width,
        height = yogaNode.height,
      )
      node.setLayoutFrame(frame)
    }

    val childOffsetX = offsetX + yogaNode.left
    val childOffsetY = offsetY + yogaNode.top
    node.layoutChildren.forEach { child ->
      applyLayoutFrames(child, childOffsetX, childOffsetY)
    }
  }

  private fun debugViewportEnabled(): Boolean {
    val value = getEnv("GPUI_HOST_DEBUG_VIEWPORT") ?: return false
    return value == "1" ||
      value.equals("true", ignoreCase = true) ||
      value.equals("yes", ignoreCase = true) ||
      value.equals("on", ignoreCase = true)
  }
}

@Suppress("NO_ACTUAL_FOR_EXPECT") // The per-platform implementations live in platform source sets.
internal expect fun getEnv(name: String): String?
