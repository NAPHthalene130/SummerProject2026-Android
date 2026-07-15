package com.trafficmanagement.android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.trafficmanagement.android.R
import com.trafficmanagement.android.data.remote.ApiEndpointManager
import com.trafficmanagement.android.data.remote.WorkOrderApi
import com.trafficmanagement.android.utils.AuthManager
import com.trafficmanagement.android.utils.OperatorProfile
import com.trafficmanagement.android.utils.UiMessageHelper

class ProfileFragment : Fragment(R.layout.fragment_profile) {
  private enum class AuthMode {
    LOGIN,
    REGISTER,
  }

  private lateinit var authCard: View
  private lateinit var profileCard: View
  private lateinit var loggedInActionGroup: LinearLayout
  private lateinit var authModeGroup: MaterialButtonToggleGroup
  private lateinit var tilRegisterName: TextInputLayout
  private lateinit var tilConfirmPassword: TextInputLayout
  private lateinit var etRegisterName: EditText
  private lateinit var etPhone: EditText
  private lateinit var etPassword: EditText
  private lateinit var etConfirmPassword: EditText
  private lateinit var tvAuthHint: TextView
  private lateinit var btnAuthSubmit: MaterialButton
  private lateinit var tvUserName: TextView
  private lateinit var tvUserRole: TextView
  private lateinit var tvUserPhone: TextView
  private lateinit var tvUserSite: TextView
  private lateinit var tvAvatarInitial: TextView

