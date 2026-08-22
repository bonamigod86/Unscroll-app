package com.unscroll.app.billing

object SuperwallConfig {

    const val API_KEY = ""

    const val PLACEMENT_ONBOARDING_COMPLETE = "onboarding_complete"

    val isConfigured: Boolean
        get() = API_KEY.isNotBlank()
}
