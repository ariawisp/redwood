/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(RedwoodYogaApi::class)

package app.cash.redwood.host.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.LayoutFrame
import app.cash.redwood.host.gpui.MeasureInput
import app.cash.redwood.host.gpui.MeasureMode as HostMeasureMode
import app.cash.redwood.host.gpui.SizeF
import app.cash.redwood.ui.Density
import app.cash.redwood.layout.modifier.Flex as FlexModifier
import app.cash.redwood.yoga.MeasureCallback
import app.cash.redwood.yoga.MeasureMode as YogaMeasureMode
import app.cash.redwood.yoga.Node
import app.cash.redwood.yoga.Size
import app.cash.redwood.yoga.RedwoodYogaApi
import app.cash.redwood.yoga.FlexDirection

private const val DEBUG_LAYOUT = false
private const val DEBUG_ZERO_MEASURE = false

public class GpuiNode(
  internal val handle: RedwoodNodeHandle,
  internal val layoutController: GpuiLayoutController,
  internal val layoutNode: Node = Node(),
  internal val shouldApplyLayoutFrame: Boolean = true,
  private val measureSelf: Boolean = true,
  private val onRequestFocus: (() -> Boolean)? = null,
  private val environment: GpuiEnvironment? = null,
) {
  internal val layoutChildren: MutableList<GpuiNode> = mutableListOf()
  internal var parent: GpuiNode? = null
  internal val debugId: String = "node@" + handle.hashCode().toString(16)
  internal var loggedZeroMeasure: Boolean = false
  internal var loggedZeroFrame: Boolean = false
  public var wantsFillWidth: Boolean = false
  public var wantsFillHeight: Boolean = false
  private var loggedFrameZero: Boolean = false
  private var loggedFramePositive: Boolean = false

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

  public fun rawNode(): RedwoodNodeHandle = handle

  private fun applyStyleModifiers(modifier: Modifier, density: Density) {
    val translators = environment?.modifierTranslators ?: return
    if (translators.isEmpty()) return

    val style = MutableGpuiModifierStyle()
    modifier.forEach { element ->
      translators.forEach { translator ->
        translator.translate(element, density, style)
      }
    }

    if (style.hasBackgroundUpdate) {
      handle.setBackgroundColor(style.backgroundColor)
    }
    if (style.hasPaddingUpdate) {
      handle.setPadding(style.padding)
    }
  }

  public fun applyModifier(modifier: Modifier, density: Density) {
    this.modifier = modifier
    val parentDirection = parent?.layoutNode?.flexDirection
    if (parentDirection != null) {
      modifier.forEachScoped { element ->
        if (element is FlexModifier && element.value > 0.0) {
          when (parentDirection) {
            FlexDirection.Column -> wantsFillHeight = true
            FlexDirection.Row -> wantsFillWidth = true
            else -> Unit
          }
        }
      }
    }
    layoutNode.applyModifier(modifier, density)
    applyStyleModifiers(modifier, density)
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
    maybeLogFrame(frame)
  }

  public fun measuredWidth(): Float = layoutNode.width

  public fun measuredHeight(): Float = layoutNode.height

  private object RedwoodMeasureCallback : MeasureCallback {
    override fun measure(
      node: Node,
      width: Float,
      widthMode: YogaMeasureMode,
      height: Float,
      heightMode: YogaMeasureMode,
    ): Size {
      val gpuiNode = node.context as? GpuiNode ?: return zeroSize
      val input = MeasureInput(
        width = width.toMeasureValue(),
        widthMode = widthMode.toHostMode(),
        height = height.toMeasureValue(),
        heightMode = heightMode.toHostMode(),
      )

      val size = runCatching { gpuiNode.handle.measure(input) }.getOrNull()
        ?: SizeF(width = 0f, height = 0f)
      // No debug logging when measurements collapse or recover.
      return Size(size.width, size.height)
    }
  }

  private companion object {
    private val zeroSize = Size(0f, 0f)
    private const val MaxLoggedFrames = 60
    private var loggedFramesCount: Int = 0
  }

  private fun maybeLogFrame(frame: LayoutFrame) {
    if ((loggedFrameZero && loggedFramePositive) || loggedFramesCount >= MaxLoggedFrames) return
    val isPositive = frame.width > 0f && frame.height > 0f
    val shouldLog = when {
      isPositive && !loggedFramePositive -> true
      !isPositive && !loggedFrameZero -> true
      else -> false
    }
    if (shouldLog && loggedFramesCount < MaxLoggedFrames) {
      if (isPositive) {
        loggedFramePositive = true
      } else {
        loggedFrameZero = true
      }
      loggedFramesCount += 1
    }
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
