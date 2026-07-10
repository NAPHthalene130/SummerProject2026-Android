package com.trafficmanagement.android.data.model

data class InspectionTask(
  val id: String,
  val cameraId: String,
  val roadSegmentId: String,
  val title: String,
  val zone: String,
  val dueTime: String,
  val source: String,
  val eventType: TrafficEventType,
  val riskLevel: RiskLevel,
  val anomaly: String,
  val videoReplayUrl: String,
  var status: WorkOrderStatus,
  var resultStatus: String? = null,
  var resultNote: String? = null,
  var photoCount: Int = 0,
  var photoUris: MutableList<String> = mutableListOf(),
)

enum class WorkOrderStatus {
  PENDING_REPAIR,
  WAITING_DESKTOP_REVIEW,
  REVIEWED,
}

enum class TrafficEventType {
  COLLISION,
  REAR_END,
  SPEEDING,
  ILLEGAL_PARKING,
  CONGESTION,
}

enum class RiskLevel {
  HIGH,
  MEDIUM,
  LOW,
}
