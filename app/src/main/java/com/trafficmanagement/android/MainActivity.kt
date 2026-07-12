package com.trafficmanagement.android

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.Fragment
import com.trafficmanagement.android.data.remote.ApiEndpointManager
import com.trafficmanagement.android.data.remote.WorkOrderApi
import com.trafficmanagement.android.ui.alerts.AlertsFragment
import com.trafficmanagement.android.ui.assistant.AssistantFragment
import com.trafficmanagement.android.ui.device.DeviceDetailFragment
import com.trafficmanagement.android.ui.home.HomeFragment
import com.trafficmanagement.android.ui.inspection.InspectionFragment
import com.trafficmanagement.android.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.trafficmanagement.android.utils.AuthManager

class MainActivity : AppCompatActivity() {
  private lateinit var bottomNavigationView: BottomNavigationView
  private lateinit var bottomNavCard: View
  private var accountValidationInFlight = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ApiEndpointManager.initialize(applicationContext)
    setContentView(R.layout.activity_main)

    bottomNavigationView = findViewById(R.id.bottomNav)
    bottomNavCard = findViewById(R.id.bottomNavCard)

    supportFragmentManager.addOnBackStackChangedListener {
      updateBottomNavVisibility()
    }

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
          supportFragmentManager.popBackStack()
        } else {
          finish()
        }
      }
    })

    bottomNavigationView.setOnItemSelectedListener { item ->
      clearDetailStack()
      when (item.itemId) {
        R.id.nav_home -> {
          openRootFragment(HomeFragment())
          true
        }
        R.id.nav_alerts -> {
          openRootFragment(AlertsFragment())
          true
        }
        R.id.nav_inspection -> {
          openRootFragment(InspectionFragment())
          true
        }
        R.id.nav_assistant -> {
          openRootFragment(AssistantFragment.newInstance())
          true
        }
        R.id.nav_profile -> {
          openRootFragment(ProfileFragment())
          true
        }
        else -> false
      }
    }

    if (savedInstanceState == null) {
      bottomNavigationView.selectedItemId = R.id.nav_home
    }
  }

  override fun onStart() {
    super.onStart()
    validateCurrentAccount()
  }

  private fun validateCurrentAccount() {
    if (accountValidationInFlight || !AuthManager.isLoggedIn(this)) return
    val profile = AuthManager.getCurrentProfile(this) ?: return
    val password = AuthManager.getSavedPassword(this)
    if (profile.phone.isBlank() || password.isBlank()) return
    accountValidationInFlight = true
    WorkOrderApi.loginUser(profile.phone, password) { result ->
      accountValidationInFlight = false
      result.onSuccess { json ->
        AuthManager.saveRemoteAccount(
          this,
          json.getInt("user_id"),
          json.optString("name"),
          profile.phone,
          password,
          json.optString("role_name"),
          json.optString("personnel_category"),
          json.optString("site"),
        )
      }.onFailure { error ->
        val message = error.message.orEmpty()
        val accountInvalid = message.contains("账号不存在") || message.contains("已被删除") || message.contains("手机号或密码错误")
        if (!accountInvalid) return@onFailure
        AuthManager.logout(this)
        bottomNavigationView.selectedItemId = R.id.nav_profile
        AlertDialog.Builder(this)
          .setTitle("登录已失效")
          .setMessage("该账号已被管理端删除或登录密码已变更，请重新登录。")
          .setPositiveButton("重新登录", null)
          .setCancelable(false)
          .show()
      }
    }
  }

  private fun updateBottomNavVisibility() {
    if (supportFragmentManager.backStackEntryCount == 0) {
      bottomNavCard.visibility = View.VISIBLE
    } else {
      bottomNavCard.visibility = View.GONE
    }
  }

  fun selectTab(@IdRes itemId: Int) {
    bottomNavigationView.selectedItemId = itemId
  }

  fun openDeviceDetail(deviceId: String) {
    openDetailFragment(DeviceDetailFragment.newInstance(deviceId))
  }

  fun openAssistant(query: String) {
    openDetailFragment(AssistantFragment.newInstance(query))
  }

  private fun openRootFragment(fragment: Fragment) {
    supportFragmentManager
      .beginTransaction()
      .replace(R.id.fragmentContainer, fragment)
      .commit()
  }

  private fun openDetailFragment(fragment: Fragment) {
    supportFragmentManager
      .beginTransaction()
      .replace(R.id.fragmentContainer, fragment)
      .addToBackStack(null)
      .commit()
  }

  private fun clearDetailStack() {
    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
  }
}
