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

import app.cash.redwood.treehouse.TreehouseApp

/**
 * Describes how hot reload should be orchestrated for a [TreehouseApp].
 *
 * @param enabled control flag so hosts can ship the code paths behind a feature toggle.
 * @param stateTracker optional tracker used to surface lifecycle events for tooling.
 * @param captureRequestFactory invoked prior to each capture to supply trigger metadata.
 */
public data class TreehouseHotReloadConfig(
  val enabled: Boolean = true,
  val stateTracker: TreehouseHotReloadStateTracker? = null,
  val captureRequestFactory: () -> TreehouseHotReloadCaptureRequest =
    { TreehouseHotReloadCaptureRequest(TreehouseHotReloadTrigger.SourceUpdate) },
)
