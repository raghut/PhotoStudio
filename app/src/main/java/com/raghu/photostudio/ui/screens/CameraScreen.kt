package com.raghu.photostudio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.raghu.photostudio.R
import com.raghu.photostudio.data.Prefs
import com.raghu.photostudio.image.ImageTools
import com.raghu.photostudio.model.PhotoSize
import com.raghu.photostudio.ui.CaptureHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Flash choices, in the order the button cycles through them. */
private enum class Flash(val mode: Int, val label: Int) {
    OFF(ImageCapture.FLASH_MODE_OFF, R.string.camera_flash_off),
    AUTO(ImageCapture.FLASH_MODE_AUTO, R.string.camera_flash_auto),
    ON(ImageCapture.FLASH_MODE_ON, R.string.camera_flash_on);

    fun next(): Flash = entries[(ordinal + 1) % entries.size]
}

/**
 * Where the photo frame sits on the camera preview. Used both to draw the frame and to cut the
 * captured image, so what parents see in the frame is exactly what gets printed.
 */
internal fun frameRect(viewW: Float, viewH: Float, aspect: Float): RectF {
    val maxW = viewW * 0.84f
    val maxH = viewH * 0.58f
    var w = maxW
    var h = w / aspect
    if (h > maxH) {
        h = maxH
        w = h * aspect
    }
    val cx = viewW / 2f
    val cy = viewH * 0.44f
    return RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
}

@Composable
fun CameraScreen(size: PhotoSize, onBack: () -> Unit, onCaptured: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraContent(size, onBack, onCaptured)
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.camera_permission),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.camera_allow))
                }
            }
        }
    }
}

@Composable
private fun CameraContent(photoSize: PhotoSize, onBack: () -> Unit, onCaptured: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    var useBackCamera by rememberSaveable { mutableStateOf(prefs.startWithBackCamera) }
    var flash by rememberSaveable { mutableStateOf(Flash.OFF) }
    var hasFlash by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        }
    }
    LaunchedEffect(useBackCamera) {
        try {
            cameraController.cameraSelector =
                if (useBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        } catch (e: Exception) {
            // Phone doesn't have that camera; keep the current one.
        }
    }
    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }

    // Most front cameras have no flash, so the button only appears when this one does.
    LaunchedEffect(useBackCamera) {
        hasFlash = withTimeoutOrNull(2_000) {
            // cameraInfo only exists once the camera is bound, which takes a frame or two.
            while (cameraController.cameraInfo == null) delay(50)
            cameraController.cameraInfo?.hasFlashUnit() == true
        } ?: false
        if (!hasFlash) flash = Flash.OFF
    }

    LaunchedEffect(flash, hasFlash) {
        cameraController.imageCaptureFlashMode = if (hasFlash) flash.mode else ImageCapture.FLASH_MODE_OFF
    }

    fun capture() {
        if (busy || viewSize == IntSize.Zero) return
        busy = true
        val vw = viewSize.width.toFloat()
        val vh = viewSize.height.toFloat()
        val frame = frameRect(vw, vh, photoSize.aspect)
        cameraController.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch {
                        val photo = try {
                            withContext(Dispatchers.Default) {
                                ImageTools.cropCapture(
                                    image, vw, vh, frame, photoSize.workingWidthPx, photoSize.workingHeightPx
                                )
                            }
                        } catch (e: Exception) {
                            null
                        } finally {
                            image.close()
                        }
                        busy = false
                        if (photo != null) {
                            CaptureHolder.photo = photo
                            onCaptured()
                        } else {
                            Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    busy = false
                    Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        // Live camera. Pinch to zoom and tap to focus work out of the box.
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = cameraController
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it },
        )

        // Frame overlay: dark outside, white border, face oval for passport photos.
        Canvas(Modifier.fillMaxSize()) {
            val r = frameRect(size.width, size.height, photoSize.aspect)
            val frame = Rect(r.left, r.top, r.right, r.bottom)
            val dim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRect(frame)
            }
            drawPath(dim, Color.Black.copy(alpha = 0.6f))
            drawRect(
                Color.White,
                topLeft = frame.topLeft,
                size = frame.size,
                style = Stroke(width = 3.dp.toPx()),
            )
            if (photoSize == PhotoSize.PASSPORT) {
                // Head should fill roughly 70–80% of a passport photo's height.
                val ovalW = frame.width * 0.58f
                val ovalH = frame.height * 0.62f
                drawOval(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(frame.center.x - ovalW / 2f, frame.top + frame.height * 0.12f),
                    size = Size(ovalW, ovalH),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)),
                    ),
                )
            }
        }

        // Hint at the top.
        Text(
            text = stringResource(
                if (photoSize == PhotoSize.PASSPORT) R.string.camera_hint_passport else R.string.camera_hint_full
            ),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )

        if (hasFlash) {
            FilledTonalButton(
                onClick = { flash = flash.next() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .widthIn(min = 108.dp),
            ) { Text(stringResource(flash.label)) }
        }

        // Controls at the bottom.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }

            Box(
                Modifier
                    .size(86.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(if (busy) Color.LightGray else Color.White)
                    .clickable(enabled = !busy) { capture() },
                contentAlignment = Alignment.Center,
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
            }

            FilledTonalIconButton(onClick = { useBackCamera = !useBackCamera }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.camera_switch))
            }
        }
    }
}
