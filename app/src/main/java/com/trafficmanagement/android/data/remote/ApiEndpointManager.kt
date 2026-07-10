package com.trafficmanagement.android.data.remote

import android.content.Context
import com.trafficmanagement.android.BuildConfig
import java.net.URI

object ApiEndpointManager {
  private const val PREFERENCES_NAME = "cloud_api_settings"
  private const val KEY_BASE_URL = "base_url"
  private lateinit var appContext: Context

  fun initialize(context: Context) {
    appContext = context.applicationContext
  }

  fun baseUrl(): String {
    check(::appContext.isInitialized) { "ApiEndpointManager is not initialized" }
    val saved = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .getString(KEY_BASE_URL, null)
      .orEmpty()
    return normalize(saved.ifBlank { BuildConfig.API_BASE_URL })
  }

  fun saveBaseUrl(value: String): Result<String> = runCatching {
    val normalized = normalize(value)
    val uri = URI.create(normalized)
    require(uri.scheme == "https" || uri.scheme == "http") { "地址必须以 https:// 或 http:// 开头" }
    require(!uri.host.isNullOrBlank()) { "请输入有效的云端域名或 IP 地址" }
    appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_BASE_URL, normalized)
      .apply()
    normalized
  }

  fun reset() {
    appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit()
      .remove(KEY_BASE_URL)
      .apply()
  }

  private fun normalize(value: String): String = value.trim().trimEnd('/')
}
