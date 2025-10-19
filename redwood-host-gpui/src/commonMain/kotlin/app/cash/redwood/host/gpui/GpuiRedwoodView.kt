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

import app.cash.redwood.ui.Cancellable
import app.cash.redwood.ui.Density
import app.cash.redwood.ui.LayoutDirection
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.OnBackPressedCallback
import app.cash.redwood.ui.OnBackPressedDispatcher
import app.cash.redwood.ui.Size
import app.cash.redwood.ui.UiConfiguration
import app.cash.redwood.widget.RedwoodView
import app.cash.redwood.widget.SavedStateRegistry
import app.cash.redwood.widget.Widget
import app.cash.redwood.yoga.FlexDirection
import app.cash.redwood.yoga.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class GpuiRedwoodView internal constructor(
  public val window: GpuiWindow,
  public val environment: GpuiEnvironment,
) : RedwoodView<GpuiNode> {
  private val rootNode = GpuiNode(
    handle = RedwoodNodeHandle.placeholder(),
    layoutController = environment.layoutController,
    layoutNode = Node().apply { flexDirection = FlexDirection.Column },
    shouldApplyLayoutFrame = false,
    measureSelf = false,
    environment = environment,
  )
  private val rootChildren = environment.surface.rootChildren(environment, rootNode)

  private val mutableUiConfiguration = MutableStateFlow(
    UiConfiguration(
      darkMode = false,
      safeAreaInsets = Margin.Zero,
      viewInsets = Margin.Zero,
      viewportSize = null,
      density = environment.density.rawDensity,
      layoutDirection = LayoutDirection.Ltr,
    ),
  )

  init {
    environment.surface.layoutController = environment.layoutController
    environment.layoutController.attachRoot(rootNode)
    environment.theme?.backgroundColor?.let { color ->
      rootNode.handle.setBackgroundColor(color.toGpuiColor())
    }
  }

  override val onBackPressedDispatcher: OnBackPressedDispatcher =
    object : OnBackPressedDispatcher {
      override fun addCallback(onBackPressedCallback: OnBackPressedCallback): Cancellable {
        return object : Cancellable {
          override fun cancel() = Unit
        }
      }
    }

  override val uiConfiguration: StateFlow<UiConfiguration>
    get() = mutableUiConfiguration

  override val savedStateRegistry: SavedStateRegistry?
    get() = null

  override val value: GpuiNode
    get() = rootNode

  override val children: Widget.Children<GpuiNode>
    get() = rootChildren

  override fun requestFocus(widget: Widget<GpuiNode>) {
    if (!widget.value.requestFocus()) {
      // If the widget couldn't handle the focus request, fall back to a surface-wide layout pass.
      environment.surface.requestLayout()
    }
  }

  internal fun updateViewport(size: GpuiWindowSize) {
    val density = environment.density
    val width = density.run { size.width.toDp() }
    val height = density.run { size.height.toDp() }
    mutableUiConfiguration.value = mutableUiConfiguration.value.copy(
      viewportSize = Size(width, height),
      density = density.rawDensity,
    )
    environment.layoutController.updateViewport(size)
  }

  public fun requestLayout() {
    environment.surface.requestLayout()
  }

  public fun dispose() {
    environment.surface.dispose()
  }
}

public fun GpuiApp.createRedwoodView(
  config: GpuiWindowConfig = GpuiWindowConfig(),
  density: Density = Density(1.0),
  theme: GpuiTheme? = null,
  textInputKeyBindings: List<GpuiTextInputKeyBinding>? = null,
  modifierTranslators: List<GpuiModifierTranslator> = emptyList(),
  delegate: GpuiWindowEvents? = null,
): GpuiRedwoodView {
  var pendingViewport: GpuiWindowSize? = null
  var redwoodView: GpuiRedwoodView? = null

  val window = createWindow(
    config = config,
    events = object : GpuiWindowEvents {
      override fun closeRequested(): Boolean {
        return delegate?.closeRequested() ?: true
      }

      override fun didResize(size: GpuiWindowSize) {
        val view = redwoodView
        if (view != null) {
          view.updateViewport(size)
        } else {
          pendingViewport = size
        }
        delegate?.didResize(size)
      }
    },
  )

  val surface = window.createSurface()
  val layoutController = GpuiLayoutController()
  surface.layoutController = layoutController
  val resolvedTheme = theme ?: GpuiTheme()
  val environment = GpuiEnvironment(
    surface = surface,
    density = density,
    layoutController = layoutController,
    theme = resolvedTheme,
    textInputKeyBindings = textInputKeyBindings,
    modifierTranslators = modifierTranslators,
  )
  textInputKeyBindings?.let(::configureTextInputKeyBindings)
  val view = GpuiRedwoodView(window, environment)
  redwoodView = view
  val initialViewport = pendingViewport
    ?: config.contentSize.takeIf { it.width > 0f || it.height > 0f }
  initialViewport?.let { view.updateViewport(it) }
  return view
}
