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
package app.cash.redwood.samples.gpui

import app.cash.redwood.host.gpui.GpuiApp
import app.cash.redwood.host.gpui.GpuiAppDelegate
import app.cash.redwood.host.gpui.GpuiAppOptions
import app.cash.redwood.host.gpui.GpuiRedwoodView
import app.cash.redwood.host.gpui.GpuiWindowConfig
import app.cash.redwood.host.gpui.GpuiWindowSize
import app.cash.redwood.host.gpui.createRedwoodView
import app.cash.redwood.host.gpui.runGpuiApp
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.gpui.GpuiRedwoodLayoutWidgetFactory
import app.cash.redwood.ui.Density
import app.cash.redwood.ui.Margin
import app.cash.redwood.ui.basic.api.TextFieldState
import app.cash.redwood.ui.basic.gpui.GpuiRedwoodUiBasicWidgetFactory
import app.cash.redwood.ui.dp

fun main() {
  runGpuiApp(
    options = GpuiAppOptions(),
    delegate = GpuiAppDelegate { app ->
      launchSample(app)
    },
  )
}

private fun launchSample(app: GpuiApp) {
  val view: GpuiRedwoodView = app.createRedwoodView(
    config = GpuiWindowConfig(
      title = "Redwood + GPUI",
      contentSize = GpuiWindowSize(width = 800f, height = 600f),
      decorated = true,
    ),
    density = Density(1.0),
  )

  val environment = view.environment
  val layout = GpuiRedwoodLayoutWidgetFactory(environment)
  val ui = GpuiRedwoodUiBasicWidgetFactory(environment)

  val column = layout.Column().apply {
    width(Constraint.Fill)
    height(Constraint.Fill)
    margin(Margin.Zero)
  }

  val greeting = ui.Text().apply {
    text("Hello from Redwood on GPUI!")
  }

  val nameState = TextFieldState(text = "Redwood")
  val input = ui.TextInput().apply {
    state(nameState)
    hint("Enter your name")
  }

  val spacer = layout.Spacer().apply {
    height(16.dp)
  }

  val button = ui.Button().apply {
    text("Quit")
    onClick { app.quit() }
  }

  input.onChange { newState ->
    input.state(newState)
    greeting.text("Hello, ${newState.text.ifEmpty { "friend" }}!")
  }

  column.children.apply {
    insert(0, greeting)
    insert(1, spacer)
    insert(2, input)
    insert(3, layout.Spacer().apply { height(24.dp) })
    insert(4, button)
  }

  view.children.insert(0, column)
  view.window.show()
  view.requestLayout()
}
