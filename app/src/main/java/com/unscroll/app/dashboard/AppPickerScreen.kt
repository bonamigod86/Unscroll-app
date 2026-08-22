package com.unscroll.app.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.R
import com.unscroll.app.data.AppEntry
import com.unscroll.app.data.BlockedAppsStore
import com.unscroll.app.data.InstalledAppsProvider
import com.unscroll.app.ui.theme.AccentGreen
import com.unscroll.app.ui.theme.SurfaceRaised
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppPickerScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { BlockedAppsStore.get(context) }
    val blocked by store.blockedFlow.collectAsState()
    var query by remember { mutableStateOf("") }

    val apps by produceState<List<AppEntry>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            InstalledAppsProvider.loadLaunchableApps(context)
        }
    }

    val filtered = apps
        ?.filter { it.label.contains(query, ignoreCase = true) }
        .orEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(52.dp))
        Text(
            text = stringResource(R.string.picker_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.picker_selected_count, blocked.size),
            color = AccentGreen,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.picker_search_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (apps == null) {
                CircularProgressIndicator(
                    color = AccentGreen,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = blocked.contains(app.packageName),
                            onToggle = { store.setBlocked(app.packageName, it) }
                        )
                    }
                }
            }
        }

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                contentColor = MaterialTheme.colorScheme.background
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .height(54.dp)
        ) {
            Text(stringResource(R.string.picker_save), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceRaised, RoundedCornerShape(14.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        app.icon?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(38.dp))
        } ?: Box(
            modifier = Modifier
                .size(38.dp)
                .background(SurfaceRaised, RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = Color.Gray
            )
        )
    }
}
