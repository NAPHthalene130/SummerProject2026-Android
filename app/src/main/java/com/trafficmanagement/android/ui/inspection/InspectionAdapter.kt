package com.trafficmanagement.android.ui.inspection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.model.WorkOrderItem

class InspectionAdapter(private val onTaskClick: (WorkOrderItem) -> Unit) : RecyclerView.Adapter<InspectionAdapter.TaskViewHolder>() {
  private val items = mutableListOf<WorkOrderItem>()

  fun submitItems(newItems: List<WorkOrderItem>) {
    items.clear()
    items.addAll(newItems)
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder =
    TaskViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inspection_task, parent, false))

  override fun onBindViewHolder(holder: TaskViewHolder, position: Int) = holder.bind(items[position], onTaskClick)
  override fun getItemCount(): Int = items.size

  class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val cardView = itemView as MaterialCardView
    private val tvCode: TextView = itemView.findViewById(R.id.tvTaskCode)
    private val tvLevel: TextView = itemView.findViewById(R.id.tvTaskLevel)
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
    private val tvZone: TextView = itemView.findViewById(R.id.tvTaskZone)
    private val tvDue: TextView = itemView.findViewById(R.id.tvTaskDue)
    private val tvStatus: TextView = itemView.findViewById(R.id.tvTaskStatus)
    private val tvAssignee: TextView = itemView.findViewById(R.id.tvTaskAssignee)
    private val btnDetail: MaterialButton = itemView.findViewById(R.id.btnTaskDetail)

    fun bind(item: WorkOrderItem, onTaskClick: (WorkOrderItem) -> Unit) {
      tvCode.text = item.workOrderId
      tvTitle.text = item.accidentInfo.ifBlank { "未命名事件" }
      tvZone.text = item.monitorAddress.ifBlank { item.segmentName }
      tvDue.text = item.eventTime
      tvStatus.text = statusText(item.status)
      tvAssignee.text = buildString {
        append("处置人员：${item.assignee ?: "暂未派发"}")
        when (item.feedbackReviewStatus) {
          "pending" -> append("\n处置反馈：等待电脑端审核")
          "approved" -> append("\n处置反馈：审核通过")
          "rejected" -> append("\n处置反馈：已退回${item.feedbackReviewMessage?.let { "（$it）" } ?: ""}")
        }
      }
      tvLevel.text = levelText(item.eventLevel)

      val levelBackground = when (item.eventLevel) {
        "high" -> R.drawable.bg_badge_high
        "medium" -> R.drawable.bg_badge_medium
        else -> R.drawable.bg_badge_low
      }
      val levelColor = when (item.eventLevel) {
        "high" -> R.color.badge_high_text
        "medium" -> R.color.badge_medium_text
        else -> R.color.badge_low_text
      }
      tvLevel.setBackgroundResource(levelBackground)
      tvLevel.setTextColor(ContextCompat.getColor(itemView.context, levelColor))

      val statusBackground = when (item.status) {
        "completed" -> R.drawable.bg_badge_low
        "ignored", "false_alarm" -> R.drawable.bg_badge_medium
        "processing" -> R.drawable.bg_badge_high
        else -> R.drawable.bg_soft_panel
      }
      val statusColor = when (item.status) {
        "completed" -> R.color.badge_low_text
        "ignored", "false_alarm" -> R.color.badge_medium_text
        "processing" -> R.color.badge_high_text
        else -> R.color.brand_blue_text
      }
      tvStatus.setBackgroundResource(statusBackground)
      tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))
      cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.card_stroke)
      btnDetail.text = if (item.isResolved) "查看处置记录" else "查看并处置"
      itemView.setOnClickListener { onTaskClick(item) }
      btnDetail.setOnClickListener { onTaskClick(item) }
    }
  }

  companion object {
    fun statusText(status: String): String = when (status) {
      "unassigned" -> "待派发"
      "pending" -> "待处理"
      "processing" -> "处理中"
      "completed" -> "已完成"
      "ignored" -> "已忽略"
      "false_alarm" -> "误报关闭"
      else -> "未知"
    }

    fun levelText(level: String): String = when (level) {
      "high" -> "高风险"
      "medium" -> "中风险"
      else -> "低风险"
    }
  }
}
