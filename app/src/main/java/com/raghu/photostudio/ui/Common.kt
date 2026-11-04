package com.raghu.photostudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.raghu.photostudio.R
import com.raghu.photostudio.model.SheetLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/** Small drawing of how photos sit on the 4x6 sheet. Modifier should set an aspect ratio. */
@Composable
fun SheetDiagram(layout: SheetLayout, modifier: Modifier, photoColor: Color, paperColor: Color = Color.White) {
    Canvas(modifier) {
        val k = size.width / layout.widthMm
        drawRect(paperColor)
        drawRect(Color(0xFFCCCCCC), style = Stroke(width = 1f))
        layout.cells.forEach { r ->
            drawRect(
                color = photoColor,
                topLeft = Offset(r.left * k, r.top * k),
                size = Size(r.width() * k, r.height() * k),
            )
        }
    }
}
