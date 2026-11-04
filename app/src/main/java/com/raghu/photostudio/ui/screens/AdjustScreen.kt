package com.raghu.photostudio.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.raghu.photostudio.R
import com.raghu.photostudio.data.PhotoStore
import com.raghu.photostudio.data.Prefs
import com.raghu.photostudio.image.Adjustments
import com.raghu.photostudio.image.BackgroundTools
import com.raghu.photostudio.image.ImageTools
import com.raghu.photostudio.model.PhotoSize
import com.raghu.photostudio.ui.CaptureHolder
import com.raghu.photostudio.ui.StudioTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Adjust a photo that was just taken, then save it as a new entry. */
@Composable
fun AdjustScreen(
    size: PhotoSize,
    onRetake: () -> Unit,
    onMissingPhoto: () -> Unit,
    onSaved: (jobId: String) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val original = CaptureHolder.photo

    if (original == null) {
        // App was closed in the background and the photo was lost — go back home.
        LaunchedEffect(Unit) { onMissingPhoto() }
        return
    }

    AdjustContent(
        size = size,
        original = original,
        startAdjustments = Adjustments(),
        startWhiteBackground = prefs.whiteBackgroundDefault,
        startName = "",
        title = stringResource(R.string.adjust_title),
        backLabel = stringResource(R.string.adjust_retake),
        onBack = onRetake,
        onCommit = { finalPhoto, adjustments, whiteBackground, name ->
            PhotoStore.save(context, size, original, finalPhoto, adjustments, whiteBackground, name)
        },
        onCommitted = onSaved,
    )
}

/** Adjust a photo saved earlier. Starts from the untouched capture and the sliders used last time. */
@Composable
fun EditPhotoScreen(
    jobId: String,
    onBack: () -> Unit,
    onMissingPhoto: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current

    val loaded by produceState<EditSource?>(initialValue = null, jobId) {
        value = withContext(Dispatchers.IO) {
            val job = PhotoStore.get(context, jobId)
            val photo = job?.let { BitmapFactory.decodeFile(it.editFile.path) }
            if (job == null || photo == null) {
                EditSource.Missing
            } else {
                EditSource.Ready(job.size, photo, job.adjustments, job.whiteBackground, job.name)
            }
        }
    }

    when (val source = loaded) {
        null -> Scaffold(topBar = { StudioTopBar(stringResource(R.string.edit_title), onBack) }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        EditSource.Missing -> LaunchedEffect(Unit) { onMissingPhoto() }

        is EditSource.Ready -> AdjustContent(
            size = source.size,
            original = source.photo,
            startAdjustments = source.adjustments,
            startWhiteBackground = source.whiteBackground,
            startName = source.name,
            title = stringResource(R.string.edit_title),
            backLabel = stringResource(R.string.cancel),
            onBack = onBack,
            onCommit = { finalPhoto, adjustments, whiteBackground, name ->
                PhotoStore.update(context, jobId, finalPhoto, adjustments, whiteBackground, name)
                jobId
            },
            onCommitted = { onSaved() },
        )
    }
}

private sealed interface EditSource {
    data object Missing : EditSource

    data class Ready(
        val size: PhotoSize,
        val photo: Bitmap,
        val adjustments: Adjustments,
        val whiteBackground: Boolean,
        val name: String,
    ) : EditSource
}

@Composable
private fun AdjustContent(
    size: PhotoSize,
    original: Bitmap,
    startAdjustments: Adjustments,
    startWhiteBackground: Boolean,
    startName: String,
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    onCommit: (Bitmap, Adjustments, Boolean, String) -> String,
    onCommitted: (jobId: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var adj by remember { mutableStateOf(startAdjustments) }
    var whiteBg by remember { mutableStateOf(startWhiteBackground) }
    var overlay by remember { mutableStateOf<Bitmap?>(null) }
    var segmenting by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var askName by remember { mutableStateOf(false) }
    var photoName by remember { mutableStateOf(startName) }

    // Work out the white background once, the first time it is switched on.
    LaunchedEffect(whiteBg) {
        if (whiteBg && overlay == null) {
            segmenting = true
            try {
                overlay = BackgroundTools.whiteOverlay(original)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                whiteBg = false
                Toast.makeText(context, R.string.adjust_bg_failed, Toast.LENGTH_SHORT).show()
            } finally {
                segmenting = false
            }
        }
    }

    val image = remember(original) { original.asImageBitmap() }
    val overlayImage = remember(overlay) { overlay?.asImageBitmap() }
    val filter = remember(adj) { ColorFilter.colorMatrix(ColorMatrix(adj.matrix())) }

    fun commit(name: String) {
        saving = true
        scope.launch {
            val bg = if (whiteBg) overlay else null
            val id = withContext(Dispatchers.Default) {
                val finalPhoto = ImageTools.renderFinal(original, adj, bg)
                onCommit(finalPhoto, adj, whiteBg, name.trim())
            }
            onCommitted(id)
        }
    }

    Scaffold(
        topBar = {
            StudioTopBar(title, onBack = onBack) {
                TextButton(onClick = { adj = Adjustments() }) { Text(stringResource(R.string.adjust_reset)) }
            }
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !saving,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) { Text(backLabel) }
                Button(
                    onClick = { askName = true },
                    enabled = !segmenting && !saving,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(stringResource(R.string.adjust_next))
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
        ) {
            // Live preview. Colour changes apply to the person only; the white layer sits on top.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .aspectRatio(size.aspect, matchHeightConstraintsFirst = true)
                        .border(1.dp, Color(0xFFBDBDBD)),
                ) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = filter,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (whiteBg && overlayImage != null) {
                        Image(
                            bitmap = overlayImage,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                if (segmenting) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.7f)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.adjust_working), color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.adjust_white_bg),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = whiteBg, onCheckedChange = { whiteBg = it }, enabled = !segmenting)
            }
            SliderRow(stringResource(R.string.adjust_brightness), adj.brightness, -80f..80f) {
                adj = adj.copy(brightness = it)
            }
            SliderRow(stringResource(R.string.adjust_contrast), adj.contrast, 0.6f..1.6f) {
                adj = adj.copy(contrast = it)
            }
            SliderRow(stringResource(R.string.adjust_saturation), adj.saturation, 0f..2f) {
                adj = adj.copy(saturation = it)
            }
            SliderRow(stringResource(R.string.adjust_warmth), adj.warmth, -1f..1f) {
                adj = adj.copy(warmth = it)
            }
        }
    }

    if (askName) {
        NameDialog(
            name = photoName,
            onNameChange = { photoName = it },
            onCancel = { askName = false },
            onConfirm = {
                askName = false
                commit(photoName)
            },
        )
    }
}

/** Asked once, just before saving. A name makes the photo easy to find again later. */
@Composable
private fun NameDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.name_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.name_explain),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) onNameChange(it) },
                    label = { Text(stringResource(R.string.name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.name_save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}
