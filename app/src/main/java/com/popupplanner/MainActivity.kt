package com.popupplanner

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.popupplanner.alarm.AlarmReceiver
import com.popupplanner.alarm.AlarmScheduler
import com.popupplanner.alarm.ScheduleResult
import com.popupplanner.data.AppDatabase
import com.popupplanner.data.Plan
import com.popupplanner.data.PlanDao
import com.popupplanner.theme.DeadlineTheme
import com.popupplanner.ui.AddPlanScreen
import com.popupplanner.ui.PlanListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var planDao: PlanDao
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AlarmReceiver.ensureNotificationChannel(this)
        planDao = AppDatabase.getInstance(applicationContext).planDao()
        alarmScheduler = AlarmScheduler(applicationContext)

        setContent {
            DeadlineTheme {
                PopupPlannerApp(
                    planDao = planDao,
                    alarmScheduler = alarmScheduler
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-submit the channel so setBypassDnd takes effect after the user grants
        // notification policy access (Android otherwise keeps the original value).
        AlarmReceiver.ensureNotificationChannel(this)
        if (!alarmScheduler.canScheduleExactAlarms()) return

        // Defensive re-registration: alarms can be dropped by an app update,
        // aggressive battery management, or a reboot before the receiver runs.
        lifecycleScope.launch {
            try {
                val plans = withContext(Dispatchers.IO) {
                    planDao.getFuturePlans(System.currentTimeMillis())
                }
                plans.forEach { plan -> alarmScheduler.schedule(plan) }
            } catch (throwable: Throwable) {
                Log.e(TAG, "Could not re-register alarms on resume", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

private fun areNotificationsEnabled(context: Context): Boolean {
    return AlarmReceiver.areReminderNotificationsEnabled(context)
}

private fun canUseFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    return context.getSystemService(NotificationManager::class.java)
        .canUseFullScreenIntent()
}

private fun hasDndPolicyAccess(context: Context): Boolean {
    return context.getSystemService(NotificationManager::class.java)
        .isNotificationPolicyAccessGranted
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchSettingsSafely(context, intent)
}

private fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchSettingsSafely(context, intent)
}

private fun openDndPolicySettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchSettingsSafely(context, intent)
}

private fun launchSettingsSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (throwable: Throwable) {
        Log.w("PopupPlannerSettings", "Could not open requested settings", throwable)
        val appDetails = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(appDetails)
        } catch (fallbackError: Throwable) {
            Log.e("PopupPlannerSettings", "Could not open app settings", fallbackError)
        }
    }
}

@Composable
private fun PopupPlannerApp(
    planDao: PlanDao,
    alarmScheduler: AlarmScheduler
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val plansFlow = remember(planDao) { planDao.observeAll() }
    val plans by plansFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddScreen by rememberSaveable { mutableStateOf(false) }
    var showExactAlarmDialog by rememberSaveable { mutableStateOf(false) }
    var exactAlarmGranted by remember { mutableStateOf(alarmScheduler.canScheduleExactAlarms()) }
    var notificationsGranted by remember {
        mutableStateOf(areNotificationsEnabled(context))
    }
    var fullScreenIntentGranted by remember {
        mutableStateOf(canUseFullScreenIntent(context))
    }
    var dndPolicyAccessGranted by remember {
        mutableStateOf(hasDndPolicyAccess(context))
    }
    var messageDialog by rememberSaveable { mutableStateOf("") }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        exactAlarmGranted = alarmScheduler.canScheduleExactAlarms()
        notificationsGranted = areNotificationsEnabled(context)
        fullScreenIntentGranted = canUseFullScreenIntent(context)
        dndPolicyAccessGranted = hasDndPolicyAccess(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // This also covers an immediate denial; the permission banner offers a
        // shortcut to the system notification settings if access is blocked.
        notificationsGranted = areNotificationsEnabled(context)
    }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = showAddScreen) {
        showAddScreen = false
    }

    if (showAddScreen) {
        AddPlanScreen(
            onBack = { showAddScreen = false },
            onSave = { title, note, triggerAt ->
                showAddScreen = false

                if (triggerAt <= System.currentTimeMillis()) {
                    messageDialog = "所选时刻已经过去，计划没有保存。"
                } else {
                    coroutineScope.launch {
                        try {
                            val planId = withContext(Dispatchers.IO) {
                                planDao.insert(
                                    Plan(
                                        title = title,
                                        note = note,
                                        triggerAt = triggerAt
                                    )
                                )
                            }
                            val savedPlan = Plan(
                                id = planId,
                                title = title,
                                note = note,
                                triggerAt = triggerAt
                            )

                            when (alarmScheduler.schedule(savedPlan)) {
                                ScheduleResult.Scheduled -> Unit
                                ScheduleResult.ExactAlarmPermissionRequired -> {
                                    exactAlarmGranted = false
                                    showExactAlarmDialog = true
                                }

                                ScheduleResult.InThePast -> {
                                    withContext(Dispatchers.IO) {
                                        planDao.delete(savedPlan)
                                    }
                                    messageDialog = "所选时刻已经过去，计划没有保存。"
                                }

                                ScheduleResult.Failed -> {
                                    messageDialog =
                                        "计划已保存，但精确闹钟注册失败。请检查系统权限后重试。"
                                }
                            }
                        } catch (throwable: Throwable) {
                            Log.e("PopupPlannerApp", "Could not save plan", throwable)
                            messageDialog = "保存失败，请稍后重试。"
                        }
                    }
                }
            }
        )
    } else {
        PlanListScreen(
            plans = plans,
            exactAlarmPermissionGranted = exactAlarmGranted,
            notificationsEnabled = notificationsGranted,
            fullScreenIntentGranted = fullScreenIntentGranted,
            dndPolicyAccessGranted = dndPolicyAccessGranted,
            onAddPlan = { showAddScreen = true },
            onDeletePlan = { plan ->
                alarmScheduler.cancel(plan)
                AlarmReceiver.cancelNotification(context, plan.id)
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            planDao.delete(plan)
                        }
                    } catch (throwable: Throwable) {
                        Log.e("PopupPlannerApp", "Could not delete plan", throwable)
                        messageDialog = "删除失败，请稍后重试。"
                    }
                }
            },
            onOpenExactAlarmSettings = {
                alarmScheduler.openExactAlarmSettings()
            },
            onOpenNotificationSettings = {
                openNotificationSettings(context)
            },
            onOpenFullScreenIntentSettings = {
                openFullScreenIntentSettings(context)
            },
            onOpenDndPolicySettings = {
                openDndPolicySettings(context)
            }
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("开启“闹钟和提醒”权限") },
            text = {
                Text(
                    "deadline 依靠精确闹钟在指定时刻全屏提醒你。请允许“闹钟和提醒”权限，" +
                        "计划才能准时触发；未授权时不会静默降级为不精确提醒。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        alarmScheduler.openExactAlarmSettings()
                    }
                ) {
                    Text("去系统设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text("稍后")
                }
            }
        )
    }

    if (messageDialog.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { messageDialog = "" },
            title = { Text("提示") },
            text = { Text(messageDialog) },
            confirmButton = {
                TextButton(onClick = { messageDialog = "" }) {
                    Text("知道了")
                }
            }
        )
    }
}
