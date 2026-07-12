package com.trafficmanagement.android.ui.alerts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.model.AlertItem
import com.trafficmanagement.android.data.model.ReportSyncStatus
import com.trafficmanagement.android.data.model.Severity

class AlertAdapter(
  private val items: List<AlertItem>,
  private val onResendClick: (AlertItem) -> Unit,
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
    return AlertViewHolder(view)
  }

  override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
    holder.bind(items[position], onResendClick)
  }

  override fun getItemCount(): Int = items.size

  class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val cardView: MaterialCardView = itemView as MaterialCardView
    private val tvTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
    private val tvLocation: TextView = itemView.findViewById(R.id.tvAlertLocation)
    private val tvDetail: TextView = itemView.findViewById(R.id.tvAlertDetail)
    private val tvMeta: TextView = itemView.findViewById(R.id.tvAlertSuggestion)
    private val tvSeverity: TextView = itemView.findViewById(R.id.tvAlertSeverity)
    private val photoContainer: LinearLayout = itemView.findViewById(R.id.layoutAlertPhotos)
    private val btnResend: MaterialButton = itemView.findViewById(R.id.btnAlertWorkOrder)

    fun bind(item: AlertItem, onResendClick: (AlertItem) -> Unit) {
      tvTitle.text = item.title
      tvLocation.text = "${item.location}  ·  ${item.submittedAt}"
      tvDetail.text = item.detail
      tvMeta.text = "来源：${item.reporter}\n照片：${item.photoCount} 张\n指挥端状态：${syncStatusText(item.syncStatus)}"
      renderPhotoThumbs(item.photoUris)

      when (item.severity) {
        Severity.HIGH -> {
          tvSeverity.text = "紧急"
          tvSeverity.setBackgroundResource(R.drawable.bg_badge_high)
          tvSeverity.setTextColor(ContextCompat.getColor(itemView.context, R.color.badge_high_text))
          cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.badge_high_bg)
        }
        Severity.MEDIUM -> {
          tvSeverity.text = "重要"
          tvSeverity.setBackgroundResource(R.drawable.bg_badge_medium)
          tvSeverity.setTextColor(ContextCompat.getColor(itemView.context, R.color.badge_medium_text))
          cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.badge_medium_bg)
        }
        Severity.LOW -> {
          tvSeverity.text = "一般"
          tvSeverity.setBackgroundResource(R.drawable.bg_badge_low)
          tvSeverity.setTextColor(ContextCompat.getColor(itemView.context, R.color.badge_low_text))
          cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.badge_low_bg)
        }
      }

      val waitingUpload = item.syncStatus == ReportSyncStatus.WAITING_UPLOAD
      btnResend.text = if (waitingUpload) "上传指挥端" else "已同步"
      btnResend.isEnabled = waitingUpload
      btnResend.setOnClickListener { if (waitingUpload) onResendClick(item) }
    }

    private fun syncStatusText(status: ReportSyncStatus): String {
      return when (status) {
        ReportSyncStatus.WAITING_UPLOAD -> "待上传"
        ReportSyncStatus.SENT_TO_COMMAND_CENTER -> "已推送待研判"
        ReportSyncStatus.ACCEPTED -> "指挥端已接收"
      }
    }

    private fun renderPhotoThumbs(photoUris: List<String>) {
      photoContainer.removeAllViews()
      photoContainer.visibility = if (photoUris.isEmpty()) View.GONE else View.VISIBLE
      photoUris.forEach { uri ->
        val imageView = ImageView(itemView.context).apply {
          layoutParams = LinearLayout.LayoutParams(72.dp(), 72.dp()).also {
            it.marginEnd = 8.dp()
          }
          scaleType = ImageView.ScaleType.CENTER_CROP
          setImageURI(android.net.Uri.parse(uri))
        }
        photoContainer.addView(imageView)
      }
    }

    private fun Int.dp(): Int {
      return (this * itemView.resources.displayMetrics.density).toInt()
    }
  }
}
