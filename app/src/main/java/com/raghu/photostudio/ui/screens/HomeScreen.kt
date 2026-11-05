package com.raghu.photostudio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raghu.photostudio.R
import com.raghu.photostudio.ui.StudioTopBar
import com.raghu.photostudio.ui.theme.Saffron
import com.raghu.photostudio.ui.theme.Teal

/** First screen: one big tile per tool. New tools get a tile here. */
@Composable
fun ToolsHomeScreen(onPhotos: () -> Unit, onInterest: () -> Unit, onChit: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = Teal)
        Text(
            stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        ToolTile(
            icon = { Icon(Icons.Filled.Face, contentDescription = null, modifier = Modifier.size(56.dp)) },
            title = stringResource(R.string.tool_photos),
            desc = stringResource(R.string.tool_photos_desc),
            container = Teal,
            content = Color.White,
            onClick = onPhotos,
        )
        ToolTile(
            icon = {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Text("₹", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }
            },
            title = stringResource(R.string.tool_interest),
            desc = stringResource(R.string.tool_interest_desc),
            container = Saffron,
            content = Color(0xFF3A2500),
            onClick = onInterest,
        )
        ToolTile(
            icon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(56.dp)) },
            title = stringResource(R.string.tool_chit),
            desc = stringResource(R.string.tool_chit_desc),
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onChit,
        )
        ToolTile(
            icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(48.dp)) },
            title = stringResource(R.string.home_settings),
            desc = stringResource(R.string.tool_settings_desc),
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurface,
            onClick = onSettings,
        )
    }
}

@Composable
private fun ToolTile(
    icon: @Composable () -> Unit,
    title: String,
    desc: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** The passport-photo tool's own start screen. */
@Composable
fun PhotoHomeScreen(onBack: () -> Unit, onTakePhoto: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit) {
    Scaffold(topBar = { StudioTopBar(stringResource(R.string.tool_photos), onBack) }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.photo_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Main action: big and obvious.
            Card(
                onClick = onTakePhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Teal, contentColor = Color.White),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.Face, contentDescription = null, modifier = Modifier.size(96.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.home_take_photo), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.home_take_photo_desc), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallTile(
                    painterResource(R.drawable.ic_photo_library),
                    stringResource(R.string.home_previous),
                    onHistory,
                    Modifier.weight(1f),
                )
                SmallTile(
                    rememberVectorPainter(Icons.Filled.Settings),
                    stringResource(R.string.home_settings),
                    onSettings,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SmallTile(icon: Painter, label: String, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
