package com.ivy.data.skin.fake

import android.net.Uri
import com.ivy.data.skin.CardSkinImageStore
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeCardSkinImageStore : CardSkinImageStore {
    val committed = mutableMapOf<UUID, String>()
    var staged: String? = null
    var stageFails: Boolean = false
    private var version = 0L

    override fun imagePath(cardId: UUID): String? = committed[cardId]

    override suspend fun stage(uri: Uri): Result<Unit> {
        if (stageFails) return Result.failure(IllegalStateException("stage failed"))
        version++
        staged = "staging-$version.jpg"
        return Result.success(Unit)
    }

    override fun stagedPath(): String? = staged

    override fun hasStaged(): Boolean = staged != null

    override fun commitStaged(cardId: UUID): String? {
        val path = staged ?: return null
        staged = null
        return "$cardId-$path".also { committed[cardId] = it }
    }

    override fun discardStaged() {
        staged = null
    }

    override fun delete(cardId: UUID) {
        committed.remove(cardId)
    }

    override fun deleteAll() {
        committed.clear()
        staged = null
    }
}
