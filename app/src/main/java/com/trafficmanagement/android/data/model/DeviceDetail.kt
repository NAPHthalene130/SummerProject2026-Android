package com.trafficmanagement.android.data.model

data class DeviceDetail(
  val id: String,
  val name: String,
  val type: String,
  val location: String,
  val status: String,
  val averageSpeedKmh: Double,
  val trafficFlowPerHour: Int,
  val riskScore: Int,
  val operatingNote: String,
  val latestAlert: String,
  val lastInspection: String,
  val recommendedAction: String,
)
