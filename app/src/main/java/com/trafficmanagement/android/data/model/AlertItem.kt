package com.trafficmanagement.android.data.model

data class AlertItem(
  val id: String,
  val cameraId: String,
  val roadSegmentId: String,
  val title: String,
  val location: String,
  val detail: String,
  val reporter: String,
  val eventType: TrafficEventType,
  val photoCount: Int,
  val photoUris: List<String> = emptyList(),
  val submittedAt: String,
  val severity: Severity,
  val syncStatus: ReportSyncStatus,
)

enum class Severity {
  HIGH,
  MEDIUM,
  LOW,
}

enum class ReportSyncStatus {
  WAITING_UPLOAD,
  SENT_TO_COMMAND_CENTER,
  ACCEPTED,
}
