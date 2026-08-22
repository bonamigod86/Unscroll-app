package com.unscroll.app.core

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.unscroll.app.data.BlockedAppsStore

class UnscrollAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg.isEmpty()) return

        val store = BlockedAppsStore.get(this)
        if (!store.isBlocked(pkg)) return
        if (store.isTemporarilyUnlocked(pkg)) return

        val intent = Intent(this, FrictionOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(FrictionOverlayActivity.EXTRA_PACKAGE, pkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}
