package com.popupplanner.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.popupplanner.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Restores exact alarms after a reboot, after an app update, and as soon as the
 * user grants SCHEDULE_EXACT_ALARM. Without this, a reboot would silently wipe
 * every pending reminder.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> Unit
            else -> return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduler = AlarmScheduler(appContext)
                if (!scheduler.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact-alarm access is not granted; nothing can be restored")
                    return@launch
                }

                val plans = AppDatabase.getInstance(appContext)
                    .planDao()
                    .getFuturePlans(System.currentTimeMillis())
                plans.forEach { scheduler.schedule(it) }
                Log.i(TAG, "Restored ${plans.size} future alarm(s)")
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to restore alarms", throwable)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
