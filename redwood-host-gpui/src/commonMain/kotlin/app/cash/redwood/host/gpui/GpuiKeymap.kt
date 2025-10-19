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
