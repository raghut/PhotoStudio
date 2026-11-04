package com.raghu.photostudio.image

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object BackgroundTools {

    /**
     * Finds the person in the photo (on-device, offline) and returns a layer that is opaque white
     * over the background and transparent over the person. Drawing this layer on top of the
     * photo gives a white background, while brightness/contrast still only change the person.
     */
    suspend fun whiteOverlay(src: Bitmap): Bitmap = suspendCancellableCoroutine { cont ->
        val segmenter = Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
        segmenter.process(InputImage.fromBitmap(src, 0))
            .addOnSuccessListener { mask ->
                try {
                    val w = mask.width
                    val h = mask.height
                    val buffer = mask.buffer
                    buffer.rewind()
                    val pixels = IntArray(w * h)
                    for (i in 0 until w * h) {
                        val confidence = buffer.float // 1 = person, 0 = background
                        // Soft edge between 0.35 and 0.65 so hair/shoulders don't look cut out.
                        val person = ((confidence - 0.35f) / 0.3f).coerceIn(0f, 1f)
                        val alpha = ((1f - person) * 255f).toInt()
                        pixels[i] = (alpha shl 24) or 0x00FFFFFF
                    }
                    var overlay = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
                    if (w != src.width || h != src.height) {
                        overlay = Bitmap.createScaledBitmap(overlay, src.width, src.height, true)
                    }
                    if (cont.isActive) cont.resume(overlay)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resumeWithException(e)
                } finally {
                    segmenter.close()
                }
            }
            .addOnFailureListener { e ->
                segmenter.close()
                if (cont.isActive) cont.resumeWithException(e)
            }
    }
}
