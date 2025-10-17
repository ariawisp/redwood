package app.cash.redwood.host.gpui

public enum class GpuiTextInputAction {
  Left,
  Right,
  SelectLeft,
  SelectRight,
  SelectAll,
  Home,
  End,
  PageUp,
  PageDown,
  SelectPageUp,
  SelectPageDown,
  Backspace,
  Delete,
  Paste,
  Cut,
  Copy,
  ShowCharacterPalette,
}

public data class GpuiTextInputKeyBinding(
  val keystrokes: String,
  val action: GpuiTextInputAction,
)

public fun configureTextInputKeyBindings(bindings: List<GpuiTextInputKeyBinding>) {
  val ffiBindings = bindings.map {
    TextInputKeyBindingFfi(
      keystrokes = it.keystrokes,
      action = it.action.toFfi(),
    )
  }
  setTextInputKeyBindings(ffiBindings)
}

private fun GpuiTextInputAction.toFfi(): TextInputActionFfi = TextInputActionFfi.valueOf(name)
