package app.cash.redwood.host.gpui

import app.cash.redwood.layout.api.Constraint as LayoutConstraint
import app.cash.redwood.layout.api.CrossAxisAlignment as LayoutCrossAxisAlignment
import app.cash.redwood.layout.api.MainAxisAlignment as LayoutMainAxisAlignment
import app.cash.redwood.layout.api.Margin
import app.cash.redwood.layout.api.Overflow as LayoutOverflow
import app.cash.redwood.ui.Px
import app.cash.redwood.ui.basic.api.TextFieldState
import app.cash.redwood.ui.Density

public fun LayoutConstraint.toGpui(): ConstraintKind = when (this) {
  LayoutConstraint.Wrap -> ConstraintKind.WRAP
  LayoutConstraint.Fill -> ConstraintKind.FILL
  else -> throw AssertionError("Unknown constraint: $this")
}

public fun LayoutMainAxisAlignment.toGpui(): MainAxisAlignment = when (this) {
  LayoutMainAxisAlignment.Start -> MainAxisAlignment.START
  LayoutMainAxisAlignment.Center -> MainAxisAlignment.CENTER
  LayoutMainAxisAlignment.End -> MainAxisAlignment.END
  LayoutMainAxisAlignment.SpaceBetween -> MainAxisAlignment.SPACE_BETWEEN
  LayoutMainAxisAlignment.SpaceAround -> MainAxisAlignment.SPACE_AROUND
  LayoutMainAxisAlignment.SpaceEvenly -> MainAxisAlignment.SPACE_EVENLY
  else -> throw AssertionError("Unknown main axis alignment: $this")
}

public fun LayoutCrossAxisAlignment.toGpui(): CrossAxisAlignment = when (this) {
  LayoutCrossAxisAlignment.Start -> CrossAxisAlignment.START
  LayoutCrossAxisAlignment.Center -> CrossAxisAlignment.CENTER
  LayoutCrossAxisAlignment.End -> CrossAxisAlignment.END
  LayoutCrossAxisAlignment.Stretch -> CrossAxisAlignment.STRETCH
  else -> throw AssertionError("Unknown cross axis alignment: $this")
}

public fun LayoutOverflow.toGpui(): OverflowKind = when (this) {
  LayoutOverflow.Clip -> OverflowKind.CLIP
  LayoutOverflow.Scroll -> OverflowKind.SCROLL
  else -> throw AssertionError("Unknown overflow: $this")
}

public fun Margin.toEdgeInsets(density: Density): EdgeInsets = EdgeInsets(
  start = density.run { start.toPx().toFloat() },
  end = density.run { end.toPx().toFloat() },
  top = density.run { top.toPx().toFloat() },
  bottom = density.run { bottom.toPx().toFloat() },
)

public fun TextFieldState.toFfi(): TextFieldStateFfi = TextFieldStateFfi(
  text = text,
  selectionStart = selectionStart.toUInt(),
  selectionEnd = selectionEnd.toUInt(),
  userEditCount = userEditCount.toULong(),
)

public fun TextFieldStateFfi.toRedwood(): TextFieldState = TextFieldState(
  text = text,
  selectionStart = selectionStart.toInt(),
  selectionEnd = selectionEnd.toInt(),
  userEditCount = userEditCount.toLong(),
)

public fun Px.Companion.fromHost(offsetPx: Float): Px = Px(offsetPx.toDouble())
