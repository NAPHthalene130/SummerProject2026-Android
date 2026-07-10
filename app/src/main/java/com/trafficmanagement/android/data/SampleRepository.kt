package com.trafficmanagement.android.data

import com.trafficmanagement.android.data.model.AlertItem
import com.trafficmanagement.android.data.model.DeviceDetail
import com.trafficmanagement.android.data.model.InspectionTask
import com.trafficmanagement.android.data.model.OverviewMetrics
import com.trafficmanagement.android.data.model.ReportSyncStatus
import com.trafficmanagement.android.data.model.RiskLevel
import com.trafficmanagement.android.data.model.Severity
import com.trafficmanagement.android.data.model.TrafficEventType
import com.trafficmanagement.android.data.model.WorkOrderStatus

object SampleRepository {
  private val cameras = listOf(
    DeviceDetail(
      id = "cam-jiefang-north",
      name = "解放路北口摄像头",
      type = "4K 枪机 / RTSP",
      location = "解放路-人民大道北进口",
      status = "高风险",
      averageSpeedKmh = 18.5,
      trafficFlowPerHour = 1840,
      riskScore = 87,
      operatingNote = "YOLO 检测到北向南车流持续排队，平均车速低于 20 km/h，存在追尾风险。",
      latestAlert = "疑似两车追尾，已截取事故前后 30 秒视频。",
      lastInspection = "今天 14:18",
      recommendedAction = "优先到达北进口导流，摆放警示锥桶，核验是否有人受伤并回传现场照片。",
    ),
    DeviceDetail(
      id = "cam-ring-east",
      name = "东环高架匝道摄像头",
      type = "球机 / RTSP",
      location = "东环高架入口匝道",
      status = "中风险",
      averageSpeedKmh = 64.2,
      trafficFlowPerHour = 920,
      riskScore = 63,
      operatingNote = "车速波动较大，历史事故数量偏高，雨天模型风险上浮。",
      latestAlert = "多模态模型提示疑似超速车辆。",
      lastInspection = "今天 13:40",
      recommendedAction = "核对车牌与速度记录，必要时转违章复核并联动电子警察系统。",
    ),
    DeviceDetail(
      id = "cam-school-west",
      name = "实验小学西门摄像头",
      type = "违停抓拍机",
      location = "文昌街实验小学西门",
      status = "待核查",
      averageSpeedKmh = 12.8,
      trafficFlowPerHour = 530,
      riskScore = 58,
      operatingNote = "上学高峰出现临停车辆，车辆轨迹遮挡非机动车道。",
      latestAlert = "疑似违规停车超过 3 分钟。",
      lastInspection = "今天 08:02",
      recommendedAction = "现场劝离违停车辆，拍摄处理前后照片并补充是否影响学生通行。",
    ),
  )

  private val reportItems = mutableListOf(
    AlertItem(
      id = "event-1",
      cameraId = "cam-jiefang-north",
      roadSegmentId = "seg-jiefang-001",
      title = "解放路北口疑似追尾",
      location = "解放路-人民大道北进口",
      detail = "多模态模型识别到两车短距离碰撞，YOLO 轨迹显示后车急停，风险评分 87。",
      reporter = "事件检测智能体",
      eventType = TrafficEventType.REAR_END,
      photoCount = 0,
      submittedAt = "今天 14:21",
      severity = Severity.HIGH,
      syncStatus = ReportSyncStatus.SENT_TO_COMMAND_CENTER,
    ),
    AlertItem(
      id = "event-2",
      cameraId = "cam-school-west",
      roadSegmentId = "seg-wenchang-003",
      title = "实验小学西门疑似违规停车",
      location = "文昌街实验小学西门",
      detail = "车辆在禁停区停留超过 3 分钟，遮挡非机动车通行路径。",
      reporter = "路面巡查员 李警官",
      eventType = TrafficEventType.ILLEGAL_PARKING,
      photoCount = 2,
      submittedAt = "今天 08:10",
      severity = Severity.MEDIUM,
      syncStatus = ReportSyncStatus.ACCEPTED,
    ),
  )

  private val workOrders = mutableListOf(
    InspectionTask(
      id = "wo-traffic-1",
      cameraId = "cam-jiefang-north",
      roadSegmentId = "seg-jiefang-001",
      title = "追尾事故现场处置",
      zone = "解放路-人民大道北进口",
      dueTime = "今天 14:35",
      source = "视频事件智能体",
      eventType = TrafficEventType.REAR_END,
      riskLevel = RiskLevel.HIGH,
      anomaly = "疑似两车追尾，车流量 1840 辆/小时，平均车速 18.5 km/h，天气小雨。",
      videoReplayUrl = "https://traffic.local/replay/event-1.mp4",
      status = WorkOrderStatus.PENDING_REPAIR,
    ),
    InspectionTask(
      id = "wo-traffic-2",
      cameraId = "cam-ring-east",
      roadSegmentId = "seg-ring-012",
      title = "东环匝道超速复核",
      zone = "东环高架入口匝道",
      dueTime = "今天 15:10",
      source = "YOLO 车速估计 + 违章规则",
      eventType = TrafficEventType.SPEEDING,
      riskLevel = RiskLevel.MEDIUM,
      anomaly = "目标车辆估计速度 82 km/h，限速 60 km/h，需现场与电子警察记录交叉核验。",
      videoReplayUrl = "https://traffic.local/replay/event-2.mp4",
      status = WorkOrderStatus.PENDING_REPAIR,
    ),
    InspectionTask(
      id = "wo-traffic-3",
      cameraId = "cam-school-west",
      roadSegmentId = "seg-wenchang-003",
      title = "学校门口违停处置反馈",
      zone = "文昌街实验小学西门",
      dueTime = "今天 08:30",
      source = "违停检测模型",
      eventType = TrafficEventType.ILLEGAL_PARKING,
      riskLevel = RiskLevel.MEDIUM,
      anomaly = "禁停区车辆停留超过 3 分钟，影响非机动车道。",
      videoReplayUrl = "https://traffic.local/replay/event-3.mp4",
      status = WorkOrderStatus.WAITING_DESKTOP_REVIEW,
      resultStatus = "已劝离",
      resultNote = "现场已联系驾驶员驶离，校门口通行恢复正常。",
      photoCount = 2,
    ),
  )

  fun overview(): OverviewMetrics {
    return OverviewMetrics(
      siteName = "城市交通事故与违章事件管理系统",
      citySafetyScore = 91,
      activeAlerts = reportItems.size,
      pendingTasks = workOrders.count { it.status == WorkOrderStatus.PENDING_REPAIR },
      onlineCameras = cameras.size,
    )
  }

  fun alerts(): MutableList<AlertItem> = reportItems

  fun addReport(report: AlertItem) {
    reportItems.add(0, report)
  }

  fun tasks(): MutableList<InspectionTask> = workOrders

  fun topAlert(): AlertItem? = alerts().firstOrNull()

  fun nextPendingTask(): InspectionTask? = tasks().firstOrNull { it.status == WorkOrderStatus.PENDING_REPAIR }

  fun deviceDetail(cameraId: String): DeviceDetail? = cameras.firstOrNull { it.id == cameraId }

  fun defaultDevice(): DeviceDetail = cameras.first()
}
