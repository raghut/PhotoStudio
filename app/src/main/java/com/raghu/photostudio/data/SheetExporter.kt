package com.raghu.photostudio.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.raghu.photostudio.model.SheetLayout
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt

/** The two things the shop can take away from the print preview. */
enum class SheetFormat(val mime: String, val extension: String) {
    /** The whole 4x6 sheet as one JPEG — goes to the Gallery, prints at any photo lab. */
    PHOTO("image/jpeg", "jpg"),

    /** The same sheet as a true-size PDF — for e-mail, or printing from a computer. */
    PDF("application/pdf", "pdf"),
}

/**
 * Saves the finished 4x6 sheet to the phone so it can be printed or sent later.
 *
 * Every export is written twice: once into the app's cache (so Share works on every Android
 * version through the FileProvider) and once into the phone's own Gallery / Downloads folder
 * (so the file is visible outside the app).
 */
object SheetExporter {

    private const val ALBUM = "Photo Studio"

    data class Saved(
        val shareUri: Uri,
        val format: SheetFormat,
        val name: String,
        /** False when the phone refused the public copy — the file is still shareable. */
        val inPublicFolder: Boolean,
    )

    /** Writing to Gallery / Downloads needs the old storage permission on Android 9 and below. */
    val needsLegacyPermission: Boolean
        get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    /** Blocking — call from a background dispatcher. Returns null if nothing could be written. */
    fun save(
        context: Context,
        sheet: Bitmap,
        layout: SheetLayout,
        baseName: String,
        format: SheetFormat,
    ): Saved? = runCatching {
        val name = "$baseName.${format.extension}"
        val cached = File(File(context.cacheDir, "export").apply { mkdirs() }, name)
        FileOutputStream(cached).use { out -> render(sheet, layout, format, out) }

        val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cached)
        Saved(shareUri, format, name, copyToPublicFolder(context, cached, name, format))
    }.getOrNull()

    fun share(context: Context, saved: Saved) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = saved.format.mime
            putExtra(Intent.EXTRA_STREAM, saved.shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    private fun render(sheet: Bitmap, layout: SheetLayout, format: SheetFormat, out: OutputStream) {
        when (format) {
            // The sheet bitmap is already the full 4x6 at 300 dpi (1200 x 1800 px).
            SheetFormat.PHOTO -> sheet.compress(Bitmap.CompressFormat.JPEG, 95, out)
            SheetFormat.PDF -> writePdf(sheet, layout, out)
        }
    }

    /**
     * One page the exact physical size of the sheet, with the image filling it edge to edge.
     * No margins and no scaling, so a passport photo still measures 3.5 x 4.5 cm on paper.
     */
    private fun writePdf(sheet: Bitmap, layout: SheetLayout, out: OutputStream) {
        // PDF units are points: 72 per inch.
        val wPt = (layout.widthMm / 25.4f * 72f).roundToInt()
        val hPt = (layout.heightMm / 25.4f * 72f).roundToInt()
        val pdf = PdfDocument()
        try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(wPt, hPt, 1).create())
            page.canvas.drawBitmap(
                sheet,
                null,
                RectF(0f, 0f, wPt.toFloat(), hPt.toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
            pdf.finishPage(page)
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    /** Photos go to the Gallery, PDFs to Downloads — both inside a "Photo Studio" folder. */
    private fun copyToPublicFolder(context: Context, source: File, name: String, format: SheetFormat): Boolean =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                copyViaMediaStore(context, source, name, format)
            } else {
                copyViaFilesystem(context, source, name, format)
            }
        }.getOrDefault(false)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyViaMediaStore(context: Context, source: File, name: String, format: SheetFormat): Boolean {
        val collection = if (format == SheetFormat.PHOTO) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val folder = if (format == SheetFormat.PHOTO) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS
        val pending = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/$ALBUM")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, pending) ?: return false
        val written = runCatching {
            resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } } != null
        }.getOrDefault(false)
        if (!written) {
            resolver.delete(uri, null, null)
            return false
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return true
    }

    /** Android 9 and below: write the file, then tell the Gallery it is there. */
    private fun copyViaFilesystem(context: Context, source: File, name: String, format: SheetFormat): Boolean {
        val folder = if (format == SheetFormat.PHOTO) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(folder), ALBUM)
        if (!dir.exists() && !dir.mkdirs()) return false
        val target = File(dir, name)
        source.copyTo(target, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(target.path), arrayOf(format.mime), null)
        return true
    }
}
