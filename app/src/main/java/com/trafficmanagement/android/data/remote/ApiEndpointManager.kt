package com.trafficmanagement.android.data.remote

import android.content.Context
import com.trafficmanagement.android.BuildConfig
import java.net.URI

object ApiEndpointManager {
  private const val PREFERENCES_NAME = "cloud_api_settings"
  private const val KEY_LEGACY_BASE_URL = "base_url"
  private const val KEY_CONNECTION_MODE = "connection_mode"
  private const val KEY_CLOUD_BASE_URL = "cloud_base_url"
  private const val KEY_LOCAL_BASE_URL = "local_base_url"
  private const val DEFAULT_CLOUD_BASE_URL = "http://172.29.75.177:8000"
  private lateinit var appContext: Context

  enum class ConnectionMode {
    CLOUD,
    LOCAL,
  }

  fun initialize(context: Context) {
    appContext = context.applicationContext
  }

  fun baseUrl(): String {
    check(::appContext.isInitialized) { "ApiEndpointManager is not initialized" }
    return when (connectionMode()) {
      ConnectionMode.CLOUD -> cloudBaseUrl()
      ConnectionMode.LOCAL -> localBaseUrl()
    }
  }

  fun connectionMode(): ConnectionMode {
    check(::appContext.isInitialized) { "ApiEndpointManager is not initialized" }
    val saved = preferences().getString(KEY_CONNECTION_MODE, null)
    return runCatching { ConnectionMode.valueOf(saved.orEmpty()) }
      .getOrDefault(ConnectionMode.CLOUD)
  }

  fun cloudBaseUrl(): String = normalize(
    preferences().getString(KEY_CLOUD_BASE_URL, null).orEmpty().ifBlank { DEFAULT_CLOUD_BASE_URL },
  )

  fun localBaseUrl(): String {
    val preferences = preferences()
    val saved = preferences.getString(KEY_LOCAL_BASE_URL, null).orEmpty()
    val legacy = preferences.getString(KEY_LEGACY_BASE_URL, null).orEmpty()
    return normalize(saved.ifBlank { legacy.ifBlank { BuildConfig.API_BASE_URL } })
  }

  fun saveSettings(
    mode: ConnectionMode,
    cloudUrl: String,
    localUrl: String,
  ): Result<String> = runCatching {
    val normalizedCloud = validate(cloudUrl, "云端地址")
    val normalizedLocal = validate(localUrl, "本地地址")
    preferences()
      .edit()
      .putString(KEY_CONNECTION_MODE, mode.name)
      .putString(KEY_CLOUD_BASE_URL, normalizedCloud)
      .putString(KEY_LOCAL_BASE_URL, normalizedLocal)
      .remove(KEY_LEGACY_BASE_URL)
      .apply()
    if (mode == ConnectionMode.CLOUD) normalizedCloud else normalizedLocal
  }

  private fun validate(value: String, label: String): String {
    val normalized = normalize(value)
    val uri = URI.create(normalized)
    require(uri.scheme == "https" || uri.scheme == "http") { "地址必须以 https:// 或 http:// 开头" }
    require(!uri.host.isNullOrBlank()) { "请输入有效的${label}域名或 IP 地址" }
    return normalized
  }

  fun reset() {
    preferences()
      .edit()
      .remove(KEY_LEGACY_BASE_URL)
      .remove(KEY_CONNECTION_MODE)
      .remove(KEY_CLOUD_BASE_URL)
      .remove(KEY_LOCAL_BASE_URL)
      .apply()
  }

  private fun preferences() = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  private fun normalize(value: String): String = value.trim().trimEnd('/')
}
