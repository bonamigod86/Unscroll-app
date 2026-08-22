package com.unscroll.app.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.R
import com.unscroll.app.data.FrictionChallenge
import com.unscroll.app.data.PrefsRepository
import com.unscroll.app.ui.theme.AccentDim
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.SurfaceRaised
import com.unscroll.app.ui.theme.TextSecondary

@Composable
fun GoalSetupScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsRepository.get(context) }

    var goalMinutes by remember { mutableIntStateOf(-1) }
    var challenge by remember { mutableStateOf<FrictionChallenge?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            text = stringResource(R.string.goal_setup_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.goal_setup_sub),
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalChip(stringResource(R.string.goal_opt_30), 30, goalMinutes) { goalMinutes = it }
            GoalChip(stringResource(R.string.goal_opt_60), 60, goalMinutes) { goalMinutes = it }
            GoalChip(stringResource(R.string.goal_opt_120), 120, goalMinutes) { goalMinutes = it }
            GoalChip(stringResource(R.string.goal_opt_180), 180, goalMinutes) { goalMinutes = it }
        }

        Spacer(Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.challenge_setup_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        ChallengeCard(
            title = stringResource(R.string.challenge_opt_phrase),
            description = stringResource(R.string.challenge_opt_phrase_desc),
            selected = challenge == FrictionChallenge.PHRASE
        ) { challenge = FrictionChallenge.PHRASE }
        Spacer(Modifier.height(12.dp))
        ChallengeCard(
            title = stringResource(R.string.challenge_opt_stoic),
            description = stringResource(R.string.challenge_opt_stoic_desc),
            selected = challenge == FrictionChallenge.STOIC
        ) { challenge = FrictionChallenge.STOIC }
        Spacer(Modifier.height(12.dp))
        ChallengeCard(
            title = stringResource(R.string.challenge_opt_breath),
            description = stringResource(R.string.challenge_opt_breath_desc),
            selected = challenge == FrictionChallenge.BREATH
        ) { challenge = FrictionChallenge.BREATH }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val goal = goalMinutes.takeIf { it > 0 } ?: return@Button
                val picked = challenge ?: return@Button
                prefs.setDailyGoalMinutes(goal)
                prefs.setFrictionChallenge(picked)
                prefs.setGoalConfigured(true)
                onFinished()
            },
            enabled = goalMinutes > 0 && challenge != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                contentColor = MaterialTheme.colorScheme.background
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.goal_cta_begin), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun GoalChip(label: String, minutes: Int, selectedMinutes: Int, onSelect: (Int) -> Unit) {
    val selected = selectedMinutes == minutes
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(48.dp)
            .background(
                if (selected) AccentDim.copy(alpha = 0.35f) else SurfaceRaised,
                RoundedCornerShape(12.dp)
            )
            .border(
                2.dp,
                if (selected) AccentGreen else SurfaceRaised,
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect(minutes) }
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = label,
            color = if (selected) AccentGreen else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ChallengeCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) AccentDim.copy(alpha = 0.25f) else SurfaceRaised,
                RoundedCornerShape(16.dp)
            )
            .border(
                2.dp,
                if (selected) AccentGreen else SurfaceRaised,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    2.dp,
                    if (selected) AccentGreen else TextSecondary.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(11.dp)
                        .background(AccentGreen, CircleShape)
                )
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
