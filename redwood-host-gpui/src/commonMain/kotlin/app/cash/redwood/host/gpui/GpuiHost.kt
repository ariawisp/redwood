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

/**
 * High-level helpers around the generated UniFFI bindings. These wrappers smooth over the raw
 * handles and data classes so higher level modules can work with Kotlin-friendly types.
 */

public data class GpuiAppOptions(
  val headless: Boolean = false,
) {
  internal fun toRust(): AppOptions = AppOptions(headless = headless)
}

public data class GpuiWindowSize(
  val width: Float,
  val height: Float,
) {
  internal fun toRust(): Size2D = Size2D(width = width, height = height)

  public companion object {
    public val Zero: GpuiWindowSize = GpuiWindowSize(width = 0f, height = 0f)
  }
}

public data class GpuiWindowConfig(
  val title: String = "",
  val contentSize: GpuiWindowSize = GpuiWindowSize.Zero,
  val minSize: GpuiWindowSize? = null,
  val maxSize: GpuiWindowSize? = null,
  val decorated: Boolean = true,
) {
  internal fun toRust(): WindowConfig {
    return WindowConfig(
      title = title,
      contentSize = contentSize.toRust(),
      minSize = minSize?.toRust(),
      maxSize = maxSize?.toRust(),
      decorated = decorated,
    )
  }
}

public interface GpuiWindowEvents {
  /**
   * Called on the GPUI main thread when the window is about to close. Return `true` to allow the
   * close request, or `false` to cancel it.
   */
  public fun closeRequested(): Boolean

  /**
   * Called whenever the GPUI window is resized. Measured in logical pixels.
   */
  public fun didResize(size: GpuiWindowSize)
}

public fun interface GpuiAppDelegate {
  /**
   * Invoked on the GPUI main thread once the application is ready for interaction.
   */
  public fun onAppReady(app: GpuiApp)
}

public class GpuiApp internal constructor(
  internal val handle: GpuiAppHandle,
) {
  public fun createWindow(
    config: GpuiWindowConfig = GpuiWindowConfig(),
    events: GpuiWindowEvents? = null,
  ): GpuiWindow {
    val windowHandle = handle.createWindow(config.toRust(), events?.let(::EventsAdapter))
    return GpuiWindow(windowHandle)
  }

  public fun post(task: () -> Unit) {
    handle.post(UiTaskAdapter(task))
  }

  public fun quit() {
    handle.quit()
  }
}

public class GpuiWindow internal constructor(
  internal val handle: GpuiWindowHandle,
) {
  public fun show() {
    handle.show()
  }

  public fun hide() {
    handle.hide()
  }

  public fun setTitle(title: String) {
    handle.setTitle(title)
  }

  public fun resize(size: GpuiWindowSize) {
    handle.resize(size.toRust())
  }

  public fun requestFrame(scale: Float = 1f) {
    handle.frame(scale)
  }

  public fun createSurface(): GpuiSurface {
    val surfaceHandle = handle.createSurface()
    return GpuiSurface(surfaceHandle)
  }
}

public class GpuiSurface internal constructor(
  internal val handle: RedwoodSurfaceHandle,
) {
  internal var layoutController: GpuiLayoutController? = null

  public fun rootChildren(environment: GpuiEnvironment, parentNode: GpuiNode): GpuiChildren {
    return GpuiChildren(environment, handle.rootChildren(), parentNode)
  }

  public fun requestLayout() {
    val controller = layoutController
    if (controller != null) {
      controller.requestLayout()
    } else {
      handle.requestLayout()
    }
  }

  public fun dispose() {
    handle.dispose()
  }

  public fun createRow(): RedwoodFlexNode = handle.createRow()
  public fun createColumn(): RedwoodFlexNode = handle.createColumn()
  public fun createBox(): RedwoodBoxNode = handle.createBox()
  public fun createSpacer(): RedwoodSpacerNode = handle.createSpacer()
  public fun createText(): RedwoodTextNode = handle.createText()
  public fun createButton(): RedwoodButtonNode = handle.createButton()
  public fun createImage(): RedwoodImageNode = handle.createImage()
  public fun createTextInput(): RedwoodTextInputNode = handle.createTextInput()
  public fun createUniformList(): RedwoodUniformListNode = handle.createUniformList()
  public fun createList(): RedwoodListNode = handle.createList()
}

public fun runGpuiApp(
  options: GpuiAppOptions = GpuiAppOptions(),
  delegate: GpuiAppDelegate,
) {
  runApp(
    options.toRust(),
    object : AppDelegate {
      override fun onAppReady(app: GpuiAppHandle) {
        delegate.onAppReady(GpuiApp(app))
      }
    },
  )
}

private class UiTaskAdapter(
  private val task: () -> Unit,
) : UiTask {
  override fun run() {
    task()
  }
}

private class EventsAdapter(
  private val delegate: GpuiWindowEvents,
) : WindowEvents {
  override fun closeRequested(): Boolean = delegate.closeRequested()

  override fun didResize(size: Size2D) {
    delegate.didResize(GpuiWindowSize(width = size.width, height = size.height))
  }
}
