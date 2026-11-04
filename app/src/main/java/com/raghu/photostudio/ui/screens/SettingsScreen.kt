package com.raghu.photostudio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raghu.photostudio.R
import com.raghu.photostudio.data.Prefs
import com.raghu.photostudio.ui.StudioTopBar

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val language = remember { Prefs.currentLanguage() }
    var whiteBg by remember { mutableStateOf(prefs.whiteBackgroundDefault) }
    var backCamera by remember { mutableStateOf(prefs.startWithBackCamera) }
    var cutLines by remember { mutableStateOf(prefs.cutLines) }

    Scaffold(topBar = { StudioTopBar(stringResource(R.string.settings_title), onBack) }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LanguageChip(stringResource(R.string.lang_english), language == Prefs.LANG_EN) {
                        Prefs.setLanguage(Prefs.LANG_EN)
                    }
                    LanguageChip(stringResource(R.string.lang_telugu), language == Prefs.LANG_TE) {
                        Prefs.setLanguage(Prefs.LANG_TE)
                    }
                }
            }

            Section(stringResource(R.string.settings_photo)) {
                ToggleRow(stringResource(R.string.settings_white_bg), whiteBg) {
                    whiteBg = it; prefs.whiteBackgroundDefault = it
                }
                ToggleRow(stringResource(R.string.settings_back_camera), backCamera) {
                    backCamera = it; prefs.startWithBackCamera = it
                }
                ToggleRow(stringResource(R.string.settings_cut_lines), cutLines) {
                    cutLines = it; prefs.cutLines = it
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { if (!selected) onClick() },
        label = { Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp)) },
        modifier = Modifier.height(52.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
