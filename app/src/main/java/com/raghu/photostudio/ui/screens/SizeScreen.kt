package com.raghu.photostudio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raghu.photostudio.R
import com.raghu.photostudio.model.PhotoSize
import com.raghu.photostudio.model.SheetLayout
import com.raghu.photostudio.ui.SheetDiagram
import com.raghu.photostudio.ui.StudioTopBar

@Composable
fun SizeScreen(onBack: () -> Unit, onPick: (PhotoSize) -> Unit) {
    val passportLayout = remember { SheetLayout.forSize(PhotoSize.PASSPORT) }
    val fullLayout = remember { SheetLayout.forSize(PhotoSize.FOUR_BY_SIX) }

    Scaffold(topBar = { StudioTopBar(stringResource(R.string.size_title), onBack) }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SizeCard(
                title = stringResource(R.string.size_passport),
                desc = stringResource(R.string.size_passport_desc, passportLayout.count),
                layout = passportLayout,
                onClick = { onPick(PhotoSize.PASSPORT) },
                modifier = Modifier.weight(1f),
            )
            SizeCard(
                title = stringResource(R.string.size_4x6),
                desc = stringResource(R.string.size_4x6_desc),
                layout = fullLayout,
                onClick = { onPick(PhotoSize.FOUR_BY_SIX) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SizeCard(title: String, desc: String, layout: SheetLayout, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheetDiagram(
                layout = layout,
                modifier = Modifier
                    .width(if (layout.landscape) 130.dp else 90.dp)
                    .aspectRatio(layout.widthMm / layout.heightMm),
                photoColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(desc, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
