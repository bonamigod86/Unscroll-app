package com.unscroll.app.core

import android.content.Intent
import android.os.Bundle
import java.text.Normalizer
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
import androidx.compose.foundation.layout.Row
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
import com.unscroll.app.MainActivity
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
import java.util.Calendar

class FrictionOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val store = BlockedAppsStore.get(this)
        val prefs = PrefsRepository.get(this)
        prefs.incrementInterventions()
        val spec = resolveChallengeSpec(prefs)

        setContent {
            UnscrollTheme {
                FrictionScreen(
                    spec = spec,
                    blockedPackage = pkg,
                    store = store,
                    prefs = prefs,
                    onBackToLife = {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

private data class ChallengeSpec(
    val type: FrictionChallenge,
    val breathSeconds: Int,
    val holdMs: Float,
    val quotePoolRes: Int,
    val night: Boolean
)

private fun resolveChallengeSpec(prefs: PrefsRepository): ChallengeSpec {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val night = hour >= 23 || hour < 6
    if (night) {
        return ChallengeSpec(FrictionChallenge.BREATH, NIGHT_BREATH_SECONDS, 0f, 0, true)
    }
    val opensToday = prefs.todayInterventions.value
    val intensity = when {
        opensToday >= HEAVY_THRESHOLD -> 2
        opensToday <= LIGHT_THRESHOLD -> 0
        else -> 1
    }
    return when (prefs.frictionChallenge.value) {
        FrictionChallenge.PHRASE -> ChallengeSpec(
            type = FrictionChallenge.PHRASE,
            breathSeconds = 30,
            holdMs = listOf(LIGHT_HOLD_MS, NORMAL_HOLD_MS, HEAVY_HOLD_MS)[intensity],
            quotePoolRes = if (intensity == 2) R.array.stoic_quotes_long else 0,
            night = false
        )
        FrictionChallenge.STOIC -> ChallengeSpec(
            type = FrictionChallenge.STOIC,
            breathSeconds = listOf(15, 30, 60)[intensity],
            holdMs = 0f,
            quotePoolRes = if (intensity == 2) R.array.stoic_quotes_long else R.array.stoic_quotes,
            night = false
        )
        FrictionChallenge.BREATH -> ChallengeSpec(
            type = FrictionChallenge.BREATH,
            breathSeconds = listOf(15, 30, 60)[intensity],
            holdMs = 0f,
            quotePoolRes = 0,
            night = false
        )
    }
}

@Composable
private fun FrictionScreen(
    spec: ChallengeSpec,
    blockedPackage: String,
    store: BlockedAppsStore,
    prefs: PrefsRepository,
    onBackToLife: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var phase by remember { mutableIntStateOf(0) }

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
            text = if (spec.night) stringResource(R.string.night_body) else appLabel,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        if (phase == 0) {
            ChallengeBody(
                spec = spec,
                onCompleted = { phase = 1 },
                onBackToLife = onBackToLife
            )
        } else {
            DurationPicker { minutes ->
                store.grantTemporaryUnlock(blockedPackage, minutes)
                prefs.incrementUnlocks()
                UnscrollAccessibilityService.scheduleRelock(
                    context.applicationContext,
                    blockedPackage,
                    minutes * 60_000L
                )
                (context as? ComponentActivity)?.finish()
            }
        }
    }
}

@Composable
private fun ChallengeBody(
    spec: ChallengeSpec,
    onCompleted: () -> Unit,
    onBackToLife: () -> Unit
) {
    when (spec.type) {
        FrictionChallenge.PHRASE -> HoldThenPhraseFlow(
            holdMs = spec.holdMs,
            quotePoolRes = spec.quotePoolRes,
            onCompleted = onCompleted,
            onBackToLife = onBackToLife
        )
        FrictionChallenge.STOIC -> StoicTypingChallenge(
            poolRes = spec.quotePoolRes,
            onCompleted = onCompleted,
            onBackToLife = onBackToLife
        )
        FrictionChallenge.BREATH -> BreathingChallenge(
            totalSeconds = spec.breathSeconds,
            onCompleted = onCompleted,
            onBackToLife = onBackToLife
        )
    }
}

@Composable
private fun HoldThenPhraseFlow(
    holdMs: Float,
    quotePoolRes: Int,
    onCompleted: () -> Unit,
    onBackToLife: () -> Unit
) {
    var phase by remember { mutableIntStateOf(0) }
    if (phase == 0) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HoldToBeginButton(holdMs = holdMs, onComplete = { phase = 1 })
            Spacer(Modifier.height(28.dp))
            BackToLifeButton(onBackToLife)
        }
    } else {
        if (quotePoolRes != 0) {
            StoicTypingChallenge(poolRes = quotePoolRes, onCompleted = onCompleted, onBackToLife = onBackToLife)
        } else {
            TypedTextChallenge(
                target = stringResource(R.string.friction_phrase),
                onCompleted = onCompleted,
                onBackToLife = onBackToLife
            )
        }
    }
}

@Composable
private fun StoicTypingChallenge(poolRes: Int, onCompleted: () -> Unit, onBackToLife: () -> Unit) {
    val quotes = stringArrayResource(poolRes)
    val quote = remember { quotes.random() }
    TypedTextChallenge(target = quote, onCompleted = onCompleted, onBackToLife = onBackToLife)
}

@Composable
private fun TypedTextChallenge(
    target: String,
    onCompleted: () -> Unit,
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
                if (matches) onCompleted() else showMismatch = true
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
private fun DurationPicker(onPick: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.override_question),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OVERRIDE_MINUTES.forEach { minutes ->
                Button(
                    onClick = { onPick(minutes) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceRaised,
                        contentColor = AccentGreen
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(width = 92.dp, height = 64.dp)
                ) {
                    Text(
                        text = stringResource(R.string.override_opt, minutes),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.override_note),
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BreathingChallenge(
    totalSeconds: Int,
    onCompleted: () -> Unit,
    onBackToLife: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var inhale by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val startedAt = System.currentTimeMillis()
        while (secondsLeft > 0) {
            delay(200)
            val elapsed = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            secondsLeft = (totalSeconds - elapsed).coerceAtLeast(0)
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
                onClick = onCompleted,
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
private fun HoldToBeginButton(holdMs: Float, onComplete: () -> Unit) {
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
                .pointerInput(holdMs) {
                    detectTapGestures(
                        onPress = {
                            job?.cancel()
                            job = scope.launch {
                                while (progress < 1f && isActive) {
                                    delay(16)
                                    progress = (progress + 16f / holdMs).coerceAtMost(1f)
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
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")

private val OVERRIDE_MINUTES = listOf(2, 5, 10)
private const val LIGHT_HOLD_MS = 2000f
private const val NORMAL_HOLD_MS = 3000f
private const val HEAVY_HOLD_MS = 4000f
private const val LIGHT_THRESHOLD = 1
private const val HEAVY_THRESHOLD = 4
private const val NIGHT_BREATH_SECONDS = 60
private const val BREATH_MIN_SCALE = 0.55f
