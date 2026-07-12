package com.trafficmanagement.android.ui.inspection

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.WorkOrderRepository
import com.trafficmanagement.android.data.model.WorkOrderFilter
import com.trafficmanagement.android.data.model.WorkOrderItem
import com.trafficmanagement.android.data.remote.ApiEndpointManager
import com.trafficmanagement.android.data.remote.RemoteImageLoader
import com.trafficmanagement.android.utils.AuthManager
import com.trafficmanagement.android.utils.UiMessageHelper

class InspectionFragment : Fragment(R.layout.fragment_inspection) {
  private val orders = mutableListOf<WorkOrderItem>()
  private lateinit var adapter: InspectionAdapter
  private lateinit var recyclerView: RecyclerView
  private lateinit var progress: View
  private lateinit var messageLayout: LinearLayout
  private lateinit var messageTitle: TextView
  private lateinit var messageBody: TextView
  private var filter = WorkOrderFilter.UNRESOLVED

  private var selectedProcessImageUri: Uri? = null
  private var activeProcessImageName: TextView? = null
  private var activeProcessImagePreview: ImageView? = null
  private val pickProcessImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    selectedProcessImageUri = uri
    activeProcessImageName?.text = if (uri == null) "未选择图片" else "已选择图片"
    activeProcessImagePreview?.apply {
      if (uri == null) {
        visibility = View.GONE
        setImageDrawable(null)
      } else {
        visibility = View.VISIBLE
        setImageURI(uri)
      }
    }
  }

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
      if (WorkOrderRepository.isMockMode) "当前为 Mock 演示数据，所有操作仅在本机生效" else "已连接云端工单服务"

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

    val userId = AuthManager.getCurrentUserId(requireContext())
    if (userId <= 0) {
      progress.isVisible = false
      orders.clear()
      adapter.submitItems(emptyList())
      showMessage("请先注册或登录", "登录后可查看与本人职责类别相同的全部工单，并处理现场反馈。")
      return
    }

    WorkOrderRepository.fetchWorkOrders(userId) { result ->
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
        if (orders.isEmpty()) "当前职责组暂无工单。" else "当前筛选条件下没有工单，请切换分类查看。",
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
    renderImages(dialogView.findViewById(R.id.layoutSceneImages), order.sceneImages)

    val recordLayout = dialogView.findViewById<LinearLayout>(R.id.layoutProcessRecord)
    recordLayout.isVisible = order.isResolved || !order.processMessage.isNullOrBlank()
    dialogView.findViewById<TextView>(R.id.tvDialogProcessRecord).text = buildString {
      append(order.processMessage ?: "暂无处置说明")
      order.completedAt?.let { append("\n完成时间：$it") }
      if (order.processImages.isNotEmpty()) append("\n处置图片：${order.processImages.size} 张")
    }
    renderImages(dialogView.findViewById(R.id.layoutProcessImages), order.processImages)

    dialogView.findViewById<LinearLayout>(R.id.layoutOrderActions).isVisible = !order.isResolved
    dialogView.findViewById<MaterialButton>(R.id.btnDialogProcessing).apply {
      isEnabled = order.status != "processing"
      setOnClickListener { showFeedbackDialog(order, "processing", dialog) }
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

  private fun renderImages(container: LinearLayout, images: List<String>) {
    container.removeAllViews()
    if (images.isEmpty()) {
      container.addView(TextView(requireContext()).apply {
        text = "暂无图片"
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
        contentDescription = "工单图片"
      }
      container.addView(imageView)
      RemoteImageLoader.load(imageView, source)
    }
  }

  private fun dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

  private fun showFeedbackDialog(order: WorkOrderItem, status: String, detailDialog: AlertDialog) {
    val feedbackView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_work_order_feedback, null)
    feedbackView.findViewById<TextView>(R.id.tvFeedbackTitle).text = feedbackTitle(status)
    val messageInput = feedbackView.findViewById<TextInputEditText>(R.id.etProcessMessage)
    messageInput.setText(defaultProcessMessage(status))

    selectedProcessImageUri = null
    activeProcessImageName = feedbackView.findViewById(R.id.tvProcessImageName)
    activeProcessImagePreview = feedbackView.findViewById(R.id.ivProcessImagePreview)
    feedbackView.findViewById<MaterialButton>(R.id.btnPickProcessImage).setOnClickListener {
      pickProcessImageLauncher.launch("image/*")
    }

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
        submitFeedback(order, status, message, selectedProcessImageUri, detailDialog) {
          feedbackDialog.dismiss()
        }
      }
    }
    feedbackDialog.setOnDismissListener {
      activeProcessImageName = null
      activeProcessImagePreview = null
    }
    feedbackDialog.show()
  }

  private fun submitFeedback(
    order: WorkOrderItem,
    status: String,
    message: String,
    imageUri: Uri?,
    detailDialog: AlertDialog,
    onSuccess: (() -> Unit)? = null,
  ) {
    if (imageUri == null) {
      updateStatus(order, status, message, null, detailDialog, onSuccess)
      return
    }

    WorkOrderRepository.uploadProcessImage(requireContext(), imageUri) { uploadResult ->
      if (!isAdded) return@uploadProcessImage
      uploadResult.onSuccess { imageUrl ->
        updateStatus(order, status, message, imageUrl, detailDialog, onSuccess)
      }.onFailure {
        showApiError("上传处置图片失败", it)
      }
    }
  }

  private fun feedbackTitle(status: String): String {
    return when (status) {
      "processing" -> "标记处理中"
      "completed" -> "完成工单"
      "ignored", "false_alarm" -> "标记误报"
      else -> "更新工单"
    }
  }

  private fun defaultProcessMessage(status: String): String {
    return when (status) {
      "processing" -> "现场人员已开始处理，可上传现场过程图片。"
      "completed" -> "现场处置完成，道路恢复通行。"
      "ignored", "false_alarm" -> "人工复核为误报，工单关闭。"
      else -> ""
    }
  }

  private fun updateStatus(
    order: WorkOrderItem,
    status: String,
    message: String?,
    imageUrl: String?,
    detailDialog: AlertDialog,
    onSuccess: (() -> Unit)? = null,
  ) {
    WorkOrderRepository.updateStatus(
      order.workOrderId,
      AuthManager.getCurrentUserId(requireContext()),
      status,
      message,
      imageUrl,
    ) { result ->
      if (!isAdded) return@updateStatus
      result.onSuccess { updated ->
        replaceOrder(updated)
        onSuccess?.invoke()
        detailDialog.dismiss()
        val notice = if (status == "completed" || status == "ignored" || status == "false_alarm") {
          "处置结果已提交，等待电脑端审核"
        } else {
          "工单状态已更新为 ${InspectionAdapter.statusText(status)}"
        }
        UiMessageHelper.showShort(requireContext(), notice)
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
