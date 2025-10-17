package com.example.redwood.emojisearch.gpui

import com.example.redwood.emojisearch.presenter.Navigator
object DesktopNavigator : Navigator {
  override fun openUrl(url: String) {
    println("Open URL: $url")
  }
}
