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

import app.cash.redwood.treehouse.StateSnapshot
import app.cash.redwood.treehouse.StateSnapshot.Id as SnapshotId
import app.cash.zipline.ZiplineService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the reason the host is requesting that the guest capture a checkpoint of its current
 * state. This is surfaced to guest applications so they can opt to skip expensive work for
 * periodic background captures versus an explicit user-triggered reload.
 */
@Serializable
@SerialName("TreehouseHotReloadTrigger")
public enum class TreehouseHotReloadTrigger {
  /** Triggered by a manual developer request (for example a keyboard shortcut). */
  Manual,

  /** Triggered because source code changed on disk and a new module is available. */
  SourceUpdate,

  /**
   * Triggered automatically while the host is attempting to recover from an earlier error. The
   * guest can leverage this to capture additional diagnostic information.
   */
  ErrorRecovery,
}

/**
 * Request issued from the host before it replaces the currently running module. Guests can use this
 * hook to capture extra metadata to help the host restore context on the new module.
 */
@Serializable
public data class TreehouseHotReloadCaptureRequest(
  val trigger: TreehouseHotReloadTrigger = TreehouseHotReloadTrigger.SourceUpdate,
  val metadata: Map<String, String> = emptyMap(),
)

/**
 * Envelope returned by the guest describing the snapshot that should be replayed after the host
 * installs the updated module.
 *
 * The [snapshot] payload is optional; guests that do not currently expose any saveable state may
 * return `null`. The host will treat that as a full restart of the guest module.
 */
@Serializable
public data class TreehouseHotReloadFrame(
  val snapshotId: SnapshotId? = null,
  val snapshot: StateSnapshot? = null,
  val metadata: Map<String, String> = emptyMap(),
)

/**
 * Result produced once the guest has applied a previously captured [TreehouseHotReloadFrame].
 * The guest may surface warnings to make it easier to diagnose mismatch between code versions.
 */
@Serializable
public data class TreehouseHotReloadResult(
  val restoredSnapshotId: SnapshotId? = null,
  val warnings: List<String> = emptyList(),
)

/**
 * Contract implemented on the guest that allows the host runtime to capture and later restore
 * saveable state around a hot reload boundary.
 */
public interface TreehouseHotReloadClient {
  /**
   * Captures the current state of the running guest. Implementations should be careful to only
   * include data that can safely be deserialized by a future version of the guest code.
   */
  public suspend fun captureFrame(
    request: TreehouseHotReloadCaptureRequest = TreehouseHotReloadCaptureRequest(),
  ): TreehouseHotReloadFrame

  /**
   * Applies the provided [frame] to the current guest instance. Implementations may opt to only
   * restore a subset of the captured state and should communicate any mismatches by returning a
   * [TreehouseHotReloadResult] with warnings.
   */
  public suspend fun restoreFrame(frame: TreehouseHotReloadFrame): TreehouseHotReloadResult
}

/**
 * Zipline bridge that forwards calls from the host runtime into a [TreehouseHotReloadClient].
 */
public interface TreehouseHotReloadBridge : ZiplineService {
  public suspend fun captureFrame(
    request: TreehouseHotReloadCaptureRequest = TreehouseHotReloadCaptureRequest(),
  ): TreehouseHotReloadFrame

  public suspend fun restoreFrame(frame: TreehouseHotReloadFrame): TreehouseHotReloadResult

  public companion object {
    public const val SERVICE_NAME: String = "treehouseHotReloadBridge"
  }
}

public class TreehouseHotReloadBridgeAdapter(
  private val client: TreehouseHotReloadClient,
) : TreehouseHotReloadBridge {
  override suspend fun captureFrame(
    request: TreehouseHotReloadCaptureRequest,
  ): TreehouseHotReloadFrame = client.captureFrame(request)

  override suspend fun restoreFrame(frame: TreehouseHotReloadFrame): TreehouseHotReloadResult =
    client.restoreFrame(frame)
}
