package com.trafficmanagement.android.data.model

data class WorkOrderItem(
  val workOrderId: String,
  val eventId: String,
  val cameraId: String,
  val cameraName: String,
  val segmentId: String,
  val segmentName: String,
  val monitorAddress: String,
  val accidentInfo: String,
  val eventTime: String,
  val eventLevel: String,
  val status: String,
  val assignee: String?,
  val description: String,
  val aiSuggestion: String,
  val sceneImages: List<String>,
  val sceneInfo: String,
  val processMessage: String?,
  val processImages: List<String>,
  val completedAt: String?,
) {
  val isCompleted: Boolean
    get() = status == "completed"

  val isIgnored: Boolean
    get() = status == "ignored" || status == "false_alarm"

  val isResolved: Boolean
    get() = isCompleted || isIgnored
}

data class StaffMember(
  val id: String,
  val name: String,
  val role: String,
  val status: String,
  val distanceKm: Double,
)

enum class WorkOrderFilter {
  UNRESOLVED,
  COMPLETED,
  IGNORED,
  ALL,
}
