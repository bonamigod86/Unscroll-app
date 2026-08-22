package com.unscroll.app.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.R
import com.unscroll.app.core.PermissionChecker
import com.unscroll.app.ui.theme.AccentDim
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.SurfaceRaised
import com.unscroll.app.ui.theme.TextSecondary

@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var accessibilityOn by remember {
        mutableStateOf(PermissionChecker.isAccessibilityServiceEnabled(context))
    }
    var batteryOk by remember {
        mutableStateOf(PermissionChecker.isIgnoringBatteryOptimizations(context))
    }

    DisposableEffect(context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                accessibilityOn = PermissionChecker.isAccessibilityServiceEnabled(context)
                batteryOk = PermissionChecker.isIgnoringBatteryOptimizations(context)
            }
        }
        (context as? androidx.activity.ComponentActivity)?.lifecycle?.addObserver(observer)
        onDispose {
            (context as? androidx.activity.ComponentActivity)?.lifecycle?.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))
        Text(
            text = stringResource(R.string.perm_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        PermCard(
            title = stringResource(R.string.perm_accessibility_title),
            description = stringResource(R.string.perm_accessibility_desc),
            granted = accessibilityOn,
            buttonText = stringResource(R.string.perm_open_settings)
        ) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        PermCard(
            title = stringResource(R.string.perm_battery_title),
            description = stringResource(R.string.perm_battery_desc),
            granted = batteryOk,
            buttonText = stringResource(R.string.perm_request)
        ) {
            requestIgnoreBattery(context)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                contentColor = MaterialTheme.colorScheme.background
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.perm_done), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PermCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceRaised, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            StatusChip(granted)
        }
        Spacer(Modifier.height(8.dp))
        Text(description, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        if (!granted) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentDim,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text(buttonText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatusChip(granted: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (granted) AccentGreen.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(if (granted) R.string.perm_granted else R.string.perm_request),
            color = if (granted) AccentGreen else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun requestIgnoreBattery(context: Context) {
    val direct = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(direct) }
        .onFailure { runCatching { context.startActivity(fallback) } }
}
