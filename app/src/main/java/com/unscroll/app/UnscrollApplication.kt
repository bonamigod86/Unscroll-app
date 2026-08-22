package com.unscroll.app

import android.app.Application
import com.superwall.sdk.Superwall
import com.unscroll.app.billing.SuperwallConfig

class UnscrollApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (SuperwallConfig.isConfigured) {
            runCatching {
                Superwall.configure(applicationContext, SuperwallConfig.API_KEY)
            }
        }
    }
}
