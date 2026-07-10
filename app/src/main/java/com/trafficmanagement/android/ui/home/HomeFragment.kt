package com.trafficmanagement.android.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.trafficmanagement.android.MainActivity
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.SampleRepository

class HomeFragment : Fragment(R.layout.fragment_home) {
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val metrics = SampleRepository.overview()

    view.findViewById<TextView>(R.id.tvSiteName).text = metrics.siteName

    val topEvent = SampleRepository.topAlert()
    val nextTask = SampleRepository.nextPendingTask()

    view.findViewById<TextView>(R.id.tvFocusTitle).text = topEvent?.title ?: "暂无交通事件"
    view.findViewById<TextView>(R.id.tvFocusLocation).text = topEvent?.location ?: "当前路网运行平稳"
    view.findViewById<TextView>(R.id.tvFocusDetail).text =
      topEvent?.detail ?: "暂无事故或违章告警，持续监测车流量、车速、轨迹与天气风险。"

    view.findViewById<TextView>(R.id.tvPendingTaskTitle).text = nextTask?.title ?: "暂无待处理工单"
    view.findViewById<TextView>(R.id.tvPendingTaskMeta).text =
      nextTask?.let { "${it.zone}  ·  ${it.dueTime}" } ?: "移动端工单已全部处理完成"

    view.findViewById<MaterialButton>(R.id.btnFocusAssistant).setOnClickListener {
      topEvent?.title?.let { title ->
        (activity as? MainActivity)?.openAssistant("$title 应该如何处置")
      }
    }
    view.findViewById<MaterialButton>(R.id.btnPendingTask).setOnClickListener {
      (activity as? MainActivity)?.selectTab(R.id.nav_inspection)
    }
  }
}
