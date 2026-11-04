package com.raghu.photostudio.model

import android.graphics.RectF
import kotlin.math.floor
import kotlin.math.max

/** A 4 x 6 inch sheet with photo cells, all measured in millimetres. */
data class SheetLayout(
    val landscape: Boolean,
    val cells: List<RectF>,
) {
    val widthMm: Float get() = if (landscape) SHEET_LONG_MM else SHEET_SHORT_MM
    val heightMm: Float get() = if (landscape) SHEET_SHORT_MM else SHEET_LONG_MM
    val count: Int get() = cells.size

    companion object {
        const val SHEET_LONG_MM = 152.4f   // 6 inch
        const val SHEET_SHORT_MM = 101.6f  // 4 inch

        /** Blank border kept on every side so printers without borderless mode don't clip photos. */
        private const val EDGE_MM = 3.5f

        /** Minimum space between photos for cutting. */
        private const val GAP_MM = 1.5f

        fun forSize(size: PhotoSize): SheetLayout {
            if (size.fullSheet) {
                val landscape = size.widthMm > size.heightMm
                val w = if (landscape) SHEET_LONG_MM else SHEET_SHORT_MM
                val h = if (landscape) SHEET_SHORT_MM else SHEET_LONG_MM
                return SheetLayout(landscape, listOf(RectF(0f, 0f, w, h)))
            }
            // Try both sheet orientations and keep the one that fits more photos.
            // Passport (35x45mm): landscape fits 4 x 2 = 8, portrait only 2 x 3 = 6.
            val landscape = grid(SHEET_LONG_MM, SHEET_SHORT_MM, size, true)
            val portrait = grid(SHEET_SHORT_MM, SHEET_LONG_MM, size, false)
            return if (portrait.count > landscape.count) portrait else landscape
        }

        private fun grid(sheetW: Float, sheetH: Float, size: PhotoSize, landscape: Boolean): SheetLayout {
            val xs = axis(sheetW, size.widthMm)
            val ys = axis(sheetH, size.heightMm)
            val cells = ys.flatMap { y -> xs.map { x -> RectF(x, y, x + size.widthMm, y + size.heightMm) } }
            return SheetLayout(landscape, cells)
        }

        /** Start positions of photos along one side of the sheet. */
        private fun axis(total: Float, item: Float): List<Float> {
            val n = floor((total - 2 * EDGE_MM + GAP_MM) / (item + GAP_MM)).toInt()
            if (n <= 0) return emptyList()
            val spare = total - n * item
            val gap = if (n > 1) max(GAP_MM, (spare - 2 * EDGE_MM) / (n - 1)) else 0f
            val start = (spare - (n - 1) * gap) / 2f
            return List(n) { i -> start + i * (item + gap) }
        }
    }
}
