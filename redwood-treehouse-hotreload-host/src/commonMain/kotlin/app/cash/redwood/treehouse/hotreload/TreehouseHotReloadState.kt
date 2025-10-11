/*
 * Copyright (C) 2025 Zed Industries.
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
package app.cash.redwood.treehouse.hotreload

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public enum class TreehouseHotReloadPhase {
  Idle,
  Capturing,
  Restoring,
  Failed,
}

public data class TreehouseHotReloadStatus(
  val phase: TreehouseHotReloadPhase,
  val details: String? = null,
)

public class TreehouseHotReloadStateTracker {
  private val mutableStatus = MutableStateFlow(TreehouseHotReloadStatus(TreehouseHotReloadPhase.Idle))

  public val status: StateFlow<TreehouseHotReloadStatus> = mutableStatus

  public fun update(status: TreehouseHotReloadStatus) {
    mutableStatus.value = status
  }

  public fun capturing(details: String? = null) {
    update(TreehouseHotReloadStatus(TreehouseHotReloadPhase.Capturing, details))
  }

  public fun restoring(details: String? = null) {
    update(TreehouseHotReloadStatus(TreehouseHotReloadPhase.Restoring, details))
  }

  public fun failed(details: String? = null) {
    update(TreehouseHotReloadStatus(TreehouseHotReloadPhase.Failed, details))
  }

  public fun idle(details: String? = null) {
    update(TreehouseHotReloadStatus(TreehouseHotReloadPhase.Idle, details))
  }
}
