package com.unscroll.app.core

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.R
import com.unscroll.app.data.BlockedAppsStore
import com.unscroll.app.data.FrictionChallenge
import com.unscroll.app.data.PrefsRepository
import com.unscroll.app.ui.theme.AccentDim
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.BackgroundDark
import com.unscroll.app.ui.theme.SurfaceRaised
import com.unscroll.app.ui.theme.TextPrimary
import com.unscroll.app.ui.theme.TextSecondary
import com.unscroll.app.ui.theme.UnscrollTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FrictionOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val store = BlockedAppsStore.get(this)
        val prefs = PrefsRepository.get(this)
        prefs.incrementInterventions()
        val challenge = prefs.frictionChallenge.value

        setContent {
            UnscrollTheme {
                FrictionScreen(
                    challenge = challenge,
                    blockedPackage = pkg,
                    onConfirm = { minutes ->
                        store.grantTemporaryUnlock(pkg, minutes)
                        prefs.incrementUnlocks()
                        finish()
                    },
                    onBackToLife = {
                        startActivity(
                            Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_HOME)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }
}

@Composable
private fun FrictionScreen(
    challenge: FrictionChallenge,
    blockedPackage: String,
    onConfirm: (Int) -> Unit,
    onBackToLife: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val appLabel = remember(blockedPackage) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(blockedPackage, 0)).toString()
        }.getOrDefault(blockedPackage)
    }
    val appIcon = remember(blockedPackage) {
        runCatching {
            val pm = context.packageManager
            (pm.getApplicationIcon(blockedPackage) as android.graphics.drawable.BitmapDrawable)
                .bitmap
                .asImageBitmap()
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        appIcon?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = stringResource(R.string.friction_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = appLabel,
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        val unlock: () -> Unit = { onConfirm(TEMP_UNLOCK_MINUTES) }
        when (challenge) {
            FrictionChallenge.PHRASE -> HoldThenPhraseFlow(
                onUnlocked = unlock,
                onBackToLife = onBackToLife
            )
            FrictionChallenge.STOIC -> StoicTypingChallenge(
                onUnlocked = unlock,
                onBackToLife = onBackToLife
            )
            FrictionChallenge.BREATH -> BreathingChallenge(
                onUnlocked = unlock,
                onBackToLife = onBackToLife
            )
        }
    }
}

@Composable
private fun HoldThenPhraseFlow(onUnlocked: () -> Unit, onBackToLife: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    if (phase == 0) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HoldToBeginButton(onComplete = { phase = 1 })
            Spacer(Modifier.height(28.dp))
            BackToLifeButton(onBackToLife)
        }
    } else {
        TypedTextChallenge(
            target = stringResource(R.string.friction_phrase),
            onUnlocked = onUnlocked,
            onBackToLife = onBackToLife
        )
    }
}

@Composable
private fun StoicTypingChallenge(onUnlocked: () -> Unit, onBackToLife: () -> Unit) {
    val quotes = stringArrayResource(R.array.stoic_quotes)
    val quote = remember { quotes.random() }
    TypedTextChallenge(
        target = quote,
        onUnlocked = onUnlocked,
        onBackToLife = onBackToLife
    )
}

@Composable
private fun TypedTextChallenge(
    target: String,
    onUnlocked: () -> Unit,
    onBackToLife: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showMismatch by remember { mutableStateOf(false) }
    val matches = normalize(input) == normalize(target)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.friction_phrase_prompt, target),
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                showMismatch = false
            },
            placeholder = { Text(stringResource(R.string.friction_phrase_hint)) },
            isError = showMismatch,
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        if (showMismatch) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.friction_mismatch),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (matches) onUnlocked() else showMismatch = true
            },
            enabled = input.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.friction_confirm),
                color = BackgroundDark,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        BackToLifeButton(onBackToLife)
    }
}

@Composable
private fun BackToLifeButton(onBackToLife: () -> Unit) {
    Button(
        onClick = onBackToLife,
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = TextSecondary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.friction_back_to_life))
    }
}

@Composable
private fun BreathingChallenge(onUnlocked: () -> Unit, onBackToLife: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(BREATH_TOTAL_SECONDS) }
    var inhale by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val startedAt = System.currentTimeMillis()
        while (secondsLeft > 0) {
            delay(200)
            val elapsed = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            secondsLeft = (BREATH_TOTAL_SECONDS - elapsed).coerceAtLeast(0)
            inhale = elapsed % 10 < 5
        }
    }

    val finished = secondsLeft == 0
    val scale by animateFloatAsState(
        targetValue = if (finished || inhale) 1f else BREATH_MIN_SCALE,
        animationSpec = tween(durationMillis = 5000),
        label = "breathScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.85f
                    }
                    .clip(CircleShape)
                    .background(AccentDim.copy(alpha = 0.35f))
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (finished) {
                    Text(
                        text = stringResource(R.string.breath_done),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                } else {
                    Text(
                        text = stringResource(if (inhale) R.string.breath_in else R.string.breath_out),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = secondsLeft.toString(),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentGreen
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        if (finished) {
            Button(
                onClick = onUnlocked,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.friction_confirm),
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        BackToLifeButton(onBackToLife)
    }
}

@Composable
private fun HoldToBeginButton(onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var job by remember { mutableStateOf<Job?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp)
    ) {
        CircularProgressIndicator(
            progress = { progress },
            strokeWidth = 5.dp,
            color = AccentGreen,
            trackColor = AccentDim,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            job?.cancel()
                            job = scope.launch {
                                while (progress < 1f && isActive) {
                                    delay(16)
                                    progress = (progress + 16f / HOLD_DURATION_MS).coerceAtMost(1f)
                                }
                                if (progress >= 1f) onComplete()
                            }
                            try {
                                awaitRelease()
                            } catch (_: Exception) {
                            }
                            job?.cancel()
                            if (progress < 1f) progress = 0f
                        }
                    )
                }
        ) {
            Text(
                text = stringResource(R.string.friction_hold),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

private fun normalize(text: String): String =
    text.trim().lowercase().replace(Regex("\\s+"), " ")

private const val HOLD_DURATION_MS = 3000f
private const val TEMP_UNLOCK_MINUTES = 10
private const val BREATH_TOTAL_SECONDS = 30
private const val BREATH_MIN_SCALE = 0.55f
