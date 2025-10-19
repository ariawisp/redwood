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

import app.cash.redwood.Modifier
import app.cash.redwood.ui.Density

/**
 * Allows callers to translate Redwood [Modifier] elements into GPUI-specific styling hints
 * (for example background colors or padding) that are applied by [GpuiNode].
 *
 * Implementations can be supplied via [GpuiEnvironment.modifierTranslators] to mirror
 * schema-specific modifier semantics without hard-linking this module to those schemas.
 */
public fun interface GpuiModifierTranslator {
  /**
   * Inspect [element] and update [style] with any GPUI styling derived from it.
   *
   * This method may be invoked multiple times for a single modifier chain; translators
   * should overwrite existing data in [style] when they want to take precedence.
   */
  public fun translate(element: Modifier.Element, density: Density, style: MutableGpuiModifierStyle)
}

/**
 * A mutable accumulator that builds up GPUI styling derived from Redwood modifiers.
 *
 * Translators operate on this object, and the final values are consumed by [GpuiNode].
 */
public class MutableGpuiModifierStyle {
  private var backgroundSet: Boolean = false
  private var paddingSet: Boolean = false
  private var _backgroundColor: UInt? = null
  private var _padding: EdgeInsets? = null

  /** Background color for the node expressed as 0xAARRGGBB. */
  public val backgroundColor: UInt?
    get() = _backgroundColor

  /** Optional padding applied to the node before yoga layout in logical pixels. */
  public val padding: EdgeInsets?
    get() = _padding

  /**
   * Set or clear the background color that should be applied. Passing `null` removes the background.
   */
  public fun setBackgroundColor(color: UInt?) {
    backgroundSet = true
    _backgroundColor = color
  }

  public fun setBackgroundColor(color: Long?) {
    setBackgroundColor(color?.toGpuiColor())
  }

  /**
   * Set padding in logical pixels. Passing `null` removes any previously applied padding.
   */
  public fun setPadding(padding: EdgeInsets?) {
    paddingSet = true
    _padding = padding
  }

  internal val hasBackgroundUpdate: Boolean
    get() = backgroundSet

  internal val hasPaddingUpdate: Boolean
    get() = paddingSet
}
