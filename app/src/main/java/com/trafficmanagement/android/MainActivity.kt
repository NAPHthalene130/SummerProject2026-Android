package com.trafficmanagement.android

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.Fragment
import com.trafficmanagement.android.data.remote.ApiEndpointManager
import com.trafficmanagement.android.ui.alerts.AlertsFragment
import com.trafficmanagement.android.ui.assistant.AssistantFragment
import com.trafficmanagement.android.ui.device.DeviceDetailFragment
import com.trafficmanagement.android.ui.home.HomeFragment
import com.trafficmanagement.android.ui.inspection.InspectionFragment
import com.trafficmanagement.android.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
  private lateinit var bottomNavigationView: BottomNavigationView
  private lateinit var bottomNavCard: View

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

