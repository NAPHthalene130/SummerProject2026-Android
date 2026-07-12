package com.trafficmanagement.android.ui.alerts

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.trafficmanagement.android.BuildConfig
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.model.AlertItem
import com.trafficmanagement.android.data.model.Severity
import com.trafficmanagement.android.utils.UiMessageHelper
import com.trafficmanagement.android.utils.AuthManager
import com.trafficmanagement.android.data.remote.WorkOrderApi
import java.io.File

class AlertsFragment : Fragment(R.layout.fragment_alerts) {
  private lateinit var adapter: AlertAdapter
  private lateinit var recyclerView: RecyclerView
  private val reports = mutableListOf<AlertItem>()
  private var pendingPhotoUri: Uri? = null
  private var reportPhotoCount = 0
  private var reportPhotoCountView: TextView? = null
  private var reportPhotoContainer: LinearLayout? = null
  private val selectedReportPhotoUris = mutableListOf<Uri>()

  private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    if (success) {
      pendingPhotoUri?.let { selectedReportPhotoUris.add(it) }
      reportPhotoCount++
      updateReportPhotoUi()
      UiMessageHelper.showShort(requireContext(), "照片已保存")
    } else {
      pendingPhotoUri = null
      UiMessageHelper.showShort(requireContext(), "未完成拍照")
    }
  }

  private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
    if (uris.isNotEmpty()) {
      selectedReportPhotoUris.addAll(uris)
      reportPhotoCount += uris.size
      updateReportPhotoUi()
      UiMessageHelper.showShort(requireContext(), "已选择 ${uris.size} 张照片")
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<TextView>(R.id.tvAlertSummary).text = "正在同步上报记录..."
    view.findViewById<MaterialButton>(R.id.btnCreateAlertReport).setOnClickListener {
      showReportDialog()
    }

    recyclerView = view.findViewById(R.id.recyclerAlerts)
    recyclerView.layoutManager = LinearLayoutManager(requireContext())
    adapter = AlertAdapter(
      items = reports,
      onResendClick = { alert ->
        syncReport(alert)
      },
    )
    recyclerView.adapter = adapter
    loadReports()
  }

  private fun loadReports() {
    val userId = AuthManager.getCurrentUserId(requireContext())
    if (userId <= 0) {
      reports.clear(); adapter.notifyDataSetChanged()
      view?.findViewById<TextView>(R.id.tvAlertSummary)?.text = "请先注册后端账号再查看上报"
      return
    }
    WorkOrderApi.fetchMobileReports(userId) { result ->
      if (!isAdded) return@fetchMobileReports
      result.onSuccess { items ->
        reports.clear(); reports.addAll(items); adapter.notifyDataSetChanged()
        view?.findViewById<TextView>(R.id.tvAlertSummary)?.text = "已同步 ${reports.size} 条真实上报"
      }.onFailure { UiMessageHelper.showShort(requireContext(), "同步上报失败：${it.message}") }
    }
  }

  private fun syncReport(alert: AlertItem) {
    val userId = AuthManager.getCurrentUserId(requireContext())
    if (userId <= 0) return
    val severity = when (alert.severity) { Severity.HIGH -> "high"; Severity.LOW -> "low"; else -> "medium" }
    WorkOrderApi.submitMobileReport(userId, alert.title, alert.location, alert.detail, severity, alert.photoUris) { result ->
      result.onSuccess { loadReports(); UiMessageHelper.showShort(requireContext(), "已重新同步到指挥端") }
        .onFailure { UiMessageHelper.showShort(requireContext(), "同步失败：${it.message}") }
    }
  }

  private fun showReportDialog() {
    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_alert_report, null)
    val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
    reportPhotoCount = 0

    val chipHigh = dialogView.findViewById<Chip>(R.id.chipReportHigh)
    val chipMedium = dialogView.findViewById<Chip>(R.id.chipReportMedium)
    val chipLow = dialogView.findViewById<Chip>(R.id.chipReportLow)
    val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etReportTitle)
    val etLocation = dialogView.findViewById<TextInputEditText>(R.id.etReportLocation)
    val etDetail = dialogView.findViewById<TextInputEditText>(R.id.etReportDetail)
    reportPhotoCountView = dialogView.findViewById(R.id.tvReportPhotoCount)
    reportPhotoContainer = dialogView.findViewById(R.id.layoutReportPhotos)
    selectedReportPhotoUris.clear()
    updateReportPhotoUi()

    chipMedium.isChecked = true

    dialogView.findViewById<MaterialButton>(R.id.btnReportTakePhoto).setOnClickListener {
      launchCamera()
    }
    dialogView.findViewById<MaterialButton>(R.id.btnReportSelectPhoto).setOnClickListener {
      pickImagesLauncher.launch("image/*")
    }
    dialogView.findViewById<MaterialButton>(R.id.btnReportCancel).setOnClickListener {
      dialog.dismiss()
    }
    dialogView.findViewById<MaterialButton>(R.id.btnReportSubmit).setOnClickListener {
      val title = etTitle.text?.toString()?.trim().orEmpty()
      val location = etLocation.text?.toString()?.trim().orEmpty()
      val detail = etDetail.text?.toString()?.trim().orEmpty()
      if (title.isEmpty() || location.isEmpty() || detail.isEmpty()) {
        UiMessageHelper.showShort(requireContext(), "请填写事件标题、位置和说明")
        return@setOnClickListener
      }

      val severity = when {
        chipHigh.isChecked -> Severity.HIGH
        chipLow.isChecked -> Severity.LOW
        else -> Severity.MEDIUM
      }
      val userId = AuthManager.getCurrentUserId(requireContext())
      if (userId <= 0) {
        UiMessageHelper.showShort(requireContext(), "请先在‘我的’页面注册并登录")
        return@setOnClickListener
      }
      val severityCode = when (severity) { Severity.HIGH -> "high"; Severity.LOW -> "low"; else -> "medium" }
      WorkOrderApi.submitMobileReport(userId, title, location, detail, severityCode, selectedReportPhotoUris.map { it.toString() }) { result ->
        result.onSuccess {
          loadReports(); recyclerView.scrollToPosition(0)
          UiMessageHelper.showShort(requireContext(), "现场事件已上传，指挥端可审核转工单")
          dialog.dismiss()
        }.onFailure { UiMessageHelper.showShort(requireContext(), "上报失败：${it.message}") }
      }
    }

    dialog.show()
  }

  private fun updateReportPhotoUi() {
    reportPhotoCountView?.text = "已选择 $reportPhotoCount 张照片"
    renderPhotoThumbs(reportPhotoContainer, selectedReportPhotoUris)
  }

  private fun renderPhotoThumbs(container: LinearLayout?, uris: List<Uri>) {
    container ?: return
    container.removeAllViews()
    uris.forEach { uri ->
      val imageView = ImageView(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(72.dp(), 72.dp()).also {
          it.marginEnd = 8.dp()
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        setImageURI(uri)
      }
      container.addView(imageView)
    }
  }

  private fun Int.dp(): Int {
    return (this * resources.displayMetrics.density).toInt()
  }

  private fun launchCamera() {
    val uri = createPhotoUri()
    pendingPhotoUri = uri
    takePictureLauncher.launch(uri)
  }

  private fun createPhotoUri(): Uri {
    val photoDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
      ?: requireContext().filesDir
    if (!photoDir.exists()) photoDir.mkdirs()
    val photoFile = File.createTempFile("traffic_event_${System.currentTimeMillis()}_", ".jpg", photoDir)
    return FileProvider.getUriForFile(
      requireContext(),
      "${BuildConfig.APPLICATION_ID}.fileprovider",
      photoFile,
    )
  }
}
