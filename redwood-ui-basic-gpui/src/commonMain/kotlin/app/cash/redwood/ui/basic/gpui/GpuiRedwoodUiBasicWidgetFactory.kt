package app.cash.redwood.ui.basic.gpui

import app.cash.redwood.Modifier
import app.cash.redwood.host.gpui.GpuiEnvironment
import app.cash.redwood.host.gpui.GpuiNode
import app.cash.redwood.host.gpui.TextChangeHandler
import app.cash.redwood.host.gpui.TextFieldStateFfi
import app.cash.redwood.host.gpui.ButtonClickHandler
import app.cash.redwood.host.gpui.ImageClickHandler
import app.cash.redwood.host.gpui.toFfi
import app.cash.redwood.host.gpui.toRedwood
import app.cash.redwood.ui.basic.api.TextFieldState
import app.cash.redwood.ui.basic.modifier.Reuse
import app.cash.redwood.ui.basic.widget.Button
import app.cash.redwood.ui.basic.widget.Image
import app.cash.redwood.ui.basic.widget.RedwoodUiBasicWidgetFactory
import app.cash.redwood.ui.basic.widget.Text
import app.cash.redwood.ui.basic.widget.TextInput
import app.cash.redwood.widget.Widget

public class GpuiRedwoodUiBasicWidgetFactory(
  private val environment: GpuiEnvironment,
) : RedwoodUiBasicWidgetFactory<GpuiNode> {
  override fun Button(): Button<GpuiNode> = GpuiButton(environment)

  override fun Image(): Image<GpuiNode> = GpuiImage(environment)

  override fun Text(): Text<GpuiNode> = GpuiText(environment)

  override fun TextInput(): TextInput<GpuiNode> = GpuiTextInput(environment)

  override fun Reuse(value: GpuiNode, modifier: Reuse) {
    // Reuse semantics are not yet implemented for GPUI hosts.
  }
}

private class GpuiText(
  private val environment: GpuiEnvironment,
) : Text<GpuiNode> {
  private val node = environment.surface.createText()

  override val value: GpuiNode = GpuiNode(
    handle = node.rawNode(),
    layoutController = environment.layoutController,
  )

  override val allChildren: List<Widget.Children<GpuiNode>> = emptyList()

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.applyModifier(value, environment.density)
    }

  override fun text(text: String) {
    node.setText(text)
  }
}

private class GpuiButton(
  private val environment: GpuiEnvironment,
) : Button<GpuiNode> {
  private val node = environment.surface.createButton()

  private var clickHandler: ButtonClickHandler? = null

  override val value: GpuiNode = GpuiNode(
    handle = node.rawNode(),
    layoutController = environment.layoutController,
  )

  override val allChildren: List<Widget.Children<GpuiNode>> = emptyList()

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.applyModifier(value, environment.density)
    }

  override fun text(text: String?) {
    node.setText(text)
  }

  override fun enabled(enabled: Boolean) {
    node.setEnabled(enabled)
  }

  override fun onClick(onClick: (() -> Unit)?) {
    val handler = onClick?.let { callback ->
      object : ButtonClickHandler {
        override fun onClick() {
          println("[GpuiRedwoodUiBasic] button handler invoked")
          callback()
        }
      }
    }
    println("[GpuiRedwoodUiBasic] button handler registered: ${handler != null}")
    clickHandler = handler
    node.setOnClick(handler)
  }
}

private class GpuiImage(
  private val environment: GpuiEnvironment,
) : Image<GpuiNode> {
  private val node = environment.surface.createImage()

  private var clickHandler: ImageClickHandler? = null

  override val value: GpuiNode = GpuiNode(
    handle = node.rawNode(),
    layoutController = environment.layoutController,
  )

  override val allChildren: List<Widget.Children<GpuiNode>> = emptyList()

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.applyModifier(value, environment.density)
    }

  override fun url(url: String) {
    node.setUrl(url)
  }

  override fun onClick(onClick: (() -> Unit)?) {
    val handler = onClick?.let { callback ->
      object : ImageClickHandler {
        override fun onClick() {
          callback()
        }
      }
    }
    clickHandler = handler
    node.setOnClick(handler)
  }
}

private class GpuiTextInput(
  private val environment: GpuiEnvironment,
) : TextInput<GpuiNode> {
  private val node = environment.surface.createTextInput()

  private var changeHandler: TextChangeHandler? = null

  override val value: GpuiNode = GpuiNode(
    handle = node.rawNode(),
    layoutController = environment.layoutController,
    onRequestFocus = {
      runCatching { node.requestFocus() }.isSuccess
    },
  )

  override val allChildren: List<Widget.Children<GpuiNode>> = emptyList()

  override var modifier: Modifier = Modifier
    set(value) {
      field = value
      this.value.applyModifier(value, environment.density)
    }

  override fun state(state: TextFieldState) {
    node.setState(state.toFfi())
  }

  override fun hint(hint: String) {
    node.setHint(hint.ifEmpty { null })
  }

  override fun onChange(onChange: ((TextFieldState) -> Unit)?) {
    val handler = onChange?.let { callback ->
      object : TextChangeHandler {
        override fun onChange(newState: TextFieldStateFfi) {
          println("[GpuiRedwoodUiBasic] text input handler invoked")
          callback(newState.toRedwood())
        }
      }
    }
    println("[GpuiRedwoodUiBasic] text input handler registered: ${handler != null}")
    changeHandler = handler
    node.setOnChange(handler)
  }
}
