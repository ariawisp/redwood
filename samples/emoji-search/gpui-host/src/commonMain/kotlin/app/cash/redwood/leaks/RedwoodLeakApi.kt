package app.cash.redwood.leaks

import kotlin.RequiresOptIn

/**
 * Local stub of the Redwood leak detection opt-in marker so the sample can compile without
 * depending on the full leak detector.
 */
@RedwoodLeakApi
@RequiresOptIn("This API is unstable and for Redwood internal use only")
public annotation class RedwoodLeakApi
