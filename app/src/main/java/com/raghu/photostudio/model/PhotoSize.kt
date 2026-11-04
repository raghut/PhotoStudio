package com.raghu.photostudio.model

/**
 * Physical photo sizes the shop can print. Add a new entry here (e.g. STAMP(20f, 25f, false))
 * and the camera frame, sheet layout and printing all adapt automatically.
 */
enum class PhotoSize(val widthMm: Float, val heightMm: Float, val fullSheet: Boolean) {
    /** Indian passport / visa / Aadhaar-style photo: 3.5 x 4.5 cm. */
    PASSPORT(35f, 45f, false),

    /** One full 4 x 6 inch photo (portrait). */
    FOUR_BY_SIX(101.6f, 152.4f, true);

    /** width / height */
    val aspect: Float get() = widthMm / heightMm

    /**
     * Resolution we keep the single photo at. 600 dpi for small photos (sharp, good for sharing too),
     * capped so a 4x6 photo stays a manageable size in memory.
     */
    val workingDpi: Float
        get() {
            val longInches = maxOf(widthMm, heightMm) / 25.4f
            return minOf(600f, 1800f / longInches)
        }

    val workingWidthPx: Int get() = (widthMm / 25.4f * workingDpi).toInt()
    val workingHeightPx: Int get() = (heightMm / 25.4f * workingDpi).toInt()
}
