@file:OptIn(RedwoodYogaApi::class)

package app.cash.redwood.host.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.layout.api.MainAxisAlignment
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.layout.modifier.Flex as FlexModifier
import app.cash.redwood.layout.modifier.Grow as GrowModifier
import app.cash.redwood.layout.modifier.Height as HeightModifier
import app.cash.redwood.layout.modifier.HorizontalAlignment as HorizontalAlignmentModifier
import app.cash.redwood.layout.modifier.Margin as MarginModifier
import app.cash.redwood.layout.modifier.Shrink as ShrinkModifier
import app.cash.redwood.layout.modifier.Size as SizeModifier
import app.cash.redwood.layout.modifier.VerticalAlignment as VerticalAlignmentModifier
import app.cash.redwood.layout.modifier.Width as WidthModifier
import app.cash.redwood.ui.Density
import app.cash.redwood.yoga.AlignItems
import app.cash.redwood.yoga.AlignSelf
import app.cash.redwood.yoga.JustifyContent
import app.cash.redwood.yoga.Node
import app.cash.redwood.yoga.RedwoodYogaApi

internal fun MainAxisAlignment.toJustifyContent() = when (this) {
  MainAxisAlignment.Start -> JustifyContent.FlexStart
  MainAxisAlignment.Center -> JustifyContent.Center
  MainAxisAlignment.End -> JustifyContent.FlexEnd
  MainAxisAlignment.SpaceBetween -> JustifyContent.SpaceBetween
  MainAxisAlignment.SpaceAround -> JustifyContent.SpaceAround
  MainAxisAlignment.SpaceEvenly -> JustifyContent.SpaceEvenly
  else -> throw AssertionError("Unsupported main axis alignment: $this")
}

internal fun CrossAxisAlignment.toAlignItems() = when (this) {
  CrossAxisAlignment.Start -> AlignItems.FlexStart
  CrossAxisAlignment.Center -> AlignItems.Center
  CrossAxisAlignment.End -> AlignItems.FlexEnd
  CrossAxisAlignment.Stretch -> AlignItems.Stretch
  else -> throw AssertionError("Unsupported cross axis alignment: $this")
}

internal fun CrossAxisAlignment.toAlignSelf() = when (this) {
  CrossAxisAlignment.Start -> AlignSelf.FlexStart
  CrossAxisAlignment.Center -> AlignSelf.Center
  CrossAxisAlignment.End -> AlignSelf.FlexEnd
  CrossAxisAlignment.Stretch -> AlignSelf.Stretch
  else -> throw AssertionError("Unsupported cross axis alignment: $this")
}

internal fun CrossAxisAlignment.toBoxJustifyContent() = when (this) {
  CrossAxisAlignment.Start -> JustifyContent.FlexStart
  CrossAxisAlignment.Center -> JustifyContent.Center
  CrossAxisAlignment.End -> JustifyContent.FlexEnd
  CrossAxisAlignment.Stretch -> JustifyContent.FlexStart
  else -> throw AssertionError("Unsupported cross axis alignment: $this")
}

/**
 * Updates this Yoga [Node] to match the configuration specified by [parentModifier].
 *
 * This logic mirrors the helpers used by other Redwood hosts that implement Yoga-based layout.
 *
 * @return `true` if the node became dirty as a consequence of this call.
 */
