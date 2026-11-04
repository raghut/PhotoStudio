package com.raghu.photostudio.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.raghu.photostudio.model.SheetLayout
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object ImageTools {

    private const val SHEET_DPI = 300f
    private fun mmToPx(mm: Float): Float = mm / 25.4f * SHEET_DPI

    /**
     * Turns a camera capture into the photo inside the on-screen frame.
     *
     * @param viewW/viewH size of the camera preview on screen (px)
     * @param frame the frame rectangle drawn on the preview (px, same coordinate space)
     */
    fun cropCapture(
        image: ImageProxy,
        viewW: Float,
        viewH: Float,
        frame: RectF,
        outW: Int,
        outH: Int,
    ): Bitmap {
        var bmp = image.toBitmap()

        // CameraX may report a crop matching the preview's visible area.
        val crop = image.cropRect
        val cropIsValid = crop.width() > 0 && crop.height() > 0 &&
            crop.left >= 0 && crop.top >= 0 && crop.right <= bmp.width && crop.bottom <= bmp.height
        if (cropIsValid && (crop.width() != bmp.width || crop.height() != bmp.height)) {
            bmp = Bitmap.createBitmap(bmp, crop.left, crop.top, crop.width(), crop.height())
        }

        val rotation = image.imageInfo.rotationDegrees
        if (rotation != 0) {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }

        // The preview uses FILL_CENTER: the image is scaled to cover the view and centred.
        // Work out which part of the image the user actually saw, then map the frame into it.
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val scale = max(viewW / w, viewH / h)
        val offX = (w - viewW / scale) / 2f
        val offY = (h - viewH / scale) / 2f

        val left = (offX + frame.left / scale).roundToInt().coerceIn(0, bmp.width - 1)
        val top = (offY + frame.top / scale).roundToInt().coerceIn(0, bmp.height - 1)
        val right = (offX + frame.right / scale).roundToInt().coerceIn(left + 1, bmp.width)
        val bottom = (offY + frame.bottom / scale).roundToInt().coerceIn(top + 1, bmp.height)

        val cropped = Bitmap.createBitmap(bmp, left, top, right - left, bottom - top)
        return Bitmap.createScaledBitmap(cropped, outW, outH, true)
    }

    /** Final single photo: colour adjustments + optional white background layer. */
    fun renderFinal(original: Bitmap, adj: Adjustments, whiteOverlay: Bitmap?): Bitmap {
        val out = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(adj.matrix())
        }
        canvas.drawBitmap(original, 0f, 0f, paint)
        if (whiteOverlay != null) {
            canvas.drawBitmap(
                whiteOverlay,
                null,
                Rect(0, 0, out.width, out.height),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
        }
        return out
    }

    /** Lays out copies of the photo on a 4x6 sheet at 300 dpi, ready to print. */
    fun renderSheet(photo: Bitmap, layout: SheetLayout, cutLines: Boolean): Bitmap {
        val wPx = mmToPx(layout.widthMm).roundToInt()
        val hPx = mmToPx(layout.heightMm).roundToInt()
        val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        if (cutLines && layout.count > 1) {
            // Lines run across the whole sheet; photos are drawn on top, so lines only show in the gaps.
            val linePaint = Paint().apply {
                color = 0xFFBDBDBD.toInt()
                strokeWidth = 2f
            }
            val xs = layout.cells.flatMap { listOf(it.left, it.right) }.distinct()
            val ys = layout.cells.flatMap { listOf(it.top, it.bottom) }.distinct()
            xs.forEach { val x = mmToPx(it); canvas.drawLine(x, 0f, x, hPx.toFloat(), linePaint) }
            ys.forEach { val y = mmToPx(it); canvas.drawLine(0f, y, wPx.toFloat(), y, linePaint) }
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        layout.cells.forEach { r ->
            val dst = RectF(mmToPx(r.left), mmToPx(r.top), mmToPx(r.right), mmToPx(r.bottom))
            canvas.drawBitmap(photo, null, dst, paint)
        }
        return out
    }

    /** Small version of a saved photo for the history grid. */
    fun decodeThumbnail(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
