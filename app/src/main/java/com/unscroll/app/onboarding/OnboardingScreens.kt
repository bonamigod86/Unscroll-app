package com.unscroll.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.R
import com.unscroll.app.billing.SuperwallConfig
import com.unscroll.app.paywall.PaywallScreen
import com.unscroll.app.paywall.SuperwallPaywallGate
import com.unscroll.app.ui.theme.AccentDim
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.SurfaceRaised
import com.unscroll.app.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(onUnlocked: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    val choice = rememberSaveable { mutableStateOf(-1) }

    when (step) {
        0 -> HookScreen(
            progress = 1f / QUIZ_STEPS,
            selected = choice.value,
            onSelect = { choice.value = it },
            onNext = { step = 1 }
        )
        1 -> AgitationScreen(
            progress = 2f / QUIZ_STEPS,
            choice = choice.value
        ) { step = 2 }
        2 -> DiagnosisScreen(
            progress = 3f / QUIZ_STEPS,
            choice = choice.value
        ) { step = 3 }
        3 -> SolutionScreen(progress = 4f / QUIZ_STEPS) { step = 4 }
        4 -> ProofScreen(progress = 5f / QUIZ_STEPS) { step = 5 }
        else -> {
            if (SuperwallConfig.isConfigured) {
                SuperwallPaywallGate(onUnlocked = onUnlocked)
            } else {
                PaywallScreen(onUnlocked = onUnlocked)
            }
        }
    }
}

@Composable
private fun OnboardingLayout(
    title: String,
    progress: Float,
    ctaEnabled: Boolean = true,
    ctaText: String,
    onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        StepProgressBar(progress)
        Spacer(Modifier.height(28.dp))
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
        Button(
            onClick = onNext,
            enabled = ctaEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                contentColor = MaterialTheme.colorScheme.background
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(ctaText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(20.dp)
        )
    }
}

@Composable
private fun StepProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        trackColor = SurfaceRaised,
        color = AccentGreen,
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
    )
}

@Composable
fun HookScreen(progress: Float, selected: Int, onSelect: (Int) -> Unit, onNext: () -> Unit) {
    val options = listOf(
        stringResource(R.string.onboarding_hook_opt_lt2),
        stringResource(R.string.onboarding_hook_opt_2_4),
        stringResource(R.string.onboarding_hook_opt_gt4)
    )
    OnboardingLayout(
        title = stringResource(R.string.onboarding_hook_title),
        progress = progress,
        ctaText = stringResource(R.string.onboarding_cta_continue),
        ctaEnabled = selected >= 0,
        onNext = onNext
    ) {
        options.forEachIndexed { index, label ->
            SelectableOption(label = label, selected = selected == index) { onSelect(index) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun AgitationScreen(progress: Float, choice: Int, onNext: () -> Unit) {
    val hours = listOf(2, 4, 6).getOrElse(choice) { 4 }
    val days = hours * 365 / 24
    OnboardingLayout(
        title = stringResource(R.string.onboarding_agitation_days, days),
        progress = progress,
        ctaText = stringResource(R.string.onboarding_cta_continue),
        onNext = onNext
    ) {
        Text(
            text = stringResource(R.string.onboarding_agitation_body, hours, days),
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun DiagnosisScreen(progress: Float, choice: Int, onNext: () -> Unit) {
    val level = if (choice == 0) {
        stringResource(R.string.onboarding_diagnosis_high)
    } else {
        stringResource(R.string.onboarding_diagnosis_very_high)
    }
    OnboardingLayout(
        title = stringResource(R.string.onboarding_diagnosis_title),
        progress = progress,
        ctaText = stringResource(R.string.onboarding_cta_continue),
        onNext = onNext
    ) {
        Box(
            modifier = Modifier
                .background(AccentDim.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(horizontal = 32.dp, vertical = 18.dp)
        ) {
            Text(
                text = level,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = AccentGreen
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_diagnosis_note),
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SolutionScreen(progress: Float, onNext: () -> Unit) {
    OnboardingLayout(
        title = stringResource(R.string.onboarding_solution_title),
        progress = progress,
        ctaText = stringResource(R.string.onboarding_cta_continue),
        onNext = onNext
    ) {
        BulletLine(stringResource(R.string.onboarding_solution_bullet1))
        BulletLine(stringResource(R.string.onboarding_solution_bullet2))
        BulletLine(stringResource(R.string.onboarding_solution_bullet3))
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentDim.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "🔒 " + stringResource(R.string.privacy_badge),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProofScreen(progress: Float, onNext: () -> Unit) {
    OnboardingLayout(
        title = stringResource(R.string.onboarding_proof_quote),
        progress = progress,
        ctaText = stringResource(R.string.onboarding_cta_continue),
        onNext = onNext
    ) {
        Text(
            text = stringResource(R.string.onboarding_proof_sub),
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectableOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) AccentGreen else SurfaceRaised
    val bg = if (selected) AccentDim.copy(alpha = 0.25f) else SurfaceRaised
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(bg, RoundedCornerShape(14.dp))
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(8.dp)
                .background(AccentGreen, RoundedCornerShape(4.dp))
        ) {}
        Spacer(Modifier.padding(start = 12.dp))
        Text(text, fontSize = 15.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

private const val QUIZ_STEPS = 5