internal fun Node.applyModifier(parentModifier: Modifier, density: Density): Boolean {
  val wasDirty = isDirty()

  val oldMarginStart = marginStart
  var newMarginStart = Float.NaN
  val oldMarginEnd = marginEnd
  var newMarginEnd = Float.NaN
  val oldMarginTop = marginTop
  var newMarginTop = Float.NaN
  val oldMarginBottom = marginBottom
  var newMarginBottom = Float.NaN
  val oldAlignSelf = alignSelf
  var newAlignSelf = AlignSelf.Auto
  val oldRequestedMinWidth = requestedMinWidth
  var newRequestedMinWidth = Float.NaN
  val oldRequestedMaxWidth = requestedMaxWidth
  var newRequestedMaxWidth = Float.NaN
  val oldRequestedMinHeight = requestedMinHeight
  var newRequestedMinHeight = Float.NaN
  val oldRequestedMaxHeight = requestedMaxHeight
  var newRequestedMaxHeight = Float.NaN
  val oldFlexGrow = flexGrow
  var newFlexGrow = 0f
  val oldFlexShrink = flexShrink
  var newFlexShrink = 0f
  val oldFlexBasis = flexBasis
  var newFlexBasis = -1f

  parentModifier.forEachScoped { childModifier ->
    when (childModifier) {
      is GrowModifier -> {
        newFlexGrow = childModifier.value.toFloat()
      }
      is ShrinkModifier -> {
        newFlexShrink = childModifier.value.toFloat()
      }
      is MarginModifier -> with(density) {
        newMarginStart = childModifier.margin.start.toPx().toFloat()
        newMarginEnd = childModifier.margin.end.toPx().toFloat()
        newMarginTop = childModifier.margin.top.toPx().toFloat()
        newMarginBottom = childModifier.margin.bottom.toPx().toFloat()
      }
      is HorizontalAlignmentModifier -> {
        newAlignSelf = childModifier.alignment.toAlignSelf()
      }
      is VerticalAlignmentModifier -> {
        newAlignSelf = childModifier.alignment.toAlignSelf()
      }
      is WidthModifier -> with(density) {
        val width = childModifier.width.toPx().toFloat()
        newRequestedMinWidth = width
        newRequestedMaxWidth = width
      }
      is HeightModifier -> with(density) {
        val height = childModifier.height.toPx().toFloat()
        newRequestedMinHeight = height
        newRequestedMaxHeight = height
      }
      is SizeModifier -> with(density) {
        val width = childModifier.width.toPx().toFloat()
        val height = childModifier.height.toPx().toFloat()
        newRequestedMinWidth = width
        newRequestedMaxWidth = width
        newRequestedMinHeight = height
        newRequestedMaxHeight = height
      }
      is FlexModifier -> {
        val flex = childModifier.value.coerceAtLeast(0.0).toFloat()
        newFlexGrow = flex
        newFlexShrink = 1.0f
        newFlexBasis = if (flex > 0) 0.0f else -1.0f
      }
    }
  }

  if (!newMarginStart.isNaN() && oldMarginStart != newMarginStart) {
    marginStart = newMarginStart
  }
  if (!newMarginEnd.isNaN() && oldMarginEnd != newMarginEnd) {
    marginEnd = newMarginEnd
  }
  if (!newMarginTop.isNaN() && oldMarginTop != newMarginTop) {
    marginTop = newMarginTop
  }
  if (!newMarginBottom.isNaN() && oldMarginBottom != newMarginBottom) {
    marginBottom = newMarginBottom
  }
  if (oldAlignSelf != newAlignSelf) {
    alignSelf = newAlignSelf
  }
  if (!newRequestedMinWidth.isNaN() && oldRequestedMinWidth != newRequestedMinWidth) {
    requestedMinWidth = newRequestedMinWidth
  }
  if (!newRequestedMaxWidth.isNaN() && oldRequestedMaxWidth != newRequestedMaxWidth) {
    requestedMaxWidth = newRequestedMaxWidth
  }
  if (!newRequestedMinHeight.isNaN() && oldRequestedMinHeight != newRequestedMinHeight) {
    requestedMinHeight = newRequestedMinHeight
  }
  if (!newRequestedMaxHeight.isNaN() && oldRequestedMaxHeight != newRequestedMaxHeight) {
    requestedMaxHeight = newRequestedMaxHeight
  }
  if (oldFlexGrow != newFlexGrow) {
    flexGrow = newFlexGrow
  }
  if (oldFlexShrink != newFlexShrink) {
    flexShrink = newFlexShrink
  }
  if (oldFlexBasis != newFlexBasis && newFlexBasis >= 0f) {
    flexBasis = newFlexBasis
  }

  return !wasDirty && isDirty()
}
