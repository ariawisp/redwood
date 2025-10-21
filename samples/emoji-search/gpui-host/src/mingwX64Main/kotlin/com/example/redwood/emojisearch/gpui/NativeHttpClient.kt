package com.example.redwood.emojisearch.gpui

import com.example.redwood.emojisearch.presenter.HttpClient
import io.ktor.client.HttpClient as KtorHttpClient
import io.ktor.client.engine.winhttp.WinHttp
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText

actual fun platformHttpClient(): HttpClient = NativeKtorHttpClient()

private class NativeKtorHttpClient : HttpClient {
  private val client = KtorHttpClient(WinHttp) {}

  override suspend fun call(url: String, headers: Map<String, String>): String {
    val response = client.get(url) {
      headers { headers.forEach { (k, v) -> append(k, v) } }
    }
    return response.bodyAsText()
  }
}
