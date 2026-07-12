package com.trafficmanagement.android.data.remote

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.trafficmanagement.android.BuildConfig
import com.trafficmanagement.android.data.model.StaffMember
import com.trafficmanagement.android.data.model.WorkOrderItem
import com.trafficmanagement.android.data.model.AlertItem
import com.trafficmanagement.android.data.model.ReportSyncStatus
import com.trafficmanagement.android.data.model.Severity
import com.trafficmanagement.android.data.model.TrafficEventType
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.UUID

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

  fun fetchWorkOrders(userId: Int = 0, callback: (Result<List<WorkOrderItem>>) -> Unit) {
    execute(callback) {
      val suffix = if (userId > 0) "?user_id=$userId" else ""
      val payload = request("GET", "/api/v1/work-orders/$suffix")
      val array = JSONArray(payload)
      List(array.length()) { index -> parseWorkOrder(array.getJSONObject(index)) }
    }
  }

  fun registerUser(name: String, phone: String, password: String, category: String, callback: (Result<JSONObject>) -> Unit) {
    execute(callback) {
      val body = JSONObject().put("name", name).put("phone", phone).put("password", password)
        .put("personnel_category", category).put("site", "中心城区交通处置平台")
      requestJson("POST", "/api/v1/mobile-users/register", body.toString())
    }
  }

  fun loginUser(phone: String, password: String, callback: (Result<JSONObject>) -> Unit) {
    execute(callback) {
      val body = JSONObject().put("phone", phone).put("password", password)
      requestJson("POST", "/api/v1/mobile-users/login", body.toString())
    }
  }

  fun submitMobileReport(userId: Int, title: String, location: String, detail: String, severity: String, imageUrls: List<String>, callback: (Result<JSONObject>) -> Unit) {
    execute(callback) {
      val images = JSONArray().also { array -> imageUrls.forEach(array::put) }
      val body = JSONObject().put("reporter_user_id", userId).put("title", title).put("location", location)
        .put("detail", detail).put("severity", severity).put("event_type", "traffic_event").put("image_urls", images)
      requestJson("POST", "/api/v1/mobile-reports", body.toString())
    }
  }

  fun fetchMobileReports(userId: Int, callback: (Result<List<AlertItem>>) -> Unit) {
    execute(callback) {
      val payload = request("GET", "/api/v1/mobile-reports?reporter_user_id=$userId")
      val array = JSONArray(payload)
      List(array.length()) { index -> parseMobileReport(array.getJSONObject(index)) }
    }
  }

  private fun parseMobileReport(json: JSONObject): AlertItem {
    val severity = when (json.optString("severity")) { "high" -> Severity.HIGH; "low" -> Severity.LOW; else -> Severity.MEDIUM }
    val status = when (json.optString("status")) {
      "converted" -> ReportSyncStatus.ACCEPTED
      "rejected" -> ReportSyncStatus.REJECTED
      else -> ReportSyncStatus.SENT_TO_COMMAND_CENTER
    }
    val images = json.optStringList("image_urls")
    return AlertItem(
      id = "report-${json.optInt("report_id")}", cameraId = "manual-mobile", roadSegmentId = "manual-road",
      title = json.optString("title"), location = json.optString("location"), detail = json.optString("detail"),
      reporter = json.optString("reporter_name"), eventType = TrafficEventType.COLLISION,
      photoCount = images.size, photoUris = images, submittedAt = json.optString("created_at"),
      severity = severity, syncStatus = status, reviewMessage = json.optNullableString("review_message"),
    )
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
    userId: Int,
    status: String,
    processMessage: String? = null,
    processImageUrl: String? = null,
    callback: (Result<WorkOrderItem>) -> Unit,
  ) {
    execute(callback) {
      val body = JSONObject().put("status", status)
      processMessage?.takeIf { it.isNotBlank() }?.let { body.put("process_message", it) }
      processImageUrl?.takeIf { it.isNotBlank() }?.let { body.put("process_image_url", it) }
      if (status == "completed" || status == "ignored" || status == "false_alarm") {
        body.put("user_id", userId)
        if (status == "false_alarm") body.put("status", "ignored")
        parseWorkOrder(requestJson("POST", workOrderPath(workOrderId, "mobile-feedback"), body.toString()))
      } else {
        parseWorkOrder(requestJson("PATCH", workOrderPath(workOrderId, "status"), body.toString()))
      }
    }
  }

  fun uploadImage(context: Context, imageUri: Uri, callback: (Result<String>) -> Unit) {
    execute(callback) {
      val boundary = "TrafficBoundary${UUID.randomUUID().toString().replace("-", "")}"
      val fileName = "work-order-${System.currentTimeMillis()}.jpg"
      val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
      val payload = requestMultipart(
        path = "/api/v1/uploads/images",
        boundary = boundary,
        fieldName = "file",
        fileName = fileName,
        mimeType = mimeType,
      ) { output ->
        context.contentResolver.openInputStream(imageUri)?.use { input ->
          input.copyTo(output)
        } ?: throw IllegalArgumentException("无法读取选择的图片")
      }
      val json = JSONObject(payload)
      json.optString("url")
        .ifBlank { json.optString("image_url") }
        .ifBlank { json.optString("file_url") }
        .ifBlank { throw IllegalStateException("上传成功但后端未返回图片 URL") }
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

  private fun requestMultipart(
    path: String,
    boundary: String,
    fieldName: String,
    fileName: String,
    mimeType: String,
    writeFile: (java.io.OutputStream) -> Unit,
  ): String {
    val baseUrl = ApiEndpointManager.baseUrl()
    val lineBreak = "\r\n"
    val connection = URI.create("$baseUrl$path").toURL().openConnection() as HttpURLConnection
    return try {
      connection.requestMethod = "POST"
      connection.connectTimeout = 8_000
      connection.readTimeout = 20_000
      connection.doOutput = true
      connection.setRequestProperty("Accept", "application/json")
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

      connection.outputStream.use { output ->
        output.write("--$boundary$lineBreak".toByteArray(StandardCharsets.UTF_8))
        output.write(
          "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"$lineBreak"
            .toByteArray(StandardCharsets.UTF_8),
        )
        output.write("Content-Type: $mimeType$lineBreak$lineBreak".toByteArray(StandardCharsets.UTF_8))
        writeFile(output)
        output.write(lineBreak.toByteArray(StandardCharsets.UTF_8))
        output.write("--$boundary--$lineBreak".toByteArray(StandardCharsets.UTF_8))
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
    feedbackReviewStatus = json.optString("feedback_review_status", "none"),
    feedbackRequestedStatus = json.optNullableString("feedback_requested_status"),
    feedbackReviewMessage = json.optNullableString("feedback_review_message"),
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
