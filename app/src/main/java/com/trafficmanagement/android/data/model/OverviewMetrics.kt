package com.trafficmanagement.android.data.model

data class OverviewMetrics(
  val siteName: String,
  val citySafetyScore: Int,
  val activeAlerts: Int,
  val pendingTasks: Int,
  val onlineCameras: Int,
)
