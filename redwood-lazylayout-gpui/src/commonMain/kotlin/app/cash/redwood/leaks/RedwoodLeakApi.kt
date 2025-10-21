package app.cash.redwood.leaks

import kotlin.RequiresOptIn

/**
 * Local stub of the Redwood leak detection opt-in marker so this host can compile
 * without depending on the internal leak detector artifacts.
 */
@RedwoodLeakApi
@RequiresOptIn("This API is unstable and for Redwood internal use only")
public annotation class RedwoodLeakApi
