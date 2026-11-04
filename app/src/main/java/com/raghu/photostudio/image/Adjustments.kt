package com.raghu.photostudio.image

import android.graphics.ColorMatrix

/** Slider values from the Adjust screen. */
data class Adjustments(
    val brightness: Float = 0f,   // -80 .. 80   (added to each colour channel, 0..255 scale)
    val contrast: Float = 1f,     // 0.6 .. 1.6
    val saturation: Float = 1f,   // 0 .. 2      (0 = black & white)
    val warmth: Float = 0f,       // -1 .. 1     (negative = cooler/blue, positive = warmer/yellow)
) {
    /** 4x5 colour matrix, usable both by Compose (live preview) and android.graphics (final render). */
    fun matrix(): FloatArray {
        val m = ColorMatrix()
        m.setSaturation(saturation)

        val c = contrast
        val t = 128f * (1f - c) + brightness
        m.postConcat(
            ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )

        val r = 1f + warmth * 0.12f
        val b = 1f - warmth * 0.12f
        m.postConcat(
            ColorMatrix(
                floatArrayOf(
                    r, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
        return m.array
    }
}
