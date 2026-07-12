package com.trafficmanagement.android.data

import android.content.Context
import android.net.Uri
import com.trafficmanagement.android.BuildConfig
import com.trafficmanagement.android.data.model.StaffMember
import com.trafficmanagement.android.data.model.WorkOrderItem
import com.trafficmanagement.android.data.remote.WorkOrderApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WorkOrderRepository {
  val isMockMode: Boolean
    get() = BuildConfig.WORK_ORDER_USE_MOCK

  private val mockOrders = mutableListOf(
    WorkOrderItem(
      workOrderId = "WO-MOCK-001",
      eventId = "EVT-MOCK-001",
      cameraId = "CAM-006",
      cameraName = "高新一路事故高发点",
      segmentId = "SEG-GAOXIN-01",
      segmentName = "高新一路东段",
      monitorAddress = "高新一路与创新路交叉口",
      accidentInfo = "疑似两车追尾，占用右侧车道",
      eventTime = "2026-07-10 09:15:00",
      eventLevel = "high",
      status = "unassigned",
      assignee = null,
      description = "监控画面检测到两辆小客车异常停止，后方车辆开始排队。",
      aiSuggestion = "优先确认人员安全，设置警示区域，并联动交警和清障车辆。",
      sceneImages = listOf("https://placehold.co/640x360/172033/f4f8ff?text=Mock+Accident"),
      sceneInfo = "右侧车道有两辆车辆停止，后方排队约 120 米。",
      processMessage = null,
      processImages = emptyList(),
      completedAt = null,
    ),
    WorkOrderItem(
      workOrderId = "WO-MOCK-002",
      eventId = "EVT-MOCK-002",
      cameraId = "CAM-003",
      cameraName = "创新路西段摄像头",
      segmentId = "SEG-INNOVATION-02",
      segmentName = "创新路施工路段",
      monitorAddress = "创新路东段施工围挡区域",
      accidentInfo = "施工占道导致车辆拥堵",
      eventTime = "2026-07-10 09:22:00",
      eventLevel = "medium",
      status = "pending",
      assignee = "人员B",
      description = "施工围挡附近车辆排队明显，通行效率持续下降。",
      aiSuggestion = "核查施工占道范围，补充临时警示牌并发布绕行提示。",
      sceneImages = listOf("https://placehold.co/640x360/2c2f39/f4f8ff?text=Mock+Congestion"),
      sceneInfo = "施工区域仅剩一条车道通行，车辆排队约 18 辆。",
      processMessage = null,
      processImages = emptyList(),
      completedAt = null,
    ),
    WorkOrderItem(
      workOrderId = "WO-MOCK-003",
      eventId = "EVT-MOCK-003",
      cameraId = "CAM-004",
      cameraName = "学院路园区入口摄像头",
      segmentId = "SEG-CAMPUS-03",
      segmentName = "学院路园区入口西段",
      monitorAddress = "学院路园区入口",
      accidentInfo = "早高峰入口拥堵",
      eventTime = "2026-07-10 09:30:00",
      eventLevel = "medium",
      status = "processing",
      assignee = "人员C",
      description = "园区入口车辆集中进入，短时拥堵影响学院路通行。",
      aiSuggestion = "安排现场疏导，优化入口排队动线并开放临时停车区。",
      sceneImages = listOf("https://placehold.co/640x360/1b3044/f4f8ff?text=Mock+Campus"),
      sceneInfo = "入口等待车辆约 18 辆，机动车与非机动车短时交织。",
      processMessage = null,
      processImages = emptyList(),
      completedAt = null,
    ),
    WorkOrderItem(
      workOrderId = "WO-MOCK-004",
      eventId = "EVT-MOCK-004",
      cameraId = "CAM-002",
      cameraName = "核心十字路口鹰眼",
      segmentId = "SEG-TECH-04",
      segmentName = "科技大道核心路口",
      monitorAddress = "科技大道核心十字路口",
      accidentInfo = "违停车辆影响右转车道",
      eventTime = "2026-07-10 08:40:00",
      eventLevel = "low",
      status = "completed",
      assignee = "人员A",
      description = "路口附近检测到违停车辆，影响右转车辆通行。",
      aiSuggestion = "现场提醒车辆驶离，并将该位置纳入重点巡查点。",
      sceneImages = listOf("https://placehold.co/640x360/14263a/f4f8ff?text=Mock+Parking"),
      sceneInfo = "违停车辆停靠约 4 分钟，未造成持续拥堵。",
      processMessage = "现场已完成劝离，路口恢复正常通行。",
      processImages = listOf("https://placehold.co/640x360/123826/f4f8ff?text=Mock+Resolved"),
      completedAt = "2026-07-10 08:58:00",
    ),
    WorkOrderItem(
      workOrderId = "WO-MOCK-005",
      eventId = "EVT-MOCK-005",
      cameraId = "CAM-005",
      cameraName = "云计算路北段摄像头",
      segmentId = "SEG-CLOUD-05",
      segmentName = "云计算路北段",
      monitorAddress = "云计算路公交港湾附近",
      accidentInfo = "AI 误报行人进入机动车道",
      eventTime = "2026-07-10 07:52:00",
      eventLevel = "low",
      status = "ignored",
      assignee = "人员D",
      description = "模型将隔离区域内的保洁人员误判为行人闯入机动车道。",
      aiSuggestion = "将该样本加入误报样本库，优化施工人员识别标签。",
      sceneImages = listOf("https://placehold.co/640x360/202b3b/f4f8ff?text=Mock+False+Alarm"),
      sceneInfo = "现场人员始终位于隔离区域内作业。",
      processMessage = "人工复核为误报，工单已关闭。",
      processImages = emptyList(),
      completedAt = "2026-07-10 08:05:00",
    ),
  )

  private val mockStaff = mutableListOf(
    StaffMember("1", "人员A", "道路管理员", "idle", 0.8),
    StaffMember("2", "人员B", "巡检员", "busy", 1.1),
    StaffMember("3", "人员C", "巡检员", "busy", 1.5),
    StaffMember("4", "人员D", "应急处置员", "idle", 1.8),
  )

  fun fetchWorkOrders(userId: Int = 0, callback: (Result<List<WorkOrderItem>>) -> Unit) {
    if (!isMockMode) {
      WorkOrderApi.fetchWorkOrders(userId, callback)
      return
    }
    callback(Result.success(mockOrders.toList()))
  }

  fun fetchStaff(callback: (Result<List<StaffMember>>) -> Unit) {
    if (!isMockMode) {
      WorkOrderApi.fetchStaff(callback)
      return
    }
    callback(Result.success(mockStaff.toList()))
  }

  fun dispatchWorkOrder(workOrderId: String, userId: String, callback: (Result<WorkOrderItem>) -> Unit) {
    if (!isMockMode) {
      WorkOrderApi.dispatchWorkOrder(workOrderId, userId, callback)
      return
    }
    val staff = mockStaff.firstOrNull { it.id == userId }
    val index = mockOrders.indexOfFirst { it.workOrderId == workOrderId }
    if (staff == null || index < 0) {
      callback(Result.failure(IllegalArgumentException("Mock 工单或人员不存在")))
      return
    }
    val updated = mockOrders[index].copy(status = "pending", assignee = staff.name)
    mockOrders[index] = updated
    callback(Result.success(updated))
  }

  fun updateStatus(
    workOrderId: String,
    userId: Int,
    status: String,
    processMessage: String?,
    processImageUrl: String?,
    callback: (Result<WorkOrderItem>) -> Unit,
  ) {
    if (!isMockMode) {
      WorkOrderApi.updateStatus(workOrderId, userId, status, processMessage, processImageUrl, callback)
      return
    }
    val index = mockOrders.indexOfFirst { it.workOrderId == workOrderId }
    if (index < 0) {
      callback(Result.failure(IllegalArgumentException("Mock 工单不存在")))
      return
    }
    val current = mockOrders[index]
    val resolved = status == "completed" || status == "ignored" || status == "false_alarm"
    val updated = current.copy(
      status = status,
      processMessage = processMessage ?: current.processMessage,
      processImages = processImageUrl?.takeIf { it.isNotBlank() }?.let(::listOf) ?: current.processImages,
      completedAt = if (resolved) nowText() else current.completedAt,
    )
    mockOrders[index] = updated
    callback(Result.success(updated))
  }

  fun uploadProcessImage(
    context: Context,
    imageUri: Uri,
    callback: (Result<String>) -> Unit,
  ) {
    if (!isMockMode) {
      WorkOrderApi.uploadImage(context, imageUri, callback)
      return
    }
    callback(Result.success(imageUri.toString()))
  }

  private fun nowText(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
}
