package app.cash.redwood.treehouse

import app.cash.redwood.protocol.Change

/**
 * Host-agnostic sink for protocol changes emitted by a Redwood guest composition.
 * A host implements this to forward protocol changes to its rendering pipeline.
 */
public fun interface FrameSink {
  public fun apply(changes: List<Change>)
}

