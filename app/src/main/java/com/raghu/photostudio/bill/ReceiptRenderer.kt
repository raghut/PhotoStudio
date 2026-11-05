package com.raghu.photostudio.bill

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Typeface
import com.raghu.photostudio.tools.Bill
import com.raghu.photostudio.tools.Money

/**
 * Draws a bill as a receipt. It is recorded once as a [Picture] (vector drawing), so the same
 * drawing becomes a sharp PNG for WhatsApp and a sharp PDF page for the printer.
 */
object ReceiptRenderer {

    /** Receipt width in drawing units. Everything else is laid out against this. */
    const val WIDTH = 1080

    class Receipt(val picture: Picture, val width: Int, val height: Int) {
        fun toBitmap(): Bitmap {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            c.drawPicture(picture)
            return bmp
        }
    }

    fun render(bill: Bill, l: Bill.Labels): Receipt {
        val picture = Picture()
        // Record on a tall canvas, then keep only the height actually used.
        val canvas = picture.beginRecording(WIDTH, 20_000)
        val height = Drawer(canvas).draw(bill, l)
        picture.endRecording()
        return Receipt(picture, WIDTH, height)
    }

    private class Drawer(private val c: Canvas) {
        private val m = 56f
        private val right = WIDTH - m
        private var y = m

        private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK, align: Paint.Align = Paint.Align.LEFT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                textAlign = align
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }

        private val grey = Color.rgb(90, 90, 90)

        // Columns (right edges) for qty, rate, amount; the item name gets the rest.
        private val amountX = right
        private val rateX = right - 230f
        private val qtyX = right - 440f
        private val nameMax = qtyX - 110f - m

        fun draw(bill: Bill, l: Bill.Labels): Int {
            // Header
            if (bill.shopName.isNotBlank()) {
                centred(bill.shopName.trim(), paint(56f, bold = true))
                y += 8f
            }
            if (bill.shopDetails.isNotBlank()) centred(bill.shopDetails.trim(), paint(32f, color = grey))
            y += 18f
            rule(dashed = false)

            val info = paint(32f)
            line("${l.bill} #${bill.number}", info, "${l.date}: ${Money.date(bill.date)}", info)
            if (bill.customerName.isNotBlank() || bill.customerPhone.isNotBlank()) {
                val who = listOf(bill.customerName.trim(), bill.customerPhone.trim()).filter { it.isNotEmpty() }
                text("${l.customer}: ${who.joinToString(" · ")}", info)
            }
            y += 6f
            rule(dashed = true)

            // Column headings
            val head = paint(28f, bold = true, color = grey)
            val headRight = paint(28f, bold = true, color = grey, align = Paint.Align.RIGHT)
            baseline(head) {
                c.drawText(l.item, m, it, head)
                c.drawText(l.qty, qtyX, it, headRight)
                c.drawText(l.rate, rateX, it, headRight)
                c.drawText(l.amount, amountX, it, headRight)
            }
            y += 6f
            rule(dashed = true)

            // Items
            val name = paint(34f)
            val num = paint(34f, align = Paint.Align.RIGHT)
            bill.items.forEachIndexed { i, item ->
                val lines = wrap("${i + 1}. ${item.name.trim()}", name, nameMax)
                lines.forEachIndexed { k, part ->
                    baseline(name) {
                        c.drawText(part, m, it, name)
                        if (k == 0) {
                            c.drawText(Money.plain(item.qty), qtyX, it, num)
                            c.drawText(Money.rupees(item.rate), rateX, it, num)
                            c.drawText(Money.rupees(item.amount), amountX, it, num)
                        }
                    }
                }
                y += 10f
            }
            rule(dashed = true)

            // Totals
            val label = paint(34f)
            val value = paint(34f, align = Paint.Align.RIGHT)
            if (bill.discount > 0) {
                total(l.subtotal, Money.rupees(bill.subtotal), label, value)
                total(l.discount, "− " + Money.rupees(bill.discount), label, value)
            }
            y += 4f
            total(l.total, Money.rupees(bill.total), paint(46f, bold = true), paint(46f, bold = true, align = Paint.Align.RIGHT))
            val bal = bill.balance
            if (bill.received != null && bal != null) {
                total(l.received, Money.rupees(bill.received), label, value)
                total(l.balance, Money.rupees(bal), paint(38f, bold = true), paint(38f, bold = true, align = Paint.Align.RIGHT))
            }
            y += 14f
            rule(dashed = false)
            y += 10f
            centred(l.thanks, paint(32f, color = grey))
            return (y + m).toInt()
        }

        /** Runs [block] with the baseline for one line of [p], then moves down a line. */
        private inline fun baseline(p: Paint, block: (Float) -> Unit) {
            val fm = p.fontMetrics
            block(y - fm.ascent)
            y += fm.descent - fm.ascent
        }

        private fun text(s: String, p: Paint) = wrap(s, p, right - m).forEach { part ->
            baseline(p) { c.drawText(part, m, it, p) }
        }

        private fun centred(s: String, p: Paint) {
            p.textAlign = Paint.Align.CENTER
            wrap(s, p, right - m).forEach { part -> baseline(p) { c.drawText(part, WIDTH / 2f, it, p) } }
        }

        private fun line(left: String, lp: Paint, rightText: String, rp: Paint) {
            val rpR = Paint(rp).apply { textAlign = Paint.Align.RIGHT }
            baseline(lp) {
                c.drawText(left, m, it, lp)
                c.drawText(rightText, right, it, rpR)
            }
        }

        private fun total(label: String, value: String, lp: Paint, vp: Paint) {
            baseline(lp) {
                c.drawText(label, m + 300f, it, lp)
                c.drawText(value, right, it, vp)
            }
            y += 6f
        }

        private fun rule(dashed: Boolean) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (dashed) grey else Color.BLACK
                strokeWidth = if (dashed) 2f else 3f
                if (dashed) pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }
            y += 8f
            c.drawLine(m, y, right, y, p)
            y += 16f
        }

        /** Splits [s] into lines no wider than [max], breaking at spaces where possible. */
        private fun wrap(s: String, p: Paint, max: Float): List<String> {
            val out = ArrayList<String>()
            var rest = s
            while (rest.isNotEmpty()) {
                var n = p.breakText(rest, true, max, null)
                if (n <= 0) n = 1
                if (n < rest.length) {
                    val space = rest.lastIndexOf(' ', n)
                    if (space > 0) n = space
                }
                out += rest.substring(0, n).trimEnd()
                rest = rest.substring(n).trimStart()
            }
            return out.ifEmpty { listOf("") }
        }
    }
}
