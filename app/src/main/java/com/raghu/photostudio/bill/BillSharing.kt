package com.raghu.photostudio.bill

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.core.content.FileProvider
import com.raghu.photostudio.tools.Bill
import java.io.File
import java.io.FileOutputStream

/** Sends a bill out: as a receipt picture, as a WhatsApp text, or to the printer. */
object BillSharing {

    /** Receipt as a PNG picture — pick WhatsApp and the customer in the share sheet. */
    fun shareImage(context: Context, bill: Bill, labels: Bill.Labels) {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, "bill_${bill.number}.png")
        val bmp = ReceiptRenderer.render(bill, labels).toBitmap()
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    /**
     * Bill as a text message. With the customer's mobile number it opens their WhatsApp chat
     * directly with the bill typed in; otherwise the share sheet.
     */
    fun shareText(context: Context, bill: Bill, labels: Bill.Labels) {
        val text = bill.toText(labels)
        Bill.whatsAppNumber(bill.customerPhone)?.let { number ->
            val chat = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=" + Uri.encode(text)))
            try {
                context.startActivity(chat)
                return
            } catch (_: ActivityNotFoundException) {
                // No WhatsApp and no browser: fall through to the share sheet.
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    /** Prints the receipt at the top of the page, about 10 cm wide (cut off the rest of A4). */
    fun print(context: Context, bill: Bill, labels: Bill.Labels, jobName: String) {
        val receipt = ReceiptRenderer.render(bill, labels)
        val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val defaults = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        pm.print(jobName, ReceiptAdapter(context, receipt, jobName), defaults)
    }

    private class ReceiptAdapter(
        private val context: Context,
        private val receipt: ReceiptRenderer.Receipt,
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
                callback.onLayoutCancelled(); return
            }
            attributes = newAttributes
            val info = PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
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
            val attrs = attributes ?: run { callback.onWriteFailed("No print settings"); return }
            val pdf = PrintedPdfDocument(context, attrs)
            try {
                val page = pdf.startPage(0)
                val canvas = page.canvas
                // Points: 72 per inch. Receipt about 100 mm wide, smaller if the paper is smaller.
                val pageW = page.info.pageWidth.toFloat()
                val pageH = page.info.pageHeight.toFloat()
                val margin = 24f
                val maxW = minOf(100f / 25.4f * 72f, pageW - 2 * margin)
                var scale = maxW / receipt.width
                if (receipt.height * scale > pageH - 2 * margin) scale = (pageH - 2 * margin) / receipt.height
                canvas.save()
                canvas.translate((pageW - receipt.width * scale) / 2f, margin)
                canvas.scale(scale, scale)
                canvas.drawPicture(receipt.picture)
                canvas.restore()
                pdf.finishPage(page)
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled(); return
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
