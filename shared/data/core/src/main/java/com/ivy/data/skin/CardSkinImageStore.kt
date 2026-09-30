package com.ivy.data.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.ivy.base.threading.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device-only photos used as card faces. Files live in the app's private files dir
 * (never in backups). Picking a photo first stages it; saving the card commits the
 * staged file under a versioned name so an image loader never serves a stale bitmap.
 */
interface CardSkinImageStore {
    /** The committed photo of the card, or null when none exists on this device. */
    fun imagePath(cardId: UUID): String?

    /** Decodes, downsizes and stores the picked image as the staged photo. */
    suspend fun stage(uri: Uri): Result<Unit>

    /** The staged photo's path (to preview it before saving), or null. */
    fun stagedPath(): String?

    /** Whether a staged photo is waiting to be committed. */
    fun hasStaged(): Boolean

    /** Moves the staged photo into place for [cardId], replacing any older version. */
    fun commitStaged(cardId: UUID): String?

    /** Drops the staged photo (cancelled edit). */
    fun discardStaged()

    /** Removes every photo of [cardId]. */
    fun delete(cardId: UUID)

    /** Removes every photo (logout). */
    fun deleteAll()
}

private const val MaxWidthPx = 1200
private const val JpegQuality = 85

/** Power-of-two sample size that brings [width] down to at most [maxWidth]. */
fun computeSampleSize(width: Int, maxWidth: Int = MaxWidthPx): Int {
    var sample = 1
    while (width / (sample * 2) >= maxWidth) sample *= 2
    return sample
}

@Singleton
class FilesDirCardSkinImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatchersProvider,
) : CardSkinImageStore {

    private val dir: File get() = File(context.filesDir, DirName).apply { mkdirs() }

    /** Staged files are versioned too, so a re-pick never reuses a cached bitmap. */
    private val stagedFiles: List<File>
        get() = dir.listFiles { file -> file.name.startsWith(StagingPrefix) }?.toList().orEmpty()
    private val staging: File? get() = stagedFiles.maxByOrNull { it.lastModified() }

    override fun imagePath(cardId: UUID): String? = versions(cardId).maxByOrNull { it.lastModified() }?.absolutePath

    override suspend fun stage(uri: Uri): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                decoder.setTargetSampleSize(computeSampleSize(info.size.width))
            }
            val target = File(dir, "$StagingPrefix${System.currentTimeMillis()}.jpg")
            target.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JpegQuality, out)
            }
            bitmap.recycle()
            stagedFiles.filter { it != target }.forEach { it.delete() }
        }
    }

    override fun stagedPath(): String? = staging?.absolutePath

    override fun hasStaged(): Boolean = staging != null

    override fun commitStaged(cardId: UUID): String? = staging?.let { staged ->
        val target = File(dir, "$cardId-${System.currentTimeMillis()}.jpg")
        val moved = staged.renameTo(target) || staged.copyTo(target, overwrite = true).exists().also { staged.delete() }
        if (moved) {
            versions(cardId).filter { it != target }.forEach { it.delete() }
            discardStaged()
        }
        target.absolutePath.takeIf { moved }
    }

    override fun discardStaged() {
        stagedFiles.forEach { it.delete() }
    }

    override fun delete(cardId: UUID) {
        versions(cardId).forEach { it.delete() }
    }

    override fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun versions(cardId: UUID): List<File> =
        dir.listFiles { file -> file.name.startsWith("$cardId-") && file.name.endsWith(".jpg") }?.toList().orEmpty()

    companion object {
        const val DirName = "card_skins"
        private const val StagingPrefix = "staging-"
    }
}
