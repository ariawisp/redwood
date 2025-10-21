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
package app.cash.redwood.host.gpui

import app.cash.redwood.layout.api.Constraint as LayoutConstraint
import app.cash.redwood.layout.api.CrossAxisAlignment as LayoutCrossAxisAlignment
import app.cash.redwood.layout.api.MainAxisAlignment as LayoutMainAxisAlignment
import app.cash.redwood.layout.api.Overflow as LayoutOverflow
import app.cash.redwood.ui.Density
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.Px
import app.cash.redwood.ui.basic.api.TextFieldState

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

public fun Px.Companion.fromHost(offsetPx: Float): Px = Px(offsetPx.coerceAtLeast(0f).toDouble())

public fun GpuiTextInputTheme.toFfi(): TextInputThemeFfi = TextInputThemeFfi(
  textColor = textColor.toUInt(),
  placeholderColor = placeholderColor.toUInt(),
  backgroundColor = backgroundColor.toUInt(),
  borderColor = borderColor.toUInt(),
  selectionColor = selectionColor.toUInt(),
  caretColor = caretColor.toUInt(),
  paddingHorizontal = paddingHorizontal,
  paddingVertical = paddingVertical,
  disabledTextColor = disabledTextColor?.toUInt(),
  disabledBackgroundColor = disabledBackgroundColor?.toUInt(),
  disabledBorderColor = disabledBorderColor?.toUInt(),
  readOnlyTextColor = readOnlyTextColor?.toUInt(),
  readOnlyBackgroundColor = readOnlyBackgroundColor?.toUInt(),
  readOnlyBorderColor = readOnlyBorderColor?.toUInt(),
)
