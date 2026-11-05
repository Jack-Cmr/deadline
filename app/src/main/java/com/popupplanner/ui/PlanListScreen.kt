package com.popupplanner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.popupplanner.data.Plan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DeadlineFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日 EEE HH:mm", Locale.CHINA)
private val CardShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    plans: List<Plan>,
    exactAlarmPermissionGranted: Boolean,
    notificationsEnabled: Boolean,
    fullScreenIntentGranted: Boolean,
    dndPolicyAccessGranted: Boolean,
    onAddPlan: () -> Unit,
    onDeletePlan: (Plan) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onOpenDndPolicySettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("deadline", fontWeight = FontWeight.Bold)
                        Text(
                            text = "到点，弹出来。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPlan,
                icon = {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text("新建计划") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (
                !exactAlarmPermissionGranted ||
                !notificationsEnabled ||
                !fullScreenIntentGranted ||
                !dndPolicyAccessGranted
            ) {
                ReminderPermissionBanner(
                    exactAlarmPermissionGranted = exactAlarmPermissionGranted,
                    notificationsEnabled = notificationsEnabled,
                    fullScreenIntentGranted = fullScreenIntentGranted,
                    dndPolicyAccessGranted = dndPolicyAccessGranted,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings,
                    onOpenDndPolicySettings = onOpenDndPolicySettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                )
            }

            if (plans.isEmpty()) {
                EmptyPlans(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = plans, key = { plan -> plan.id }) { plan ->
                        SwipeablePlanCard(
                            plan = plan,
                            onDelete = { onDeletePlan(plan) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePlanCard(
    plan: Plan,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val deleting = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardShape)
                    .background(
                        if (deleting) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (deleting) {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        PlanCard(plan = plan, onDelete = onDelete)
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDeadline(plan.triggerAt),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (plan.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = plan.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun ReminderPermissionBanner(
    exactAlarmPermissionGranted: Boolean,
    notificationsEnabled: Boolean,
    fullScreenIntentGranted: Boolean,
    dndPolicyAccessGranted: Boolean,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onOpenDndPolicySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp)
        ) {
            Text(
                text = "提醒权限需要开启",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "缺少以下权限时，deadline 可能无法在到点后全屏提醒你。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(6.dp))

            val missingCount = listOf(
                !exactAlarmPermissionGranted,
                !notificationsEnabled,
                !fullScreenIntentGranted,
                !dndPolicyAccessGranted
            ).count { it }
            val nextPermission = when {
                !exactAlarmPermissionGranted ->
                    "精确闹钟权限未开启" to onOpenExactAlarmSettings

                !notificationsEnabled ->
                    "通知权限被关闭" to onOpenNotificationSettings

                !fullScreenIntentGranted ->
                    "全屏通知权限受限" to onOpenFullScreenIntentSettings

                else ->
                    "勿扰模式例外未开启" to onOpenDndPolicySettings
            }
            PermissionActionRow(
                label = nextPermission.first,
                onClick = nextPermission.second
            )
            if (missingCount > 1) {
                Text(
                    text = "还有 ${missingCount - 1} 项需要开启，处理完返回后会自动更新。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun PermissionActionRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("去开启")
        }
    }
}

@Composable
private fun EmptyPlans(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "还没有计划",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "点右下角的“新建计划”，写下第一件要到点提醒你的事。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatDeadline(triggerAt: Long): String {
    return Instant.ofEpochMilli(triggerAt)
        .atZone(ZoneId.systemDefault())
        .format(DeadlineFormatter)
}
