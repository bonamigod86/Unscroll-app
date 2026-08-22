package com.unscroll.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.unscroll.app.billing.BillingManager
import com.unscroll.app.data.PrefsRepository
import com.unscroll.app.navigation.UnscrollNavGraph
import com.unscroll.app.onboarding.OnboardingScreen
import com.unscroll.app.paywall.LocalBilling
import com.unscroll.app.ui.theme.UnscrollTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PrefsRepository.get(this)

        setContent {
            UnscrollTheme {
                val billing = remember { BillingManager(this) }
                val onboardingDone by prefs.onboardingDone.collectAsState()

                LaunchedEffect(Unit) {
                    prefs.touchStreak()
                    billing.connect()
                }
                DisposableEffect(Unit) {
                    onDispose { billing.dispose() }
                }

                CompositionLocalProvider(LocalBilling provides billing) {
                    if (!onboardingDone) {
                        OnboardingScreen(onUnlocked = { prefs.setOnboardingDone(true) })
                    } else {
                        UnscrollNavGraph()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PrefsRepository.get(this).touchStreak()
    }
}
