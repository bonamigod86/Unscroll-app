package com.unscroll.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.unscroll.app.R
import com.unscroll.app.data.PrefsRepository
import com.unscroll.app.ui.theme.AccentGreen

class ReclaimedWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = PrefsRepository.get(context)
        val message = context.getString(
            R.string.widget_reclaimed,
            formatMinutes(prefs.todayReclaimedMinutes)
        )
        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black)
            ) {
                Text(
                    text = message,
                    style = TextStyle(color = ColorProvider(AccentGreen)),
                    modifier = GlanceModifier.padding(12.dp)
                )
            }
        }
    }

    private fun formatMinutes(minutes: Float): String {
        val total = minutes.toInt()
        if (total < 60) return "$total min"
        val hours = total / 60
        val rest = total % 60
        return if (rest == 0) "${hours}h" else "${hours}h $rest m"
    }
}

class ReclaimedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReclaimedWidget()
}
