package com.example.redwood.emojisearch.gpui

import com.example.redwood.emojisearch.presenter.HttpClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException

class JvmHttpClient(
  private val okHttpClient: OkHttpClient,
) : HttpClient {

  override suspend fun call(url: String, headers: Map<String, String>): String {
    val request = Request.Builder()
      .url(url)
      .headers(headers.toHeaders())
      .build()
    val response = okHttpClient.newCall(request).await()
    return response.body?.string().orEmpty()
  }
}

private suspend fun Call.await(): Response {
  return suspendCancellableCoroutine { continuation ->
    val callback = ContinuationCallback(this, continuation)
    enqueue(callback)
    continuation.invokeOnCancellation(callback)
  }
}

private class ContinuationCallback(
  private val call: Call,
  private val continuation: CancellableContinuation<Response>,
) : Callback,
  CompletionHandler {

  override fun onResponse(call: Call, response: Response) {
    continuation.resume(response)
  }

  override fun onFailure(call: Call, e: IOException) {
    if (!call.isCanceled()) {
      continuation.resumeWithException(e)
    }
  }

  override fun invoke(cause: Throwable?) {
    try {
      call.cancel()
    } catch (_: Throwable) {
    }
  }
}
