package com.unscroll.app.paywall

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.BuildConfig
import com.unscroll.app.R
import com.unscroll.app.billing.BillingManager
import com.unscroll.app.billing.PurchaseStatus
import com.unscroll.app.data.PrefsRepository
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.AccentDim
import com.unscroll.app.ui.theme.TextSecondary

val LocalBilling = androidx.compose.runtime.staticCompositionLocalOf<BillingManager> {
    error("BillingManager not provided")
}

@Composable
fun PaywallScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val billing = LocalBilling.current
    val prefs = PrefsRepository.get(context)

    val products by billing.products.collectAsState()
    val connected by billing.connected.collectAsState()
    val status by billing.status.collectAsState()
    val subscribed by prefs.subscribed.collectAsState()

    val fallbackPrice = stringResource(R.string.paywall_price_fallback)
    val price = remember(products) {
        if (products.isEmpty()) fallbackPrice else billing.weeklyPriceFallback(fallbackPrice)
    }

    LaunchedEffect(subscribed) {
        if (subscribed) onUnlocked()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(AccentDim.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.paywall_badge),
                color = AccentGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = price,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(24.dp))

        FeatureRow(stringResource(R.string.paywall_feature1))
        FeatureRow(stringResource(R.string.paywall_feature2))
        FeatureRow(stringResource(R.string.paywall_feature3))

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { billing.launchPurchase(context as ComponentActivity) },
            enabled = products.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                contentColor = MaterialTheme.colorScheme.background
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = stringResource(R.string.paywall_cta),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        if (products.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            if (!connected) {
                Text(
                    text = stringResource(R.string.billing_unavailable),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            if (BuildConfig.DEBUG) {
                TextButton(onClick = {
                    prefs.setSubscribed(true)
                    onUnlocked()
                }) {
                    Text(stringResource(R.string.billing_debug_bypass), color = AccentGreen)
                }
            }
        } else {
            when (status) {
                PurchaseStatus.PENDING -> StatusText(stringResource(R.string.billing_pending))
                PurchaseStatus.FAILED -> StatusText(stringResource(R.string.billing_failed))
                PurchaseStatus.IDLE -> Unit
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { billing.restorePurchases() }) {
            Text(stringResource(R.string.paywall_restore), color = TextSecondary)
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.paywall_terms, price),
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .background(AccentDim.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
        ) {
            Text("✓", color = AccentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(12.dp))
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun StatusText(message: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}