  private var authMode = AuthMode.LOGIN

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bindViews(view)
    bindActions(view)
    authModeGroup.check(R.id.btnModeLogin)
    renderAuthMode()
    refreshUi()
  }

  private fun bindViews(view: View) {
    authCard = view.findViewById(R.id.authCard)
    profileCard = view.findViewById(R.id.profileCard)
    loggedInActionGroup = view.findViewById(R.id.loggedInActionGroup)
    authModeGroup = view.findViewById(R.id.authModeGroup)
    tilRegisterName = view.findViewById(R.id.tilRegisterName)
    tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword)
    etRegisterName = view.findViewById(R.id.etRegisterName)
    etPhone = view.findViewById(R.id.etPhone)
    etPassword = view.findViewById(R.id.etPassword)
    etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
    tvAuthHint = view.findViewById(R.id.tvAuthHint)
    btnAuthSubmit = view.findViewById(R.id.btnAuthSubmit)
    tvUserName = view.findViewById(R.id.tvUserName)
    tvUserRole = view.findViewById(R.id.tvUserRole)
    tvUserPhone = view.findViewById(R.id.tvUserPhone)
    tvUserSite = view.findViewById(R.id.tvUserSite)
    tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial)
  }

  private fun bindActions(view: View) {
    authModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      authMode = if (checkedId == R.id.btnModeRegister) AuthMode.REGISTER else AuthMode.LOGIN
      renderAuthMode()
    }

    btnAuthSubmit.setOnClickListener {
      if (authMode == AuthMode.LOGIN) handleLogin() else handleRegister()
    }

    view.findViewById<MaterialButton>(R.id.btnServerSettings).setOnClickListener {
      showCloudApiDialog()
    }

    view.findViewById<MaterialButton>(R.id.btnSyncData).setOnClickListener {
      UiMessageHelper.showShort(requireContext(), "今日工单与事件数据已同步")
    }
    view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
      AuthManager.logout(requireContext())
      clearInputs()
      refreshUi()
      UiMessageHelper.showShort(requireContext(), "已退出登录")
    }
    view.findViewById<MaterialButton>(R.id.btnAbout).setOnClickListener {
      showSystemInfoDialog()
    }
  }

  private fun handleLogin() {
    val phone = etPhone.text.toString().trim()
    val password = etPassword.text.toString().trim()

    if (phone.isEmpty() || password.isEmpty()) {
      UiMessageHelper.showShort(requireContext(), "请输入手机号和密码")
      return
    }
    if (!isValidPhone(phone)) {
      UiMessageHelper.showShort(requireContext(), "手机号必须是 11 位数字")
      return
    }
    btnAuthSubmit.isEnabled = false
    WorkOrderApi.loginUser(phone, password) { result ->
      if (!isAdded) return@loginUser
      btnAuthSubmit.isEnabled = true
      result.onSuccess { json ->
        AuthManager.saveRemoteAccount(
          requireContext(), json.getInt("user_id"), json.optString("name"), phone, password,
          json.optString("role_name"), json.optString("personnel_category"), json.optString("site"),
        )
        clearInputs(); refreshUi(); UiMessageHelper.showShort(requireContext(), "登录成功")
      }.onFailure { UiMessageHelper.showShort(requireContext(), "登录失败：${it.message}") }
    }
  }

  private fun handleRegister() {
    val name = etRegisterName.text.toString().trim()
    val phone = etPhone.text.toString().trim()
    val password = etPassword.text.toString().trim()
    val confirmPassword = etConfirmPassword.text.toString().trim()

    if (name.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
      UiMessageHelper.showShort(requireContext(), "请完整填写注册信息")
      return
    }
    if (!isValidPhone(phone)) {
      UiMessageHelper.showShort(requireContext(), "手机号必须是 11 位数字")
      return
    }
    if (password != confirmPassword) {
      UiMessageHelper.showShort(requireContext(), "两次输入的密码不一致")
      return
    }

    val names = arrayOf("交警执法", "道路养护", "市政设施", "清障救援", "交通疏导", "应急消防")
    val codes = arrayOf("traffic_police", "road_maintenance", "municipal_facilities", "vehicle_rescue", "traffic_coordination", "emergency_fire")
    AlertDialog.Builder(requireContext()).setTitle("选择职责类别").setItems(names) { _, index ->
      btnAuthSubmit.isEnabled = false
      WorkOrderApi.registerUser(name, phone, password, codes[index]) { result ->
        if (!isAdded) return@registerUser
        btnAuthSubmit.isEnabled = true
        result.onSuccess { json ->
          AuthManager.saveRemoteAccount(requireContext(), json.getInt("user_id"), name, phone, password,
            json.optString("role_name", names[index]), codes[index], json.optString("site"))
          clearInputs(); refreshUi(); UiMessageHelper.showShort(requireContext(), "注册成功：${names[index]}")
        }.onFailure { UiMessageHelper.showShort(requireContext(), "注册失败：${it.message}") }
      }
    }.show()
  }

  private fun showCloudApiDialog() {
    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cloud_api_settings, null)
    val modeGroup = dialogView.findViewById<RadioGroup>(R.id.rgConnectionMode)
    val cloudUrlInput = dialogView.findViewById<TextInputEditText>(R.id.etCloudApiUrl)
    val localUrlInput = dialogView.findViewById<TextInputEditText>(R.id.etLocalApiUrl)
    val statusView = dialogView.findViewById<TextView>(R.id.tvCloudApiStatus)

    fun renderSettings() {
      cloudUrlInput.setText(ApiEndpointManager.cloudBaseUrl())
      localUrlInput.setText(ApiEndpointManager.localBaseUrl())
      modeGroup.check(
        if (ApiEndpointManager.connectionMode() == ApiEndpointManager.ConnectionMode.CLOUD) {
          R.id.rbCloudConnection
        } else {
          R.id.rbLocalConnection
        },
      )
      statusView.text = "当前使用：${ApiEndpointManager.baseUrl()}"
    }

    renderSettings()
    modeGroup.setOnCheckedChangeListener { _, checkedId ->
      val selectedUrl = if (checkedId == R.id.rbCloudConnection) {
        cloudUrlInput.text?.toString().orEmpty()
      } else {
        localUrlInput.text?.toString().orEmpty()
      }
      statusView.text = "切换后将使用：${selectedUrl.ifBlank { "请填写服务器地址" }}"
    }

    val dialog = AlertDialog.Builder(requireContext())
      .setView(dialogView)
      .setNegativeButton("取消", null)
      .setNeutralButton("恢复默认", null)
      .setPositiveButton("测试并保存", null)
      .create()

    dialog.setOnShowListener {
      dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
        ApiEndpointManager.reset()
        renderSettings()
        statusView.text = "已恢复默认配置，当前使用云端服务器。"
      }
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        cloudUrlInput.error = null
        localUrlInput.error = null
        val mode = if (modeGroup.checkedRadioButtonId == R.id.rbLocalConnection) {
          ApiEndpointManager.ConnectionMode.LOCAL
        } else {
          ApiEndpointManager.ConnectionMode.CLOUD
        }
        val saveResult = ApiEndpointManager.saveSettings(
          mode = mode,
          cloudUrl = cloudUrlInput.text?.toString().orEmpty(),
          localUrl = localUrlInput.text?.toString().orEmpty(),
        )
        saveResult.onFailure {
          statusView.text = "配置错误：${it.message}"
          return@setOnClickListener
        }

        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.isEnabled = false
        statusView.text = "正在连接 ${ApiEndpointManager.baseUrl()} ..."
        WorkOrderApi.ping { result ->
          if (!isAdded) return@ping
          button.isEnabled = true
          result.onSuccess {
            val modeName = if (mode == ApiEndpointManager.ConnectionMode.CLOUD) "云端" else "本地"
            UiMessageHelper.showShort(requireContext(), "${modeName}服务器连接成功")
            dialog.dismiss()
          }.onFailure {
            statusView.text = "连接失败：${it.message ?: "请检查服务器地址、网络和后端状态"}"
          }
        }
      }
    }
    dialog.show()
  }
  private fun showSystemInfoDialog() {
    AlertDialog.Builder(requireContext())
      .setTitle("系统信息")
      .setMessage(
        """
        系统名称：交通事故与违章事件管理系统

        服务端职责：
        接入 RTSP 视频流，使用 YOLO 识别车流量、车速和车辆轨迹；结合天气、历史事故数量等因素预测路段事故风险；将抽帧视频送入多模态模型判断事故和违章事件。

        管理端职责：
        展示路段风险/车流量地图、摄像头实时画面和风险报表；由智能体生成处理意见并包装成工单；跟踪移动端反馈。

        移动端职责：
        接收工单，查看事件说明和视频回放地址，到场后提交处理状态、处理报告和现场照片。
        """.trimIndent(),
      )
      .setPositiveButton("知道了", null)
      .show()
  }

  private fun refreshUi() {
    val loggedIn = AuthManager.isLoggedIn(requireContext())
    authCard.isVisible = !loggedIn
    profileCard.isVisible = loggedIn
    loggedInActionGroup.isVisible = loggedIn

    if (loggedIn) {
      bindProfile(AuthManager.getCurrentProfile(requireContext()))
    } else {
      authModeGroup.check(R.id.btnModeLogin)
      authMode = AuthMode.LOGIN
      renderAuthMode()
    }
  }

  private fun bindProfile(profile: OperatorProfile?) {
    if (profile == null) return
    tvUserName.text = profile.name
    tvUserRole.text = profile.role
    tvUserPhone.text = "手机号 · ${profile.phone}"
    tvUserSite.text = profile.currentSite
    tvAvatarInitial.text = profile.name.firstOrNull()?.toString() ?: "交"
  }

  private fun renderAuthMode() {
    val registerMode = authMode == AuthMode.REGISTER
    tilRegisterName.isVisible = registerMode
    tilConfirmPassword.isVisible = registerMode
    btnAuthSubmit.text = if (registerMode) "注册并登录" else "登录"
    tvAuthHint.text =
      if (registerMode) {
        "注册后将默认进入交通处置移动端账号。"
      } else {
        "使用已注册账号登录。"
      }
  }

  private fun clearInputs() {
    etRegisterName.text?.clear()
    etPhone.text?.clear()
    etPassword.text?.clear()
    etConfirmPassword.text?.clear()
  }

  private fun isValidPhone(phone: String): Boolean {
    return phone.length == 11 && phone.all(Char::isDigit)
  }
}
