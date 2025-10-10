package app.cash.redwood.treehouse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateRegistry
import app.cash.redwood.compose.RedwoodComposition
import app.cash.redwood.protocol.ChangesSink
import app.cash.redwood.protocol.guest.GuestProtocolAdapter
import app.cash.redwood.protocol.guest.ProtocolRedwoodComposition
import app.cash.redwood.ui.OnBackPressedDispatcher
import app.cash.redwood.ui.UiConfiguration
import app.cash.redwood.ui.core.api.FocusDirector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Starts a Redwood guest composition that emits protocol changes to the provided [frameSink].
 *
 * This harness is host-agnostic: the caller provides a [GuestProtocolAdapter], wiring of UI
 * services (back press, focus, saveable state, ui configs), and a sink for protocol changes.
 *
 * The returned [ProtocolGuestComposition] exposes [onFrame] to flush pending protocol changes
 * (typically called from a frame/tick callback) and [close] to tear down resources.
 */
public fun startProtocolGuest(
  scope: CoroutineScope,
  guestAdapter: GuestProtocolAdapter,
  widgetVersion: UInt,
  onBackPressedDispatcher: OnBackPressedDispatcher,
  focusDirector: FocusDirector,
  saveableStateRegistry: SaveableStateRegistry?,
  uiConfigurations: StateFlow<UiConfiguration>,
  frameSink: FrameSink,
  content: @Composable () -> Unit,
): ProtocolGuestComposition {
  // Route protocol changes into the provided sink.
  guestAdapter.initChangesSink(ChangesSink { changes -> frameSink.apply(changes) })

  // Create a Redwood composition that drives the protocol guest adapter.
  val composition: RedwoodComposition = ProtocolRedwoodComposition(
    scope = scope,
    guestAdapter = guestAdapter,
    widgetVersion = widgetVersion,
    onBackPressedDispatcher = onBackPressedDispatcher,
    focusDirector = focusDirector,
    saveableStateRegistry = saveableStateRegistry,
    uiConfigurations = uiConfigurations,
  )

  composition.setContent(content)

  // Emit initial changes produced by setContent(). Further changes should be emitted by calling onFrame().
  guestAdapter.emitChanges()

  return ProtocolGuestComposition(guestAdapter, composition)
}

/** Controller for a running protocol guest composition. */
public class ProtocolGuestComposition internal constructor(
  private val guestAdapter: GuestProtocolAdapter,
  private val composition: RedwoodComposition,
) {
  /** Flush any pending protocol changes to the frame sink. Call on each frame/tick. */
  public fun onFrame() {
    guestAdapter.emitChanges()
  }

  /** Cancel the composition and release resources. */
  public fun close() {
    composition.cancel()
  }
}

