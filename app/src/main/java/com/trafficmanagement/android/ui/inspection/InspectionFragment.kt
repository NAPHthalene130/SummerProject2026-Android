package com.trafficmanagement.android.ui.inspection

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.trafficmanagement.android.data.remote.ApiEndpointManager
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.WorkOrderRepository
import com.trafficmanagement.android.data.model.StaffMember
import com.trafficmanagement.android.data.model.WorkOrderFilter
import com.trafficmanagement.android.data.model.WorkOrderItem
import com.trafficmanagement.android.data.remote.RemoteImageLoader
import com.trafficmanagement.android.utils.UiMessageHelper

class InspectionFragment : Fragment(R.layout.fragment_inspection) {
  private val orders = mutableListOf<WorkOrderItem>()
  private val staffMembers = mutableListOf<StaffMember>()
  private lateinit var adapter: InspectionAdapter
  private lateinit var recyclerView: RecyclerView
  private lateinit var progress: View
  private lateinit var messageLayout: LinearLayout
  private lateinit var messageTitle: TextView
  private lateinit var messageBody: TextView
  private var filter = WorkOrderFilter.UNRESOLVED
  private val syncHandler = Handler(Looper.getMainLooper())
  private val syncRunnable = object : Runnable {
    override fun run() {
      if (isAdded && !WorkOrderRepository.isMockMode) {
        loadData(silent = true)
        syncHandler.postDelayed(this, SYNC_INTERVAL_MS)
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    progress = view.findViewById(R.id.progressOrders)
    messageLayout = view.findViewById(R.id.layoutOrderMessage)
    messageTitle = view.findViewById(R.id.tvOrderMessageTitle)
    messageBody = view.findViewById(R.id.tvOrderMessageBody)
    view.findViewById<TextView>(R.id.tvWorkOrderDataSource).text =
      if (WorkOrderRepository.isMockMode) {
        "当前为 Mock 演示数据，所有操作仅在本机生效"
      } else {
        "已连接云端工单服务"
      }
    recyclerView = view.findViewById(R.id.recyclerInspection)
    adapter = InspectionAdapter(::showWorkOrderDialog)
    recyclerView.layoutManager = LinearLayoutManager(requireContext())
    recyclerView.adapter = adapter

    view.findViewById<MaterialButton>(R.id.btnRefreshOrders).setOnClickListener { loadData() }
    view.findViewById<MaterialButtonToggleGroup>(R.id.orderFilterGroup)
      .addOnButtonCheckedListener { _, checkedId, isChecked ->
        if (!isChecked) return@addOnButtonCheckedListener
        filter = when (checkedId) {
          R.id.btnFilterCompleted -> WorkOrderFilter.COMPLETED
          R.id.btnFilterIgnored -> WorkOrderFilter.IGNORED
          R.id.btnFilterAll -> WorkOrderFilter.ALL
          else -> WorkOrderFilter.UNRESOLVED
        }
        renderOrders(view)
      }

    configureMetric(view.findViewById(R.id.metricUnresolved), "未解决工单")
    configureMetric(view.findViewById(R.id.metricCompleted), "已解决工单")
    configureMetric(view.findViewById(R.id.metricIgnored), "已忽略工单")
    configureMetric(view.findViewById(R.id.metricAll), "全部工单")
    loadData()
  }

  override fun onStart() {
    super.onStart()
    if (!WorkOrderRepository.isMockMode) {
      syncHandler.postDelayed(syncRunnable, SYNC_INTERVAL_MS)
    }
  }

  override fun onStop() {
    syncHandler.removeCallbacks(syncRunnable)
    super.onStop()
  }

  private fun loadData(silent: Boolean = false) {
    if (!isAdded) return
    if (!silent) {
      progress.isVisible = true
      recyclerView.isVisible = false
      messageLayout.isVisible = false
    }

    WorkOrderRepository.fetchWorkOrders { result ->
      if (!isAdded) return@fetchWorkOrders
      progress.isVisible = false
      result.onSuccess { loadedOrders ->
        orders.clear()
        orders.addAll(loadedOrders)
        view?.let(::renderOrders)
      }.onFailure { error ->
        if (!silent || orders.isEmpty()) {
          orders.clear()
          adapter.submitItems(emptyList())
          showMessage(
            "后端工单加载失败",
            "${error.message ?: "网络连接异常"}\n当前地址：${ApiEndpointManager.baseUrl()}",
          )
        }
      }
    }

    WorkOrderRepository.fetchStaff { result ->
      if (!isAdded) return@fetchStaff
      result.onSuccess { loadedStaff ->
        staffMembers.clear()
        staffMembers.addAll(loadedStaff.sortedBy { it.distanceKm })
      }
    }
  }

  private fun renderOrders(root: View) {
    val unresolved = orders.count { !it.isResolved }
    val completed = orders.count { it.isCompleted }
    val ignored = orders.count { it.isIgnored }
    setMetric(root.findViewById(R.id.metricUnresolved), unresolved)
    setMetric(root.findViewById(R.id.metricCompleted), completed)
    setMetric(root.findViewById(R.id.metricIgnored), ignored)
    setMetric(root.findViewById(R.id.metricAll), orders.size)

    val visibleOrders = when (filter) {
      WorkOrderFilter.UNRESOLVED -> orders.filterNot { it.isResolved }
      WorkOrderFilter.COMPLETED -> orders.filter { it.isCompleted }
      WorkOrderFilter.IGNORED -> orders.filter { it.isIgnored }
      WorkOrderFilter.ALL -> orders
    }
    adapter.submitItems(visibleOrders)
    recyclerView.isVisible = visibleOrders.isNotEmpty()
    if (visibleOrders.isEmpty()) {
      showMessage(
        "暂无匹配工单",
        if (orders.isEmpty()) "后端数据库中暂无工单。" else "当前筛选条件下没有工单，请切换分类查看。",
      )
    } else {
      messageLayout.isVisible = false
    }
  }

  private fun configureMetric(metricView: View, label: String) {
    metricView.findViewById<TextView>(R.id.tvMetricLabel).text = label
  }

  private fun setMetric(metricView: View, value: Int) {
    metricView.findViewById<TextView>(R.id.tvMetricValue).text = value.toString()
  }

  private fun showMessage(title: String, body: String) {
    recyclerView.isVisible = false
    messageLayout.isVisible = true
    messageTitle.text = title
    messageBody.text = body
  }

  private fun showWorkOrderDialog(order: WorkOrderItem) {
    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_inspection_detail, null)
    val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

    dialogView.findViewById<TextView>(R.id.tvDialogOrderCode).text = order.workOrderId
    dialogView.findViewById<TextView>(R.id.tvDialogTaskTitle).text = order.accidentInfo.ifBlank { "未命名事件" }
    dialogView.findViewById<TextView>(R.id.tvDialogStatus).text =
      "${InspectionAdapter.levelText(order.eventLevel)} · ${InspectionAdapter.statusText(order.status)}"
    dialogView.findViewById<TextView>(R.id.tvDialogMeta).text = buildString {
      append("监控点：${order.cameraName}\n")
      append("地址：${order.monitorAddress.ifBlank { order.segmentName }}\n")
      append("事件时间：${order.eventTime}\n")
      append("处置人员：${order.assignee ?: "暂未派发"}")
    }
    dialogView.findViewById<TextView>(R.id.tvDialogDescription).text = order.description
    dialogView.findViewById<TextView>(R.id.tvDialogAiSuggestion).text = order.aiSuggestion
    dialogView.findViewById<TextView>(R.id.tvDialogSceneInfo).text = order.sceneInfo
    renderSceneImages(dialogView.findViewById(R.id.layoutSceneImages), order.sceneImages)

    val recordLayout = dialogView.findViewById<LinearLayout>(R.id.layoutProcessRecord)
    recordLayout.isVisible = order.isResolved || !order.processMessage.isNullOrBlank()
    dialogView.findViewById<TextView>(R.id.tvDialogProcessRecord).text = buildString {
      append(order.processMessage ?: "暂无处置说明")
      order.completedAt?.let { append("\n完成时间：$it") }
      if (order.processImages.isNotEmpty()) append("\n处置图片：${order.processImages.joinToString()}")
    }

    val actions = dialogView.findViewById<LinearLayout>(R.id.layoutOrderActions)
    actions.isVisible = !order.isResolved
    val assignButton = dialogView.findViewById<MaterialButton>(R.id.btnDialogAssign)
    assignButton.text = if (order.assignee == null) "派发工单" else "重新派发"
    assignButton.setOnClickListener { showStaffDialog(order, dialog) }
    dialogView.findViewById<MaterialButton>(R.id.btnDialogProcessing).apply {
      isEnabled = order.status != "processing"
      setOnClickListener { updateStatus(order, "processing", null, null, dialog) }
    }
    dialogView.findViewById<MaterialButton>(R.id.btnDialogComplete).setOnClickListener {
      showFeedbackDialog(order, "completed", dialog)
    }
    dialogView.findViewById<MaterialButton>(R.id.btnDialogFalseAlarm).setOnClickListener {
      showFeedbackDialog(order, "ignored", dialog)
    }
    dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
    dialog.show()
  }

  private fun renderSceneImages(container: LinearLayout, images: List<String>) {
    container.removeAllViews()
    if (images.isEmpty()) {
      container.addView(TextView(requireContext()).apply {
        text = "暂无现场图片"
        setTextColor(resources.getColor(R.color.text_tertiary, requireContext().theme))
      })
      return
    }

    images.forEach { source ->
      val imageView = ImageView(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(dp(240), dp(135)).also {
          it.marginEnd = dp(10)
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundResource(R.drawable.bg_soft_panel)
        contentDescription = "工单现场图片"
      }
      container.addView(imageView)
      RemoteImageLoader.load(imageView, source)
    }
  }

  private fun dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

  private fun showStaffDialog(order: WorkOrderItem, detailDialog: AlertDialog) {
    val availableStaff = staffMembers.filter { it.status == "idle" }
    if (availableStaff.isEmpty()) {
      UiMessageHelper.showShort(requireContext(), "暂无空闲人员，请刷新后重试")
      return
    }
    val labels = availableStaff.map { "${it.name} · ${it.role} · ${it.distanceKm} km" }.toTypedArray()
    AlertDialog.Builder(requireContext())
      .setTitle("派发工单")
      .setItems(labels) { staffDialog, index ->
        val staff = availableStaff[index]
        staffDialog.dismiss()
        WorkOrderRepository.dispatchWorkOrder(order.workOrderId, staff.id) { result ->
          if (!isAdded) return@dispatchWorkOrder
          result.onSuccess { updated ->
            replaceOrder(updated)
            detailDialog.dismiss()
            UiMessageHelper.showShort(requireContext(), "已派发给 ${staff.name}")
          }.onFailure { showApiError("派发工单失败", it) }
        }
      }
      .setNegativeButton("取消", null)
      .show()
  }

  private fun showFeedbackDialog(order: WorkOrderItem, status: String, detailDialog: AlertDialog) {
    val feedbackView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_work_order_feedback, null)
    feedbackView.findViewById<TextView>(R.id.tvFeedbackTitle).text =
      if (status == "completed") "完成工单" else "忽略工单"
    val messageInput = feedbackView.findViewById<TextInputEditText>(R.id.etProcessMessage)
    val imageInput = feedbackView.findViewById<TextInputEditText>(R.id.etProcessImageUrl)
    messageInput.setText(if (status == "completed") "现场处置完成，道路恢复通行。" else "该工单已忽略。")

    val feedbackDialog = AlertDialog.Builder(requireContext())
      .setView(feedbackView)
      .setNegativeButton("取消", null)
      .setPositiveButton("提交", null)
      .create()
    feedbackDialog.setOnShowListener {
      feedbackDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val message = messageInput.text?.toString().orEmpty().trim()
        if (message.isBlank()) {
          messageInput.error = "请填写处置说明"
          return@setOnClickListener
        }
        feedbackDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        updateStatus(order, status, message, imageInput.text?.toString(), detailDialog) {
          feedbackDialog.dismiss()
        }
      }
    }
    feedbackDialog.show()
  }

  private fun updateStatus(
    order: WorkOrderItem,
    status: String,
    message: String?,
    imageUrl: String?,
    detailDialog: AlertDialog,
    onSuccess: (() -> Unit)? = null,
  ) {
    WorkOrderRepository.updateStatus(order.workOrderId, status, message, imageUrl) { result ->
      if (!isAdded) return@updateStatus
      result.onSuccess { updated ->
        replaceOrder(updated)
        onSuccess?.invoke()
        detailDialog.dismiss()
        UiMessageHelper.showShort(requireContext(), "工单状态已更新为${InspectionAdapter.statusText(status)}")
      }.onFailure { showApiError("更新工单失败", it) }
    }
  }

  private fun replaceOrder(updated: WorkOrderItem) {
    val index = orders.indexOfFirst { it.workOrderId == updated.workOrderId }
    if (index >= 0) orders[index] = updated else orders.add(0, updated)
    view?.let(::renderOrders)
  }

  private fun showApiError(prefix: String, error: Throwable) {
    UiMessageHelper.showShort(requireContext(), "$prefix：${error.message ?: "网络异常"}")
  }

  companion object {
    private const val SYNC_INTERVAL_MS = 5_000L
  }
}
