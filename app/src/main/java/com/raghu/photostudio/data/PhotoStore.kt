package com.raghu.photostudio.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.raghu.photostudio.image.Adjustments
import com.raghu.photostudio.model.PhotoSize
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

data class PhotoJob(
    val id: String,
    val size: PhotoSize,
    val createdAt: Long,
    val dir: File,
    /** What the shop called this photo, or "" when it was never named. */
    val name: String,
    /** Slider values used last time, so editing again starts where it left off. */
    val adjustments: Adjustments,
    val whiteBackground: Boolean,
) {
    /** The finished photo: colour changes and white background already applied. */
    val photoFile: File get() = File(dir, PHOTO_FILE)

    /** The untouched capture, kept so the photo can be edited again without losing quality. */
    val originalFile: File get() = File(dir, ORIGINAL_FILE)

    /** What the Adjust screen re-opens: the raw capture when there is one, else the finished photo. */
    val editFile: File get() = if (originalFile.exists()) originalFile else photoFile

    /**
     * True for photos taken before the app kept originals. Editing those starts from the
     * finished photo, so the earlier changes stay baked in.
     */
    val editsAreBakedIn: Boolean get() = !originalFile.exists()

    companion object {
        const val PHOTO_FILE = "photo.jpg"
        const val ORIGINAL_FILE = "original.jpg"
    }
}

/**
 * Saves every finished photo in the app's private storage:
 * files/jobs/<timestamp>/{photo.jpg, original.jpg, meta.txt}
 */
object PhotoStore {

    private const val META_FILE = "meta.txt"

    /** Photos from the first version recorded only the size, in their own file. */
    private const val LEGACY_SIZE_FILE = "size.txt"

    private fun root(context: Context): File = File(context.filesDir, "jobs").apply { mkdirs() }

    /**
     * @param original the straight-from-camera crop, kept for editing later
     * @param finalPhoto the same photo with the adjustments and white background applied
     */
    fun save(
        context: Context,
        size: PhotoSize,
        original: Bitmap,
        finalPhoto: Bitmap,
        adjustments: Adjustments,
        whiteBackground: Boolean,
        name: String,
    ): String {
        val id = System.currentTimeMillis().toString()
        val dir = File(root(context), id).apply { mkdirs() }
        writeJpeg(File(dir, PhotoJob.PHOTO_FILE), finalPhoto)
        writeJpeg(File(dir, PhotoJob.ORIGINAL_FILE), original)
        writeMeta(dir, size, name, adjustments, whiteBackground)
        return id
    }

    /** Replaces the finished photo of an existing job, keeping its id, date and original. */
    fun update(
        context: Context,
        id: String,
        finalPhoto: Bitmap,
        adjustments: Adjustments,
        whiteBackground: Boolean,
        name: String,
    ): Boolean {
        val dir = File(root(context), id)
        val job = load(dir) ?: return false
        writeJpeg(File(dir, PhotoJob.PHOTO_FILE), finalPhoto)
        writeMeta(dir, job.size, name, adjustments, whiteBackground)
        return true
    }

    fun list(context: Context): List<PhotoJob> =
        root(context).listFiles()
            ?.mapNotNull { load(it) }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()

    fun get(context: Context, id: String): PhotoJob? = load(File(root(context), id))

    fun delete(context: Context, id: String) {
        File(root(context), id).deleteRecursively()
    }

    private fun writeJpeg(target: File, bitmap: Bitmap) {
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    }

    private fun writeMeta(
        dir: File,
        size: PhotoSize,
        name: String,
        adjustments: Adjustments,
        whiteBackground: Boolean,
    ) {
        val meta = Properties()
        meta["size"] = size.name
        meta["name"] = name
        meta["brightness"] = adjustments.brightness.toString()
        meta["contrast"] = adjustments.contrast.toString()
        meta["saturation"] = adjustments.saturation.toString()
        meta["warmth"] = adjustments.warmth.toString()
        meta["white_bg"] = whiteBackground.toString()
        File(dir, META_FILE).outputStream().use { meta.store(it, "Photo Studio") }
    }

    private fun readMeta(dir: File): Properties {
        val meta = Properties()
        val file = File(dir, META_FILE)
        if (file.exists()) runCatching { file.inputStream().use { meta.load(it) } }
        return meta
    }

    private fun load(dir: File): PhotoJob? {
        if (!dir.isDirectory || !File(dir, PhotoJob.PHOTO_FILE).exists()) return null
        val meta = readMeta(dir)
        val sizeName = meta.getProperty("size")
            ?: runCatching { File(dir, LEGACY_SIZE_FILE).readText().trim() }.getOrNull()
        val size = runCatching { PhotoSize.valueOf(sizeName.orEmpty()) }.getOrDefault(PhotoSize.PASSPORT)
        val adjustments = Adjustments(
            brightness = meta.float("brightness", 0f),
            contrast = meta.float("contrast", 1f),
            saturation = meta.float("saturation", 1f),
            warmth = meta.float("warmth", 0f),
        )
        return PhotoJob(
            id = dir.name,
            size = size,
            createdAt = dir.name.toLongOrNull() ?: dir.lastModified(),
            dir = dir,
            name = meta.getProperty("name").orEmpty(),
            adjustments = adjustments,
            // Photos from the first version have the white background already baked in.
            whiteBackground = meta.getProperty("white_bg")?.toBoolean() ?: false,
        )
    }

    private fun Properties.float(key: String, fallback: Float): Float =
        getProperty(key)?.toFloatOrNull() ?: fallback

    /** Send the single photo to WhatsApp / Gmail etc. — handy for online application forms. */
    fun share(context: Context, job: PhotoJob) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", job.photoFile)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    }
}
