package com.raghu.photostudio.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.raghu.photostudio.model.SheetLayout
import java.io.FileOutputStream

/**
 * Prints the sheet at its exact physical size (never "fit to page"), so a passport photo
 * always comes out 3.5 x 4.5 cm. If A4 is chosen by mistake, the 4x6 sheet is still printed
 * at true size in the middle of the A4 page.
 */
object SheetPrinter {

    fun print(context: Context, sheet: Bitmap, layout: SheetLayout, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val defaults = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.NA_INDEX_4X6.asPortrait())
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, SheetAdapter(context, sheet, layout, jobName), defaults)
    }

    private class SheetAdapter(
        private val context: Context,
        private val sheet: Bitmap,
        private val layout: SheetLayout,
        private val jobName: String,
    ) : PrintDocumentAdapter() {

        private var attributes: PrintAttributes? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            attributes = newAttributes
            val info = PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, newAttributes != oldAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            val attrs = attributes
            if (attrs == null) {
                callback.onWriteFailed("No print settings")
                return
            }
            val pdf = PrintedPdfDocument(context, attrs)
            try {
                val page = pdf.startPage(0)
                val canvas = page.canvas
                // PDF units are points: 72 per inch.
                val pageW = page.info.pageWidth.toFloat()
                val pageH = page.info.pageHeight.toFloat()
                val sheetW = layout.widthMm / 25.4f * 72f
                val sheetH = layout.heightMm / 25.4f * 72f

                canvas.save()
                canvas.translate(pageW / 2f, pageH / 2f)
                // Turn a landscape sheet to match a portrait page (printers feed 4x6 paper upright).
                if ((sheetW > sheetH) != (pageW > pageH)) canvas.rotate(90f)
                canvas.drawBitmap(
                    sheet,
                    null,
                    RectF(-sheetW / 2f, -sheetH / 2f, sheetW / 2f, sheetH / 2f),
                    Paint(Paint.FILTER_BITMAP_FLAG),
                )
                canvas.restore()
                pdf.finishPage(page)

                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdf.close()
            }
        }
    }
}
