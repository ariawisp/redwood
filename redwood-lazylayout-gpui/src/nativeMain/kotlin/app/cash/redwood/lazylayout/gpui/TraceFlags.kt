package app.cash.redwood.lazylayout.gpui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun traceFlagEnabled(name: String): Boolean {
  val pointer = getenv(name) ?: return false
  return pointer.toKString() != "0"
}
