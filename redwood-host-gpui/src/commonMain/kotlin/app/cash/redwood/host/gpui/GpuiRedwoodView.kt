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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class GpuiRedwoodView internal constructor(
  public val window: GpuiWindow,
  public val environment: GpuiEnvironment,
) : RedwoodView<GpuiNode> {
  private val rootNode = GpuiNode(RedwoodNodeHandle.placeholder())
  private val rootChildren = environment.surface.rootChildren()

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
  val view = GpuiRedwoodView(window, GpuiEnvironment(surface, density))
  redwoodView = view
  pendingViewport?.let { view.updateViewport(it) }
  return view
}
