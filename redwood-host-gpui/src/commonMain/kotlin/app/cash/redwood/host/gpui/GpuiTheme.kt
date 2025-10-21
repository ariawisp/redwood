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

public data class GpuiTheme(
  val textColor: Long = 0xFF252525,
  val backgroundColor: Long = 0xFFFFFFFF,
  val buttonTextColor: Long? = null,
  val textInput: GpuiTextInputTheme? = GpuiTextInputTheme(),
)

public fun Long.toGpuiColor(): UInt = this.toULong().toUInt()

public data class GpuiTextInputTheme(
  val textColor: Long = 0x252525FF,
  val placeholderColor: Long = 0xB0B0B099,
  val backgroundColor: Long = 0xF4F5F5FF,
  val borderColor: Long = 0xD9D9D9FF,
  val selectionColor: Long = 0x2A63D9FF,
  val caretColor: Long = 0x2A63D9FF,
  val paddingHorizontal: Float = 12f,
  val paddingVertical: Float = 8f,
  val disabledTextColor: Long? = null,
  val disabledBackgroundColor: Long? = null,
  val disabledBorderColor: Long? = null,
  val readOnlyTextColor: Long? = null,
  val readOnlyBackgroundColor: Long? = null,
  val readOnlyBorderColor: Long? = null,
)
