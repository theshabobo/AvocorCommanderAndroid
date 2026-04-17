package com.avocor.commander.kiosk

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object KioskManager {

    fun enableKiosk(activity: Activity) {
        // Keep screen on
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive sticky mode — hide system bars
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Start lock task mode (user sees a confirmation dialog unless app is device owner)
        try {
            activity.startLockTask()
        } catch (e: Exception) {
            // Not device owner — lock task may fail on some devices
            // Immersive mode still active as a basic kiosk experience
        }
    }

    fun disableKiosk(activity: Activity) {
        // Restore screen timeout
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Restore system bars
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())

        // Stop lock task
        try {
            activity.stopLockTask()
        } catch (e: Exception) {
            // Ignore — might not be in lock task mode
        }
    }
}
