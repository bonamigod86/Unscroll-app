package com.unscroll.app.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.R
import com.unscroll.app.core.PermissionChecker
import com.unscroll.app.data.BlockedAppsStore
import com.unscroll.app.data.PrefsRepository
import com.unscroll.app.ui.theme.AccentDim
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.DangerRed
import com.unscroll.app.ui.theme.SurfaceRaised
import com.unscroll.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onOpenPicker: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = PrefsRepository.get(context)
    val store = BlockedAppsStore.get(context)

    val streak by prefs.streak.collectAsState()
    val interventions by prefs.interventions.collectAsState()
    val blocked by store.blockedFlow.collectAsState()
    val goalMinutes by prefs.dailyGoalMinutes.collectAsState()
    val unlocksToday by prefs.todayUnlocks.collectAsState()
    val hoursReclaimed = "%.1f h".format(prefs.estimatedHoursReclaimed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.home_greeting),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButtonSmall(stringResource(R.string.home_settings), onOpenSettings)
        }
        Spacer(Modifier.height(20.dp))

        if (!PermissionChecker.isAccessibilityServiceEnabled(context)) {
            SetupBanner(onOpenPermissions)
            Spacer(Modifier.height(16.dp))
        }

        StatCard(
            label = if (streak > 0) {
                stringResource(R.string.home_streak_days, streak)
            } else {
                stringResource(R.string.home_streak_zero)
            },
            value = streak.toString(),
            highlight = true
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCard(
                label = stringResource(R.string.home_interventions_title),
                value = interventions.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.home_hours_saved_title),
                value = hoursReclaimed,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(14.dp))

        GoalProgressCard(
            goalMinutes = goalMinutes,
            todayMinutes = prefs.todayReclaimedMinutes,
            unlocksToday = unlocksToday
        )
        Spacer(Modifier.height(14.dp))

        NavRow(
            title = stringResource(R.string.home_blocked_apps_title, blocked.size),
            onClick = onOpenPicker
        )

        if (blocked.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.home_empty_blocklist),
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalProgressCard(goalMinutes: Int, todayMinutes: Float, unlocksToday: Int) {
    val ratio = if (goalMinutes > 0) (todayMinutes / goalMinutes).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceRaised, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_goal_today_label),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.home_unlocks_today, unlocksToday),
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { ratio },
                color = AccentGreen,
                trackColor = AccentDim.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(
                    R.string.home_goal_today_value,
                    formatMinutes(todayMinutes),
                    formatMinutes(goalMinutes.toFloat())
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatMinutes(minutes: Float): String {
    val total = minutes.toInt()
    if (total < 60) return "$total min"
    val hours = total / 60
    val rest = total % 60
    return if (rest == 0) "${hours}h" else "${hours}h $rest m"
}

@Composable
private fun SetupBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DangerRed.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_finish_setup),
                color = DangerRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text("→", color = DangerRed, fontSize = 18.sp)
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(if (highlight) 140.dp else 110.dp)
            .background(
                if (highlight) AccentDim.copy(alpha = 0.35f) else SurfaceRaised,
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = if (highlight) 44.sp else 30.sp,
                fontWeight = FontWeight.Black,
                color = if (highlight) AccentGreen else MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun NavRow(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceRaised, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("→", color = AccentGreen, fontSize = 18.sp)
        }
    }
}

@Composable
private fun TextButtonSmall(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceRaised,
            contentColor = AccentGreen
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Text(text, fontSize = 13.sp)
    }
}
