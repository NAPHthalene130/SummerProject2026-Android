package com.trafficmanagement.android.data.remote

import android.os.Handler
import android.os.Looper
import com.trafficmanagement.android.BuildConfig
import com.trafficmanagement.android.data.model.StaffMember
import com.trafficmanagement.android.data.model.WorkOrderItem
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

object WorkOrderApi {
  private val executor = Executors.newCachedThreadPool()
  private val mainHandler = Handler(Looper.getMainLooper())
  fun ping(callback: (Result<Boolean>) -> Unit) {
    execute(callback) {
      request("GET", "/health")
      true
    }
  }

  private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

  fun fetchWorkOrders(callback: (Result<List<WorkOrderItem>>) -> Unit) {
    execute(callback) {
      val payload = request("GET", "/api/v1/work-orders/")
      val array = JSONArray(payload)
      List(array.length()) { index -> parseWorkOrder(array.getJSONObject(index)) }
    }
  }

  fun fetchStaff(callback: (Result<List<StaffMember>>) -> Unit) {
    execute(callback) {
      val payload = request("GET", "/api/v1/staff/")
      val array = JSONArray(payload)
      List(array.length()) { index -> parseStaff(array.getJSONObject(index)) }
    }
  }

  fun dispatchWorkOrder(workOrderId: String, userId: String, callback: (Result<WorkOrderItem>) -> Unit) {
    execute(callback) {
      val body = JSONObject().put("user_id", userId.toInt()).toString()
      parseWorkOrder(requestJson("PUT", workOrderPath(workOrderId, "dispatch"), body))
    }
  }

  fun updateStatus(
    workOrderId: String,
    status: String,
    processMessage: String? = null,
    processImageUrl: String? = null,
    callback: (Result<WorkOrderItem>) -> Unit,
  ) {
    execute(callback) {
      val body = JSONObject().put("status", status)
      processMessage?.takeIf { it.isNotBlank() }?.let { body.put("process_message", it) }
      processImageUrl?.takeIf { it.isNotBlank() }?.let { body.put("process_image_url", it) }
      parseWorkOrder(requestJson("PATCH", workOrderPath(workOrderId, "status"), body.toString()))
    }
  }

  private fun workOrderPath(workOrderId: String, action: String): String {
    val encoded = URLEncoder.encode(workOrderId, StandardCharsets.UTF_8.toString())
    return "/api/v1/work-orders/$encoded/$action"
  }

  private fun requestJson(method: String, path: String, body: String): JSONObject =
    JSONObject(request(method, path, body))

  private fun request(method: String, path: String, body: String? = null): String {
    val baseUrl = ApiEndpointManager.baseUrl()
    val connection = URI.create("$baseUrl$path").toURL().openConnection() as HttpURLConnection
    return try {
      connection.requestMethod = method
      connection.connectTimeout = 8_000
      connection.readTimeout = 12_000
      connection.setRequestProperty("Accept", "application/json")
      if (body != null) {
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(body) }
      }

      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      val payload = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
      if (status !in 200..299) {
        val detail = runCatching { JSONObject(payload).optString("detail") }.getOrNull()
        throw IllegalStateException(detail?.takeIf { it.isNotBlank() } ?: "HTTP $status")
      }
      payload
    } finally {
      connection.disconnect()
    }
  }

  private fun parseWorkOrder(json: JSONObject): WorkOrderItem = WorkOrderItem(
    workOrderId = json.optString("work_order_id"),
    eventId = json.optString("event_id"),
    cameraId = json.optString("camera_id"),
    cameraName = json.optString("camera_name"),
    segmentId = json.optString("segment_id"),
    segmentName = json.optString("segment_name"),
    monitorAddress = json.optString("monitor_address"),
    accidentInfo = json.optString("accident_info"),
    eventTime = json.optString("event_time"),
    eventLevel = json.optString("event_level"),
    status = json.optString("status"),
    assignee = json.optNullableString("assignee"),
    description = json.optString("description"),
    aiSuggestion = json.optString("ai_suggestion"),
    sceneImages = json.optStringList("scene_images"),
    sceneInfo = json.optString("scene_info"),
    processMessage = json.optNullableString("process_message"),
    processImages = json.optStringList("process_images"),
    completedAt = json.optNullableString("completed_at"),
  )

  private fun parseStaff(json: JSONObject) = StaffMember(
    id = json.optString("id"),
    name = json.optString("name"),
    role = json.optString("role"),
    status = json.optString("status"),
    distanceKm = json.optDouble("distance_km", 0.0),
  )

  private fun JSONObject.optNullableString(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
  }

  private fun JSONObject.optStringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
  }

  private fun <T> execute(callback: (Result<T>) -> Unit, block: () -> T) {
    executor.execute {
      val result = runCatching(block)
      mainHandler.post { callback(result) }
    }
  }
}
