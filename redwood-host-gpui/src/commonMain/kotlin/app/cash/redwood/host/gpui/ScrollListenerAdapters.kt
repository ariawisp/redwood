package app.cash.redwood.host.gpui

import app.cash.redwood.ui.Px

public fun scrollListener(onScroll: ((Px) -> Unit)?): ScrollListener? {
  return onScroll?.let { callback ->
    object : ScrollListener {
      override fun onScroll(deltaPx: Float, offsetPx: Float) {
        callback(Px.fromHost(offsetPx))
      }
    }
  }
}
