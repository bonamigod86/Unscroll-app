package com.unscroll.app.core

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.unscroll.app.data.BlockedAppsStore

class UnscrollAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg.isEmpty()) return

        lastForegroundPackage = pkg

        if (pkg == packageName) return

        val store = BlockedAppsStore.get(this)
        if (!store.isBlocked(pkg)) return
        if (store.isTemporarilyUnlocked(pkg)) return

        startActivity(buildOverlayIntent(this, pkg))
    }

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        var lastForegroundPackage: String? = null
            private set

        private val mainHandler = Handler(Looper.getMainLooper())

        fun scheduleRelock(context: Context, blockedPackage: String, delayMs: Long) {
            val appContext = context.applicationContext
            mainHandler.postDelayed({
                if (lastForegroundPackage == blockedPackage &&
                    !BlockedAppsStore.get(appContext).isTemporarilyUnlocked(blockedPackage)
                ) {
                    appContext.startActivity(
                        buildOverlayIntent(appContext, blockedPackage).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                }
            }, delayMs)
        }

        fun buildOverlayIntent(context: Context, pkg: String): Intent =
            Intent(context, FrictionOverlayActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(FrictionOverlayActivity.EXTRA_PACKAGE, pkg)
            }
    }
}
