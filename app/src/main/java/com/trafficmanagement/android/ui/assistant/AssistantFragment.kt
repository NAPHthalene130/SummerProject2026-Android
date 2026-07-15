package com.trafficmanagement.android.ui.assistant

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.remote.WorkOrderApi
import io.noties.markwon.Markwon

class AssistantFragment : Fragment(R.layout.fragment_assistant) {
  companion object {
    private const val ARG_INITIAL_QUERY = "initial_query"
    private const val STATE_THREAD_ID = "assistant_thread_id"

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

  private var threadId: String? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    threadId = savedInstanceState?.getString(STATE_THREAD_ID)

    val input = view.findViewById<EditText>(R.id.etAssistantInput)
    val output = view.findViewById<TextView>(R.id.tvAssistantResult)
    val markwon = Markwon.create(requireContext())
    output.movementMethod = LinkMovementMethod.getInstance()
    val cardResult = view.findViewById<MaterialCardView>(R.id.cardResult)
    val askButton = view.findViewById<MaterialButton>(R.id.btnAskAssistant)
    val quickAccidentButton = view.findViewById<MaterialButton>(R.id.btnQuickAc)
    val quickSpeedButton = view.findViewById<MaterialButton>(R.id.btnQuickPower)
    val quickParkingButton = view.findViewById<MaterialButton>(R.id.btnQuickLight)
    val actionButtons = listOf(
      askButton,
      quickAccidentButton,
      quickSpeedButton,
      quickParkingButton,
    )

    fun ask(query: String) {
      input.setText(query)
      actionButtons.forEach { it.isEnabled = false }
      askButton.text = "正在生成建议…"
      output.text = "正在查询工单与交通规定知识库，请稍候…"
      cardResult.visibility = View.VISIBLE

      WorkOrderApi.askAssistant(query, threadId) { result ->
        if (this.view !== view) return@askAssistant
        actionButtons.forEach { it.isEnabled = true }
        askButton.text = "生成建议"
        result.onSuccess { response ->
          threadId = response.threadId.takeIf { it.isNotBlank() }
          markwon.setMarkdown(output, response.reply)
        }.onFailure { error ->
          output.text = "助手服务暂时不可用：${error.message ?: "网络连接异常"}\n请检查后端地址或稍后重试。"
        }
      }
    }

    askButton.setOnClickListener {
      val query = input.text.toString().trim()
      if (query.isNotBlank()) ask(query)
    }

    quickAccidentButton.setOnClickListener {
      ask("追尾事故现场处置建议")
    }
    quickSpeedButton.setOnClickListener {
      ask("超速违章如何复核")
    }
    quickParkingButton.setOnClickListener {
      ask("违规停车如何处理")
    }

    arguments?.getString(ARG_INITIAL_QUERY)?.takeIf { it.isNotBlank() }?.let { query ->
      input.setText(query)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    threadId?.let { outState.putString(STATE_THREAD_ID, it) }
  }
}
