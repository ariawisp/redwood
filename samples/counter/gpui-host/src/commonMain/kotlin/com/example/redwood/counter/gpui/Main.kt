package com.example.redwood.counter.gpui

import app.cash.redwood.host.gpui.GpuiApp
import app.cash.redwood.host.gpui.GpuiAppDelegate
import app.cash.redwood.host.gpui.GpuiAppOptions
import app.cash.redwood.host.gpui.GpuiRedwoodView
import app.cash.redwood.host.gpui.GpuiTheme
import app.cash.redwood.host.gpui.GpuiWindowConfig
import app.cash.redwood.host.gpui.GpuiWindowEvents
import app.cash.redwood.host.gpui.GpuiWindowSize
import app.cash.redwood.host.gpui.createRedwoodView
import app.cash.redwood.host.gpui.runGpuiApp
import app.cash.redwood.runtime.RedwoodComposition
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.basic.gpui.GpuiRedwoodUiBasicWidgetSystem
import com.example.redwood.counter.presenter.Counter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

fun main() {
  runGpuiApp(
    options = GpuiAppOptions(),
    delegate = GpuiAppDelegate(::launchCounter),
  )
}

private fun launchCounter(app: GpuiApp) {
  val scope = MainScope()

  lateinit var view: GpuiRedwoodView

  view = app.createRedwoodView(
    config = GpuiWindowConfig(
      title = "Counter (GPUI)",
      contentSize = GpuiWindowSize(width = 400f, height = 300f),
      decorated = true,
    ),
    theme = GpuiTheme(),
    delegate = object : GpuiWindowEvents {
      override fun closeRequested(): Boolean {
        scope.cancel()
        view.dispose()
        return true
      }

      override fun didResize(size: GpuiWindowSize) {
        // Handled internally by GpuiRedwoodView.
      }
    },
  )

  val widgetSystem = GpuiRedwoodUiBasicWidgetSystem(view.environment)
  val composition = RedwoodComposition(
    scope = scope,
    view = view,
    widgetSystem = widgetSystem,
  )

  composition.setContent { Counter() }

  view.window.show()
  view.requestLayout()
}
