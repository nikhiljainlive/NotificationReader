package com.nikhiljain.notificationreader.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.nikhiljain.notificationreader.R
import com.nikhiljain.notificationreader.util.isNotificationPermissionEnabled
import com.nikhiljain.notificationreader.util.isNotificationServiceEnabled

class MainActivity : AppCompatActivity() {
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
                as NavHostFragment
    }

    override fun onResume() {
        super.onResume()
        if (!isNotificationServiceEnabled || !isNotificationPermissionEnabled) {
            navHostFragment.findNavController().navigate(R.id.fragment_permissions)
        }
    }
}