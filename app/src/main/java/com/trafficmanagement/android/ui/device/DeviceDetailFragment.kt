package com.trafficmanagement.android.ui.device

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.trafficmanagement.android.MainActivity
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.SampleRepository
import com.trafficmanagement.android.data.model.DeviceDetail
import com.trafficmanagement.android.utils.UiMessageHelper

class DeviceDetailFragment : Fragment(R.layout.fragment_device_detail) {
  companion object {
    private const val ARG_DEVICE_ID = "device_id"

    fun newInstance(deviceId: String): DeviceDetailFragment {
      return DeviceDetailFragment().apply {
        arguments = Bundle().apply {
          putString(ARG_DEVICE_ID, deviceId)
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val cameraId = arguments?.getString(ARG_DEVICE_ID)
    val device = cameraId?.let { SampleRepository.deviceDetail(it) } ?: SampleRepository.defaultDevice()

    bindDevice(view, device)

    view.findViewById<MaterialButton>(R.id.btnAskDeviceAssistant).setOnClickListener {
      (activity as? MainActivity)?.openAssistant("${device.name} 当前事件如何处置")
    }
    view.findViewById<MaterialButton>(R.id.btnCreateDeviceWorkOrder).setOnClickListener {
      UiMessageHelper.showShort(requireContext(), "${device.name} 已生成交通处置工单")
    }
  }

  private fun bindDevice(view: View, device: DeviceDetail) {
    view.findViewById<TextView>(R.id.tvDeviceName).text = device.name
    view.findViewById<TextView>(R.id.tvDeviceType).text = device.type
    view.findViewById<TextView>(R.id.tvDeviceLocation).text = device.location
    view.findViewById<TextView>(R.id.tvDeviceStatus).apply {
      text = device.status
      when {
        device.status.contains("高") -> {
          setBackgroundResource(R.drawable.bg_badge_high)
          setTextColor(ContextCompat.getColor(context, R.color.badge_high_text))
        }
        device.status.contains("中") || device.status.contains("待") -> {
          setBackgroundResource(R.drawable.bg_badge_medium)
          setTextColor(ContextCompat.getColor(context, R.color.badge_medium_text))
        }
        else -> {
          setBackgroundResource(R.drawable.bg_badge_low)
          setTextColor(ContextCompat.getColor(context, R.color.badge_low_text))
        }
      }
    }

    view.findViewById<TextView>(R.id.tvDevicePower).text = String.format("%.1f km/h", device.averageSpeedKmh)
    view.findViewById<TextView>(R.id.tvDeviceToday).text = "${device.trafficFlowPerHour} 辆/h"
    view.findViewById<TextView>(R.id.tvDeviceMonth).text = "${device.riskScore} 分"
    view.findViewById<TextView>(R.id.tvDeviceNote).text = device.operatingNote
    view.findViewById<TextView>(R.id.tvDeviceLatestAlert).text = "最新事件 · ${device.latestAlert}"
    view.findViewById<TextView>(R.id.tvDeviceLastInspection).text = "最近核查 · ${device.lastInspection}"
    view.findViewById<TextView>(R.id.tvDeviceAction).text = device.recommendedAction
  }
}
