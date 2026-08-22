package com.unscroll.app.dashboard

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.BuildConfig
import com.unscroll.app.R
import com.unscroll.app.billing.BillingManager
import com.unscroll.app.data.PrefsRepository
import com.unscroll.app.paywall.LocalBilling
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.SurfaceRaised
import com.unscroll.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenGoalSetup: () -> Unit
) {
    val context = LocalContext.current
    val billing: BillingManager = LocalBilling.current
    val prefs = PrefsRepository.get(context)
    val subscribed by prefs.subscribed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(52.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = AccentGreen, fontSize = 22.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 16.dp))
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(24.dp))

        SectionLabel(stringResource(R.string.settings_subscription_title))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceRaised, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (subscribed) R.string.billing_subscribed else R.string.billing_not_subscribed
                    ),
                    color = if (subscribed) AccentGreen else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = { billing.restorePurchases() }) {
                Text(stringResource(R.string.settings_restore), color = AccentGreen)
            }
            TextButton(onClick = { openSubscriptions(context) }) {
                Text(stringResource(R.string.settings_manage_sub), color = TextSecondary)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionNavRow(stringResource(R.string.settings_goal_row), onOpenGoalSetup)
        Spacer(Modifier.height(10.dp))
        SectionNavRow(stringResource(R.string.settings_blocked_apps_row), onOpenPicker)
        Spacer(Modifier.height(10.dp))
        SectionNavRow(stringResource(R.string.settings_permissions_row), onOpenPermissions)

        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.settings_estimate_note),
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SectionNavRow(title: String, onClick: () -> Unit) {
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

private fun openSubscriptions(context: android.content.Context) {
    val uri = "https://play.google.com/store/account/subscriptions" +
        "?sku=${BillingManager.PRODUCT_ID}&package=${context.packageName}"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)))
    }
}
