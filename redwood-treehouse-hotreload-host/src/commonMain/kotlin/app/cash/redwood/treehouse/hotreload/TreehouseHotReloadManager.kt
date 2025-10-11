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
import app.cash.zipline.Zipline
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public interface TreehouseHotReloadStateStore {
  public suspend fun save(key: String, snapshot: StateSnapshot)
  public suspend fun read(key: String): StateSnapshot?
}

/**
 * Coordinates hot reload requests between the Treehouse host runtime and the guest module running
 * inside Zipline. This class is intentionally framework-agnostic: the caller is responsible for
 * wiring it into the existing Treehouse lifecycle (for example, [CodeHost] and [TreehouseApp]).
 */
public class TreehouseHotReloadManager(
  private val stateStore: TreehouseHotReloadStateStore,
  private val bridgeProvider: suspend () -> TreehouseHotReloadBridge?,
  private val stateKeySelector: (TreehouseHotReloadFrame) -> String =
    { frame -> frame.snapshotId?.value ?: DEFAULT_STATE_KEY },
  private val stateTracker: TreehouseHotReloadStateTracker? = null,
) {

  private val mutex = Mutex()
  private var lastFrame: TreehouseHotReloadFrame? = null

  /**
   * Captures a snapshot from the currently running guest module. The returned frame is also cached
   * locally so the caller may choose to skip network round-trips when immediately restoring the
   * next module.
   */
  public suspend fun captureFrame(
    request: TreehouseHotReloadCaptureRequest = TreehouseHotReloadCaptureRequest(),
  ): TreehouseHotReloadFrame? = mutex.withLock {
    stateTracker?.capturing()
    val bridge = bridgeProvider() ?: return@withLock null
    val frame = bridge.captureFrame(request)
    cacheFrame(frame)
    lastFrame = frame
    stateTracker?.idle()
    frame
  }

  /**
   * Restores a previously captured [frame] on the current guest module. If [frame] is `null`, the
   * manager will attempt to replay the most recent capture stored in memory.
   */
  public suspend fun restoreFrame(
    frame: TreehouseHotReloadFrame? = lastFrame,
  ): TreehouseHotReloadResult? = mutex.withLock {
    stateTracker?.restoring()
    val targetFrame = frame ?: return@withLock null
    val bridge = bridgeProvider() ?: return@withLock null
    val hydrated = hydrateFrame(targetFrame)
    val result = try {
      bridge.restoreFrame(hydrated)
    } catch (exception: Throwable) {
      stateTracker?.failed(exception.message)
      throw exception
    }
    stateTracker?.idle()
    result
  }

  /**
   * Records that the host replaced its running code without successfully restoring the guest
   * snapshot. This clears the cached frame but retains the state persisted in [StateStore] so that
   * the application can fall back to its cold-start path.
   */
  public fun clearCachedFrame() {
    lastFrame = null
    stateTracker?.idle()
  }

  private suspend fun cacheFrame(frame: TreehouseHotReloadFrame) {
    val snapshot = frame.snapshot ?: return
    val key = stateKeySelector(frame)
    stateStore.save(key, snapshot)
  }

  private suspend fun hydrateFrame(frame: TreehouseHotReloadFrame): TreehouseHotReloadFrame {
    if (frame.snapshot != null) return frame

    val key = stateKeySelector(frame)
    val restoredSnapshot = stateStore.read(key)
    return frame.copy(snapshot = restoredSnapshot)
  }

  public companion object {
    public const val DEFAULT_STATE_KEY: String = "__treehouse_hot_reload__"

    /**
     * Helper that looks up the hot reload bridge inside the provided [Zipline] instance.
     */
    public fun bridgeFromZipline(zipline: Zipline): TreehouseHotReloadBridge? {
      return zipline.takeOrNull<TreehouseHotReloadBridge>(TreehouseHotReloadBridge.SERVICE_NAME)
    }
  }
}

private fun <T : Any> Zipline.takeOrNull(name: String): T? = try {
  take(name)
} catch (_: Throwable) {
  null
}
