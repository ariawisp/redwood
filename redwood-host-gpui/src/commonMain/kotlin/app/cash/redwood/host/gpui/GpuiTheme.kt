package app.cash.redwood.host.gpui

public data class GpuiTheme(
  val textInput: GpuiTextInputTheme? = null,
)

public data class GpuiTextInputTheme(
  val textColor: Long = 0x252525FF,
  val placeholderColor: Long = 0xB0B0B099,
  val backgroundColor: Long = 0xF4F5F5FF,
  val borderColor: Long = 0xD9D9D9FF,
  val selectionColor: Long = 0x2A63D9FF,
  val caretColor: Long = 0x2A63D9FF,
  val paddingHorizontal: Float = 12f,
  val paddingVertical: Float = 8f,
)
