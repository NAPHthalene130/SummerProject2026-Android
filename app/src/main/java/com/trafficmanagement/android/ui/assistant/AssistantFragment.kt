package com.trafficmanagement.android.ui.assistant

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.trafficmanagement.android.R

class AssistantFragment : Fragment(R.layout.fragment_assistant) {
  companion object {
    private const val ARG_INITIAL_QUERY = "initial_query"

    fun newInstance(initialQuery: String? = null): AssistantFragment {
      return AssistantFragment().apply {
        arguments = Bundle().apply {
          if (!initialQuery.isNullOrBlank()) {
            putString(ARG_INITIAL_QUERY, initialQuery)
          }
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val input = view.findViewById<EditText>(R.id.etAssistantInput)
    val output = view.findViewById<TextView>(R.id.tvAssistantResult)
    val cardResult = view.findViewById<MaterialCardView>(R.id.cardResult)

    fun ask(query: String) {
      input.setText(query)
      output.text = getAnswer(query)
      cardResult.visibility = View.VISIBLE
    }

    view.findViewById<MaterialButton>(R.id.btnAskAssistant).setOnClickListener {
      val query = input.text.toString().trim()
      if (query.isNotBlank()) ask(query)
    }

    view.findViewById<MaterialButton>(R.id.btnQuickAc).setOnClickListener {
      ask("追尾事故现场处置建议")
    }
    view.findViewById<MaterialButton>(R.id.btnQuickPower).setOnClickListener {
      ask("超速违章如何复核")
    }
    view.findViewById<MaterialButton>(R.id.btnQuickLight).setOnClickListener {
      ask("违规停车如何处理")
    }

    arguments?.getString(ARG_INITIAL_QUERY)?.takeIf { it.isNotBlank() }?.let { query ->
      input.setText(query)
    }
  }

  private fun getAnswer(query: String): String {
    return when {
      query.contains("追尾") || query.contains("撞车") || query.contains("事故") -> """
        【事件判断】
        优先按交通事故处置。先确认人员安全，再确认是否占用主干道和是否需要临时交通管制。

        【处置建议】
        1. 到场后开启警示灯，设置警示锥桶。
        2. 拍摄事故全景、车辆位置、车牌、碰撞点和路面痕迹。
        3. 如有伤员，先联动 120 和事故中队。
        4. 在移动端填写处理报告并上传照片。
        5. 指挥端确认后关闭工单，视频回放保留为证据。
      """.trimIndent()

      query.contains("超速") || query.contains("速度") -> """
        【事件判断】
        超速事件需要复核 YOLO 速度估计、道路限速、电子警察记录和视频时间戳。

        【处置建议】
        1. 核对路段限速和天气条件。
        2. 查看车辆轨迹和至少 5 秒连续帧。
        3. 与雷达或电子警察测速记录交叉校验。
        4. 证据一致时转违章工单，证据不足时标记为需人工复核。
      """.trimIndent()

      query.contains("违停") || query.contains("违规停车") || query.contains("停车") -> """
        【事件判断】
        违规停车应关注停留时长、禁停区域、是否遮挡车道/人行道/校门消防通道。

        【处置建议】
        1. 现场确认车辆位置和是否影响通行。
        2. 拍摄处理前照片和周边标志标线。
        3. 可联系驾驶员驶离；拒不驶离时转执法流程。
        4. 处理完成后上传处理报告和处理后照片。
      """.trimIndent()

      else -> """
        【智能体提示】
        请描述事件类型、路段、摄像头、风险评分或现场现象。

        支持问题示例：
        - 追尾事故现场处置建议
        - 超速违章如何复核
        - 违规停车如何处理
      """.trimIndent()
    }
  }
}
