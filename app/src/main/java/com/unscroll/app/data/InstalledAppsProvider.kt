package com.unscroll.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

object InstalledAppsProvider {

    fun loadLaunchableApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val self = context.packageName
        return pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != self }
            .mapNotNull { info ->
                val launch = pm.getLaunchIntentForPackage(info.packageName) ?: return@mapNotNull null
                if (launch.action != Intent.ACTION_MAIN) return@mapNotNull null
                val label = try {
                    pm.getApplicationLabel(info).toString()
                } catch (_: Exception) {
                    info.packageName
                }
                AppEntry(
                    packageName = info.packageName,
                    label = label,
                    icon = info.loadIcon(pm)?.toImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun Drawable.toImageBitmap(sizePx: Int = 96): ImageBitmap {
        if (this is android.graphics.drawable.BitmapDrawable) {
            val existing = bitmap
            if (existing != null && !existing.isRecycled) return existing.asImageBitmap()
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bmp.asImageBitmap()
    }
}
