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

import app.cash.redwood.treehouse.ZiplineTreehouseUi

/**
 * Default implementation that bridges [ZiplineTreehouseUi] to the hot reload protocol.
 *
 * The guest captures saveable Compose state via [ZiplineTreehouseUi.snapshotState] and expects the
 * host to provide that same snapshot back when the refreshed module starts up. Clients can provide
 * [onRestore] to run any module-specific reinitialization once the snapshot was restored.
 */
public class DefaultTreehouseHotReloadClient(
  private val treehouseUi: ZiplineTreehouseUi,
  private val onRestore: (TreehouseHotReloadFrame) -> TreehouseHotReloadResult = { frame ->
    TreehouseHotReloadResult(restoredSnapshotId = frame.snapshotId)
  },
) : TreehouseHotReloadClient {

  override suspend fun captureFrame(
    request: TreehouseHotReloadCaptureRequest,
  ): TreehouseHotReloadFrame {
    val snapshot = treehouseUi.snapshotState()
    return TreehouseHotReloadFrame(
      snapshotId = null,
      snapshot = snapshot,
      metadata = request.metadata,
    )
  }

  override suspend fun restoreFrame(frame: TreehouseHotReloadFrame): TreehouseHotReloadResult {
    return onRestore(frame)
  }
}

public fun defaultTreehouseHotReloadClient(
  treehouseUi: ZiplineTreehouseUi,
  onRestore: (TreehouseHotReloadFrame) -> TreehouseHotReloadResult = { frame ->
    TreehouseHotReloadResult(restoredSnapshotId = frame.snapshotId)
  },
): TreehouseHotReloadClient = DefaultTreehouseHotReloadClient(treehouseUi, onRestore)
