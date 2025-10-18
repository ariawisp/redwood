package app.cash.redwood.host.gpui

import platform.Foundation.NSProcessInfo

internal actual fun getEnv(name: String): String? {
  return NSProcessInfo.processInfo.environment[name] as? String
}
