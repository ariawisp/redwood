package com.example.redwood.emojisearch.gpui

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
import app.cash.redwood.compose.RedwoodComposition
import androidx.compose.runtime.BroadcastFrameClock
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.basic.gpui.GpuiRedwoodUiBasicWidgetSystem
import com.example.redwood.emojisearch.presenter.EmojiSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import kotlin.time.TimeSource

fun main() {
  runGpuiApp(
    options = GpuiAppOptions(),
    delegate = GpuiAppDelegate(::launchEmojiSearch),
  )
}

private fun launchEmojiSearch(app: GpuiApp) {
  val frameClock = BroadcastFrameClock {}
  val baseScope = MainScope()
  val scope: CoroutineScope = CoroutineScope(baseScope.coroutineContext + frameClock)
  val httpClient = JvmHttpClient(OkHttpClient())
  val navigator = DesktopNavigator

  lateinit var view: GpuiRedwoodView

  view = app.createRedwoodView(
    config = GpuiWindowConfig(
      title = "Emoji Search (GPUI)",
      contentSize = GpuiWindowSize(width = 640f, height = 480f),
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

  composition.setContent {
    EmojiSearch(
      httpClient = httpClient,
      navigator = navigator,
      viewInsets = Margin.Zero,
    )
  }

  view.window.show()
  view.requestLayout()

  // Simple frame ticker to drive Compose recomposition.
  val start = TimeSource.Monotonic.markNow()
  scope.launch {
    while (isActive) {
      frameClock.sendFrame(start.elapsedNow().inWholeNanoseconds)
      delay(16)
    }
  }
}
