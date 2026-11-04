package com.raghu.photostudio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.raghu.photostudio.R
import com.raghu.photostudio.data.PhotoJob
import com.raghu.photostudio.data.PhotoStore
import com.raghu.photostudio.data.Prefs
import com.raghu.photostudio.data.SheetExporter
import com.raghu.photostudio.data.SheetFormat
import com.raghu.photostudio.image.ImageTools
import com.raghu.photostudio.model.SheetLayout
import com.raghu.photostudio.print.SheetPrinter
import com.raghu.photostudio.ui.StudioTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SheetScreen(jobId: String, onBack: () -> Unit, onHome: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val job = remember(jobId) { PhotoStore.get(context, jobId) }

    if (job == null) {
        Scaffold(topBar = { StudioTopBar(stringResource(R.string.sheet_title), onBack) }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.sheet_not_found))
            }
        }
        return
    }

    val layout = remember(job) { SheetLayout.forSize(job.size) }
    val sheet by produceState<Bitmap?>(initialValue = null, job) {
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeFile(job.photoFile.path)?.let { photo ->
                ImageTools.renderSheet(photo, layout, prefs.cutLines)
            }
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var chooseFormat by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Writes the sheet to the phone, then offers to send it straight on.
    val saveAs: (SheetFormat) -> Unit = { format ->
        val current = sheet
        if (current != null && !saving) {
            saving = true
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    SheetExporter.save(context, current, layout, exportName(job), format)
                }
                saving = false
                if (saved == null) {
                    snackbar.showSnackbar(context.getString(R.string.save_failed))
                } else {
                    val message = when {
                        !saved.inPublicFolder -> R.string.saved_private
                        format == SheetFormat.PHOTO -> R.string.saved_photo
                        else -> R.string.saved_pdf
                    }
                    val result = snackbar.showSnackbar(
                        message = context.getString(message),
                        actionLabel = context.getString(R.string.sheet_share),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) SheetExporter.share(context, saved)
                }
            }
        }
    }

    // Android 9 and below ask for storage access before anything can reach Gallery / Downloads.
    var pendingFormat by remember { mutableStateOf<SheetFormat?>(null) }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val format = pendingFormat
        pendingFormat = null
        when {
            format == null -> Unit
            granted -> saveAs(format)
            else -> scope.launch { snackbar.showSnackbar(context.getString(R.string.save_permission)) }
        }
    }
    val requestSave: (SheetFormat) -> Unit = { format ->
        val needsPermission = SheetExporter.needsLegacyPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingFormat = format
            storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveAs(format)
        }
    }

    Scaffold(
        topBar = {
            StudioTopBar(stringResource(R.string.sheet_title), onBack) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_photo))
                }
                IconButton(onClick = { PhotoStore.share(context, job) }) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.sheet_share))
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.sheet_delete))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                OutlinedButton(
                    onClick = { chooseFormat = true },
                    enabled = sheet != null && !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(stringResource(if (saving) R.string.save_working else R.string.sheet_save))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                    ) { Text(stringResource(R.string.sheet_done)) }
                    Button(
                        onClick = {
                            val current = sheet ?: return@Button
                            val time = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(job.createdAt))
                            SheetPrinter.print(context, current, layout, context.getString(R.string.print_job_name, time))
                        },
                        enabled = sheet != null,
                        modifier = Modifier
                            .weight(1.6f)
                            .height(60.dp),
                    ) { Text(stringResource(R.string.sheet_print)) }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                val current = sheet
                Box(
                    Modifier
                        .aspectRatio(layout.widthMm / layout.heightMm)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFBDBDBD)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (current == null) {
                        CircularProgressIndicator()
                    } else {
                        Image(
                            bitmap = remember(current) { current.asImageBitmap() },
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (job.name.isNotBlank()) {
                Text(job.name, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = if (layout.count > 1) {
                    stringResource(R.string.sheet_info_passport, layout.count)
                } else {
                    stringResource(R.string.sheet_info_full)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.sheet_tip),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    if (chooseFormat) {
        AlertDialog(
            onDismissRequest = { chooseFormat = false },
            title = { Text(stringResource(R.string.save_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormatOption(
                        title = stringResource(R.string.save_photo),
                        detail = stringResource(R.string.save_photo_desc),
                        onClick = {
                            chooseFormat = false
                            requestSave(SheetFormat.PHOTO)
                        },
                    )
                    FormatOption(
                        title = stringResource(R.string.save_pdf),
                        detail = stringResource(R.string.save_pdf_desc),
                        onClick = {
                            chooseFormat = false
                            requestSave(SheetFormat.PDF)
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { chooseFormat = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    PhotoStore.delete(context, job.id)
                    onBack()
                }) { Text(stringResource(R.string.delete_yes), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FormatOption(title: String, detail: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * File name the shop will recognise later: the photo's name when it has one, e.g.
 * Ravi_passport_04-11-2026_1830, otherwise PhotoStudio_passport_04-11-2026_1830.
 */
private fun exportName(job: PhotoJob): String {
    val stamp = SimpleDateFormat("dd-MM-yyyy_HHmm", Locale.US).format(Date(job.createdAt))
    val tag = if (job.size.fullSheet) "4x6" else "passport"
    // Keep letters and digits of any language (Telugu included); everything else becomes "_".
    val safeName = job.name.replace(Regex("""[^\p{L}\p{N}]+"""), "_").trim('_').take(40)
    return if (safeName.isEmpty()) "PhotoStudio_${tag}_$stamp" else "${safeName}_${tag}_$stamp"
}
