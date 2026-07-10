package com.trafficmanagement.android.domain

object AssistantSuggestionEngine {
  fun answer(query: String): String {
    val normalized = query.trim()
    if (normalized.isEmpty()) {
      return "请输入事故、违章、路段或摄像头信息。"
    }

    return when {
      normalized.contains("追尾") || normalized.contains("事故") ->
        "优先确认人员安全，设置警示区域，拍摄事故全景和碰撞点，上传现场照片与处理报告。"
      normalized.contains("超速") ->
        "核对限速、测速证据、车辆轨迹和视频时间戳，证据一致后转违章复核。"
      normalized.contains("违停") || normalized.contains("停车") ->
        "确认禁停标志、停留时长和通行影响，劝离或转执法流程，并上传处理前后照片。"
      else ->
        "请补充事件类型、位置、风险评分和现场现象，系统会生成可执行的处置建议。"
    }
  }
}
